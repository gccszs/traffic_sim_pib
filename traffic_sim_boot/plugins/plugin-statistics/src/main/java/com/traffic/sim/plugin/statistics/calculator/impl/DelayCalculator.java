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
 * 延误统计计算器
 * 计算延误时间
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class DelayCalculator implements StatisticsCalculator {
    
    @Override
    public StatisticsResult calculate(SimulationStepData currentStep, 
                                     SimulationStepData previousStep,
                                     StatisticsContext context) {
        var vehicles = currentStep.getVehicles();
        
        if (vehicles.isEmpty()) {
            return StatisticsResult.empty();
        }
        
        List<Double> delays = new ArrayList<>();
        
        for (var vehicle : vehicles) {
            // 从车辆属性中提取延误时间
            Double delay = extractDelay(vehicle);
            if (delay != null && delay > 0) {
                delays.add(delay);
            }
        }
        
        StatisticsResult result = new StatisticsResult();
        
        if (!delays.isEmpty()) {
            double delayMin = delays.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double delayMax = delays.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double delayAve = delays.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            result.set("delay_min", delayMin);
            result.set("delay_max", delayMax);
            result.set("delay_ave", delayAve);
        } else {
            result.set("delay_min", 0.0);
            result.set("delay_max", 0.0);
            result.set("delay_ave", 0.0);
        }
        
        return result;
    }
    
    /**
     * 从车辆属性中提取延误时间
     */
    private Double extractDelay(SimulationStepData.Vehicle vehicle) {
        if (vehicle.getAttributes() != null) {
            Object delay = vehicle.getAttributes().get("delay");
            if (delay instanceof Number) {
                return ((Number) delay).doubleValue();
            }
        }
        return null;
    }
    
    @Override
    public String getName() {
        return "DelayCalculator";
    }
    
    @Override
    public List<String> getCalculatedFields() {
        return Arrays.asList("delay_min", "delay_max", "delay_ave");
    }
}

