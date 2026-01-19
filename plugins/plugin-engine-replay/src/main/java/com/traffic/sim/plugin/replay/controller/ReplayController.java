package com.traffic.sim.plugin.replay.controller;

import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.util.RequestContext;
import com.traffic.sim.plugin.replay.dto.CreateReplayTaskRequest;
import com.traffic.sim.plugin.replay.dto.ReplayControlRequest;
import com.traffic.sim.common.dto.ReplayDataDTO;
import com.traffic.sim.plugin.replay.dto.ReplayTaskDTO;
import com.traffic.sim.plugin.replay.service.ReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 回放功能Controller
 * 
 * @author traffic-sim
 */
@RestController
@RequestMapping("/replay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "回放管理", description = "仿真历史数据回放相关接口")
public class ReplayController {
    
    private final ReplayService replayService;
    
    /**
     * 创建回放任务
     */
    @PostMapping("/create")
    @Operation(summary = "创建回放任务", description = "根据仿真任务创建回放任务")
    public ResponseEntity<ApiResponse<ReplayTaskDTO>> createReplayTask(
            @Valid @RequestBody CreateReplayTaskRequest request) {
        String userIdStr = RequestContext.getCurrentUserId();
        if (userIdStr == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户未登录"));
        }
        Long userId = Long.parseLong(userIdStr);
        ReplayTaskDTO task = replayService.createReplayTask(request, userId);
        return ResponseEntity.ok(ApiResponse.success("回放任务创建成功", task));
    }
    
    /**
     * 获取回放任务详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取回放任务详情", description = "根据任务ID获取回放任务详细信息")
    public ResponseEntity<ApiResponse<ReplayTaskDTO>> getReplayTask(@PathVariable String taskId) {
        String userIdStr = RequestContext.getCurrentUserId();
        if (userIdStr == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户未登录"));
        }
        Long userId = Long.parseLong(userIdStr);
        ReplayTaskDTO task = replayService.getReplayTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(task));
    }
    
    /**
     * 获取回放任务列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取回放任务列表", description = "分页获取当前用户的回放任务列表")
    public ResponseEntity<ApiResponse<PageResult<ReplayTaskDTO>>> getReplayTaskList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userIdStr = RequestContext.getCurrentUserId();
        if (userIdStr == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户未登录"));
        }
        Long userId = Long.parseLong(userIdStr);
        PageResult<ReplayTaskDTO> result = replayService.getReplayTaskList(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 获取回放数据
     */
    @GetMapping("/{taskId}/data")
    @Operation(summary = "获取回放数据", description = "根据步数范围获取回放数据")
    public ResponseEntity<ApiResponse<List<ReplayDataDTO>>> getReplayData(
            @PathVariable String taskId,
            @RequestParam Long startStep,
            @RequestParam Long endStep) {
        String userIdStr = RequestContext.getCurrentUserId();
        if (userIdStr == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户未登录"));
        }
        Long userId = Long.parseLong(userIdStr);
        List<ReplayDataDTO> data = replayService.getReplayData(taskId, userId, startStep, endStep);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    /**
     * 控制回放
     */
    @PostMapping("/{taskId}/control")
    @Operation(summary = "控制回放", description = "控制回放播放、暂停、停止、跳转、设置速度等")
    public ResponseEntity<ApiResponse<ReplayTaskDTO>> controlReplay(
            @PathVariable String taskId,
            @Valid @RequestBody ReplayControlRequest request) {
        String userIdStr = RequestContext.getCurrentUserId();
        if (userIdStr == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户未登录"));
        }
        Long userId = Long.parseLong(userIdStr);
        ReplayTaskDTO task = replayService.controlReplay(taskId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("操作成功", task));
    }
    
    /**
     * 删除回放任务
     */
    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除回放任务", description = "删除指定的回放任务及其数据")
    public ResponseEntity<ApiResponse<String>> deleteReplayTask(@PathVariable String taskId) {
        String userIdStr = RequestContext.getCurrentUserId();
        if (userIdStr == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户未登录"));
        }
        Long userId = Long.parseLong(userIdStr);
        replayService.deleteReplayTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }
}

