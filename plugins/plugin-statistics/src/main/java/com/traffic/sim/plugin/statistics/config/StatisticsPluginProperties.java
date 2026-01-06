package com.traffic.sim.plugin.statistics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Statistics Plugin Configuration Properties
 * 
 * @author traffic-sim
 */
@Data
@ConfigurationProperties(prefix = "plugin.statistics")
public class StatisticsPluginProperties {
    
    /**
     * 异步处理配置
     */
    private AsyncConfig async = new AsyncConfig();
    
    /**
     * 性能配置
     */
    private PerformanceConfig performance = new PerformanceConfig();
    
    /**
     * 计算器配置
     */
    private CalculatorsConfig calculators = new CalculatorsConfig();
    
    @Data
    public static class AsyncConfig {
        /**
         * 核心线程数
         */
        private int corePoolSize = 4;
        
        /**
         * 最大线程数
         */
        private int maxPoolSize = 8;
        
        /**
         * 队列容量
         */
        private int queueCapacity = 100;
    }
    
    @Data
    public static class PerformanceConfig {
        /**
         * 最大处理时间（毫秒）
         */
        private long maxProcessingTimeMs = 100;
        
        /**
         * 是否启用缓存
         */
        private boolean enableCaching = true;
        
        /**
         * 缓存大小
         */
        private int cacheSize = 1000;
    }
    
    @Data
    public static class CalculatorsConfig {
        /**
         * 启用的计算器列表
         */
        private List<String> enabled = new ArrayList<>(Arrays.asList(
            "speed", "acceleration", "in-out", "flow", 
            "queue", "stop", "delay"
        ));
    }
}

