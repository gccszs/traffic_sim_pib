package com.traffic.sim.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * WebSocket消息模型
 * 
 * @author traffic-sim
 */
@Data
public class WebSocketInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 消息类型：frontend/eng/backend */
    private String type;
    
    /** 操作类型：hello/hi/start/pause/stop/sim_data/statistics等 */
    private String ope;
    
    /** 时间戳 */
    private Long time;
    
    /** 消息数据 */
    private Map<String, Object> data;
    
    public WebSocketInfo() {
        this.time = System.currentTimeMillis();
    }
    
    public WebSocketInfo(String type, String ope) {
        this.type = type;
        this.ope = ope;
        this.time = System.currentTimeMillis();
    }
    
    public WebSocketInfo(String type, String ope, Long time) {
        this.type = type;
        this.ope = ope;
        this.time = time != null ? time : System.currentTimeMillis();
    }
}

