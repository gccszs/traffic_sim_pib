package com.traffic.sim.plugin.replay.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 回放数据DTO
 * 
 * @author traffic-sim
 */
@Data
public class ReplayDataDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 仿真步数
     */
    private Long step;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 仿真数据
     */
    private Map<String, Object> simData;
    
    /**
     * 统计数据
     */
    private Map<String, Object> statistics;
}

