package com.simeng.pib.service;

import com.simeng.pib.feign.PythonFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 地图转换服务 - 基于OpenFeign远程调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapConversionService {

    private final PythonFeignClient pythonFeignClient;

    @Value("${simeng.python-service.enabled:true}")
    private boolean useFeignClient;

    /**
     * 通过Feign调用Python服务转换文件
     * Python服务接收TXT/OSM文件，返回转换后的XML文件
     */
    private ConversionResult convertViaFeign(String inputFilePath, String outputXmlPath) {
        try {
            log.info("Calling Python service to convert map file: {}", inputFilePath);

            // 读取文件并创建MultipartFile
            Path inputPath = Paths.get(inputFilePath);
            MultipartFile multipartFile = createMultipartFile(inputPath);

            // 调用Python服务进行转换，返回XML文件内容（字节数组）
            ResponseEntity<byte[]> response = pythonFeignClient.uploadAndConvertFile(multipartFile);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 将返回的XML字节数组写入目标文件
                Files.write(Paths.get(outputXmlPath), response.getBody());
                log.info("Map conversion successful via Feign, file saved to: {}", outputXmlPath);
                return new ConversionResult(true, "Map conversion via Feign successful", "feign");
            } else {
                log.error("Map conversion via Feign failed, status: {}", response.getStatusCode());
                return new ConversionResult(false, "Python service returned error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error calling Python service via Feign", e);
            return new ConversionResult(false, "Feign call error: " + e.getMessage());
        }
    }

    /**
     * 创建MultipartFile对象
     */
    private MultipartFile createMultipartFile(Path filePath) throws IOException {
        File file = filePath.toFile();
        byte[] fileContent = Files.readAllBytes(filePath);
        
        return new MultipartFile() {
            @Override
            public String getName() {
                return "txtFile";  // 参数名必须是txtFile，与Python服务接口匹配
            }

            @Override
            public String getOriginalFilename() {
                return file.getName();
            }

            @Override
            public String getContentType() {
                try {
                    return Files.probeContentType(filePath);
                } catch (IOException e) {
                    return "application/octet-stream";
                }
            }

            @Override
            public boolean isEmpty() {
                return fileContent.length == 0;
            }

            @Override
            public long getSize() {
                return fileContent.length;
            }

            @Override
            public byte[] getBytes() {
                return fileContent;
            }

            @Override
            public java.io.InputStream getInputStream() {
                return new java.io.ByteArrayInputStream(fileContent);
            }

            @Override
            public void transferTo(File dest) throws IOException {
                try (FileOutputStream out = new FileOutputStream(dest)) {
                    out.write(fileContent);
                }
            }
        };
    }

    /**
     * 完整的地图文件转换流程
     * 1. 如果是XML文件，直接保存，不调用OpenFeign
     * 2. 如果是TXT/OSM文件，调用OpenFeign的fileupload接口转换
     */
    public ConversionResult convertMapFile(String inputFilePath, String outputXmlPath) {
        Path inputPath = Paths.get(inputFilePath);
        String extension = getFileExtension(inputPath.getFileName().toString());

        try {
            // 如果已经是XML格式，直接复制，不需要调用OpenFeign
            if (".xml".equals(extension)) {
                Files.copy(inputPath, Paths.get(outputXmlPath), StandardCopyOption.REPLACE_EXISTING);
                log.info("File is already XML format, saved directly: {}", outputXmlPath);
                return new ConversionResult(true, "Map file is already in XML format", "direct");
            }

            // 如果是TXT或OSM格式，调用OpenFeign转换
            if (useFeignClient) {
                log.info("File needs conversion, calling OpenFeign: {} -> {}", inputFilePath, outputXmlPath);
                return convertViaFeign(inputFilePath, outputXmlPath);
            } else {
                return new ConversionResult(false, "Python service is disabled in configuration");
            }

        } catch (Exception e) {
            log.error("Map conversion error", e);
            return new ConversionResult(false, "Conversion error: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    /**
     * 转换结果
     */
    public static class ConversionResult {
        private final boolean success;
        private final String message;
        private final String method;

        public ConversionResult(boolean success, String message) {
            this(success, message, null);
        }

        public ConversionResult(boolean success, String message, String method) {
            this.success = success;
            this.message = message;
            this.method = method;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getMethod() {
            return method;
        }
    }
}
