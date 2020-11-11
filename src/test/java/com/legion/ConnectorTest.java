package com.legion;

import com.legion.connector.LegionNodeConnector;
import com.legion.core.utils.Digest;
import com.legion.net.common.config.LegionProperties;
import com.legion.net.entities.SyncModuleInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void versionTest1() {
        List<SyncModuleInfo> groupMatchList = new ArrayList<>();
        String targetVersion = "v0.0.0";
//        groupMatchList.add(new SyncModuleInfo("sdzw","http:/1","v0.0.0"));
        groupMatchList.add(new SyncModuleInfo("sdzw", "http:/4", "v4.0.0"));
//        long id = System.currentTimeMillis();

        Digest.findMatchVersionGroup(groupMatchList, SyncModuleInfo::getRouteVersion, targetVersion)
                .forEach(s -> System.out.println("M : " + s.toString()));

    }
}
