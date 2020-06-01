package com.legion.gms;

import com.legion.common.event.EventBus;
import com.legion.net.entities.InetAddressAndPort;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.entities.LegionNodeInfo;
import com.legion.node.controller.ClusterEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FailureDetector implements IFailureDetector {

    public static int intervalInMillis = 1000;
    private final ConcurrentHashMap<InetAddressAndPort, ArrivalWindow> arrivalSamples = new ConcurrentHashMap<>();
    protected static final long INITIAL_VALUE_NANOS = TimeUnit.NANOSECONDS.convert(intervalInMillis, TimeUnit.MILLISECONDS);
    private static final int SAMPLE_SIZE = 1000;
    private long lastInterpret = System.nanoTime();
    /**5 seconds*/
    private static final long DEFAULT_MAX_PAUSE = 5000L * 1000000L;
    private static final long MAX_LOCAL_PAUSE_IN_NANOS = DEFAULT_MAX_PAUSE;
    public static final FailureDetector instance = new FailureDetector();
    private long lastPause = 0L;
    /**0.434...*/
    private final double PHI_FACTOR = 1.0 / Math.log(10.0);
    public static volatile double PHI_CONVICT_THRESHOLD = 8.0;
    /**if the phi is larger than this percentage of the max, log a debug message*/
    private static final int DEBUG_PERCENTAGE = 80;

    @Override
    public boolean isAlive(InetAddressAndPort netInfo) {
        LegionNodeContext context = LegionNodeContext.context();
        LegionNodeInfo legionNode = context.getNodeInfoByNet(netInfo);
        if (legionNode == null)
            log.error("Unknown endpoint: " + netInfo, new IllegalArgumentException(""));
        return legionNode != null && legionNode.isAlive();
    }

    @Override
    public void interpret(InetAddressAndPort net) {
        ArrivalWindow hbWnd = arrivalSamples.get(net);
        if (hbWnd == null) {
            return;
        }
        long now = System.nanoTime();
        long diff = now - lastInterpret;
        lastInterpret = now;
        if (diff > MAX_LOCAL_PAUSE_IN_NANOS) {
            log.warn("Not marking nodes down due to local pause of {}ns > {}ns", diff, MAX_LOCAL_PAUSE_IN_NANOS);
            lastPause = now;
            return;
        }
        if (System.nanoTime() - lastPause < MAX_LOCAL_PAUSE_IN_NANOS) {
            log.debug("Still not marking nodes down due to local pause");
            return;
        }
        double phi = hbWnd.phi(now);
        if (log.isDebugEnabled())
            log.debug("PHI for {} : {}", net, phi);

        if (PHI_FACTOR * phi > PHI_CONVICT_THRESHOLD) {
            if (log.isTraceEnabled())
                log.trace("Node {} phi {} > {}; intervals: {} mean: {}ns", new Object[]{net, PHI_FACTOR * phi, PHI_CONVICT_THRESHOLD, hbWnd, hbWnd.mean()});
            LegionNodeInfo localLegionNode = LegionNodeContext.context().getNodeInfoByNet(net);
            if (localLegionNode == null) {
                log.error("node[{}] is not existed anymore, this is a repeat action, check code!", net);
                return;
            }
            //仅仅标记不存在，由self check 的expire逻辑处理去除
            EventBus.pushEvent(ClusterEvent.MAKE_PENDING, localLegionNode);
            log.warn("Node {} phi {} > {}; mean: {}ns is qualified for remove", net, PHI_FACTOR * phi, PHI_CONVICT_THRESHOLD, hbWnd.mean());
        } else if (log.isTraceEnabled() && (PHI_FACTOR * phi * DEBUG_PERCENTAGE / 100.0 > PHI_CONVICT_THRESHOLD)) {
            log.trace("PHI for {} : {}", net, phi);
        } else if (log.isTraceEnabled()) {
            log.trace("PHI for {} : {}", net, phi);
            log.trace("mean for {} : {}ns", net, hbWnd.mean());
        }
    }

    @Override
    public void report(InetAddressAndPort netInfo) {
        long now = System.nanoTime();
        ArrivalWindow heartbeatWindow = arrivalSamples.get(netInfo);
        if (heartbeatWindow == null) {
            // avoid adding an empty ArrivalWindow to the Map
            heartbeatWindow = new ArrivalWindow(SAMPLE_SIZE);
            heartbeatWindow.add(now, netInfo);
            heartbeatWindow = arrivalSamples.putIfAbsent(netInfo, heartbeatWindow);
            if (heartbeatWindow != null)
                heartbeatWindow.add(now, netInfo);
        } else {
            heartbeatWindow.add(now, netInfo);
        }

        if (log.isDebugEnabled() && heartbeatWindow != null)
            log.debug("Average for {} is {}ns", netInfo, heartbeatWindow.mean());
    }

    @Override
    public void remove(InetAddressAndPort netInfo) {
        arrivalSamples.remove(netInfo);
    }

    @Override
    public void forceConviction(InetAddressAndPort legionNode) {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<InetAddressAndPort> eps = arrivalSamples.keySet();

        sb.append("-----------------------------------------------------------------------");
        for (InetAddressAndPort ep : eps) {
            ArrivalWindow hWnd = arrivalSamples.get(ep);
            sb.append(ep).append(" : ");
            sb.append(hWnd);
            sb.append(System.getProperty("line.separator"));
        }
        sb.append("-----------------------------------------------------------------------");
        return sb.toString();
    }
}

@Slf4j
class ArrayBackedBoundedStats {
    private final long[] arrivalIntervals;
    private long sum = 0;
    private int index = 0;
    private boolean isFilled = false;
    private volatile double mean = 0;

    public ArrayBackedBoundedStats(final int size) {
        arrivalIntervals = new long[size];
    }

    public void add(long interval) {
        if (index == arrivalIntervals.length) {
            isFilled = true;
            index = 0;
        }

        if (isFilled)
            sum = sum - arrivalIntervals[index];

        arrivalIntervals[index++] = interval;
        sum += interval;
        mean = (double) sum / size();
    }

    private int size() {
        return isFilled ? arrivalIntervals.length : index;
    }

    public double mean() {
        return mean;
    }

    public long[] getArrivalIntervals() {
        return arrivalIntervals;
    }

}

@Slf4j
class ArrivalWindow {
    private long tLast = 0L;
    private final ArrayBackedBoundedStats arrivalIntervals;
    private double lastReportedPhi = Double.MIN_VALUE;

    // in the event of a long partition, never record an interval longer than the rpc timeout,
    // since if a host is regularly experiencing connectivity problems lasting this long we'd
    // rather mark it down quickly instead of adapting
    // this value defaults to the same initial value the FD is seeded with
    private final long MAX_INTERVAL_IN_NANO = FailureDetector.INITIAL_VALUE_NANOS;

    ArrivalWindow(int size) {
        arrivalIntervals = new ArrayBackedBoundedStats(size);
    }

    synchronized void add(long value, InetAddressAndPort ep) {
        assert tLast >= 0;
        if (tLast > 0L) {
            long interArrivalTime = (value - tLast);
            if (interArrivalTime <= MAX_INTERVAL_IN_NANO) {
                arrivalIntervals.add(interArrivalTime);
                log.trace("Reporting interval time of {}ns for {}", interArrivalTime, ep);
            } else {
                log.trace("Ignoring interval time of {}ns for {}", interArrivalTime, ep);
            }
        } else {
            // We use a very large initial interval since the "right" average depends on the cluster size
            // and it's better to err high (false negatives, which will be corrected by waiting a bit longer)
            // than low (false positives, which cause "flapping").
            arrivalIntervals.add(FailureDetector.INITIAL_VALUE_NANOS);
        }
        tLast = value;
    }

    double mean() {
        return arrivalIntervals.mean();
    }

    // see CASSANDRA-2597 for an explanation of the math at work here.
    double phi(long tnow) {
        assert arrivalIntervals.mean() > 0 && tLast > 0; // should not be called before any samples arrive
        long t = tnow - tLast;
        lastReportedPhi = t / mean();
        return lastReportedPhi;
    }

    double getLastReportedPhi() {
        return lastReportedPhi;
    }

    public String toString() {
        return Arrays.toString(arrivalIntervals.getArrivalIntervals());
    }
}
