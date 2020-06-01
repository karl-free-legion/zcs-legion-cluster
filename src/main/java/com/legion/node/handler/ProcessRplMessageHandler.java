package com.legion.node.handler;

import com.legion.common.utils.CacheUtils;
import com.legion.core.api.X;
import com.legion.net.common.exceptions.LegionNetException;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.SingleEmitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ProcessRplMessageHandler extends AbstractMessageHandler {
    @Override
    public boolean preHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        if (message.getIsReply() && message.getHeader().getTagType().equals(X.XTagType.PROCESS)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean doHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        setCurrentStepComplete(message);
        if (message.getHeader().hasNodeSource()) {
            String nodeId = message.getHeader().getNodeSource().getNodeId();
            if (StringUtils.isBlank(nodeId)) {
                log.error("reply process message failed, node source is empty, Header: {}", message.getHeader());
                throw new LegionNetException("-404", "Node Forward Process message not found Node" + nodeId);
            }
            forwardMessage(message, nodeId);
            return true;
        }
        return false;
    }

    /**
     * 设置消息完成(流程定义有状态结构)
     * traceId在流程中不能修改, traceId被TraceSpan占用
     *
     * @param message message
     */
    private void setCurrentStepComplete(X.XMessage message) {
        String traceId = message.getHeader().getTrackId() + "_" + message.getHeader().getTag().hashCode();
        SingleEmitter<X.XMessage> emitter = CacheUtils.getEmitter(traceId);

        if (emitter != null) {
            CacheUtils.removeEmitter(traceId);
            emitter.onSuccess(message);
        }
    }
}
