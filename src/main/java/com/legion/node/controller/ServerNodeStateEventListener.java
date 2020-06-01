package com.legion.node.controller;

import com.legion.common.event.EventListener;
import com.legion.common.event.Subscriber;
import com.legion.gms.FailureDetector;
import com.legion.net.common.util.FunctionUtils;
import com.legion.net.entities.ApplicationState;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.entities.LegionNodeInfo;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Create At 2019/8/20
 *
 * @author Zing
 * @version 0.0.1
 */
@Slf4j
@EventListener
public class ServerNodeStateEventListener {

    /**
     * 当有新节点确认可以加入集群时触发
     *
     * @param legionNode 新的节点信息
     */
    @Subscriber(topic = "addNode")
    public void addNode(@NonNull LegionNodeInfo legionNode) {
        // 2. 将节点放入Metadata
        LegionNodeContext context = LegionNodeContext.context();
        try {
            log.info("node add >>> {}", legionNode);
            if (context.isSelfNode(legionNode.getNodeId())) {
                log.warn("node add skip self >>> {}", legionNode);
                return;
            }
            context.addLegionNode(legionNode);
            boolean runningOK = context.getChannelGroup().nodeResume(legionNode.getNetInfo());
            if (runningOK) {
                context.markNodeAlive(legionNode.getNodeId());
            } else {
                context.markNodePending(legionNode.getNodeId());
            }
            log.info("new node{} alive state is:{}", legionNode.getNetInfo(), legionNode.isAlive());
            context.appStateUpdateAsync(ApplicationState.NODE_ADD, FunctionUtils.LONG_INCREASE);
        } catch (Exception e) {
            log.error("add new node{} failed!", legionNode, e);
            context.markNodePending(legionNode.getNodeId());
        }
    }

    /**
     * gossip 发现节点不可达时
     *
     * @param legionNode legionNode
     */
    @Subscriber(topic = "makeNodePending")
    public void makeNodePending(@NonNull LegionNodeInfo legionNode) {
        try {
            LegionNodeContext context = LegionNodeContext.context();
            if (context.isSelfNode(legionNode.getNodeId())) {
                log.warn("node pending skip self >>> {}", legionNode);
                return;
            }
            LegionNodeInfo localLegionNode = context.getNodeInfoById(legionNode.getNodeId());
            if (localLegionNode == null) {
                log.error("make node {} pending failed due to nodeInfo no exist in context!", legionNode.getNetInfo());
                return;
            }
            if(legionNode.isAlive()) {
                context.markNodePending(legionNode);
                context.addExpireTimeForLegionNode(legionNode.getNetInfo());
                boolean pendingOk = context.getChannelGroup().nodePending(legionNode.getNetInfo());
                log.warn("make node {} pending ok is:{}", legionNode.getNetInfo(), pendingOk);
                context.appStateUpdateAsync(ApplicationState.NODE_PENDING, FunctionUtils.LONG_INCREASE);
            }else {
                log.info("make node {} pending repeat", legionNode.getNetInfo());
            }
        } catch (Exception e) {
            log.error("make node {} pending failed due to {}", legionNode.getNetInfo(), e);
        }

    }

    /**
     * gossip 发现节点不可达时
     *
     * @param legionNode legionNode
     */
    @Subscriber(topic = "makeNodeRunning")
    public void makeNodeRunning(@NonNull LegionNodeInfo legionNode) {
        try {
            LegionNodeContext context = LegionNodeContext.context();
            if (context.isSelfNode(legionNode.getNodeId())) {
                log.warn("node running skip self >>> {}", legionNode);
                return;
            }
            if (!context.containNodeInfo(legionNode.getNodeId()).isPresent()) {
                context.addLegionNode(legionNode);
            } else {
                context.containNodeInfo(legionNode.getNodeId()).ifPresent(n -> context.updateLegionNode(n));
            }
            boolean runningOK = context.getChannelGroup().nodeResume(legionNode.getNetInfo());
            context.markNodeAlive(legionNode.getNodeId());
            log.warn("make node {} running ok is:{}", legionNode.getNetInfo(), runningOK);
            context.appStateUpdateAsync(ApplicationState.NODE_RUNNING, FunctionUtils.LONG_INCREASE);
        } catch (Exception e) {
            log.error("make node {} running failed due to {}", legionNode.getNetInfo(), e);
        }
    }

    /**
     * 将一个节点移出集群
     *
     * @param legionNode nodeInfo
     */
    @Subscriber(topic = "deleteNode")
    public void deleteNode(@NonNull LegionNodeInfo legionNode) {
        try {
            LegionNodeContext context = LegionNodeContext.context();
            if (context.isSelfNode(legionNode.getNodeId())) {
                log.warn("node remove skip self >>> {}", legionNode);
                return;
            }

            context.removeLegionNode(legionNode);
            boolean removeOk = context.getChannelGroup().nodeRemove(legionNode.getNetInfo());
            FailureDetector.instance.remove(legionNode.getNetInfo());
            log.warn("make node {} remove result is:{}", legionNode.getNetInfo(), removeOk);
            context.appStateUpdateAsync(ApplicationState.NODE_REMOVE, FunctionUtils.LONG_INCREASE);
        } catch (Exception e) {
            log.error("remove node {} failed due to {}", legionNode.getNetInfo(), e);
        }
    }


}
