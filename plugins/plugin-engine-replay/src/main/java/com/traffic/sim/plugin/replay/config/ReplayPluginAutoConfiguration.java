package com.traffic.sim.plugin.replay.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * 回放插件自动配置类
 * 
 * @author traffic-sim
 */
@AutoConfiguration
@EnableConfigurationProperties(ReplayPluginProperties.class)
@ComponentScan(basePackages = "com.traffic.sim.plugin.replay")
@EntityScan(basePackages = "com.traffic.sim.plugin.replay.entity")
@EnableJpaRepositories(basePackages = "com.traffic.sim.plugin.replay.repository")
@EnableMongoRepositories(basePackages = "com.traffic.sim.plugin.replay.repository")
public class ReplayPluginAutoConfiguration {
    // 自动配置类，启用配置属性扫描、组件扫描和JPA/MongoDB仓库扫描
}

