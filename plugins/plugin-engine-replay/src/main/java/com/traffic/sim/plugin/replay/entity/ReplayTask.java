package com.traffic.sim.plugin.replay.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

/**
 * 回放任务实体
 * 
 * @author traffic-sim
 */
@Entity
@Table(name = "replay_task")
@Data
public class ReplayTask {
    
    @Id
    @Column(name = "task_id", length = 64)
    private String taskId;
    
    /**
     * 关联的仿真任务ID
     */
    @Column(name = "simulation_task_id", length = 64, nullable = false)
    private String simulationTaskId;
    
    /**
     * 回放任务名称
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    /**
     * 状态：CREATED/PLAYING/PAUSED/STOPPED/FINISHED
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;
    
    /**
     * 当前步数
     */
    @Column(name = "current_step")
    private Long currentStep = 0L;
    
    /**
     * 总步数
     */
    @Column(name = "total_steps")
    private Long totalSteps = 0L;
    
    /**
     * 播放速度（倍速）
     */
    @Column(name = "playback_speed")
    private Double playbackSpeed = 1.0;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "create_time", nullable = false, updatable = false)
    @CreationTimestamp
    private Date createTime;
    
    @Column(name = "update_time", nullable = false)
    @UpdateTimestamp
    private Date updateTime;
    
    /**
     * 回放任务状态枚举
     */
    public enum ReplayStatus {
        CREATED("CREATED", "已创建"),
        PLAYING("PLAYING", "播放中"),
        PAUSED("PAUSED", "已暂停"),
        STOPPED("STOPPED", "已停止"),
        FINISHED("FINISHED", "已完成");
        
        private final String code;
        private final String desc;
        
        ReplayStatus(String code, String desc) {
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

