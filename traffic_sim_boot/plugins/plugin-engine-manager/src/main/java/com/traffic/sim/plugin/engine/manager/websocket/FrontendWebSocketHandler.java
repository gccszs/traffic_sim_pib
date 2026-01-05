package com.traffic.sim.plugin.engine.manager.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.sim.common.model.SimInfo;
import com.traffic.sim.common.model.WebSocketInfo;
import com.traffic.sim.common.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

/**
 * 前端WebSocket处理器
 * 路径: /ws/frontend
 * 认证: Cookie中的id字段（session_id）
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FrontendWebSocketHandler implements WebSocketHandler {
    
    private final SessionService sessionService;
    private EngineWebSocketHandler engineWebSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 设置引擎WebSocket处理器（解决循环依赖）
     */
    public void setEngineWebSocketHandler(EngineWebSocketHandler engineWebSocketHandler) {
        this.engineWebSocketHandler = engineWebSocketHandler;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session);
        log.info("Frontend WebSocket connected: {}", sessionId);
        
        SimInfo simInfo = sessionService.getSessionInfo(sessionId);
        if (simInfo != null) {
            simInfo.setFrontendConnection(session);
            sessionService.updateSessionInfo(sessionId, simInfo);
        } else {
            log.warn("Session not found for frontend connection: {}", sessionId);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid session ID"));
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage textMessage) {
            handleTextMessage(session, textMessage);
        }
    }
    
    private void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = extractSessionId(session);
        
        try {
            WebSocketInfo wsMessage = objectMapper.readValue(
                message.getPayload(), WebSocketInfo.class);
            
            SimInfo simInfo = sessionService.getSessionInfo(sessionId);
            
            if ("eng".equals(wsMessage.getType())) {
                // 转发给引擎
                if (simInfo != null && simInfo.isEngineInitialized() && engineWebSocketHandler != null) {
                    engineWebSocketHandler.forwardToEngine(sessionId, wsMessage);
                } else {
                    sendErrorMessage(session, "Engine not initialized");
                }
            } else if ("backend".equals(wsMessage.getType())) {
                // 处理后端消息（如初始化）
                handleBackendMessage(session, sessionId, wsMessage, simInfo);
            }
        } catch (Exception e) {
            log.error("Error handling frontend message", e);
            sendErrorMessage(session, "Error processing message: " + e.getMessage());
        }
    }
    
    /**
     * 处理后端消息
     */
    private void handleBackendMessage(WebSocketSession session, String sessionId, 
                                     WebSocketInfo wsMessage, SimInfo simInfo) {
        if (simInfo == null) {
            sendErrorMessage(session, "Session not found");
            return;
        }
        
        if ("hello".equals(wsMessage.getOpe())) {
            // 前端初始化握手
            simInfo.setFrontendInitOk(true);
            sessionService.updateSessionInfo(sessionId, simInfo);
            
            // 发送响应
            WebSocketInfo response = new WebSocketInfo("frontend", "hi", System.currentTimeMillis());
            sendMessageToFrontend(sessionId, response);
            log.info("Frontend initialized for session: {}", sessionId);
        }
    }
    
    /**
     * 发送消息给前端
     */
    public void sendMessageToFrontend(String sessionId, WebSocketInfo message) {
        SimInfo simInfo = sessionService.getSessionInfo(sessionId);
        if (simInfo != null && simInfo.getFrontendConnection() != null) {
            WebSocketSession session = simInfo.getFrontendConnection();
            if (session.isOpen()) {
                try {
                    String json = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    log.error("Failed to send message to frontend for session: {}", sessionId, e);
                }
            }
        }
    }
    
    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            WebSocketInfo errorMsg = new WebSocketInfo("frontend", "err", System.currentTimeMillis());
            Map<String, Object> data = Map.of("message", errorMessage);
            errorMsg.setData(data);
            String json = objectMapper.writeValueAsString(errorMsg);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Failed to send error message", e);
        }
    }
    
    /**
     * 从Cookie中提取session ID
     */
    private String extractSessionId(WebSocketSession session) {
        // 从Cookie中获取id字段
        String cookieHeader = session.getHandshakeHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "id".equals(parts[0].trim())) {
                    return parts[1].trim();
                }
            }
        }
        return null;
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for frontend session", exception);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = extractSessionId(session);
        log.info("Frontend WebSocket closed: {}, status: {}", sessionId, closeStatus);
        
        SimInfo simInfo = sessionService.getSessionInfo(sessionId);
        if (simInfo != null) {
            simInfo.setFrontendConnection(null);
            simInfo.setFrontendInitOk(false);
            sessionService.updateSessionInfo(sessionId, simInfo);
        }
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}

