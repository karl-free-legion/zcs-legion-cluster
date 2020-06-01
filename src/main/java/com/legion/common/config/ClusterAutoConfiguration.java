package com.legion.common.config;

import com.google.common.io.CharStreams;
import com.legion.common.LegionBeanHelper;
import com.legion.net.common.config.LegionProperties;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.netty.server.AbstractNodeServer;
import com.legion.node.connector.DefaultNodeConnector;
import com.legion.node.server.DefaultNodeServer;
import com.legion.process.StateRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 自动配置
 *
 * @author lance
 * 8/22/2019 10:54
 */
@Slf4j
@Configuration
@ConditionalOnClass({LegionBeanHelper.class, ClusterPropertiesConfiguration.class})
public class ClusterAutoConfiguration {
    @Autowired
    private LegionProperties properties;
    @Autowired
    private LegionBeanHelper legionBeanHelper;

    @PostConstruct
    public void init() {
        LegionNodeContext.context().start(properties, new DefaultNodeConnector(properties));

        //start node listen
        AbstractNodeServer legionServer = new DefaultNodeServer(legionBeanHelper);
        legionServer.start(properties);

        //load process define
        new ResourceManage().loadProcessDefine();
    }

    /**
     * 资源管理
     */
    private static class ResourceManage {
        private static final ResourcePatternResolver RESOURCE_RESOLVER = new PathMatchingResourcePatternResolver();
        private String[] processLocations = {"process/**/*.json"};

        private void loadProcessDefine() {
            try {
                Resource[] resources = resolveProcessLocations();
                for (Resource resource : resources) {
                    InputStream input = resource.getInputStream();
                    String content = CharStreams.toString(new InputStreamReader(input, Charset.forName("utf-8")));
                    StateRunner.CACHE_PROCESS_DEFINE.put(getFileName(resource.getFilename()), content);
                }
            } catch (IOException e) {
                log.warn("load process define fail: ", e);
            }
        }

        /**
         * 读取流程资源
         */
        private Resource[] resolveProcessLocations() {
            return Stream.of(Optional.ofNullable(this.processLocations).orElse(new String[0]))
                    .flatMap(location -> Stream.of(getResources(location))).toArray(Resource[]::new);
        }

        /**
         * 加载路径下资源文件
         */
        private Resource[] getResources(String location) {
            try {
                return RESOURCE_RESOLVER.getResources(location);
            } catch (IOException e) {
                return new Resource[0];
            }
        }

        private String getFileName(String fileName) {
            return StringUtils.substringBefore(fileName, ".").toLowerCase();
        }
    }
}
