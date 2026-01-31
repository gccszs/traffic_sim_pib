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
 * 停车统计计算器
 * 计算停车次数
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class StopCalculator implements StatisticsCalculator {
    
    private static final double STOP_SPEED_THRESHOLD = 0.01; // m/s，速度低于此值视为停车
    
    @Override
    public StatisticsResult calculate(SimulationStepData currentStep, 
                                     SimulationStepData previousStep,
                                     StatisticsContext context) {
        var vehicles = currentStep.getVehicles();
        
        StatisticsResult result = new StatisticsResult();
        
        if (vehicles.isEmpty()) {
            log.debug("No vehicles for stop calculation");
            result.set("stop_min", 0);
            result.set("stop_max", 0);
            result.set("stop_ave", 0.0);
            return result;
        }
        
        List<Integer> stopCounts = new ArrayList<>();
        
        for (var vehicle : vehicles) {
            Double speed = vehicle.getSpeed();
            if (speed != null && speed < STOP_SPEED_THRESHOLD) {
                // 从车辆属性中获取停车次数
                Integer stopCount = extractStopCount(vehicle);
                if (stopCount != null) {
                    stopCounts.add(stopCount);
                } else {
                    // 如果没有停车次数信息，当前停车计为1次
                    stopCounts.add(1);
                }
            }
        }
        
        if (!stopCounts.isEmpty()) {
            int stopMin = stopCounts.stream().mapToInt(Integer::intValue).min().orElse(0);
            int stopMax = stopCounts.stream().mapToInt(Integer::intValue).max().orElse(0);
            double stopAve = stopCounts.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            
            result.set("stop_min", stopMin);
            result.set("stop_max", stopMax);
            result.set("stop_ave", stopAve);
            
            log.info("Stop calculation: count={}, min={}, max={}, ave={}", 
                stopCounts.size(), stopMin, stopMax, stopAve);
        } else {
            result.set("stop_min", 0);
            result.set("stop_max", 0);
            result.set("stop_ave", 0.0);
        }
        
        return result;
    }
    
    /**
     * 从车辆属性中提取停车次数
     */
    private Integer extractStopCount(SimulationStepData.Vehicle vehicle) {
        if (vehicle.getAttributes() != null) {
            Object stopCount = vehicle.getAttributes().get("stopCount");
            if (stopCount instanceof Number) {
                return ((Number) stopCount).intValue();
            }
        }
        return null;
    }
    
    @Override
    public String getName() {
        return "StopCalculator";
    }
    
    @Override
    public List<String> getCalculatedFields() {
        return Arrays.asList("stop_min", "stop_max", "stop_ave");
    }
}

