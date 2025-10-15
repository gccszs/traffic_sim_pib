package edu.uestc.iscssl.itsenginmanager.v2;

import edu.uestc.iscssl.common.common.SimulationTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class TaskManager {
    volatile boolean hasInit=false;
    private EngineManager engineManager;
    //仿真请求队列
    private PriorityBlockingQueue<SimulationTask> tasks;
    Log logger;
    private EngineService engineService;
    public TaskManager(EngineService engineService) {
        tasks=new PriorityBlockingQueue<>(10, new Comparator<SimulationTask>() {
            @Override
            public int compare(SimulationTask o1, SimulationTask o2) {
                int result;
                if (o1.getPriority()>o2.getPriority())
                    result=1;
                else if (o1.getPriority()==o2.getPriority())
                    result=0;
                else result=-1;
                return result;
            }
        });
        logger = LogFactory.getLog(EngineService.class);
        this.engineService=engineService;
    }
    public void bind(EngineManager engineManager){
        this.engineManager=engineManager;
        engineManager.bind(this);
    }
    public void submitSimulationTask(SimulationTask task){
        if (!hasInit){
            synchronized (this){
                if (!hasInit){
                    new Thread(new TaskHandler(),"任务分配线程").start();
                    logger.info("任务分配线程启动");
                    hasInit=true;
                }
            }
        }
        tasks.add(task);
        this.engineManager.needEngineSign();
    }
    public void stopSimulation(String simulationId) throws RemoteException {
        this.engineManager.stopSimulation(simulationId);
    }
    /**
     * 获取接受的仿真任务和仿真引擎
     */
    class TaskHandler implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
                    SimulationTask task=tasks.take();
                    engineManager.startSimulation(task);
                    engineService.onTaskDistributeToEngine(task.getSimulationId());
                    logger.info("接受用户："+task.getUserId()+"提交任务:"+task.getSimulationId());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//            while (true){
//                synchronized (tasks){
//                    SimulationTask task;
//                    if ((task=tasks.peek())!=null){
//                        if (!allowSimulate(task)){
//                            try {
//                                task=tasks.take();
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            tasks.add(task);
//                            continue;
//                        }
//                        try {
//                            engineManager.startSimulation(task);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        } catch (RemoteException e) {
//                            e.printStackTrace();
//                        } catch (AlreadyBoundException e) {
//                            e.printStackTrace();
//                        } catch (MalformedURLException e) {
//                            e.printStackTrace();
//                        } catch (NotBoundException e) {
//                            e.printStackTrace();
//                        }
//                    }  else{
//                        try {
//                            tasks.wait();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//
//                    //logger.info("正在分配任务:"+task.getSimulationId());
//
//            }
        }
        protected boolean allowSimulate(SimulationTask task){
            return true;
        }
    }
}
