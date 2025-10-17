package com.simeng.pib;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class SimEngPibApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimEngPibApplication.class, args);
    }
}
