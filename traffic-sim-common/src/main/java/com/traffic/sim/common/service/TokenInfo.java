package com.traffic.sim.common.service;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 令牌信息
 * 
 * @author traffic-sim
 */
@Data
public class TokenInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 用户ID */
    private String userId;
    
    /** 用户名 */
    private String username;
    
    /** 角色 */
    private String role;
    
    /** 签发时间 */
    private Long issuedAt;
    
    /** 过期时间 */
    private Long expiresAt;
    
    /** 权限列表 */
    private List<String> permissions;
}

