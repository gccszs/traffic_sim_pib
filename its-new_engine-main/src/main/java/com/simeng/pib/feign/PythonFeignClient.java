package com.simeng.pib.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author ChonghaoGao
 * @date 2025/10/16 19:32)
 */
@FeignClient(value = "python-web-tools",url = "http://localhost:8000")
public interface PythonFeignClient {
    @GetMapping("/test")
    String TestPyCon();

    @PostMapping(value = "/fileupload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String fileUpload(@RequestPart("txtFile") MultipartFile txtFile);
}
