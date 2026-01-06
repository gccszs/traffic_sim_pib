package com.traffic.sim.plugin.statistics.service;

import com.traffic.sim.plugin.statistics.model.StatisticsContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 统计上下文工厂
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class StatisticsContextFactory {
    
    // 缓存上下文（按会话ID）
    private final ConcurrentMap<String, StatisticsContext> contextCache = new ConcurrentHashMap<>();
    
    /**
     * 创建或获取统计上下文
     */
    public StatisticsContext create(String sessionId) {
        return contextCache.computeIfAbsent(sessionId, id -> {
            StatisticsContext context = new StatisticsContext(id);
            // 设置默认道路容量
            context.setRoadCapacity(1000.0);
            return context;
        });
    }
    
    /**
     * 移除上下文
     */
    public void remove(String sessionId) {
        contextCache.remove(sessionId);
    }
    
    /**
     * 清空所有上下文
     */
    public void clear() {
        contextCache.clear();
    }
}

