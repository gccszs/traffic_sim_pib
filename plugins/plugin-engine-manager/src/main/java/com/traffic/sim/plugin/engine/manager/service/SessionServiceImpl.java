package com.traffic.sim.plugin.engine.manager.service;

import com.traffic.sim.common.model.SimInfo;
import com.traffic.sim.common.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话服务实现
 * 
 * @author traffic-sim
 */
@Slf4j
@Service
public class SessionServiceImpl implements SessionService {
    
    // 内存存储（单体架构）
    private final Map<String, SimInfo> sessionStore = new ConcurrentHashMap<>();
    
    @Override
    public SimInfo createSession(String sessionId) {
        // 检查是否已经存在（可能前端已经先连接了）
        SimInfo existingSimInfo = sessionStore.get(sessionId);
        if (existingSimInfo != null) {
            log.info("Session already exists (frontend may have connected first): {}", sessionId);
            // 保留已有的连接信息，只更新时间戳
            existingSimInfo.setLastUpdateTime(System.currentTimeMillis());
            return existingSimInfo;
        }
        
        // 创建新的 Session
        SimInfo simInfo = new SimInfo();
        simInfo.setSessionId(sessionId);
        simInfo.setCreateTime(System.currentTimeMillis());
        simInfo.setLastUpdateTime(System.currentTimeMillis());
        sessionStore.put(sessionId, simInfo);
        log.info("Session created: {}", sessionId);
        return simInfo;
    }
    
    @Override
    public SimInfo getSessionInfo(String sessionId) {
        return sessionStore.get(sessionId);
    }
    
    @Override
    public void updateSessionInfo(String sessionId, SimInfo simInfo) {
        if (simInfo != null) {
            simInfo.setLastUpdateTime(System.currentTimeMillis());
            sessionStore.put(sessionId, simInfo);
        }
    }
    
    @Override
    public boolean sessionExists(String sessionId) {
        return sessionStore.containsKey(sessionId);
    }
    
    @Override
    public void removeSession(String sessionId) {
        SimInfo removed = sessionStore.remove(sessionId);
        if (removed != null) {
            log.info("Session removed: {}", sessionId);
        }
    }
    
    @Override
    public List<SimInfo> getAllActiveSessions() {
        return new ArrayList<>(sessionStore.values());
    }
}

