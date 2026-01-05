package com.traffic.sim.plugin.statistics.calculator;

import com.traffic.sim.plugin.statistics.model.SimulationStepData;
import com.traffic.sim.plugin.statistics.model.StatisticsContext;
import com.traffic.sim.plugin.statistics.model.StatisticsResult;

import java.util.List;

/**
 * 统计计算器接口
 * 
 * @author traffic-sim
 */
public interface StatisticsCalculator {
    
    /**
     * 计算统计指标
     * 
     * @param currentStep 当前仿真步数据
     * @param previousStep 上一步仿真数据（用于增量计算，可能为null）
     * @param context 统计上下文
     * @return 统计结果
     */
    StatisticsResult calculate(SimulationStepData currentStep,
                               SimulationStepData previousStep,
                               StatisticsContext context);
    
    /**
     * 获取计算器名称
     */
    String getName();
    
    /**
     * 获取计算的统计字段列表
     */
    List<String> getCalculatedFields();
}

