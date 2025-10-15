package edu.uestc.iscssl.itsbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
//import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class ItsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItsBackendApplication.class, args);
    }

}
