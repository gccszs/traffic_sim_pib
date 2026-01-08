package com.traffic.sim.plugin.simulation.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 仿真插件自动配置类
 * 
 * @author traffic-sim
 */
@AutoConfiguration
@EnableConfigurationProperties(SimulationPluginProperties.class)
@ComponentScan(basePackages = "com.traffic.sim.plugin.simulation")
@EntityScan(basePackages = "com.traffic.sim.plugin.simulation.entity")
@EnableJpaRepositories(basePackages = "com.traffic.sim.plugin.simulation.repository")
public class SimulationPluginAutoConfig {
    // 自动配置类，用于启用组件扫描和JPA配置
}

