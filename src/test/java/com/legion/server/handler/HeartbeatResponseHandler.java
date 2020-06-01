package com.legion.server.handler;

import com.google.protobuf.ByteString;
import com.legion.core.api.X;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Create At 2019/8/16
 *
 * @author Zing
 * @version 0.0.1
 */
@Slf4j
public class HeartbeatResponseHandler extends SimpleChannelInboundHandler<X.XMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, X.XMessage xMessage) throws Exception {
        if (X.XMessageType.HEARTBEAT.equals(xMessage.getHeader().getMsgType())) {
            log.info("request:{}", xMessage.getBody().toStringUtf8());
            String localAddress = ctx.channel().localAddress().toString();
            X.XMessage rpl = X.XMessage.newBuilder()
                    .setHeader(X.XHeader.newBuilder()
                            .setMsgType(X.XMessageType.HEARTBEAT)
                            .build())
                    .setBody(ByteString.copyFromUtf8(localAddress + " - pong"))
                    .build();
            ctx.writeAndFlush(rpl);
        }
    }
}
