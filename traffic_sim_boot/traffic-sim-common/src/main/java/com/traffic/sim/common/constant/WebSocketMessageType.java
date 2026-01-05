package com.traffic.sim.common.constant;

/**
 * WebSocket消息类型常量
 * 
 * @author traffic-sim
 */
public class WebSocketMessageType {
    
    /** 发送给前端 */
    public static final String FRONTEND = "frontend";
    
    /** 发送给引擎 */
    public static final String ENG = "eng";
    
    /** 发送给后端 */
    public static final String BACKEND = "backend";
    
    private WebSocketMessageType() {
        // 工具类，禁止实例化
    }
}

