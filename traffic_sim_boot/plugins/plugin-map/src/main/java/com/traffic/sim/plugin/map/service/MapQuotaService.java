package com.traffic.sim.plugin.map.service;

import com.traffic.sim.common.exception.BusinessException;
import com.traffic.sim.plugin.map.config.MapPluginProperties;
import com.traffic.sim.plugin.map.entity.MapEntity;
import com.traffic.sim.plugin.map.entity.UserMapQuota;
import com.traffic.sim.plugin.map.repository.MapRepository;
import com.traffic.sim.plugin.map.repository.UserMapQuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地图配额服务
 * 
 * @author traffic-sim
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MapQuotaService {
    
    private final UserMapQuotaRepository quotaRepository;
    private final MapRepository mapRepository;
    private final MapPluginProperties mapProperties;
    
    /**
     * 检查用户配额
     */
    public void checkUserQuota(Long userId, long fileSize) {
        UserMapQuota quota = getOrCreateQuota(userId);
        
        // 检查地图数量限制
        if (quota.getCurrentMaps() >= quota.getMaxMaps()) {
            throw new BusinessException("地图数量已达上限：" + quota.getMaxMaps());
        }
        
        // 检查存储空间限制
        if (quota.getTotalSize() + fileSize > quota.getMaxSize()) {
            throw new BusinessException("存储空间不足");
        }
    }
    
    /**
     * 更新用户配额（上传后）
     */
    @Transactional
    public void updateQuotaAfterUpload(Long userId, long fileSize) {
        UserMapQuota quota = getOrCreateQuota(userId);
        quota.setCurrentMaps(quota.getCurrentMaps() + 1);
        quota.setTotalSize(quota.getTotalSize() + fileSize);
        quotaRepository.save(quota);
        log.debug("Updated quota after upload for user {}: maps={}, size={}", 
                  userId, quota.getCurrentMaps(), quota.getTotalSize());
    }
    
    /**
     * 更新用户配额（删除后）
     */
    @Transactional
    public void updateQuotaAfterDelete(Long userId, long fileSize) {
        UserMapQuota quota = getOrCreateQuota(userId);
        quota.setCurrentMaps(Math.max(0, quota.getCurrentMaps() - 1));
        quota.setTotalSize(Math.max(0, quota.getTotalSize() - fileSize));
        quotaRepository.save(quota);
        log.debug("Updated quota after delete for user {}: maps={}, size={}", 
                  userId, quota.getCurrentMaps(), quota.getTotalSize());
    }
    
    /**
     * 获取或创建用户配额
     */
    public UserMapQuota getOrCreateQuota(Long userId) {
        return quotaRepository.findByUserId(userId)
            .orElseGet(() -> {
                UserMapQuota newQuota = new UserMapQuota();
                newQuota.setUserId(userId);
                newQuota.setMaxMaps(mapProperties.getQuota().getDefaultMaxMaps());
                newQuota.setMaxSize(mapProperties.getQuota().getDefaultMaxSize());
                return quotaRepository.save(newQuota);
            });
    }
    
    /**
     * 同步用户配额（从实际地图数据计算）
     */
    @Transactional
    public void syncUserQuota(Long userId) {
        List<MapEntity> maps = mapRepository.findByOwnerId(userId);
        
        int currentMaps = maps.size();
        long totalSize = maps.stream()
            .mapToLong(m -> m.getFileSize() != null ? m.getFileSize() : 0L)
            .sum();
        
        UserMapQuota quota = getOrCreateQuota(userId);
        quota.setCurrentMaps(currentMaps);
        quota.setTotalSize(totalSize);
        quotaRepository.save(quota);
        
        log.info("Synced quota for user {}: maps={}, size={}", userId, currentMaps, totalSize);
    }
    
    /**
     * 获取用户配额信息
     */
    public UserMapQuota getUserQuota(Long userId) {
        return getOrCreateQuota(userId);
    }
}

