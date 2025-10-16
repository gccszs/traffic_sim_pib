package com.simeng.pib.service.impl;

import com.simeng.pib.model.SimInfo;
import com.simeng.pib.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理服务
 */
@Slf4j
@Service
public class SessionServiceImpl implements SessionService {
    
    /**
     * 存储认证ID和仿真实例信息的对应关系
     */
    private final Map<String, SimInfo> idInfos = new ConcurrentHashMap<>();
    
    /**
     * 创建新的会话ID
     */
    public String createSessionId() {
        String sessionId = UUID.randomUUID().toString();
        idInfos.put(sessionId, new SimInfo());
        log.debug("Created new session: {}", sessionId);
        return sessionId;
    }
    
    /**
     * 获取会话信息
     */
    @Override
    public SimInfo getSessionInfo(String sessionId) {
        return idInfos.get(sessionId);
    }
    
    /**
     * 检查会话是否存在
     */
    @Override
    public boolean sessionExists(String sessionId) {
        return sessionId != null && idInfos.containsKey(sessionId);
    }
    
    /**
     * 删除会话
     */
    @Override
    public boolean deleteSession(String sessionId) {
        if (sessionId != null && idInfos.containsKey(sessionId)) {
            idInfos.remove(sessionId);
            log.debug("Deleted session: {}", sessionId);
            return true;
        }
        return false;
    }
    
    /**
     * 更新会话信息
     */
    @Override
    public void updateSessionInfo(String sessionId, SimInfo simInfo) {
        if (sessionExists(sessionId)) {
            idInfos.put(sessionId, simInfo);
        }
    }
    
    /**
     * 获取所有会话数量
     */
    @Override
    public int getSessionCount() {
        return idInfos.size();
    }
}
