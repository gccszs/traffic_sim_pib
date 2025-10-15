package com.simeng.pib.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ChonghaoGao
 * @date 2025/9/12 10:22)
 */
@ConfigurationProperties(prefix = "simeng.python")
@Configuration
@Data
public class PythonEnvConfig {
    private String pyenvPath;
    private String pythonVersion;
}
