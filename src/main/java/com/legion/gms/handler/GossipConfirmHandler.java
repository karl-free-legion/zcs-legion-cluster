package com.legion.gms.handler;

import com.legion.core.api.Gossip;
import com.legion.gms.GossipController;
import com.legion.net.common.exceptions.LegionNetException;
import com.legion.net.entities.InetAddressAndPort;
import com.legion.net.entities.LegionNodeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component("gossip_confirm_handler")
public class GossipConfirmHandler extends GossipHandler<Gossip.GossipConfirm>{

    @Autowired
    private GossipController gossipController;

    @Override
    protected void preHandle(Gossip.GossipConfirm stageData) {

    }

    @Override
    protected void handleSuccess(Gossip.GossipConfirm stageData) {

    }

    @Override
    protected void handleFail(LegionNetException e) {

    }

    @Override
    protected void onHandle(Gossip.GossipConfirm gossipConfirm, String fromNodeId, InetAddressAndPort fromNodeNet) throws LegionNetException {
        List<Gossip.NodeInfo> gossipConfirmConfirmNodeListList = gossipConfirm.getConfirmNodeListList();
        //修复BUG，不做自己节点信息的外部同步 2020-8-18
        List<Gossip.NodeInfo> confirmList = gossipConfirmConfirmNodeListList.stream().filter(item -> !item.getNodeId().equals(LegionNodeContext.context().getSelfInfo().getNodeId())).collect(Collectors.toList());
        confirmList.removeIf(item -> item.getNodeId().equals(LegionNodeContext.context().getSelfInfo().getNodeId()));
        if (confirmList != null && confirmList.size() > 0) {
            /* Notify the Failure Detector */
            gossipController.notifyFailureDetector(confirmList);
            gossipController.applyStateLocally(confirmList);
        }
    }
}
