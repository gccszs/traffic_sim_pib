package com.traffic.sim.plugin.engine.manager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 配置线程池用于异步保存仿真数据
 * 
 * @author traffic-sim
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 仿真数据持久化线程池
     * 
     * @return 线程池执行器
     */
    @Bean(name = "simulationDataExecutor")
    public Executor simulationDataExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：根据 CPU 核心数设置
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        executor.setCorePoolSize(corePoolSize);
        
        // 最大线程数
        executor.setMaxPoolSize(corePoolSize * 2);
        
        // 队列容量：允许缓存一定数量的任务
        executor.setQueueCapacity(500);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("sim-data-");
        
        // 拒绝策略：由调用线程执行（避免数据丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("Initialized simulation data executor with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
            corePoolSize, corePoolSize * 2, 500);
        
        return executor;
    }
}
