package com.traffic.sim.common.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 信号灯状态
 * 
 * @author traffic-sim
 */
@Data
public class SignalState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 路口ID */
    private Integer crossId;
    
    /** 信号灯状态 */
    private String state;
    
    /** 周期时间 */
    private Integer cycleTime;
    
    /** 当前阶段 */
    private Integer currentPhase;
}

