package com.traffic.sim.plugin.replay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建回放任务请求
 * 
 * @author traffic-sim
 */
@Data
public class CreateReplayTaskRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 关联的仿真任务ID
     */
    @NotBlank(message = "仿真任务ID不能为空")
    private String simulationTaskId;
    
    /**
     * 回放任务名称
     */
    @NotBlank(message = "回放任务名称不能为空")
    private String name;
}

