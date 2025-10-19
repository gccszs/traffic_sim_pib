package com.simeng.pib.service;

import com.simeng.pib.pojo.ConversionResult;
import com.simeng.pib.service.impl.MapConversionServiceImpl;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface MapConversionService {
    ConversionResult convertViaFeign(String inputFilePath, String outputXmlPath);

    MultipartFile createMultipartFile(Path filePath) throws IOException;

    ConversionResult convertMapFile(String inputFilePath, String outputXmlPath);
}
