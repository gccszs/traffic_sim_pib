package com.traffic.sim.plugin.statistics.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 仿真步数据
 * 
 * @author traffic-sim
 */
@Data
public class SimulationStepData {
    
    /**
     * 仿真步数
     */
    private Long step;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 车辆列表
     */
    private List<Vehicle> vehicles = new ArrayList<>();
    
    /**
     * 信号灯列表
     */
    private List<Signal> signals = new ArrayList<>();
    
    /**
     * 原始数据
     */
    private Map<String, Object> rawData;
    
    /**
     * 车辆数据模型
     */
    @Data
    public static class Vehicle {
        private Integer id;
        private Double speed;        // m/s
        private Double acceleration; // m/s²
        private Double x;
        private Double y;
        private Integer roadId;
        private Integer laneId;
        private String type;
        private Map<String, Object> attributes;
    }
    
    /**
     * 信号灯数据模型
     */
    @Data
    public static class Signal {
        private Integer crossId;
        private String state;       // RED, YELLOW, GREEN
        private Integer phase;
        private Long cycleTime;
        private Map<String, Object> attributes;
    }
}

