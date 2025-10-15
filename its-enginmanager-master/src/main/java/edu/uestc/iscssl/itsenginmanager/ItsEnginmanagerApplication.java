package edu.uestc.iscssl.itsenginmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import edu.uestc.iscssl.common.common.EngineManagerStatusUpdater;
import edu.uestc.iscssl.itsenginmanager.v2.EngineService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

@EnableDiscoveryClient
@SpringBootApplication(exclude= {DataSourceAutoConfiguration.class})
public class ItsEnginmanagerApplication {
    public static void main(String[] args) throws SocketException {
        // 启动Spring Boot应用
        SpringApplication.run(ItsEnginmanagerApplication.class, args);
        
        Log logger = LogFactory.getLog(ItsEnginmanagerApplication.class);
        String rmiRegistryAddr = "127.0.0.1";
        String instanceId = UUID.randomUUID().toString(); // 使用UUID生成唯一实例ID
        
        // 初始化RMI服务
        try {
            // 创建并注册RMI注册表
            LocateRegistry.createRegistry(1099);
            logger.info("RMI注册表创建成功，端口: 1099");
            
            // 初始化引擎服务
            EngineService engineService = new EngineService(rmiRegistryAddr);
            engineService.init();
            
            // 创建并绑定引擎管理器服务
            EngineManagerService engineManagerService = new EngineManagerService(engineService, instanceId);
            
            try {
                // 尝试绑定服务
                Naming.bind("engineManagerService", engineManagerService);
                logger.info("引擎管理器服务绑定成功: engineManagerService");
            } catch (AlreadyBoundException e) {
                // 如果已绑定，则先解绑再重新绑定
                logger.warn("引擎管理器服务已存在，尝试重新绑定");
                Naming.unbind("engineManagerService");
                Naming.bind("engineManagerService", engineManagerService);
                logger.info("引擎管理器服务重新绑定成功");
            }
            
            logger.info("RMI服务注册完成: " + rmiRegistryAddr + ":1099");
            
            // 尝试连接到服务器状态更新器（可选）
            try {
                EngineManagerStatusUpdater engineManagerStatusUpdater = 
                    (EngineManagerStatusUpdater) Naming.lookup("rmi://" + rmiRegistryAddr + ":1099/engineManagerUpdater");
                engineManagerService.setUpdater(engineManagerStatusUpdater);
                
                // 向服务器更新本节点信息
                engineManagerStatusUpdater.onEnginManagerAvaliable(instanceId, getLocalIpAddr());
                logger.info("完成向服务器更新本节点信息，实例ID: " + instanceId);
            } catch (NotBoundException | RemoteException e) {
                logger.warn("无法连接到服务器状态更新器，将在可用时重试: " + e.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("初始化引擎管理器失败: " + e.getMessage(), e);
        }
    }
    private static List<String> getLocalIpAddr() throws SocketException {
        Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
        InetAddress ip;
        List<String> result=new ArrayList<>();
        while (allNetInterfaces.hasMoreElements())
        {
            NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
            Enumeration addresses = netInterface.getInetAddresses();
            while (addresses.hasMoreElements())
            {
                ip = (InetAddress) addresses.nextElement();
                if (ip != null && ip instanceof Inet4Address)
                {
                   result.add(ip.getHostAddress());
                }
            }

        }
        return result;
    }
}
