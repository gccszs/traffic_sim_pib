package com.traffic.sim.plugin.map.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 地图插件自动配置类
 * 
 * @author traffic-sim
 */
@AutoConfiguration
@EnableConfigurationProperties(MapPluginProperties.class)
@ComponentScan(basePackages = "com.traffic.sim.plugin.map")
@EntityScan(basePackages = "com.traffic.sim.plugin.map.entity")
@EnableJpaRepositories(basePackages = "com.traffic.sim.plugin.map.repository")
public class MapPluginConfig {
    // 配置类，用于启用配置属性绑定和组件扫描
}

