package com.simeng.pib.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.simeng.pib.model.SimInfo;
import com.simeng.pib.model.dto.ApiResponse;
import com.simeng.pib.pojo.ConversionResult;
import com.simeng.pib.service.MapConversionService;
import com.simeng.pib.service.impl.SessionServiceImpl;
import com.simeng.pib.util.XmlJsonConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地图文件处理控制器
 */
@Slf4j
@RestController
public class MapController {

    @Autowired
    private SessionServiceImpl sessionServiceImpl;

    @Autowired
    private MapConversionService mapConversionService;

    @Autowired
    private XmlJsonConverter xmlJsonConverter;

    @Value("${simeng.cache-dir}")
    private String cacheDir;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".txt", ".xml", ".osm");

    /**
     * 上传路网文件到缓存，并验证路网文件格式
     */
    @PostMapping("/upload_map")
    public ResponseEntity<ApiResponse<String>> uploadMap(
            @CookieValue(value = "id", required = false) String id,
            @RequestParam("file") MultipartFile file) {

        if (id == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_AUTH", "unable to get cookie"));
        }

        if (!sessionServiceImpl.sessionExists(id)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_AUTH", "invalid session"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_FILE", "no file uploaded"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_FILE", "invalid filename"));
        }

        // 检查文件扩展名
        String extension = getFileExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_FILE", "not a permitted file format"));
        }

        try {
            // 创建缓存目录
            Path cachePath = Paths.get(cacheDir, id);
            Files.createDirectories(cachePath);

            // 保存上传的文件
            Path filePath = cachePath.resolve(filename).toAbsolutePath();
            log.info("打印上传文件路径:{}",filePath);
            file.transferTo(filePath.toFile());

            log.info("Uploaded file: {} for session: {}", filename, id);

            // 转换文件格式
            String xmlFilePath = filePath.toString().replaceAll("\\.(txt|osm)$", ".xml");

            ConversionResult result = mapConversionService.convertMapFile(
                filePath.toString(), xmlFilePath,id);

            if (!result.isSuccess()) {
                return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("ERR_CONVERT", result.getMessage()));
            }

            // 更新会话信息
            SimInfo simInfo = sessionServiceImpl.getSessionInfo(id);
            simInfo.setXml_path(xmlFilePath);
            // 设置XML文件名（用于Python服务）
            String xmlFileName = Paths.get(xmlFilePath).getFileName().toString();
            simInfo.setMap_xml_name(xmlFileName);
            sessionServiceImpl.updateSessionInfo(id, simInfo);

            String method = result.getMethod() != null ? result.getMethod() : "old";
            return ResponseEntity.ok(ApiResponse.success("verify map data ok", method));

        } catch (IOException e) {
            log.error("Failed to upload map file", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ERR_FILE", "failed to save file"));
        }
    }

    /**
     * 将路网XML转为JSON
     */
    @GetMapping("/get_map_json")
    public ResponseEntity<ApiResponse<Map<String,Object>>> getMapJson(
            @CookieValue(value = "id", required = false) String id) {

        if (id == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_AUTH", "unable to get cookie"));
        }

        if (!sessionServiceImpl.sessionExists(id)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_AUTH", "invalid session"));
        }

        SimInfo simInfo = sessionServiceImpl.getSessionInfo(id);
        String xmlFilePath = simInfo.getXml_path();

        if (xmlFilePath == null || xmlFilePath.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_FILE", "no map file found"));
        }

        try {
            Path xmlPath = Paths.get(xmlFilePath);
            if (!Files.exists(xmlPath)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ERR_FILE", "map file not found"));
            }

            String xmlContent = Files.readString(xmlPath);
            JsonNode json = XmlJsonConverter.xmlToJson(xmlContent);
            Map<String,Object> data = new HashMap<>();
            if (json == null) {
                return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("ERR_CONVERT", "failed to convert XML to JSON"));
            }
            log.info("{jsonData:}{}",json.isEmpty());
            data.put("Data",json);
            return ResponseEntity.ok(ApiResponse.success("convert xml to json ok",data));

        } catch (IOException e) {
            log.error("Failed to read map file", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ERR_FILE", "failed to read map file"));
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex).toLowerCase();
    }
}
