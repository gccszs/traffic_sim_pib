package com.simeng.pib.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ChonghaoGao
 * @date 2025/9/11 23:31)
 */

@Configuration
@ConfigurationProperties(prefix = "simeng.cors.allowed-origins")
@Data
public class CorsProperties {
  private List<String> allowedOrigins;
}
