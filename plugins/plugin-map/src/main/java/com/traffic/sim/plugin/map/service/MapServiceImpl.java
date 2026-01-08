package com.traffic.sim.plugin.map.service;

import com.traffic.sim.common.dto.MapDTO;
import com.traffic.sim.common.dto.MapInfoDTO;
import com.traffic.sim.common.dto.MapUpdateRequest;
import com.traffic.sim.common.dto.UserMapSpaceDTO;
import com.traffic.sim.common.exception.BusinessException;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.service.MapService;
import com.traffic.sim.common.service.TokenInfo;
import com.traffic.sim.common.util.RequestContext;
import com.traffic.sim.plugin.map.client.PythonGrpcClient;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    private final PythonGrpcClient pythonGrpcClient;
    
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
            // 调用Python服务转换文件
            PythonGrpcClient.ConvertFileResponse convertResponse = 
                pythonGrpcClient.uploadAndConvertFile(file, userId.toString());
            
            if (!convertResponse.isSuccess()) {
                throw new BusinessException("地图转换失败: " + convertResponse.getMessage());
            }
            
            // 保存原始文件
            String storagePath = saveFile(file, userId);
            
            // 保存转换后的XML文件
            String xmlStoragePath = null;
            if (convertResponse.getXmlData() != null && convertResponse.getXmlData().length > 0) {
                xmlStoragePath = saveXmlFile(convertResponse.getXmlData(), 
                    convertResponse.getXmlFileName(), userId);
            }
            
            // 创建地图实体
            MapEntity mapEntity = new MapEntity();
            mapEntity.setName(name != null ? name : getMapNameFromFile(file.getOriginalFilename()));
            mapEntity.setDescription(description);
            mapEntity.setFilePath(storagePath);
            mapEntity.setFileName(file.getOriginalFilename());
            mapEntity.setXmlFileName(convertResponse.getXmlFileName());
            mapEntity.setOwnerId(userId);
            mapEntity.setFileSize(file.getSize());
            mapEntity.setStoragePath(xmlStoragePath != null ? xmlStoragePath : storagePath);
            mapEntity.setStatus(status != null ? MapEntity.MapStatus.fromCode(status) : MapEntity.MapStatus.PRIVATE);
            
            // 生成mapId
            String mapId = UUID.randomUUID().toString();
            mapEntity.setMapId(mapId);
            
            MapEntity saved = mapRepository.save(mapEntity);
            
            // 更新配额
            quotaService.updateQuotaAfterUpload(userId, file.getSize());
            
            log.info("Map uploaded and converted successfully: mapId={}, userId={}", mapId, userId);
            
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
        if (!permissionService.canAccess(mapEntity, userId, isCurrentUserAdmin())) {
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
        if (!permissionService.canModify(mapEntity, userId, isCurrentUserAdmin())) {
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
        if (!permissionService.canDelete(mapEntity, userId, isCurrentUserAdmin())) {
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
        
        if (!permissionService.canAccess(mapEntity, userId, isCurrentUserAdmin())) {
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
        if (mapFile == null || mapFile.isEmpty()) {
            throw new BusinessException("地图文件路径不能为空");
        }
        
        MapInfoDTO mapInfo = new MapInfoDTO();
        
        try {
            // 读取文件内容
            Path filePath = Paths.get(mapFile);
            if (!Files.exists(filePath)) {
                throw new BusinessException("地图文件不存在: " + mapFile);
            }
            
            byte[] fileContent = Files.readAllBytes(filePath);
            String fileName = filePath.getFileName().toString();
            
            // 获取当前用户ID用于创建预览目录
            String userId = RequestContext.getCurrentUserId();
            if (userId == null) {
                userId = "anonymous";
            }
            
            // 创建临时 MultipartFile 进行预览
            // 由于 gRPC 客户端需要 MultipartFile，这里创建一个简单的包装
            PythonGrpcClient.PreviewFileResponse previewResponse = 
                previewMapFileByContent(fileContent, fileName, userId);
            
            if (!previewResponse.isSuccess()) {
                throw new BusinessException("预览失败: " + previewResponse.getMessage());
            }
            
            // 构建返回数据
            Map<String, Object> mapData = new HashMap<>();
            mapData.put("roadCount", previewResponse.getRoadCount());
            mapData.put("intersectionCount", previewResponse.getIntersectionCount());
            mapData.put("previewData", previewResponse.getPreviewData());
            
            mapInfo.setMapData(mapData);
            mapInfo.setMetadata(Collections.singletonMap("fileName", fileName));
            
            log.info("Map preview successful: file={}, roads={}, intersections={}", 
                fileName, previewResponse.getRoadCount(), previewResponse.getIntersectionCount());
            
        } catch (IOException e) {
            log.error("Failed to read map file for preview", e);
            throw new BusinessException("读取地图文件失败: " + e.getMessage());
        }
        
        return mapInfo;
    }
    
    /**
     * 通过文件内容预览地图
     */
    private PythonGrpcClient.PreviewFileResponse previewMapFileByContent(
            byte[] fileContent, String fileName, String userId) {
        
        // 直接使用 gRPC 客户端的底层方法
        return pythonGrpcClient.previewMapFile(
            new ByteArrayMultipartFile(fileContent, fileName), userId);
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
     * 判断当前用户是否为管理员
     * 
     * @return 是否是管理员
     */
    private boolean isCurrentUserAdmin() {
        TokenInfo tokenInfo = RequestContext.getCurrentUser();
        if (tokenInfo == null) {
            return false;
        }
        String role = tokenInfo.getRole();
        return "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
    }
    
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
     * 保存XML文件
     */
    private String saveXmlFile(byte[] xmlData, String xmlFileName, Long userId) throws IOException {
        String basePath = mapProperties.getStorage().getBasePath();
        String userPath = basePath + File.separator + userId;
        
        // 创建用户目录
        Path userDir = Paths.get(userPath);
        Files.createDirectories(userDir);
        
        // 生成文件名
        String fileName = UUID.randomUUID().toString() + "_" + xmlFileName;
        Path filePath = userDir.resolve(fileName);
        
        // 保存XML文件
        Files.write(filePath, xmlData);
        
        return filePath.toString();
    }
    
    /**
     * 从文件名获取地图名称
     */
    private String getMapNameFromFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "未命名地图";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
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
    
    /**
     * 字节数组包装的 MultipartFile 实现
     * 用于将文件内容转换为 MultipartFile 对象
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        
        private final byte[] content;
        private final String name;
        
        public ByteArrayMultipartFile(byte[] content, String name) {
            this.content = content;
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getOriginalFilename() {
            return name;
        }
        
        @Override
        public String getContentType() {
            return "application/octet-stream";
        }
        
        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }
        
        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }
        
        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }
        
        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }
}

