package com.traffic.sim.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应DTO
 * 
 * @author traffic-sim
 */
@Data
public class LoginResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 访问令牌 */
    private String accessToken;
    
    /** 刷新令牌 */
    private String refreshToken;
    
    /** 用户信息 */
    private UserDTO user;
    
    /** 令牌过期时间（秒） */
    private Long expiresIn;
}

