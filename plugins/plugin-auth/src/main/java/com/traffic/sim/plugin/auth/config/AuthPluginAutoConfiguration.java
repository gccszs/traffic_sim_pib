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
    private final AuthPluginProperties authProperties;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        AuthPluginProperties.Interceptor interceptorConfig = authProperties.getInterceptor();
        
        // 注册认证拦截器
        registry.addInterceptor(authenticationInterceptor)
            .addPathPatterns(interceptorConfig.getProtectedPaths().toArray(new String[0]))
            .excludePathPatterns(interceptorConfig.getExcludePaths().toArray(new String[0]));
        
        // 注册权限拦截器（在认证拦截器之后执行）
        registry.addInterceptor(permissionInterceptor)
            .addPathPatterns(interceptorConfig.getProtectedPaths().toArray(new String[0]))
            .excludePathPatterns(interceptorConfig.getExcludePermissionPaths().toArray(new String[0]));
    }
}

