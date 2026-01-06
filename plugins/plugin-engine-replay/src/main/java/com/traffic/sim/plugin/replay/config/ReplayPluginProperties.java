package com.traffic.sim.plugin.replay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 回放插件配置属性
 * 
 * @author traffic-sim
 */
@Data
@ConfigurationProperties(prefix = "plugin.replay")
public class ReplayPluginProperties {
    
    /**
     * 存储配置
     */
    private StorageConfig storage = new StorageConfig();
    
    /**
     * 回放配置
     */
    private ReplayConfig replay = new ReplayConfig();
    
    @Data
    public static class StorageConfig {
        /**
         * 数据压缩启用
         */
        private boolean compressionEnabled = true;
        
        /**
         * 压缩算法
         */
        private String compressionAlgorithm = "gzip";
        
        /**
         * 数据保留时间（天）
         */
        private int retentionDays = 30;
    }
    
    @Data
    public static class ReplayConfig {
        /**
         * 默认播放速度（倍速）
         */
        private double defaultSpeed = 1.0;
        
        /**
         * 最大播放速度
         */
        private double maxSpeed = 10.0;
        
        /**
         * 最小播放速度
         */
        private double minSpeed = 0.1;
        
        /**
         * 批量查询步数
         */
        private int batchStepSize = 100;
    }
}

