package edu.uestc.iscssl.itsbackend.service;

import edu.uestc.iscssl.common.params.webParm;
import edu.uestc.iscssl.itsbackend.domain.simulation.SimulationEntity;

import java.rmi.RemoteException;
import java.util.List;

public interface EngineManagerService {
    void submitSimulationEntity(SimulationEntity simulationEntity,webParm webParm);
    void setStep(String simulationId,int step);
    void stop(String simulationId);
    List<SimulationEntity> getUserSimulations(long userId);
    void changeSignalPlan(String simulationId,int crossID,int cycleTime,int ewStraight,int snStraight,int snLeft) ;
    boolean SetVehicleGeneratingModel(String simulationId,int LinkID, int model, int demand, double extraParams) throws RemoteException;
    void SetVehicleFollowingModel(String simulationId,int modelID) throws RemoteException;
}
