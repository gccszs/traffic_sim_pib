package com.simeng.pib.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author ChonghaoGao
 * @date 2025/10/16 19:32)
 */
@FeignClient(value = "python-web-tools",url = "http://localhost:8000")
public interface PythonFeignClient {
    @GetMapping("/test")
    String TestPyCon();
}
