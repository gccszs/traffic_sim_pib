package com.traffic.sim.plugin.map.controller;

import com.traffic.sim.common.dto.MapDTO;
import com.traffic.sim.common.dto.MapUpdateRequest;
import com.traffic.sim.common.dto.UserMapSpaceDTO;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.service.MapService;
import com.traffic.sim.plugin.auth.util.RequestContext;
import com.traffic.sim.plugin.map.entity.MapEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 地图API Controller（新版接口）
 * 
 * @author traffic-sim
 */
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
@Slf4j
public class MapApiController {
    
    private final MapService mapService;
    
    /**
     * 【新版】上传地图文件（文件上传方式）
     * POST /api/map/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<MapDTO>> uploadMap(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "1") Integer status) {
        
        Long userId = getCurrentUserId();
        MapDTO mapDTO = mapService.uploadAndConvertMap(file, name, description, status, userId);
        
        return ResponseEntity.ok(ApiResponse.success(mapDTO));
    }
    
    /**
     * 【新版】获取用户自己的地图列表
     * GET /api/map/my-maps
     */
    @GetMapping("/my-maps")
    public ResponseEntity<ApiResponse<PageResult<MapDTO>>> getMyMaps(
            @RequestParam(required = false) String mapName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = getCurrentUserId();
        PageResult<MapDTO> result = mapService.getUserMaps(userId, mapName, status, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 【新版】获取用户地图空间信息
     * GET /api/map/my-space
     */
    @GetMapping("/my-space")
    public ResponseEntity<ApiResponse<UserMapSpaceDTO>> getMyMapSpace() {
        Long userId = getCurrentUserId();
        UserMapSpaceDTO space = mapService.getUserMapSpace(userId);
        
        return ResponseEntity.ok(ApiResponse.success(space));
    }
    
    /**
     * 【新版】获取地图详情（带权限验证）
     * GET /api/map/{mapId}
     */
    @GetMapping("/{mapId}")
    public ResponseEntity<ApiResponse<MapDTO>> getMap(@PathVariable String mapId) {
        Long userId = getCurrentUserId();
        MapDTO mapDTO = mapService.getMapById(mapId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(mapDTO));
    }
    
    /**
     * 【新版】更新地图信息
     * PUT /api/map/{mapId}
     */
    @PutMapping("/{mapId}")
    public ResponseEntity<ApiResponse<MapDTO>> updateMap(
            @PathVariable String mapId,
            @RequestBody MapUpdateRequest request) {
        
        Long userId = getCurrentUserId();
        MapDTO mapDTO = mapService.updateMap(mapId, request, userId);
        
        return ResponseEntity.ok(ApiResponse.success(mapDTO));
    }
    
    /**
     * 【新版】删除用户自己的地图
     * DELETE /api/map/{mapId}
     */
    @DeleteMapping("/{mapId}")
    public ResponseEntity<ApiResponse<String>> deleteMap(@PathVariable String mapId) {
        Long userId = getCurrentUserId();
        mapService.deleteMap(mapId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Delete successful"));
    }
    
    /**
     * 【新版】获取公开地图列表
     * GET /api/map/public
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PageResult<MapDTO>>> getPublicMaps(
            @RequestParam(required = false) String mapName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PageResult<MapDTO> result = mapService.getPublicMaps(mapName, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(result));
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

