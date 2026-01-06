package com.traffic.sim.common.service;

import com.traffic.sim.common.dto.LoginRequest;
import com.traffic.sim.common.dto.LoginResponse;
import com.traffic.sim.common.dto.RegisterRequest;

/**
 * 认证服务接口
 * 定义在common模块，由plugin-auth模块实现
 * 
 * @author traffic-sim
 */
public interface AuthService {
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 用户注册
     */
    void register(RegisterRequest request);
    
    /**
     * 验证JWT令牌
     */
    TokenInfo validateToken(String token);
    
    /**
     * 刷新令牌
     */
    LoginResponse refreshToken(String refreshToken);
    
    /**
     * 用户登出
     */
    void logout(String token);
}

