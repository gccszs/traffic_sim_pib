package com.traffic.sim.plugin.replay.service;

import com.traffic.sim.common.exception.BusinessException;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.plugin.replay.config.ReplayPluginProperties;
import com.traffic.sim.plugin.replay.dto.*;
import com.traffic.sim.plugin.replay.entity.ReplayTask;
import com.traffic.sim.plugin.replay.repository.ReplayTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 回放服务实现类
 * 
 * @author traffic-sim
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReplayServiceImpl implements ReplayService {
    
    private final ReplayTaskRepository replayTaskRepository;
    private final ReplayDataService replayDataService;
    private final ReplayPluginProperties properties;
    
    @Override
    @Transactional
    public ReplayTaskDTO createReplayTask(CreateReplayTaskRequest request, Long userId) {
        log.info("Creating replay task for simulation task: {}, user: {}", 
                request.getSimulationTaskId(), userId);
        
        // 生成任务ID
        String taskId = UUID.randomUUID().toString().replace("-", "");
        
        // 创建回放任务实体
        ReplayTask replayTask = new ReplayTask();
        replayTask.setTaskId(taskId);
        replayTask.setSimulationTaskId(request.getSimulationTaskId());
        replayTask.setName(request.getName());
        replayTask.setStatus(ReplayTask.ReplayStatus.CREATED.getCode());
        replayTask.setCurrentStep(0L);
        replayTask.setTotalSteps(0L);
        replayTask.setPlaybackSpeed(properties.getReplay().getDefaultSpeed());
        replayTask.setUserId(userId);
        
        // 保存到数据库
        replayTask = replayTaskRepository.save(replayTask);
        
        // 转换为DTO
        return convertToDTO(replayTask);
    }
    
    @Override
    public ReplayTaskDTO getReplayTask(String taskId, Long userId) {
        ReplayTask replayTask = replayTaskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException("回放任务不存在或无权限访问"));
        
        return convertToDTO(replayTask);
    }
    
    @Override
    public PageResult<ReplayTaskDTO> getReplayTaskList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<ReplayTask> taskPage = replayTaskRepository.findByUserId(userId, pageable);
        
        List<ReplayTaskDTO> content = taskPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return new PageResult<>(
                content,
                taskPage.getTotalElements(),
                page,
                size
        );
    }
    
    @Override
    public List<ReplayDataDTO> getReplayData(String taskId, Long userId, Long startStep, Long endStep) {
        // 验证权限
        ReplayTask replayTask = replayTaskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException("回放任务不存在或无权限访问"));
        
        // 验证步数范围
        if (startStep < 0 || endStep < startStep) {
            throw new BusinessException("无效的步数范围");
        }
        
        if (replayTask.getTotalSteps() > 0 && endStep > replayTask.getTotalSteps()) {
            throw new BusinessException("步数超出范围，最大步数: " + replayTask.getTotalSteps());
        }
        
        // 从MongoDB获取回放数据
        return replayDataService.getReplayData(taskId, startStep, endStep);
    }
    
    @Override
    @Transactional
    public ReplayTaskDTO controlReplay(String taskId, Long userId, ReplayControlRequest request) {
        ReplayTask replayTask = replayTaskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException("回放任务不存在或无权限访问"));
        
        ReplayControlRequest.ReplayControlAction action = request.getAction();
        
        switch (action) {
            case PLAY:
                if (!ReplayTask.ReplayStatus.PAUSED.getCode().equals(replayTask.getStatus()) &&
                    !ReplayTask.ReplayStatus.STOPPED.getCode().equals(replayTask.getStatus())) {
                    throw new BusinessException("当前状态无法播放");
                }
                replayTask.setStatus(ReplayTask.ReplayStatus.PLAYING.getCode());
                break;
                
            case PAUSE:
                if (!ReplayTask.ReplayStatus.PLAYING.getCode().equals(replayTask.getStatus())) {
                    throw new BusinessException("当前状态无法暂停");
                }
                replayTask.setStatus(ReplayTask.ReplayStatus.PAUSED.getCode());
                break;
                
            case STOP:
                replayTask.setStatus(ReplayTask.ReplayStatus.STOPPED.getCode());
                replayTask.setCurrentStep(0L);
                break;
                
            case SEEK:
                if (request.getTargetStep() == null) {
                    throw new BusinessException("跳转操作需要指定目标步数");
                }
                if (replayTask.getTotalSteps() > 0 && request.getTargetStep() > replayTask.getTotalSteps()) {
                    throw new BusinessException("目标步数超出范围");
                }
                replayTask.setCurrentStep(request.getTargetStep());
                break;
                
            case SET_SPEED:
                if (request.getSpeed() == null) {
                    throw new BusinessException("设置速度操作需要指定速度值");
                }
                double speed = request.getSpeed();
                if (speed < properties.getReplay().getMinSpeed() || 
                    speed > properties.getReplay().getMaxSpeed()) {
                    throw new BusinessException(
                            String.format("速度必须在 %.1f 到 %.1f 之间", 
                                    properties.getReplay().getMinSpeed(),
                                    properties.getReplay().getMaxSpeed()));
                }
                replayTask.setPlaybackSpeed(speed);
                break;
                
            default:
                throw new BusinessException("不支持的控制操作: " + action);
        }
        
        replayTask = replayTaskRepository.save(replayTask);
        log.info("Replay task {} control action: {}, new status: {}", 
                taskId, action, replayTask.getStatus());
        
        return convertToDTO(replayTask);
    }
    
    @Override
    @Transactional
    public void deleteReplayTask(String taskId, Long userId) {
        ReplayTask replayTask = replayTaskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException("回放任务不存在或无权限访问"));
        
        // 删除MongoDB中的回放数据
        replayDataService.deleteReplayData(taskId);
        
        // 删除任务记录
        replayTaskRepository.delete(replayTask);
        
        log.info("Deleted replay task: {}", taskId);
    }
    
    /**
     * 转换为DTO
     */
    private ReplayTaskDTO convertToDTO(ReplayTask replayTask) {
        ReplayTaskDTO dto = new ReplayTaskDTO();
        dto.setTaskId(replayTask.getTaskId());
        dto.setSimulationTaskId(replayTask.getSimulationTaskId());
        dto.setName(replayTask.getName());
        dto.setStatus(replayTask.getStatus());
        dto.setCurrentStep(replayTask.getCurrentStep());
        dto.setTotalSteps(replayTask.getTotalSteps());
        dto.setPlaybackSpeed(replayTask.getPlaybackSpeed());
        dto.setUserId(replayTask.getUserId());
        dto.setCreateTime(replayTask.getCreateTime());
        dto.setUpdateTime(replayTask.getUpdateTime());
        return dto;
    }
}

