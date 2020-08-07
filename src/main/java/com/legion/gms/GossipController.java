package com.legion.gms;

import com.legion.common.event.EventBus;
import com.legion.core.api.Gossip;
import com.legion.core.api.X;
import com.legion.net.common.LegionConstants;
import com.legion.net.common.config.LegionProperties;
import com.legion.net.common.exceptions.LegionNetException;
import com.legion.net.common.util.FunctionUtils;
import com.legion.net.entities.*;
import com.legion.net.netty.transport.LegionCourier;
import com.legion.net.netty.transport.Transporter;
import com.legion.node.controller.ClusterEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Gossip逻辑控制器
 * 封装通讯中的处理逻辑
 * 管理流程全局状态
 */
@Component
@Slf4j
public class GossipController {
    @Autowired
    private LegionProperties properties;

    private final Map<InetAddressAndPort, Long> justRemovedLegionNode = new ConcurrentHashMap<>();

    private static final ReentrantLock taskLock = new ReentrantLock();
    private final Random random = new Random();

    private static final String GOSSIP_THREAD_NAME = "gossip_stage";

    /**
     * Maximimum difference between generation value and local time we are willing to accept about a peer
     */
    private static final long MAX_GENERATION_DIFFERENCE = 86400 * 365 * 1000 * 1000 * 1000;

    private volatile long firstSynSendAt = 0L;


    /**
     * Remove node re join delay(ms)
     */
    private final static int REJOIN_DELAY = 2 * 60 * 1000;


    private ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(1);

    /**
     * Gossip Task 运行间隔(s)
     */
    private final static int GOSSIP_INIT_INTERVAL = 1;

    /**
     * Gossip init 消息发送节点数量
     */
    private static final int GOSSIP_INIT_RANG = 1;

    @PostConstruct
    void GossipControllerPost() {
        scheduled.scheduleWithFixedDelay(new GossipTask(), 3, GOSSIP_INIT_INTERVAL, TimeUnit.SECONDS);
        firstSynSendAt = System.nanoTime();
        if (log.isDebugEnabled()) {
            log.debug("GossipController init complete");
        }
        //设置FailureDetector配置(FailureDetector直接做Bean, 通过引用properties配置), 待修改
        FailureDetector.intervalInMillis = properties.getServer().getDetector().getIntervalInMillis();
        FailureDetector.PHI_CONVICT_THRESHOLD = properties.getServer().getDetector().getPhiConvictThreshold();
    }

    /**
     * @param localNode localNode
     * @param version   version
     * @return LegionNodeInfo
     */
    public LegionNodeInfo getStateForVersionBiggerThan(LegionNodeInfo localNode, int version) {
        LegionNodeInfo pullNode = null;

        if (localNode != null && localNode.isAlive()) {
            HeartBeatState heartBeatState = localNode.getHbState();
            int localHbVersion = heartBeatState.getHeartBeatVersion();
            if (localHbVersion > version) {
                pullNode = localNode;
                if (log.isDebugEnabled())
                    log.debug("local heartbeat version {} greater than {} for {}", localHbVersion, version, localNode);
            }
        }
        return pullNode;
    }


    public void notifyFailureDetector(List<Gossip.NodeInfo> nodeInfos) {
        for (Gossip.NodeInfo nodeInfo : nodeInfos) {
            notifyFailureDetector(nodeInfo);
        }
    }

    private void notifyFailureDetector(Gossip.NodeInfo remoteNodePB) {
        LegionNodeContext context = LegionNodeContext.context();
        LegionNodeInfo localNode = context.getNodeInfoById(remoteNodePB.getNodeId());
        if (localNode != null) {
            long localGen = localNode.getHbState().getGeneration();
            long remoteGen = remoteNodePB.getGeneration();
            IFailureDetector fd = FailureDetector.instance;
            if (remoteGen > localGen) {
                localNode.updateTimestamp();
                if (!localNode.isAlive()) {
                    if (log.isDebugEnabled())
                        log.debug("Clearing interval times for {}:{} due to generation change", localNode.getHost(), localNode.getPort());
                    fd.remove(localNode.getNetInfo());
                }
                fd.report(localNode.getNetInfo());
                return;
            }
            if (remoteGen == localGen) {
                int localVersion = getMaxLegionNodeStateVersion(localNode);
                int remoteVersion = remoteNodePB.getMaxVersion();
                if (remoteVersion > localVersion) {
                    localNode.updateTimestamp();
                    fd.report(localNode.getNetInfo());
                }

            }
        }
    }

    /**
     * 根据Gossip消息同步本地节点
     *
     * @param nodeIfs
     * @throws LegionNetException
     */
    public void applyStateLocally(List<Gossip.NodeInfo> nodeIfs) throws LegionNetException {
        LegionNodeContext context = LegionNodeContext.context();
        for (Gossip.NodeInfo remoteNodePB : nodeIfs) {
            try {
                InetAddressAndPort remoteNetInfo = InetAddressAndPort.getByNameAndPort(remoteNodePB.getAddress(), remoteNodePB.getPort());
                if (justRemovedLegionNode.containsKey(remoteNetInfo)) {
                    if (log.isDebugEnabled())
                        log.debug("Ignoring gossip for Node[{}] because it is quarantined", remoteNodePB.getAddress());
                    continue;
                }
                LegionNodeInfo localNode = context.getNodeInfoById(remoteNodePB.getNodeId());
                if (localNode != null) {
                    long localGeneration = localNode.getHbState().getGeneration();
                    long remoteGeneration = remoteNodePB.getGeneration();
                    long localTime = System.nanoTime();
                    if (log.isDebugEnabled())
                        log.debug("{} local generation {}, remote generation {}", localNode.getNetInfo(), localGeneration, remoteGeneration);

                    if (remoteGeneration > localTime + MAX_GENERATION_DIFFERENCE) {
                        log.warn("received an invalid gossip generation for peer {}; local time = {}, received generation = {}", localNode.getNetInfo(), localTime, remoteGeneration);
                    } else if (remoteGeneration > localGeneration) {
                        if (log.isDebugEnabled())
                            log.debug("Updating heartbeat state generation to {} from {} for {}", remoteGeneration, localGeneration, localNode.getNetInfo());
                        handleMajorStateChange(remoteNodePB);
                    } else if (remoteGeneration == localGeneration) {// generation has not changed, apply new states
                        int remoteMaxVersion = remoteNodePB.getMaxVersion();
                        int localMaxVersion = getMaxLegionNodeStateVersion(localNode);

                        if (remoteMaxVersion > localMaxVersion) {
                            applyNewStates(localNode, remoteNodePB);
                        } else if (log.isDebugEnabled())
                            log.debug("Ignoring remote version {} <= {} for {}", remoteMaxVersion, localMaxVersion, localNode.getNetInfo());
                    } else if (log.isDebugEnabled()) {
                        log.debug("Ignoring remote generation {} < {}", remoteGeneration, localGeneration);
                    }
                } else {
                    FailureDetector.instance.report(remoteNetInfo);
                    handleMajorStateChange(remoteNodePB);
                }
            } catch (Exception e) {
                String errMsg = String.format("apply remote node %s to local failed due to %s", remoteNodePB.toString(), e);
                log.error(errMsg);
                throw new LegionNetException(errMsg, e);
            }
        }
    }

    /**
     * 根据外部状态重置本地状态
     *
     * @param localNode
     * @param remoteNodePB
     */
    private void applyNewStates(LegionNodeInfo localNode, Gossip.NodeInfo remoteNodePB) {
        int oldVersion = localNode.getHbState().getHeartBeatVersion();
        localNode.setHbState(new HeartBeatState(remoteNodePB.getGeneration(), remoteNodePB.getMaxVersion()));
        if (log.isDebugEnabled())
            log.debug("Updating heartbeat state version to {} from {} for {} ...", localNode.getHbState().getHeartBeatVersion(), oldVersion, localNode.getNetInfo());

        localNode.setSeedNum(remoteNodePB.getSeedNum());
        Map<String, Long> remoteServiceStates = remoteNodePB.getServiceStateMapMap();
        Map<String, Gossip.ModuleInfo> remoteModuleGroupStates = remoteNodePB.getLegionModuleGroupStateMap();
        localNode.updateServiceStates(remoteServiceStates.entrySet());
        localNode.updateModuleGroupStates(remoteModuleGroupStates);
    }

    private int getMaxLegionNodeStateVersion(LegionNodeInfo legionNode) {
        int maxVersion = legionNode.getHbState().getHeartBeatVersion();
        return maxVersion;
    }

    private void handleMajorStateChange(Gossip.NodeInfo remoteNode) {
        LegionNodeContext context = LegionNodeContext.context();
        LegionNodeInfo localNode = context.getNodeInfoById(remoteNode.getNodeId());
        if (localNode != null) {
            localNode.setHbState(new HeartBeatState(remoteNode.getGeneration(), remoteNode.getMaxVersion()));
            localNode.setSeedNum(remoteNode.getSeedNum());
            localNode.updateModuleGroupStates(remoteNode.getLegionModuleGroupStateMap());
            localNode.updateServiceStates(remoteNode.getServiceStateMapMap().entrySet());
            if (!localNode.isAlive())
                EventBus.getInstance().pushEvent(ClusterEvent.MAKE_RUNNING, localNode);
        } else {
            try {
                LegionNodeInfo newNode = LegionNodeInfo.fromGossipNodeInfo(remoteNode);
                if (justRemovedLegionNode.containsKey(newNode.getNetInfo())) {
                    log.warn("Node[{}] is just removed, skip add node.", newNode.getNetInfo());
                    return;
                }
                EventBus.getInstance().pushEvent(ClusterEvent.ADD_NODE, newNode);
            } catch (Exception e) {
                log.error("add node [{}] failed due to {}", remoteNode.toString(), e);
            }
        }
    }

    private class GossipTask implements Runnable {
        public void run() {
            try {
                Transporter.context().getClosing().compareAndSet(true,false);
                taskLock.lock();
                Thread.currentThread().setName(GOSSIP_THREAD_NAME);
                LegionNodeContext context = LegionNodeContext.context();

                //每次更新自己的HB version
                context.getSelfInfo().getHbState().updateHeartBeat();
                context.syncSelf2Cluster();
                if (log.isDebugEnabled())
                    log.debug("My heartbeat version is now {}", context.getNodeInfoById(context.getSelfInfo().getNodeId()).getHbState().getHeartBeatVersion());
                sendGossipInit(context);
                //当前节点已与所有节点断链，取配置中的初始节点重新尝试连接
                if (!context.getClusterNodes().
                        entrySet().stream().filter(n -> n.getValue().isAlive()).anyMatch(n -> !n.getKey().equals(context.getSelfInfo().getNodeId()))) {
                    LegionProperties legionProperties = context.readNetProperties();
                    legionProperties.getCluster().forEach(defaultNet -> {
                        try {
                            EventBus.getInstance().pushEvent(ClusterEvent.ADD_NODE, new LegionNodeInfo(defaultNet.getHost(), defaultNet.getPort()));
                            log.info("retry node[{},{}] fire ADD_NODE event", defaultNet.getHost(), defaultNet.getPort());
                        } catch (UnknownHostException e) {
                            log.error("retry node[{},{}] failed!", defaultNet.getHost(), defaultNet.getPort(), e);
                        }
                    });
                }
                //巡检
                doStatusCheck();
                if (log.isDebugEnabled()) {
//                    log.debug("the Local Context is {}", context.toString());
                }
            } catch (Exception e) {
                log.error("GossipTask run error", e);
            } finally {
                taskLock.unlock();
            }
        }
    }

    private void sendGossipInit(LegionNodeContext context) {
        //发送gossip init
        final List<Gossip.NodeInfoLight> initNodeList =
                context.getClusterNodes().values().stream()
                        .map(LegionNodeInfo::toGossipNodeInfoLight).collect(Collectors.toList());
        final List<LegionNodeInfo> availablePeers = context.getClusterNodes()
                .values().stream()
                .filter(p -> !context.isSelfNode(p.getNetInfo()))
                .collect(Collectors.toList());

        if (initNodeList.size() > 0 && availablePeers.size() > 0) {
            Gossip.GossipInit.Builder gossipInit = Gossip.GossipInit.newBuilder();
            gossipInit.addAllSelfNodeList(initNodeList);
            IntStream.range(0, GOSSIP_INIT_RANG).parallel().forEach(i -> {
                int index = (availablePeers.size() == 1) ? 0 : random.nextInt(availablePeers.size());
                LegionNodeInfo peer = availablePeers.get(index);
                LegionNodeContext.context().appStateUpdateAsync(ApplicationState.GOS_MESSAGE_SENT, FunctionUtils.LONG_INCREASE);

                LegionCourier.instance().sendNode(gossipInit.build(), X.XMessageType.GOSSIP_INIT, peer.getNetInfo())
                        .addListener(future -> {
                            if (future.isSuccess()) {
                                future.get();
                                if (log.isDebugEnabled())
                                    log.debug("send GOSSIP_INIT to legion node {} success", peer.getNetInfo());
                            } else {
                                EventBus.pushEvent(ClusterEvent.MAKE_PENDING, peer);
//                                            EventBus.getInstance().pushEvent(ClusterEvent.MAKE_REMOVE, nodeInfo);//FailureDetector不可用时的强制方法
                                log.warn("send GOSSIP_INIT to legion node {} failed due to unreachable, mark it pending", peer.getNetInfo());
                            }
                        });
            });
        } else if (log.isDebugEnabled()) {
            log.debug("Local cluster info size{} too small to send GossipInit", context.getClusterNodes().size());
        }
    }

    private void doStatusCheck() {
        if (log.isDebugEnabled())
            log.debug("Performing status check ...");

        long now = System.currentTimeMillis();

        LegionNodeContext context = LegionNodeContext.context();

        for (LegionNodeInfo legionNode : context.getClusterNodes().values()) {
            if (legionNode != null && !context.isSelfNode(legionNode.getNodeId())) {
                FailureDetector.instance.interpret(legionNode.getNetInfo());
                // check for dead state removal
                long expireTime = context.getExpireTimeForLegionNode(legionNode.getNetInfo());
                if (!legionNode.isAlive() && (now > expireTime)) {
                    if (log.isDebugEnabled()) {
                        log.debug("time is expiring for LegionNode : {} ({})", legionNode, expireTime);
                    }
                    //Fire event to remove Node
                    EventBus.pushEvent(ClusterEvent.MAKE_REMOVE, legionNode);
                    justRemovedLegionNode.put(legionNode.getNetInfo(), System.currentTimeMillis());
                    context.removeExpireTimeForLegionNode(legionNode.getNetInfo());
                }
            }
        }

        if (!justRemovedLegionNode.isEmpty()) {
            for (Map.Entry<InetAddressAndPort, Long> entry : justRemovedLegionNode.entrySet()) {
                if ((now - entry.getValue()) > REJOIN_DELAY) {
                    log.info("{} elapsed, {} node{} can rejoin over", REJOIN_DELAY, entry.getKey());
                    justRemovedLegionNode.remove(entry.getKey());
                }
            }
        }

        //Module hb validation check
        if (context.getSelfInfo().getModuleGroupStates() != null && context.getSelfInfo().getModuleGroupStates().size() > 0) {
            List<String> removeList = context.getSelfInfo().getModuleGroupStates().entrySet().stream()
                    .filter(e -> (now - e.getValue().getState()) > LegionConstants.MODULE_GROUP_TIMEOUT_MS)
                    .peek(e -> {
                        log.warn("remove group id[{}], dmlTime[{}]",
                                e.getKey(), DateFormatUtils.format(e.getValue().getState(), "yyyy-MM-dd HH:mm:ss"));
                    })
                    .map(e -> e.getKey())
                    .collect(Collectors.toList());
            if (removeList.size() > 0) context.moduleGroupRemove(removeList);
        }
    }

}
