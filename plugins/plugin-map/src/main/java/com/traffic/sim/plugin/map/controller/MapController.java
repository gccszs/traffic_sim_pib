package com.traffic.sim.plugin.map.controller;

import com.traffic.sim.common.dto.MapDTO;
import com.traffic.sim.common.dto.MapInfoDTO;
import com.traffic.sim.common.dto.MapUploadResponse;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.service.MapService;
import com.traffic.sim.common.util.RequestContext;
import com.traffic.sim.plugin.auth.annotation.RequireRole;
import com.traffic.sim.plugin.map.dto.MapListResponse;
import com.traffic.sim.plugin.map.dto.MapSaveResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 地图Controller（旧版兼容接口）
 *
 * @author traffic-sim
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class MapController {

    private final MapService mapService;

    /**
     * 【旧版兼容】保存地图信息
     * POST /saveMapInfo
     */
    @PostMapping("/saveMapInfo")
    public ResponseEntity<ApiResponse<MapSaveResponse>> saveMapInfo(
            @RequestBody Map<String, String> request) {

        Long userId = getCurrentUserId();
        MapDTO mapDTO = mapService.saveMapInfo(request, userId);

        MapSaveResponse response = new MapSaveResponse();
        response.setMapId(mapDTO.getId().toString());
        response.setStatus("success");

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 【旧版兼容】获取用户地图列表
     * GET /getUserMap
     */
    @GetMapping("/getUserMap")
    public ResponseEntity<ApiResponse<MapListResponse>> getUserMap(
            @RequestParam(required = false) String mapName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        Long userId = getCurrentUserId();
        PageResult<MapDTO> result = mapService.getUserMaps(userId, mapName, page, limit);

        MapListResponse response = MapListResponse.fromPageResult(result);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 【旧版兼容】获取公开地图列表
     * GET /getPublicMap
     */
    @GetMapping("/getPublicMap")
    public ResponseEntity<ApiResponse<MapListResponse>> getPublicMap(
            @RequestParam(required = false) String mapName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        PageResult<MapDTO> result = mapService.getPublicMaps(mapName, page, limit);

        MapListResponse response = MapListResponse.fromPageResult(result);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 【旧版兼容】获取地图 JSON 数据
     * GET /get_map_json
     * 
     * 返回当前用户最近上传/操作的地图JSON数据
     */
    @GetMapping("/get_map_json")
    public ResponseEntity<MapUploadResponse> getMapJson() {
        Long userId = getCurrentUserId();
        MapUploadResponse response = mapService.getLatestMapJson(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 【旧版兼容】从 MongoDB 获取地图数据
     * GET /getMapInfoDB
     */
    @GetMapping("/getMapInfoDB")
    public ResponseEntity<ApiResponse<MapInfoDTO>> getMapInfoDB(
            @RequestParam String mapId) {

        Long userId = getCurrentUserId();
        MapInfoDTO mapInfo = mapService.getMapInfoFromMongoDB(mapId, userId);

        return ResponseEntity.ok(ApiResponse.success(mapInfo));
    }

    /**
     * 【旧版兼容】预览地图信息
     * POST /previewMapInfo
     */
    @PostMapping("/previewMapInfo")
    public ResponseEntity<ApiResponse<MapInfoDTO>> previewMapInfo(
            @RequestBody Map<String, String> request) {

        MapInfoDTO mapInfo = mapService.previewMapInfo(request.get("mapFile"));
        return ResponseEntity.ok(ApiResponse.success(mapInfo));
    }

    /**
     * 【旧版兼容】删除地图（管理员）
     */
    @DeleteMapping("/deleteMap")
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<String>> deleteMap(
            @RequestBody Map<String, Object> request) {

        String mapId = (String) request.get("mapId");
        Integer status = (Integer) request.get("status");

        mapService.deleteMapByAdmin(mapId, status);
        return ResponseEntity.ok(ApiResponse.success("Delete Success"));
    }

    /**
     * 【旧版兼容】获取所有地图（管理员）
     */
    @GetMapping("/getAllMap")
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<MapListResponse>> getAllMap(
            @RequestParam(required = false) String mapName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        PageResult<MapDTO> result = mapService.getAllMaps(mapName, page, limit);

        MapListResponse response = MapListResponse.fromPageResult(result);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        String userIdStr = RequestContext.getCurrentUserId();
        if (userIdStr == null) {
            throw new RuntimeException("用户未登录");
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("无效的用户ID: " + userIdStr);
        }
    }
}

