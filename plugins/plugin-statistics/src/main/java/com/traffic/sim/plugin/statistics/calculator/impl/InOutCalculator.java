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
        
        // 计算拥堵指数
        double roadCapacity = context.getRoadCapacity() != null ? 
            context.getRoadCapacity() : 1000.0; // 默认容量
        double jamIndex = roadCapacity > 0 ? 
            (carNumber * 100.0 / roadCapacity) : 0.0;
        
        StatisticsResult result = new StatisticsResult();
        result.set("car_number", carNumber);
        result.set("car_in", carIn);
        result.set("car_out", carOut);
        result.set("jam_index", jamIndex);
        
        // 计算累计流量（转换为小时流量）
        var buffer = context.getBuffer();
        buffer.addOutFlow(carOut);
        buffer.addInFlow(carIn);
        
        double carsInPerHour = UnitConverter.flowToPerHour(buffer.getAverageInFlow());
        double carsOutPerHour = UnitConverter.flowToPerHour(buffer.getAverageOutFlow());
        
        result.set("global_cars_in", carsInPerHour);
        result.set("global_cars_out", carsOutPerHour);
        
        return result;
    }
    
    @Override
    public String getName() {
        return "InOutCalculator";
    }
    
    @Override
    public List<String> getCalculatedFields() {
        return Arrays.asList("car_number", "car_in", "car_out", 
                           "jam_index", "global_cars_in", "global_cars_out");
    }
}

