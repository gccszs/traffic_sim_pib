package com.traffic.sim.plugin.auth.config;

import com.traffic.sim.plugin.auth.service.AuthServiceImpl;
import com.traffic.sim.plugin.auth.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 认证相关定时清理任务
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AuthCleanupScheduler {
    
    private final CaptchaService captchaService;
    private final AuthServiceImpl authService;
    
    /**
     * 每5分钟清理一次过期验证码
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cleanExpiredCaptcha() {
        log.debug("开始清理过期验证码...");
        captchaService.cleanExpiredCaptcha();
    }
    
    /**
     * 每10分钟清理一次过期令牌
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void cleanExpiredTokens() {
        log.debug("开始清理过期令牌...");
        authService.cleanExpiredTokens();
    }
}

