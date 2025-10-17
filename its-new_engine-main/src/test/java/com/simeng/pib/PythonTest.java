package com.simeng.pib;

import com.simeng.pib.feign.PythonFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
   }

}
