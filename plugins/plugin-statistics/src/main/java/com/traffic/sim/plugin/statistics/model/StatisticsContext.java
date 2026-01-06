package com.traffic.sim.plugin.statistics.model;

import lombok.Data;

import java.util.Map;

/**
 * 统计上下文
 * 包含地图信息、历史数据等
 * 
 * @author traffic-sim
 */
@Data
public class StatisticsContext {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 道路容量
     */
    private Double roadCapacity;
    
    /**
     * 统计缓冲区（用于累计计算）
     */
    private StatisticsBuffer buffer;
    
    /**
     * 地图信息
     */
    private Map<String, Object> mapInfo;
    
    /**
     * 自定义上下文数据
     */
    private Map<String, Object> custom;
    
    public StatisticsContext(String sessionId) {
        this.sessionId = sessionId;
        this.buffer = new StatisticsBuffer();
    }
}

