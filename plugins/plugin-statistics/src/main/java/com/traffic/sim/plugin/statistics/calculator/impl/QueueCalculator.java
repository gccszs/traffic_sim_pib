package com.traffic.sim.plugin.statistics.calculator.impl;

import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculator;
import com.traffic.sim.plugin.statistics.model.SimulationStepData;
import com.traffic.sim.plugin.statistics.model.StatisticsContext;
import com.traffic.sim.plugin.statistics.model.StatisticsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 排队统计计算器
 * 计算排队长度和排队时间
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class QueueCalculator implements StatisticsCalculator {
    
    private static final double LOW_SPEED_THRESHOLD = 0.1; // m/s，低速视为排队
    
    @Override
    public StatisticsResult calculate(SimulationStepData currentStep, 
                                     SimulationStepData previousStep,
                                     StatisticsContext context) {
        var vehicles = currentStep.getVehicles();
        
        StatisticsResult result = new StatisticsResult();
        
        if (vehicles.isEmpty()) {
            log.debug("No vehicles for queue calculation");
            result.set("queue_length_min", 0.0);
            result.set("queue_length_max", 0.0);
            result.set("queue_length_ave", 0.0);
            result.set("queue_time_min", 0.0);
            result.set("queue_time_max", 0.0);
            result.set("queue_time_ave", 0.0);
            return result;
        }
        
        List<Double> queueLengths = new ArrayList<>();
        List<Double> queueTimes = new ArrayList<>();
        
        for (var vehicle : vehicles) {
            Double speed = vehicle.getSpeed();
            if (speed == null || speed < LOW_SPEED_THRESHOLD) {
                // 低速车辆视为排队
                // 排队长度：简化计算，使用车辆间距（实际需要根据道路信息计算）
                double queueLength = estimateQueueLength(vehicle);
                queueLengths.add(queueLength);
                
                // 排队时间：从车辆属性中获取（如果存在）
                Double queueTime = extractQueueTime(vehicle);
                if (queueTime != null) {
                    queueTimes.add(queueTime);
                }
            }
        }
        
        if (!queueLengths.isEmpty()) {
            double queueLengthMin = queueLengths.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double queueLengthMax = queueLengths.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double queueLengthAve = queueLengths.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            result.set("queue_length_min", queueLengthMin);
            result.set("queue_length_max", queueLengthMax);
            result.set("queue_length_ave", queueLengthAve);
            
            log.info("Queue length calculation: count={}, min={}, max={}, ave={}", 
                queueLengths.size(), queueLengthMin, queueLengthMax, queueLengthAve);
        } else {
            result.set("queue_length_min", 0.0);
            result.set("queue_length_max", 0.0);
            result.set("queue_length_ave", 0.0);
        }
        
        if (!queueTimes.isEmpty()) {
            double queueTimeMin = queueTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double queueTimeMax = queueTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double queueTimeAve = queueTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            result.set("queue_time_min", queueTimeMin);
            result.set("queue_time_max", queueTimeMax);
            result.set("queue_time_ave", queueTimeAve);
            
            log.info("Queue time calculation: count={}, min={}, max={}, ave={}", 
                queueTimes.size(), queueTimeMin, queueTimeMax, queueTimeAve);
        } else {
            result.set("queue_time_min", 0.0);
            result.set("queue_time_max", 0.0);
            result.set("queue_time_ave", 0.0);
        }
        
        return result;
    }
    
    /**
     * 估算排队长度（简化实现）
     */
    private double estimateQueueLength(SimulationStepData.Vehicle vehicle) {
        // 简化实现：假设每辆车占用5米
        // 实际需要根据道路信息和车辆位置计算
        return 5.0;
    }
    
    /**
     * 从车辆属性中提取排队时间
     */
    private Double extractQueueTime(SimulationStepData.Vehicle vehicle) {
        if (vehicle.getAttributes() != null) {
            Object queueTime = vehicle.getAttributes().get("queueTime");
            if (queueTime instanceof Number) {
                return ((Number) queueTime).doubleValue();
            }
        }
        return null;
    }
    
    @Override
    public String getName() {
        return "QueueCalculator";
    }
    
    @Override
    public List<String> getCalculatedFields() {
        return Arrays.asList("queue_length_min", "queue_length_max", "queue_length_ave",
                           "queue_time_min", "queue_time_max", "queue_time_ave");
    }
}

