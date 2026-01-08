package com.traffic.sim.plugin.auth.interceptor;

import com.traffic.sim.common.constant.ErrorCode;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.service.AuthService;
import com.traffic.sim.common.service.TokenInfo;
import com.traffic.sim.common.util.JsonUtils;
import com.traffic.sim.plugin.auth.config.AuthPluginProperties;
import com.traffic.sim.common.util.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 认证拦截器
 * 用于拦截请求并验证JWT令牌
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {
    
    private final AuthService authService;
    private final AuthPluginProperties authProperties;
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        String path = request.getRequestURI();
        
        // 检查是否在排除列表中
        if (isExcludedPath(path)) {
            return true;
        }
        
        // 提取JWT令牌
        String token = extractToken(request);
        
        if (!StringUtils.hasText(token)) {
            handleUnauthorized(response, "未提供认证令牌");
            return false;
        }
        
        // 验证令牌
        TokenInfo tokenInfo = authService.validateToken(token);
        if (tokenInfo == null) {
            handleUnauthorized(response, "认证令牌无效或已过期");
            return false;
        }
        
        // 将用户信息存入请求上下文
        RequestContext.setCurrentUser(tokenInfo);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) throws Exception {
        // 清理请求上下文
        RequestContext.clear();
    }
    
    /**
     * 提取令牌
     */
    private String extractToken(HttpServletRequest request) {
        // 从Authorization头中提取
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        
        // 从请求参数中提取（可选）
        String token = request.getParameter("token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        
        return null;
    }
    
    /**
     * 检查路径是否在排除列表中
     */
    private boolean isExcludedPath(String path) {
        List<String> excludePaths = authProperties.getInterceptor().getExcludePaths();
        return excludePaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
    
    /**
     * 处理未授权响应
     */
    private void handleUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.ERR_AUTH, message);
        String json = JsonUtils.toJson(apiResponse);
        response.getWriter().write(json);
    }
}

