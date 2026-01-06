package com.traffic.sim.common.constant;

/**
 * WebSocket操作类型常量
 * 
 * @author traffic-sim
 */
public class WebSocketOperation {
    
    /** 初始化握手请求 */
    public static final String HELLO = "hello";
    
    /** 初始化握手响应 */
    public static final String HI = "hi";
    
    /** 启动仿真 */
    public static final String START = "start";
    
    /** 暂停仿真 */
    public static final String PAUSE = "pause";
    
    /** 停止仿真 */
    public static final String STOP = "stop";
    
    /** 错误消息 */
    public static final String ERR = "err";
    
    /** 引擎就绪通知 */
    public static final String ENG_OK = "eng_ok";
    
    /** 仿真数据 */
    public static final String SIM_DATA = "sim_data";
    
    /** 统计信息 */
    public static final String STATISTICS = "statistics";
    
    private WebSocketOperation() {
        // 工具类，禁止实例化
    }
}

