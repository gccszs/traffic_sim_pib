package edu.uestc.iscssl.itsbackend.service;

import edu.uestc.iscssl.itsbackend.VO.MapVO;
import edu.uestc.iscssl.itsbackend.domain.simulation.MapEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface MapService {
    List<MapVO> getMapsVO(long userId);
    Page<MapEntity> getAllMapsVO(Pageable pageable);
    Page<MapEntity> getMapsVOByMapName(String mapName, Pageable pageable);
    String getMapInfo(int mapId) throws IOException;
    String getMapFilePath(int mapId);
    MapEntity saveMap(MapEntity mapEntity);
    long countByName(String name);//地图名重复
    long countByMapId(String id);
    void deleteMapById(String mapId, int status);
    int findMapStatusById(String mapId);
    MapEntity findMapEntitiesById(int id);
    MapEntity findMapEntityByMapId(String mapId);
    Page<MapEntity> findByOwnerId(long ownerId, Pageable pageable);
    Page<MapEntity> findByOwnerIdAndMapName(long ownerId, String mapName, Pageable pageable);
    Page<MapEntity> findByStatus(MapEntity.MAP_STATUS status, Pageable pageable);
    List<MapEntity> findByMapName(String mapName);
    Page<MapEntity> findByStatusAndMapName(MapEntity.MAP_STATUS status, String mapName, Pageable pageable);


}
