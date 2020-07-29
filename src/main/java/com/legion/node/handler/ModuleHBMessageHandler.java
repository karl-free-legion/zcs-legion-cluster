package com.legion.node.handler;

import com.legion.core.api.X;
import com.legion.net.netty.transport.LegionCourier;
import com.legion.net.netty.transport.SentResultEntity;
import com.legion.node.server.ModuleHeartbeatHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;


/**
 * Module heart beat message handler
 */
@Slf4j
public class ModuleHBMessageHandler extends AbstractMessageHandler {

    private ModuleHeartbeatHandler moduleHeartbeatHandler = new ModuleHeartbeatHandler();

    @Override
    public boolean preHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        return message.getHeader().getMsgType().equals(X.XMessageType.HEARTBEAT);
    }

    @Override
    public boolean doHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        boolean result = false;
        try {
            handleHeartBeat(message, channelHandlerContext.channel());
            result = true;
        } catch (Throwable e) {
            log.error("module hb message handler failed. {}", message.getHeader(), e);
        }
        return result;
    }

    /**
     * module注册legion后, 需要处理业务
     *
     * @param message XMessage
     * @param channel channel客户注册上来
     */
    private void handleHeartBeat(X.XMessage message, Channel channel) {
        X.XMessage reply = moduleHeartbeatHandler.handleHeartbeat(message, channel);
        Promise<SentResultEntity> result = LegionCourier.instance().sendModule(reply, X.XMessageType.HEARTBEAT, channel);
        result.addListener(r -> {
            if (!r.isSuccess()) {
                log.error("reply to module[{}] failed, r", message.getHeader().getModuleSource().getGroupId(), r.cause());
            }
        });
    }
}
