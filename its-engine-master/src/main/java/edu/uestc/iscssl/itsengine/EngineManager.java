package edu.uestc.iscssl.itsengine;
import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * 引擎管理器接口
 * 不再直接使用静态初始化块，改为通过EngineManagerFactory创建实例
 */
public interface EngineManager extends Library, EngineControllable {
    // 移除静态实例，改为通过工厂类创建
}
