package com.traffic.sim.plugin.replay.controller;

import com.traffic.sim.common.util.RequestContext;
import com.traffic.sim.common.dto.ReplayDataDTO;
import com.traffic.sim.plugin.replay.config.ReplayProperties;
import com.traffic.sim.plugin.replay.entity.ReplayTask;
import com.traffic.sim.plugin.replay.service.ReplayDataService;
import com.traffic.sim.plugin.replay.service.ReplayService;
import com.traffic.sim.plugin.replay.service.SseReplayControlService;
import com.traffic.sim.plugin.replay.service.SseReplayControlService.ReplayControlState;
import com.traffic.sim.plugin.replay.service.SseReplayControlService.ReplayStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 回放SSE控制器
 * 通过SSE（Server-Sent Events）推送回放数据
 * 支持动态控制：播放、暂停、倍速、跳转
 * 
 * sessionId = 仿真任务的 taskId
 * 
 * @author traffic-sim
 */
@RestController
@RequestMapping("/replay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "回放SSE推送", description = "通过SSE推送回放数据，支持动态控制")
public class ReplaySSEController {
    
    private final ReplayDataService replayDataService;
    private final SseReplayControlService controlService;
    private final ReplayProperties replayProperties;
    private final ReplayService replayService;  // 使用 Service 层
    
    // 用于异步推送数据的线程池
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();
    
    /**
     * 建立SSE连接，开始回放
     * 直接使用仿真任务ID进行回放
     * 支持动态控制：播放、暂停、倍速、跳转
     * 
     * sessionId = taskId（仿真任务ID）
     * 
     * @param taskId 仿真任务ID（simulation_task 表的 task_id，也是 sessionId）
     * @return SSE Emitter
     */
    @GetMapping(value = "/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE回放数据流", description = "通过SSE连接按步推送回放数据，支持动态控制。sessionId = taskId")
    public SseEmitter streamReplayData(@PathVariable String taskId) {
        
        log.info("[Replay] Starting SSE stream for task: {}", taskId);
        
        // 获取当前用户ID
        String currentUserId = RequestContext.getCurrentUserId();
        Long userId = null;
        if (currentUserId != null) {
            try {
                userId = Long.parseLong(currentUserId);
            } catch (NumberFormatException e) {
                log.warn("[Replay] Invalid user ID format: {}", currentUserId);
            }
        }
        
        // 创建回放会话（sessionId = taskId）
        ReplayControlState controlState = controlService.createSession(taskId);
        
        // 创建回放任务记录（通过 Service 层）
        String replayTaskId = replayService.createReplayHistory(taskId, userId);
        
        // 创建SSE Emitter（从配置读取超时时间）
        SseEmitter emitter = new SseEmitter(replayProperties.getSse().getTimeoutMs());
        
        // 异步推送数据
        sseExecutor.execute(() -> {
            try {
                pushReplayDataWithControl(emitter, taskId, controlState, replayTaskId);
            } catch (Exception e) {
                log.error("[Replay] Error during SSE stream for task: {}", taskId, e);
                replayService.updateReplayHistoryStatus(replayTaskId, ReplayTask.ReplayStatus.STOPPED.getCode(), controlState.getCurrentStep());
                emitter.completeWithError(e);
            } finally {
                controlService.removeSession(taskId);
            }
        });
        
        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("[Replay] SSE timeout for task: {}", taskId);
            replayService.updateReplayHistoryStatus(replayTaskId, ReplayTask.ReplayStatus.STOPPED.getCode(), controlState.getCurrentStep());
            controlService.removeSession(taskId);
            emitter.complete();
        });
        
        emitter.onCompletion(() -> {
            log.info("[Replay] SSE completed for task: {}", taskId);
            controlService.removeSession(taskId);
        });
        
        emitter.onError(throwable -> {
            log.error("[Replay] SSE error for task: {}", taskId, throwable);
            replayService.updateReplayHistoryStatus(replayTaskId, ReplayTask.ReplayStatus.STOPPED.getCode(), controlState.getCurrentStep());
            controlService.removeSession(taskId);
        });
        
        return emitter;
    }
    
    /**
     * 推送回放数据（支持即时动态控制）
     * 
     * 控制操作在回放的任何时刻都能立即生效：
     * - 暂停：立即停止推送，保持当前步数
     * - 播放：立即恢复推送
     * - 倍速：立即调整推送速度
     * - 跳转：立即跳转到目标步数
     * 
     * @param emitter SSE Emitter
     * @param taskId 仿真任务ID（sessionId）
     * @param controlState 控制状态
     * @param replayTaskId 回放任务ID
     */
    private void pushReplayDataWithControl(SseEmitter emitter, String taskId, 
                                          ReplayControlState controlState,
                                          String replayTaskId) 
            throws IOException, InterruptedException {
        
        // 查询所有回放数据
        List<ReplayDataDTO> replayDataList = replayDataService.getReplayData(taskId, 0L, Long.MAX_VALUE);
        
        if (replayDataList.isEmpty()) {
            log.warn("[Replay] No data found for task: {}", taskId);
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("没有找到回放数据"));
            replayService.updateReplayHistoryStatus(replayTaskId, ReplayTask.ReplayStatus.STOPPED.getCode(), 0L);
            emitter.complete();
            return;
        }
        
        log.info("[Replay] Loaded {} steps for task: {}", replayDataList.size(), taskId);
        
        // 更新回放任务总步数
        replayService.updateReplayHistoryTotalSteps(replayTaskId, (long) replayDataList.size());
        
        // 发送开始事件
        emitter.send(SseEmitter.event()
                .name("start")
                .data("{\"totalSteps\": " + replayDataList.size() + "}"));
        
        // 发送就绪事件
        emitter.send(SseEmitter.event()
                .name("ready")
                .data("{\"message\": \"回放已就绪，等待播放指令\", \"status\": \"PAUSED\"}"));
        
        // 从配置读取基础延迟时间和暂停检查间隔
        final long baseDelayMs = replayProperties.getSse().getBaseDelayMs();
        final long pauseCheckIntervalMs = replayProperties.getSse().getPauseCheckIntervalMs();
        
        // 逐步推送数据
        long currentStep = 0;
        boolean stoppedByUser = false;
        
        while (currentStep < replayDataList.size()) {
            
            // 【即时控制】检查是否停止
            if (controlState.getStatus() == ReplayStatus.STOPPED) {
                log.info("[Replay] Stopped at step {}/{}", currentStep, replayDataList.size());
                emitter.send(SseEmitter.event()
                        .name("stopped")
                        .data("{\"currentStep\": " + currentStep + "}"));
                stoppedByUser = true;
                break;
            }
            
            // 【即时控制】检查是否暂停
            while (controlState.getStatus() == ReplayStatus.PAUSED) {
                if (controlState.getStatus() == ReplayStatus.STOPPED) {
                    log.info("[Replay] Stopped during pause at step {}/{}", currentStep, replayDataList.size());
                    emitter.send(SseEmitter.event()
                            .name("stopped")
                            .data("{\"currentStep\": " + currentStep + "}"));
                    stoppedByUser = true;
                    break;
                }
                
                // 【即时响应】在暂停期间也检查跳转请求
                if (controlState.isSeekRequested()) {
                    long targetStep = controlState.getTargetStep();
                    if (targetStep >= 0 && targetStep < replayDataList.size()) {
                        log.info("[Replay] Seek during pause: {} -> {}", currentStep, targetStep);
                        currentStep = targetStep;
                        controlService.clearSeekRequest(taskId);
                        emitter.send(SseEmitter.event()
                                .name("seeked")
                                .data("{\"currentStep\": " + currentStep + "}"));
                    } else {
                        controlService.clearSeekRequest(taskId);
                    }
                }
                
                Thread.sleep(pauseCheckIntervalMs);
            }
            
            if (stoppedByUser) {
                break;
            }
            
            // 【即时控制】检查是否有跳转请求
            if (controlState.isSeekRequested()) {
                long targetStep = controlState.getTargetStep();
                if (targetStep >= 0 && targetStep < replayDataList.size()) {
                    log.info("[Replay] Seek: {} -> {}", currentStep, targetStep);
                    currentStep = targetStep;
                    controlService.clearSeekRequest(taskId);
                    emitter.send(SseEmitter.event()
                            .name("seeked")
                            .data("{\"currentStep\": " + currentStep + "}"));
                } else {
                    controlService.clearSeekRequest(taskId);
                }
            }
            
            // 发送当前步数据
            ReplayDataDTO stepData = replayDataList.get((int) currentStep);
            emitter.send(SseEmitter.event()
                    .name("data")
                    .data(stepData));
            
            // 更新内存中的当前步数
            controlService.updateCurrentStep(taskId, currentStep);
            
            // 递增步数
            currentStep++;
            
            // 【即时响应】将延迟拆分成多个短 sleep，以便快速响应控制命令
            double currentSpeed = controlState.getSpeed();
            long delayMs = (long) (baseDelayMs / currentSpeed);
            long remainingDelayMs = delayMs;
            
            while (remainingDelayMs > 0) {
                // 每次 sleep 最多 50ms，以便快速响应
                long sleepTime = Math.min(remainingDelayMs, 50);
                Thread.sleep(sleepTime);
                remainingDelayMs -= sleepTime;
                
                // 【即时响应】在延迟期间检查跳转和速度变化
                if (controlState.isSeekRequested()) {
                    // 有跳转请求，立即中断延迟
                    break;
                }
                
                // 【即时响应】速度变化时重新计算剩余延迟
                double newSpeed = controlState.getSpeed();
                if (Math.abs(newSpeed - currentSpeed) > 0.01) {
                    // 速度变化，重新计算剩余延迟时间
                    long elapsedMs = delayMs - remainingDelayMs;
                    long newTotalDelayMs = (long) (baseDelayMs / newSpeed);
                    remainingDelayMs = Math.max(0, newTotalDelayMs - elapsedMs);
                    currentSpeed = newSpeed;
                }
                
                // 检查是否暂停或停止
                if (controlState.getStatus() != ReplayStatus.PLAYING) {
                    break;
                }
            }
        }
        
        // 【回放结束】保存回放历史记录
        if (stoppedByUser) {
            log.info("[Replay] History saved: STOPPED at step {}", currentStep);
            replayService.updateReplayHistoryStatus(
                replayTaskId, 
                ReplayTask.ReplayStatus.STOPPED.getCode(), 
                currentStep
            );
            replayService.updateReplayHistorySpeed(replayTaskId, controlState.getSpeed());
        } else {
            log.info("[Replay] History saved: FINISHED at step {}", replayDataList.size());
            emitter.send(SseEmitter.event()
                    .name("end")
                    .data("{\"message\": \"回放完成\", \"totalSteps\": " + replayDataList.size() + "}"));
            
            replayService.updateReplayHistoryStatus(
                replayTaskId, 
                ReplayTask.ReplayStatus.FINISHED.getCode(), 
                (long) replayDataList.size()
            );
            replayService.updateReplayHistorySpeed(replayTaskId, controlState.getSpeed());
        }
        
        emitter.complete();
    }
}
