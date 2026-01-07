package com.traffic.sim.plugin.auth.service;

import com.traffic.sim.common.service.TokenInfo;
import com.traffic.sim.plugin.auth.config.AuthPluginProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT令牌服务
 * 
 * @author traffic-sim
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {
    
    private final AuthPluginProperties authProperties;
    
    /**
     * 生成访问令牌
     */
    public String generateAccessToken(TokenInfo tokenInfo) {
        return generateToken(tokenInfo, authProperties.getJwt().getExpire());
    }
    
    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(TokenInfo tokenInfo) {
        return generateToken(tokenInfo, authProperties.getJwt().getRefreshExpire());
    }
    
    /**
     * 生成令牌
     */
    private String generateToken(TokenInfo tokenInfo, Long expireSeconds) {
        long now = System.currentTimeMillis();
        long expireTime = now + expireSeconds * 1000;
        
        SecretKey key = Keys.hmacShaKeyFor(
            authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
        
        return Jwts.builder()
            .subject(tokenInfo.getUserId())
            .claim("username", tokenInfo.getUsername())
            .claim("role", tokenInfo.getRole())
            .claim("permissions", tokenInfo.getPermissions())
            .issuedAt(new Date(now))
            .expiration(new Date(expireTime))
            .signWith(key)
            .compact();
    }
    
    /**
     * 解析令牌
     */
    public TokenInfo parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
            );
            
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setUserId(claims.getSubject());
            tokenInfo.setUsername(claims.get("username", String.class));
            tokenInfo.setRole(claims.get("role", String.class));
            tokenInfo.setPermissions(claims.get("permissions", List.class));
            tokenInfo.setIssuedAt(claims.getIssuedAt().getTime());
            tokenInfo.setExpiresAt(claims.getExpiration().getTime());
            
            return tokenInfo;
        } catch (Exception e) {
            log.error("解析JWT令牌失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证令牌是否有效
     * 注意：parseToken 在解析过程中会验证签名和过期时间，
     * 如果令牌无效或已过期，会返回 null
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        // parseToken 已经处理了所有验证逻辑，包括签名验证和过期检查
        return parseToken(token) != null;
    }
}

