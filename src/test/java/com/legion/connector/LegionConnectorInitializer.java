package com.legion.connector;

import com.legion.core.AbstractLegionNodeInitializer;
import io.netty.channel.ChannelHandler;

/**
 * Create At 2019/8/16
 *
 * @author Zing
 * @version 0.0.1
 */
public class LegionConnectorInitializer extends AbstractLegionNodeInitializer {
    @Override
    protected ChannelHandler[] handlers() {
        return new ChannelHandler[]{
                new HeartbeatSender()
        };
    }
}
