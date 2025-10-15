package edu.uestc.iscssl.itsenginmanager.v2;

import edu.uestc.iscssl.common.common.EngineStatusUpdater;
import edu.uestc.iscssl.itsenginmanager.ItsEnginmanagerApplication;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class EngineStatusUpdaterImpl extends UnicastRemoteObject implements EngineStatusUpdater {
    EngineManager engineManager;
    public EngineStatusUpdaterImpl(EngineManager engineManager) throws RemoteException {
        super();
        this.engineManager=engineManager;
    }
    @Override
    public void afterEngineAvaliable(String instanceId,String simulationId) throws RemoteException {
        boolean isNewInstance=false;
        Engine engine;
        if (simulationId==null){
            this.engineManager.initEngines.remove(instanceId);
            engine=new Engine(instanceId,"initialling");
            isNewInstance=true;
            try {
                engine.init();
            } catch (NotBoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }else {
            engine=this.engineManager.usingEngines.remove(simulationId);
        }
        engineManager.afterEngineAvaliable(engine,isNewInstance);
    }

//    public static void main(String[] args) throws RemoteException, AlreadyBoundException, MalformedURLException {
//        EngineStatusUpdaterImpl engineStatusUpdater=new EngineStatusUpdaterImpl(new EngineManager());
//        LocateRegistry.createRegistry(1099);
//        Naming.bind("engineManager",engineStatusUpdater);
//    }
}
