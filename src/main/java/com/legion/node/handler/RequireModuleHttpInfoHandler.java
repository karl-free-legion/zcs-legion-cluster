package com.legion.node.handler;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.legion.core.LegionGlobalConstant;
import com.legion.core.api.Gossip;
import com.legion.core.api.HttpRoute;
import com.legion.core.api.X;
import com.legion.net.common.util.GossipUtils;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.entities.LegionNodeInfo;
import com.legion.net.entities.SyncModuleInfo;
import com.legion.net.netty.transport.LegionCourier;
import com.legion.net.netty.transport.SentResultEntity;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;


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
        try {
            Optional<? extends Message> httpRouteReq = Optional.of(Any.parseFrom(message.getBody()).unpack(HttpRoute.HttpRouteReq.class));
            httpRouteReq.ifPresent(m -> handleRouteReq(m, message, channelHandlerContext.channel()));
            return true;
        } catch (Throwable e) {
            log.error("module ACQUIRE ROUTE message handler failed. {}", message.getHeader(), e);
        }
        return false;
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
        //路由信息过滤
        final Map<String, Set<SyncModuleInfo>> wholeSets = new HashMap<>();
        LegionNodeContext.context().getClusterNodes().values().forEach(ln ->
                ln.getModuleGroupStates().entrySet().stream()
                        .filter(e -> StringUtils.isNotBlank(e.getValue().getHttpInfo()))
                        .filter(e ->
                                (LegionGlobalConstant.ACQUIRE_ALL_HTTP_ROUTE.equals(targetGroupId)
                                        || GossipUtils.matchGroup(e.getKey(), targetGroupId)))
                        .forEachOrdered(e -> {
                            final String[] temp = GossipUtils.deSerializeGroupModule(e.getKey());
                            if (temp != null && temp.length > 1) {
                                Set<SyncModuleInfo> ms = wholeSets.getOrDefault(temp[0], new HashSet<>());
                                ms.add(e.getValue());
                                wholeSets.putIfAbsent(temp[0], ms);
                            }
                        }));
        wholeSets.entrySet().stream().filter(e -> e.getValue() != null && e.getValue().size() > 0).forEach(e -> {
//            log.info("available gid={}, ms[{}]", key, value.stream().map(SyncModuleInfo::toString).collect(Collectors.joining("],[")));
            HttpRoute.GroupCluster.Builder groupCluster = HttpRoute.GroupCluster.newBuilder();
            Gossip.ModuleInfo.Builder moduleInfo = Gossip.ModuleInfo.newBuilder();
            e.getValue().forEach(m -> {
                moduleInfo.setModuleId(m.getModuleId());
                moduleInfo.setHttpInfo(m.getHttpInfo());
                String routeVersion = m.getRouteVersion();
                if (StringUtils.isBlank(routeVersion)) {
                    moduleInfo.setRouteVersion(LegionGlobalConstant.DEFAULT_ROUTE_VERSION);
                } else {
                    moduleInfo.setRouteVersion(routeVersion);
                }
                groupCluster.addModuleInfo(moduleInfo);
            });
            groupCluster.setGroupId(e.getKey());
            rplBody.addGroupInfo(groupCluster);
        });
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
