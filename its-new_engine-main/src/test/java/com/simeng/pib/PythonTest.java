package com.simeng.pib;

import com.simeng.pib.feign.PythonFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.*;

/**
 * @author ChonghaoGao
 * @date 2025/9/12 10:25)
 */
@Slf4j
@SpringBootTest
public class PythonTest {

    @Autowired
    PythonFeignClient pythonFeignClient;

    @Test
    public void testFeign(){
      log.info("接收到python那边{}",pythonFeignClient.TestPyCon());
      MultipartFile txtFile = new MockMultipartFile("测试文件.txt","原始文件名称.txt",
              "text/plain","羔子的测试以及小手段".getBytes());
      log.info(pythonFeignClient.fileUpload(txtFile));
   }

}
