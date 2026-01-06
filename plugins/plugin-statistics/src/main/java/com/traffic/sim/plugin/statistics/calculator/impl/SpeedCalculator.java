package com.traffic.sim.plugin.statistics.calculator.impl;

import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculator;
import com.traffic.sim.plugin.statistics.model.SimulationStepData;
import com.traffic.sim.plugin.statistics.model.StatisticsContext;
import com.traffic.sim.plugin.statistics.model.StatisticsResult;
import com.traffic.sim.plugin.statistics.util.UnitConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 速度统计计算器
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class SpeedCalculator implements StatisticsCalculator {
    
    private static final double LOW_SPEED_THRESHOLD = 0.1; // m/s
    
    @Override
    public StatisticsResult calculate(SimulationStepData currentStep, 
                                     SimulationStepData previousStep,
                                     StatisticsContext context) {
        var vehicles = currentStep.getVehicles();
        
        if (vehicles.isEmpty()) {
            return StatisticsResult.empty();
        }
        
        double speedSum = 0.0;
        double speedMin = Double.MAX_VALUE;
        double speedMax = Double.MIN_VALUE;
        int lowSpeedCount = 0;
        
        for (var vehicle : vehicles) {
            Double speed = vehicle.getSpeed();
            if (speed == null) {
                continue;
            }
            
            speedSum += speed;
            speedMin = Math.min(speedMin, speed);
            speedMax = Math.max(speedMax, speed);
            
            if (speed < LOW_SPEED_THRESHOLD) {
                lowSpeedCount++;
            }
        }
        
        double speedAve = speedSum / vehicles.size();
        
        // 转换为 km/h
        StatisticsResult result = new StatisticsResult();
        result.set("speed_min", UnitConverter.mpsToKmh(speedMin == Double.MAX_VALUE ? 0 : speedMin));
        result.set("speed_max", UnitConverter.mpsToKmh(speedMax == Double.MIN_VALUE ? 0 : speedMax));
        result.set("speed_ave", UnitConverter.mpsToKmh(speedAve));
        result.set("low_speed", lowSpeedCount);
        
        return result;
    }
    
    @Override
    public String getName() {
        return "SpeedCalculator";
    }
    
    @Override
    public List<String> getCalculatedFields() {
        return Arrays.asList("speed_min", "speed_max", "speed_ave", "low_speed");
    }
}

