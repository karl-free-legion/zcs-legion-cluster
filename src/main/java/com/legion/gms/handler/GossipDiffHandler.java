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

@Component("gossip_diff_handler")
@Slf4j
public class GossipDiffHandler extends GossipHandler<Gossip.GossipDiff> {

    @Autowired
    private GossipController gossipController;

    @Override
    protected void preHandle(Gossip.GossipDiff stageData) {

    }

    @Override
    protected void handleSuccess(Gossip.GossipDiff stageData) {

    }

    @Override
    protected void handleFail(LegionNetException e) {

    }

    @Override
    protected void onHandle(Gossip.GossipDiff gossipDiff, String fromNodeId, InetAddressAndPort fromNodeNet) throws LegionNetException {

        List<Gossip.NodeInfo> hadNodeList = gossipDiff.getHadNodeListList();
        List<Gossip.NodeInfoLight> noneNodeList = gossipDiff.getNoneNodeListList();

        if (hadNodeList.size() > 0) {
            /* Notify the Failure Detector */
            gossipController.notifyFailureDetector(hadNodeList);
            gossipController.applyStateLocally(hadNodeList);
        }

        LegionNodeContext legionContext = LegionNodeContext.context();
        if (noneNodeList != null && noneNodeList.size() > 0) {
            Gossip.GossipConfirm.Builder gossipConfirm = Gossip.GossipConfirm.newBuilder();
            List<Gossip.NodeInfo> confirmNodeList = new ArrayList<>();
            for (Gossip.NodeInfoLight missingNode : noneNodeList) {
                LegionNodeInfo localNodeRaw = legionContext.getNodeInfoById(missingNode.getNodeId());
                if (localNodeRaw == null) {
                    continue;
                }
                LegionNodeInfo localNode = gossipController.getStateForVersionBiggerThan(localNodeRaw, missingNode.getMaxVersion());
                if (localNode != null && localNode.isAlive()) {
                    confirmNodeList.add(localNode.toGossipNodeInfo());
                }
            }
            gossipConfirm.addAllConfirmNodeList(confirmNodeList);
            LegionNodeContext.context().appStateUpdateAsync(ApplicationState.GOS_MESSAGE_SENT, FunctionUtils.LONG_INCREASE);

            //send message
            LegionCourier.instance()
                    .sendNode(gossipConfirm.build(), X.XMessageType.GOSSIP_CONFIRM, fromNodeNet)
                    .addListener(future -> {
                        if (!future.isSuccess()) {
                            LegionNodeInfo nodeInfo = legionContext.getNodeInfoById(future.get().toString());
                            EventBus.getInstance().pushEvent(ClusterEvent.MAKE_PENDING, nodeInfo);
                            log.warn("send GOSSIP_CONFIRM to legion node {} failed due to unreachable, mark it pending", fromNodeNet);
                        } else if (log.isTraceEnabled()) {
                                log.trace("send GOSSIP_CONFIRM to legion node {} success", fromNodeNet);
                        }
                    });
            //
            // .whenComplete(
            // (failNodeId, e) -> {
            //     if (e != null) {
            //         LegionNodeInfo nodeInfo = legionContext.getNodeInfoById(failNodeId);
            //         EventBus.getInstance().pushEvent(ClusterEvent.MAKE_PENDING, nodeInfo);
            //         log.warn("send GOSSIP_CONFIRM to legion node {} failed due to unreachable, mark it pending", fromNodeNet);
            //     } else if (log.isDebugEnabled()) {
            //         log.debug("send GOSSIP_CONFIRM to legion node {} success", fromNodeNet);
            //     }
            // });
        }
    }
}
