package com.legion.node.handler;

import com.legion.core.api.Gossip;
import com.legion.core.api.X;
import com.legion.core.utils.RequestURI;
import com.legion.net.entities.LegionChannelGroup;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.entities.LegionNodeInfo;
import com.legion.net.netty.transport.LegionCourier;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class SimpleSendMessageHandler extends AbstractMessageHandler {

    @Override
    public boolean preHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        //可处理的消息类型判断
        if (message.getHeader().getMsgType().equals(X.XMessageType.NORMAL) && !message.getIsReply()) {
            return true;
        }

        return !message.getIsReply() && message.getHeader().getTagType().equals(X.XTagType.PROCESS) && message.getHeader().hasNodeSource();
    }

    @Override
    public boolean doHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        RequestURI request = RequestURI.create(message.getHeader().getUri());
        String groupId = request.getGroupId();
        return routeMessage(message, groupId, message.getHeader().getRouteVersion());
    }

    private boolean routeMessage(X.XMessage message, String groupId, String routeVersion) {
        LegionNodeContext context = LegionNodeContext.context();
        LegionChannelGroup channelGroup = context.getChannelGroup();
        LegionNodeInfo selfInfo = context.getSelfInfo();
        List<Gossip.ModuleInfo> matchModules = context.getSelfInfo().getVersionMatchedModule(groupId,routeVersion);

        //修复module无法负载的问题
        List<Object> sendTarget = channelGroup.getOneRowSelected(groupId, matchModules).stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (!message.getHeader().hasNodeSource()) {
            //修复legion转发时, 排除自己
            //修复重复跳转的问题，有NodeSource证明是Forward过来的，本节点一定有对应的Module的连接，如果没有让客户端重试
            List<LegionNodeInfo> availableNode = context.getClusterNodes()
                    .values()
                    .stream()
                    .filter(legionNodeInfo -> legionNodeInfo.getNetInfo().compareTo(selfInfo.getNetInfo()) != 0)
                    .filter(LegionNodeInfo::isAlive)
                    .filter(n -> n.matchGroupVersion(groupId, routeVersion))
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
