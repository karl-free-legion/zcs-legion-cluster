package com.legion.gms.handler;


import com.legion.common.event.EventBus;
import com.legion.core.api.Gossip;
import com.legion.core.api.X;
import com.legion.gms.GossipController;
import com.legion.net.common.exceptions.LegionNetException;
import com.legion.net.common.util.FunctionUtils;
import com.legion.net.entities.ApplicationState;
import com.legion.net.entities.InetAddressAndPort;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.entities.LegionNodeInfo;
import com.legion.net.netty.transport.LegionCourier;
import com.legion.node.controller.ClusterEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("gossip_init_handler")
public class GossipInitHandler extends GossipHandler<Gossip.GossipInit> {

    @Autowired
    private GossipController gossipController;

    @Override
    protected void preHandle(Gossip.GossipInit stageData) {

    }

    @Override
    protected void handleSuccess(Gossip.GossipInit gossipInit) {
//        stageData.getSelfNodeListList().forEach();

    }

    @Override
    protected void handleFail(LegionNetException e) {

    }

    @Override
    protected void onHandle(Gossip.GossipInit gossipInit, String fromNodeId, InetAddressAndPort fromNodeNet) {

        LegionNodeContext legionContext = LegionNodeContext.context();
        Gossip.GossipDiff.Builder gossipDiff = Gossip.GossipDiff.newBuilder();
        List<Gossip.NodeInfo> hadNodeList = new ArrayList<>();
        List<Gossip.NodeInfoLight> noneNodeList = new ArrayList<>();

        //comparing
        if (gossipInit.getSelfNodeListList().size() == 0) {
            legionContext.getClusterNodes()
                    .values().stream().filter(LegionNodeInfo::isAlive)
                    .forEach(myNode -> gossipInit.getSelfNodeListList().add(myNode.toGossipNodeInfoLight()));
        }
        for (Gossip.NodeInfoLight remoteNode : gossipInit.getSelfNodeListList()) {
            LegionNodeInfo localNode = legionContext.getNodeInfoById(remoteNode.getNodeId());
            if (localNode != null) {
                long remoteGen = remoteNode.getGeneration();
                int maxRemoteVersion = remoteNode.getMaxVersion();

                long localGen = localNode.getHbState().getGeneration();
                int maxLocalVersion = localNode.getMaxVersion();

                if (localGen == remoteGen && maxRemoteVersion == maxLocalVersion) {
                    continue;
                }
                if (remoteGen > localGen) {
                    pullFromRemote(noneNodeList, remoteNode);
                } else if (remoteGen < localGen) {
                    push2Remote(hadNodeList, localNode, 0);
                } else {
                    if (maxRemoteVersion > maxLocalVersion) {
                        pullFromRemote(noneNodeList, remoteNode);
                    } else if (maxRemoteVersion < maxLocalVersion) {
                        push2Remote(hadNodeList, localNode, maxRemoteVersion);
                    }
                }
            } else {
                pullFromRemote(noneNodeList, remoteNode);
            }
        }
        gossipDiff.addAllHadNodeList(hadNodeList);
        gossipDiff.addAllNoneNodeList(noneNodeList);

        LegionNodeContext.context().appStateUpdateAsync(ApplicationState.GOS_MESSAGE_SENT, FunctionUtils.LONG_INCREASE);
        //发送信息
        LegionCourier.instance()
                .sendNode(gossipDiff.build(), X.XMessageType.GOSSIP_DIFF, fromNodeNet)
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        LegionNodeInfo nodeInfo = legionContext.getNodeInfoByNet(fromNodeNet);
                        if(nodeInfo != null)
                            EventBus.getInstance().pushEvent(ClusterEvent.MAKE_PENDING, nodeInfo);
                        log.warn("send GOSSIP_DIFF to legion node {} failed due to unreachable, mark it pending", fromNodeNet);
                    } else if (log.isTraceEnabled()) {
                        log.trace("send GOSSIP_DIFF to legion node {} success", fromNodeNet);
                    }
                });
        // .whenComplete(
        //         (failNodeId, e) -> {
        //             if (e != null) {
        //                 LegionNodeInfo nodeInfo = legionContext.getNodeInfoById(failNodeId);
        //                 EventBus.getInstance().pushEvent(ClusterEvent.MAKE_PENDING, nodeInfo);
        //                 log.warn("send GOSSIP_DIFF to legion node {} failed due to unreachable, mark it pending", fromNodeNet);
        //             } else if (log.isDebugEnabled()) {
        //                 log.debug("send GOSSIP_DIFF to legion node {} success", fromNodeNet);
        //             }
        //         });
    }

    private void pullFromRemote(List<Gossip.NodeInfoLight> noneNodeList, Gossip.NodeInfoLight remoteNode) {
        noneNodeList.add(Gossip.NodeInfoLight.newBuilder()
                .setAddress(remoteNode.getAddress())
                .setGeneration(remoteNode.getGeneration())
                .setMaxVersion(0)
                .setNodeId(remoteNode.getNodeId())
                .build());
        if (log.isTraceEnabled()) {
            log.trace("pull Node  {}", remoteNode.toString());
        }
    }

    private void push2Remote(List<Gossip.NodeInfo> hadNodeList, LegionNodeInfo localNode, int maxRemoteVersion) {
        LegionNodeInfo pushNode = gossipController.getStateForVersionBiggerThan(localNode, maxRemoteVersion);
        if (pushNode != null) {
            hadNodeList.add(localNode.toGossipNodeInfo());
            if (log.isTraceEnabled()) {
                log.trace("push Node {}", localNode.toString());
            }
        }
    }
}
