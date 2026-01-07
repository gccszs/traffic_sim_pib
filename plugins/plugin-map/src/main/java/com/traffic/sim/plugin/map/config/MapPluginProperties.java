package com.traffic.sim.plugin.map.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * 地图插件配置属性
 * 
 * @author traffic-sim
 */
@Data
@ConfigurationProperties(prefix = "plugin.map")
public class MapPluginProperties {
    
    /**
     * 配额配置
     */
    private QuotaConfig quota = new QuotaConfig();
    
    /**
     * 文件上传配置
     */
    private UploadConfig upload = new UploadConfig();
    
    /**
     * 存储配置
     */
    private StorageConfig storage = new StorageConfig();
    
    /**
     * Python服务配置
     */
    private PythonServiceConfig pythonService = new PythonServiceConfig();
    
    @Data
    public static class QuotaConfig {
        /**
         * 默认最大地图数量
         */
        private int defaultMaxMaps = 50;
        
        /**
         * 默认最大存储空间（字节，默认1GB）
         */
        private long defaultMaxSize = 1073741824L;
    }
    
    @Data
    public static class UploadConfig {
        /**
         * 最大文件大小（100MB）
         */
        private long maxFileSize = 104857600L;
        
        /**
         * 允许的文件扩展名
         */
        private List<String> allowedExtensions = Arrays.asList("txt", "osm", "xml");
    }
    
    @Data
    public static class StorageConfig {
        /**
         * 基础存储路径
         */
        private String basePath = "maps";
        
        /**
         * 用户路径模式
         */
        private String userPathPattern = "maps/{userId}/{mapId}";
    }
    
    @Data
    public static class PythonServiceConfig {
        /**
         * Python服务URL（HTTP接口）
         */
        private String url = "http://localhost:8000";
        
        /**
         * 超时时间（毫秒）
         */
        private long timeout = 300000;
        
        /**
         * gRPC主机地址
         */
        private String grpcHost = "localhost";
        
        /**
         * gRPC端口
         */
        private int grpcPort = 50052;
    }
}

