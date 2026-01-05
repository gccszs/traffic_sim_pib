package com.traffic.sim.plugin.map.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 地图插件配置类
 * 
 * @author traffic-sim
 */
@Configuration
@EnableConfigurationProperties(MapPluginProperties.class)
public class MapPluginConfig {
    // 配置类，用于启用配置属性绑定
}

