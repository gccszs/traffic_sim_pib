package com.traffic.sim.plugin.auth.util;

import com.traffic.sim.common.service.TokenInfo;

/**
 * 请求上下文工具类
 * 用于存储当前请求的用户信息
 * 
 * @author traffic-sim
 */
public class RequestContext {
    
    private static final ThreadLocal<TokenInfo> USER_CONTEXT = new ThreadLocal<>();
    
    /**
     * 设置当前用户信息
     */
    public static void setCurrentUser(TokenInfo tokenInfo) {
        USER_CONTEXT.set(tokenInfo);
    }
    
    /**
     * 获取当前用户信息
     */
    public static TokenInfo getCurrentUser() {
        return USER_CONTEXT.get();
    }
    
    /**
     * 获取当前用户ID
     */
    public static String getCurrentUserId() {
        TokenInfo tokenInfo = getCurrentUser();
        return tokenInfo != null ? tokenInfo.getUserId() : null;
    }
    
    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        TokenInfo tokenInfo = getCurrentUser();
        return tokenInfo != null ? tokenInfo.getUsername() : null;
    }
    
    /**
     * 清除当前用户信息
     */
    public static void clear() {
        USER_CONTEXT.remove();
    }
}

