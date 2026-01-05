package com.traffic.sim.plugin.engine.manager.config;

import com.traffic.sim.plugin.engine.manager.websocket.EngineWebSocketHandler;
import com.traffic.sim.plugin.engine.manager.websocket.FrontendWebSocketHandler;
import lombok.RequiredArgsConstructor;
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
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final FrontendWebSocketHandler frontendWebSocketHandler;
    private final EngineWebSocketHandler engineWebSocketHandler;
    private final EngineManagerProperties properties;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 前端WebSocket连接
        var frontendRegistration = registry
                .addHandler(frontendWebSocketHandler, properties.getWebsocket().getFrontendPath())
                .setAllowedOrigins(properties.getWebsocket().getAllowedOrigins().toArray(new String[0]));
        
        if (properties.getWebsocket().isSockjsEnabled()) {
            frontendRegistration.withSockJS();
        }
        
        // 仿真引擎WebSocket连接
        registry.addHandler(engineWebSocketHandler, properties.getWebsocket().getEnginePath())
                .setAllowedOrigins(properties.getWebsocket().getAllowedOrigins().toArray(new String[0]));
    }
}

