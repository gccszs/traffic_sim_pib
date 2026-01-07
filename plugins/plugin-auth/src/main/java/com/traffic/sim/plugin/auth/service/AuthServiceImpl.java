package com.traffic.sim.plugin.auth.service;

import com.traffic.sim.common.constant.ErrorCode;
import com.traffic.sim.common.dto.LoginRequest;
import com.traffic.sim.common.dto.LoginResponse;
import com.traffic.sim.common.dto.RegisterRequest;
import com.traffic.sim.common.dto.UserDTO;
import com.traffic.sim.common.exception.BusinessException;
import com.traffic.sim.common.service.AuthService;
import com.traffic.sim.common.service.PermissionService;
import com.traffic.sim.common.service.TokenInfo;
import com.traffic.sim.common.service.UserService;
import com.traffic.sim.plugin.auth.config.AuthPluginProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务实现
 * 
 * @author traffic-sim
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final CaptchaService captchaService;
    private final AuthPluginProperties authProperties;
    private final PermissionService permissionService;
    
    /**
     * 令牌存储容量限制
     */
    private static final int MAX_TOKEN_STORE_SIZE = 50000;
    
    /**
     * 失效令牌保留时间（毫秒）- 默认保留1小时
     */
    private static final long INVALIDATED_TOKEN_RETENTION_MS = 60 * 60 * 1000L;
    
    /**
     * 存储刷新令牌的Map，key为refreshToken，value为TokenInfo
     */
    private final ConcurrentHashMap<String, TokenInfo> refreshTokenStore = new ConcurrentHashMap<>();
    
    /**
     * 存储已失效的令牌（用于登出）
     */
    private final ConcurrentHashMap<String, Long> invalidatedTokens = new ConcurrentHashMap<>();
    
    /**
     * 存储accessToken到refreshToken的映射关系
     * 用于登出时同时清理对应的refreshToken
     */
    private final ConcurrentHashMap<String, String> accessToRefreshMapping = new ConcurrentHashMap<>();
    
    @Override
    public LoginResponse login(LoginRequest request) {
        // 检查令牌存储容量
        checkTokenStoreCapacity();
        
        // 验证验证码
        if (authProperties.getCaptcha().getEnabled()) {
            if (!captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptcha())) {
                throw new BusinessException(ErrorCode.ERR_AUTH, "验证码错误或已过期");
            }
        }
        
        // 验证用户
        if (!userService.validatePassword(request.getUsername(), request.getPassword())) {
            throw new BusinessException(ErrorCode.ERR_AUTH, "用户名或密码错误");
        }
        
        // 获取用户信息
        UserDTO user = userService.getUserByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.ERR_AUTH, "用户不存在");
        }
        
        // 检查用户状态
        if (!"NORMAL".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ERR_AUTH, "用户已被禁用");
        }
        
        // 生成TokenInfo
        TokenInfo tokenInfo = createTokenInfo(user);
        
        // 生成令牌
        String accessToken = jwtTokenService.generateAccessToken(tokenInfo);
        String refreshToken = jwtTokenService.generateRefreshToken(tokenInfo);
        
        // 存储刷新令牌
        refreshTokenStore.put(refreshToken, tokenInfo);
        
        // 存储accessToken到refreshToken的映射关系
        accessToRefreshMapping.put(accessToken, refreshToken);
        
        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUser(user);
        response.setExpiresIn(authProperties.getJwt().getExpire());
        
        log.info("用户登录成功: {}", request.getUsername());
        return response;
    }
    
    @Override
    public void register(RegisterRequest request) {
        // 验证密码强度
        validatePasswordStrength(request.getPassword());

        // 检查用户名是否已存在
        if (userService.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.ERR_EXIST, "用户名已存在");
        }

        // 创建用户
        userService.createUserWithPassword(
            request.getUsername(),
            request.getPassword(),
            request.getEmail(),
            request.getPhoneNumber(),
            request.getInstitution()
        );

        log.info("用户注册成功: {}", request.getUsername());
    }
    
    @Override
    public TokenInfo validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        // 检查令牌是否已被失效
        if (invalidatedTokens.containsKey(token)) {
            return null;
        }
        
        // 验证令牌
        if (!jwtTokenService.validateToken(token)) {
            return null;
        }
        
        // 解析令牌
        return jwtTokenService.parseToken(token);
    }
    
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_AUTH, "刷新令牌不能为空");
        }
        
        // 从存储中获取TokenInfo
        TokenInfo tokenInfo = refreshTokenStore.get(refreshToken);
        if (tokenInfo == null) {
            throw new BusinessException(ErrorCode.ERR_AUTH, "刷新令牌无效或已过期");
        }
        
        // 验证刷新令牌
        if (!jwtTokenService.validateToken(refreshToken)) {
            refreshTokenStore.remove(refreshToken);
            throw new BusinessException(ErrorCode.ERR_AUTH, "刷新令牌已过期");
        }
        
        // 重新生成令牌
        String newAccessToken = jwtTokenService.generateAccessToken(tokenInfo);
        String newRefreshToken = jwtTokenService.generateRefreshToken(tokenInfo);
        
        // 更新刷新令牌存储
        refreshTokenStore.remove(refreshToken);
        refreshTokenStore.put(newRefreshToken, tokenInfo);
        
        // 更新accessToken到refreshToken的映射关系
        // 移除旧的映射（通过遍历找到旧的accessToken）
        accessToRefreshMapping.entrySet().removeIf(entry -> refreshToken.equals(entry.getValue()));
        accessToRefreshMapping.put(newAccessToken, newRefreshToken);
        
        // 获取用户信息
        UserDTO user = userService.getUserById(Long.parseLong(tokenInfo.getUserId()));
        
        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setUser(user);
        response.setExpiresIn(authProperties.getJwt().getExpire());
        
        return response;
    }
    
    @Override
    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        // 将令牌标记为失效
        invalidatedTokens.put(token, System.currentTimeMillis());
        
        // 通过映射关系找到并清理对应的refreshToken
        String refreshToken = accessToRefreshMapping.remove(token);
        if (refreshToken != null) {
            refreshTokenStore.remove(refreshToken);
            log.debug("已清理对应的刷新令牌");
        }
        
        // 解析令牌获取用户信息用于日志
        TokenInfo tokenInfo = jwtTokenService.parseToken(token);
        log.info("用户登出: {}", tokenInfo != null ? tokenInfo.getUsername() : "unknown");
    }
    
    /**
     * 创建TokenInfo
     */
    private TokenInfo createTokenInfo(UserDTO user) {
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setUserId(String.valueOf(user.getId()));
        tokenInfo.setUsername(user.getUsername());
        tokenInfo.setRole(user.getRoleName());
        tokenInfo.setIssuedAt(System.currentTimeMillis());
        tokenInfo.setExpiresAt(System.currentTimeMillis() + 
            authProperties.getJwt().getExpire() * 1000L);
        
        // 从数据库动态获取权限列表
        List<String> permissions = permissionService.getPermissionsByUserId(user.getId());
        
        // 如果数据库中没有配置权限，则使用默认权限
        if (permissions == null || permissions.isEmpty()) {
            log.debug("用户 {} 未配置权限，使用默认权限", user.getUsername());
            permissions = getDefaultPermissions(user.getRoleName());
        }
        
        tokenInfo.setPermissions(permissions);
        
        return tokenInfo;
    }
    
    /**
     * 获取默认权限（用于数据库未配置权限时的降级处理）
     */
    private List<String> getDefaultPermissions(String roleName) {
        if ("ADMIN".equals(roleName)) {
            return Arrays.asList("user:create", "user:update", "user:delete", "user:query",
                    "map:create", "map:update", "map:delete", "map:query",
                    "simulation:create", "simulation:control", "simulation:query");
        } else {
            return Arrays.asList("user:query", "map:query", "simulation:query");
        }
    }
    
    /**
     * 验证密码强度
     */
    private void validatePasswordStrength(String password) {
        AuthPluginProperties.Password passwordConfig = authProperties.getPassword();
        
        if (password.length() < passwordConfig.getMinLength()) {
            throw new BusinessException(ErrorCode.ERR_ARG, 
                "密码长度不能少于" + passwordConfig.getMinLength() + "位");
        }
        
        if (passwordConfig.getRequireUppercase() && 
            !password.matches(".*[A-Z].*")) {
            throw new BusinessException(ErrorCode.ERR_ARG, "密码必须包含大写字母");
        }
        
        if (passwordConfig.getRequireLowercase() && 
            !password.matches(".*[a-z].*")) {
            throw new BusinessException(ErrorCode.ERR_ARG, "密码必须包含小写字母");
        }
        
        if (passwordConfig.getRequireDigit() && 
            !password.matches(".*[0-9].*")) {
            throw new BusinessException(ErrorCode.ERR_ARG, "密码必须包含数字");
        }
        
        if (passwordConfig.getRequireSpecial() && 
            !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new BusinessException(ErrorCode.ERR_ARG, "密码必须包含特殊字符");
        }
    }
    
    /**
     * 清理过期的令牌存储
     * 由定时任务调用
     */
    public void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        
        // 清理过期的刷新令牌
        int refreshTokensBefore = refreshTokenStore.size();
        refreshTokenStore.entrySet().removeIf(entry -> {
            TokenInfo tokenInfo = entry.getValue();
            // 使用刷新令牌的过期时间判断
            long refreshExpireTime = tokenInfo.getIssuedAt() + 
                authProperties.getJwt().getRefreshExpire() * 1000L;
            return now > refreshExpireTime;
        });
        int refreshTokensRemoved = refreshTokensBefore - refreshTokenStore.size();
        
        // 清理过期的失效令牌记录（保留一段时间后删除）
        int invalidatedBefore = invalidatedTokens.size();
        invalidatedTokens.entrySet().removeIf(entry -> 
            now - entry.getValue() > INVALIDATED_TOKEN_RETENTION_MS);
        int invalidatedRemoved = invalidatedBefore - invalidatedTokens.size();
        
        // 清理孤立的映射关系
        int mappingsBefore = accessToRefreshMapping.size();
        accessToRefreshMapping.entrySet().removeIf(entry -> 
            !refreshTokenStore.containsKey(entry.getValue()));
        int mappingsRemoved = mappingsBefore - accessToRefreshMapping.size();
        
        if (refreshTokensRemoved > 0 || invalidatedRemoved > 0 || mappingsRemoved > 0) {
            log.info("令牌清理完成: 刷新令牌移除 {}, 失效记录移除 {}, 映射关系移除 {}", 
                refreshTokensRemoved, invalidatedRemoved, mappingsRemoved);
        }
    }
    
    /**
     * 检查并维护令牌存储容量
     */
    private void checkTokenStoreCapacity() {
        if (refreshTokenStore.size() >= MAX_TOKEN_STORE_SIZE) {
            log.warn("刷新令牌存储接近上限: {}, 触发清理", refreshTokenStore.size());
            cleanExpiredTokens();
        }
    }
}

