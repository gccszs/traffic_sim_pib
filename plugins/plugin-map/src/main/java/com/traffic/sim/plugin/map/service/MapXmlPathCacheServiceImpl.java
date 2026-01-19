package com.traffic.sim.plugin.map.service;

import com.traffic.sim.common.service.MapXmlPathCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 地图XML路径缓存服务实现
 * 使用Redis缓存 mapId -> xmlFilePath 的映射
 * 
 * @author traffic-sim
 */
@Service
@Slf4j
public class MapXmlPathCacheServiceImpl implements MapXmlPathCacheService {
    
    /**
     * Redis模板，可选注入
     */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;
    
    /**
     * 是否开启缓存
     */
    @Value("${app.cache.enabled:true}")
    private boolean cacheEnabled;
    
    /**
     * Redis Key 前缀
     */
    private static final String KEY_PREFIX = "map:xml_path:";
    
    /**
     * 缓存过期时间（7天）
     */
    private static final long EXPIRE_DAYS = 7;
    
    @Override
    public void cacheXmlPath(String mapId, String xmlFilePath) {
        if (mapId == null || xmlFilePath == null) {
            log.warn("Cannot cache null mapId or xmlFilePath");
            return;
        }
        
        if (!cacheEnabled) {
            log.debug("Cache is disabled, skipping cacheXmlPath for mapId: {}", mapId);
            return;
        }
        
        if (redisTemplate == null) {
            log.warn("RedisTemplate is not available, skipping cacheXmlPath for mapId: {}", mapId);
            return;
        }
        
        try {
            String key = KEY_PREFIX + mapId;
            redisTemplate.opsForValue().set(key, xmlFilePath, EXPIRE_DAYS, TimeUnit.DAYS);
            log.info("Cached xml path for mapId {}: {}", mapId, xmlFilePath);
        } catch (Exception e) {
            log.error("Failed to cache xml path for mapId {}: {}", mapId, e.getMessage(), e);
            // Redis连接失败时不影响主流程
        }
    }
    
    @Override
    public String getXmlPath(String mapId) {
        if (mapId == null) {
            return null;
        }
        
        if (!cacheEnabled) {
            log.debug("Cache is disabled, skipping getXmlPath for mapId: {}", mapId);
            return null;
        }
        
        if (redisTemplate == null) {
            log.warn("RedisTemplate is not available, skipping getXmlPath for mapId: {}", mapId);
            return null;
        }
        
        try {
            String key = KEY_PREFIX + mapId;
            String xmlFilePath = redisTemplate.opsForValue().get(key);
            
            if (xmlFilePath != null) {
                log.debug("Cache hit for mapId {}: {}", mapId, xmlFilePath);
            } else {
                log.debug("Cache miss for mapId: {}", mapId);
            }
            
            return xmlFilePath;
        } catch (Exception e) {
            log.error("Failed to get xml path from cache for mapId {}: {}", mapId, e.getMessage(), e);
            // Redis连接失败时返回null，不影响主流程
            return null;
        }
    }
    
    @Override
    public void evictXmlPath(String mapId) {
        if (mapId == null) {
            return;
        }
        
        if (!cacheEnabled) {
            log.debug("Cache is disabled, skipping evictXmlPath for mapId: {}", mapId);
            return;
        }
        
        if (redisTemplate == null) {
            log.warn("RedisTemplate is not available, skipping evictXmlPath for mapId: {}", mapId);
            return;
        }
        
        try {
            String key = KEY_PREFIX + mapId;
            Boolean deleted = redisTemplate.delete(key);
            log.info("Evicted xml path cache for mapId {}: {}", mapId, deleted);
        } catch (Exception e) {
            log.error("Failed to evict xml path cache for mapId {}: {}", mapId, e.getMessage(), e);
            // Redis连接失败时不影响主流程
        }
    }
    
    @Override
    public boolean hasXmlPath(String mapId) {
        if (mapId == null) {
            return false;
        }
        
        if (!cacheEnabled) {
            log.debug("Cache is disabled, skipping hasXmlPath for mapId: {}", mapId);
            return false;
        }
        
        if (redisTemplate == null) {
            log.warn("RedisTemplate is not available, skipping hasXmlPath for mapId: {}", mapId);
            return false;
        }
        
        try {
            String key = KEY_PREFIX + mapId;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check xml path cache for mapId {}: {}", mapId, e.getMessage(), e);
            // Redis连接失败时返回false，不影响主流程
            return false;
        }
    }
}
