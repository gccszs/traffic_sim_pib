package com.traffic.sim.common.service;

import com.traffic.sim.common.model.SimInfo;

import java.util.List;

/**
 * 会话服务接口
 * 定义在common模块，由plugin-engine-manager模块实现
 * 
 * @author traffic-sim
 */
public interface SessionService {
    
    /**
     * 创建会话
     * 
     * @param sessionId 会话ID
     * @return 会话信息
     */
    SimInfo createSession(String sessionId);
    
    /**
     * 获取会话信息
     * 
     * @param sessionId 会话ID
     * @return 会话信息，不存在返回null
     */
    SimInfo getSessionInfo(String sessionId);
    
    /**
     * 更新会话信息
     * 
     * @param sessionId 会话ID
     * @param simInfo 会话信息
     */
    void updateSessionInfo(String sessionId, SimInfo simInfo);
    
    /**
     * 检查会话是否存在
     * 
     * @param sessionId 会话ID
     * @return 是否存在
     */
    boolean sessionExists(String sessionId);
    
    /**
     * 删除会话
     * 
     * @param sessionId 会话ID
     */
    void removeSession(String sessionId);
    
    /**
     * 获取所有活跃会话
     * 
     * @return 所有活跃会话列表
     */
    List<SimInfo> getAllActiveSessions();
}

