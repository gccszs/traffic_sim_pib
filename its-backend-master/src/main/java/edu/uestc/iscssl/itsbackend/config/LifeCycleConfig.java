package edu.uestc.iscssl.itsbackend.config;

import edu.uestc.iscssl.common.common.Constant;
import edu.uestc.iscssl.common.common.EngineManagerStatusUpdater;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Logger;

@Configuration
public class LifeCycleConfig {
    Log logger= LogFactory.getLog(LifeCycleConfig.class);
    @Autowired
    EngineManagerStatusUpdater engineManagerStatusService;
    @Bean
    public CommandLineRunner rmiServerStart(){
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                LocateRegistry.createRegistry(1099);
                Naming.bind("engineManagerUpdater",engineManagerStatusService);
                logger.info("rmi服务器启动成功:" + "127.0.0.1:" + 1099);
            }
        };
    }
}
