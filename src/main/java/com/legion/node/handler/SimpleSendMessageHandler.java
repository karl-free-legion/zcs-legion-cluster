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
import java.util.concurrent.atomic.AtomicBoolean;
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

    private boolean routeMessage(X.XMessage message, String groupId, final String routeVersion) {
        LegionNodeContext context = LegionNodeContext.context();
        LegionChannelGroup channelGroup = context.getChannelGroup();
        LegionNodeInfo selfInfo = context.getSelfInfo();
        List<Gossip.ModuleInfo> matchModules = context.getSelfInfo().getVersionMatchedModule(groupId,routeVersion);

        //随机方式送到目标
        AtomicBoolean sendStatus = new AtomicBoolean(false);
        List<Channel> sendTarget = channelGroup.getOneRowSelected(groupId, matchModules).stream().filter(Objects::nonNull).collect(Collectors.toList());
        Collections.shuffle(sendTarget);
        for (Channel channel : sendTarget) {
            try {
                LegionCourier.instance().sendModule(message, message.getHeader().getMsgType(), channel);
            }catch (Exception e) {
                log.warn("send message[from={},gid={},v={}] to Module[{},{}] failed", message.getHeader().getModuleSource(), groupId, routeVersion, channel.isActive(),channel.isWritable());
            }
            sendStatus.set(true);
            if(sendStatus.get()) {
                break;
            }
        }
        //本节点无直达路径，转发
        if (!message.getHeader().hasNodeSource() && !sendStatus.get()) {
            //修复legion转发时, 排除自己
            //修复重复跳转的问题，有NodeSource证明是Forward过来的，本节点一定有对应的Module的连接，如果没有让客户端重试
            List<LegionNodeInfo> forwardTarget = context.getClusterNodes()
                    .values()
                    .stream()
                    .filter(legionNodeInfo -> legionNodeInfo.getNetInfo().compareTo(selfInfo.getNetInfo()) != 0)
                    .filter(LegionNodeInfo::isAlive)
                    .filter(n -> n.matchGroupVersion(groupId, routeVersion))
                    .collect(Collectors.toList());
            for (LegionNodeInfo node : forwardTarget) {
                try {
                    LegionCourier.instance().sendNodeForward(message, message.getHeader().getMsgType(), node.getNetInfo());
                    if (log.isDebugEnabled()) {
                        log.debug("forward message[from={},gid={},v={}] to Node[{}]", message.getHeader().getModuleSource(), groupId, routeVersion, node.getNetInfo());
                    }
                }catch (Exception e) {
                    log.warn("forward message[from={},gid={},v={}] to Node[{}] Failed", message.getHeader().getModuleSource(), groupId, routeVersion, node.getNetInfo(),e);
                }
                sendStatus.set(true);
                if(sendStatus.get()) {
                    break;
                }
            }
        }
        //暂无路由，进入重试等待队列
        if(!sendStatus.get()) {
            LegionCourier.instance().sendModule(message, message.getHeader().getMsgType());
            log.warn("send message[from={},isForward={},gid={},v={}] failed", message.getHeader().getModuleSource(), message.getHeader().hasNodeSource(), groupId, routeVersion);
        }
        return sendStatus.get();
    }
}
