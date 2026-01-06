package com.traffic.sim.plugin.engine.manager.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.sim.common.model.SimInfo;
import com.traffic.sim.common.model.StatisticsData;
import com.traffic.sim.common.model.WebSocketInfo;
import com.traffic.sim.common.service.SessionService;
import com.traffic.sim.common.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

/**
 * 引擎WebSocket处理器
 * 路径: /ws/exe/{exe_id}
 * 参数: exe_id = session_id
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EngineWebSocketHandler implements WebSocketHandler {
    
    private final SessionService sessionService;
    private FrontendWebSocketHandler frontendWebSocketHandler;
    private StatisticsService statisticsService; // 可选依赖，由 plugin-statistics 模块提供
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 设置前端WebSocket处理器（解决循环依赖）
     */
    public void setFrontendWebSocketHandler(FrontendWebSocketHandler frontendWebSocketHandler) {
        this.frontendWebSocketHandler = frontendWebSocketHandler;
    }
    
    /**
     * 设置统计服务（可选，由 plugin-statistics 模块提供）
     */
    public void setStatisticsService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String exeId = extractExeId(session);
        log.info("Engine WebSocket connected: {}", exeId);
        
        SimInfo simInfo = sessionService.getSessionInfo(exeId);
        if (simInfo != null) {
            simInfo.setSimengConnection(session);
            sessionService.updateSessionInfo(exeId, simInfo);
        } else {
            log.warn("Session not found for engine connection: {}", exeId);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid exe ID"));
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage textMessage) {
            handleTextMessage(session, textMessage);
        }
    }
    
    private void handleTextMessage(WebSocketSession session, TextMessage message) {
        String exeId = extractExeId(session);
        
        try {
            WebSocketInfo wsMessage = objectMapper.readValue(
                message.getPayload(), WebSocketInfo.class);
            
            SimInfo simInfo = sessionService.getSessionInfo(exeId);
            
            if ("frontend".equals(wsMessage.getType())) {
                // 需要转发给前端
                if ("sim_data".equals(wsMessage.getOpe())) {
                    // 仿真数据，需要先进行统计处理
                    processSimulationData(exeId, wsMessage, simInfo);
                } else {
                    // 其他消息直接转发
                    if (simInfo != null && simInfo.isFrontendInitialized() && frontendWebSocketHandler != null) {
                        frontendWebSocketHandler.sendMessageToFrontend(exeId, wsMessage);
                    }
                }
            } else if ("backend".equals(wsMessage.getType())) {
                // 处理后端消息（如初始化）
                handleBackendMessage(session, exeId, wsMessage, simInfo);
            }
        } catch (Exception e) {
            log.error("Error handling engine message", e);
            sendErrorMessage(session, "Error processing message: " + e.getMessage());
        }
    }
    
    /**
     * 处理仿真数据：调用统计服务，然后转发给前端
     */
    private void processSimulationData(String sessionId, WebSocketInfo wsMessage, SimInfo simInfo) {
        try {
            // 1. 调用统计服务处理数据（如果可用）
            Map<String, Object> simData = wsMessage.getData();
            if (simData == null) {
                log.warn("Simulation data is null for session: {}", sessionId);
                return;
            }
            
            StatisticsData statistics = null;
            if (statisticsService != null) {
                try {
                    statistics = statisticsService.processSimulationStep(simData);
                } catch (Exception e) {
                    log.warn("Statistics service failed, forwarding raw data: {}", e.getMessage());
                }
            }
            
            // 2. 构建统计消息
            WebSocketInfo statsMessage = new WebSocketInfo(
                "frontend", 
                "statistics", 
                System.currentTimeMillis()
            );
            
            // 3. 序列化统计数据
            Map<String, Object> statsData = new HashMap<>();
            if (statistics != null) {
                statsData.put("step", statistics.getStep());
                statsData.put("timestamp", statistics.getTimestamp());
                statsData.put("vehicleCount", statistics.getVehicleCount());
                statsData.put("averageSpeed", statistics.getAverageSpeed());
                statsData.put("congestionIndex", statistics.getCongestionIndex());
                statsData.put("signalStates", statistics.getSignalStates());
                if (statistics.getCustom() != null) {
                    statsData.putAll(statistics.getCustom());
                }
            }
            statsMessage.setData(statsData);
            
            // 4. 转发给前端
            if (simInfo != null && simInfo.isFrontendInitialized() && frontendWebSocketHandler != null) {
                frontendWebSocketHandler.sendMessageToFrontend(sessionId, statsMessage);
            }
        } catch (Exception e) {
            log.error("Error processing simulation data for session: {}", sessionId, e);
            // 即使统计处理失败，也尝试转发原始数据
            if (simInfo != null && simInfo.isFrontendInitialized() && frontendWebSocketHandler != null) {
                frontendWebSocketHandler.sendMessageToFrontend(sessionId, wsMessage);
            }
        }
    }
    
    /**
     * 处理后端消息
     */
    private void handleBackendMessage(WebSocketSession session, String exeId, 
                                     WebSocketInfo wsMessage, SimInfo simInfo) {
        if (simInfo == null) {
            sendErrorMessage(session, "Session not found");
            return;
        }
        
        if ("hello".equals(wsMessage.getOpe())) {
            // 引擎初始化握手
            simInfo.setSimengInitOk(true);
            sessionService.updateSessionInfo(exeId, simInfo);
            
            // 发送响应
            WebSocketInfo response = new WebSocketInfo("backend", "hi", System.currentTimeMillis());
            sendMessageToEngine(exeId, response);
            log.info("Engine initialized for session: {}", exeId);
        }
    }
    
    /**
     * 转发消息给引擎
     */
    public void forwardToEngine(String sessionId, WebSocketInfo message) {
        SimInfo simInfo = sessionService.getSessionInfo(sessionId);
        if (simInfo != null && simInfo.getSimengConnection() != null) {
            WebSocketSession session = simInfo.getSimengConnection();
            if (session.isOpen()) {
                try {
                    String json = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    log.error("Failed to forward message to engine for session: {}", sessionId, e);
                }
            }
        }
    }
    
    /**
     * 发送消息给引擎
     */
    private void sendMessageToEngine(String exeId, WebSocketInfo message) {
        SimInfo simInfo = sessionService.getSessionInfo(exeId);
        if (simInfo != null && simInfo.getSimengConnection() != null) {
            WebSocketSession session = simInfo.getSimengConnection();
            if (session.isOpen()) {
                try {
                    String json = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    log.error("Failed to send message to engine for session: {}", exeId, e);
                }
            }
        }
    }
    
    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            WebSocketInfo errorMsg = new WebSocketInfo("backend", "err", System.currentTimeMillis());
            Map<String, Object> data = Map.of("message", errorMessage);
            errorMsg.setData(data);
            String json = objectMapper.writeValueAsString(errorMsg);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Failed to send error message", e);
        }
    }
    
    /**
     * 从URI路径中提取exe_id（session_id）
     */
    private String extractExeId(WebSocketSession session) {
        String uri = session.getUri().toString();
        // URI格式: ws://host/ws/exe/{exe_id}
        String[] parts = uri.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("exe".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1].split("\\?")[0]; // 去掉查询参数
            }
        }
        return null;
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for engine session", exception);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String exeId = extractExeId(session);
        log.info("Engine WebSocket closed: {}, status: {}", exeId, closeStatus);
        
        SimInfo simInfo = sessionService.getSessionInfo(exeId);
        if (simInfo != null) {
            simInfo.setSimengConnection(null);
            simInfo.setSimengInitOk(false);
            sessionService.updateSessionInfo(exeId, simInfo);
        }
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}

