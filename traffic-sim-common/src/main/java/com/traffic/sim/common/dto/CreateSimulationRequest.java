package com.traffic.sim.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建仿真请求DTO
 * 
 * @author traffic-sim
 */
@Data
public class CreateSimulationRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    

    /** 仿真配置信息 */
    private SimInfoDTO simInfo;
    
    /** 控制视图 */
    private List<ControlViewDTO> controlViews;
    
    /**
     * 仿真信息DTO
     */
    @Data
    public static class SimInfoDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 仿真名称 */
        private String name;
        
        /** 地图ID（前端传入，用于查询地图文件路径） */
        private String mapId;

//        /** 地图XML文件名 */
        private String mapXmlName;
        
        /** 地图XML文件路径（内部使用，由后端根据 mapId 查询填充） */
        private String mapXmlPath;
        
        /** OD矩阵配置 */
        private FixedODDTO fixedOd;
    }
    
    /**
     * OD矩阵配置DTO
     */
    @Data
    public static class FixedODDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** OD对列表 */
        private List<OriginODDTO> od;
        
        /** 信号灯组配置 */
        private List<SignalGroupDTO> sg;

    }
    
    /**
     * 起点OD配置DTO
     */
    @Data
    public static class OriginODDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 起点ID */
        private String originId;
        
        /** 目的地列表 */
        private List<DestinationDTO> dist;
    }
    
    /**
     * 目的地配置DTO
     */
    @Data
    public static class DestinationDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 目的地ID */
        private String dest;
        
        /** 流量比例 */
        private Double percent;
    }
    
    /**
     * 信号灯组配置DTO
     */
    @Data
    public static class SignalGroupDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 路口ID */
        private Integer crossId;
        
        /** 周期时间 */
        private Integer cycleTime;
        
        /** 东西直行时间 */
        private Integer ewStraight;
        
        /** 南北直行时间 */
        private Integer snStraight;
        
        /** 南北左转时间 */
        private Integer snLeft;
    }
    
    /**
     * 控制视图DTO
     */
    @Data
    public static class ControlViewDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 是否使用插件 */
        private Boolean usePlugin;
        
        /** 激活的插件名称 */
        private String activePlugin;
    }
}

