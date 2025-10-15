package edu.uestc.iscssl.itsengine;

import com.sun.jna.Native;
import edu.uestc.iscssl.common.common.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * EngineManager工厂类
 * 负责创建和管理EngineManager实例，支持通过依赖注入获取dll路径配置
 */
@Component
public class EngineManagerFactory {

    private static Constant constant;
    private static EngineManager instance;
    
    @Autowired
    public void setConstant(Constant constant) {
        EngineManagerFactory.constant = constant;
    }
    
    /**
     * 获取EngineManager实例
     * @return EngineManager实例
     */
    public synchronized EngineManager getEngineManager() {
        if (instance == null) {
            String dllPath = constant != null ? constant.getDllPath() : "./engineDll/SimEngine";
            instance = Native.load(dllPath, EngineManager.class);
        }
        return instance;
    }
    
    /**
     * 供静态方法使用的获取实例的方法
     * @return EngineManager实例
     */
    public static EngineManager getInstance() {
        if (instance == null) {
            // 如果工厂类还未初始化，使用默认路径
            String dllPath = "./engineDll/SimEngine";
            instance = Native.load(dllPath, EngineManager.class);
        }
        return instance;
    }
}