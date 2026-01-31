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
    
    /** 最小速度 */
    private Double minSpeed;
    
    /** 最大速度 */
    private Double maxSpeed;
    
    /** 平均加速度 */
    private Double averageAcceleration;
    
    /** 最小加速度 */
    private Double minAcceleration;
    
    /** 最大加速度 */
    private Double maxAcceleration;
    
    /** 进入车辆数（当前步） */
    private Integer vehiclesIn;
    
    /** 离开车辆数（当前步） */
    private Integer vehiclesOut;
    
    /** 低速车辆数 */
    private Integer lowSpeedCount;
    
    /** 拥堵指数 */
    private Double congestionIndex;
    
    // 全局统计信息
    
    /** 总进入车辆数（累计） */
    private Integer totalVehiclesIn;
    
    /** 总离开车辆数（累计） */
    private Integer totalVehiclesOut;
    
    /** 最小排队长度 */
    private Double minQueueLength;
    
    /** 最大排队长度 */
    private Double maxQueueLength;
    
    /** 平均排队长度 */
    private Double averageQueueLength;
    
    /** 最小排队时间 */
    private Double minQueueTime;
    
    /** 最大排队时间 */
    private Double maxQueueTime;
    
    /** 平均排队时间 */
    private Double averageQueueTime;
    
    /** 最大停车次数 */
    private Integer maxStopCount;
    
    /** 最小停车次数 */
    private Integer minStopCount;
    
    /** 平均停车次数 */
    private Double averageStopCount;
    
    /** 最大延误 */
    private Double maxDelay;
    
    /** 最小延误 */
    private Double minDelay;
    
    /** 平均延误 */
    private Double averageDelay;
    
    /** 平均交叉口流量 */
    private Double averageCrossFlow;
    
    /** 平均道路流量 */
    private Double averageRoadFlow;
    
    /** 平均车道流量 */
    private Double averageLaneFlow;
    
    /** 信号灯状态列表 */
    private List<SignalState> signalStates;
    
    /** 自定义指标 */
    private Map<String, Object> custom;
    
    public StatisticsData() {
        this.timestamp = System.currentTimeMillis();
    }
}

