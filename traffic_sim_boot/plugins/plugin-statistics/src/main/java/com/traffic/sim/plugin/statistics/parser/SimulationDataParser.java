package com.traffic.sim.plugin.statistics.parser;

import com.traffic.sim.plugin.statistics.model.SimulationStepData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 仿真数据解析器
 * 将原始Map数据解析为SimulationStepData
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class SimulationDataParser {
    
    /**
     * 解析仿真数据
     */
    public SimulationStepData parse(Map<String, Object> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return null;
        }
        
        try {
            SimulationStepData stepData = new SimulationStepData();
            stepData.setRawData(rawData);
            
            // 提取步数
            Object stepObj = rawData.get("step");
            if (stepObj instanceof Number) {
                stepData.setStep(((Number) stepObj).longValue());
            }
            
            // 提取时间戳
            Object timestampObj = rawData.get("timestamp");
            if (timestampObj instanceof Number) {
                stepData.setTimestamp(((Number) timestampObj).longValue());
            } else {
                stepData.setTimestamp(System.currentTimeMillis());
            }
            
            // 解析车辆数据
            List<SimulationStepData.Vehicle> vehicles = parseVehicles(rawData);
            stepData.setVehicles(vehicles);
            
            // 解析信号灯数据
            List<SimulationStepData.Signal> signals = parseSignals(rawData);
            stepData.setSignals(signals);
            
            return stepData;
        } catch (Exception e) {
            log.error("Error parsing simulation data", e);
            return null;
        }
    }
    
    /**
     * 解析车辆数据
     */
    @SuppressWarnings("unchecked")
    private List<SimulationStepData.Vehicle> parseVehicles(Map<String, Object> rawData) {
        List<SimulationStepData.Vehicle> vehicles = new ArrayList<>();
        
        Object vehiclesObj = rawData.get("vehicles");
        if (vehiclesObj instanceof List) {
            List<Map<String, Object>> vehicleList = (List<Map<String, Object>>) vehiclesObj;
            for (Map<String, Object> vehicleMap : vehicleList) {
                SimulationStepData.Vehicle vehicle = new SimulationStepData.Vehicle();
                
                vehicle.setId(getInteger(vehicleMap, "id"));
                vehicle.setSpeed(getDouble(vehicleMap, "speed"));
                vehicle.setAcceleration(getDouble(vehicleMap, "acceleration"));
                vehicle.setX(getDouble(vehicleMap, "x"));
                vehicle.setY(getDouble(vehicleMap, "y"));
                vehicle.setRoadId(getInteger(vehicleMap, "roadId"));
                vehicle.setLaneId(getInteger(vehicleMap, "laneId"));
                vehicle.setType(getString(vehicleMap, "type"));
                
                // 保留原始属性
                vehicle.setAttributes(vehicleMap);
                
                vehicles.add(vehicle);
            }
        }
        
        return vehicles;
    }
    
    /**
     * 解析信号灯数据
     */
    @SuppressWarnings("unchecked")
    private List<SimulationStepData.Signal> parseSignals(Map<String, Object> rawData) {
        List<SimulationStepData.Signal> signals = new ArrayList<>();
        
        Object signalsObj = rawData.get("signals");
        if (signalsObj instanceof List) {
            List<Map<String, Object>> signalList = (List<Map<String, Object>>) signalsObj;
            for (Map<String, Object> signalMap : signalList) {
                SimulationStepData.Signal signal = new SimulationStepData.Signal();
                
                signal.setCrossId(getInteger(signalMap, "crossId"));
                signal.setState(getString(signalMap, "state"));
                signal.setPhase(getInteger(signalMap, "phase"));
                signal.setCycleTime(getLong(signalMap, "cycleTime"));
                
                // 保留原始属性
                signal.setAttributes(signalMap);
                
                signals.add(signal);
            }
        }
        
        return signals;
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
     * 从Map中提取Long值
     */
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
    
    /**
     * 从Map中提取String值
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}

