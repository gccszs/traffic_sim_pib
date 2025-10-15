package edu.uestc.iscssl.itsbackend.repository.mongo;

import edu.uestc.iscssl.itsbackend.utils.DataInfo;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataInfoRepository extends MongoRepository<DataInfo,String>{

    Optional<DataInfo> findDataInfoById(ObjectId id);

}
