package com.traffic.sim.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 统计数据模型
 * 
 * @author traffic-sim
 */
@Data
public class StatisticsData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 仿真步数 */
    private Long step;
    
    /** 时间戳 */
    private Long timestamp;
    
    /** 车辆总数 */
    private Integer vehicleCount;
    
    /** 平均速度 */
    private Double averageSpeed;
    
    /** 拥堵指数 */
    private Double congestionIndex;
    
    /** 信号灯状态列表 */
    private List<SignalState> signalStates;
    
    /** 自定义指标 */
    private Map<String, Object> custom;
    
    public StatisticsData() {
        this.timestamp = System.currentTimeMillis();
    }
}

