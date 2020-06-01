package com.legion.node.handler;

import com.legion.core.api.X;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.entities.LegionNodeInfo;
import com.legion.net.netty.transport.LegionCourier;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SimpleRplMessageHandler extends AbstractMessageHandler {

    @Override
    public boolean preHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        //可处理的消息类型判断
        if (message.getHeader().getMsgType().equals(X.XMessageType.NORMAL) && message.getIsReply() && !message.getHeader().getTagType().equals(X.XTagType.PROCESS)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean doHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        String groupId = message.getHeader().getModuleSource().getGroupId();
        String moduleId = message.getHeader().getModuleSource().getModuleId();
        LegionNodeContext context = LegionNodeContext.context();
        LegionNodeInfo selfInfo = context.getSelfInfo();
        //当前Node中尚未注册该(Group), Node需要把Message消息转发给其他Node
        if (!selfInfo.matchGroupModule(groupId, moduleId)) {
            List<LegionNodeInfo> availableNode = context.getClusterNodes()
                    .values()
                    .stream()
                    .filter(LegionNodeInfo::isAlive)
                    .filter(n -> n.matchGroupModule(groupId, moduleId))
                    .collect(Collectors.toList());
            for (LegionNodeInfo info : availableNode) {
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
        } else {
            //新加流程适配module的负载状况 fix bugs spring20190903
            Channel channel = context.getChannelGroup().getModuleChannel(groupId, moduleId);
            if (channel != null) {
                LegionCourier.instance().sendModule(message, message.getHeader().getMsgType(), channel);
                return false;
            }
        }
        LegionCourier.instance().sendModule(message, message.getHeader().getMsgType());
        return true;
    }
}
