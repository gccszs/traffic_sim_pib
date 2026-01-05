package com.traffic.sim.plugin.statistics.calculator.impl;

import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculator;
import com.traffic.sim.plugin.statistics.model.SimulationStepData;
import com.traffic.sim.plugin.statistics.model.StatisticsContext;
import com.traffic.sim.plugin.statistics.model.StatisticsResult;
import com.traffic.sim.plugin.statistics.util.UnitConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 流量统计计算器
 * 计算路段和路口流量
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class FlowCalculator implements StatisticsCalculator {
    
    @Override
    public StatisticsResult calculate(SimulationStepData currentStep, 
                                     SimulationStepData previousStep,
                                     StatisticsContext context) {
        var vehicles = currentStep.getVehicles();
        
        if (vehicles.isEmpty()) {
            return StatisticsResult.empty();
        }
        
        // 计算路段流量（按roadId分组）
        Map<Integer, Integer> roadFlow = new HashMap<>();
        Map<String, Integer> laneFlow = new HashMap<>(); // key: "roadId_laneId"
        Map<Integer, Integer> crossFlow = new HashMap<>();
        
        for (var vehicle : vehicles) {
            Integer roadId = vehicle.getRoadId();
            Integer laneId = vehicle.getLaneId();
            Integer crossId = extractCrossId(vehicle);
            
            // 路段流量
            if (roadId != null) {
                roadFlow.put(roadId, roadFlow.getOrDefault(roadId, 0) + 1);
            }
            
            // 车道流量
            if (roadId != null && laneId != null) {
                String laneKey = roadId + "_" + laneId;
                laneFlow.put(laneKey, laneFlow.getOrDefault(laneKey, 0) + 1);
            }
            
            // 路口流量（简化实现，实际需要根据车辆位置判断路口）
            if (crossId != null) {
                crossFlow.put(crossId, crossFlow.getOrDefault(crossId, 0) + 1);
            }
        }
        
        // 计算平均流量
        double flowRdAve = roadFlow.isEmpty() ? 0.0 : 
            roadFlow.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double flowLaAve = laneFlow.isEmpty() ? 0.0 : 
            laneFlow.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double flowCrossAve = crossFlow.isEmpty() ? 0.0 : 
            crossFlow.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
        
        // 转换为每小时流量
        flowRdAve = UnitConverter.flowToPerHour(flowRdAve);
        flowLaAve = UnitConverter.flowToPerHour(flowLaAve);
        flowCrossAve = UnitConverter.flowToPerHour(flowCrossAve);
        
        StatisticsResult result = new StatisticsResult();
        result.set("flow_rd_ave", flowRdAve);
        result.set("flow_la_ave", flowLaAve);
        result.set("flow_cross_ave", flowCrossAve);
        
        // 存储详细流量数据到custom字段
        Map<String, Object> flowDetails = new HashMap<>();
        flowDetails.put("roadFlow", roadFlow);
        flowDetails.put("laneFlow", laneFlow);
        flowDetails.put("crossFlow", crossFlow);
        result.set("flow_details", flowDetails);
        
        return result;
    }
    
    /**
     * 从车辆数据中提取路口ID（简化实现）
     */
    private Integer extractCrossId(SimulationStepData.Vehicle vehicle) {
        // 实际实现需要根据车辆位置和地图信息判断路口
        // 这里简化处理，可以从车辆属性中获取
        if (vehicle.getAttributes() != null) {
            Object crossId = vehicle.getAttributes().get("crossId");
            if (crossId instanceof Number) {
                return ((Number) crossId).intValue();
            }
        }
        return null;
    }
    
    @Override
    public String getName() {
        return "FlowCalculator";
    }
    
    @Override
    public List<String> getCalculatedFields() {
        return Arrays.asList("flow_rd_ave", "flow_la_ave", "flow_cross_ave", "flow_details");
    }
}

