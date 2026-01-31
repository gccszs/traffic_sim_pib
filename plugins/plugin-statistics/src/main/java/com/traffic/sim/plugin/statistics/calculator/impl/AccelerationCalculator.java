package com.traffic.sim.plugin.statistics.calculator.impl;

import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculator;
import com.traffic.sim.plugin.statistics.model.SimulationStepData;
import com.traffic.sim.plugin.statistics.model.StatisticsContext;
import com.traffic.sim.plugin.statistics.model.StatisticsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 加速度统计计算器
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class AccelerationCalculator implements StatisticsCalculator {
    
    @Override
    public StatisticsResult calculate(SimulationStepData currentStep, 
                                     SimulationStepData previousStep,
                                     StatisticsContext context) {
        var vehicles = currentStep.getVehicles();
        
        StatisticsResult result = new StatisticsResult();
        
        if (vehicles.isEmpty()) {
            result.set("acc_min", 0.0);
            result.set("acc_max", 0.0);
            result.set("acc_ave", 0.0);
            return result;
        }
        
        double accSum = 0.0;
        double accMin = Double.MAX_VALUE;
        double accMax = Double.MIN_VALUE;
        int validCount = 0;
        
        for (var vehicle : vehicles) {
            Double acc = vehicle.getAcceleration();
            if (acc == null) {
                continue;
            }
            
            accSum += acc;
            accMin = Math.min(accMin, acc);
            accMax = Math.max(accMax, acc);
            validCount++;
        }
        
        if (validCount == 0) {
            result.set("acc_min", 0.0);
            result.set("acc_max", 0.0);
            result.set("acc_ave", 0.0);
            return result;
        }
        
        double accAve = accSum / validCount;
        
        result.set("acc_min", accMin);
        result.set("acc_max", accMax);
        result.set("acc_ave", accAve);
        
        return result;
    }
    
    @Override
    public String getName() {
        return "AccelerationCalculator";
    }
    
    @Override
    public List<String> getCalculatedFields() {
        return Arrays.asList("acc_min", "acc_max", "acc_ave");
    }
}

