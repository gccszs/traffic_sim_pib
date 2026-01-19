package com.traffic.sim.plugin.map.client;

import com.traffic.sim.plugin.map.grpc.ConvertMapRequest;
import com.traffic.sim.plugin.map.grpc.ConvertMapResponse;
import com.traffic.sim.plugin.map.grpc.MapConvertServiceGrpc;
import com.traffic.sim.plugin.map.grpc.PreviewMapRequest;
import com.traffic.sim.plugin.map.grpc.PreviewMapResponse;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Python地图服务gRPC客户端
 * 用于调用Python服务进行地图格式转换（MapConvertService）
 * 
 * ============================================================
 * 【部署说明】
 * 该客户端连接到 Python 引擎服务的 MapConvertService (默认端口 50052)
 * 
 * 部署架构:
 * ┌─────────────────┐        gRPC (50052)        ┌──────────────────┐
 * │  Java 主服务    │  ─────────────────────────> │  Python 引擎服务  │
 * │ (plugin-map)   │                            │ (MapConvertService)│
 * └─────────────────┘                            └──────────────────┘
 * 
 * 支持的部署模式:
 * 1. 同主机部署: Java和Python服务在同一台机器，使用 localhost:50052
 * 2. 分布式部署: Java和Python服务在不同机器，配置 PYTHON_MAP_SERVICE_HOST
 * 
 * 配置方式:
 * - application.yml: grpc.client.map-service.address
 * - 环境变量: PYTHON_MAP_SERVICE_HOST, PYTHON_MAP_SERVICE_PORT
 * ============================================================
 * 
 * @author traffic-sim
 */
@Component
@Slf4j
public class PythonGrpcClient {
    
    @Value("${grpc.client.map-service.enabled:true}")
    private boolean grpcEnabled;
    
    /**
     * gRPC 客户端 Stub
     * 使用 @Lazy 延迟初始化，避免 Python 服务不可用时启动失败
     * 
     * 对应配置: grpc.client.map-service.address
     */
    @Lazy
    @GrpcClient("map-service")
    private MapConvertServiceGrpc.MapConvertServiceBlockingStub blockingStub;
    
    /**
     * 初始化检查
     */
    @PostConstruct
    public void init() {
        if (!grpcEnabled) {
            log.info("Map gRPC client is disabled by configuration, will use fallback responses");
            return;
        }
        
        try {
            if (blockingStub != null) {
                log.info("Map gRPC client (MapConvertService) is available");
            } else {
                log.warn("Map gRPC client is null, will use fallback responses");
            }
        } catch (Exception e) {
            log.warn("Map gRPC client initialization check failed: {}", e.getMessage());
        }
    }
    
    /**
     * 检查 gRPC 是否可用
     */
    private boolean isGrpcAvailable() {
        if (!grpcEnabled) {
            return false;
        }
        try {
            return blockingStub != null;
        } catch (Exception e) {
            log.debug("Map gRPC client not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 上传并转换地图文件
     * 调用 Python 服务的 ConvertMap RPC
     * 
     * @param file 地图文件（支持 .osm, .txt 格式）
     * @param userId 用户ID
     * @return 转换结果（包含XML文件数据）
     */
    public ConvertFileResponse uploadAndConvertFile(MultipartFile file, String userId) {
        // 如果 gRPC 不可用，返回兜底数据
        if (!isGrpcAvailable()) {
            log.warn("Map gRPC service not available, returning fallback response");
            return createFallbackConvertResponse(
                "Map conversion service unavailable",
                "Please ensure Python MapConvertService is running at the configured address"
            );
        }
        
        try {
            // 读取文件内容
            byte[] fileContent = file.getBytes();
            String fileName = file.getOriginalFilename();
            
            log.info("Calling ConvertMap RPC - userId: {}, fileName: {}, size: {} bytes",
                userId, fileName, fileContent.length);
            
            // 构建 gRPC 请求
            ConvertMapRequest request = ConvertMapRequest.newBuilder()
                .setFileContent(ByteString.copyFrom(fileContent))
                .setFileName(fileName != null ? fileName : "unknown.txt")
                .setUserId(userId)
                .build();
            
            // 调用 gRPC 服务
            ConvertMapResponse grpcResponse = blockingStub.convertMap(request);
            
            // 转换响应
            ConvertFileResponse response = new ConvertFileResponse();
            response.setSuccess(grpcResponse.getSuccess());
            response.setMessage(grpcResponse.getMessage());
            
            if (grpcResponse.getSuccess()) {
                response.setXmlData(grpcResponse.getXmlData().toByteArray());
                response.setXmlFileName(grpcResponse.getXmlFileName());
                response.setXmlFilePath(grpcResponse.getXmlFilePath());  // Python端保存的文件路径
                log.info("ConvertMap successful - xmlFileName: {}, xmlSize: {} bytes, xmlFilePath: {}",
                    grpcResponse.getXmlFileName(), grpcResponse.getXmlData().size(), grpcResponse.getXmlFilePath());
            } else {
                log.warn("ConvertMap failed: {}", grpcResponse.getMessage());
            }
            
            return response;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for ConvertMap: {} - {}", e.getStatus().getCode(), e.getMessage());
            return createFallbackConvertResponse(
                "Map conversion failed: " + e.getStatus().getCode(),
                e.getMessage()
            );
        } catch (IOException e) {
            log.error("Failed to read file for ConvertMap", e);
            return createFallbackConvertResponse(
                "Failed to read file: " + e.getMessage(),
                null
            );
        } catch (Exception e) {
            log.error("Unexpected error in ConvertMap", e);
            return createFallbackConvertResponse(
                "Unexpected error: " + e.getMessage(),
                null
            );
        }
    }
    
    /**
     * 预览地图文件
     * 调用 Python 服务的 PreviewMap RPC
     * 
     * @param file 地图文件
     * @param userId 用户ID
     * @return 预览结果（包含道路数量、交叉口数量等统计信息）
     */
    public PreviewFileResponse previewMapFile(MultipartFile file, String userId) {
        // 如果 gRPC 不可用，返回兜底数据
        if (!isGrpcAvailable()) {
            log.warn("Map gRPC service not available for preview, returning fallback response");
            return createFallbackPreviewResponse(
                "Map preview service unavailable",
                "Please ensure Python MapConvertService is running"
            );
        }
        
        try {
            // 读取文件内容
            byte[] fileContent = file.getBytes();
            String fileName = file.getOriginalFilename();
            
            log.info("Calling PreviewMap RPC - userId: {}, fileName: {}, size: {} bytes",
                userId, fileName, fileContent.length);
            
            // 构建 gRPC 请求
            PreviewMapRequest request = PreviewMapRequest.newBuilder()
                .setFileContent(ByteString.copyFrom(fileContent))
                .setFileName(fileName != null ? fileName : "unknown.txt")
                .setUserId(userId)
                .build();
            
            // 调用 gRPC 服务
            PreviewMapResponse grpcResponse = blockingStub.previewMap(request);
            
            // 转换响应
            PreviewFileResponse response = new PreviewFileResponse();
            response.setSuccess(grpcResponse.getSuccess());
            response.setMessage(grpcResponse.getMessage());
            
            if (grpcResponse.getSuccess()) {
                response.setPreviewData(grpcResponse.getPreviewData());
                response.setRoadCount(grpcResponse.getRoadCount());
                response.setIntersectionCount(grpcResponse.getIntersectionCount());
                log.info("PreviewMap successful - roads: {}, intersections: {}",
                    grpcResponse.getRoadCount(), grpcResponse.getIntersectionCount());
            } else {
                log.warn("PreviewMap failed: {}", grpcResponse.getMessage());
            }
            
            return response;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for PreviewMap: {} - {}", e.getStatus().getCode(), e.getMessage());
            return createFallbackPreviewResponse(
                "Map preview failed: " + e.getStatus().getCode(),
                e.getMessage()
            );
        } catch (IOException e) {
            log.error("Failed to read file for PreviewMap", e);
            return createFallbackPreviewResponse(
                "Failed to read file: " + e.getMessage(),
                null
            );
        } catch (Exception e) {
            log.error("Unexpected error in PreviewMap", e);
            return createFallbackPreviewResponse(
                "Unexpected error: " + e.getMessage(),
                null
            );
        }
    }
    
    /**
     * 创建转换兜底响应
     */
    private ConvertFileResponse createFallbackConvertResponse(String message, String detail) {
        ConvertFileResponse response = new ConvertFileResponse();
        response.setSuccess(false);
        response.setMessage(message + (detail != null ? " - " + detail : ""));
        return response;
    }
    
    /**
     * 创建预览兜底响应
     */
    private PreviewFileResponse createFallbackPreviewResponse(String message, String detail) {
        PreviewFileResponse response = new PreviewFileResponse();
        response.setSuccess(false);
        response.setMessage(message + (detail != null ? " - " + detail : ""));
        response.setRoadCount(0);
        response.setIntersectionCount(0);
        return response;
    }
    
    /**
     * 转换文件响应
     */
    public static class ConvertFileResponse {
        private boolean success;
        private String message;
        private byte[] xmlData;
        private String xmlFileName;
        private String xmlFilePath;  // Python端保存的XML文件路径
        private Map<String, Object> mapJsonData;  // 解析后的地图JSON数据
        private int roadCount;
        private int intersectionCount;
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public byte[] getXmlData() {
            return xmlData;
        }
        
        public void setXmlData(byte[] xmlData) {
            this.xmlData = xmlData;
        }
        
        public String getXmlFileName() {
            return xmlFileName;
        }
        
        public void setXmlFileName(String xmlFileName) {
            this.xmlFileName = xmlFileName;
        }
        
        public String getXmlFilePath() {
            return xmlFilePath;
        }
        
        public void setXmlFilePath(String xmlFilePath) {
            this.xmlFilePath = xmlFilePath;
        }
        
        public Map<String, Object> getMapJsonData() {
            return mapJsonData;
        }
        
        public void setMapJsonData(Map<String, Object> mapJsonData) {
            this.mapJsonData = mapJsonData;
        }
        
        public int getRoadCount() {
            return roadCount;
        }
        
        public void setRoadCount(int roadCount) {
            this.roadCount = roadCount;
        }
        
        public int getIntersectionCount() {
            return intersectionCount;
        }
        
        public void setIntersectionCount(int intersectionCount) {
            this.intersectionCount = intersectionCount;
        }
    }
    
    /**
     * 预览文件响应
     */
    public static class PreviewFileResponse {
        private boolean success;
        private String message;
        private String previewData;  // 预览数据（JSON格式）
        private String mapName;
        private String mapType;
        private int roadCount;       // 道路数量
        private int intersectionCount;  // 交叉口数量
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getPreviewData() {
            return previewData;
        }
        
        public void setPreviewData(String previewData) {
            this.previewData = previewData;
        }
        
        public String getMapName() {
            return mapName;
        }
        
        public void setMapName(String mapName) {
            this.mapName = mapName;
        }
        
        public String getMapType() {
            return mapType;
        }
        
        public void setMapType(String mapType) {
            this.mapType = mapType;
        }
        
        public int getRoadCount() {
            return roadCount;
        }
        
        public void setRoadCount(int roadCount) {
            this.roadCount = roadCount;
        }
        
        public int getIntersectionCount() {
            return intersectionCount;
        }
        
        public void setIntersectionCount(int intersectionCount) {
            this.intersectionCount = intersectionCount;
        }
    }
}
