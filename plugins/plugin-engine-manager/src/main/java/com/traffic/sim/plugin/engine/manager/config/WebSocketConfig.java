package com.traffic.sim.plugin.engine.manager.config;

import com.traffic.sim.plugin.engine.manager.websocket.EngineWebSocketHandler;
import com.traffic.sim.plugin.engine.manager.websocket.FrontendWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 
 * @author traffic-sim
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final FrontendWebSocketHandler frontendWebSocketHandler;
    private final EngineWebSocketHandler engineWebSocketHandler;
    private final EngineManagerProperties properties;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String frontendPath = properties.getWebsocket().getFrontendPath();
        String enginePath = properties.getWebsocket().getEnginePath();
        String[] allowedOrigins = properties.getWebsocket().getAllowedOrigins().toArray(new String[0]);
        boolean sockjsEnabled = properties.getWebsocket().isSockjsEnabled();
        
        log.info("========== WebSocket Configuration ==========");
        log.info("Frontend path: {}", frontendPath);
        log.info("Engine path: {}", enginePath);
        log.info("Allowed origins: {}", String.join(", ", allowedOrigins));
        log.info("SockJS enabled: {}", sockjsEnabled);
        log.info("=============================================");
        
        // 前端WebSocket连接
        var frontendRegistration = registry
                .addHandler(frontendWebSocketHandler, frontendPath)
                .setAllowedOriginPatterns(allowedOrigins);  // 使用 Patterns 支持通配符
        
        if (sockjsEnabled) {
            frontendRegistration.withSockJS();
            log.info("Frontend WebSocket: SockJS enabled");
        }
        
        // 仿真引擎WebSocket连接（不使用SockJS，因为引擎是原生WebSocket客户端）
        registry.addHandler(engineWebSocketHandler, enginePath)
                .setAllowedOriginPatterns(allowedOrigins);  // 使用 Patterns 支持通配符
        
        log.info("WebSocket handlers registered successfully!");
    }
}

