package com.legion.node.connector;

import com.legion.net.common.config.LegionProperties;
import com.legion.net.netty.connector.AbstractNodeConnector;
import com.legion.net.netty.connector.message.MessageHandler;
import io.netty.channel.ChannelHandler;

/**
 * 默认Node客户端实现
 *
 * @author lance
 * 8/19/2019 16:47
 */
public class DefaultNodeConnector extends AbstractNodeConnector {

    public DefaultNodeConnector(LegionProperties properties) {
        super(properties);
    }

    @Override
    public ChannelHandler[] handlers() {
        return new ChannelHandler[]{new MessageHandler()};
    }
}
