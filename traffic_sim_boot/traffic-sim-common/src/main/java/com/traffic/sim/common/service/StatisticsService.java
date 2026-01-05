package com.traffic.sim.common.service;

import com.traffic.sim.common.model.StatisticsData;

import java.util.List;
import java.util.Map;

/**
 * 统计服务接口
 * 定义在common模块，由plugin-statistics模块实现
 * 
 * @author traffic-sim
 */
public interface StatisticsService {
    
    /**
     * 处理单个仿真步的统计数据
     * 
     * @param simData 仿真原始数据
     * @return 处理后的统计数据
     */
    StatisticsData processSimulationStep(Map<String, Object> simData);
    
    /**
     * 聚合多个仿真步的统计数据
     * 
     * @param stepStats 多个仿真步的统计数据
     * @return 聚合后的统计数据
     */
    StatisticsData aggregateStatistics(List<StatisticsData> stepStats);
}

