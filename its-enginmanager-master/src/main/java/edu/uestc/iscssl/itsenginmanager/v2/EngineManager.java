package edu.uestc.iscssl.itsenginmanager.v2;

import edu.uestc.iscssl.common.common.Constant;
import edu.uestc.iscssl.common.common.SimulationTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class EngineManager {
    Log logger;
    ConcurrentHashMap<String, Engine> initEngines;//哪些已经创建
    BlockingQueue<Engine> avaliableEngines;
    ConcurrentHashMap<String, Engine> usingEngines;//正在被使用

    AtomicInteger engineRequireCount = new AtomicInteger();//需要被仿真多少个
    TaskManager taskManager;
    String rmiServerAddr;
    // volatile boolean hasInit = false;

    public EngineManager(String rmiServerAddr){
        this.logger = LogFactory.getLog(EngineManager.class);
        initEngines = new ConcurrentHashMap<>();
        avaliableEngines = new LinkedBlockingQueue<>();
        usingEngines = new ConcurrentHashMap<>();
        this.rmiServerAddr=rmiServerAddr;
    }

    public void createEngine() {
        String instanceId = getInstanceId();
        doCreateEngineInstance(instanceId);
        initEngines.put(instanceId, new Engine(instanceId, "initialling"));
    }

    public Engine getAvalibleEngine() throws InterruptedException {
        return avaliableEngines.take();
    }
    public void needEngineSign(){
        synchronized (engineRequireCount){
            this.engineRequireCount.incrementAndGet();
            engineRequireCount.notify();
        }

    }
    public void init(){
        try {
            LocateRegistry.createRegistry(1099);

        } catch (RemoteException e) {
            logger.info("本机已开启rmi服务");
        }
        EngineStatusUpdaterImpl engineStatusUpdater= null;
        try {
            engineStatusUpdater = new EngineStatusUpdaterImpl(this);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            Naming.bind("engineManager",engineStatusUpdater);
        } catch (AlreadyBoundException e) {
            try {
                Naming.unbind("engineManager");
                Naming.bind("engineManager",engineStatusUpdater);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            } catch (NotBoundException ex) {
                ex.printStackTrace();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (AlreadyBoundException ex) {
                ex.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        new Thread(new EngineHandler(), "引擎创建线程").start();
        logger.info("引擎创建线程启动");
    }
    public  void startSimulation(SimulationTask task)  {
        Engine engine = null;
        try {
            engine = getAvalibleEngine();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("获取可用引擎：" + engine.instanceId);
        try {
            engine.start(task);
            usingEngines.put(task.getSimulationId(), engine);
            logger.info("可用引擎" + task.getSimulationId() + "状态：" + usingEngines.get(task.getSimulationId()).getStatus());
        } catch (RemoteException e) {
            logger.warn("引擎："+engine.instanceId+"分配任务："+task.getSimulationId()+"失败，重新为任务分配引擎并尝试释放引擎资源");

            task.setPriority(task.getPriority()-1);

            synchronized (engineRequireCount){
                this.taskManager.submitSimulationTask(task);
                createEngine();
                engineRequireCount.decrementAndGet();
            }

            try {
                engine.close();
                logger.warn("引擎"+engine.instanceId+"销毁完成");
            } catch (RemoteException ex) {
                logger.warn("引擎"+engine.instanceId+"销毁失败");
                try {
                    Naming.unbind(engine.instanceId);
                } catch (RemoteException exc) {
                    exc.printStackTrace();
                } catch (NotBoundException exc) {
                    exc.printStackTrace();
                } catch (MalformedURLException exc) {
                    exc.printStackTrace();
                }
            }
        }


    }
    public  void stopSimulation(String simulationId) throws RemoteException {
        //this.usingEngines.remove(simulationId).stop();
        Engine curEngine = this.usingEngines.remove(simulationId);
        curEngine.stop();
    }

    public void afterEngineAvaliable(Engine engine,boolean isNewInstance){
        if (!isNewInstance)
            engineRequireCount.decrementAndGet();
        this.avaliableEngines.add(engine);

    }
    protected String getInstanceId() {
        return UUID.randomUUID().toString();
    }

    protected void doCreateEngineInstance(String instanceId) {
        try {
            logger.info("创建引擎:" + instanceId);
            logger.info("当前目录"+System.getProperty("user.dir"));
            String command = "cmd /c start " + new Constant().getJdkPath() + "\\bin\\javaw -jar " + new Constant().getJarPath() + " " +instanceId;
            logger.info("command:" + command);
            Runtime.getRuntime().exec(command);
            //Runtime.getRuntime().exec("cmd /c start C:\\jdk8u202\\jre\\bin\\java -jar its-engine.jar " +instanceId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void bind(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    class EngineHandler implements Runnable {
        @Override
        public void run() {
            synchronized (engineRequireCount){
                while (true){
                    if (engineRequireCount.get()<=0) {
                        try {
                            engineRequireCount.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else {
                        logger.info("当前等待任务数" + engineRequireCount.get() + ",创建中引擎数" + initEngines.size() + "可使用引擎数"+avaliableEngines.size());
                        logger.info("可用引擎不足，创建新引擎");
                        createEngine();
                        engineRequireCount.decrementAndGet();
                    }
                }
            }
        }
    }
}
