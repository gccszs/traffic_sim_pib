package com.traffic.sim.plugin.engine.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仿真数据收集器
 * 用于在实时仿真过程中收集每一步的数据，仿真结束后保存到MongoDB
 * 
 * @author traffic-sim
 */
@Slf4j
@Service
public class SimulationDataCollector {
    
    /**
     * 缓存每个会话的仿真步数据
     * Key: sessionId, Value: List<StepData>
     */
    private final Map<String, List<StepData>> sessionDataCache = new ConcurrentHashMap<>();
    
    /**
     * 添加一步仿真数据
     * 
     * @param sessionId 会话ID
     * @param step 步数
     * @param simData 仿真原始数据
     * @param statistics 统计数据
     */
    public void addStepData(String sessionId, Long step, Map<String, Object> simData, 
                           Map<String, Object> statistics) {
        sessionDataCache.computeIfAbsent(sessionId, k -> new ArrayList<>())
            .add(new StepData(step, System.currentTimeMillis(), simData, statistics));
        
        log.debug("Collected step data for session: {}, step: {}", sessionId, step);
    }
    
    /**
     * 获取会话的所有收集数据
     * 
     * @param sessionId 会话ID
     * @return 步数据列表
     */
    public List<StepData> getSessionData(String sessionId) {
        return sessionDataCache.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * 获取并清除会话数据
     * 
     * @param sessionId 会话ID
     * @return 步数据列表
     */
    public List<StepData> getAndClearSessionData(String sessionId) {
        List<StepData> data = sessionDataCache.remove(sessionId);
        if (data != null) {
            log.info("Retrieved and cleared {} steps of data for session: {}", data.size(), sessionId);
            return data;
        }
        return new ArrayList<>();
    }
    
    /**
     * 清除会话数据
     * 
     * @param sessionId 会话ID
     */
    public void clearSessionData(String sessionId) {
        List<StepData> removed = sessionDataCache.remove(sessionId);
        if (removed != null) {
            log.info("Cleared {} steps of data for session: {}", removed.size(), sessionId);
        }
    }
    
    /**
     * 获取当前缓存的会话数量
     */
    public int getCachedSessionCount() {
        return sessionDataCache.size();
    }
    
    /**
     * 获取指定会话的步数
     */
    public int getSessionStepCount(String sessionId) {
        List<StepData> data = sessionDataCache.get(sessionId);
        return data != null ? data.size() : 0;
    }
    
    /**
     * 单步数据模型
     */
    public static class StepData {
        private final Long step;
        private final Long timestamp;
        private final Map<String, Object> simData;
        private final Map<String, Object> statistics;
        
        public StepData(Long step, Long timestamp, Map<String, Object> simData, 
                       Map<String, Object> statistics) {
            this.step = step;
            this.timestamp = timestamp;
            // 深拷贝避免数据被修改
            this.simData = simData != null ? new HashMap<>(simData) : new HashMap<>();
            this.statistics = statistics != null ? new HashMap<>(statistics) : new HashMap<>();
        }
        
        public Long getStep() {
            return step;
        }
        
        public Long getTimestamp() {
            return timestamp;
        }
        
        public Map<String, Object> getSimData() {
            return simData;
        }
        
        public Map<String, Object> getStatistics() {
            return statistics;
        }
    }
}
