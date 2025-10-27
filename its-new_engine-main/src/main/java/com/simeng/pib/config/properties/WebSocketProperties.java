package com.simeng.pib.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "websocket")
@Data
public class WebSocketProperties {
    
    private ServerConfig server;
    private ClientConfig client;
    
    @Data
    public static class ServerConfig {
        private String frontendPath;
        private String enginePath;
        private String[] allowedOrigins;
    }
    
    @Data
    public static class ClientConfig {
        private RemoteService front;
        private RemoteService engine;
    }
    
    @Data
    public static class RemoteService {
        private String remoteUrl;  // 远程服务的完整WebSocket URL
        private ReconnectConfig reconnect;
    }
    
    @Data
    public static class ReconnectConfig {
        private boolean enabled;
        private int maxAttempts;
        private long backoffPeriod;
    }
}