package edu.uestc.iscssl.itsenginmanager;

import edu.uestc.iscssl.common.common.EngineManagerStatusUpdater;
import edu.uestc.iscssl.common.common.SIMULATION_STATUS;
import edu.uestc.iscssl.common.common.SimulationTask;
import edu.uestc.iscssl.common.facade.EngineManagerFacade;
import edu.uestc.iscssl.itsenginmanager.v2.EngineService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class EngineManagerService extends UnicastRemoteObject implements EngineManagerFacade {
    private EngineService engineService;
    private EngineManagerStatusUpdater updater;
    private String instanceId;
    protected EngineManagerService(EngineService engineService,String instanceId) throws RemoteException {
        this.engineService=engineService;
        this.instanceId=instanceId;
        this.engineService.setEngineManagerService(this);
    }

    @Override
    public int submitSimulationTask(SimulationTask task) {

        engineService.submitSimulationTask(task);
        try {
            updater.onTaskStatusChanged(task.getSimulationId(), SIMULATION_STATUS.RECIEVED);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void stopSimulation(String simulationId) throws RemoteException {
        try {
            engineService.stopSimulationTask(simulationId);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            this.updater.onTaskStatusChanged(simulationId,SIMULATION_STATUS.STOP);
        }
    }

    @Override
    public void setStep(String simulationId, int step) {
        engineService.setStep(simulationId,step);
    }

    @Override
    public void changeSignalPlan(String simulationId,int crossID, int cycleTime, int ewStraight, int snStraight, int snLeft) throws RemoteException {
        engineService.changeSignalPlan(simulationId,crossID,cycleTime,ewStraight,snStraight,snLeft);
    }

    @Override
    public boolean SetVehicleGeneratingModel(String simulationId, int LinkID, int model, int demand, double extraParams) throws RemoteException {
        return engineService.SetVehicleGeneratingModel(simulationId,LinkID,model,demand,extraParams);
    }

    @Override
    public void SetVehicleFollowingModel(String simulationId, int modelID) throws RemoteException {
        engineService.SetVehicleFollowingModel(simulationId,modelID);
    }



    public void setUpdater(EngineManagerStatusUpdater updater) {
        this.updater = updater;
    }

    public EngineManagerStatusUpdater getUpdater() {
        return updater;
    }
}
