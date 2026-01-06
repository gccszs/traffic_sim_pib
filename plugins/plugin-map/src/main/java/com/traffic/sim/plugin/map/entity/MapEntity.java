package com.traffic.sim.plugin.map.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 地图实体
 * 
 * @author traffic-sim
 */
@Entity
@Table(name = "map")
@Data
public class MapEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "map_id")
    private String mapId;              // MongoDB地图ID
    
    @Column(nullable = false)
    private String name;                // 地图名称
    
    @Column(length = 500)
    private String description;         // 地图描述
    
    @Column(name = "file_path", nullable = false)
    private String filePath;            // 文件路径
    
    @Column(name = "file_name")
    private String fileName;            // 原始文件名
    
    @Column(name = "xml_file_name")
    private String xmlFileName;         // XML文件名
    
    @Lob
    @Column(name = "map_image", columnDefinition = "LONGTEXT")
    private String mapImage;            // 地图图片（Base64）
    
    // 用户隔离相关
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;               // 所有者用户ID
    
    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private MapStatus status;           // 地图状态
    
    // 文件大小和存储信息
    @Column(name = "file_size")
    private Long fileSize;              // 文件大小（字节）
    
    @Column(name = "storage_path")
    private String storagePath;         // 存储路径
    
    @Column(name = "create_time", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
    
    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;
    
    /**
     * 地图状态枚举
     */
    public enum MapStatus {
        PUBLIC(0, "公开"),      // 公开地图，所有用户可见
        PRIVATE(1, "私有"),     // 私有地图，仅所有者可见
        FORBIDDEN(2, "禁用");   // 禁用地图，管理员操作
        
        private final int code;
        private final String desc;
        
        MapStatus(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getDesc() {
            return desc;
        }
        
        public static MapStatus fromCode(int code) {
            for (MapStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown map status code: " + code);
        }
    }
    
    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
        if (status == null) {
            status = MapStatus.PRIVATE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}

