package com.traffic.sim.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求DTO
 * 
 * @author traffic-sim
 */
@Data
public class LoginRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    private String captcha;
    
    private String captchaId;
}

