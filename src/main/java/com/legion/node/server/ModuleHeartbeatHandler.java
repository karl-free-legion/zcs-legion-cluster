package com.legion.node.server;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.legion.core.api.Modulehb;
import com.legion.core.api.X;
import com.legion.net.common.exceptions.LegionNetException;
import com.legion.net.common.util.FunctionUtils;
import com.legion.net.entities.ApplicationState;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.entities.LegionNodeInfo;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * create at     2019/8/25 3:06 下午
 *
 * @author zing
 * @version 0.0.1
 */
@Slf4j
public class ModuleHeartbeatHandler {

    public X.XMessage handleHeartbeat(X.XMessage message, Channel channel) {
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
        try {
            Modulehb.HBMessage mi = Any.parseFrom(rpl.getBody()).unpack(Modulehb.HBMessage.class);
            Modulehb.ModuleReq req = mi.getModuleSync();
            if (req.getInitialized()) {
                if (theModuleNotRegistered(req)) {
                    log.debug("module register>>>groupId:{},moduleId:{},token:{}", req.getGroupId(), req.getModuleId(), req.getModuleKey());
                    LegionNodeContext.context().appStateUpdateAsync(ApplicationState.MODULE_JOIN, FunctionUtils.LONG_INCREASE);
                }
                Modulehb.HBMessage rplBody = mi.toBuilder().setLegionResp(newModuleJoinClusterResponse(req)).build();
                rpl.setBody(Any.pack(rplBody).toByteString());
                if (StringUtils.isNotBlank(req.getGroupId()) && StringUtils.isNotBlank(req.getModuleId())) {
                    LegionNodeContext.context().moduleGroupAdd(req);
                    LegionNodeContext.context().getChannelGroup().putGroupModuleChannel(req.getGroupId(), req.getModuleId(), channel);
                }
            } else {
                Modulehb.HBMessage rplBody = mi.toBuilder().setLegionResp(newNodePartitionResponse(req, channel.remoteAddress().toString())).build();
                rpl.setBody(Any.pack(rplBody).toByteString());
                log.info("module apply join>>>groupId:{}", req.getGroupId());
            }
        } catch (InvalidProtocolBufferException e) {
            throw new LegionNetException("-501", "message transfer failed", e);
        }
        return rpl.build();
    }


    private boolean theModuleNotRegistered(Modulehb.ModuleReq req) {
        String groupId = req.getGroupId();
        String moduleId = req.getModuleId();
        return !LegionNodeContext.context().getSelfInfo().matchGroupModule(groupId, moduleId);

    }

    private Modulehb.LegionResp newModuleJoinClusterResponse(Modulehb.ModuleReq mi) {
        log.info("ModuleInfo>>:{}", mi);
        List<Modulehb.XAddress> link = distributeNode(mi.getModuleKey());
        return Modulehb.LegionResp.newBuilder()
                .setModuleKey(mi.getModuleKey())
                .addAllLegionNodes(link)
                .build();
    }

    private Modulehb.LegionResp newNodePartitionResponse(Modulehb.ModuleReq moduleInfo, String addressStr) {
        log.debug("module:{}from>>>{}", moduleInfo, addressStr);
        long token = UUID.randomUUID().hashCode();
        // 所有需要链接的节点
        List<Modulehb.XAddress> link = distributeNode(token);

        // 返回的消息
        return Modulehb.LegionResp.newBuilder()
                .setModuleKey(String.valueOf(token))
                .addAllLegionNodes(link)
                .build();

    }

    private List<Modulehb.XAddress> distributeNode(Object token) {
        LegionNodeContext context = LegionNodeContext.context();

        LegionNodeInfo master = context.tokenPartition(token);

        return context.getLinkClusterByToken(token).stream()
                .map(LegionNodeContext.context()::getNodeInfoByNet)
                .map(n -> Modulehb.XAddress.newBuilder()
                        .setNodeId(n.getNodeId())
                        .setHost(n.getHost())
                        .setPort(n.getPort())
                ).map(addr -> {
                    if (0 == StringUtils.compare(master.getNodeId(), addr.getNodeId())) {
                        addr.setIsMaster(true);
                    } else {
                        addr.setIsMaster(false);
                    }
                    return addr.build();
                })
                .peek(a -> log.debug("token:{} got addr:{}", token, a))
                .collect(Collectors.toList());
    }
}
