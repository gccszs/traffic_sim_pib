package com.traffic.sim.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 仿真任务DTO
 * 
 * @author traffic-sim
 */
@Data
public class SimulationTaskDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 任务ID */
    private String taskId;
    
    /** 仿真名称 */
    private String name;
    
    /** 地图XML文件名 */
    private String mapXmlName;
    
    /** 地图XML文件路径 */
    private String mapXmlPath;
    
    /** 仿真配置（JSON字符串） */
    private String simConfig;
    
    /** 状态：CREATED/RUNNING/PAUSED/STOPPED/FINISHED */
    private String status;
    
    /** 用户ID */
    private Long userId;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
}

