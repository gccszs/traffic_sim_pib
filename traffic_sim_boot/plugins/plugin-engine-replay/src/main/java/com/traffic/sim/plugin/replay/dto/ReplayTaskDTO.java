package com.traffic.sim.plugin.replay.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 回放任务DTO
 * 
 * @author traffic-sim
 */
@Data
public class ReplayTaskDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String taskId;
    private String simulationTaskId;
    private String name;
    private String status;
    private Long currentStep;
    private Long totalSteps;
    private Double playbackSpeed;
    private Long userId;
    private Date createTime;
    private Date updateTime;
}

