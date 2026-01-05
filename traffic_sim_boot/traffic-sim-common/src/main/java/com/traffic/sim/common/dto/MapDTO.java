package com.traffic.sim.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 地图DTO
 * 
 * @author traffic-sim
 */
@Data
public class MapDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String mapId;
    private String name;
    private String description;
    private String filePath;
    private String fileName;
    private String xmlFileName;
    private String mapImage;
    private Long ownerId;
    private Integer status;  // 0-公开，1-私有，2-禁用
    private Long fileSize;
    private String storagePath;
    private Date createTime;
    private Date updateTime;
}

