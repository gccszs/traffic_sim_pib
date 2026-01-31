package com.traffic.sim.plugin.replay.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 回放控制服务
 * 管理每个回放会话的状态（播放、暂停、速度、当前步数等）
 * 
 * @author traffic-sim
 */
@Service
@Slf4j
public class SseReplayControlService {
    
    /**
     * 存储每个回放会话的控制状态
     * Key: sessionId (simulationTaskId)
     * Value: ReplayControlState
     */
    private final Map<String, ReplayControlState> controlStates = new ConcurrentHashMap<>();
    
    /**
     * 创建回放会话
     * 初始状态为 PAUSED，等待前端调用 play 接口开始播放
     */
    public ReplayControlState createSession(String sessionId) {
        ReplayControlState state = new ReplayControlState();
        state.setSessionId(sessionId);
        state.setStatus(ReplayStatus.PAUSED);  // 初始状态为暂停，等待前端播放指令
        state.setSpeed(1.0);
        state.setCurrentStep(0L);
        controlStates.put(sessionId, state);
        log.info("Created replay session: {} (initial status: PAUSED, waiting for play command)", sessionId);
        return state;
    }
    
    /**
     * 获取回放会话状态
     */
    public ReplayControlState getSessionState(String sessionId) {
        return controlStates.get(sessionId);
    }
    
    /**
     * 播放
     */
    public void play(String sessionId) {
        ReplayControlState state = controlStates.get(sessionId);
        if (state != null) {
            state.setStatus(ReplayStatus.PLAYING);
            log.info("Replay session {} resumed", sessionId);
        }
    }
    
    /**
     * 暂停
     */
    public void pause(String sessionId) {
        ReplayControlState state = controlStates.get(sessionId);
        if (state != null) {
            state.setStatus(ReplayStatus.PAUSED);
            log.info("Replay session {} paused", sessionId);
        }
    }
    
    /**
     * 停止
     */
    public void stop(String sessionId) {
        ReplayControlState state = controlStates.get(sessionId);
        if (state != null) {
            state.setStatus(ReplayStatus.STOPPED);
            log.info("Replay session {} stopped", sessionId);
        }
    }
    
    /**
     * 设置速度
     */
    public void setSpeed(String sessionId, double speed) {
        ReplayControlState state = controlStates.get(sessionId);
        if (state != null) {
            state.setSpeed(speed);
            log.info("Replay session {} speed changed to {}", sessionId, speed);
        }
    }
    
    /**
     * 跳转到指定步数
     */
    public void seekTo(String sessionId, long targetStep) {
        ReplayControlState state = controlStates.get(sessionId);
        if (state != null) {
            state.setTargetStep(targetStep);
            state.setSeekRequested(true);
            log.info("Replay session {} seek to step {}", sessionId, targetStep);
        }
    }
    
    /**
     * 更新当前步数
     */
    public void updateCurrentStep(String sessionId, long currentStep) {
        ReplayControlState state = controlStates.get(sessionId);
        if (state != null) {
            state.setCurrentStep(currentStep);
        }
    }
    
    /**
     * 清除跳转请求标志
     */
    public void clearSeekRequest(String sessionId) {
        ReplayControlState state = controlStates.get(sessionId);
        if (state != null) {
            state.setSeekRequested(false);
        }
    }
    
    /**
     * 移除回放会话
     */
    public void removeSession(String sessionId) {
        controlStates.remove(sessionId);
        log.info("Removed replay session: {}", sessionId);
    }
    
    /**
     * 回放控制状态
     */
    @Data
    public static class ReplayControlState {
        private String sessionId;
        private ReplayStatus status;
        private double speed;
        private long currentStep;
        private long targetStep;
        private boolean seekRequested;
    }
    
    /**
     * 回放状态枚举
     */
    public enum ReplayStatus {
        PLAYING,
        PAUSED,
        STOPPED
    }
}

