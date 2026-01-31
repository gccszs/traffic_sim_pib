package com.traffic.sim.plugin.replay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 回放配置属性
 * 
 * @author traffic-sim
 */
@Configuration
@ConfigurationProperties(prefix = "plugin.replay")
@Data
public class ReplayProperties {
    
    /**
     * SSE 回放配置
     */
    private SseConfig sse = new SseConfig();
    
    @Data
    public static class SseConfig {
        /**
         * SSE 回放基础延迟时间（毫秒）
         * 每推送一步数据的基础延迟，实际延迟 = baseDelayMs / speed
         * 例如：baseDelayMs=100, speed=2.0, 实际延迟=50ms
         */
        private long baseDelayMs = 100;
        
        /**
         * SSE 连接超时时间（毫秒）
         * 默认 30 分钟
         */
        private long timeoutMs = 1800000;
        
        /**
         * 暂停时的状态检查间隔（毫秒）
         * 避免 CPU 空转
         */
        private long pauseCheckIntervalMs = 100;
    }
}

