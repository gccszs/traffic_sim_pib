package com.traffic.sim.plugin.statistics.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 统计计算结果
 * 
 * @author traffic-sim
 */
@Data
public class StatisticsResult {
    
    private Map<String, Object> data = new HashMap<>();
    
    /**
     * 设置统计值
     */
    public void set(String key, Object value) {
        data.put(key, value);
    }
    
    /**
     * 获取统计值
     */
    public Object get(String key) {
        return data.get(key);
    }
    
    /**
     * 合并另一个统计结果
     */
    public void merge(StatisticsResult other) {
        if (other != null && other.data != null) {
            this.data.putAll(other.data);
        }
    }
    
    /**
     * 创建空结果
     */
    public static StatisticsResult empty() {
        return new StatisticsResult();
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }
}

