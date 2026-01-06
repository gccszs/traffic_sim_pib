package com.traffic.sim.common.constant;

/**
 * 用户状态常量
 * 
 * @author traffic-sim
 */
public class UserStatus {
    
    /** 正常状态 */
    public static final String NORMAL = "NORMAL";
    
    /** 已禁用 */
    public static final String BANNED = "BANNED";
    
    /** 已锁定 */
    public static final String BLOCKED = "BLOCKED";
    
    private UserStatus() {
        // 工具类，禁止实例化
    }
}

