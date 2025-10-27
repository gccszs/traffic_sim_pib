package com.simeng.pib.config;

import com.simeng.pib.config.properties.WebSocketProperties;
import com.simeng.pib.websocket.EngineWebSocketHandler;
import com.simeng.pib.websocket.FrontendWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final FrontendWebSocketHandler frontendWebSocketHandler;
    private final EngineWebSocketHandler engineWebSocketHandler;

    private final WebSocketProperties webSocketProperties;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 前端WebSocket连接
        registry.addHandler(frontendWebSocketHandler, webSocketProperties.getServer().getFrontendPath())
                .setAllowedOrigins(webSocketProperties.getServer().getAllowedOrigins());

        // 仿真引擎WebSocket连接
        registry.addHandler(engineWebSocketHandler, webSocketProperties.getServer().getEnginePath())
                .setAllowedOrigins(webSocketProperties.getServer().getAllowedOrigins());
    }
}
