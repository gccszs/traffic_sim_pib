package com.traffic.sim.common.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 地图更新请求
 * 
 * @author traffic-sim
 */
@Data
public class MapUpdateRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Size(max = 255, message = "地图名称长度不能超过255个字符")
    private String name;
    
    @Size(max = 500, message = "地图描述长度不能超过500个字符")
    private String description;
    
    private Integer status;  // 0-公开，1-私有，2-禁用
}

