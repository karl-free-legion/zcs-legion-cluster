package com.legion.node.handler;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.legion.common.utils.CommonUtils;
import com.legion.core.LegionGlobalConstant;
import com.legion.core.api.HttpRoute;
import com.legion.core.api.X;
import com.legion.net.common.util.GossipUtils;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.entities.LegionNodeInfo;
import com.legion.net.netty.transport.LegionCourier;
import com.legion.net.netty.transport.SentResultEntity;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;


/**
 * 全网路由信息获取
 */
@Slf4j
public class RequireModuleHttpInfoHandler extends AbstractMessageHandler {

    @Override
    public boolean preHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        return message.getHeader().getMsgType().equals(X.XMessageType.ACQUIRE_ROUTE);
    }

    @Override
    public boolean doHandler(X.XMessage message, ChannelHandlerContext channelHandlerContext) {
        boolean result = false;
        try {
            Optional<? extends Message> httpRouteReq = Optional.of(Any.parseFrom(message.getBody()).unpack(HttpRoute.HttpRouteReq.class));
            result = httpRouteReq.isPresent();
            httpRouteReq.ifPresent(m -> handleRouteReq(m, message, channelHandlerContext.channel()));
        } catch (Throwable e) {
            log.error("module hb message handler failed. {}", message.getHeader(), e);
        }
        return result;
    }

    private void handleRouteReq(Message httpRouteReq, X.XMessage message, Channel channel) {
        final String targetGroupId = ((HttpRoute.HttpRouteReq) httpRouteReq).getGroupId();
        if (StringUtils.isBlank(targetGroupId)) {
            log.error("module[{}] ACQUIRE ROUTE messages failed, due to empty groupId", message.getHeader().getModuleSource().getGroupId());
            return;
        }

        //回复报文组装
        HttpRoute.HttpRouteResp.Builder rplBody = HttpRoute.HttpRouteResp.newBuilder();
        X.XMessage.Builder rpl = message.toBuilder();
        rpl.setIsReply(true);
        LegionNodeInfo selfInfo = LegionNodeContext.context().getSelfInfo();
        rpl.setHeader(rpl.getHeader().toBuilder()
                .setNodeSource(X.XNodeSource.newBuilder()
                        .setNodeId(selfInfo.getNodeId())
                        .setHost(selfInfo.getHost())
                        .setPort(selfInfo.getPort())
                        .build())
                .build());

        //路由信息组装
        LegionNodeContext.context().getClusterNodes().values().stream()
                .filter(ln ->
                        LegionGlobalConstant.ACQUIRE_ALL_HTTP_ROUTE.equals(targetGroupId)
                                || ln.matchGroup(targetGroupId))
                .map(ln -> {
                    HttpRoute.GroupCluster.Builder groupCluster = HttpRoute.GroupCluster.newBuilder();
                    ln.getLegionGroupModuleHttp().entrySet().stream().filter(e ->
                            GossipUtils.matchGroup(e.getKey(), targetGroupId)
                    ).forEach(e -> {
                                HttpRoute.ModuleInfo.Builder moduleInfo = HttpRoute.ModuleInfo.newBuilder();
                                final String[] temp = GossipUtils.deSerializeGroupModule(e.getKey());
                                if (temp != null && temp.length > 1) {
                                    moduleInfo.setModuleId(temp[1]);
                                    moduleInfo.setHttpInfo(e.getValue());
                                    groupCluster.addModuleInfo(moduleInfo);
                                }
                            }
                    );
                    return groupCluster;
                })
                .filter(CommonUtils.distinctByKey(t -> t.getGroupId()))
                .forEach(rplBody::addGroupInfo);


        //回复发送
        rpl.setBody(Any.pack(rplBody.build()).toByteString());
        Promise<SentResultEntity> result = LegionCourier.instance().sendModule(rpl.build(), X.XMessageType.ACQUIRE_ROUTE, channel);
        result.addListener(r -> {
            if (!r.isSuccess()) {
                log.error("reply ACQUIRE_ROUTE to module[{}] failed, r", message.getHeader().getModuleSource().getGroupId(), r.cause());
            }
        });
    }
}
