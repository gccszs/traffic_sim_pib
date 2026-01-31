package com.traffic.sim.plugin.statistics.calculator.impl;

import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculator;
import com.traffic.sim.plugin.statistics.model.SimulationStepData;
import com.traffic.sim.plugin.statistics.model.StatisticsContext;
import com.traffic.sim.plugin.statistics.model.StatisticsResult;
import com.traffic.sim.plugin.statistics.util.UnitConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 车辆进出统计计算器
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class InOutCalculator implements StatisticsCalculator {
    
    @Override
    public StatisticsResult calculate(SimulationStepData currentStep, 
                                     SimulationStepData previousStep,
                                     StatisticsContext context) {
        var currentVehicles = currentStep.getVehicles();
        List<SimulationStepData.Vehicle> previousVehicles = previousStep != null ? 
            previousStep.getVehicles() : Collections.emptyList();
        
        // 计算车辆ID集合
        Set<Integer> currentIds = currentVehicles.stream()
            .map(SimulationStepData.Vehicle::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Set<Integer> previousIds = previousVehicles.stream()
            .map(SimulationStepData.Vehicle::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        // 进入车辆 = 当前有但上一步没有
        int carIn = (int) currentIds.stream()
            .filter(id -> !previousIds.contains(id))
            .count();
        
        // 离开车辆 = 上一步有但当前没有
        int carOut = (int) previousIds.stream()
            .filter(id -> !currentIds.contains(id))
            .count();
        
        // 当前车辆总数
        int carNumber = currentVehicles.size();
        
        // 计算拥堵指数（基于速度和密度的综合指标）
        double jamIndex = calculateCongestionIndex(currentVehicles, context);
        
        StatisticsResult result = new StatisticsResult();
        result.set("car_number", carNumber);
        result.set("car_in", carIn);
        result.set("car_out", carOut);
        result.set("jam_index", jamIndex);
        
        // 计算累计流量（累积总数，不转换为小时流量）
        var buffer = context.getBuffer();
        buffer.addOutFlow(carOut);
        buffer.addInFlow(carIn);
        
        // 获取累积总数
        int totalCarsIn = buffer.getTotalInFlow();
        int totalCarsOut = buffer.getTotalOutFlow();
        
        result.set("cars_in", totalCarsIn);  // 累积进入车辆总数
        result.set("cars_out", totalCarsOut);  // 累积离开车辆总数
        
        return result;
    }
    
    /**
     * 计算拥堵指数（基于速度的简化算法）
     * 返回值范围：0-1（前端显示时会乘以100）
     * 0 = 完全畅通，1 = 严重拥堵
     */
    private double calculateCongestionIndex(List<SimulationStepData.Vehicle> vehicles, 
                                           StatisticsContext context) {
        if (vehicles.isEmpty()) {
            return 0.0;
        }
        
        // 计算平均速度（m/s）
        double totalSpeed = 0.0;
        int validSpeedCount = 0;
        
        for (var vehicle : vehicles) {
            Double speed = vehicle.getSpeed();
            if (speed != null && speed >= 0) {
                totalSpeed += speed;
                validSpeedCount++;
            }
        }
        
        if (validSpeedCount == 0) {
            return 0.0;
        }
        
        double averageSpeed = totalSpeed / validSpeedCount;
        
        // 基于速度的拥堵指数
        // 自由流速度为14 m/s（约50 km/h，城市道路典型值）
        double freeFlowSpeed = 14.0; // m/s
        
        // 拥堵指数 = 1 - (实际速度 / 自由流速度)
        // 当速度 = 自由流速度时，拥堵指数 = 0（畅通）
        // 当速度 = 0时，拥堵指数 = 1（完全拥堵）
        double jamIndex = 1.0 - (averageSpeed / freeFlowSpeed);
        
        // 限制在0-1之间
        jamIndex = Math.min(1.0, Math.max(0.0, jamIndex));
        
        log.debug("Congestion calculation: carNumber={}, avgSpeed={} m/s ({} km/h), freeFlowSpeed={} m/s, jamIndex={}", 
            vehicles.size(), averageSpeed, averageSpeed * 3.6, freeFlowSpeed, jamIndex);
        
        return jamIndex;
    }
    
    @Override
    public String getName() {
        return "InOutCalculator";
    }
    
    @Override
    public List<String> getCalculatedFields() {
        return Arrays.asList("car_number", "car_in", "car_out", 
                           "jam_index", "cars_in", "cars_out");
    }
}

