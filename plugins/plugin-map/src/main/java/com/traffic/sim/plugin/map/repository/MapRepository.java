package com.traffic.sim.plugin.map.repository;

import com.traffic.sim.plugin.map.entity.MapEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 地图Repository
 * 
 * @author traffic-sim
 */
@Repository
public interface MapRepository extends JpaRepository<MapEntity, Long> {
    
    /**
     * 根据mapId查找地图
     */
    Optional<MapEntity> findByMapId(String mapId);
    
    /**
     * 根据所有者ID查找地图列表
     */
    List<MapEntity> findByOwnerId(Long ownerId);
    
    /**
     * 根据所有者ID和状态查找地图列表
     */
    List<MapEntity> findByOwnerIdAndStatus(Long ownerId, MapEntity.MapStatus status);
    
    /**
     * 根据所有者ID分页查询地图
     */
    Page<MapEntity> findByOwnerId(Long ownerId, Pageable pageable);
    
    /**
     * 根据所有者ID和地图名称模糊查询
     */
    @Query("SELECT m FROM MapEntity m WHERE m.ownerId = :ownerId AND m.name LIKE %:mapName%")
    Page<MapEntity> findByOwnerIdAndNameContaining(@Param("ownerId") Long ownerId, 
                                                    @Param("mapName") String mapName, 
                                                    Pageable pageable);
    
    /**
     * 根据所有者ID、状态和地图名称模糊查询
     */
    @Query("SELECT m FROM MapEntity m WHERE m.ownerId = :ownerId " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:mapName IS NULL OR m.name LIKE %:mapName%)")
    Page<MapEntity> findByOwnerIdAndStatusAndNameContaining(@Param("ownerId") Long ownerId,
                                                             @Param("status") MapEntity.MapStatus status,
                                                             @Param("mapName") String mapName,
                                                             Pageable pageable);
    
    /**
     * 查询公开地图（状态为PUBLIC）
     */
    @Query("SELECT m FROM MapEntity m WHERE m.status = :status " +
           "AND (:mapName IS NULL OR m.name LIKE %:mapName%)")
    Page<MapEntity> findPublicMaps(@Param("status") MapEntity.MapStatus status,
                                   @Param("mapName") String mapName,
                                   Pageable pageable);
    
    /**
     * 查询所有地图（管理员用）
     */
    @Query("SELECT m FROM MapEntity m WHERE (:mapName IS NULL OR m.name LIKE %:mapName%)")
    Page<MapEntity> findAllMaps(@Param("mapName") String mapName, Pageable pageable);
    
    /**
     * 统计用户的地图数量
     */
    long countByOwnerId(Long ownerId);
    
    /**
     * 统计用户指定状态的地图数量
     */
    long countByOwnerIdAndStatus(Long ownerId, MapEntity.MapStatus status);
}

