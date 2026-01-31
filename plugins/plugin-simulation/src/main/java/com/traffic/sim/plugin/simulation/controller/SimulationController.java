package com.traffic.sim.plugin.simulation.controller;

import com.traffic.sim.common.constant.ErrorCode;
import com.traffic.sim.common.dto.CreateSimulationRequest;
import com.traffic.sim.common.dto.GreenRatioControlRequest;
import com.traffic.sim.common.dto.SimulationTaskDTO;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.service.SimulationService;
import com.traffic.sim.common.util.RequestContext;
import com.traffic.sim.plugin.simulation.service.PluginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 仿真任务Controller
 *
 * @author traffic-sim
 */
@RestController
@RequestMapping("/simulation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "仿真任务管理", description = "仿真任务的创建、查询和控制接口")
public class SimulationController {

    private final SimulationService simulationService;
    private final PluginService pluginService;

    /**
     * 获取所有插件信息
     * GET /api/simulation/get_plugin_info
     */
    @GetMapping("/get_plugin_info")
    @Operation(summary = "获取插件信息", description = "从Python引擎获取所有插件及其配置信息")
    public ResponseEntity<Map<String, Object>> getPluginInfo() {
        log.info("Request to get all plugin info");
        try {
            Map<String, Object> result = pluginService.getPluginInfo(null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get plugin info", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "res", "ERR_SYSTEM",
                    "msg", e.getMessage()
            ));
        }
    }

    /**
     * 获取指定插件信息
     * GET /api/simulation/get_plugin_info/{pluginName}
     */
    @GetMapping("/get_plugin_info/{pluginName}")
    @Operation(summary = "获取指定插件信息")
    public ResponseEntity<Map<String, Object>> getPluginInfoByName(@PathVariable String pluginName) {
        log.info("Request to get plugin info for: {}", pluginName);
        try {
            Map<String, Object> result = pluginService.getPluginInfo(pluginName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get plugin info for: " + pluginName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "res", "ERR_SYSTEM",
                    "msg", e.getMessage()
            ));
        }
    }

    /**
     * 上传插件 (通过 gRPC)
     * POST /api/simulation/upload_plugin
     */
    @PostMapping("/upload_plugin")
    @Operation(summary = "上传插件", description = "通过gRPC将插件zip包发送至Python引擎")
    public ResponseEntity<Map<String, Object>> uploadPlugin(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        log.info("Request to upload plugin: {}", file.getOriginalFilename());
        try {
            Map<String, Object> result = pluginService.uploadPlugin(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to upload plugin", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "res", "ERR_SYSTEM",
                    "msg", e.getMessage()
            ));
        }
    }

    /**
     * 更新插件信息
     * POST /api/simulation/update_plugin_info
     */
    @PostMapping("/update_plugin_info")
    @Operation(summary = "更新插件信息")
    public ResponseEntity<Map<String, Object>> updatePluginInfo(
            @RequestBody Map<String, Object> request) {
        log.info("Request to update plugin info");
        try {
            String pluginName = (String) request.get("pluginName");
            Object updateInfosObj = request.get("updateInfos");
            Boolean applyDisk = (Boolean) request.getOrDefault("applyDisk", true);

            // 将 updateInfos 转换为 List<Map<String, Object>>
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> updateInfos = (List<Map<String, Object>>) updateInfosObj;

            Map<String, Object> result = pluginService.updatePluginInfo(pluginName, updateInfos, applyDisk);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to update plugin info", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "res", "ERR_SYSTEM",
                    "msg", e.getMessage()
            ));
        }
    }

    /**
     * 准备仿真任务（生成taskId并创建session）
     */
    @PostMapping("/prepare")
    @Operation(summary = "准备仿真任务", description = "生成taskId并创建session，返回taskId供前端连接WebSocket")
    public ResponseEntity<ApiResponse<String>> prepareSimulation() {
        String currentUserId = RequestContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(ErrorCode.ERR_AUTH, "未认证"));
        }

        // 生成唯一的taskId
        String taskId = UUID.randomUUID().toString().replace("-", "");
        log.info("Preparing simulation task: userId={}, taskId={}", currentUserId, taskId);

        try {
            // 创建session，供前端和引擎连接WebSocket使用
            simulationService.prepareSimulation(taskId, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("Task prepared successfully", taskId));
        } catch (Exception e) {
            log.error("Failed to prepare simulation", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.ERR_CREATE, e.getMessage()));
        }
    }

    /**
     * 启动仿真引擎
     */
    @PostMapping("/start")
    @Operation(summary = "启动仿真引擎", description = "使用taskId启动仿真引擎（前端应先连接WebSocket）")
    public ResponseEntity<ApiResponse<String>> startSimulation(
            @RequestBody @Valid CreateSimulationRequest request,
            @RequestParam String taskId) {
        String currentUserId = RequestContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(ErrorCode.ERR_AUTH, "未认证"));
        }

        log.info("Starting simulation engine: userId={}, taskId={}", currentUserId, taskId);

        try {
            SimulationTaskDTO task = simulationService.startSimulation(request, currentUserId, taskId);
            return ResponseEntity.ok(ApiResponse.success("Simulation started successfully", task.getTaskId()));
        } catch (Exception e) {
            log.error("Failed to start simulation", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.ERR_CREATE, e.getMessage()));
        }
    }

    /**
     * 创建仿真引擎（兼容旧接口）
     * @deprecated 请使用 /prepare 和 /start 两步流程
     */
    @Deprecated
    @PostMapping("/create")
    @Operation(summary = "创建仿真任务（已废弃）", description = "创建新的仿真任务并初始化仿真引擎。建议使用 /prepare 和 /start 两步流程")
    public ResponseEntity<ApiResponse<String>> createSimulation(
            @RequestBody @Valid CreateSimulationRequest request) {
        String currentUserId = RequestContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(ErrorCode.ERR_AUTH, "未认证"));
        }

        // 生成唯一的taskId（每次创建都不同）
        String simTaskId = UUID.randomUUID().toString().replace("-", "");
        log.info("Received create simulation request (deprecated): userId={}, taskId={}", currentUserId, simTaskId);

        try {
            // 准备
            simulationService.prepareSimulation(simTaskId, currentUserId);
            // 启动
            SimulationTaskDTO task = simulationService.startSimulation(request, currentUserId, simTaskId);
            return ResponseEntity.ok(ApiResponse.success("Simulation created successfully", task.getTaskId()));
        } catch (Exception e) {
            log.error("Failed to create simulation", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.ERR_CREATE, e.getMessage()));
        }
    }

    /**
     * 获取仿真任务列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取仿真任务列表", description = "分页查询仿真任务列表")
    public ResponseEntity<ApiResponse<PageResult<SimulationTaskDTO>>> getSimulationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            PageResult<SimulationTaskDTO> result = simulationService.getSimulationList(page, size);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Failed to get simulation list", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.ERR_UNKNOWN, e.getMessage()));
        }
    }

    /**
     * 获取仿真任务详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取仿真任务详情", description = "根据任务ID获取仿真任务详细信息")
    public ResponseEntity<ApiResponse<SimulationTaskDTO>> getSimulationTask(
            @PathVariable String taskId) {

        try {
            SimulationTaskDTO task = simulationService.getSimulationTask(taskId);
            return ResponseEntity.ok(ApiResponse.success(task));
        } catch (Exception e) {
            log.error("Failed to get simulation task: {}", taskId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.ERR_NOT_FOUND, e.getMessage()));
        }
    }

    /**
     * 绿信比控制
     */
    @PostMapping("/control_green_ratio")
    @Operation(summary = "滤信比控制", description = "实时调整信号灯的绿信比值（0-100）")
    public ResponseEntity<ApiResponse<String>> controlGreenRatio(
            @RequestBody @Valid GreenRatioControlRequest request,
            @CookieValue(value = "id", required = false) String sessionId) {

        String currentUserId = RequestContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(ErrorCode.ERR_AUTH, "未认证"));
        }

        log.info("Received green ratio control request: greenRatio={}, sessionId={}, userId={}",
                request.getGreenRatio(), sessionId, currentUserId);

        // 验证会话ID
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.ERR_AUTH, "Session ID is required"));
        }

        try {
            simulationService.controlGreenRatio(
                    request.getGreenRatio(),
                    sessionId,
                    request.getSimulationInfo() != null ?
                            request.getSimulationInfo() : Collections.emptyMap()
            );

            return ResponseEntity.ok(ApiResponse.success("Green ratio updated successfully"));
        } catch (Exception e) {
            log.error("Failed to control green ratio", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.ERR_ENGINE, e.getMessage()));
        }
    }
}

