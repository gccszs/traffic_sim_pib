package edu.uestc.iscssl.itsengine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import edu.uestc.iscssl.common.common.EngineControll;
import edu.uestc.iscssl.common.common.EngineStatusUpdater;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;


@EnableDiscoveryClient
@SpringBootApplication
public class ItsEngineApplication{
    static String InstanceId;
    static Log logger  = LogFactory.getLog(ItsEngineApplication.class);
    static EngineStatusUpdater engineStatusUpdater;
    // Spring Boot应用上下文注入
    @Autowired
    private static DiscoveryClient discoveryClient;
    
    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException, AlreadyBoundException, InterruptedException {
        // 启动Spring Boot应用
        SpringApplication.run(ItsEngineApplication.class, args);
        
        System.setProperty("jna.protected","true");
        InstanceId=args[0];
        logger.info("仿真引擎id:"+InstanceId);
        EngineControll controll=new EngineControllImpl(InstanceId);
        try {
            logger.info("实例绑定id:" + InstanceId);
            Naming.bind(InstanceId,controll);
        } catch (AlreadyBoundException e) {
            logger.info("实例绑定id失败:解绑再绑定" + InstanceId);
            Naming.unbind(InstanceId);
            Naming.bind(InstanceId,controll);
        }
        
        // 注册到引擎管理器
        try {
            // 尝试连接到引擎管理器服务
            engineStatusUpdater=(EngineStatusUpdater)Naming.lookup("rmi://127.0.0.1:1099/engineManagerService");
            registToManager(null);
        } catch (Exception e) {
            logger.warn("无法立即连接到引擎管理器，将在任务开始时重试: " + e.getMessage());
        }
    }
    public static void registToManager(String simulationId) throws RemoteException, NotBoundException, MalformedURLException {
        logger.info("引擎"+InstanceId+"状态可用");
        engineStatusUpdater.afterEngineAvaliable(InstanceId,simulationId);
        logger.info("完成注册");
    }


}
