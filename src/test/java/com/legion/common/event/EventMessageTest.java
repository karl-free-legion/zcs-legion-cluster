package com.legion.common.event;

import com.legion.net.entities.HeartBeatState;
import com.legion.net.entities.InetAddressAndPort;
import com.legion.net.entities.LegionNodeInfo;
import com.legion.node.controller.ClusterEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.UnknownHostException;

/**
 * create at     2019-08-19 11:24
 *
 * @author zing
 * @version 0.0.1
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class EventMessageTest {
    @Test
    public void testMessagePost() {
        EventBus.getInstance().post("AHA", "试试");
    }

    @Test
    public void testLegionNodeUnreachable() throws UnknownHostException {
        EventBus.pushEvent(ClusterEvent.MAKE_PENDING,
                new LegionNodeInfo(new HeartBeatState(0, 0), InetAddressAndPort.getByNameAndPort("localhost", 8848)));
    }

    @Test
    public void testEmptySubs() {
        EventBus.pushEvent("DA");
    }
}
