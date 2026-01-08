package com.traffic.sim.plugin.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserBanRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "操作类型不能为空")
    private String action;
    
    private String reason;
}
