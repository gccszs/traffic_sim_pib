package com.traffic.sim.plugin.statistics.util;

/**
 * 单位转换工具类
 * 
 * @author traffic-sim
 */
public class UnitConverter {
    
    /**
     * 米/秒 转 千米/小时
     */
    public static double mpsToKmh(double mps) {
        return mps * 3.6;
    }
    
    /**
     * 千米/小时 转 米/秒
     */
    public static double kmhToMps(double kmh) {
        return kmh / 3.6;
    }
    
    /**
     * 将流量转换为每小时流量
     * 假设每步时间为0.1秒，则每小时 = 36000步
     */
    public static double flowToPerHour(double flowPerStep) {
        return flowPerStep * 36000;
    }
    
    /**
     * 将每小时流量转换为每步流量
     */
    public static double perHourToFlow(double flowPerHour) {
        return flowPerHour / 36000;
    }
}

