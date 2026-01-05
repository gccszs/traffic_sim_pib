package com.traffic.sim.plugin.replay.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 回放控制请求
 * 
 * @author traffic-sim
 */
@Data
public class ReplayControlRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 控制操作：PLAY, PAUSE, STOP, SEEK, SET_SPEED
     */
    @NotNull(message = "控制操作不能为空")
    private ReplayControlAction action;
    
    /**
     * 目标步数（用于SEEK操作）
     */
    private Long targetStep;
    
    /**
     * 播放速度（用于SET_SPEED操作）
     */
    private Double speed;
    
    /**
     * 回放控制操作枚举
     */
    public enum ReplayControlAction {
        PLAY("PLAY", "播放"),
        PAUSE("PAUSE", "暂停"),
        STOP("STOP", "停止"),
        SEEK("SEEK", "跳转"),
        SET_SPEED("SET_SPEED", "设置速度");
        
        private final String code;
        private final String desc;
        
        ReplayControlAction(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDesc() {
            return desc;
        }
    }
}

