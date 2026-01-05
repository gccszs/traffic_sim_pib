package com.traffic.sim.plugin.statistics.service;

import com.traffic.sim.common.model.StatisticsData;
import com.traffic.sim.common.service.StatisticsService;
import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculator;
import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculatorRegistry;
import com.traffic.sim.plugin.statistics.model.SimulationStepData;
import com.traffic.sim.plugin.statistics.model.StatisticsContext;
import com.traffic.sim.plugin.statistics.model.StatisticsResult;
import com.traffic.sim.plugin.statistics.parser.SimulationDataParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统计服务实现
 * 
 * @author traffic-sim
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    
    private final StatisticsCalculatorRegistry calculatorRegistry;
    private final SimulationDataParser dataParser;
    private final StatisticsContextFactory contextFactory;
    
    // 缓存上一步数据（按会话ID）
    private final Map<String, SimulationStepData> stepCache = new ConcurrentHashMap<>();
    
    @Override
    public StatisticsData processSimulationStep(Map<String, Object> simData) {
        try {
            // 1. 解析仿真数据
            SimulationStepData currentStep = dataParser.parse(simData);
            if (currentStep == null) {
                log.warn("Failed to parse simulation data");
                return createEmptyStatisticsData();
            }
            
            String sessionId = extractSessionId(simData);
            
            // 2. 获取上一步数据
            SimulationStepData previousStep = stepCache.get(sessionId);
            
            // 3. 创建统计上下文
            StatisticsContext context = contextFactory.create(sessionId);
            
            // 4. 执行所有计算器
            StatisticsResult result = new StatisticsResult();
            for (StatisticsCalculator calculator : calculatorRegistry.getAll()) {
                try {
                    StatisticsResult calcResult = calculator.calculate(
                        currentStep, previousStep, context);
                    if (calcResult != null && !calcResult.isEmpty()) {
                        result.merge(calcResult);
                    }
                } catch (Exception e) {
                    log.error("Error in calculator: {}", calculator.getName(), e);
                }
            }
            
            // 5. 构建统计数据结构
            StatisticsData statisticsData = buildStatisticsData(
                currentStep.getStep(), result);
            
            // 6. 更新缓存
            stepCache.put(sessionId, currentStep);
            
            return statisticsData;
        } catch (Exception e) {
            log.error("Error processing simulation step", e);
            return createEmptyStatisticsData();
        }
    }
    
    @Override
    public StatisticsData aggregateStatistics(List<StatisticsData> stepStats) {
        if (stepStats == null || stepStats.isEmpty()) {
            return createEmptyStatisticsData();
        }
        
        StatisticsData aggregated = new StatisticsData();
        
        // 聚合基础统计
        int totalVehicles = 0;
        double totalSpeed = 0.0;
        double totalCongestion = 0.0;
        int count = 0;
        
        for (StatisticsData stats : stepStats) {
            if (stats == null) {
                continue;
            }
            
            if (stats.getVehicleCount() != null) {
                totalVehicles += stats.getVehicleCount();
            }
            if (stats.getAverageSpeed() != null) {
                totalSpeed += stats.getAverageSpeed();
                count++;
            }
            if (stats.getCongestionIndex() != null) {
                totalCongestion += stats.getCongestionIndex();
            }
        }
        
        aggregated.setVehicleCount(totalVehicles / stepStats.size());
        aggregated.setAverageSpeed(count > 0 ? totalSpeed / count : 0.0);
        aggregated.setCongestionIndex(totalCongestion / stepStats.size());
        
        return aggregated;
    }
    
    /**
     * 构建统计数据
     */
    private StatisticsData buildStatisticsData(Long step, StatisticsResult result) {
        StatisticsData data = new StatisticsData();
        data.setStep(step);
        data.setTimestamp(System.currentTimeMillis());
        
        // 从结果中提取数据
        Map<String, Object> resultData = result.getData();
        if (resultData != null) {
            data.setVehicleCount(getInteger(resultData, "car_number"));
            data.setAverageSpeed(getDouble(resultData, "speed_ave"));
            data.setCongestionIndex(getDouble(resultData, "jam_index"));
            
            // 设置自定义字段
            data.setCustom(resultData);
        }
        
        return data;
    }
    
    /**
     * 创建空统计数据
     */
    private StatisticsData createEmptyStatisticsData() {
        StatisticsData data = new StatisticsData();
        data.setStep(0L);
        data.setTimestamp(System.currentTimeMillis());
        data.setVehicleCount(0);
        data.setAverageSpeed(0.0);
        data.setCongestionIndex(0.0);
        return data;
    }
    
    /**
     * 从Map中提取Integer值
     */
    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    /**
     * 从Map中提取Double值
     */
    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
    
    /**
     * 从仿真数据中提取会话ID
     */
    private String extractSessionId(Map<String, Object> simData) {
        // 尝试从数据中提取sessionId
        Object sessionId = simData.get("sessionId");
        if (sessionId != null) {
            return sessionId.toString();
        }
        // 如果没有，使用默认值
        return "default";
    }
}

