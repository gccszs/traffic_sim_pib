package com.traffic.sim.plugin.map.client;

import com.google.protobuf.ByteString;
import com.traffic.sim.plugin.map.config.MapPluginProperties;
import com.traffic.sim.plugin.map.grpc.ConvertMapRequest;
import com.traffic.sim.plugin.map.grpc.ConvertMapResponse;
import com.traffic.sim.plugin.map.grpc.MapConvertServiceGrpc;
import com.traffic.sim.plugin.map.grpc.PreviewMapRequest;
import com.traffic.sim.plugin.map.grpc.PreviewMapResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Python服务gRPC客户端
 * 用于调用Python服务进行地图格式转换
 * 
 * @author traffic-sim
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PythonGrpcClient {
    
    private final MapPluginProperties mapProperties;
    
    private ManagedChannel channel;
    private MapConvertServiceGrpc.MapConvertServiceBlockingStub blockingStub;
    
    @PostConstruct
    public void init() {
        String host = mapProperties.getPythonService().getGrpcHost();
        int port = mapProperties.getPythonService().getGrpcPort();
        
        log.info("Initializing gRPC client for Python service at {}:{}", host, port);
        
        channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
        
        blockingStub = MapConvertServiceGrpc.newBlockingStub(channel);
        
        log.info("gRPC client initialized successfully");
    }
    
    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("gRPC channel shutdown successfully");
            } catch (InterruptedException e) {
                log.warn("gRPC channel shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 上传并转换地图文件
     * 
     * @param file 地图文件
     * @param userId 用户ID
     * @return 转换结果（包含XML文件数据）
     */
    public ConvertFileResponse uploadAndConvertFile(MultipartFile file, String userId) {
        ConvertFileResponse response = new ConvertFileResponse();
        
        try {
            byte[] fileContent = file.getBytes();
            String fileName = file.getOriginalFilename();
            
            log.info("Sending ConvertMap request: userId={}, fileName={}, size={}", 
                userId, fileName, fileContent.length);
            
            ConvertMapRequest request = ConvertMapRequest.newBuilder()
                .setFileContent(ByteString.copyFrom(fileContent))
                .setFileName(fileName != null ? fileName : "map.txt")
                .setUserId(userId)
                .build();
            
            long timeout = mapProperties.getPythonService().getTimeout();
            ConvertMapResponse grpcResponse = blockingStub
                .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
                .convertMap(request);
            
            response.setSuccess(grpcResponse.getSuccess());
            response.setMessage(grpcResponse.getMessage());
            response.setXmlData(grpcResponse.getXmlData().toByteArray());
            response.setXmlFileName(grpcResponse.getXmlFileName());
            response.setConversionMethod(grpcResponse.getConversionMethod());
            
            log.info("ConvertMap response: success={}, xmlFileName={}", 
                grpcResponse.getSuccess(), grpcResponse.getXmlFileName());
            
        } catch (IOException e) {
            log.error("Failed to read file content", e);
            response.setSuccess(false);
            response.setMessage("文件读取失败: " + e.getMessage());
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed", e);
            response.setSuccess(false);
            response.setMessage("gRPC调用失败: " + e.getStatus().getDescription());
        } catch (Exception e) {
            log.error("Unexpected error during file conversion", e);
            response.setSuccess(false);
            response.setMessage("转换失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 预览地图文件
     * 
     * @param file 地图文件
     * @param userId 用户ID
     * @return 预览结果
     */
    public PreviewFileResponse previewMapFile(MultipartFile file, String userId) {
        PreviewFileResponse response = new PreviewFileResponse();
        
        try {
            byte[] fileContent = file.getBytes();
            String fileName = file.getOriginalFilename();
            
            log.info("Sending PreviewMap request: userId={}, fileName={}", userId, fileName);
            
            PreviewMapRequest request = PreviewMapRequest.newBuilder()
                .setFileContent(ByteString.copyFrom(fileContent))
                .setFileName(fileName != null ? fileName : "map.txt")
                .setUserId(userId)
                .build();
            
            long timeout = mapProperties.getPythonService().getTimeout();
            PreviewMapResponse grpcResponse = blockingStub
                .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
                .previewMap(request);
            
            response.setSuccess(grpcResponse.getSuccess());
            response.setMessage(grpcResponse.getMessage());
            response.setPreviewData(grpcResponse.getPreviewData());
            response.setRoadCount(grpcResponse.getRoadCount());
            response.setIntersectionCount(grpcResponse.getIntersectionCount());
            
            log.info("PreviewMap response: success={}, roads={}, intersections={}", 
                grpcResponse.getSuccess(), grpcResponse.getRoadCount(), grpcResponse.getIntersectionCount());
            
        } catch (IOException e) {
            log.error("Failed to read file content", e);
            response.setSuccess(false);
            response.setMessage("文件读取失败: " + e.getMessage());
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed", e);
            response.setSuccess(false);
            response.setMessage("gRPC调用失败: " + e.getStatus().getDescription());
        } catch (Exception e) {
            log.error("Unexpected error during preview", e);
            response.setSuccess(false);
            response.setMessage("预览失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 转换文件响应
     */
    @Data
    public static class ConvertFileResponse {
        private boolean success;
        private String message;
        private byte[] xmlData;
        private String xmlFileName;
        private String conversionMethod;
    }
    
    /**
     * 预览文件响应
     */
    @Data
    public static class PreviewFileResponse {
        private boolean success;
        private String message;
        private String previewData;
        private int roadCount;
        private int intersectionCount;
    }
}

