package edu.uestc.iscssl.itsbackend.utils;

import edu.uestc.iscssl.common.params.webParm;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;

@Document
public class ParamInfo {
    @Id
    ObjectId id;
    long userId;
    webParm webparm;
    String simulationId;
    String createTime;


    public String getSimulationId() {
        return simulationId;
    }

    public void setSimulationId(String simulationId) {
        this.simulationId = simulationId;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public webParm getWebparm() {
        return webparm;
    }

    public void setWebparm(webParm webparm) {
        this.webparm = webparm;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
