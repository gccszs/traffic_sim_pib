package com.traffic.sim.plugin.statistics.calculator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统计计算器注册表
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class StatisticsCalculatorRegistry {
    
    private final Map<String, StatisticsCalculator> calculators = new ConcurrentHashMap<>();
    
    /**
     * 注册计算器
     */
    public void register(StatisticsCalculator calculator) {
        if (calculator != null) {
            calculators.put(calculator.getName(), calculator);
            log.debug("Registered statistics calculator: {}", calculator.getName());
        }
    }
    
    /**
     * 获取所有计算器
     */
    public List<StatisticsCalculator> getAll() {
        return new ArrayList<>(calculators.values());
    }
    
    /**
     * 根据名称获取计算器
     */
    public StatisticsCalculator get(String name) {
        return calculators.get(name);
    }
    
    /**
     * 检查计算器是否存在
     */
    public boolean contains(String name) {
        return calculators.containsKey(name);
    }
}

