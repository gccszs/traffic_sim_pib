package com.traffic.sim.plugin.engine.manager.config;

import com.traffic.sim.common.service.StatisticsService;
import com.traffic.sim.plugin.engine.manager.websocket.EngineWebSocketHandler;
import com.traffic.sim.plugin.engine.manager.websocket.FrontendWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * WebSocket处理器配置类
 * 用于解决循环依赖问题和可选依赖注入
 * 
 * @author traffic-sim
 */
@Configuration
@RequiredArgsConstructor
public class WebSocketHandlerConfig {
    
    private final FrontendWebSocketHandler frontendWebSocketHandler;
    private final EngineWebSocketHandler engineWebSocketHandler;
    
    @Autowired(required = false)
    private StatisticsService statisticsService;
    
    @PostConstruct
    public void init() {
        // 解决循环依赖
        frontendWebSocketHandler.setEngineWebSocketHandler(engineWebSocketHandler);
        engineWebSocketHandler.setFrontendWebSocketHandler(frontendWebSocketHandler);
        
        // 注入可选的统计服务（如果 plugin-statistics 模块已加载）
        if (statisticsService != null) {
            engineWebSocketHandler.setStatisticsService(statisticsService);
        }
    }
}

