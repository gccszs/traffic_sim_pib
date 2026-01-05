package com.traffic.sim.plugin.simulation.service;

import com.traffic.sim.common.constant.ErrorCode;
import com.traffic.sim.common.dto.CreateSimulationRequest;
import com.traffic.sim.common.dto.SimulationTaskDTO;
import com.traffic.sim.common.exception.BusinessException;
import com.traffic.sim.common.exception.ServiceException;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.service.SimulationService;
import com.traffic.sim.common.util.JsonUtils;
import com.traffic.sim.plugin.simulation.entity.SimulationTask;
import com.traffic.sim.plugin.simulation.grpc.SimulationPythonGrpcClient;
import com.traffic.sim.plugin.simulation.repository.SimulationTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 仿真服务实现类
 * 
 * @author traffic-sim
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationServiceImpl implements SimulationService {
    
    private final SimulationTaskRepository simulationTaskRepository;
    private final SimulationPythonGrpcClient simulationPythonGrpcClient; // 即使 gRPC 不可用，这个 Bean 也会存在（返回兜底数据）
    
    @Override
    @Transactional
    public SimulationTaskDTO createSimulation(CreateSimulationRequest request, String sessionId) {
        log.info("Creating simulation task: name={}, sessionId={}", request.getName(), sessionId);
        
        // 1. 验证请求参数
        validateCreateRequest(request);
        
        // 2. 生成任务ID
        String taskId = UUID.randomUUID().toString().replace("-", "");
        
        // 3. 调用Python服务创建仿真引擎（支持容错，gRPC不可用时使用兜底数据）
        ApiResponse response = simulationPythonGrpcClient.createSimeng(request, sessionId);
        
        // 检查响应，如果是兜底响应（包含gRPC不可用提示），记录警告但继续执行
        if (response.getMsg() != null && response.getMsg().contains("gRPC unavailable")) {
            log.warn("gRPC service unavailable, using fallback response. Message: {}", response.getMsg());
            // 继续执行，但记录警告
        } else if (!ErrorCode.ERR_OK.equals(response.getRes())) {
            // 如果是真正的错误响应，抛出异常
            throw new BusinessException(response.getRes(), 
                "Failed to create simulation engine: " + response.getMsg());
        }
        
        // 4. 保存仿真任务记录
        SimulationTask task = new SimulationTask();
        task.setTaskId(taskId);
        task.setName(request.getName());
        task.setMapXmlName(request.getSimInfo() != null ? request.getSimInfo().getMapXmlName() : null);
        task.setMapXmlPath(request.getSimInfo() != null ? request.getSimInfo().getMapXmlPath() : null);
        task.setSimConfig(JsonUtils.toJson(request));
        task.setStatus("CREATED");
        // TODO: 从请求上下文获取userId，当前使用sessionId作为临时方案
        // 实际应该从RequestContext.getCurrentUserId()获取
        try {
            task.setUserId(Long.parseLong(sessionId));
        } catch (NumberFormatException e) {
            // 如果sessionId不是数字，使用hashCode作为临时userId
            // 实际应该从认证上下文获取
            task.setUserId((long) sessionId.hashCode());
        }
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        
        task = simulationTaskRepository.save(task);
        
        // 5. 转换为DTO并返回
        return convertToDTO(task);
    }
    
    @Override
    public SimulationTaskDTO getSimulationTask(String taskId) {
        SimulationTask task = simulationTaskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ERR_NOT_FOUND, 
                "Simulation task not found: " + taskId));
        
        return convertToDTO(task);
    }
    
    @Override
    public PageResult<SimulationTaskDTO> getSimulationList(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<SimulationTask> taskPage = simulationTaskRepository.findAll(pageable);
        
        List<SimulationTaskDTO> dtoList = taskPage.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageResult<>(
            dtoList,
            taskPage.getTotalElements(),
            page,
            size
        );
    }
    
    @Override
    public void controlGreenRatio(int greenRatio, String sessionId, Map<String, Object> simulationInfo) {
        log.info("Control green ratio: {} for session: {}, simulation: {}", 
            greenRatio, sessionId, simulationInfo);
        
        // 1. 验证参数
        if (greenRatio < 0 || greenRatio > 100) {
            throw new BusinessException(ErrorCode.ERR_ARG, 
                "Green ratio must be between 0 and 100");
        }
        
        // 2. 调用gRPC服务
        try {
            ApiResponse response = simulationPythonGrpcClient.controlGreenRatio(greenRatio);
            
            if (!ErrorCode.ERR_OK.equals(response.getRes())) {
                throw new BusinessException(response.getRes(), 
                    "Failed to control green ratio: " + response.getMsg());
            }
            
            log.info("Green ratio controlled successfully: {}", greenRatio);
        } catch (ServiceException e) {
            log.error("Failed to control green ratio via gRPC", e);
            throw new BusinessException(ErrorCode.ERR_ENGINE, 
                "Failed to control green ratio: " + e.getMessage());
        }
    }
    
    /**
     * 验证创建请求
     */
    private void validateCreateRequest(CreateSimulationRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.ERR_ARG, "Request cannot be null");
        }
        
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_ARG, "Simulation name cannot be empty");
        }
        
        if (request.getSimInfo() == null) {
            throw new BusinessException(ErrorCode.ERR_ARG, "SimInfo cannot be null");
        }
        
        if (request.getSimInfo().getMapXmlName() == null || 
            request.getSimInfo().getMapXmlName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_ARG, "Map XML name cannot be empty");
        }
        
        if (request.getSimInfo().getMapXmlPath() == null || 
            request.getSimInfo().getMapXmlPath().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_ARG, "Map XML path cannot be empty");
        }
    }
    
    /**
     * 转换为DTO
     */
    private SimulationTaskDTO convertToDTO(SimulationTask task) {
        SimulationTaskDTO dto = new SimulationTaskDTO();
        dto.setTaskId(task.getTaskId());
        dto.setName(task.getName());
        dto.setMapXmlName(task.getMapXmlName());
        dto.setMapXmlPath(task.getMapXmlPath());
        dto.setSimConfig(task.getSimConfig());
        dto.setStatus(task.getStatus());
        dto.setUserId(task.getUserId());
        dto.setCreateTime(task.getCreateTime());
        dto.setUpdateTime(task.getUpdateTime());
        return dto;
    }
}

