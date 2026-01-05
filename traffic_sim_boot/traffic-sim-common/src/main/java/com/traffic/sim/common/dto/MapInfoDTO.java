package com.traffic.sim.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 地图信息DTO（从MongoDB获取）
 * 
 * @author traffic-sim
 */
@Data
public class MapInfoDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 地图ID
     */
    private String mapId;
    
    /**
     * 地图数据（从MongoDB获取的完整数据）
     */
    private Map<String, Object> mapData;
    
    /**
     * 地图元数据
     */
    private Map<String, Object> metadata;
}

