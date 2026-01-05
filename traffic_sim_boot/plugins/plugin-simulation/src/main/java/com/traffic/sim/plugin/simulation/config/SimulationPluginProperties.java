package com.traffic.sim.plugin.simulation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 仿真插件配置属性
 * 
 * @author traffic-sim
 */
@Data
@Component
@ConfigurationProperties(prefix = "plugin.simulation")
public class SimulationPluginProperties {
    
    /**
     * Python服务配置
     */
    private PythonServiceConfig pythonService = new PythonServiceConfig();
    
    @Data
    public static class PythonServiceConfig {
        /** 是否启用 */
        private boolean enabled = true;
        
        /** gRPC端口 */
        private int grpcPort = 50051;
        
        /** 超时时间（毫秒） */
        private long timeout = 300000;
    }
}

