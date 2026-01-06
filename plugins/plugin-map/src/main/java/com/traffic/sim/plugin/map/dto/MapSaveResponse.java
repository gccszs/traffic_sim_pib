package com.traffic.sim.plugin.map.dto;

import lombok.Data;

/**
 * 地图保存响应（旧版兼容）
 * 
 * @author traffic-sim
 */
@Data
public class MapSaveResponse {
    
    private String mapId;
    private String status;
}

