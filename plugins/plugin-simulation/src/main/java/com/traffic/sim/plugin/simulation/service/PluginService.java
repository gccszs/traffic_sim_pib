package com.traffic.sim.plugin.simulation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.traffic.sim.plugin.simulation.grpc.proto.*;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * 引擎插件服务
 * 通过 gRPC 调用 Python 服务管理引擎插件
 * 
 * @author traffic-sim
 */
@Service
@Slf4j
public class PluginService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${grpc.client.python-service.enabled:true}")
    private boolean grpcEnabled;
    
    // 使用 @Lazy 延迟初始化，避免启动时失败
    @Lazy
    @GrpcClient("python-service")
    private PythonServiceGrpc.PythonServiceBlockingStub blockingStub;
    
    @PostConstruct
    public void init() {
        log.info("PluginService initialized, grpcEnabled={}", grpcEnabled);
    }
    
    /**
     * 检查gRPC是否可用
     */
    private boolean isGrpcAvailable() {
        if (!grpcEnabled) {
            return false;
        }
        try {
            return blockingStub != null;
        } catch (Exception e) {
            log.debug("gRPC client not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取所有插件信息
     * 
     * @return 插件信息列表
     */
    public Map<String, Object> getPluginInfo() {
        return getPluginInfo(null);
    }
    
    /**
     * 获取指定插件信息
     * 
     * @param pluginName 插件名称（可选，为空获取全部）
     * @return 插件信息
     */
    public Map<String, Object> getPluginInfo(String pluginName) {
        log.info("getPluginInfo called, pluginName={}, grpcEnabled={}", pluginName, grpcEnabled);
        if (!isGrpcAvailable()) {
            log.warn("gRPC service is not available, returning empty plugin list");
            return Map.of(
                "res", "ERR_OK",
                "msg", "gRPC service not available, returning empty plugin list",
                "addition", Collections.emptyList()
            );
        }
        
        try {
            GetPluginInfoRequest.Builder requestBuilder = GetPluginInfoRequest.newBuilder();
            if (pluginName != null && !pluginName.isEmpty()) {
                requestBuilder.setPluginName(pluginName);
            }
            
            GetPluginInfoResponse response = blockingStub.getPluginInfo(requestBuilder.build());
            
            // 转换响应 - 使用新格式: plugin_name + manifest
            List<Map<String, Object>> additionList = new ArrayList<>();
            for (PluginInfo plugin : response.getPluginsList()) {
                Map<String, Object> pluginMap = new LinkedHashMap<>();
                pluginMap.put("plugin_name", plugin.getName());
                // 解析 JSON 字符串为 Map
                try {
                    Map<String, Object> manifest = objectMapper.readValue(
                        plugin.getManifestJson(), 
                        new TypeReference<Map<String, Object>>() {}
                    );
                    pluginMap.put("manifest", manifest);
                } catch (Exception e) {
                    pluginMap.put("manifest", plugin.getManifestJson());
                }
                additionList.add(pluginMap);
            }
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("res", response.getRes());
            result.put("msg", response.getMsg());
            result.put("addition", additionList);
            return result;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for getPluginInfo", e);
            return Map.of(
                "res", "ERR_FAIL",
                "msg", "gRPC error: " + e.getStatus().getCode()
            );
        } catch (Exception e) {
            log.error("Error getting plugin info", e);
            return Map.of(
                "res", "ERR_FAIL",
                "msg", e.getMessage()
            );
        }
    }
    
    /**
     * 上传插件
     * 
     * @param file 插件 zip 文件
     * @return 上传结果
     */
    public Map<String, Object> uploadPlugin(MultipartFile file) throws IOException {
        if (!isGrpcAvailable()) {
            log.warn("gRPC service is not available, cannot upload plugin");
            return Map.of(
                "res", "ERR_FAIL",
                "msg", "gRPC service not available"
            );
        }
        
        try {
            UploadPluginRequest request = UploadPluginRequest.newBuilder()
                .setFileName(file.getOriginalFilename())
                .setFileContent(ByteString.copyFrom(file.getBytes()))
                .build();
            
            ApiResponse response = blockingStub.uploadPlugin(request);
            
            return Map.of(
                "res", response.getRes(),
                "msg", response.getMsg(),
                "data", response.getData()
            );
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for uploadPlugin", e);
            return Map.of(
                "res", "ERR_FAIL",
                "msg", "gRPC error: " + e.getStatus().getCode()
            );
        } catch (Exception e) {
            log.error("Error uploading plugin", e);
            return Map.of(
                "res", "ERR_FAIL",
                "msg", e.getMessage()
            );
        }
    }
    
    /**
     * 更新插件信息
     * 
     * @param pluginName 插件名称
     * @param updateInfosJson 更新内容JSON字符串
     * @param applyDisk 是否写入磁盘
     * @return 更新结果
     */
    public Map<String, Object> updatePluginInfo(String pluginName, 
                                                 String updateInfosJson,
                                                 boolean applyDisk) {
        if (!isGrpcAvailable()) {
            log.warn("gRPC service is not available, cannot update plugin info");
            return Map.of(
                "res", "ERR_FAIL",
                "msg", "gRPC service not available"
            );
        }
        
        try {
            UpdatePluginInfoRequest request = UpdatePluginInfoRequest.newBuilder()
                .setPluginName(pluginName != null ? pluginName : "")
                .setUpdateInfosJson(updateInfosJson != null ? updateInfosJson : "[]")
                .setApplyDisk(applyDisk)
                .build();
            
            ApiResponse response = blockingStub.updatePluginInfo(request);
            
            return Map.of(
                "res", response.getRes(),
                "msg", response.getMsg()
            );
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for updatePluginInfo", e);
            return Map.of(
                "res", "ERR_FAIL",
                "msg", "gRPC error: " + e.getStatus().getCode()
            );
        } catch (Exception e) {
            log.error("Error updating plugin info", e);
            return Map.of(
                "res", "ERR_FAIL",
                "msg", e.getMessage()
            );
        }
    }

    /**
     * 更新插件信息
     * 
     * @param pluginName 插件名称
     * @param updateInfos 更新内容
     * @param applyDisk 是否写入磁盘
     * @return 更新结果
     */
    public Map<String, Object> updatePluginInfo(String pluginName, 
                                                 List<Map<String, Object>> updateInfos,
                                                 boolean applyDisk) {
        try {
            // 将 updateInfos 转换为 JSON 字符串，调用重载方法
            String updateInfosJson = objectMapper.writeValueAsString(updateInfos);
            return updatePluginInfo(pluginName, updateInfosJson, applyDisk);
        } catch (Exception e) {
            log.error("Error converting updateInfos to JSON", e);
            return Map.of(
                "res", "ERR_FAIL",
                "msg", "Failed to serialize updateInfos: " + e.getMessage()
            );
        }
    }
}
