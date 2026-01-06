package com.traffic.sim.plugin.map.dto;

import com.traffic.sim.common.dto.MapDTO;
import com.traffic.sim.common.response.PageResult;
import lombok.Data;

import java.util.List;

/**
 * 地图列表响应（旧版兼容）
 * 
 * @author traffic-sim
 */
@Data
public class MapListResponse {
    
    private List<MapDTO> mapList;
    private Integer totalPages;
    private Long totalElement;
    
    /**
     * 从PageResult转换
     */
    public static MapListResponse fromPageResult(PageResult<MapDTO> pageResult) {
        MapListResponse response = new MapListResponse();
        response.setMapList(pageResult.getRecords());
        response.setTotalPages(pageResult.getPages());
        response.setTotalElement(pageResult.getTotal());
        return response;
    }
}

