package edu.uestc.iscssl.common.common;

import edu.uestc.iscssl.common.params.webParm;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface EngineControll extends Remote, Serializable {
    int start(SimulationTask task) throws RemoteException;
    int setStep(int step) throws RemoteException;
    int stop() throws RemoteException;
    int getStatus() throws RemoteException;
    int close() throws RemoteException;
    void changeSignalPlan(int crossID,int cycleTime,int ewStraight,int snStraight,int snLeft) throws RemoteException;
    boolean SetVehicleGeneratingModel(int LinkID, int model, int demand, double extraParams) throws  RemoteException;
    void SetVehicleFollowingModel(int modelID)throws  RemoteException;
}
