package com.traffic.sim.plugin.map.service;

import com.traffic.sim.plugin.map.entity.MapEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 地图权限服务
 * 
 * @author traffic-sim
 */
@Service
@RequiredArgsConstructor
public class MapPermissionService {
    
    /**
     * 检查用户是否有权限访问地图
     */
    public boolean canAccess(MapEntity map, Long userId, boolean isAdmin) {
        // 管理员可以访问所有地图（除了禁用的）
        if (isAdmin && map.getStatus() != MapEntity.MapStatus.FORBIDDEN) {
            return true;
        }
        
        // 公开地图，所有用户都可以访问
        if (map.getStatus() == MapEntity.MapStatus.PUBLIC) {
            return true;
        }
        
        // 私有地图，只有所有者可以访问
        if (map.getStatus() == MapEntity.MapStatus.PRIVATE) {
            return map.getOwnerId().equals(userId);
        }
        
        // 禁用地图，无法访问
        return false;
    }
    
    /**
     * 检查用户是否有权限修改地图
     */
    public boolean canModify(MapEntity map, Long userId, boolean isAdmin) {
        // 管理员可以修改所有地图
        if (isAdmin) {
            return true;
        }
        
        // 只有所有者可以修改
        return map.getOwnerId().equals(userId);
    }
    
    /**
     * 检查用户是否有权限删除地图
     */
    public boolean canDelete(MapEntity map, Long userId, boolean isAdmin) {
        // 管理员可以删除所有地图
        if (isAdmin) {
            return true;
        }
        
        // 只有所有者可以删除
        return map.getOwnerId().equals(userId);
    }
}

