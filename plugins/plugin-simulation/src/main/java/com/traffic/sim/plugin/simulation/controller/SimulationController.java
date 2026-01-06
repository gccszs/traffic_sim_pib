package com.traffic.sim.plugin.simulation.controller;

import com.traffic.sim.common.constant.ErrorCode;
import com.traffic.sim.common.dto.CreateSimulationRequest;
import com.traffic.sim.common.dto.GreenRatioControlRequest;
import com.traffic.sim.common.dto.SimulationTaskDTO;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 仿真任务Controller
 * 
 * @author traffic-sim
 */
@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "仿真任务管理", description = "仿真任务的创建、查询和控制接口")
public class SimulationController {
    
    private final SimulationService simulationService;
    
    /**
     * 创建仿真引擎
     */
    @PostMapping("/create")
    @Operation(summary = "创建仿真任务", description = "创建新的仿真任务并初始化仿真引擎")
    public ResponseEntity<ApiResponse<String>> createSimulation(
            @RequestBody @Valid CreateSimulationRequest request,
            @CookieValue(value = "id", required = false) String sessionId) {
        
        log.info("Received create simulation request: name={}, sessionId={}", 
            request.getName(), sessionId);
        
        // 验证会话ID
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.ERR_AUTH, "Session ID is required"));
        }
        
        try {
            SimulationTaskDTO task = simulationService.createSimulation(request, sessionId);
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
        
        log.info("Received green ratio control request: greenRatio={}, sessionId={}", 
            request.getGreenRatio(), sessionId);
        
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

