package com.traffic.sim.plugin.auth.interceptor;

import com.traffic.sim.common.constant.ErrorCode;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.service.TokenInfo;
import com.traffic.sim.common.util.JsonUtils;
import com.traffic.sim.plugin.auth.annotation.RequirePermission;
import com.traffic.sim.plugin.auth.annotation.RequireRole;
import com.traffic.sim.plugin.auth.util.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * 权限拦截器
 * 用于验证用户权限和角色
 * 
 * @author traffic-sim
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 获取当前用户信息
        TokenInfo tokenInfo = RequestContext.getCurrentUser();
        if (tokenInfo == null) {
            handleForbidden(response, "未认证");
            return false;
        }
        
        // 检查方法级别的权限注解
        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission != null) {
            if (!hasPermission(tokenInfo, Arrays.asList(requirePermission.value()))) {
                handleForbidden(response, "权限不足");
                return false;
            }
        }
        
        // 检查方法级别的角色注解
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole != null) {
            if (!hasRole(tokenInfo, Arrays.asList(requireRole.value()))) {
                handleForbidden(response, "角色权限不足");
                return false;
            }
        }
        
        // 检查类级别的权限注解
        RequirePermission classPermission = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        if (classPermission != null) {
            if (!hasPermission(tokenInfo, Arrays.asList(classPermission.value()))) {
                handleForbidden(response, "权限不足");
                return false;
            }
        }
        
        // 检查类级别的角色注解
        RequireRole classRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        if (classRole != null) {
            if (!hasRole(tokenInfo, Arrays.asList(classRole.value()))) {
                handleForbidden(response, "角色权限不足");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查是否有权限
     */
    private boolean hasPermission(TokenInfo tokenInfo, List<String> requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.isEmpty()) {
            return true;
        }
        
        List<String> userPermissions = tokenInfo.getPermissions();
        if (userPermissions == null || userPermissions.isEmpty()) {
            return false;
        }
        
        // 检查是否包含所有必需的权限
        return userPermissions.containsAll(requiredPermissions);
    }
    
    /**
     * 检查是否有角色
     */
    private boolean hasRole(TokenInfo tokenInfo, List<String> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return true;
        }
        
        String userRole = tokenInfo.getRole();
        if (userRole == null) {
            return false;
        }
        
        // 检查是否包含任一必需的角色
        return requiredRoles.contains(userRole);
    }
    
    /**
     * 处理禁止访问响应
     */
    private void handleForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        
        ApiResponse<Object> apiResponse = ApiResponse.error(ErrorCode.ERR_PERMISSION, message);
        String json = JsonUtils.toJson(apiResponse);
        response.getWriter().write(json);
    }
}

