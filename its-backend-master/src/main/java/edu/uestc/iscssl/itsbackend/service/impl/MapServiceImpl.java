package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.itsbackend.VO.MapVO;
import edu.uestc.iscssl.itsbackend.domain.simulation.MapEntity;
import edu.uestc.iscssl.itsbackend.repository.MapRepository;
import edu.uestc.iscssl.itsbackend.service.MapService;
import edu.uestc.iscssl.itsbackend.service.UserService;
import edu.uestc.iscssl.itsbackend.utils.XmlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MapServiceImpl implements MapService {
    @Autowired
    MapRepository mapRepository;
    @Autowired
    UserService userService;

    @Override
    public List<MapVO> getMapsVO(long userId) {
        List<MapVO> results=new ArrayList<>();
        mapRepository.findByOwnerIdOrStatus(userId,MapEntity.MAP_STATUS.PUBLIC).forEach(entity -> results.add(new MapVO(entity)));
        return results;
    }

    @Override
    public Page<MapEntity> getAllMapsVO(Pageable pageable) {
        return mapRepository.findAll(pageable);
    }

    @Override
    public Page<MapEntity> getMapsVOByMapName(String mapName, Pageable pageable) {
        return mapRepository.findByName(mapName, pageable);
    }

    @Override
    public String getMapInfo(int mapId) throws IOException {
        return XmlUtils.xml2Json(getMapFilePath(mapId));
    }

    @Override
    public String getMapFilePath(int mapId) {
        try {
            return mapRepository.findById(mapId).get().getFilePath();
        }catch (NoSuchElementException e){
            System.out.println("no such mapId:"+mapId);
            e.printStackTrace();
        }
        return "single.xml";
    }

    @Override
    public MapEntity saveMap(MapEntity mapEntity) {
        mapRepository.saveAndFlush(mapEntity);
        return mapEntity;
    }

    @Override
    public long countByName(String name) {
        return mapRepository.countByName(name);
    }

    @Override
    public long countByMapId(String id) {
        return mapRepository.countByMapId(id);
    }

    @Override
    public void deleteMapById(String mapId, int status) {
        mapRepository.deleteMapById(mapId,status);
    }

    @Override
    public int findMapStatusById(String mapId) {
        return mapRepository.findMapStatusById(mapId);
    }

    @Override
    public MapEntity findMapEntitiesById(int id) {
        return mapRepository.findMapEntitiesById(id);
    }

    @Override
    public MapEntity findMapEntityByMapId(String mapId) {
        return mapRepository.findMapEntityByMapId(mapId);
    }

    @Override
    public Page<MapEntity> findByOwnerId(long ownerId, Pageable pageable) {
        return mapRepository.findByOwnerId(ownerId, pageable);
    }

    @Override
    public Page<MapEntity> findByOwnerIdAndMapName(long ownerId, String mapName, Pageable pageable) {
        return mapRepository.findByOwnerIdAndName(ownerId, mapName, pageable);
    }

    @Override
    public Page<MapEntity> findByStatus(MapEntity.MAP_STATUS status, Pageable pageable) {
        return mapRepository.findByStatus(status, pageable);
    }

    @Override
    public List<MapEntity> findByMapName(String mapName) {
        return mapRepository.findByName(mapName);
    }

    @Override
    public Page<MapEntity> findByStatusAndMapName(MapEntity.MAP_STATUS status, String mapName, Pageable pageable) {
        return mapRepository.findByStatusAndName(status, mapName, pageable);
    }
}
