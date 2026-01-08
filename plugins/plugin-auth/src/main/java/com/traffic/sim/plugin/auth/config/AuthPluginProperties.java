package com.traffic.sim.plugin.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证插件配置属性
 * 
 * @author traffic-sim
 */
@Data
@ConfigurationProperties(prefix = "plugin.auth")
public class AuthPluginProperties {
    
    /**
     * JWT配置
     */
    private Jwt jwt = new Jwt();
    
    /**
     * 密码配置
     */
    private Password password = new Password();
    
    /**
     * 验证码配置
     */
    private Captcha captcha = new Captcha();
    
    /**
     * 拦截器配置
     */
    private Interceptor interceptor = new Interceptor();
    
    @Data
    public static class Jwt {
        /**
         * JWT密钥
         */
        private String secret = "traffic-sim-jwt-secret-key-change-in-production";
        
        /**
         * 访问令牌过期时间（秒）
         */
        private Long expire = 3600L;
        
        /**
         * 刷新令牌过期时间（秒）
         */
        private Long refreshExpire = 86400L;
    }
    
    @Data
    public static class Password {
        /**
         * 最小长度
         */
        private Integer minLength = 6;
        
        /**
         * 需要大写字母
         */
        private Boolean requireUppercase = false;
        
        /**
         * 需要小写字母
         */
        private Boolean requireLowercase = false;
        
        /**
         * 需要数字
         */
        private Boolean requireDigit = false;
        
        /**
         * 需要特殊字符
         */
        private Boolean requireSpecial = false;
    }
    
    @Data
    public static class Captcha {
        /**
         * 是否启用验证码
         */
        private Boolean enabled = true;
        
        /**
         * 验证码图片宽度
         */
        private Integer width = 120;
        
        /**
         * 验证码图片高度
         */
        private Integer height = 40;
        
        /**
         * 验证码长度
         */
        private Integer length = 4;
        
        /**
         * 验证码过期时间（秒）
         */
        private Integer expireSeconds = 300;
    }
    
    @Data
    public static class Interceptor {
        /**
         * 排除路径列表（不需要认证的路径）
         */
        private java.util.List<String> excludePaths = java.util.Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/captcha",
            "/auth/refresh",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/error",
            "/actuator/**"
        );
    }
}

