package com.traffic.sim.plugin.auth.service;

import com.traffic.sim.common.constant.ErrorCode;
import com.traffic.sim.common.dto.LoginRequest;
import com.traffic.sim.common.dto.LoginResponse;
import com.traffic.sim.common.dto.RegisterRequest;
import com.traffic.sim.common.dto.UserDTO;
import com.traffic.sim.common.exception.BusinessException;
import com.traffic.sim.common.service.AuthService;
import com.traffic.sim.common.service.TokenInfo;
import com.traffic.sim.common.service.UserService;
import com.traffic.sim.plugin.auth.config.AuthPluginProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    
    /**
     * 存储刷新令牌的Map，key为refreshToken，value为TokenInfo
     */
    private final ConcurrentHashMap<String, TokenInfo> refreshTokenStore = new ConcurrentHashMap<>();
    
    /**
     * 存储已失效的令牌（用于登出）
     */
    private final ConcurrentHashMap<String, Long> invalidatedTokens = new ConcurrentHashMap<>();
    
    @Override
    public LoginResponse login(LoginRequest request) {
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
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ERR_AUTH, "用户已被禁用");
        }
        
        // 生成TokenInfo
        TokenInfo tokenInfo = createTokenInfo(user);
        
        // 生成令牌
        String accessToken = jwtTokenService.generateAccessToken(tokenInfo);
        String refreshToken = jwtTokenService.generateRefreshToken(tokenInfo);
        
        // 存储刷新令牌
        refreshTokenStore.put(refreshToken, tokenInfo);
        
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
        UserDTO existingUser = userService.getUserByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.ERR_EXIST, "用户名已存在");
        }
        
        // 创建用户DTO
        // 注意：UserDTO 不包含密码字段，密码需要通过其他方式传递给 UserService
        // plugin-user 模块实现时，可能需要扩展 UserService 接口或使用其他方式传递密码
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(request.getUsername());
        userDTO.setEmail(request.getEmail());
        userDTO.setPhoneNumber(request.getPhoneNumber());
        userDTO.setInstitution(request.getInstitution());
        userDTO.setStatus("ACTIVE");
        userDTO.setRoleId(1); // 默认角色ID，可根据需求调整
        userDTO.setRoleName("USER"); // 默认角色名称
        
        // TODO: 密码传递问题需要解决
        // 方案1: 扩展 UserService.createUser 方法，添加密码参数
        // 方案2: 创建 CreateUserRequest DTO，包含密码字段
        // 方案3: 在 UserDTO 中添加临时密码字段（不推荐）
        // 当前实现：假设 plugin-user 模块会通过其他方式获取密码（如从 RegisterRequest）
        userService.createUser(userDTO);
        
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
        
        // 解析令牌获取刷新令牌（如果有）
        TokenInfo tokenInfo = jwtTokenService.parseToken(token);
        if (tokenInfo != null) {
            // 清理刷新令牌（需要找到对应的refreshToken，这里简化处理）
            // 实际实现中可能需要维护accessToken和refreshToken的映射关系
        }
        
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
        
        // 设置权限列表（根据角色，这里简化处理）
        List<String> permissions = new ArrayList<>();
        if ("ADMIN".equals(user.getRoleName())) {
            permissions.add("user:create");
            permissions.add("user:update");
            permissions.add("user:delete");
            permissions.add("user:query");
        } else {
            permissions.add("user:query");
        }
        tokenInfo.setPermissions(permissions);
        
        return tokenInfo;
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
}

