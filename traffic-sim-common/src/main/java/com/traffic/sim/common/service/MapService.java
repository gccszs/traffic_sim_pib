package com.traffic.sim.common.service;

import com.traffic.sim.common.dto.MapDTO;
import com.traffic.sim.common.dto.MapInfoDTO;
import com.traffic.sim.common.dto.MapUpdateRequest;
import com.traffic.sim.common.dto.UserMapSpaceDTO;
import com.traffic.sim.common.response.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 地图服务接口
 * 定义在common模块，由plugin-map模块实现
 * 
 * @author traffic-sim
 */
public interface MapService {
    
    /**
     * 【旧版兼容】保存地图信息
     */
    MapDTO saveMapInfo(Map<String, String> request, Long userId);
    
    /**
     * 【新版】上传并转换地图文件
     */
    MapDTO uploadAndConvertMap(MultipartFile file, String name, 
                               String description, Integer status, Long userId);
    
    /**
     * 获取用户地图列表
     */
    PageResult<MapDTO> getUserMaps(Long userId, String mapName, 
                                   Integer status, int page, int size);
    
    /**
     * 【旧版兼容】获取用户地图列表
     */
    PageResult<MapDTO> getUserMaps(Long userId, String mapName, int page, int limit);
    
    /**
     * 获取公开地图列表
     */
    PageResult<MapDTO> getPublicMaps(String mapName, int page, int size);
    
    /**
     * 获取地图详情（带权限验证）
     */
    MapDTO getMapById(String mapId, Long userId);
    
    /**
     * 更新地图信息
     */
    MapDTO updateMap(String mapId, MapUpdateRequest request, Long userId);
    
    /**
     * 删除地图
     */
    void deleteMap(String mapId, Long userId);
    
    /**
     * 【管理员】删除地图
     */
    void deleteMapByAdmin(String mapId, Integer status);
    
    /**
     * 【管理员】获取所有地图
     */
    PageResult<MapDTO> getAllMaps(String mapName, int page, int limit);
    
    /**
     * 从MongoDB获取地图数据
     */
    MapInfoDTO getMapInfoFromMongoDB(String mapId, Long userId);
    
    /**
     * 预览地图信息
     */
    MapInfoDTO previewMapInfo(String mapFile);
    
    /**
     * 获取用户地图空间信息
     */
    UserMapSpaceDTO getUserMapSpace(Long userId);
    
    /**
     * 检查用户配额
     */
    void checkUserQuota(Long userId, long fileSize);
}

