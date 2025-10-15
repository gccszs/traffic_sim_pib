package edu.uestc.iscssl.common.facade;
import edu.uestc.iscssl.common.common.SimulationTask;
import edu.uestc.iscssl.common.params.webParm;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface EngineManagerFacade extends Remote, Serializable {

    int submitSimulationTask(SimulationTask task)throws RemoteException;

    void stopSimulation(String simulationId)throws RemoteException;

    void setStep(String simulationId,int step)throws RemoteException;
    void changeSignalPlan(String simulationId,int crossID,int cycleTime,int ewStraight,int snStraight,int snLeft) throws RemoteException;
    boolean SetVehicleGeneratingModel(String simulationId,int LinkID, int model, int demand, double extraParams)throws RemoteException;
    void SetVehicleFollowingModel(String simulationId,int modelID) throws RemoteException;
}
