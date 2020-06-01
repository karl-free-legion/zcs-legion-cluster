package com.legion.node.handler;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.legion.core.api.X;
import com.legion.core.exception.LegionException;
import com.legion.core.utils.RequestURI;
import com.legion.net.entities.LegionChannelGroup;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.netty.transport.LegionCourier;
import com.legion.process.StateMachine;
import com.legion.process.StateRunner;
import com.legion.process.state.State;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 流程发送
 * @author lance
 * 10/18/2019 18:33
 */
@Slf4j
public class ProcessSendMessageHandler extends AbstractMessageHandler {

    @Override
    public boolean preHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        if (!message.getIsReply() && message.getHeader().getTagType().equals(X.XTagType.PROCESS) && !message.getHeader().hasNodeSource()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean doHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        RequestURI requestUri = RequestURI.create(message.getHeader().getUri());
        innerProcess(requestUri, message);
        return false;
    }


    /**
     * 内部流程处理，兼容旧版Legion
     *
     * @param requestUri requestURI
     * @param message    message
     */

    private void innerProcess(RequestURI requestUri, X.XMessage message) {
        X.XHeader header = message.getHeader();
        if(log.isInfoEnabled()){
            log.info("inner process>>>!!!{}, {}", message, requestUri.getGroupId());
        }
        Single<X.XMessage> xMessageSingle = Single.just(message);

        //忽略流程定义名称大小写
        String defineId = StringUtils.isBlank(requestUri.getDefineId()) ? requestUri.getGroupId() : requestUri.getDefineId();
        StateMachine.Builder builder = StateMachine.fromJson(StateRunner.CACHE_PROCESS_DEFINE.get(defineId.toLowerCase()));

        //XHelper helper;
        StateMachine machine = builder.build();
        Map<String, State> states = machine.getStates();
        xMessageSingle = StateRunner.newInstance().run(machine.getStart(), states, Maps.newHashMap(), xMessageSingle, (ProcessSendMessageHandler) this);
        xMessageSingle.observeOn(Schedulers.single())
                .subscribe(rpl -> {
                    X.XMessage.Builder reply = newProcessReply(rpl.getRpl().getRplCode(),
                            rpl.getRpl().getRplMessage(),
                            header,
                            rpl.getBody(),
                            rpl.getRpl().getStatusMap(),
                            rpl.getRpl().getExtensionMap());

                    // 错误码传递
                    X.XReplyHeader.Builder h = rpl.getRpl().toBuilder();
                    if (StringUtils.isNotBlank(h.getErrorCode())) {
                        X.XReplyHeader.Builder newRplHead = reply.getRpl().toBuilder().setErrorCode(h.getErrorCode());
                        reply.setRpl(newRplHead.build());
                    }

                    //消息完成后Node -> Module
                    LegionChannelGroup channelGroup = LegionNodeContext.context().getChannelGroup();
                    Channel channel = channelGroup.getModuleChannel(header.getModuleSource().getGroupId(),
                            header.getModuleSource().getModuleId());
                    if (channel.isActive() && channel.isWritable()) {
                        channel.writeAndFlush(reply.build());
                    }
                }, e -> {
                    log.warn("process error.", e);
                    sendProcessError(e, message);
                });
    }

    private X.XMessage.Builder newProcessReply(int code, String message, X.XHeader originHeader, ByteString body,
                                               Map<String, String> status, Map<String, String> extension) {
        X.XReplyHeader.Builder replyHeader = X.XReplyHeader.newBuilder()
                .setRplCode(code)
                .setRplMessage(message);
        if (status != null) {
            replyHeader.putAllStatus(status);
        }

        if (extension != null) {
            replyHeader.putAllExtension(extension);
        }
        X.XMessage.Builder replyMessage = X.XMessage.newBuilder().setIsReply(true).setRpl(replyHeader.build()).setHeader(originHeader);
        if (body != null && !body.isEmpty()) {
            replyMessage.setBody(body);
        }
        return replyMessage;
    }

    /**
     * 流程处理失败
     *
     * @param e exception
     * @param message message
     */
    private void sendProcessError(Throwable e, X.XMessage message) {
        LegionException ex = (e instanceof LegionException) ? (LegionException) e : new LegionException("process failed", e);
        X.XHeader mh = message.getHeader();
        X.XMessage.Builder reply = newProcessReply(ex.getCode(), ex.getErrorCode(),
                mh, message.getBody(), null, null);
        X.XReplyHeader.Builder replyHeader = reply.getRpl().toBuilder();
        replyHeader.setErrorCode(ex.getErrorCode());
        replyHeader.setRplMessage(ex.getMessage());
        reply.setRpl(replyHeader.build());
        String moduleId = mh.getModuleSource().getModuleId();
        String groupId = mh.getModuleSource().getGroupId();
        Channel channel = LegionNodeContext.context().getChannelGroup().getModuleChannel(groupId, moduleId);
        if (channel == null) {
            forwardMessage(message, mh.getNodeSource().getNodeId());
        } else {
            LegionCourier.instance().sendModule(reply.build(), message.getHeader().getMsgType(), channel);
        }
    }
}
