package com.simeng.pib.config;

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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 前端WebSocket连接
        registry.addHandler(frontendWebSocketHandler, "/ws/frontend")
                .setAllowedOrigins("http://127.0.0.1:7142", "http://localhost:7142","http://localhost:3822","http://127.0.0.1:3822");

        // 仿真引擎WebSocket连接
        registry.addHandler(engineWebSocketHandler, "/ws/exe/{exe_id}")
                .setAllowedOrigins("*");
    }
}
