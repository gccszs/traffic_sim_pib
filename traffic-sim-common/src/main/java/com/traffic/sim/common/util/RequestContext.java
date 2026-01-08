package com.traffic.sim.common.util;

import com.traffic.sim.common.service.TokenInfo;

public class RequestContext {
    
    private static final ThreadLocal<TokenInfo> USER_CONTEXT = new ThreadLocal<>();
    
    public static void setCurrentUser(TokenInfo tokenInfo) {
        USER_CONTEXT.set(tokenInfo);
    }
    
    public static TokenInfo getCurrentUser() {
        return USER_CONTEXT.get();
    }
    
    public static String getCurrentUserId() {
        TokenInfo tokenInfo = getCurrentUser();
        return tokenInfo != null ? tokenInfo.getUserId() : null;
    }
    
    public static String getCurrentUsername() {
        TokenInfo tokenInfo = getCurrentUser();
        return tokenInfo != null ? tokenInfo.getUsername() : null;
    }
    
    public static void clear() {
        USER_CONTEXT.remove();
    }
}
