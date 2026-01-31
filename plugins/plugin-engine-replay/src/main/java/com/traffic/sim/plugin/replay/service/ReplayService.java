package com.traffic.sim.plugin.replay.service;

import com.traffic.sim.plugin.replay.dto.CreateReplayTaskRequest;
import com.traffic.sim.plugin.replay.dto.ReplayControlRequest;
import com.traffic.sim.common.dto.ReplayDataDTO;
import com.traffic.sim.plugin.replay.dto.ReplayTaskDTO;
import com.traffic.sim.common.response.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 回放服务接口
 * 
 * @author traffic-sim
 */
public interface ReplayService {
    
    /**
     * 获取回放地图信息
     * 直接通过仿真任务ID从 MongoDB 获取地图 JSON 数据（addition 字段）
     * 
     * @param simulationTaskId 仿真任务ID（simulation_task 表的 task_id）
     * @return 地图信息，包含 addition 字段
     */
    Map<String, Object> getReplayMapInfo(String simulationTaskId);
    
    /**
     * 获取回放信息
     * 从 simulation_data 集合获取仿真数据的统计信息（总步数等）
     * 
     * @param simulationTaskId 仿真任务ID
     * @return 回放信息（totalSteps, startTime, endTime 等）
     */
    Map<String, Object> getReplayInfo(String simulationTaskId);
    
    // ========== 以下是旧的回放任务管理接口，保留用于兼容 ==========
    
    /**
     * 创建回放任务（已废弃）
     */
    @Deprecated
    ReplayTaskDTO createReplayTask(CreateReplayTaskRequest request, Long userId);
    
    /**
     * 获取回放任务（已废弃）
     */
    @Deprecated
    ReplayTaskDTO getReplayTask(String taskId, Long userId);
    
    /**
     * 获取回放任务列表（已废弃）
     */
    @Deprecated
    PageResult<ReplayTaskDTO> getReplayTaskList(Long userId, int page, int size);
    
    /**
     * 获取回放数据（已废弃）
     */
    @Deprecated
    List<ReplayDataDTO> getReplayData(String taskId, Long userId, Long startStep, Long endStep);
    
    /**
     * 控制回放（已废弃）
     */
    @Deprecated
    ReplayTaskDTO controlReplay(String taskId, Long userId, ReplayControlRequest request);
    
    /**
     * 删除回放任务（已废弃）
     */
    @Deprecated
    void deleteReplayTask(String taskId, Long userId);
    
    // ========== 回放历史记录管理接口 ==========
    
    /**
     * 获取回放历史记录列表（分页）
     * 
     * @param userId 用户ID
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 分页结果
     */
    org.springframework.data.domain.Page<com.traffic.sim.plugin.replay.entity.ReplayTask> getReplayHistoryList(Long userId, int page, int size);
    
    /**
     * 获取指定仿真任务的回放历史记录
     * 
     * @param simulationTaskId 仿真任务ID
     * @return 回放历史列表
     */
    List<com.traffic.sim.plugin.replay.entity.ReplayTask> getReplayHistoryBySimulationTask(String simulationTaskId);
    
    /**
     * 获取回放历史详情
     * 
     * @param replayTaskId 回放任务ID
     * @return 回放任务详情
     */
    com.traffic.sim.plugin.replay.entity.ReplayTask getReplayHistoryDetail(String replayTaskId);
    
    /**
     * 删除回放历史记录
     * 
     * @param replayTaskId 回放任务ID
     * @param userId 用户ID（用于权限验证）
     */
    void deleteReplayHistory(String replayTaskId, Long userId);
    
    /**
     * 统计用户回放次数
     * 
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getReplayStats(Long userId);
    
    /**
     * 创建回放历史记录（由 SSE Controller 调用）
     * 
     * @param simulationTaskId 仿真任务ID
     * @param userId 用户ID
     * @return 回放任务ID
     */
    String createReplayHistory(String simulationTaskId, Long userId);
    
    /**
     * 更新回放历史状态
     * 
     * @param replayTaskId 回放任务ID
     * @param status 状态
     * @param currentStep 当前步数
     */
    void updateReplayHistoryStatus(String replayTaskId, String status, Long currentStep);
    
    /**
     * 更新回放历史总步数
     * 
     * @param replayTaskId 回放任务ID
     * @param totalSteps 总步数
     */
    void updateReplayHistoryTotalSteps(String replayTaskId, Long totalSteps);
    
    /**
     * 更新回放历史播放速度
     * 
     * @param replayTaskId 回放任务ID
     * @param speed 播放速度
     */
    void updateReplayHistorySpeed(String replayTaskId, Double speed);
}

