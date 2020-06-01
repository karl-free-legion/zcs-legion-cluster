package com.legion;

import com.legion.connector.LegionNodeConnector;
import com.legion.net.common.config.LegionProperties;
import org.junit.Test;

/**
 * Create At 2019/8/16
 *
 * @author Zing
 * @version 0.0.1
 */
public class ConnectorTest {
    @Test
    public void testConnector() throws InterruptedException {
        LegionProperties.Address config = new LegionProperties.Address();
        config.setHost("127.0.0.1");
        config.setPort(9464);


        LegionNodeConnector connector = new LegionNodeConnector();

        connector.linkStart(config);
        Thread.currentThread().join();
    }
}
