package com.traffic.sim.plugin.replay.controller;

import com.traffic.sim.common.exception.BusinessException;
import com.traffic.sim.common.util.RequestContext;
import com.traffic.sim.common.dto.ReplayDataDTO;
import com.traffic.sim.plugin.replay.entity.ReplayTask;
import com.traffic.sim.plugin.replay.repository.ReplayTaskRepository;
import com.traffic.sim.plugin.replay.service.ReplayDataService;
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
 * 
 * @author traffic-sim
 */
@RestController
@RequestMapping("/replay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "回放SSE推送", description = "通过SSE推送回放数据")
public class ReplaySSEController {
    
    private final ReplayDataService replayDataService;
    private final ReplayTaskRepository replayTaskRepository;
    
    // 用于异步推送数据的线程池
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();
    
    /**
     * 建立SSE连接，开始回放
     * 
     * @param taskId 回放任务ID
     * @param speed 回放速度（倍速，默认1.0）
     * @return SSE Emitter
     */
    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE回放数据流", description = "通过SSE连接按步推送回放数据")
    public SseEmitter streamReplayData(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1.0") double speed) {
        
        String userIdStr = RequestContext.getCurrentUserId();
        if (userIdStr == null) {
            throw new BusinessException("用户未登录");
        }
        Long userId = Long.parseLong(userIdStr);
        
        log.info("Starting SSE replay stream for task: {}, user: {}, speed: {}", taskId, userId, speed);
        
        // 验证回放任务
        ReplayTask replayTask = replayTaskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException("回放任务不存在或无权限访问"));
        
        // 获取仿真任务ID
        String simulationTaskId = replayTask.getSimulationTaskId();
        
        // 创建SSE Emitter（超时时间30分钟）
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 异步推送数据
        sseExecutor.execute(() -> {
            try {
                pushReplayData(emitter, simulationTaskId, speed);
            } catch (Exception e) {
                log.error("Error during SSE replay stream for task: {}", taskId, e);
                emitter.completeWithError(e);
            }
        });
        
        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout for task: {}", taskId);
            emitter.complete();
        });
        
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for task: {}", taskId);
        });
        
        emitter.onError(throwable -> {
            log.error("SSE connection error for task: {}", taskId, throwable);
        });
        
        return emitter;
    }
    
    /**
     * 推送回放数据
     * 
     * @param emitter SSE Emitter
     * @param simulationTaskId 仿真任务ID
     * @param speed 播放速度
     */
    private void pushReplayData(SseEmitter emitter, String simulationTaskId, double speed) 
            throws IOException, InterruptedException {
        
        // 查询所有回放数据
        log.info("Loading replay data for simulation task: {}", simulationTaskId);
        List<ReplayDataDTO> replayDataList = replayDataService.getReplayData(simulationTaskId, 0L, Long.MAX_VALUE);
        
        if (replayDataList.isEmpty()) {
            log.warn("No replay data found for simulation task: {}", simulationTaskId);
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("没有找到回放数据"));
            emitter.complete();
            return;
        }
        
        log.info("Found {} steps of replay data, starting to push", replayDataList.size());
        
        // 发送开始事件
        emitter.send(SseEmitter.event()
                .name("start")
                .data("{\"totalSteps\": " + replayDataList.size() + "}"));
        
        // 计算每步的延迟时间（假设每步代表1秒，根据速度调整）
        long delayMs = (long) (1000 / speed);
        
        // 逐步推送数据
        for (int i = 0; i < replayDataList.size(); i++) {
            ReplayDataDTO stepData = replayDataList.get(i);
            
            // 发送数据事件
            emitter.send(SseEmitter.event()
                    .name("data")
                    .data(stepData));
            
            log.debug("Pushed step {} / {}", i + 1, replayDataList.size());
            
            // 延迟（模拟实时播放）
            if (i < replayDataList.size() - 1) {
                Thread.sleep(delayMs);
            }
        }
        
        // 发送结束事件
        emitter.send(SseEmitter.event()
                .name("end")
                .data("{\"message\": \"回放完成\"}"));
        
        log.info("Completed pushing {} steps of replay data", replayDataList.size());
        emitter.complete();
    }
}
