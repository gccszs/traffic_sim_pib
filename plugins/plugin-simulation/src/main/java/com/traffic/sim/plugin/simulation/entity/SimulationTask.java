package com.traffic.sim.plugin.simulation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

/**
 * 仿真任务实体
 * 
 * @author traffic-sim
 */
@Entity
@Table(name = "simulation_task")
@Data
public class SimulationTask {
    
    @Id
    @Column(name = "task_id", length = 64)
    private String taskId;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "map_xml_name", length = 255)
    private String mapXmlName;  // 引擎内部使用的随机UUID
    
    @Column(name = "map_id", length = 64)
    private String mapId;  // 用户地图的ID
    
    @Column(name = "map_name", length = 255)
    private String mapName;  // 用户地图的名称（用于前端显示）
    
    @Column(name = "map_xml_path", length = 500)
    private String mapXmlPath;
    
    @Column(name = "sim_config", columnDefinition = "LONGTEXT")
    private String simConfig;
    
    @Column(name = "status", length = 20, nullable = false)
    private String status; // CREATED/RUNNING/PAUSED/STOPPED/FINISHED
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "create_time", nullable = false, updatable = false)
    @CreationTimestamp
    private Date createTime;
    
    @Column(name = "update_time", nullable = false)
    @UpdateTimestamp
    private Date updateTime;
}

