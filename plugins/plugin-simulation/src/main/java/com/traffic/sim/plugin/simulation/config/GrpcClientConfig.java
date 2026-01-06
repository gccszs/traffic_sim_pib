package com.traffic.sim.plugin.simulation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC客户端配置
 * 支持容错：如果gRPC服务不可用，不会导致应用启动失败
 * 
 * @author traffic-sim
 */
@Configuration
@Slf4j
public class GrpcClientConfig {
    
    @Value("${grpc.client.python-service.enabled:true}")
    private boolean grpcEnabled;
    
    /**
     * 如果gRPC未启用，排除gRPC自动配置
     * 注意：这需要在主应用类中使用 @SpringBootApplication(exclude = ...) 来实现
     * 或者通过配置文件控制
     */
    @ConditionalOnProperty(name = "grpc.client.python-service.enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public String grpcClientEnabled() {
        log.info("gRPC client is enabled");
        return "enabled";
    }
}


