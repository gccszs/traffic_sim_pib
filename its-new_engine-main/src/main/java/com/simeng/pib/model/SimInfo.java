package com.simeng.pib.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 仿真实例信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimInfo {

    /**
     * 仿真实例名
     */
    private String name = "test";

    /**
     * 路网地图内部路径（完整路径）
     */
    private String xml_path = "";

    /**
     * 路网地图XML文件名（仅文件名）
     */
    private String map_xml_name = "";

    /**
     * 仿真文件目录
     */
    private String sim_dir = "";

    /**
     * 用户设置的仿真信息
     */
    private Map<String, Object> sim_info = new ConcurrentHashMap<>();

    /**
     * 用户设置的插件信息
     */
    private List<Map<String, Object>> control_views = new CopyOnWriteArrayList<>();

    /**
     * 前端WebSocket连接
     */
    private WebSocketSession frontend_connection;

    /**
     * 仿真引擎WebSocket连接
     */
    private WebSocketSession simeng_connection;

    /**
     * 前端是否初始化完成
     */
    private boolean frontend_init_ok = false;

    /**
     * 仿真引擎是否初始化完成
     */
    private boolean simeng_init_ok = false;
}
