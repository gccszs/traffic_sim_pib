package com.simeng.pib.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Python服务远程调用客户端
 * @author ChonghaoGao
 * @date 2025/10/16 19:32
 */
@FeignClient(
    name = "python-web-tools",
    url = "${simeng.python-service.url:http://210.41.100.253:8000}",
    configuration = FeignClientConfig.class
)
public interface PythonFeignClient {

    /**
     * 测试Python服务连接
     */
    @GetMapping("/test")
    String testConnection();

    /**
     * 上传文件到Python服务并转换为XML
     * Python服务会自动识别文件格式（TXT/OSM）并转换为XML文件
     *
     * @param txtFile 地图文件（TXT或OSM格式）
     * @return XML文件的字节数组
     */
    @PostMapping(value = "/fileupload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<byte[]> uploadAndConvertFile(@RequestPart("upload_file") MultipartFile txtFile);
}
