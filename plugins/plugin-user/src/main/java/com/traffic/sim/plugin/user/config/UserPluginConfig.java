package com.traffic.sim.plugin.user.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 用户插件自动配置类
 * 
 * @author traffic-sim
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.traffic.sim.plugin.user")
@EntityScan(basePackages = "com.traffic.sim.plugin.user.entity")
@EnableJpaRepositories(basePackages = "com.traffic.sim.plugin.user.repository")
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

