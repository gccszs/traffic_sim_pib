package com.traffic.sim.plugin.engine.manager.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.context.annotation.Bean;

/**
 * WebSocket 超时配置
 * 
 * @author traffic-sim
 */
@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketTimeoutConfig {
    
    /**
     * 配置 WebSocket 会话超时时间
     */
    @Bean
    public ServletContextListener webSocketTimeoutListener() {
        return new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent sce) {
                log.info("Configuring WebSocket timeout settings...");
                
                // 设置 WebSocket 会话超时时间为 30 分钟（1800000 毫秒）
                // 这个设置会影响所有 WebSocket 连接
                System.setProperty("org.apache.tomcat.websocket.DEFAULT_SESSION_IDLE_TIMEOUT", "1800000");
                
                // 设置异步发送超时时间为 60 秒
                System.setProperty("org.apache.tomcat.websocket.ASYNC_SEND_TIMEOUT", "60000");
                
                log.info("WebSocket timeout configured: session_idle=1800000ms, async_send=60000ms");
            }
            
            @Override
            public void contextDestroyed(ServletContextEvent sce) {
                // 清理资源
            }
        };
    }
}
