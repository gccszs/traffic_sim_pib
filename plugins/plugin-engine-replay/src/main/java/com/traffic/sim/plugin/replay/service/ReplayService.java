package com.traffic.sim.plugin.replay.service;

import com.traffic.sim.plugin.replay.dto.CreateReplayTaskRequest;
import com.traffic.sim.plugin.replay.dto.ReplayControlRequest;
import com.traffic.sim.plugin.replay.dto.ReplayDataDTO;
import com.traffic.sim.plugin.replay.dto.ReplayTaskDTO;
import com.traffic.sim.common.response.PageResult;

import java.util.List;

/**
 * 回放服务接口
 * 
 * @author traffic-sim
 */
public interface ReplayService {
    
    /**
     * 创建回放任务
     * 
     * @param request 创建回放任务请求
     * @param userId 用户ID
     * @return 回放任务DTO
     */
    ReplayTaskDTO createReplayTask(CreateReplayTaskRequest request, Long userId);
    
    /**
     * 获取回放任务
     * 
     * @param taskId 任务ID
     * @param userId 用户ID（用于权限验证）
     * @return 回放任务DTO
     */
    ReplayTaskDTO getReplayTask(String taskId, Long userId);
    
    /**
     * 获取回放任务列表
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<ReplayTaskDTO> getReplayTaskList(Long userId, int page, int size);
    
    /**
     * 获取回放数据
     * 
     * @param taskId 任务ID
     * @param userId 用户ID（用于权限验证）
     * @param startStep 起始步数
     * @param endStep 结束步数
     * @return 回放数据列表
     */
    List<ReplayDataDTO> getReplayData(String taskId, Long userId, Long startStep, Long endStep);
    
    /**
     * 控制回放
     * 
     * @param taskId 任务ID
     * @param userId 用户ID（用于权限验证）
     * @param request 控制请求
     * @return 更新后的回放任务DTO
     */
    ReplayTaskDTO controlReplay(String taskId, Long userId, ReplayControlRequest request);
    
    /**
     * 删除回放任务
     * 
     * @param taskId 任务ID
     * @param userId 用户ID（用于权限验证）
     */
    void deleteReplayTask(String taskId, Long userId);
}

