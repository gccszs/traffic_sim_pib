package edu.uestc.iscssl.itsbackend.repository;


import edu.uestc.iscssl.itsbackend.domain.simulation.MapEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

public interface MapRepository extends JpaRepository<MapEntity,Integer> {
    @Override
    Page<MapEntity> findAll(Pageable pageable);

    @Override
    Optional<MapEntity> findById(Integer integer);

    Page<MapEntity> findByOwnerId(long ownerId, Pageable pageable);
    List<MapEntity> findByOwnerIdOrStatus(long ownerId, MapEntity.MAP_STATUS status);
    Page<MapEntity> findByOwnerIdAndName(long ownerId, String mapName, Pageable pageable);
    Page<MapEntity> findByStatus(MapEntity.MAP_STATUS status, Pageable pageable);
    Page<MapEntity> findByStatusAndName(MapEntity.MAP_STATUS status, String mapName, Pageable pageable);
    MapEntity findMapEntitiesById(Integer id);
    MapEntity findMapEntityByMapId(String mapId);
    List<MapEntity> findByName(String name);
    Page<MapEntity> findByName(String name, Pageable pageable);

    int countByName(String name);
    int countByMapId(String id);

    @Modifying
    @Query(value = "update map set status = :status where map_id = :mapId",nativeQuery = true)
    @Transactional
    void deleteMapById(@Param("mapId") String mapId,@Param("status") int status);

    @Query(value = "select visitStatus from map where map_id = :mapId",nativeQuery = true)
    int findMapStatusById(String mapId);
}
