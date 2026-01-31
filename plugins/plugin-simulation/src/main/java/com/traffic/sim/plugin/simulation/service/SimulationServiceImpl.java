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
import org.springframework.data.mongodb.core.MongoTemplate;
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
    private final MongoTemplate mongoTemplate; // MongoDB 模板

    @Override
    public void prepareSimulation(String taskId, String userId) {
        log.info("Preparing simulation task: userId={}, taskId={}", userId, taskId);
        
        // 创建 Session，供前端和引擎连接 WebSocket 使用
        SimInfo simInfo = sessionService.createSession(taskId);
        
        // 设置基本信息
        if (simInfo.getSimInfo() == null) {
            simInfo.setSimInfo(new HashMap<>());
        }
        simInfo.getSimInfo().put("taskId", taskId);
        simInfo.getSimInfo().put("userId", userId);
        
        sessionService.updateSessionInfo(taskId, simInfo);
        log.info("Session created and ready for WebSocket connections: taskId={}", taskId);
    }

    @Override
    public SimulationTaskDTO startSimulation(CreateSimulationRequest request, String userId, String taskId) {
        log.info("Starting simulation engine: userId={}, taskId={}, mapId={}",
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

        // 更新 Session 信息
        SimInfo simInfo = sessionService.getSessionInfo(taskId);
        if (simInfo == null) {
            log.error("Session not found for taskId: {}. Please call /prepare first.", taskId);
            throw new BusinessException(ErrorCode.ERR_NOT_FOUND, 
                "Session not found. Please call /api/simulation/prepare first.");
        }
        
        simInfo.setName(request.getSimInfo().getName());
        simInfo.setMapXmlName(xmlMapName);
        sessionService.updateSessionInfo(taskId, simInfo);

        // 根据 mapId 查询地图XML文件路径（优先使用 mapId）
        String userMapName = null;  // 用户定义的地图名称
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
                }
                
                // 查询地图信息，获取用户定义的地图名称
                try {
                    MapDTO mapDTO = mapService.getMapById(mapId, parseUserId(userId));
                    if (mapDTO != null) {
                        userMapName = mapDTO.getName();  // 获取用户定义的地图名称
                        log.info("Found map name for mapId {}: {}", mapId, userMapName);
                        
                        // 如果没有 xmlPath，使用默认路径
                        if (request.getSimInfo().getMapXmlPath() == null || 
                            request.getSimInfo().getMapXmlPath().trim().isEmpty()) {
                            String defaultXmlPath = mapId + "/" + mapId + ".xml";
                            request.getSimInfo().setMapXmlPath(defaultXmlPath);
                            log.info("Using default xmlPath for mapId {}: {}", mapId, defaultXmlPath);
                        }
                    } else {
                        log.error("No map found for mapId: {}", mapId);
                    }
                } catch (Exception e) {
                    log.error("Failed to get map info for mapId: {}", mapId, e);
                }
            } else {
                log.warn("mapId is null or empty, cannot lookup xmlPath");
            }
        }

        // 调用Python服务创建仿真引擎（支持容错，gRPC不可用时使用兜底数据）
        ApiResponse response = simulationPythonGrpcClient.createSimeng(request, taskId);

        // 保存仿真任务记录
        SimulationTask task = new SimulationTask();
        task.setTaskId(taskId);
        task.setName(request.getSimInfo().getName());
        task.setMapId(request.getSimInfo().getMapId());  // 保存用户的地图ID
        task.setMapName(userMapName);  // 保存用户定义的地图名称
        task.setMapXmlName(request.getSimInfo() != null ? request.getSimInfo().getMapXmlName() : null);  // 引擎内部的随机UUID
        task.setMapXmlPath(request.getSimInfo() != null ? request.getSimInfo().getMapXmlPath() : null);
        
        // 从 simConfig 中移除地图图片和地图JSON数据，避免数据冗余
        // 这些数据已经保存在 map 表中，不需要在 simulation_task 表中重复保存
        CreateSimulationRequest cleanedRequest = cleanSimConfigForStorage(request);
        task.setSimConfig(JsonUtils.toJson(cleanedRequest));

        // 检查响应，如果是兜底响应（包含gRPC不可用提示），记录警告但继续执行
        if (response.getMsg() != null && response.getMsg().contains("gRPC unavailable")) {
            log.warn("gRPC service unavailable, using fallback response. Message: {}", response.getMsg());
            task.setStatus("CREATED");
        } else if (!ErrorCode.ERR_OK.equals(response.getRes())) {
            throw new BusinessException(response.getRes(),
                    "Failed to create simulation engine: " + response.getMsg());
        } else if (response.getData() != null && response.getData().toString().contains("Failed")) {
            log.error("Simulation engine failed to start: {}", response.getData());
            task.setStatus("FAILED");
        } else {
            task.setStatus("CREATED");
        }

        // 使用真实的 userId
        task.setUserId(parseUserId(userId));
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());

        task = simulationTaskRepository.save(task);
        
        // 保存地图 JSON 数据到 MongoDB（如果存在）
        saveMapJsonToMongoDB(request, taskId, userId);

        // 更新 SessionService 中的 SimInfo
        SimInfo updatedSimInfo = sessionService.getSessionInfo(taskId);
        if (updatedSimInfo != null) {
            if (updatedSimInfo.getSimInfo() == null) {
                updatedSimInfo.setSimInfo(new HashMap<>());
            }
            updatedSimInfo.getSimInfo().put("taskId", taskId);
            sessionService.updateSessionInfo(taskId, updatedSimInfo);
            log.info("Updated session {} with taskId: {}", taskId, taskId);
        }

        return convertToDTO(task);
    }

    @Override
    @Deprecated
    @Transactional
    public SimulationTaskDTO createSimulation(CreateSimulationRequest request, String userId, String taskId) {
        log.warn("Using deprecated createSimulation method. Please use prepareSimulation + startSimulation instead.");
        
        // 调用新的两步流程
        prepareSimulation(taskId, userId);
        return startSimulation(request, userId, taskId);
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
                .map(this::convertToSimpleDTO)  // 使用简化版本的转换方法
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
     * 转换为DTO（完整版本）
     */
    private SimulationTaskDTO convertToDTO(SimulationTask task) {
        SimulationTaskDTO dto = new SimulationTaskDTO();
        dto.setTaskId(task.getTaskId());
        dto.setName(task.getName());
        dto.setMapId(task.getMapId());
        dto.setMapName(task.getMapName());  // 用户定义的地图名称
        dto.setMapXmlName(task.getMapXmlName());  // 引擎内部的随机UUID
        dto.setMapXmlPath(task.getMapXmlPath());
        dto.setSimConfig(task.getSimConfig());
        dto.setStatus(task.getStatus());
        dto.setUserId(task.getUserId());
        dto.setCreateTime(task.getCreateTime());
        dto.setUpdateTime(task.getUpdateTime());
        return dto;
    }

    /**
     * 转换为简化DTO（仅包含列表展示需要的字段）
     * 字段：taskId、name、mapId、mapName、createTime
     */
    private SimulationTaskDTO convertToSimpleDTO(SimulationTask task) {
        SimulationTaskDTO dto = new SimulationTaskDTO();
        dto.setTaskId(task.getTaskId());
        dto.setName(task.getName());
        dto.setMapId(task.getMapId());  // 地图ID
        dto.setMapName(task.getMapName());  // 返回用户定义的地图名称，而不是引擎内部的UUID
        dto.setCreateTime(task.getCreateTime());
        // 其他字段不设置，保持为 null
        return dto;
    }

    /**
     * 清理仿真配置，移除不需要持久化的大数据字段
     * 移除 map_pic（地图图片Base64）和 map_json（地图JSON数据）
     * 这些数据已经保存在 map 表中，不需要在 simulation_task 表中重复保存
     * 
     * @param request 原始请求
     * @return 清理后的请求副本
     */
    private CreateSimulationRequest cleanSimConfigForStorage(CreateSimulationRequest request) {
        // 创建深拷贝，避免修改原始请求对象
        CreateSimulationRequest cleanedRequest = new CreateSimulationRequest();
        
        // 复制 simInfo
        if (request.getSimInfo() != null) {
            CreateSimulationRequest.SimInfoDTO cleanedSimInfo = new CreateSimulationRequest.SimInfoDTO();
            CreateSimulationRequest.SimInfoDTO originalSimInfo = request.getSimInfo();
            
            // 复制基本字段
            cleanedSimInfo.setName(originalSimInfo.getName());
            cleanedSimInfo.setMapId(originalSimInfo.getMapId());
            cleanedSimInfo.setMapXmlName(originalSimInfo.getMapXmlName());
            cleanedSimInfo.setMapXmlPath(originalSimInfo.getMapXmlPath());
            cleanedSimInfo.setFixedOd(originalSimInfo.getFixedOd());
            
            // 复制 additionalFields，但排除 map_pic 和 map_json
            if (originalSimInfo.getAdditionalFields() != null) {
                Map<String, Object> cleanedFields = new HashMap<>();
                originalSimInfo.getAdditionalFields().forEach((key, value) -> {
                    // 排除地图图片和地图JSON数据
                    if (!"map_pic".equals(key) && !"map_json".equals(key)) {
                        cleanedFields.put(key, value);
                    }
                });
                cleanedFields.forEach(cleanedSimInfo::setAdditionalField);
            }
            
            cleanedRequest.setSimInfo(cleanedSimInfo);
            log.info("Cleaned simConfig: removed map_pic and map_json from storage");
        }
        
        // 复制 controlViews
        cleanedRequest.setControlViews(request.getControlViews());
        
        return cleanedRequest;
    }
    
    /**
     * 保存地图 JSON 数据到 MongoDB
     * 从请求中提取 map_json 字段，保存到 MongoDB 的 map 集合
     * 
     * @param request 仿真请求
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    @SuppressWarnings("unchecked")
    private void saveMapJsonToMongoDB(CreateSimulationRequest request, String taskId, String userId) {
        try {
            if (request.getSimInfo() == null) {
                return;
            }
            
            // 从 mapJson 字段获取地图数据
            Object mapJsonObj = request.getSimInfo().getMapJson();
            Map<String, Object> mapJsonData = null;
            
            if (mapJsonObj instanceof Map) {
                mapJsonData = (Map<String, Object>) mapJsonObj;
            } else if (mapJsonObj == null) {
                // 尝试从 additionalFields 中获取 map_json
                if (request.getSimInfo().getAdditionalFields() != null) {
                    Object additionalMapJson = request.getSimInfo().getAdditionalFields().get("map_json");
                    if (additionalMapJson instanceof Map) {
                        mapJsonData = (Map<String, Object>) additionalMapJson;
                    }
                }
            }
            
            if (mapJsonData == null || mapJsonData.isEmpty()) {
                log.warn("No map_json data found in request for taskId: {}", taskId);
                return;
            }
            
            // 构建 MongoDB 文档
            Map<String, Object> document = new HashMap<>();
            document.put("userId", Long.parseLong(userId));
            document.put("taskId", taskId);
            document.put("mapId", request.getSimInfo().getMapId());
            document.put("mapName", request.getSimInfo().getName());
            document.put("addition", mapJsonData);  // 地图 JSON 数据存储在 addition 字段
            document.put("createdAt", System.currentTimeMillis());
            document.put("updatedAt", System.currentTimeMillis());
            
            // 保存到 MongoDB 的 map 集合
            mongoTemplate.save(document, "map");
            
            log.info("Saved map JSON data to MongoDB: taskId={}, userId={}, mapId={}, dataSize={}", 
                    taskId, userId, request.getSimInfo().getMapId(), mapJsonData.size());
        } catch (Exception e) {
            log.error("Failed to save map JSON to MongoDB for taskId: {}", taskId, e);
            // 不抛出异常，避免影响主流程
        }
    }
}

