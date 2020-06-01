package com.legion.server;

import com.legion.core.AbstractLegionNodeInitializer;
import com.legion.net.common.config.LegionProperties;
import com.legion.server.handler.HeartbeatResponseHandler;
import io.netty.channel.ChannelHandler;

/**
 * Create At 2019/8/16
 *
 * @author Zing
 * @version 0.0.1
 */

@ChannelHandler.Sharable
public class LegionServerInitializer extends AbstractLegionNodeInitializer {
    private LegionProperties.HeartBeat heartBeat;

    public LegionServerInitializer(final LegionProperties.HeartBeat heartBeat) {
        this.heartBeat = heartBeat;
    }


    @Override
    protected ChannelHandler[] handlers() {
        return new ChannelHandler[]{
                new HeartbeatResponseHandler()
        };
    }
}
