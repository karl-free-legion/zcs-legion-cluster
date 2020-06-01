package com.legion.common.config;

import com.legion.net.common.config.LegionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LegionProperties配置
 * @author lance
 * 11/26/2019 21:08
 */
@Configuration
public class ClusterPropertiesConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "legion")
    public LegionProperties properties(){
        return new LegionProperties();
    }
}
