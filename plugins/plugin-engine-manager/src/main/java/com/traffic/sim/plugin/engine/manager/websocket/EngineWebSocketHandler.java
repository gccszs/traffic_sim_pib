package com.traffic.sim.plugin.engine.manager.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.sim.common.model.SimInfo;
import com.traffic.sim.common.model.StatisticsData;
import com.traffic.sim.common.model.WebSocketInfo;
import com.traffic.sim.common.service.SessionService;
import com.traffic.sim.common.service.StatisticsService;
import com.traffic.sim.plugin.engine.manager.service.SimulationDataCollector;
import com.traffic.sim.plugin.engine.manager.service.SimulationDataPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final SimulationDataCollector dataCollector;
    private final SimulationDataPersistenceService dataPersistenceService;
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

        // 设置会话超时时间为 30 分钟（1800000 毫秒）
        session.setTextMessageSizeLimit(1024 * 1024); // 1MB
        session.setBinaryMessageSizeLimit(1024 * 1024); // 1MB

        // 尝试设置超时时间（如果底层实现支持）
        try {
            // 对于 Tomcat WebSocket，需要通过 NativeWebSocketSession 访问
            if (session instanceof org.springframework.web.socket.adapter.standard.StandardWebSocketSession) {
                org.springframework.web.socket.adapter.standard.StandardWebSocketSession standardSession =
                        (org.springframework.web.socket.adapter.standard.StandardWebSocketSession) session;
                standardSession.getNativeSession().setMaxIdleTimeout(1800000); // 30 分钟
                log.info("Set WebSocket session idle timeout to 1800000ms for session: {}", exeId);
            }
        } catch (Exception e) {
            log.warn("Failed to set session timeout: {}", e.getMessage());
        }

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
                // 收集所有发往前端的消息数据
                if (wsMessage.getData() != null) {
                    dataCollector.addMessageToCurrentStep(exeId, wsMessage.getOpe(), wsMessage.getData());
                }

                // 需要转发给前端
                if ("simdata".equals(wsMessage.getOpe())) {
                    // 仿真数据消息，检查是否是 sim_one_step
                    Map<String, Object> data = wsMessage.getData();
                    if (data != null && "sim_one_step".equals(data.get("pos"))) {
                        // 这是一个仿真步结束的消息，需要添加统计信息并保存数据
                        processSimulationStepEnd(exeId, wsMessage, simInfo);
                    } else {
                        // 其他 simdata 消息直接转发
                        if (simInfo != null && simInfo.isFrontendInitialized() && frontendWebSocketHandler != null) {
                            frontendWebSocketHandler.sendMessageToFrontend(exeId, wsMessage);
                        }
                    }
                } else if ("sim_data".equals(wsMessage.getOpe())) {
                    // 兼容旧版本的 sim_data 消息
                    processSimulationData(exeId, wsMessage, simInfo);
                } else if ("sim_end".equals(wsMessage.getOpe()) || "finished".equals(wsMessage.getOpe())) {
                    // 仿真结束，保存数据到MongoDB
                    handleSimulationEnd(exeId, simInfo);
                    // 转发结束消息
                    if (simInfo != null && simInfo.isFrontendInitialized() && frontendWebSocketHandler != null) {
                        frontendWebSocketHandler.sendMessageToFrontend(exeId, wsMessage);
                    }
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
     * 处理仿真步结束：调用统计服务计算统计信息，添加到消息中，异步保存数据，然后转发给前端
     */
    private void processSimulationStepEnd(String sessionId, WebSocketInfo wsMessage, SimInfo simInfo) {
        try {
            Map<String, Object> data = wsMessage.getData();
            if (data == null) {
                log.warn("Simulation step data is null for session: {}", sessionId);
                return;
            }

            // 提取步数信息
            Object overStepObj = data.get("over_step:");
            Long currentStep;
            if (overStepObj instanceof Number) {
                currentStep = ((Number) overStepObj).longValue();
            } else {
                currentStep = null;
            }

            // 调用统计服务计算统计信息（如果可用）
            Map<String, Object> infoStat = null;
            if (statisticsService != null && currentStep != null) {
                try {
                    // 从收集器中获取当前步的所有消息，构建完整的仿真数据
                    SimulationDataCollector.CurrentStepCollector stepCollector =
                            dataCollector.getCurrentStepCollector(sessionId);

                    if (stepCollector != null && !stepCollector.getMessages().isEmpty()) {
                        // 构建包含所有车辆数据的完整数据结构
                        Map<String, Object> completeStepData = buildCompleteStepData(
                                currentStep, stepCollector.getMessages());

                        // 调用统计服务处理当前步
                        StatisticsData statistics = statisticsService.processSimulationStep(completeStepData);

                        if (statistics != null) {
                            // 构建 infoStat 对象
                            infoStat = new HashMap<>();

                            // 基本统计信息
                            infoStat.put("speed_min", statistics.getMinSpeed() != null ? statistics.getMinSpeed() : 0.0);
                            infoStat.put("speed_max", statistics.getMaxSpeed() != null ? statistics.getMaxSpeed() : 0.0);
                            infoStat.put("speed_ave", statistics.getAverageSpeed() != null ? statistics.getAverageSpeed() : 0.0);
                            infoStat.put("acc_min", statistics.getMinAcceleration() != null ? statistics.getMinAcceleration() : 0.0);
                            infoStat.put("acc_max", statistics.getMaxAcceleration() != null ? statistics.getMaxAcceleration() : 0.0);
                            infoStat.put("acc_ave", statistics.getAverageAcceleration() != null ? statistics.getAverageAcceleration() : 0.0);
                            infoStat.put("car_number", statistics.getVehicleCount() != null ? statistics.getVehicleCount() : 0);
                            infoStat.put("car_in", statistics.getVehiclesIn() != null ? statistics.getVehiclesIn() : 0);
                            infoStat.put("car_out", statistics.getVehiclesOut() != null ? statistics.getVehiclesOut() : 0);
                            infoStat.put("low_speed", statistics.getLowSpeedCount() != null ? statistics.getLowSpeedCount() : 0);
                            infoStat.put("jam_index", statistics.getCongestionIndex() != null ? statistics.getCongestionIndex() : 0.0);

                            // 全局统计信息
                            Map<String, Object> global = new HashMap<>();
                            global.put("cars_in", statistics.getTotalVehiclesIn() != null ? statistics.getTotalVehiclesIn() : 0);
                            global.put("cars_out", statistics.getTotalVehiclesOut() != null ? statistics.getTotalVehiclesOut() : 0);
                            global.put("queue_length_min", statistics.getMinQueueLength() != null ? statistics.getMinQueueLength() : 0.0);
                            global.put("queue_length_max", statistics.getMaxQueueLength() != null ? statistics.getMaxQueueLength() : 0.0);
                            global.put("queue_length_ave", statistics.getAverageQueueLength() != null ? statistics.getAverageQueueLength() : 0.0);
                            global.put("queue_time_min", statistics.getMinQueueTime() != null ? statistics.getMinQueueTime() : 0.0);
                            global.put("queue_time_max", statistics.getMaxQueueTime() != null ? statistics.getMaxQueueTime() : 0.0);
                            global.put("queue_time_ave", statistics.getAverageQueueTime() != null ? statistics.getAverageQueueTime() : 0.0);
                            global.put("stop_max", statistics.getMaxStopCount() != null ? statistics.getMaxStopCount() : 0);
                            global.put("stop_min", statistics.getMinStopCount() != null ? statistics.getMinStopCount() : 0);
                            global.put("stop_ave", statistics.getAverageStopCount() != null ? statistics.getAverageStopCount() : 0.0);
                            global.put("delay_max", statistics.getMaxDelay() != null ? statistics.getMaxDelay() : 0.0);
                            global.put("delay_min", statistics.getMinDelay() != null ? statistics.getMinDelay() : 0.0);
                            global.put("delay_ave", statistics.getAverageDelay() != null ? statistics.getAverageDelay() : 0.0);

                            // 交叉口流量
                            Map<String, Object> crossFlow = new HashMap<>();
                            crossFlow.put("flow_ave", statistics.getAverageCrossFlow() != null ? statistics.getAverageCrossFlow() : 0.0);
                            global.put("cross_flow", crossFlow);

                            // 道路流量
                            Map<String, Object> flow = new HashMap<>();
                            flow.put("flow_RD_ave", statistics.getAverageRoadFlow() != null ? statistics.getAverageRoadFlow() : 0.0);
                            flow.put("flow_LA_ave", statistics.getAverageLaneFlow() != null ? statistics.getAverageLaneFlow() : 0.0);
                            global.put("flow", flow);

                            infoStat.put("global", global);

                            // 将 infoStat 添加到原始消息的 data 中
                            data.put("infoStat", infoStat);

                            log.debug("Added statistics to sim_one_step message for session: {}, step: {}",
                                    sessionId, currentStep);
                        } else {
                            log.debug("Statistics calculation returned null for session: {}, step: {}",
                                    sessionId, currentStep);
                        }
                    } else {
                        log.debug("No messages collected for step {} in session: {}", currentStep, sessionId);
                    }
                } catch (Exception e) {
                    log.error("Statistics service failed for session: {}, step: {}",
                            sessionId, currentStep, e);
                }
            }

            // 完成当前仿真步的数据收集
            if (currentStep != null) {
                SimulationDataCollector.CurrentStepCollector stepCollector =
                        dataCollector.finishCurrentStep(sessionId, currentStep);

                if (stepCollector != null && !stepCollector.getMessages().isEmpty()) {
                    // 异步保存当前仿真步数据到 MongoDB
                    String simulationTaskId = extractSimulationTaskId(simInfo);
                    String userId = extractUserId(simInfo);
                    String taskId = extractTaskId(simInfo);

                    if (simulationTaskId != null && userId != null && taskId != null) {
                        // 构建完整的仿真步数据（包含所有车辆的详细信息）
                        Map<String, Object> completeStepData = buildCompleteStepData(
                                currentStep, stepCollector.getMessages());
                        
                        // 构建 StepData，保存完整的车辆数据用于回放
                        SimulationDataCollector.StepData stepData = new SimulationDataCollector.StepData(
                                currentStep,
                                stepCollector.getTimestamp(),
                                completeStepData,  // ✅ 保存完整的车辆数据（包含所有车辆的位置、速度等）
                                infoStat  // 统计数据
                        );

                        // 异步保存（不阻塞当前线程）
                        dataPersistenceService.saveStepDataAsync(simulationTaskId, userId, taskId, stepData)
                                .thenAccept(success -> {
                                    if (success) {
                                        log.debug("Successfully saved step {} data (vehicles: {}) for task: {}", 
                                                currentStep, 
                                                completeStepData.get("vehicles") != null ? 
                                                    ((List<?>) completeStepData.get("vehicles")).size() : 0,
                                                simulationTaskId);
                                    } else {
                                        log.warn("Failed to save step {} data for task: {}", currentStep, simulationTaskId);
                                    }
                                })
                                .exceptionally(ex -> {
                                    log.error("Error saving step {} data for task: {}", currentStep, simulationTaskId, ex);
                                    return null;
                                });

                        log.debug("Triggered async save for step {} (messages: {}, vehicles: {}) for session: {}",
                                currentStep, 
                                stepCollector.getMessages().size(),
                                completeStepData.get("vehicles") != null ? 
                                    ((List<?>) completeStepData.get("vehicles")).size() : 0,
                                sessionId);
                    }
                }
            }

            // 转发给前端（无论是否成功添加统计信息）
            if (simInfo != null && simInfo.isFrontendInitialized() && frontendWebSocketHandler != null) {
                frontendWebSocketHandler.sendMessageToFrontend(sessionId, wsMessage);
            }

        } catch (Exception e) {
            log.error("Error processing simulation step end for session: {}", sessionId, e);
            // 即使处理失败，也尝试转发原始消息
            if (simInfo != null && simInfo.isFrontendInitialized() && frontendWebSocketHandler != null) {
                frontendWebSocketHandler.sendMessageToFrontend(sessionId, wsMessage);
            }
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

            // 4. 收集仿真数据用于回放
            Long step = extractStep(simData, statsData);
            if (step != null) {
                dataCollector.addStepData(sessionId, step, simData, statsData);
                log.debug("Collected step {} data for session: {}", step, sessionId);
            }

            // 5. 转发给前端
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

            // 发送响应 - type 应该是 "eng"（发给引擎的消息）
            WebSocketInfo response = new WebSocketInfo("eng", "hi", System.currentTimeMillis());
            sendMessageToEngine(exeId, response);
            log.info("Engine initialized for session: {}", exeId);

            // 向前端发送引擎初始化完毕的消息（如果前端已经连接）
            if (simInfo.isFrontendInitialized() && frontendWebSocketHandler != null) {
                WebSocketInfo engOkMsg = new WebSocketInfo("frontend", "eng_ok", System.currentTimeMillis());
                frontendWebSocketHandler.sendMessageToFrontend(exeId, engOkMsg);
                log.info("Notified frontend that engine is ready for session: {}", exeId);
            }
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

        if (simInfo == null) {
            log.error("Cannot send message: SimInfo is null for session: {}", exeId);
            return;
        }

        if (simInfo.getSimengConnection() == null) {
            log.error("Cannot send message: Engine connection is null for session: {}", exeId);
            return;
        }

        WebSocketSession session = simInfo.getSimengConnection();

        if (session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                log.info("Sending JSON to engine [{}]: {}", exeId, json);  // 输出实际的 JSON
                session.sendMessage(new TextMessage(json));
                log.info("Successfully sent message to engine: ope={}, session={}", message.getOpe(), exeId);
            } catch (Exception e) {
                log.error("Failed to send message to engine for session: {}, error: {}", exeId, e.getMessage(), e);
            }
        } else {
            log.error("Cannot send message: Engine session is closed for session: {}", exeId);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            WebSocketInfo errorMsg = new WebSocketInfo("eng", "err", System.currentTimeMillis());
            Map<String, Object> data = Map.of("message", errorMessage);
            errorMsg.setData(data);
            String json = objectMapper.writeValueAsString(errorMsg);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Failed to send error message", e);
        }
    }

    /**
     * 从 URI路径中提取exe_id（session_id）
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

    /**
     * 从仿真数据中提取步数
     */
    private Long extractStep(Map<String, Object> simData, Map<String, Object> statsData) {
        // 先从统计数据中获取
        Object stepObj = statsData.get("step");
        if (stepObj == null) {
            // 再从原始仿真数据中获取
            stepObj = simData.get("step");
        }

        if (stepObj instanceof Number) {
            return ((Number) stepObj).longValue();
        } else if (stepObj instanceof String) {
            try {
                return Long.parseLong((String) stepObj);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse step from string: {}", stepObj);
            }
        }
        return null;
    }

    /**
     * 处理仿真结束，保存数据到MongoDB
     */
    private void handleSimulationEnd(String sessionId, SimInfo simInfo) {
        try {
            log.info("Simulation ended for session: {}, saving data to MongoDB", sessionId);

            // 获取收集的数据
            List<SimulationDataCollector.StepData> stepDataList =
                    dataCollector.getAndClearSessionData(sessionId);

            if (stepDataList.isEmpty()) {
                log.warn("No simulation data collected for session: {}", sessionId);
                return;
            }

            // 从 SimInfo 中获取仿真任务ID、用户ID和任务ID
            String simulationTaskId = extractSimulationTaskId(simInfo);
            String userId = extractUserId(simInfo);
            String taskId = extractTaskId(simInfo);

            if (simulationTaskId == null || userId == null || taskId == null) {
                log.error("Cannot save simulation data: missing required IDs (simulationTaskId: {}, userId: {}, taskId: {})",
                        simulationTaskId, userId, taskId);
                return;
            }

            // 异步保存到MongoDB
            boolean saved = dataPersistenceService.saveSimulationData(simulationTaskId, userId, taskId, stepDataList);

            if (saved) {
                log.info("Successfully saved {} steps of simulation data for task: {}, userId: {}, taskId: {}",
                        stepDataList.size(), simulationTaskId, userId, taskId);
            } else {
                log.error("Failed to save simulation data for task: {}", simulationTaskId);
            }
        } catch (Exception e) {
            log.error("Error handling simulation end for session: {}", sessionId, e);
        }
    }

    /**
     * 从收集的消息中构建完整的仿真步数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildCompleteStepData(Long step, List<Map<String, Object>> messages) {
        Map<String, Object> completeData = new HashMap<>();
        completeData.put("step", step);
        completeData.put("over_step:", step);

        // 使用 Map 来去重车辆（按 id）
        Map<Object, Map<String, Object>> vehicleMap = new HashMap<>();
        List<Map<String, Object>> signals = new ArrayList<>();

        int produceVehCount = 0;
        int vehRunCount = 0;

        // 遍历所有消息，提取车辆和信号灯数据
        for (Map<String, Object> message : messages) {
            String pos = (String) message.get("pos");
            Object result = message.get("result");

            if (pos != null && result != null) {
                if ("veh_run".equals(pos) || "produce_veh".equals(pos)) {
                    // 车辆数据
                    if (result instanceof Map) {
                        Map<String, Object> vehicleData = (Map<String, Object>) result;
                        Object vehicleId = vehicleData.get("id");
                        if (vehicleId != null) {
                            if ("produce_veh".equals(pos)) {
                                produceVehCount++;
                            } else {
                                vehRunCount++;
                            }

                            // 提取关键字段并重命名以匹配解析器期望
                            Map<String, Object> vehicle = new HashMap<>();

                            // 先保留所有原始数据
                            vehicle.putAll(vehicleData);

                            // 然后设置/覆盖关键字段
                            vehicle.put("id", vehicleId);

                            // 优先使用 cur_spd，如果没有则使用 speed
                            Object speed = vehicleData.get("cur_spd");
                            if (speed == null) {
                                speed = vehicleData.get("speed");
                            }
                            vehicle.put("speed", speed);

                            // 计算加速度：(cur_spd - last_spd) / time_step
                            // 假设 time_step = 1 秒
                            Object curSpd = vehicleData.get("cur_spd");
                            Object lastSpd = vehicleData.get("last_spd");

                            if (curSpd instanceof Number && lastSpd instanceof Number) {
                                // veh_run 消息：有速度历史，计算加速度
                                double acceleration = ((Number) curSpd).doubleValue() - ((Number) lastSpd).doubleValue();
                                vehicle.put("acceleration", acceleration);
                            } else if ("produce_veh".equals(pos)) {
                                // produce_veh 消息：首次出现，默认加速度为0
                                vehicle.put("acceleration", 0.0);
                            }

                            vehicle.put("x", vehicleData.get("x"));
                            vehicle.put("y", vehicleData.get("y"));
                            vehicle.put("roadId", vehicleData.get("link_id"));
                            vehicle.put("laneId", vehicleData.get("lane_id"));

                            // 使用 vehicleId 作为 key，后面的数据会覆盖前面的（veh_run 优先于 produce_veh）
                            Map<String, Object> existingVehicle = vehicleMap.get(vehicleId);
                            if (existingVehicle != null) {
                                log.debug("Vehicle {} already exists, merging {} data", vehicleId, pos);
                                // 如果新数据有加速度，使用新数据；否则保留旧数据的加速度
                                Object newAcceleration = vehicle.get("acceleration");
                                Object existingAcceleration = existingVehicle.get("acceleration");

                                // 合并：新数据覆盖旧数据，但保留有价值的字段
                                existingVehicle.putAll(vehicle);

                                // 如果新数据没有加速度但旧数据有，恢复旧数据的加速度
                                if (newAcceleration == null && existingAcceleration != null) {
                                    existingVehicle.put("acceleration", existingAcceleration);
                                }
                            } else {
                                vehicleMap.put(vehicleId, vehicle);
                            }
                        }
                    } else if (result instanceof List) {
                        // produce_veh 可能返回数组
                        List<Map<String, Object>> vehicleList = (List<Map<String, Object>>) result;
                        for (Map<String, Object> vehicleData : vehicleList) {
                            Object vehicleId = vehicleData.get("id");
                            if (vehicleId != null) {
                                produceVehCount++;

                                Map<String, Object> vehicle = new HashMap<>();

                                // 先保留所有原始数据
                                vehicle.putAll(vehicleData);

                                // 然后设置/覆盖关键字段
                                vehicle.put("id", vehicleId);

                                Object speed = vehicleData.get("cur_spd");
                                if (speed == null) {
                                    speed = vehicleData.get("speed");
                                }
                                vehicle.put("speed", speed);

                                // 计算加速度
                                Object curSpd = vehicleData.get("cur_spd");
                                Object lastSpd = vehicleData.get("last_spd");

                                if (curSpd instanceof Number && lastSpd instanceof Number) {
                                    // veh_run 消息：计算加速度
                                    double acceleration = ((Number) curSpd).doubleValue() - ((Number) lastSpd).doubleValue();
                                    vehicle.put("acceleration", acceleration);
                                } else {
                                    // produce_veh 消息：默认加速度为0
                                    vehicle.put("acceleration", 0.0);
                                }

                                vehicle.put("x", vehicleData.get("x"));
                                vehicle.put("y", vehicleData.get("y"));
                                vehicle.put("roadId", vehicleData.get("link_id"));
                                vehicle.put("laneId", vehicleData.get("lane_id"));
                                vehicleMap.put(vehicleId, vehicle);
                            }
                        }
                    }
                } else if ("controller_run".equals(pos)) {
                    // 信号灯数据
                    if (result instanceof Map) {
                        signals.add((Map<String, Object>) result);
                    }
                }
            }
        }

        // 将去重后的车辆数据转换为列表
        List<Map<String, Object>> vehicles = new ArrayList<>(vehicleMap.values());

        completeData.put("vehicles", vehicles);
        completeData.put("signals", signals);

        log.debug("Built complete step data: step={}, unique_vehicles={}, signals={}",
                step, vehicles.size(), signals.size());

        return completeData;
    }

    /**
     * 从 SimInfo 中提取仿真任务ID
     */
    private String extractSimulationTaskId(SimInfo simInfo) {
        if (simInfo == null) {
            return null;
        }

        // 尝试从 simInfo 字段中获取 taskId
        Map<String, Object> simInfoMap = simInfo.getSimInfo();
        if (simInfoMap != null && simInfoMap.containsKey("taskId")) {
            return (String) simInfoMap.get("taskId");
        }

        // 如果没有，使用 sessionId 作为 taskId（临时方案）
        String sessionId = simInfo.getSessionId();
        if (sessionId != null && !sessionId.isEmpty()) {
            log.warn("Using sessionId as taskId: {}", sessionId);
            return sessionId;
        }

        log.warn("Simulation task ID not found in SimInfo");
        return null;
    }

    /**
     * 从 SimInfo 中提取用户ID
     */
    private String extractUserId(SimInfo simInfo) {
        if (simInfo == null) {
            return null;
        }

        // 尝试从 simInfo 字段中获取 userId
        Map<String, Object> simInfoMap = simInfo.getSimInfo();
        if (simInfoMap != null && simInfoMap.containsKey("userId")) {
            Object userId = simInfoMap.get("userId");
            return userId != null ? userId.toString() : null;
        }

        log.warn("User ID not found in SimInfo");
        return null;
    }

    /**
     * 从 SimInfo 中提取任务ID（来自simulation_task表）
     */
    private String extractTaskId(SimInfo simInfo) {
        if (simInfo == null) {
            return null;
        }

        // 尝试从 simInfo 字段中获取 taskId
        Map<String, Object> simInfoMap = simInfo.getSimInfo();
        if (simInfoMap != null && simInfoMap.containsKey("taskId")) {
            return (String) simInfoMap.get("taskId");
        }

        log.warn("Task ID not found in SimInfo");
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
            // 尝试保存数据（如果还有未保存的数据）
            int cachedStepCount = dataCollector.getSessionStepCount(exeId);
            if (cachedStepCount > 0) {
                log.warn("Connection closed but {} steps of data not saved yet, attempting to save",
                        cachedStepCount);
                handleSimulationEnd(exeId, simInfo);
            }

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

