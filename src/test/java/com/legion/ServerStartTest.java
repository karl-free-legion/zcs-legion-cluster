package com.legion;

import com.legion.net.common.config.LegionProperties;
import com.legion.net.common.util.GossipUtils;
import com.legion.server.LegionServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Create At 2019/8/16
 *
 * @author Zing
 * @version 0.0.1
 */
@Slf4j
public class ServerStartTest {
    @Test
    public void testServerStart() {
        LegionProperties config = new LegionProperties();
        LegionServer server = new LegionServer();
        server.start(config.getServer());
    }

    @Test
    public void testGroupModuleMatch() {
        String groupId = "g001";
        String moduleId = "7c999ab28a08aa2e253e3fea6a73fc58";
        String source = GossipUtils.serializeGroupModule(groupId, moduleId);
        log.info("serialize - {}", source);
        GossipUtils.deSerializeGroupModule(source);
        log.info("deSerialize - {}", Arrays.asList(GossipUtils.deSerializeGroupModule(source)).stream().collect(Collectors.joining(",")));
        assert GossipUtils.matchGroup(source, groupId);
        assert GossipUtils.matchGroupModule(source, groupId, moduleId);
    }


}
