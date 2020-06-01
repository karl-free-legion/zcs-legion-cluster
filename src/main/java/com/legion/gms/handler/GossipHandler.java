package com.legion.gms.handler;

import com.google.protobuf.Message;
import com.legion.core.api.X;
import com.legion.gms.GossipStage;
import com.legion.net.common.exceptions.LegionNetException;
import com.legion.net.entities.InetAddressAndPort;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Gossip 消息处理器 （三次握手 Init, Diff, Confirm）
 *
 * @param <T>
 */
@Slf4j
public abstract class GossipHandler<T extends Message> {

    public void doHandle(X.XMessage xmsg, GossipStage gossipStage) {
        try {
            String fromNodeId = xmsg.getHeader().getNodeSource().getNodeId();
            InetAddressAndPort fromNodeNet = InetAddressAndPort.getByNameAndPort(xmsg.getHeader().getNodeSource().getHost(), xmsg.getHeader().getNodeSource().getPort());
            Optional result = gossipStage.deserialize(xmsg.getBody());
            result.ifPresent(t ->
                    {
                        T data = (T) t;
                        try {
                            onHandle(data, fromNodeId, fromNodeNet);
                            handleSuccess(data);
                        } catch (Exception e) {
                            log.error("Message GossipStage[{}] From {} Handle failed due to {}", gossipStage.getCode(), xmsg.getHeader(), e);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Message GossipStage[{}] From {} Handle failed due to", gossipStage.getCode(), xmsg.getHeader(), e);
        }
    }

    protected abstract void onHandle(T data, String fromNodeId, InetAddressAndPort fromNodeNet);

    protected abstract void preHandle(T stageData);

    protected abstract void handleSuccess(T stageData);

    protected abstract void handleFail(LegionNetException e);
}
