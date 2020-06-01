package com.legion.node.handler;

import com.legion.common.LegionBeanHelper;
import com.legion.core.api.X;
import com.legion.gms.GossipStage;
import com.legion.net.common.util.FunctionUtils;
import com.legion.net.entities.ApplicationState;
import com.legion.net.entities.LegionNodeContext;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * all gossip message handler
 */
@Slf4j
public class GossipMessageHandler extends AbstractMessageHandler {
    private LegionBeanHelper legionBeanHelper;

    public GossipMessageHandler(LegionBeanHelper legionBeanHelper) {
        this.legionBeanHelper = legionBeanHelper;
    }

    @Override
    public boolean preHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        if (message.getHeader().getMsgType().equals(X.XMessageType.GOSSIP_INIT)) {
            return true;
        }
        if (message.getHeader().getMsgType().equals(X.XMessageType.GOSSIP_DIFF)) {
            return true;
        }
        if (message.getHeader().getMsgType().equals(X.XMessageType.GOSSIP_CONFIRM)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean doHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        try {
            X.XMessageType type = message.getHeader().getMsgType();
            GossipStage gossipStage = convertGossip(type);
            legionBeanHelper.loadGossipHandler(gossipStage).doHandle(message, gossipStage);
            LegionNodeContext.context().appStateUpdateAsync(ApplicationState.GOS_MESSAGE_RECV, FunctionUtils.LONG_INCREASE);
            return true;
        } catch (Exception e) {
            log.error("{} gossip message handler failed!", message.getHeader());
        }
        return false;
    }



    /**
     * Gossip类型转换
     *
     * @param type XMessageType
     * @return GossipStage
     */
    private GossipStage convertGossip(X.XMessageType type) {
        switch (type) {
            case GOSSIP_DIFF:
                return GossipStage.DIFF;
            case GOSSIP_CONFIRM:
                return GossipStage.CONFIRM;
            case GOSSIP_INIT:
            default:
                return GossipStage.INIT;
        }
    }

}
