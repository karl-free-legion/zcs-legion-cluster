package com.legion.core;

import com.legion.core.api.X;
import com.legion.net.netty.core.handler.ExceptionHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * Create At 2019/8/16
 *
 * @author Zing
 * @version 0.0.1
 */
public abstract class AbstractLegionNodeInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                .addLast(new ExceptionHandler())
                .addLast(new ProtobufVarint32FrameDecoder())
                .addLast(new ProtobufDecoder(X.XMessage.getDefaultInstance()))
                .addLast(new ProtobufVarint32LengthFieldPrepender())
                .addLast(new ProtobufEncoder());
        ChannelHandler[] handlers = handlers();
        for (ChannelHandler handler : handlers) {
            ch.pipeline().addLast(handler);
        }
    }

    protected abstract ChannelHandler[] handlers();
}
