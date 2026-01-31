package com.traffic.sim.plugin.statistics.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计缓冲区
 * 用于累计计算和滑动窗口统计
 * 
 * @author traffic-sim
 */
@Data
public class StatisticsBuffer {
    
    /**
     * 进入流量历史
     */
    private List<Integer> inFlowHistory = new ArrayList<>();
    
    /**
     * 离开流量历史
     */
    private List<Integer> outFlowHistory = new ArrayList<>();
    
    /**
     * 累积进入车辆总数
     */
    private int totalInFlow = 0;
    
    /**
     * 累积离开车辆总数
     */
    private int totalOutFlow = 0;
    
    /**
     * 窗口大小（默认100步）
     */
    private int windowSize = 100;
    
    /**
     * 添加进入流量
     */
    public void addInFlow(int flow) {
        inFlowHistory.add(flow);
        totalInFlow += flow;  // 累加总数
        if (inFlowHistory.size() > windowSize) {
            inFlowHistory.remove(0);
        }
    }
    
    /**
     * 添加离开流量
     */
    public void addOutFlow(int flow) {
        outFlowHistory.add(flow);
        totalOutFlow += flow;  // 累加总数
        if (outFlowHistory.size() > windowSize) {
            outFlowHistory.remove(0);
        }
    }
    
    /**
     * 获取累积进入车辆总数
     */
    public int getTotalInFlow() {
        return totalInFlow;
    }
    
    /**
     * 获取累积离开车辆总数
     */
    public int getTotalOutFlow() {
        return totalOutFlow;
    }
    
    /**
     * 获取平均进入流量
     */
    public double getAverageInFlow() {
        if (inFlowHistory.isEmpty()) {
            return 0.0;
        }
        return inFlowHistory.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
    }
    
    /**
     * 获取平均离开流量
     */
    public double getAverageOutFlow() {
        if (outFlowHistory.isEmpty()) {
            return 0.0;
        }
        return outFlowHistory.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
    }
    
    /**
     * 清空缓冲区
     */
    public void clear() {
        inFlowHistory.clear();
        outFlowHistory.clear();
    }
}

