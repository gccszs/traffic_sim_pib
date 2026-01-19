package com.traffic.sim.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 地图上传响应DTO
 * 上传成功后直接返回地图JSON数据，前端无需再次请求
 * 
 * 响应结构兼容原 get_map_json 接口格式:
 * {
 *   "res": "ERR_OK",
 *   "msg": "convert xml to json ok",
 *   "addition": {
 *     "data": { ... }
 *   }
 * }
 * 
 * @author traffic-sim
 */
@Data
public class MapUploadResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 结果码
     * ERR_OK: 成功
     * ERR_FAIL: 失败
     */
    private String res;
    
    /**
     * 提示消息
     */
    private String msg;
    
    /**
     * 附加数据（包含地图核心数据）
     */
    private MapAddition addition;
    
    /**
     * 创建成功响应
     */
    public static MapUploadResponse success(Map<String, Object> mapData, String mapId) {
        MapUploadResponse response = new MapUploadResponse();
        response.setRes("ERR_OK");
        response.setMsg("convert xml to json ok");
        
        MapAddition addition = new MapAddition();
        addition.setData(mapData);
        addition.setMapId(mapId);
        response.setAddition(addition);
        
        return response;
    }
    
    /**
     * 创建成功响应（兼容旧版本，不包含mapId）
     */
    public static MapUploadResponse success(Map<String, Object> mapData) {
        return success(mapData, null);
    }
    
    /**
     * 创建失败响应
     */
    public static MapUploadResponse fail(String message) {
        MapUploadResponse response = new MapUploadResponse();
        response.setRes("ERR_FAIL");
        response.setMsg(message);
        return response;
    }
    
    /**
     * 地图附加数据
     */
    @Data
    public static class MapAddition implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /**
         * 地图核心数据
         * 包含: Demand, MarginalPoint, Cross, Link 等
         */
        private Map<String, Object> data;
        
        /**
         * 地图ID
         * 上传成功后返回给前端，用于后续仿真创建等操作
         */
        private String mapId;
    }
}
