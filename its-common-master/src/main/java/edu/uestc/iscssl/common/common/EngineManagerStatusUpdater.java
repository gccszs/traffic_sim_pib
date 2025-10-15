package edu.uestc.iscssl.common.common;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
//engineManager调用的后端方法
public interface EngineManagerStatusUpdater extends Remote, Serializable {
    //engineManager初始化完成后向rmi注册
    void onEnginManagerAvaliable(String instanceId, List<String> instanceIpAddr) throws RemoteException;
    //task被engineManager接收后的回调
    void onTaskStatusChanged(String simulationTaskId,SIMULATION_STATUS status) throws RemoteException;
}
