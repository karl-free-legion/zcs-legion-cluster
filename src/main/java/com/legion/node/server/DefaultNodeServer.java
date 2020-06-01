package com.legion.node.server;

import com.legion.common.LegionBeanHelper;
import com.legion.net.netty.server.AbstractNodeServer;
import com.legion.net.netty.server.message.NodeMessageProcess;
import com.legion.node.handler.*;

/**
 * NodeServer处理消息
 *
 * @author lance
 * 8/19/2019 14:48
 */
public class DefaultNodeServer extends AbstractNodeServer {
    private LegionBeanHelper legionBeanHelper;

    public DefaultNodeServer(LegionBeanHelper legionBeanHelper) {
        this.legionBeanHelper = legionBeanHelper;
    }

    @Override
    public NodeMessageProcess[] processes() {
        return new NodeMessageProcess[]{
                new SimpleSendMessageHandler(),
                new SimpleRplMessageHandler(),
                new GossipMessageHandler(legionBeanHelper),
                new ProcessSendMessageHandler(),
                new ProcessRplMessageHandler(),
                new ModuleHBMessageHandler()
        };
    }

    @Override
    protected void beforeShutdown() {

    }
}
