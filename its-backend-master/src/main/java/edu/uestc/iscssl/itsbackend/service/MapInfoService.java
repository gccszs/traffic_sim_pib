package edu.uestc.iscssl.itsbackend.service;

import edu.uestc.iscssl.itsbackend.utils.MapInfo;
import org.bson.types.ObjectId;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 18:04 2019/6/27
 */
public interface MapInfoService {
    MapInfo addMapInfo(MapInfo mapInfo);
    MapInfo findMapInfoById(ObjectId id);

}
