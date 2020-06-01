package com.legion.common.event;

import lombok.extern.slf4j.Slf4j;

/**
 * Create At 2019/8/20
 *
 * @author Zing
 * @version 0.0.1
 */
@Slf4j
@EventListener
public class TestEventListener {

    @Subscriber(topic = "AHA", order = 2)
    public void testLegionMessage1(String message) {
        log.info("AHA1:{}", message);
    }

    @Subscriber(topic = "AHA", order = 1)
    public void testLegionMessage2(String message) {
        log.info("AHA2:{}", message);
    }

    @Subscriber(topic = "DA", order = 1)
    public void testDa() {
        log.info("DA:{}", "这个参数是空的");
    }
}
