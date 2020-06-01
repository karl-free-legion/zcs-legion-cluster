package com.legion.connector;

import com.legion.net.common.config.LegionProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Create At 2019/8/16
 *
 * @author Zing
 * @version 0.0.1
 */
@Slf4j
public class LegionNodeConnector {

    private EventLoopGroup worker = new NioEventLoopGroup();
    private Bootstrap connector = new Bootstrap();

    private AtomicInteger retryLimit = new AtomicInteger(3);

    public volatile boolean isRunning = false;


    public Bootstrap linkStart(LegionProperties.Address legionNode) {
        String host = legionNode.getHost();
        int port = legionNode.getPort();
        log.info("link to cluster node >>> {}:{}", host, port);
        connector.group(worker)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioSocketChannel.class)
                .handler(new LegionConnectorInitializer())
        ;

        doConnect(legionNode);
        return connector;


    }

    public ChannelFuture doConnect(LegionProperties.Address address) {
        ChannelFuture future;
        String host = address.getHost();
        int port = address.getPort();
        future = connector.connect(host, port)
                .addListener((ChannelFuture f) -> {
                    if (f.isSuccess()) {
                        isRunning = true;
                        resetRetryLimit();
                    } else if (retryLimit.getAndDecrement() > 0) {
                        f.channel().eventLoop().schedule(() -> doConnect(address), 5, TimeUnit.SECONDS);
                    } else {
                        stopConnect(f);
                    }
                });
        return future;
    }

    private void stopConnect(ChannelFuture f) {
        try {
            if (f != null) {

                f.channel().closeFuture().sync();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            worker.shutdownGracefully();
        }
    }

    private void resetRetryLimit() {
        retryLimit = new AtomicInteger(3);
    }
}
