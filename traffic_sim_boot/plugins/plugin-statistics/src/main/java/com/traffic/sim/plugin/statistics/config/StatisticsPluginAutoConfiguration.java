package com.traffic.sim.plugin.statistics.config;

import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculator;
import com.traffic.sim.plugin.statistics.calculator.StatisticsCalculatorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Statistics Plugin Auto Configuration
 * 
 * @author traffic-sim
 */
@Slf4j
@AutoConfiguration
@EnableAsync
@EnableConfigurationProperties(StatisticsPluginProperties.class)
@ComponentScan(basePackages = "com.traffic.sim.plugin.statistics")
public class StatisticsPluginAutoConfiguration {
    
    /**
     * 注册统计计算器
     */
    @Bean
    @ConditionalOnMissingBean
    public StatisticsCalculatorRegistry calculatorRegistry(List<StatisticsCalculator> calculators) {
        StatisticsCalculatorRegistry registry = new StatisticsCalculatorRegistry();
        calculators.forEach(registry::register);
        log.info("Registered {} statistics calculators", calculators.size());
        return registry;
    }
    
    /**
     * 异步执行器配置
     */
    @Bean(name = "statisticsExecutor")
    @ConditionalOnMissingBean(name = "statisticsExecutor")
    public Executor statisticsExecutor(StatisticsPluginProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getAsync().getCorePoolSize());
        executor.setMaxPoolSize(properties.getAsync().getMaxPoolSize());
        executor.setQueueCapacity(properties.getAsync().getQueueCapacity());
        executor.setThreadNamePrefix("statistics-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Initialized statistics executor: core={}, max={}, queue={}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }
}

