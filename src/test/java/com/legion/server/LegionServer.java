package com.legion.server;

import com.legion.net.common.config.LegionProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * Create At 2019/8/16
 *
 * @author Zing
 * @version 0.0.1
 */
@Slf4j
public class LegionServer {

    private EventLoopGroup bossGroup = new NioEventLoopGroup();
    private EventLoopGroup workerGroup = new NioEventLoopGroup();


    public ServerBootstrap start(LegionProperties.Server config) {
        assert config != null;
        String address = config.getHost();
        int port = config.getPort();

        ServerBootstrap server = new ServerBootstrap();

        server.group(bossGroup, workerGroup)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioServerSocketChannel.class)
                .childHandler(new LegionServerInitializer(config.getHeartBeat()))
        ;
        try {
            ChannelFuture future = server.bind(address, port).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("legion server start failed", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            log.info("legion server shut down");
        }
        return server;
    }

}
