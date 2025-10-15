package edu.uestc.iscssl.common.common;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
//引擎调用的engineManager的方法
public interface EngineStatusUpdater extends Remote, Serializable {
     //引擎达到可用状态之后，向引擎管理发送信号。
     void afterEngineAvaliable(String instanceId,String simulationId) throws RemoteException;
}
