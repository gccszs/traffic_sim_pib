package com.traffic.sim.common.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

/**
 * 绿信比控制请求DTO
 * 
 * @author traffic-sim
 */
@Data
public class GreenRatioControlRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 绿信比值（必填，0-100） */
    @NotNull(message = "绿信比值不能为空")
    @Min(value = 0, message = "绿信比值不能小于0")
    @Max(value = 100, message = "绿信比值不能大于100")
    private Integer greenRatio;
    
    /** 仿真信息（可选，用于日志记录） */
    private Map<String, Object> simulationInfo;
}

