package com.simeng.pib.controller;

import com.simeng.pib.model.dto.ApiResponse;
import com.simeng.pib.model.dto.PluginInfo;
import com.simeng.pib.service.PluginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 插件管理控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PluginController {
    
    private final PluginService pluginService;
    
    @Value("${simeng.plugin-dir}")
    private String pluginDir;
    
    /**
     * 上传插件ZIP包
     */
    @PostMapping("/upload_plugin")
    public ResponseEntity<ApiResponse<String>> uploadPlugin(@RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_FILE", "no file uploaded"));
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (!"application/zip".equals(contentType) && !"application/x-zip-compressed".equals(contentType)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_FILE", "not a zip file"));
        }
        
        try {
            // 验证ZIP文件结构
            String pluginName = validateAndExtractPlugin(file);
            if (pluginName == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ERR_CONTENT", "not find or find more manifest file"));
            }
            
            // 检查是否存在同名插件
            Path pluginPath = Paths.get(pluginDir, pluginName);
            if (Files.exists(pluginPath)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ERR_EXIST", "a plugin folder with the same name already exists"));
            }
            
            // 解压插件
            extractPlugin(file, pluginPath);
            
            // 添加到插件管理
            boolean added = pluginService.addPlugin(pluginName);
            if (added) {
                log.info("Successfully uploaded plugin: {}", pluginName);
                return ResponseEntity.ok(ApiResponse.success("upload plugin ok"));
            } else {
                return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("ERR_PLUGIN", "failed to load plugin"));
            }
            
        } catch (IOException e) {
            log.error("Failed to upload plugin", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ERR_FILE", "failed to process plugin file"));
        }
    }
    
    /**
     * 删除插件
     */
    @GetMapping("/del_plugin/")
    public ResponseEntity<ApiResponse<String>> deletePlugin(@RequestParam(required = false) String name) {
        
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_ARG", "no plugin name"));
        }
        
        boolean deleted = pluginService.deletePlugin(name);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("del ok"));
        } else {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_FILE", "can't find the plugin dir"));
        }
    }
    
    /**
     * 获取插件信息
     */
    @GetMapping("/get_plugin_info/")
    public ResponseEntity<ApiResponse<List<PluginInfo>>> getPluginInfo(
            @RequestParam(required = false) String name) {
        
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_ARG", "no plugin name"));
        }
        
        List<PluginInfo> pluginInfos;
        
        if ("all".equals(name)) {
            pluginInfos = pluginService.getAllPlugins();
        } else {
            PluginInfo pluginInfo = pluginService.getPlugin(name);
            if (pluginInfo != null) {
                pluginInfos = List.of(pluginInfo);
            } else {
                pluginInfos = List.of();
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success("get plugin info ok", pluginInfos));
    }
    
    /**
     * 更新插件信息
     */
    @PostMapping("/update_plugin_info")
    public ResponseEntity<ApiResponse<String>> updatePluginInfo(@RequestBody Map<String, Object> updateData) {
        
        String operation = (String) updateData.get("ope");
        boolean applyToDisk = "save".equals(operation);
        
        Map<String, Object> pluginInfo = (Map<String, Object>) updateData.get("info");
        if (pluginInfo == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_ARG", "missing plugin info"));
        }
        
        String pluginName = (String) pluginInfo.get("pluginName");
        if (pluginName == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_ARG", "missing plugin name"));
        }
        
        // 转换更新信息格式
        List<Map<String, Object>> updateInfos = convertUpdateInfos(pluginInfo);
        
        boolean updated = pluginService.updatePluginInfo(pluginName, updateInfos, applyToDisk);
        if (updated) {
            return ResponseEntity.ok(ApiResponse.success("update plugin info success"));
        } else {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ERR_UPDATE", "update plugin info failed"));
        }
    }
    
    /**
     * 获取插件代码内容
     */
    @GetMapping("/get_plugin_code/")
    public ResponseEntity<ApiResponse<String>> getPluginCode(
            @CookieValue(value = "id", required = false) String id,
            @RequestParam(required = false) String name) {
        
        if (id == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_AUTH", "unable to get id"));
        }
        
        if (name == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_ARG", "no name arg"));
        }
        
        String code = pluginService.getPluginCode(name);
        if (code != null) {
            return ResponseEntity.ok(ApiResponse.success("get plugin code ok", code));
        } else {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_FILE", "plugin code not found"));
        }
    }
    
    /**
     * 验证并提取插件名称
     */
    private String validateAndExtractPlugin(MultipartFile file) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            String pluginName = null;
            
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // 检查根目录下的JSON文件
                if (!entryName.contains("/") && entryName.endsWith(".json")) {
                    if (pluginName != null) {
                        // 找到多个JSON文件
                        return null;
                    }
                    pluginName = entryName.substring(0, entryName.length() - 5);
                }
            }
            
            return pluginName;
        }
    }
    
    /**
     * 解压插件到指定目录
     */
    private void extractPlugin(MultipartFile file, Path targetPath) throws IOException {
        Files.createDirectories(targetPath);
        
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetPath.resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath);
                }
            }
        }
    }
    
    /**
     * 转换更新信息格式
     */
    private List<Map<String, Object>> convertUpdateInfos(Map<String, Object> pluginInfo) {
        // 这里简化处理，实际应该按照原Python代码的逻辑进行转换
        // 原代码中有复杂的格式转换逻辑
        return List.of();
    }
}
