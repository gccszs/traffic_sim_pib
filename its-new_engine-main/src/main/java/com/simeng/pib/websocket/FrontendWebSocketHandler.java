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
 * 前端WebSocket处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FrontendWebSocketHandler implements WebSocketHandler {

    private final SessionServiceImpl sessionServiceImpl;

    private EngineWebSocketHandler engineWebSocketHandler;

    @Autowired
    private void setEngineWebSocketHandler(EngineWebSocketHandler engineWebSocketHandler) {
        this.engineWebSocketHandler = engineWebSocketHandler;
    }

    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String cookieId = extractCookieId(session);
        log.info("Frontend WebSocket connection established for session: {}", cookieId);

        if (cookieId != null && sessionServiceImpl.sessionExists(cookieId)) {
            SimInfo simInfo = sessionServiceImpl.getSessionInfo(cookieId);
            simInfo.setFrontend_connection(session);
            sessionServiceImpl.updateSessionInfo(cookieId, simInfo);
        } else {
            log.warn("Invalid session ID in WebSocket connection: {}", cookieId);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid session ID"));
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage message) throws Exception {
        if (message instanceof TextMessage textMessage) {
            handleTextMessage(session, textMessage);
        }
    }

    private void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String cookieId = extractCookieId(session);

        if (cookieId == null || !sessionServiceImpl.sessionExists(cookieId)) {
            log.warn("Message from invalid session: {}", cookieId);
            return;
        }

        try {
            log.info("WebSocketInfo：”+{}",message.getPayload());
            WebSocketInfo wsMessage = objectMapper.readValue(message.getPayload(), WebSocketInfo.class);
            log.debug("Received message from frontend: {}", wsMessage);

            SimInfo simInfo = sessionServiceImpl.getSessionInfo(cookieId);

            if ("eng".equals(wsMessage.getType())) {
                // 转发给仿真引擎
                if (simInfo.isSimeng_init_ok()) {
                    engineWebSocketHandler.forwardToEngine(cookieId, wsMessage);
                } else {
                    sendErrorMessage(session, "The engine is not initialized, please try again later.");
                }
            } else if ("backend".equals(wsMessage.getType())) {
                handleBackendMessage(session, cookieId, wsMessage, simInfo);
            }

        } catch (Exception e) {
            log.error("Error handling frontend message", e);
            sendErrorMessage(session, "Error processing message");
        }
    }

    private void handleBackendMessage(WebSocketSession session, String cookieId,
                                      WebSocketInfo wsMessage, SimInfo simInfo) throws IOException {
        if ("hello".equals(wsMessage.getOpe())) {
            // 前端初始化
            WebSocketInfo hiMessage = new WebSocketInfo("frontend", "hi", TimeUtils.getCurrentTimestampMillis());
            sendMessage(session, hiMessage);

            simInfo.setFrontend_init_ok(true);
            sessionServiceImpl.updateSessionInfo(cookieId, simInfo);

            // 如果引擎已经初始化，通知前端
            if (simInfo.isSimeng_init_ok()) {
                WebSocketInfo engOkMessage = new WebSocketInfo("frontend", "eng_ok", TimeUtils.getCurrentTimestampMillis());
                sendMessage(session, engOkMessage);
            }
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMsg) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("msg", errorMsg);

        WebSocketInfo errorMessage = new WebSocketInfo("frontend", "err", TimeUtils.getCurrentTimestampMillis());
        errorMessage.setData(Map.of("msg", errorMsg));

        sendMessage(session, errorMessage);
    }

    private void sendMessage(WebSocketSession session, WebSocketInfo message) throws IOException {
        if (session.isOpen()) {
            String messageJson = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(messageJson));
            log.debug("Sent message to frontend: {}", messageJson);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Frontend WebSocket transport error", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String cookieId = extractCookieId(session);
        log.info("Frontend WebSocket connection closed for session: {}, status: {}", cookieId, closeStatus);

        if (cookieId != null && sessionServiceImpl.sessionExists(cookieId)) {
            SimInfo simInfo = sessionServiceImpl.getSessionInfo(cookieId);
            simInfo.setFrontend_connection(null);
            simInfo.setFrontend_init_ok(false);
            sessionServiceImpl.updateSessionInfo(cookieId, simInfo);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 从WebSocket会话中提取Cookie ID
     */
    private String extractCookieId(WebSocketSession session) {
        // 从URI中提取cookie ID，或者从headers中提取
        // 这里简化处理，实际应该从Cookie中提取
        String cookie = session.getHandshakeHeaders().getFirst("Cookie");;
        log.info("extract CookieId {}",cookie);
        if (cookie != null && cookie.startsWith("id=")) {
            return cookie.substring(3);
        }

        return null;
    }

    /**
     * 向指定会话的前端发送消息
     */
    public void sendMessageToFrontend(String sessionId, WebSocketInfo message) {
        SimInfo simInfo = sessionServiceImpl.getSessionInfo(sessionId);
        if (simInfo != null && simInfo.getFrontend_connection() != null) {
            try {
                sendMessage(simInfo.getFrontend_connection(), message);
            } catch (IOException e) {
                log.error("Failed to send message to frontend session: {}", sessionId, e);
            }
        }
    }
}
