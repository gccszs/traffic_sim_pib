package com.traffic.sim.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户地图空间信息DTO
 * 
 * @author traffic-sim
 */
@Data
public class UserMapSpaceDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 最大地图数量
     */
    private Integer maxMaps;
    
    /**
     * 当前地图数量
     */
    private Integer currentMaps;
    
    /**
     * 已使用存储空间（字节）
     */
    private Long totalSize;
    
    /**
     * 最大存储空间（字节）
     */
    private Long maxSize;
    
    /**
     * 剩余地图数量
     */
    private Integer remainingMaps;
    
    /**
     * 剩余存储空间（字节）
     */
    private Long remainingSize;
    
    /**
     * 存储空间使用率（百分比）
     */
    private Double usageRate;
}

