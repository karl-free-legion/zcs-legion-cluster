package com.legion.connector;

import com.google.protobuf.ByteString;
import com.legion.core.api.X;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static com.legion.core.api.X.XMessageType.HEARTBEAT;

/**
 * Create At 2019/8/16
 *
 * @author Zing
 * @version 0.0.1
 */
@Slf4j
public class HeartbeatSender extends SimpleChannelInboundHandler<X.XMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, X.XMessage xMessage) throws Exception {
        if (HEARTBEAT.equals(xMessage.getHeader().getMsgType())) {
            log.info(xMessage.getBody().toStringUtf8());
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = ctx.channel().remoteAddress().toString();
        String localAddress = ctx.channel().localAddress().toString();
        Runnable r = () -> {
            log.info("{} ping to {}", localAddress, remoteAddress);
            X.XMessage message = X.XMessage.newBuilder()
                    .setHeader(X.XHeader.newBuilder()
                            .setMsgType(HEARTBEAT)
                            .build())
                    .setBody(ByteString.copyFromUtf8(localAddress + "-ping"))
                    .build();
            ctx.writeAndFlush(message);
        };

        SchedulerTaskPool.addTask(remoteAddress, r);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        String remoteAddress = ctx.channel().remoteAddress().toString();
        SchedulerTaskPool.cancelTask(remoteAddress);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {

        }
    }
}
