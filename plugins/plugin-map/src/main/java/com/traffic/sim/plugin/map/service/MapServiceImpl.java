package com.traffic.sim.plugin.map.service;

import com.traffic.sim.common.dto.MapDTO;
import com.traffic.sim.common.dto.MapInfoDTO;
import com.traffic.sim.common.dto.MapUpdateRequest;
import com.traffic.sim.common.dto.UserMapSpaceDTO;
import com.traffic.sim.common.exception.BusinessException;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.service.MapService;
import com.traffic.sim.plugin.map.config.MapPluginProperties;
import com.traffic.sim.plugin.map.entity.MapEntity;
import com.traffic.sim.plugin.map.entity.UserMapQuota;
import com.traffic.sim.plugin.map.repository.MapRepository;
import com.traffic.sim.plugin.map.repository.UserMapQuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 地图服务实现
 * 
 * @author traffic-sim
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MapServiceImpl implements MapService {
    
    private final MapRepository mapRepository;
    private final UserMapQuotaRepository quotaRepository;
    private final MapQuotaService quotaService;
    private final MapPermissionService permissionService;
    private final MapPluginProperties mapProperties;
    private final MongoTemplate mongoTemplate;
    
    @Override
    @Transactional
    public MapDTO saveMapInfo(Map<String, String> request, Long userId) {
        // 旧版兼容接口实现
        MapEntity mapEntity = new MapEntity();
        mapEntity.setName(request.getOrDefault("mapName", "未命名地图"));
        mapEntity.setDescription(request.get("description"));
        mapEntity.setFilePath(request.get("filePath"));
        mapEntity.setFileName(request.get("fileName"));
        mapEntity.setXmlFileName(request.get("xmlFileName"));
        mapEntity.setMapId(request.get("mapId"));
        mapEntity.setOwnerId(userId);
        mapEntity.setStatus(MapEntity.MapStatus.PRIVATE);
        
        if (request.containsKey("fileSize")) {
            try {
                mapEntity.setFileSize(Long.parseLong(request.get("fileSize")));
            } catch (NumberFormatException e) {
                log.warn("Invalid file size: {}", request.get("fileSize"));
            }
        }
        
        MapEntity saved = mapRepository.save(mapEntity);
        return convertToDTO(saved);
    }
    
    @Override
    @Transactional
    public MapDTO uploadAndConvertMap(MultipartFile file, String name, 
                                     String description, Integer status, Long userId) {
        // 验证文件
        validateFile(file);
        
        // 检查配额
        quotaService.checkUserQuota(userId, file.getSize());
        
        try {
            // 保存文件
            String storagePath = saveFile(file, userId);
            
            // 调用Python服务转换文件（这里先简化，实际需要gRPC调用）
            // TODO: 实现gRPC调用Python服务
            
            // 创建地图实体
            MapEntity mapEntity = new MapEntity();
            mapEntity.setName(name);
            mapEntity.setDescription(description);
            mapEntity.setFilePath(storagePath);
            mapEntity.setFileName(file.getOriginalFilename());
            mapEntity.setOwnerId(userId);
            mapEntity.setFileSize(file.getSize());
            mapEntity.setStoragePath(storagePath);
            mapEntity.setStatus(status != null ? MapEntity.MapStatus.fromCode(status) : MapEntity.MapStatus.PRIVATE);
            
            // 生成mapId（MongoDB ID）
            String mapId = UUID.randomUUID().toString();
            mapEntity.setMapId(mapId);
            
            MapEntity saved = mapRepository.save(mapEntity);
            
            // 更新配额
            quotaService.updateQuotaAfterUpload(userId, file.getSize());
            
            return convertToDTO(saved);
        } catch (IOException e) {
            log.error("Failed to save file", e);
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
    }
    
    @Override
    public PageResult<MapDTO> getUserMaps(Long userId, String mapName, 
                                          Integer status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        MapEntity.MapStatus mapStatus = status != null ? MapEntity.MapStatus.fromCode(status) : null;
        
        Page<MapEntity> pageResult = mapRepository.findByOwnerIdAndStatusAndNameContaining(
            userId, mapStatus, mapName, pageable);
        
        return convertToPageResult(pageResult);
    }
    
    @Override
    public PageResult<MapDTO> getUserMaps(Long userId, String mapName, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        
        Page<MapEntity> pageResult;
        if (mapName != null && !mapName.isEmpty()) {
            pageResult = mapRepository.findByOwnerIdAndNameContaining(userId, mapName, pageable);
        } else {
            pageResult = mapRepository.findByOwnerId(userId, pageable);
        }
        
        return convertToPageResult(pageResult);
    }
    
    @Override
    public PageResult<MapDTO> getPublicMaps(String mapName, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<MapEntity> pageResult = mapRepository.findPublicMaps(
            MapEntity.MapStatus.PUBLIC, mapName, pageable);
        
        return convertToPageResult(pageResult);
    }
    
    @Override
    public MapDTO getMapById(String mapId, Long userId) {
        MapEntity mapEntity = mapRepository.findByMapId(mapId)
            .orElseThrow(() -> new BusinessException("地图不存在"));
        
        // 检查权限
        boolean isAdmin = false; // TODO: 从TokenInfo获取管理员标识
        if (!permissionService.canAccess(mapEntity, userId, isAdmin)) {
            throw new BusinessException("无权访问该地图");
        }
        
        return convertToDTO(mapEntity);
    }
    
    @Override
    @Transactional
    public MapDTO updateMap(String mapId, MapUpdateRequest request, Long userId) {
        MapEntity mapEntity = mapRepository.findByMapId(mapId)
            .orElseThrow(() -> new BusinessException("地图不存在"));
        
        // 检查权限
        boolean isAdmin = false; // TODO: 从TokenInfo获取管理员标识
        if (!permissionService.canModify(mapEntity, userId, isAdmin)) {
            throw new BusinessException("无权修改该地图");
        }
        
        if (request.getName() != null) {
            mapEntity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            mapEntity.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            mapEntity.setStatus(MapEntity.MapStatus.fromCode(request.getStatus()));
        }
        
        MapEntity saved = mapRepository.save(mapEntity);
        return convertToDTO(saved);
    }
    
    @Override
    @Transactional
    public void deleteMap(String mapId, Long userId) {
        MapEntity mapEntity = mapRepository.findByMapId(mapId)
            .orElseThrow(() -> new BusinessException("地图不存在"));
        
        // 检查权限
        boolean isAdmin = false; // TODO: 从TokenInfo获取管理员标识
        if (!permissionService.canDelete(mapEntity, userId, isAdmin)) {
            throw new BusinessException("无权删除该地图");
        }
        
        // 删除文件
        try {
            if (mapEntity.getStoragePath() != null) {
                Path filePath = Paths.get(mapEntity.getStoragePath());
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", mapEntity.getStoragePath(), e);
        }
        
        // 更新配额
        long fileSize = mapEntity.getFileSize() != null ? mapEntity.getFileSize() : 0L;
        quotaService.updateQuotaAfterDelete(userId, fileSize);
        
        // 删除数据库记录
        mapRepository.delete(mapEntity);
    }
    
    @Override
    @Transactional
    public void deleteMapByAdmin(String mapId, Integer status) {
        MapEntity mapEntity = mapRepository.findByMapId(mapId)
            .orElseThrow(() -> new BusinessException("地图不存在"));
        
        // 管理员删除逻辑
        if (status != null && status == 2) {
            // 设置为禁用状态
            mapEntity.setStatus(MapEntity.MapStatus.FORBIDDEN);
            mapRepository.save(mapEntity);
        } else {
            // 物理删除
            mapRepository.delete(mapEntity);
        }
    }
    
    @Override
    public PageResult<MapDTO> getAllMaps(String mapName, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<MapEntity> pageResult = mapRepository.findAllMaps(mapName, pageable);
        
        return convertToPageResult(pageResult);
    }
    
    @Override
    public MapInfoDTO getMapInfoFromMongoDB(String mapId, Long userId) {
        // 先检查权限
        MapEntity mapEntity = mapRepository.findByMapId(mapId)
            .orElseThrow(() -> new BusinessException("地图不存在"));
        
        boolean isAdmin = false; // TODO: 从TokenInfo获取管理员标识
        if (!permissionService.canAccess(mapEntity, userId, isAdmin)) {
            throw new BusinessException("无权访问该地图");
        }
        
        // 从MongoDB获取地图数据
        Query query = new Query(Criteria.where("_id").is(mapId));
        java.util.Map<String, Object> mapData = mongoTemplate.findOne(query, java.util.Map.class, "maps");
        
        MapInfoDTO mapInfo = new MapInfoDTO();
        mapInfo.setMapId(mapId);
        mapInfo.setMapData(mapData);
        
        return mapInfo;
    }
    
    @Override
    public MapInfoDTO previewMapInfo(String mapFile) {
        // 预览地图信息（简化实现）
        MapInfoDTO mapInfo = new MapInfoDTO();
        // TODO: 实现预览逻辑
        return mapInfo;
    }
    
    @Override
    public UserMapSpaceDTO getUserMapSpace(Long userId) {
        UserMapQuota quota = quotaService.getUserQuota(userId);
        
        UserMapSpaceDTO space = new UserMapSpaceDTO();
        space.setUserId(userId);
        space.setMaxMaps(quota.getMaxMaps());
        space.setCurrentMaps(quota.getCurrentMaps());
        space.setTotalSize(quota.getTotalSize());
        space.setMaxSize(quota.getMaxSize());
        space.setRemainingMaps(quota.getMaxMaps() - quota.getCurrentMaps());
        space.setRemainingSize(quota.getMaxSize() - quota.getTotalSize());
        
        if (quota.getMaxSize() > 0) {
            space.setUsageRate((double) quota.getTotalSize() / quota.getMaxSize() * 100);
        } else {
            space.setUsageRate(0.0);
        }
        
        return space;
    }
    
    @Override
    public void checkUserQuota(Long userId, long fileSize) {
        quotaService.checkUserQuota(userId, fileSize);
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > mapProperties.getUpload().getMaxFileSize()) {
            throw new BusinessException("文件大小超过限制: " + 
                (mapProperties.getUpload().getMaxFileSize() / 1024 / 1024) + "MB");
        }
        
        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!mapProperties.getUpload().getAllowedExtensions().contains(extension)) {
                throw new BusinessException("不支持的文件类型: " + extension);
            }
        }
    }
    
    /**
     * 保存文件
     */
    private String saveFile(MultipartFile file, Long userId) throws IOException {
        String basePath = mapProperties.getStorage().getBasePath();
        String userPath = basePath + File.separator + userId;
        
        // 创建用户目录
        Path userDir = Paths.get(userPath);
        Files.createDirectories(userDir);
        
        // 生成文件名
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = userDir.resolve(fileName);
        
        // 保存文件
        file.transferTo(filePath.toFile());
        
        return filePath.toString();
    }
    
    /**
     * 实体转DTO
     */
    private MapDTO convertToDTO(MapEntity entity) {
        MapDTO dto = new MapDTO();
        dto.setId(entity.getId());
        dto.setMapId(entity.getMapId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setFilePath(entity.getFilePath());
        dto.setFileName(entity.getFileName());
        dto.setXmlFileName(entity.getXmlFileName());
        dto.setMapImage(entity.getMapImage());
        dto.setOwnerId(entity.getOwnerId());
        dto.setStatus(entity.getStatus().getCode());
        dto.setFileSize(entity.getFileSize());
        dto.setStoragePath(entity.getStoragePath());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }
    
    /**
     * Page转PageResult
     */
    private PageResult<MapDTO> convertToPageResult(Page<MapEntity> page) {
        List<MapDTO> records = page.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageResult<>(
            records,
            page.getTotalElements(),
            page.getNumber() + 1,
            page.getSize()
        );
    }
}

