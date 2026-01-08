package com.traffic.sim.plugin.auth.config;

import com.traffic.sim.plugin.auth.interceptor.AuthenticationInterceptor;
import com.traffic.sim.plugin.auth.interceptor.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 认证插件自动配置类
 * 
 * @author traffic-sim
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthPluginProperties.class)
@RequiredArgsConstructor
public class AuthPluginAutoConfiguration implements WebMvcConfigurer {
    
    private final AuthenticationInterceptor authenticationInterceptor;
    private final PermissionInterceptor permissionInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册认证拦截器
        registry.addInterceptor(authenticationInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/auth/login",
                "/auth/register",
                "/auth/captcha",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/error"
            );
        
        // 注册权限拦截器（在认证拦截器之后执行）
        registry.addInterceptor(permissionInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/auth/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/error"
            );
    }
}

