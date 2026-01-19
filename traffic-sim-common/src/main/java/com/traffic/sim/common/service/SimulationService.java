package com.traffic.sim.common.service;

import com.traffic.sim.common.dto.CreateSimulationRequest;
import com.traffic.sim.common.dto.SimulationTaskDTO;
import com.traffic.sim.common.response.PageResult;

import java.util.Map;

/**
 * 仿真服务接口
 * 定义在common模块，由plugin-simulation模块实现
 * 
 * @author traffic-sim
 */
public interface SimulationService {
    
    /**
     * 创建仿真任务
     * 
     * @param request 创建仿真请求
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 仿真任务DTO
     */
    SimulationTaskDTO createSimulation(CreateSimulationRequest request, String userId, String sessionId);
    
    /**
     * 获取仿真任务
     * 
     * @param taskId 任务ID
     * @return 仿真任务DTO
     */
    SimulationTaskDTO getSimulationTask(String taskId);
    
    /**
     * 获取仿真任务列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<SimulationTaskDTO> getSimulationList(int page, int size);
    
    /**
     * 绿信比控制
     * 
     * @param greenRatio 绿信比值（0-100）
     * @param sessionId 会话ID
     * @param simulationInfo 仿真信息（可选，用于日志记录）
     */
    void controlGreenRatio(int greenRatio, String sessionId, Map<String, Object> simulationInfo);
}

