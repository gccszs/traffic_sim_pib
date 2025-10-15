package com.simeng.pib.config;

import com.simeng.pib.service.PluginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 应用启动时的初始化配置
 */
@Slf4j
@Component
public class ApplicationStartupConfig implements CommandLineRunner {

    @Autowired
    private PluginService pluginService;
    
    @Value("${simeng.cache-dir}")
    private String cacheDir;
    
    @Value("${simeng.plugin-dir}")
    private String pluginDir;
    
    @Value("${simeng.simeng-dir}")
    private String simengDir;

    @Value("${frontend.ip}")
    private String frontEndIp;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting SimEngPIB application initialization...");
        
        // 创建必要的目录
        createDirectories();
        
        // 初始化插件系统
        initializePlugins();
        
        log.info("SimEngPIB application initialization completed successfully!");
        log.info("frontend is running on {}",frontEndIp);
    }
    
    private void createDirectories() throws Exception {
        Path[] directories = {
            Paths.get(cacheDir),
            Paths.get(pluginDir),
            Paths.get(simengDir)
        };
        
        for (Path dir : directories) {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                log.info("Created directory: {}", dir.toAbsolutePath());
            } else {
                log.debug("Directory already exists: {}", dir.toAbsolutePath());
            }
        }
    }
    
    private void initializePlugins() {
        try {
            pluginService.initPluginInfo();
            log.info("Plugin system initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize plugin system", e);
            // 不抛出异常，让应用继续启动
        }
    }
}
