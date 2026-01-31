package com.traffic.sim.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Web配置类
 * 配置跨域、拦截器等
 * 
 * @author traffic-sim
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * 允许的跨域来源列表
     * 可通过配置文件覆盖: app.cors.allowed-origins
     * 支持通配符 * 表示允许所有来源（开发环境）
     */
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;
    
    /**
     * 跨域配置 - WebMvcConfigurer 方式
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 如果配置为 * 则允许所有来源
        if ("*".equals(allowedOrigins.trim())) {
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")  // 使用 allowedOriginPatterns 支持通配符
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                    .allowedHeaders("*")
                    .exposedHeaders("Authorization", "Content-Type", "X-Requested-With")
                    .allowCredentials(true)
                    .maxAge(3600);
        } else {
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            
            registry.addMapping("/**")
                    .allowedOrigins(origins.toArray(new String[0]))
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                    .allowedHeaders("*")
                    .exposedHeaders("Authorization", "Content-Type", "X-Requested-With")
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }
    
    /**
     * CORS 过滤器 - 更底层的跨域处理
     * 确保在 Spring Security 等过滤器之前处理跨域
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 如果配置为 * 则允许所有来源
        if ("*".equals(allowedOrigins.trim())) {
            config.addAllowedOriginPattern("*");  // 使用 allowedOriginPattern 支持通配符
        } else {
            // 允许的来源
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            origins.forEach(config::addAllowedOrigin);
        }
        
        // 允许携带凭证（cookies, authorization headers）
        config.setAllowCredentials(true);
        
        // 允许的请求方法
        config.addAllowedMethod("*");
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 暴露的响应头
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        
        // 预检请求缓存时间
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}

