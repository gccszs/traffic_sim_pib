package edu.uestc.iscssl.itsbackend.repository.mongo;

import edu.uestc.iscssl.itsbackend.utils.ParamInfo;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParamInfoRepository extends MongoRepository<ParamInfo, String> {
    //WebParm findWebParmByParamId(ObjectId paramId);

/*    @Query(value="{''WebParm'.$id': id }")
    @Transactional*/
    Optional<ParamInfo> findWebParmById(ObjectId id);

    List<ParamInfo> findAll();
    Optional<ParamInfo> findAllById(ObjectId id);;
    Optional<ParamInfo> findParamInfoBySimulationId(String id);
}
