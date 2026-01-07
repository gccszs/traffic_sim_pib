package com.traffic.sim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Traffic Simulation Boot Application
 * 主启动类
 * 
 * @author traffic-sim
 */
@Slf4j
@SpringBootApplication(exclude = { 
    SecurityAutoConfiguration.class, 
    UserDetailsServiceAutoConfiguration.class,
    SecurityFilterAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.traffic.sim",
    "com.traffic.sim.plugin"
})
@EnableConfigurationProperties
public class TrafficSimApplication {
    
    public static void main(String[] args) {
        try {
            SpringApplication.run(TrafficSimApplication.class, args);
            log.info("========================================");
            log.info("Traffic Simulation Server Started!");
            log.info("========================================");
        } catch (Exception e) {
            log.error("Failed to start application", e);
            // 如果是 gRPC 相关错误，给出提示
            if (e.getMessage() != null && e.getMessage().contains("gRPC") || 
                e.getMessage() != null && e.getMessage().contains("python-service")) {
                log.warn("gRPC service initialization failed. " +
                    "Please ensure Python gRPC service is running, or set 'grpc.client.python-service.enabled=false' " +
                    "to disable gRPC client and use fallback responses.");
            }
            throw e;
        }
    }
}

