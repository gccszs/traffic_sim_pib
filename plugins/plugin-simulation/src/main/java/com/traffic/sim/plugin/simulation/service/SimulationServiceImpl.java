package com.traffic.sim.plugin.simulation.service;

import com.traffic.sim.common.constant.ErrorCode;
import com.traffic.sim.common.dto.CreateSimulationRequest;
import com.traffic.sim.common.dto.MapDTO;
import com.traffic.sim.common.dto.SimulationTaskDTO;
import com.traffic.sim.common.exception.BusinessException;
import com.traffic.sim.common.exception.ServiceException;
import com.traffic.sim.common.model.SimInfo;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.service.MapService;
import com.traffic.sim.common.service.SessionService;
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
import java.util.HashMap;
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
    private final SimulationPythonGrpcClient simulationPythonGrpcClient; // 即使 gRPC 不可用，这个 Bean 也会存在（返回兗底数据）
    private final SessionService sessionService; // 会话服务
    private final MapService mapService; // 地图服务（用于获取用户最近上传的地图路径）
    
    @Override
    @Transactional
    public SimulationTaskDTO createSimulation(CreateSimulationRequest request, String userId, String taskId) {
        log.info("Creating simulation task: userId={}, sessionId={}, mapId={}", 
            userId, taskId, request.getSimInfo().getMapId());
        
        // 打印 fixedOd 信息
        if (request.getSimInfo().getFixedOd() != null) {
            log.info("FixedOd data: od count={}, sg count={}", 
                request.getSimInfo().getFixedOd().getOd() != null ? request.getSimInfo().getFixedOd().getOd().size() : 0,
                request.getSimInfo().getFixedOd().getSg() != null ? request.getSimInfo().getFixedOd().getSg().size() : 0);
        } else {
            log.warn("FixedOd is NULL in request!");
        }
        
        String xmlMapName = UUID.randomUUID().toString().replace("-","");
        request.getSimInfo().setMapXmlName(xmlMapName);
        
        // 0. 先创建 Session，确保引擎连接 WebSocket 时能找到对应的 session
        SimInfo simInfo = sessionService.createSession(taskId);
        simInfo.setName(request.getSimInfo().getName());
        simInfo.setMapXmlName(xmlMapName);
        sessionService.updateSessionInfo(taskId, simInfo);
        log.info("Session created for taskId: {}", taskId);
            
        // 1. 根据 mapId 查询地图XML文件路径（优先使用 mapId）
        if (request.getSimInfo().getMapXmlPath() == null || 
            request.getSimInfo().getMapXmlPath().trim().isEmpty()) {
            String mapId = request.getSimInfo().getMapId();
            if (mapId != null && !mapId.trim().isEmpty()) {
                // 使用 mapId 查询路径（从Redis缓存或数据库）
                String xmlPath = mapService.getXmlFilePathByMapId(mapId);
                if (xmlPath != null && !xmlPath.isEmpty()) {
                    request.getSimInfo().setMapXmlPath(xmlPath);
                    log.info("Found mapXmlPath for mapId {}: {}", mapId, xmlPath);
                } else {
                    log.warn("No xmlPath found for mapId: {}", mapId);
                    // 尝试直接从数据库查询完整的地图信息
                    MapDTO mapDTO = mapService.getMapById(mapId, parseUserId(userId));
                    if (mapDTO != null) {
                        log.info("Found mapDTO for mapId {}: {}", mapId, mapDTO.getName());
                        // 如果有地图实体但没有xmlFilePath，可能需要重新生成或使用默认路径
                        String defaultXmlPath = mapId + "/" + mapId + ".xml";
                        request.getSimInfo().setMapXmlPath(defaultXmlPath);
                        log.info("Using default xmlPath for mapId {}: {}", mapId, defaultXmlPath);
                    } else {
                        log.error("No map found for mapId: {}", mapId);
                    }
                }
            } else {
                log.warn("mapId is null or empty, cannot lookup xmlPath");
            }
        }
            

        // 4. 调用Python服务创建仿真引擎（支持容错，gRPC不可用时使用兗底数据）
        ApiResponse response = simulationPythonGrpcClient.createSimeng(request, taskId);
            
        // 5. 保存仿真任务记录
        SimulationTask task = new SimulationTask();
        task.setTaskId(taskId);
        task.setName(request.getSimInfo().getName());
        task.setMapXmlName(request.getSimInfo() != null ? request.getSimInfo().getMapXmlName() : null);
        task.setMapXmlPath(request.getSimInfo() != null ? request.getSimInfo().getMapXmlPath() : null);
        task.setSimConfig(JsonUtils.toJson(request));
        
        // 检查响应，如果是兜底响应（包含gRPC不可用提示），记录警告但继续执行
        if (response.getMsg() != null && response.getMsg().contains("gRPC unavailable")) {
            log.warn("gRPC service unavailable, using fallback response. Message: {}", response.getMsg());
            // 继续执行，但记录警告
            task.setStatus("CREATED");
        } else if (!ErrorCode.ERR_OK.equals(response.getRes())) {
            // 如果是真正的错误响应，抛出异常
            throw new BusinessException(response.getRes(), 
                "Failed to create simulation engine: " + response.getMsg());
        } else if (response.getData() != null && response.getData().toString().contains("Failed")) {
            // 引擎启动失败，返回的是ERR_OK但消息中包含失败信息
            log.error("Simulation engine failed to start: {}", response.getData());
            // 继续执行，保存任务记录，但标记为失败状态
            task.setStatus("FAILED");
        } else {
            // 引擎启动成功
            task.setStatus("CREATED");
        }
            
        // 使用真实的 userId
        task.setUserId(parseUserId(userId));
            
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
            
        task = simulationTaskRepository.save(task);
            
        // 6. 更新 SessionService 中的 SimInfo，添加 taskId
        // 注意：simInfo 已经在方法开头创建，这里重新获取以确保数据最新
        SimInfo updatedSimInfo = sessionService.getSessionInfo(taskId);
        if (updatedSimInfo != null) {
            if (updatedSimInfo.getSimInfo() == null) {
                updatedSimInfo.setSimInfo(new HashMap<>());
            }
            updatedSimInfo.getSimInfo().put("taskId", taskId);
            sessionService.updateSessionInfo(taskId, updatedSimInfo);
            log.info("Updated session {} with taskId: {}", taskId, taskId);
        } else {
            log.warn("SimInfo not found for session: {} (this should not happen)", taskId);
        }
            
        // 7. 转换为DTO并返回
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
     * 解析userId
     */
    private Long parseUserId(String userIdStr) {
        if (userIdStr == null) {
            return null;
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            // 如果userId不是数字，使用hashCode作为临时userId
            return (long) userIdStr.hashCode();
        }
    }
    
    /**
     * 验证创建请求
     */
    private void validateCreateRequest(CreateSimulationRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.ERR_ARG, "Request cannot be null");
        }
        if (request.getSimInfo() == null) {
            throw new BusinessException(ErrorCode.ERR_ARG, "SimInfo cannot be null");
        }

        if (request.getSimInfo().getName() == null || request.getSimInfo().getName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_ARG, "Simulation name cannot be empty");
        }
        
        // mapId 必须存在
        if (request.getSimInfo().getMapId() == null || request.getSimInfo().getMapId().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_ARG, "请选择地图（mapId不能为空）");
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

