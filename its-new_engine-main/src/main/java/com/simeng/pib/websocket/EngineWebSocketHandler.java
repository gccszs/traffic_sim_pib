package com.simeng.pib.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simeng.pib.model.SimInfo;
import com.simeng.pib.model.dto.WebSocketInfo;
import com.simeng.pib.service.impl.SessionServiceImpl;
import com.simeng.pib.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 仿真引擎WebSocket处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EngineWebSocketHandler implements WebSocketHandler {

    private final SessionServiceImpl sessionServiceImpl;
    private FrontendWebSocketHandler frontendWebSocketHandler;

    @Autowired
    private void setFrontendWebSocketHandler(FrontendWebSocketHandler frontendWebSocketHandler) {
        this.frontendWebSocketHandler = frontendWebSocketHandler;
    }

    private final ObjectMapper objectMapper;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String exeId = extractExeId(session);
        log.info("Engine WebSocket connection established for session: {}", exeId);

        if (exeId != null && sessionServiceImpl.sessionExists(exeId)) {
            SimInfo simInfo = sessionServiceImpl.getSessionInfo(exeId);
            simInfo.setSimeng_connection(session);
            sessionServiceImpl.updateSessionInfo(exeId, simInfo);
        } else {
            log.warn("Invalid exe ID in WebSocket connection: {}", exeId);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid exe ID"));
        }
    }


    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage textMessage) {
            handleTextMessage(session, textMessage);
        }else{
            throw new Exception();
        }
    }

    private void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String exeId = extractExeId(session);

        if (exeId == null || !sessionServiceImpl.sessionExists(exeId)) {
            log.warn("Message from invalid session: {}", exeId);
            return;
        }

        try {
            WebSocketInfo wsMessage = objectMapper.readValue(message.getPayload(), WebSocketInfo.class);
            log.debug("Received message from engine: {}", wsMessage);

            SimInfo simInfo = sessionServiceImpl.getSessionInfo(exeId);

            if ("frontend".equals(wsMessage.getType())) {
                // 转发给前端
                if (simInfo.isFrontend_init_ok()) {
                    frontendWebSocketHandler.sendMessageToFrontend(exeId, wsMessage);
                } else {
                    sendErrorMessage(session, "The frontend is not initialized, please try again later.");
                }
            } else if ("backend".equals(wsMessage.getType())) {
                handleBackendMessage(session, exeId, wsMessage, simInfo);
            }

        } catch (Exception e) {
            log.error("Error handling engine message", e);
            sendErrorMessage(session, "Error processing message");
        }
    }

    private void handleBackendMessage(WebSocketSession session, String exeId,
                                      WebSocketInfo wsMessage, SimInfo simInfo) throws IOException {
        if ("hello".equals(wsMessage.getOpe())) {
            // 引擎初始化
            WebSocketInfo hiInfo = new WebSocketInfo("eng", "hi", TimeUtils.getCurrentTimestampMillis());
            sendMessage(session, hiInfo);

            simInfo.setSimeng_init_ok(true);
            sessionServiceImpl.updateSessionInfo(exeId, simInfo);

            // 如果前端已经连接，通知前端引擎已就绪
            if (simInfo.isFrontend_init_ok()) {
                WebSocketInfo engOkMessage = new WebSocketInfo("frontend", "eng_ok", TimeUtils.getCurrentTimestampMillis());
                frontendWebSocketHandler.sendMessageToFrontend(exeId, engOkMessage);
            }
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMsg) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("msg", errorMsg);

        WebSocketInfo errorMessage = new WebSocketInfo("eng", "err", TimeUtils.getCurrentTimestampMillis());
        errorMessage.setData(Map.of("msg", errorMsg));

        sendMessage(session, errorMessage);
    }

    private void sendMessage(WebSocketSession session, WebSocketInfo message) throws IOException {
        if (session.isOpen()) {
            String messageJson = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(messageJson));
            log.debug("Sent message to engine: {}", messageJson);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Engine WebSocket transport error", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String exeId = extractExeId(session);
        log.info("Engine WebSocket connection closed for session: {}, status: {}", exeId, closeStatus);

        if (exeId != null && sessionServiceImpl.sessionExists(exeId)) {
            SimInfo simInfo = sessionServiceImpl.getSessionInfo(exeId);
            simInfo.setSimeng_connection(null);
            simInfo.setSimeng_init_ok(false);
            sessionServiceImpl.updateSessionInfo(exeId, simInfo);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 从WebSocket会话中提取执行ID
     */
    private String extractExeId(WebSocketSession session) {
        String path = session.getUri().getPath();
        // 从 /ws/exe/{exe_id} 中提取exe_id
        String[] pathParts = path.split("/");
        if (pathParts.length >= 3) {
            return pathParts[3];
        }
        return null;
    }

    /**
     * 转发消息给仿真引擎
     */
    public void forwardToEngine(String sessionId, WebSocketInfo message) {
        SimInfo simInfo = sessionServiceImpl.getSessionInfo(sessionId);
        if (simInfo != null && simInfo.getSimeng_connection() != null) {
            try {
                sendMessage(simInfo.getSimeng_connection(), message);
            } catch (IOException e) {
                log.error("Failed to forward message to engine session: {}", sessionId, e);
            }
        } else {
            log.warn("No engine connection for session: {}", sessionId);
        }
    }
}
