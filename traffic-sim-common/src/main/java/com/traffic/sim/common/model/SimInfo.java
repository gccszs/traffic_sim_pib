package com.traffic.sim.common.model;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 仿真会话信息
 * 
 * @author traffic-sim
 */
@Data
public class SimInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 会话ID */
    private String sessionId;
    
    /** 仿真名称 */
    private String name;
    
    /** XML文件完整路径 */
    private String xmlPath;
    
    /** XML文件名 */
    private String mapXmlName;
    
    /** 仿真目录（Python服务管理） */
    private String simDir;
    
    /** 仿真配置信息 */
    private Map<String, Object> simInfo;
    
    /** 控制视图 */
    private List<Map<String, Object>> controlViews;
    
    /** 前端WebSocket连接 */
    private transient WebSocketSession frontendConnection;
    
    /** 引擎WebSocket连接 */
    private transient WebSocketSession simengConnection;
    
    /** 前端初始化标志 */
    private boolean frontendInitOk;
    
    /** 引擎初始化标志 */
    private boolean simengInitOk;
    
    /** 创建时间 */
    private Long createTime;
    
    /** 最后更新时间 */
    private Long lastUpdateTime;
    
    /**
     * 检查引擎是否已初始化
     */
    public boolean isEngineInitialized() {
        return simengInitOk && simengConnection != null && simengConnection.isOpen();
    }
    
    /**
     * 检查前端是否已初始化
     */
    public boolean isFrontendInitialized() {
        return frontendInitOk && frontendConnection != null && frontendConnection.isOpen();
    }
}

