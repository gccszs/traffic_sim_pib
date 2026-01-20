package com.traffic.sim.plugin.engine.manager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 引擎管理插件配置属性
 * 
 * @author traffic-sim
 */
@Data
@ConfigurationProperties(prefix = "plugin.engine-manager")
public class EngineManagerProperties {
    
    private WebSocketConfig websocket = new WebSocketConfig();
    private SessionConfig session = new SessionConfig();
    private MessageConfig message = new MessageConfig();
    
    @Data
    public static class WebSocketConfig {
        // 注意：Spring WebSocket 使用 Ant-style 路径匹配，不支持 {exe_id} 语法
        // 使用 /* 匹配单层路径
        private String frontendPath = "/ws/frontend/*";
        private String enginePath = "/ws/exe/*";
        private List<String> allowedOrigins = new ArrayList<>(Arrays.asList("*"));
        private boolean sockjsEnabled = true;
    }
    
    @Data
    public static class SessionConfig {
        private long timeout = 3600; // 会话超时时间（秒）
        private long cleanupInterval = 300; // 清理间隔（秒）
    }
    
    @Data
    public static class MessageConfig {
        private long maxSize = 10485760; // 最大消息大小（10MB）
        private long timeout = 30000; // 消息处理超时（毫秒）
    }
}

