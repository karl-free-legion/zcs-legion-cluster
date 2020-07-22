package com.legion.node.handler;

import com.legion.core.api.X;
import com.legion.net.common.exceptions.LegionNetException;
import com.legion.net.entities.LegionChannelGroup;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.entities.LegionNodeInfo;
import com.legion.net.netty.server.message.NodeMessageProcess;
import com.legion.net.netty.transport.LegionCourier;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractMessageHandler implements NodeMessageProcess {

    final MessageFunnel messageFunnel = new MessageFunnel(this::finalHandler);

    @Override
    public boolean process(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        return messageFunnel.accept(message, channelHandlerContext);
    }

    @Override
    public abstract boolean preHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext);


    public abstract boolean doHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext);


    public boolean finalHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        if (preHandler(message, channelHandlerContext)) {
            return doHandler(message, channelHandlerContext);
        }
        return false;
    }

    /**
     * 流程回复消息的Forward
     *
     * @param message
     * @param nodeId
     */
    void forwardMessage(X.XMessage message, String nodeId) {
        LegionNodeContext context = LegionNodeContext.context();
        LegionNodeInfo available = context.getNodeInfoById(nodeId);
        if (available == null) {
            log.error("forward process message[header={}] to Node not existed!", message.getHeader());
            throw new LegionNetException("-404", "Node Forward Process message not found Node" + nodeId);
        }
        try {
            X.XMessage wrongMessage = message.toBuilder().setIsReply(true).setHeader(message.getHeader().toBuilder().clearNodeSource()).build();
            LegionCourier.instance().sendNodeForwardProcess(wrongMessage, wrongMessage.getHeader().getMsgType(), available.getNetInfo());

            if (log.isInfoEnabled()) {
                log.info("forward process message[{}] to Node[{}]", wrongMessage, available.getNetInfo());
            }
        } catch (Exception ex) {
            log.warn("forward process message[header={}] to Node[{}] Failed!", message.getHeader(), available.getNetInfo());
        }
    }

    public boolean send(X.XMessage message, String groupId) {
        LegionNodeContext context = LegionNodeContext.context();
        LegionChannelGroup channelGroup = context.getChannelGroup();
        LegionNodeInfo selfInfo = context.getSelfInfo();

        //修复module无法负载的问题
        List<Object> sendTarget = new ArrayList<>();
        List<Channel> availableChannel = channelGroup.getOneRow(groupId).stream().filter(channel -> channel != null).collect(Collectors.toList());
        sendTarget.addAll(availableChannel);
        if (!message.getHeader().hasNodeSource()) {
            //修复legion转发时, 排除自己
            //修复重复跳转的问题，有NodeSource证明是Forward过来的，本节点一定有对应的Module的连接，如果没有让客户端重试
            List<LegionNodeInfo> availableNode = context.getClusterNodes()
                    .values()
                    .stream()
                    .filter(legionNodeInfo -> legionNodeInfo.getNetInfo().compareTo(selfInfo.getNetInfo()) != 0)
                    .filter(LegionNodeInfo::isAlive)
                    .filter(n -> n.matchGroup(groupId))
                    .collect(Collectors.toList());
            sendTarget.addAll(availableNode);
        }
        if (sendTarget.size() > 0) {
            Collections.shuffle(sendTarget);
            for (Object target : sendTarget) {
                if (target instanceof Channel) {
                    Channel channel = (Channel) target;
                    LegionCourier.instance().sendModule(message, message.getHeader().getMsgType(), channel);
                    return true;
                } else if (target instanceof LegionNodeInfo) {
                    LegionNodeInfo info = (LegionNodeInfo) target;
                    try {
                        LegionCourier.instance().sendNodeForward(message, message.getHeader().getMsgType(), info.getNetInfo());
                        if (log.isDebugEnabled()) {
                            log.debug("forward message[from={},gid={}] to Node[{}]", message.getHeader().getModuleSource(), groupId, info.getNetInfo());
                        }
                        return true;
                    } catch (Exception ex) {
                        log.warn("forward message[from={},gid={}] to Node[{}] Failed!", message.getHeader().getModuleSource(), groupId, info.getNetInfo());
                    }
                }
            }
        }

        LegionCourier.instance().sendModule(message, message.getHeader().getMsgType());
        return true;
    }
}
