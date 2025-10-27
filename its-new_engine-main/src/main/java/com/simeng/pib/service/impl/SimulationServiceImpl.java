package com.simeng.pib.service.impl;

import com.simeng.pib.feign.PythonFeignClient;
import com.simeng.pib.pojo.CreateSimengRequest;
import com.simeng.pib.util.OdUtils;
import com.simeng.pib.model.SimInfo;
import com.simeng.pib.service.SimulationService;
import com.simeng.pib.util.XmlJsonConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ChonghaoGao
 * @date 2025/9/13 11:14)
 */
@Slf4j
@Service
public class SimulationServiceImpl implements SimulationService {

    @Autowired
    private PythonFeignClient pythonFeignClient;

    public void initSimengByFeign(Map<String, Object> simengInfo, List<Map<String, Object>> controlViews, String id) {
        CreateSimengRequest request = new CreateSimengRequest(simengInfo, controlViews, id);
        log.info(pythonFeignClient.createSimeng(request).toString());
    }

//    public void copyRoadNetFile(SimInfo simInfo, Path simFilesDir) throws IOException {
//        if (simInfo.getXml_path() != null && !simInfo.getXml_path().isEmpty()) {
//            Path roadXmlFile = Paths.get(simInfo.getXml_path());
//            if (Files.exists(roadXmlFile)) {
//                Path targetRoadFile = simFilesDir.resolve(roadXmlFile.getFileName());
//                Files.copy(roadXmlFile, targetRoadFile);
//            }
//        }
//    }
//    /**
//     * 创建OD文件
//     */
//    public void createOdFile(Path simDir, Map<String, Object> simInfoData) throws IOException {
//        Path odXmlFile = simDir.resolve("od.xml");
//
//        // 处理OD数据格式转换
//        Map<String, Object> fixedOd = (Map<String, Object>) simInfoData.get("fixed_od");
//        if (fixedOd != null) {
//            Map<String, Object> convertOdJson = new HashMap<>(fixedOd);
//
//            // 转换OD格式
//            List<Object> odList = (List<Object>) fixedOd.get("od");
//            if (odList != null) {
//                Map<String, Object> correctOriginFmt = new HashMap<>();
//                correctOriginFmt.put("orgin", odList);
//                convertOdJson.put("od", correctOriginFmt);
//            }
//
//            // 转换信号格式
//            List<Object> sgList = (List<Object>) fixedOd.get("sg");
//            if (sgList != null) {
//                Map<String, Object> correctSignalFmt = new HashMap<>();
//                correctSignalFmt.put("signal", sgList);
//                convertOdJson.put("sg", correctSignalFmt);
//            }
//
//            // 转换为XML
//            String odContent = XmlJsonConverter.jsonToXml(convertOdJson, "Data");
//            if (odContent != null) {
//                // 删除XML声明行
//                if (odContent.startsWith("<?xml")) {
//                    String[] lines = odContent.split("\n");
//                    if (lines.length > 1) {
//                        odContent = String.join("\n", Arrays.copyOfRange(lines, 1, lines.length));
//                    }
//                }
//
//                // 字段名替换
//                odContent = OdUtils.replaceOdFieldNames(odContent);
//                log.info("创建OD{}", odContent);
//                Files.writeString(odXmlFile, odContent, StandardCharsets.UTF_8);
//            }
//        } else {
//            // 创建空的OD文件
//            Files.createFile(odXmlFile);
//        }
//    }
}
