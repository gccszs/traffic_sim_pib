package com.traffic.sim.plugin.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateByAdminRequest {
    
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phoneNumber;
    
    private String institution;
    
    private Integer roleId;
    
    private String status;
    
    private String banReason;
}
