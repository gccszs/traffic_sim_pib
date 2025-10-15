package edu.uestc.iscssl.itsenginmanager.v2;

import edu.uestc.iscssl.common.common.EngineControll;
import edu.uestc.iscssl.common.common.SimulationTask;

import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Engine implements EngineControll {
    String instanceId;
    String status;
    EngineControll engineControll;
    public Engine(String instanceId, String status) {
        this.instanceId = instanceId;
        this.status = status;
    }
    public void init() throws RemoteException, NotBoundException, MalformedURLException {
        this.engineControll=(EngineControll)Naming.lookup(instanceId);
    }
    @Override
    public int start(SimulationTask task) throws RemoteException{
        return engineControll.start(task);
    }

    @Override
    public int setStep(int step) throws RemoteException {
        return engineControll.setStep(step);
    }

    @Override
    public int stop() throws RemoteException {
        return engineControll.stop();
    }

    @Override
    public int getStatus() throws RemoteException {
        return engineControll.getStatus();
    }

    @Override
    public int close() throws RemoteException {
        return engineControll.close();
    }

    @Override
    public void changeSignalPlan(int crossID, int cycleTime, int ewStraight, int snStraight, int snLeft) throws RemoteException {
        engineControll.changeSignalPlan(crossID,cycleTime,ewStraight,snStraight,snLeft);
    }

    @Override
    public boolean SetVehicleGeneratingModel(int LinkID, int model, int demand, double extraParams) throws RemoteException {
        return engineControll.SetVehicleGeneratingModel(LinkID,model,demand,extraParams);
    }

    @Override
    public void SetVehicleFollowingModel(int modelID) throws RemoteException {
        engineControll.SetVehicleFollowingModel(modelID);
    }
}
