package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.common.common.Constant;
import edu.uestc.iscssl.common.common.EngineManagerStatusUpdater;
import edu.uestc.iscssl.common.common.SIMULATION_STATUS;
import edu.uestc.iscssl.common.common.SimulationTask;
import edu.uestc.iscssl.common.facade.EngineManagerFacade;
import edu.uestc.iscssl.common.params.webParm;
import edu.uestc.iscssl.itsbackend.Process.KafkaReader;
import edu.uestc.iscssl.itsbackend.domain.simulation.SimulationEntity;
import edu.uestc.iscssl.itsbackend.repository.SimulationRepository;
import edu.uestc.iscssl.itsbackend.service.DataInfoService;
import edu.uestc.iscssl.itsbackend.service.EngineManagerService;
import edu.uestc.iscssl.itsbackend.service.MapService;
import edu.uestc.iscssl.itsbackend.utils.ITSException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EngineManagerServiceImpl extends UnicastRemoteObject implements EngineManagerStatusUpdater, EngineManagerService {
    @Autowired
    MapService mapService;
    @Autowired
    SimulationRepository simulationRepository;
    @Autowired
    DataInfoService dataInfoService;
    Log logger = LogFactory.getLog(EngineManagerServiceImpl.class);

    private Map<String,EngineManagerFacade> engineManagers=new HashMap<>();
    private Map<String,KafkaReader> kafkaReaderThreads=new HashMap<>();
    private volatile AtomicInteger index=new AtomicInteger(0);
    public EngineManagerServiceImpl() throws RemoteException {
    }

    @Override
    public void onEnginManagerAvaliable(String instanceId,List<String> instanceIpAddr) throws RemoteException {
        if (engineManagers.containsKey(instanceId))
            logger.warn("instanceId:"+instanceId+"已经被注册");
        logger.info("发现可用引擎管理："+instanceId+",开始尝试建立链接");
        bindEngineManager(instanceId,instanceIpAddr);
    }

    @Override
    public void onTaskStatusChanged(String simulationTaskId, SIMULATION_STATUS status) throws RemoteException {
        SimulationEntity simulationEntity=this.simulationRepository.findById(simulationTaskId).get();
        simulationEntity.setStatus(status);
        if (status==SIMULATION_STATUS.RUNNING){
            String filePath=mapService.getMapFilePath(simulationEntity.getMapId());
            KafkaReader reader=new KafkaReader(simulationTaskId,filePath,dataInfoService,simulationEntity.getStep());
            kafkaReaderThreads.put(simulationTaskId,reader);
            reader.start();
            logger.info("任务："+simulationTaskId+"处理线程启动");
        }
        if (status==SIMULATION_STATUS.STOP){
            KafkaReader reader=kafkaReaderThreads.remove(simulationTaskId);
            reader.shutdown();
            logger.info("任务："+simulationTaskId+"处理线程关闭");
        }
        this.simulationRepository.save(simulationEntity);
    }


    private void bindEngineManager(String instanceId,List<String> instanceIpAddr) throws RemoteException {
        String avaliableAddr=null;
        EngineManagerFacade engineManagerFacade=null;
        for(String addr:instanceIpAddr){
            try {
                engineManagerFacade=(EngineManagerFacade)Naming.lookup("rmi://"+addr+":1099/engineManagerService");
                avaliableAddr=addr;
                break;
            } catch (NotBoundException e) {
            } catch (MalformedURLException e) {
            } catch (RemoteException e) {
            }
        }
        if (avaliableAddr==null)
            throw new RemoteException("与引擎管理"+instanceId+"链接建立失败");
        engineManagers.put(instanceId,engineManagerFacade);
        logger.info("与引擎管理"+instanceId+"链接建立成功，ip:"+avaliableAddr);
    }

    @Override
    public void submitSimulationEntity(SimulationEntity entity,webParm webParm) {
        try {
            String engineInstanceId=getAvaliableEngineId();
            entity.setEngineManagerId(engineInstanceId);
            this.simulationRepository.save(entity);
            this.engineManagers.get(engineInstanceId)
                    .submitSimulationTask(new SimulationTask(entity.getUserId(),entity.getSimulaitionId(),entity.getStep(),webParm));

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void setStep(String simulationId,int step) {
        String engineInstanceId=this.simulationRepository.findById(simulationId).get().getEngineManagerId();
        EngineManagerFacade engineManagerFacade = getTargetEngineManager(engineInstanceId);
        if(null == engineManagerFacade){
            logger.error("setStep：仿真实验id:" + simulationId + "，未连接到引擎管理");
            return;
        }
        try {
            engineManagerFacade.setStep(simulationId,step);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop(String simulationId) {
        String engineInstanceId=this.simulationRepository.findById(simulationId).get().getEngineManagerId();
        EngineManagerFacade engineManagerFacade = getTargetEngineManager(engineInstanceId);
        if(null == engineManagerFacade){
            logger.error("stop：仿真实验id:" + simulationId + "，可用引擎不存在");
            return;
        }
        try {
            engineManagerFacade.stopSimulation(simulationId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<SimulationEntity>  getUserSimulations(long userId) {
        return this.simulationRepository.findByUserId(userId);
    }

    @Override
    public void changeSignalPlan(String simulationId, int crossID, int cycleTime, int ewStraight, int snStraight, int snLeft) {
        String engineInstanceId=this.simulationRepository.findById(simulationId).get().getEngineManagerId();
        try {
            getTargetEngineManager(engineInstanceId).changeSignalPlan(simulationId,crossID,cycleTime,ewStraight,snStraight,snLeft);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean SetVehicleGeneratingModel(String simulationId, int LinkID, int model, int demand, double extraParams) throws RemoteException {
        String engineInstanceId=this.simulationRepository.findById(simulationId).get().getEngineManagerId();
        return getTargetEngineManager(engineInstanceId).SetVehicleGeneratingModel(simulationId,LinkID,model,demand,extraParams);

    }

    @Override
    public void SetVehicleFollowingModel(String simulationId, int modelID) throws RemoteException {
        String engineInstanceId=this.simulationRepository.findById(simulationId).get().getEngineManagerId();
        getTargetEngineManager(engineInstanceId).SetVehicleFollowingModel(simulationId,modelID);
    }

    private synchronized String getAvaliableEngineId(){
        if (engineManagers.size()==0)throw new ITSException("没有可用仿真节点！");
        return (String)(this.engineManagers.keySet().toArray()[index.getAndIncrement()%engineManagers.size()]);

    }
    private synchronized EngineManagerFacade getTargetEngineManager(String instanceId){
        return this.engineManagers.get(instanceId);
    }
}
