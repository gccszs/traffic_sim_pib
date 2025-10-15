package com.simeng.pib.service;

import com.simeng.pib.util.PythonScriptExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * 地图转换服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapConversionService {
    
    private final PythonScriptExecutor pythonExecutor;
    
    @Value("${simeng.python-scripts.mapmaker}")
    private String mapmakerScript;
    
    @Value("${simeng.python-scripts.mapmaker-new}")
    private String mapmakerNewScript;
    
    @Value("${simeng.python-scripts.osmtrans}")
    private String osmtransScript;
    
    /**
     * OSM格式转TXT格式
     */
    public ConversionResult osmToTxt(String osmFilePath, String txtFilePath) {
        log.debug("Converting OSM to TXT: {} -> {}", osmFilePath, txtFilePath);
        
        PythonScriptExecutor.ScriptExecutionResult result = pythonExecutor.executePythonScript(
            osmtransScript,
            Arrays.asList(osmFilePath, txtFilePath),
            300 // 5分钟超时
        );
        
        if (result.isSuccess()) {
            // 检查输出文件是否存在
            if (Files.exists(Paths.get(txtFilePath))) {
                return new ConversionResult(true, "OSM to TXT conversion successful");
            } else {
                return new ConversionResult(false, "TXT file was not created");
            }
        } else {
            log.error("OSM to TXT conversion failed: {}", result.getMessage());
            return new ConversionResult(false, result.getMessage());
        }
    }
    
    /**
     * TXT格式转XML格式（旧版本）
     */
    public ConversionResult txtToXml(String txtFilePath, String xmlFilePath) {
        log.debug("Converting TXT to XML (old): {} -> {}", txtFilePath, xmlFilePath);
        
        PythonScriptExecutor.ScriptExecutionResult result = pythonExecutor.executePythonScript(
            mapmakerScript,
            Arrays.asList(txtFilePath, xmlFilePath),
            180 // 3分钟超时
        );
        
        if (result.isSuccess()) {
            if (Files.exists(Paths.get(xmlFilePath))) {
                return new ConversionResult(true, "TXT to XML conversion successful");
            } else {
                return new ConversionResult(false, "XML file was not created");
            }
        } else {
            log.warn("TXT to XML (old) conversion failed: {}", result.getMessage());
            return new ConversionResult(false, result.getMessage());
        }
    }
    
    /**
     * TXT格式转XML格式（新版本）
     */
    public ConversionResult txtToXmlNew(String txtFilePath, String xmlFilePath) {
        log.debug("Converting TXT to XML (new): {} -> {}", txtFilePath, xmlFilePath);
        
        PythonScriptExecutor.ScriptExecutionResult result = pythonExecutor.executePythonScript(
            mapmakerNewScript,
            Arrays.asList(txtFilePath, xmlFilePath),
            180 // 3分钟超时
        );
        
        if (result.isSuccess()) {
            if (Files.exists(Paths.get(xmlFilePath))) {
                return new ConversionResult(true, "TXT to XML (new) conversion successful");
            } else {
                return new ConversionResult(false, "XML file was not created");
            }
        } else {
            log.error("TXT to XML (new) conversion failed: {}", result.getMessage());
            return new ConversionResult(false, result.getMessage());
        }
    }
    
    /**
     * 完整的地图文件转换流程
     */
    public ConversionResult convertMapFile(String inputFilePath, String outputXmlPath) {
        Path inputPath = Paths.get(inputFilePath);
        String extension = getFileExtension(inputPath.getFileName().toString());


        try {
            String txtFilePath = null;
            String xmlFilePath = outputXmlPath;
            String conversionMethod = "old";
            
            // 第一步：转换为TXT格式（如果需要）
            if (".osm".equals(extension)) {
                txtFilePath = inputFilePath.replace(".osm", ".txt");
                ConversionResult osmResult = osmToTxt(inputFilePath, txtFilePath);
                if (!osmResult.isSuccess()) {
                    return osmResult;
                }
            } else if (".txt".equals(extension)) {
                txtFilePath = inputFilePath;
            }
            
            // 第二步：TXT转XML
            if (!".xml".equals(extension)) {
                // 先尝试旧版本转换
                ConversionResult xmlResult = txtToXml(txtFilePath, xmlFilePath);
                conversionMethod = "old";
                if (!xmlResult.isSuccess()) {
                    // 新版本失败，尝试旧版本
                    log.info("new converter failed, trying old converter");
                    xmlResult = txtToXmlNew(txtFilePath, xmlFilePath);
                    conversionMethod = "new";
                }
                
                if (!xmlResult.isSuccess()) {
                    return xmlResult;
                }
            }
            
            return new ConversionResult(true, "Map conversion completed successfully", conversionMethod);
            
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
