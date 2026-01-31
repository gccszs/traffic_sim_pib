package com.traffic.sim.plugin.statistics.config;

import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculator;
import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculatorRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 统计模块配置
 * 自动注册所有统计计算器
 * 
 * @author traffic-sim
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StatisticsConfig {
    
    private final StatisticsCalculatorRegistry registry;
    private final List<StatisticsCalculator> calculators;
    
    @PostConstruct
    public void init() {
        log.info("Initializing statistics module, found {} calculators", calculators.size());
        
        for (StatisticsCalculator calculator : calculators) {
            registry.register(calculator);
            log.info("Registered calculator: {} (fields: {})", 
                calculator.getName(), calculator.getCalculatedFields());
        }
        
        log.info("Statistics module initialized successfully with {} calculators", 
            registry.getAll().size());
    }
}
