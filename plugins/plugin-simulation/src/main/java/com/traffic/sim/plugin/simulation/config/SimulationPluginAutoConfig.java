package com.traffic.sim.plugin.simulation.config;

import com.traffic.sim.plugin.simulation.controller.SimulationController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 仿真插件自动配置类
 * 
 * @author traffic-sim
 */
@AutoConfiguration
@EnableConfigurationProperties(SimulationPluginProperties.class)
@ComponentScan(basePackages = {
    "com.traffic.sim.plugin.simulation.service",
    "com.traffic.sim.plugin.simulation.grpc",
    "com.traffic.sim.plugin.simulation.controller"
})
@EntityScan(basePackages = "com.traffic.sim.plugin.simulation.entity")
@EnableJpaRepositories(basePackages = "com.traffic.sim.plugin.simulation.repository")
@Import({SimulationController.class})
public class SimulationPluginAutoConfig {
    // Spring Boot 自动配置 ObjectMapper，无需手动定义
}

