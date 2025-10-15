package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.itsbackend.repository.mongo.MapInfoRepository;
import edu.uestc.iscssl.itsbackend.service.MapInfoService;
import edu.uestc.iscssl.itsbackend.utils.MapInfo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 18:08 2019/6/27
 */
@Service
public class MapInfoServiceImpl implements MapInfoService {
    @Autowired
    private MapInfoRepository mapInfoRepository;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MapInfo addMapInfo(MapInfo mapInfo) {
        return mapInfoRepository.save(mapInfo);
    }

    @Override
    public MapInfo findMapInfoById(ObjectId id) {
        return mapInfoRepository.findMapInfoById(id).get();
    }
}
