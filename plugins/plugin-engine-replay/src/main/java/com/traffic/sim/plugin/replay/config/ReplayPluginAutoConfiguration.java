package com.traffic.sim.plugin.replay.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 回放插件自动配置类
 * 
 * @author traffic-sim
 */
@Configuration
@EnableConfigurationProperties(ReplayPluginProperties.class)
@ComponentScan(basePackages = "com.traffic.sim.plugin.replay")
public class ReplayPluginAutoConfiguration {
    // 自动配置类，启用配置属性扫描和组件扫描
}

