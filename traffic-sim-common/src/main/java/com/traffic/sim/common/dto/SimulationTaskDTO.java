package com.traffic.sim.common.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仿真任务DTO
 * 
 * @author traffic-sim
 */
@Data
public class SimulationTaskDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 任务ID */
    private String taskId;
    
    /** 仿真名称 */
    private String name;
    
    /** 地图ID */
    private String mapId;
    
    /** 地图名称（用户定义的名称，用于前端显示） */
    private String mapName;
    
    /** 地图XML文件名（引擎内部使用的随机UUID） */
    private String mapXmlName;
    
    /** 地图XML文件路径 */
    private String mapXmlPath;
    
    /** 地图JSON数据 - 支持下划线命名 */
    @JsonProperty("map_json")
    private MapJsonDTO mapJson;
    
    /** 地图图片（base64） - 支持下划线命名 */
    @JsonProperty("map_pic")
    private String mapPic;
    
    /** OD矩阵配置 - 支持下划线命名 */
    @JsonProperty("fixed_od")
    private FixedOdDTO fixedOd;
    
    /** 仿真配置（JSON字符串） */
    private String simConfig;
    
    /** 状态：CREATED/RUNNING/PAUSED/STOPPED/FINISHED */
    private String status;
    
    /** 用户ID */
    private Long userId;
    
    /** 创建时间 */
    private Date createTime;
    
    /** 更新时间 */
    private Date updateTime;
    
    /** 其他所有字段 */
    private Map<String, Object> additionalFields = new HashMap<>();
    
    /**
     * 捕获所有未知字段
     */
    @JsonAnySetter
    public void setAdditionalField(String key, Object value) {
        additionalFields.put(key, value);
    }
    
    /**
     * 地图JSON数据DTO
     */
    @Data
    public static class MapJsonDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 需求数据 */
        private List<Map<String, Object>> demand;
        
        /** 边界点 */
        private List<Map<String, Object>> marginalPoint;
        
        /** 交叉口 */
        private List<Map<String, Object>> cross;
        
        /** 路段 */
        private List<Map<String, Object>> link;
        
        /** 车道 */
        private List<Map<String, Object>> lane;
        
        /** 控制器 */
        private List<Map<String, Object>> controller;
        
        /** 基线 */
        private List<Map<String, Object>> baseline;
        
        /** 其他所有字段 */
        private Map<String, Object> additionalFields = new HashMap<>();
        
        @JsonAnySetter
        public void setAdditionalField(String key, Object value) {
            additionalFields.put(key, value);
        }
    }
    
    /**
     * OD矩阵配置DTO
     */
    @Data
    public static class FixedOdDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 道路数量 */
        @JsonProperty("road_num")
        private Integer roadNum;
        
        /** 车道数量 */
        @JsonProperty("lane_num")
        private Integer laneNum;
        
        /** 控制器数量 */
        @JsonProperty("controller_num")
        private Integer controllerNum;
        
        /** 跟驰模型 */
        @JsonProperty("follow_model")
        private Integer followModel;
        
        /** 换道模型 */
        @JsonProperty("change_lane_model")
        private Integer changeLaneModel;
        
        /** 流量配置 */
        private List<Map<String, Object>> flows;
        
        /** OD对列表 */
        private List<Map<String, Object>> od;
        
        /** 信号灯组配置 */
        private List<Map<String, Object>> sg;
        
        /** 其他所有字段 */
        private Map<String, Object> additionalFields = new HashMap<>();
        
        @JsonAnySetter
        public void setAdditionalField(String key, Object value) {
            additionalFields.put(key, value);
        }
    }
}

