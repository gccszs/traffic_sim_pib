package com.traffic.sim.plugin.engine.manager.service;

import com.traffic.sim.common.mq.MessageProducer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仿真数据收集器
 * 按仿真步收集所有 WebSocket 消息数据
 * 支持通过MQ异步持久化数据
 * 
 * @author traffic-sim
 */
@Slf4j
@Service
public class SimulationDataCollector {
    
    @Autowired(required = false)
    private MessageProducer messageProducer;
    
    @Autowired(required = false)
    private SimulationDataMessageHandler messageHandler;
    
    private boolean mqEnabled = false;
    
    private String simulationTaskId;
    private String userId;
    private String taskId;
    
    /**
     * 存储每个会话的仿真步数据
     * Key: sessionId
     * Value: 该会话的所有仿真步数据列表
     */
    private final Map<String, List<StepData>> sessionDataMap = new ConcurrentHashMap<>();
    
    /**
     * 存储每个会话当前正在收集的仿真步数据
     * Key: sessionId
     * Value: 当前步的数据收集器
     */
    private final Map<String, CurrentStepCollector> currentStepMap = new ConcurrentHashMap<>();
    
    /**
     * 设置仿真任务信息（用于MQ持久化）
     */
    public void setSimulationInfo(String simulationTaskId, String userId, String taskId) {
        this.simulationTaskId = simulationTaskId;
        this.userId = userId;
        this.taskId = taskId;
        this.mqEnabled = (messageProducer != null && messageHandler != null);
        log.info("SimulationDataCollector configured: mqEnabled={}, simulationTaskId={}", mqEnabled, simulationTaskId);
    }
    
    /**
     * 添加仿真步数据（当仿真步结束时调用）
     * 如果MQ已启用，数据将通过MQ异步保存
     * 
     * @param sessionId 会话ID
     * @param step 仿真步数
     * @param simData 仿真数据
     * @param statsData 统计数据
     */
    public void addStepData(String sessionId, Long step, Map<String, Object> simData, Map<String, Object> statsData) {
        StepData stepData = new StepData(step, System.currentTimeMillis(), simData, statsData);
        
        if (mqEnabled) {
            messageHandler.sendSimulationDataSaveRequest(simulationTaskId, userId, taskId, stepData);
            log.debug("MQ: Sent step {} data for async save, session: {}", step, sessionId);
        } else {
            sessionDataMap.computeIfAbsent(sessionId, k -> new ArrayList<>())
                .add(stepData);
            log.debug("Local: Added step {} data for session: {}, total steps: {}", 
                step, sessionId, sessionDataMap.get(sessionId).size());
        }
    }
    
    /**
     * 获取并清空会话的所有仿真步数据
     * 
     * @param sessionId 会话ID
     * @return 仿真步数据列表
     */
    public List<StepData> getAndClearSessionData(String sessionId) {
        List<StepData> data = sessionDataMap.remove(sessionId);
        currentStepMap.remove(sessionId);
        return data != null ? data : new ArrayList<>();
    }
    
    /**
     * 获取会话已收集的仿真步数量
     * 
     * @param sessionId 会话ID
     * @return 仿真步数量
     */
    public int getSessionStepCount(String sessionId) {
        List<StepData> data = sessionDataMap.get(sessionId);
        return data != null ? data.size() : 0;
    }
    
    /**
     * 添加消息到当前仿真步
     * 
     * @param sessionId 会话ID
     * @param messageType 消息类型
     * @param messageData 消息数据
     */
    public void addMessageToCurrentStep(String sessionId, String messageType, Map<String, Object> messageData) {
        CurrentStepCollector collector = currentStepMap.computeIfAbsent(sessionId, k -> new CurrentStepCollector());
        collector.addMessage(messageType, messageData);
    }
    
    /**
     * 获取当前仿真步的收集器（不移除）
     * 
     * @param sessionId 会话ID
     * @return 当前步收集器
     */
    public CurrentStepCollector getCurrentStepCollector(String sessionId) {
        return currentStepMap.get(sessionId);
    }
    
    /**
     * 完成当前仿真步的收集，返回收集的数据
     * 
     * @param sessionId 会话ID
     * @param step 仿真步数
     * @return 当前步收集的所有数据
     */
    public CurrentStepCollector finishCurrentStep(String sessionId, Long step) {
        CurrentStepCollector collector = currentStepMap.remove(sessionId);
        if (collector != null) {
            collector.setStep(step);
            collector.setTimestamp(System.currentTimeMillis());
        }
        return collector;
    }
    
    /**
     * 仿真步数据
     */
    @Data
    public static class StepData {
        private final Long step;
        private final Long timestamp;
        private final Map<String, Object> simData;
        private final Map<String, Object> statsData;
    }
    
    /**
     * 当前仿真步数据收集器
     */
    @Data
    public static class CurrentStepCollector {
        private Long step;
        private Long timestamp;
        private final List<MessageData> messages = new ArrayList<>();
        
        public void addMessage(String type, Map<String, Object> data) {
            messages.add(new MessageData(type, System.currentTimeMillis(), data));
        }
        
        /**
         * 获取所有消息的数据部分（不包含元数据）
         */
        public List<Map<String, Object>> getMessages() {
            List<Map<String, Object>> result = new ArrayList<>();
            for (MessageData msg : messages) {
                result.add(msg.getData());
            }
            return result;
        }
        
        @Data
        public static class MessageData {
            private final String type;
            private final Long timestamp;
            private final Map<String, Object> data;
        }
    }
}
