package com.legion.node.handler;

import com.legion.core.api.X;
import com.legion.core.utils.RequestURI;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleSendMessageHandler extends AbstractMessageHandler {

    @Override
    public boolean preHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        //可处理的消息类型判断
        if (message.getHeader().getMsgType().equals(X.XMessageType.NORMAL) && !message.getIsReply()) {
            return true;
        }

        if(!message.getIsReply() && message.getHeader().getTagType().equals(X.XTagType.PROCESS) && message.getHeader().hasNodeSource()){
            return true;
        }
        return false;
    }

    @Override
    public boolean doHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        RequestURI request = RequestURI.create(message.getHeader().getUri());
        String groupId = request.getGroupId();
        return send(message, groupId);
    }
}
