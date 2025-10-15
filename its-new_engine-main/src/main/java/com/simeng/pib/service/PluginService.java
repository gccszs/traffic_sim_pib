package com.simeng.pib.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simeng.pib.model.dto.PluginInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {
    
    private final ObjectMapper objectMapper;
    
    @Value("${simeng.plugin-dir}")
    private String pluginDir;
    
    /**
     * 插件信息缓存
     */
    private final Map<String, Map<String, Object>> pluginInfoCache = new ConcurrentHashMap<>();
    
    /**
     * 初始化插件信息
     */
    public void initPluginInfo() {
        try {
            Path pluginPath = Paths.get(pluginDir);
            if (!Files.exists(pluginPath)) {
                Files.createDirectories(pluginPath);
                log.info("Created plugin directory: {}", pluginDir);
                return;
            }
            
            pluginInfoCache.clear();
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginPath, Files::isDirectory)) {
                for (Path pluginSubDir : stream) {
                    String pluginName = pluginSubDir.getFileName().toString();
                    Path manifestFile = pluginSubDir.resolve(pluginName + ".json");
                    
                    if (Files.exists(manifestFile)) {
                        try {
                            Map<String, Object> manifest = objectMapper.readValue(
                                manifestFile.toFile(), Map.class);
                            pluginInfoCache.put(pluginName, manifest);
                            log.debug("Loaded plugin: {}", pluginName);
                        } catch (IOException e) {
                            log.error("Failed to load plugin manifest: {}", manifestFile, e);
                        }
                    }
                }
            }
            
            log.info("Initialized {} plugins", pluginInfoCache.size());
            
        } catch (IOException e) {
            log.error("Failed to initialize plugin info", e);
        }
    }
    
    /**
     * 获取所有插件信息
     */
    public List<PluginInfo> getAllPlugins() {
        List<PluginInfo> plugins = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : pluginInfoCache.entrySet()) {
            plugins.add(new PluginInfo(entry.getKey(), entry.getValue()));
        }
        return plugins;
    }
    
    /**
     * 获取指定插件信息
     */
    public PluginInfo getPlugin(String pluginName) {
        Map<String, Object> manifest = pluginInfoCache.get(pluginName);
        if (manifest != null) {
            return new PluginInfo(pluginName, manifest);
        }
        return null;
    }
    
    /**
     * 添加插件
     */
    public boolean addPlugin(String pluginName) {
        try {
            Path pluginPath = Paths.get(pluginDir, pluginName);
            Path manifestFile = pluginPath.resolve(pluginName + ".json");
            
            if (Files.exists(manifestFile)) {
                Map<String, Object> manifest = objectMapper.readValue(
                    manifestFile.toFile(), Map.class);
                pluginInfoCache.put(pluginName, manifest);
                log.info("Added plugin: {}", pluginName);
                return true;
            } else {
                log.warn("Plugin manifest not found: {}", manifestFile);
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to add plugin: {}", pluginName, e);
            return false;
        }
    }
    
    /**
     * 删除插件
     */
    public boolean deletePlugin(String pluginName) {
        try {
            Path pluginPath = Paths.get(pluginDir, pluginName);
            if (Files.exists(pluginPath)) {
                FileUtils.deleteDirectory(pluginPath.toFile());
                pluginInfoCache.remove(pluginName);
                log.info("Deleted plugin: {}", pluginName);
                return true;
            } else {
                log.warn("Plugin directory not found: {}", pluginPath);
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to delete plugin: {}", pluginName, e);
            return false;
        }
    }
    
    /**
     * 更新插件信息
     */
    public boolean updatePluginInfo(String pluginName, List<Map<String, Object>> updateInfos, boolean applyToDisk) {
        Map<String, Object> manifest = pluginInfoCache.get(pluginName);
        if (manifest == null) {
            log.warn("Plugin not found: {}", pluginName);
            return false;
        }
        
        try {
            // 更新内存中的配置
            for (Map<String, Object> updateInfo : updateInfos) {
                String type = (String) updateInfo.get("type");
                
                if ("enable_main".equals(type)) {
                    manifest.put("enable_main", updateInfo.get("enable"));
                } else {
                    // 更新control配置
                    Map<String, Object> control = (Map<String, Object>) manifest.get("control");
                    if (control != null && control.containsKey(type)) {
                        Map<String, Object> controlItem = (Map<String, Object>) control.get(type);
                        if (updateInfo.containsKey("frequency")) {
                            controlItem.put("frequency", updateInfo.get("frequency"));
                        }
                        if (updateInfo.containsKey("enable")) {
                            controlItem.put("enable", updateInfo.get("enable"));
                        }
                    }
                }
            }
            
            // 如果需要，保存到磁盘
            if (applyToDisk) {
                Path manifestFile = Paths.get(pluginDir, pluginName, pluginName + ".json");
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(manifestFile.toFile(), manifest);
                log.debug("Updated plugin manifest on disk: {}", manifestFile);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to update plugin info: {}", pluginName, e);
            return false;
        }
    }
    
    /**
     * 复制插件到指定目录
     */
    public boolean copyPlugins(List<String> pluginNames, String targetDir) {
        try {
            Path targetPath = Paths.get(targetDir);
            Files.createDirectories(targetPath);
            
            for (String pluginName : pluginNames) {
                Path sourcePath = Paths.get(pluginDir, pluginName);
                Path destPath = targetPath.resolve(pluginName);
                
                if (Files.exists(sourcePath)) {
                    FileUtils.copyDirectory(sourcePath.toFile(), destPath.toFile());
                    log.debug("Copied plugin {} to {}", pluginName, destPath);
                } else {
                    log.warn("Plugin source not found: {}", sourcePath);
                    return false;
                }
            }
            
            return true;
            
        } catch (IOException e) {
            log.error("Failed to copy plugins", e);
            return false;
        }
    }
    
    /**
     * 获取插件代码内容
     */
    public String getPluginCode(String pluginName) {
        try {
            Map<String, Object> manifest = pluginInfoCache.get(pluginName);
            if (manifest == null) {
                return null;
            }
            
            String codeFile = (String) manifest.get("file");
            if (codeFile == null) {
                return null;
            }
            
            Path codePath = Paths.get(pluginDir, pluginName, codeFile);
            if (Files.exists(codePath)) {
                return Files.readString(codePath);
            }
            
        } catch (IOException e) {
            log.error("Failed to read plugin code: {}", pluginName, e);
        }
        
        return null;
    }
}
