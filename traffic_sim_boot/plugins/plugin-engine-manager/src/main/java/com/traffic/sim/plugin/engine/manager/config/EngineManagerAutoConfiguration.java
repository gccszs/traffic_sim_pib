package com.traffic.sim.plugin.engine.manager.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 引擎管理插件自动配置类
 * 
 * @author traffic-sim
 */
@Configuration
@EnableConfigurationProperties(EngineManagerProperties.class)
@ComponentScan(basePackages = "com.traffic.sim.plugin.engine.manager")
public class EngineManagerAutoConfiguration {
    // 自动配置类，启用配置属性扫描和组件扫描
}

