package com.traffic.sim.common.service;

/**
 * 地图XML路径缓存服务接口
 * 用于缓存 mapId -> xmlFilePath 的映射
 * 
 * @author traffic-sim
 */
public interface MapXmlPathCacheService {
    
    /**
     * 缓存地图XML路径
     * 
     * @param mapId 地图ID
     * @param xmlFilePath Python端的XML文件路径
     */
    void cacheXmlPath(String mapId, String xmlFilePath);
    
    /**
     * 获取缓存的XML路径
     * 
     * @param mapId 地图ID
     * @return XML文件路径，如果不存在返回null
     */
    String getXmlPath(String mapId);
    
    /**
     * 删除缓存
     * 
     * @param mapId 地图ID
     */
    void evictXmlPath(String mapId);
    
    /**
     * 检查缓存是否存在
     * 
     * @param mapId 地图ID
     * @return 是否存在
     */
    boolean hasXmlPath(String mapId);
}
