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
 * 注意：实际的认证和授权由 plugin-auth 模块的拦截器处理
 * 这里只是禁用 Spring Security 的默认行为
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
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 允许认证相关接口匿名访问
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/captcha").permitAll()
                // 允许Swagger UI和API文档访问
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // 允许静态资源访问
                .requestMatchers("/static/**", "/public/**").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated()
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
