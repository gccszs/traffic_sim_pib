package edu.uestc.iscssl.itsengine;

import edu.uestc.iscssl.common.common.Constant;
import edu.uestc.iscssl.common.common.EngineControll;
import edu.uestc.iscssl.common.common.SimulationTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;

@Component
public class EngineControllImpl extends UnicastRemoteObject implements EngineControll {
    Log logger;
    EngineControllable controllable;
    String simId;
    String instanceId;
    
    // 从应用上下文中获取Constant实例
    private static Constant constant;
    
    @Autowired
    public void setConstant(Constant constant) {
        EngineControllImpl.constant = constant;
    }
    
    protected EngineControllImpl(String instanceId) throws RemoteException {
       logger = LogFactory.getLog(EngineControllImpl.class);
        // 使用EngineManagerFactory获取实例
        controllable = EngineManagerFactory.getInstance();
        this.instanceId = instanceId;
    }

    @Override
    public int start(SimulationTask task) throws RemoteException {
        logger.info("接收到仿真请求：simid:"+task.getSimulationId());
        this.simId=task.getSimulationId();
//        C:\Users\zhenghaowen\IdeaProjects\its-engine\src\engineDll\192.168.1.136
        new Thread(()->{
            try {
                String kafkaAddress = constant != null ? constant.getKafkaAddress() : "localhost:9092";
                logger.info("kafka.address:" + kafkaAddress);
                controllable.SimInit(task.getWebParam().getFilePath(), task.getSimulationId(), kafkaAddress, Class2Structures.transform(task.getWebParam()));
            }catch (Throwable throwable){
                logger.error("初始化仿真失败", throwable);
            }
        }).start();
        new Thread(()->{
                while(controllable.BlockThreadSt()!=1){
                }
                logger.info("system init finished");
                controllable.SetSimStep(task.getStep());
        }).start();
        return 0;
    }

    @Override
    public int setStep(int step) throws RemoteException {
        new Thread(()->{
            logger.info("设置仿真步数:"+step);
            controllable.SetSimStep(step);
        }).start();
        return step;

    }

    @Override
    public int stop() throws RemoteException {
        logger.info("停止仿真:");
        controllable.SimStopRel();
        while (controllable.BlockThreadSt()!=-1){
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        close();
//        try {
//            ItsEngineApplication.registToManager(simId);
//        } catch (NotBoundException e) {
//            e.printStackTrace();
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
        return 0;
    }

    @Override
    public int getStatus() throws RemoteException {
        return controllable.BlockThreadSt();
    }

    @Override
    public int close() throws RemoteException {
        try {
            logger.info("仿真停止或引擎崩溃:解绑实例:" + instanceId);
            Naming.unbind(instanceId);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        System.exit(0);
           return 0;
    }

    @Override
    public void changeSignalPlan(int crossID, int cycleTime, int ewStraight, int snStraight, int snLeft) {
        controllable.NewSignalPlan(crossID,cycleTime,ewStraight,snStraight,snLeft);
    }

    @Override
    public boolean SetVehicleGeneratingModel(int LinkID, int model, int demand, double extraParams) {
        return controllable.SetVehicleGeneratingModel(LinkID,model,demand,extraParams);
    }

    @Override
    public void SetVehicleFollowingModel(int modelID) throws RemoteException {
        controllable.SetVehicleFollowingModel(modelID);
    }
}
