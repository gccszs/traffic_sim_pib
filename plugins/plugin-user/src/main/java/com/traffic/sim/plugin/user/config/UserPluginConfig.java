package com.traffic.sim.plugin.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 用户插件配置类
 * 
 * @author traffic-sim
 */
@Configuration
public class UserPluginConfig {
    
    /**
     * 密码编码器
     * 使用BCrypt进行密码加密
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

