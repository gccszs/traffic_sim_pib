package com.traffic.sim.plugin.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security配置
 * 
 * 注意：实际的认证和授权由 plugin-auth 模块的 AuthenticationInterceptor 处理
 * Spring Security 在这里只负责：
 * 1. 禁用 CSRF（使用 JWT 不需要）
 * 2. 禁用会话（无状态）
 * 3. 放行所有请求到业务层
 *
 * @author traffic-sim
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（因为使用JWT，不需要CSRF保护）
            .csrf(csrf -> csrf.disable())
            // 放行所有请求，认证由 plugin-auth 的 AuthenticationInterceptor 处理
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            // 使用无状态会话（JWT方式）
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 禁用HTTP Basic认证
            .httpBasic(httpBasic -> httpBasic.disable())
            // 禁用表单登录
            .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }
}
