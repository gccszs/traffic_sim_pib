package edu.uestc.iscssl.common.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 常量配置类
 * 使用@Configuration和@Value注解从配置文件中读取配置，避免硬编码绝对路径
 */
@Configuration
public class Constant {

    /**
     * kafka ip地址
     */
    @Value("${kafka.address:localhost:9092}")
    private String kafkaAddress;

    /**
     * dll文件地址
     * 配置为相对路径，相对于应用部署目录
     */
    @Value("${engine.dll.path:./engineDll/SimEngine}")
    private String dllPath;

    /**
     * JDK路径
     * 配置为相对路径，相对于应用部署目录
     */
    @Value("${java.path:./jdk}")
    private String jdkPath;

    /**
     * engine.jar 地址
     * 配置为相对路径，相对于应用部署目录
     */
    @Value("${engine.jar.path:./its-engine.jar}")
    private String jarPath;

    // 提供getter方法以访问这些配置值
    public String getKafkaAddress() {
        return kafkaAddress;
    }

    public String getDllPath() {
        return dllPath;
    }

    public String getJdkPath() {
        return jdkPath;
    }

    public String getJarPath() {
        return jarPath;
    }
}
