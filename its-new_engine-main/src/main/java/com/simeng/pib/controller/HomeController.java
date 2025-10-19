package com.simeng.pib.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 主页控制器
 */
@Slf4j
@Controller
public class HomeController {

    /**
     * 首页
     */
    @GetMapping("/")
    public ResponseEntity<String> index() {
        try {
            // 尝试读取前端构建的index.html
            Path frontendIndex = Paths.get("frontend", "index.html");
            log.info(frontendIndex.toString());
            if (Files.exists(frontendIndex)) {
                String content = Files.readString(frontendIndex);
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
            } else {
                // 返回默认页面
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(getDefaultIndexHtml());
            }
        } catch (IOException e) {
            log.error("Failed to read index.html", e);
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("<html><body><h1>SimEngPIB Backend</h1><p>Frontend not found</p></body></html>");
        }
    }

    /**
     * Favicon
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Resource> favicon() {
        try {
            Path faviconPath = Paths.get("frontend", "favicon.ico");
            if (Files.exists(faviconPath)) {
                Resource resource = new org.springframework.core.io.FileSystemResource(faviconPath.toFile());
                return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("image/x-icon"))
                    .body(resource);
            } else {
                // 返回默认favicon或404
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Failed to serve favicon", e);
            return ResponseEntity.notFound().build();
        }
    }
    /*
    * 如果前端没有上传界面就默认为其返回一个临时生成的后端接口界面 等待修改
    * */
    private String getDefaultIndexHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SimEngPIB - Traffic Simulation Backend</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        max-width: 800px;
                        margin: 50px auto;
                        padding: 20px;
                        background-color: #f5f5f5;
                    }
                    .container {
                        background: white;
                        padding: 30px;
                        border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    h1 { color: #333; }
                    .status { color: #28a745; font-weight: bold; }
                    .api-list {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 5px;
                        margin-top: 20px;
                    }
                    .api-item {
                        margin: 5px 0;
                        font-family: monospace;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🚦 SimEngPIB Backend</h1>
                    <p class="status">✅ Server is running successfully!</p>
                    
                    <h2>Available APIs:</h2>
                    <div class="api-list">
                        <div class="api-item">GET /cookie_id - Create session ID</div>
                        <div class="api-item">POST /upload_map - Upload map file</div>
                        <div class="api-item">GET /get_map_json - Get map as JSON</div>
                        <div class="api-item">POST /upload_plugin - Upload plugin</div>
                        <div class="api-item">GET /get_plugin_info/ - Get plugin info</div>
                        <div class="api-item">POST /create_simeng - Create simulation engine</div>
                        <div class="api-item">WS /ws/frontend - Frontend WebSocket</div>
                        <div class="api-item">WS /ws/exe/{id} - Engine WebSocket</div>
                    </div>
                    
                    <p><strong>Note:</strong> Place your frontend build files in the <code>frontend/</code> directory to serve the full application.</p>
                </div>
            </body>
            </html>
            """;
    }
}
