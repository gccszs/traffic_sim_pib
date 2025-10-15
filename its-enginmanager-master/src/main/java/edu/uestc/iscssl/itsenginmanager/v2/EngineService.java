package edu.uestc.iscssl.itsenginmanager.v2;

import edu.uestc.iscssl.common.common.EngineManagerStatusUpdater;
import edu.uestc.iscssl.common.common.SIMULATION_STATUS;
import edu.uestc.iscssl.common.common.SimulationTask;
import edu.uestc.iscssl.itsenginmanager.EngineManagerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class EngineService {
    private EngineManager engineManager;
    private TaskManager taskManager;
    private Log logger ;
    private EngineManagerService engineManagerService;
    public EngineService(String rmiServerAddr){
        logger = LogFactory.getLog(EngineService.class);
        engineManager=new EngineManager(rmiServerAddr);
        taskManager=new TaskManager(this);
        taskManager.bind(engineManager);
    }
    public void init(){
        engineManager.init();
        for (int i=0;i<0;i++){
            engineManager.createEngine();
            engineManager.engineRequireCount.decrementAndGet();
        }
    }
    public void submitSimulationTask(SimulationTask task){
        taskManager.submitSimulationTask(task);

    }
    public void setStep(String simulationId,int step){
        Engine engine = this.engineManager.usingEngines.get(simulationId);
        if(null == engine){
            logger.error("setStep：仿真实验id:" + simulationId + "，可用引擎不存在");
            return;
        }
        try {
            engine.setStep(step);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    public void stopSimulationTask(String simulationId) throws RemoteException {
        logger.info("停止任务:"+simulationId);
        taskManager.stopSimulation(simulationId);
    }
    public void onTaskDistributeToEngine(String simulationId){
        try {
            this.engineManagerService.getUpdater().onTaskStatusChanged(simulationId, SIMULATION_STATUS.RUNNING);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setEngineManagerService(EngineManagerService engineManagerService) {
        this.engineManagerService = engineManagerService;
    }
    public void changeSignalPlan(String simulationId,int crossID,int cycleTime,int ewStraight,int snStraight,int snLeft) throws RemoteException{
        this.engineManager.usingEngines.get(simulationId).changeSignalPlan(crossID,cycleTime,ewStraight,snStraight,snLeft);
    }
    public boolean SetVehicleGeneratingModel(String simulationId,int LinkID, int model, int demand, double extraParams) throws RemoteException {
        return  this.engineManager.usingEngines.get(simulationId).SetVehicleGeneratingModel(LinkID,model,demand,extraParams);
    }
    public void SetVehicleFollowingModel(String simulationId ,int modelID)throws RemoteException{
        this.engineManager.usingEngines.get(simulationId).SetVehicleFollowingModel(modelID);
    }
}
