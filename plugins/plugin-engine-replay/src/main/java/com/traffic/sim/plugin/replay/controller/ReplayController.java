package com.traffic.sim.plugin.replay.controller;

import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.plugin.replay.dto.CreateReplayTaskRequest;
import com.traffic.sim.plugin.replay.dto.ReplayControlRequest;
import com.traffic.sim.plugin.replay.entity.ReplayTask;
import com.traffic.sim.plugin.replay.service.ReplayService;
import com.traffic.sim.plugin.replay.service.SseReplayControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    private final SseReplayControlService sseControlService;
    
    /**
     * 获取仿真任务的地图信息（用于回放）
     * 直接通过仿真任务ID获取地图数据
     * 
     * @param taskId 仿真任务ID（simulation_task 表的 task_id）
     */
    @GetMapping("/map/{taskId}")
    @Operation(summary = "获取回放地图信息", description = "通过仿真任务ID获取地图JSON数据（addition字段）")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReplayMapInfo(
            @PathVariable String taskId) {
        
        try {
            Map<String, Object> mapInfo = replayService.getReplayMapInfo(taskId);
            return ResponseEntity.ok(ApiResponse.success("获取地图信息成功", mapInfo));
        } catch (Exception e) {
            log.error("Failed to get replay map info for simulationTaskId: {}", taskId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取地图信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取仿真任务的回放数据统计信息
     * 
     * @param taskId 仿真任务ID
     */
    @GetMapping("/info/{taskId}")
    @Operation(summary = "获取回放信息", description = "获取仿真任务的回放数据信息（总步数等）")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReplayInfo(
            @PathVariable String taskId) {
        
        try {
            Map<String, Object> info = replayService.getReplayInfo(taskId);
            return ResponseEntity.ok(ApiResponse.success("获取回放信息成功", info));
        } catch (Exception e) {
            log.error("Failed to get replay info for simulationTaskId: {}", taskId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取回放信息失败: " + e.getMessage()));
        }
    }
    
    // ========== SSE 回放控制接口 ==========
    
    /**
     * 播放/继续回放
     */
    @PostMapping("/control/{sessionId}/play")
    @Operation(summary = "播放回放", description = "开始或继续播放回放")
    public ResponseEntity<ApiResponse<String>> play(@PathVariable String sessionId) {
        try {
            sseControlService.play(sessionId);
            return ResponseEntity.ok(ApiResponse.success("播放成功"));
        } catch (Exception e) {
            log.error("Failed to play replay session: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("播放失败: " + e.getMessage()));
        }
    }
    
    /**
     * 暂停回放
     */
    @PostMapping("/control/{sessionId}/pause")
    @Operation(summary = "暂停回放", description = "暂停当前回放")
    public ResponseEntity<ApiResponse<String>> pause(@PathVariable String sessionId) {
        try {
            sseControlService.pause(sessionId);
            return ResponseEntity.ok(ApiResponse.success("暂停成功"));
        } catch (Exception e) {
            log.error("Failed to pause replay session: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("暂停失败: " + e.getMessage()));
        }
    }
    
    /**
     * 停止回放
     */
    @PostMapping("/control/{sessionId}/stop")
    @Operation(summary = "停止回放", description = "停止当前回放")
    public ResponseEntity<ApiResponse<String>> stop(@PathVariable String sessionId) {
        try {
            sseControlService.stop(sessionId);
            return ResponseEntity.ok(ApiResponse.success("停止成功"));
        } catch (Exception e) {
            log.error("Failed to stop replay session: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("停止失败: " + e.getMessage()));
        }
    }
    
    /**
     * 设置回放速度
     */
    @PostMapping("/control/{sessionId}/speed")
    @Operation(summary = "设置回放速度", description = "动态调整回放速度")
    public ResponseEntity<ApiResponse<String>> setSpeed(
            @PathVariable String sessionId,
            @RequestParam double speed) {
        try {
            if (speed < 0.1 || speed > 10.0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("速度必须在 0.1 到 10.0 之间"));
            }
            sseControlService.setSpeed(sessionId, speed);
            return ResponseEntity.ok(ApiResponse.success("速度设置成功"));
        } catch (Exception e) {
            log.error("Failed to set speed for replay session: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("设置速度失败: " + e.getMessage()));
        }
    }
    
    /**
     * 跳转到指定步数
     */
    @PostMapping("/control/{sessionId}/seek")
    @Operation(summary = "跳转步数", description = "跳转到指定的仿真步数")
    public ResponseEntity<ApiResponse<String>> seekTo(
            @PathVariable String sessionId,
            @RequestParam long targetStep) {
        try {
            if (targetStep < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("目标步数必须大于等于 0"));
            }
            sseControlService.seekTo(sessionId, targetStep);
            return ResponseEntity.ok(ApiResponse.success("跳转成功"));
        } catch (Exception e) {
            log.error("Failed to seek replay session: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("跳转失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取回放会话状态
     */
    @GetMapping("/control/{sessionId}/status")
    @Operation(summary = "获取回放状态", description = "获取当前回放会话的状态信息")
    public ResponseEntity<ApiResponse<SseReplayControlService.ReplayControlState>> getStatus(
            @PathVariable String sessionId) {
        try {
            SseReplayControlService.ReplayControlState state = sseControlService.getSessionState(sessionId);
            if (state == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("回放会话不存在"));
            }
            return ResponseEntity.ok(ApiResponse.success(state));
        } catch (Exception e) {
            log.error("Failed to get status for replay session: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取状态失败: " + e.getMessage()));
        }
    }
    
    // ========== 回放历史记录管理接口 ==========
    
    /**
     * 获取回放历史记录列表
     */
    @GetMapping("/history/list")
    @Operation(summary = "获取回放历史记录列表", description = "分页查询用户的回放历史记录")
    public ResponseEntity<ApiResponse<Page<ReplayTask>>> getReplayHistoryList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            String currentUserId = com.traffic.sim.common.util.RequestContext.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("未认证"));
            }
            
            Long userId = Long.parseLong(currentUserId);
            
            // 通过 Service 层获取数据
            Page<ReplayTask> replayTasks = replayService.getReplayHistoryList(userId, page, size);
            
            return ResponseEntity.ok(ApiResponse.success(replayTasks));
        } catch (Exception e) {
            log.error("Failed to get replay history list", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取回放历史失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取指定仿真任务的回放历史记录
     */
    @GetMapping("/history/simulation/{simulationTaskId}")
    @Operation(summary = "获取指定仿真任务的回放历史", description = "查询某个仿真任务的所有回放记录")
    public ResponseEntity<ApiResponse<List<ReplayTask>>> getReplayHistoryBySimulationTask(
            @PathVariable String simulationTaskId) {
        
        try {
            // 通过 Service 层获取数据
            List<ReplayTask> replayTasks = replayService.getReplayHistoryBySimulationTask(simulationTaskId);
            
            return ResponseEntity.ok(ApiResponse.success(replayTasks));
        } catch (Exception e) {
            log.error("Failed to get replay history for simulation task: {}", simulationTaskId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取回放历史失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取回放历史详情
     */
    @GetMapping("/history/{replayTaskId}")
    @Operation(summary = "获取回放历史详情", description = "根据回放任务ID获取详细信息")
    public ResponseEntity<ApiResponse<ReplayTask>> getReplayHistoryDetail(
            @PathVariable String replayTaskId) {
        
        try {
            // 通过 Service 层获取数据
            ReplayTask replayTask = replayService.getReplayHistoryDetail(replayTaskId);
            
            return ResponseEntity.ok(ApiResponse.success(replayTask));
        } catch (Exception e) {
            log.error("Failed to get replay history detail: {}", replayTaskId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取回放历史详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除回放历史记录
     */
    @DeleteMapping("/history/{replayTaskId}")
    @Operation(summary = "删除回放历史记录", description = "删除指定的回放历史记录")
    public ResponseEntity<ApiResponse<String>> deleteReplayHistory(
            @PathVariable String replayTaskId) {
        
        try {
            String currentUserId = com.traffic.sim.common.util.RequestContext.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("未认证"));
            }
            
            Long userId = Long.parseLong(currentUserId);
            
            // 通过 Service 层删除（包含权限验证）
            replayService.deleteReplayHistory(replayTaskId, userId);
            
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        } catch (Exception e) {
            log.error("Failed to delete replay history: {}", replayTaskId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("删除回放历史失败: " + e.getMessage()));
        }
    }
    
    /**
     * 统计用户回放次数
     */
    @GetMapping("/history/stats")
    @Operation(summary = "统计用户回放次数", description = "获取当前用户的回放统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReplayStats() {
        
        try {
            String currentUserId = com.traffic.sim.common.util.RequestContext.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("未认证"));
            }
            
            Long userId = Long.parseLong(currentUserId);
            
            // 通过 Service 层获取统计数据
            Map<String, Object> stats = replayService.getReplayStats(userId);
            
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to get replay stats", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取回放统计失败: " + e.getMessage()));
        }
    }

}

