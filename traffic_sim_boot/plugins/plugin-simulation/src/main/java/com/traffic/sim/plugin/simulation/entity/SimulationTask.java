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
    private String mapXmlName;
    
    @Column(name = "map_xml_path", length = 500)
    private String mapXmlPath;
    
    @Column(name = "sim_config", columnDefinition = "TEXT")
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

