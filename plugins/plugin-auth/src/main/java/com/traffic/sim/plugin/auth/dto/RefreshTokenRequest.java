package com.traffic.sim.plugin.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 刷新令牌请求DTO
 * 
 * @author traffic-sim
 */
@Data
public class RefreshTokenRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
}

