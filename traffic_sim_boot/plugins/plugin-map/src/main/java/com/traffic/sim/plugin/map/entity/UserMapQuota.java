package com.traffic.sim.plugin.map.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 用户地图配额实体
 * 
 * @author traffic-sim
 */
@Entity
@Table(name = "user_map_quota")
@Data
public class UserMapQuota {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;               // 用户ID
    
    @Column(name = "max_maps", nullable = false)
    private Integer maxMaps = 50;      // 最大地图数量（默认50）
    
    @Column(name = "current_maps", nullable = false)
    private Integer currentMaps = 0;    // 当前地图数量
    
    @Column(name = "total_size")
    private Long totalSize = 0L;       // 总文件大小（字节）
    
    @Column(name = "max_size")
    private Long maxSize = 1073741824L; // 最大存储空间（默认1GB）
    
    @Column(name = "create_time", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
    
    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}

