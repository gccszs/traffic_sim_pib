package edu.uestc.iscssl.itsbackend.repository.mongo;

import edu.uestc.iscssl.itsbackend.utils.MapInfo;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 17:55 2019/6/27
 */
@Repository
public interface MapInfoRepository extends MongoRepository<MapInfo, String> {
    Optional<MapInfo> findMapInfoById(ObjectId id);
}
