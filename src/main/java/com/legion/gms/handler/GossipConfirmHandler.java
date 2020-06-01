package com.legion.gms.handler;

import com.legion.core.api.Gossip;
import com.legion.gms.GossipController;
import com.legion.net.common.exceptions.LegionNetException;
import com.legion.net.entities.InetAddressAndPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


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
        List<Gossip.NodeInfo> confirmList = gossipConfirm.getConfirmNodeListList();
        if (confirmList != null && confirmList.size() > 0) {
            /* Notify the Failure Detector */
            gossipController.notifyFailureDetector(confirmList);
            gossipController.applyStateLocally(confirmList);
        }
    }
}
