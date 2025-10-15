package edu.uestc.iscssl.itsbackend.utils;

import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.List;

@Document
public class DataInfo {
    @Id
    ObjectId id;
    String simulationId;
    Info info;
    List<Info> infos = new ArrayList<>();
    InfoStat infoStat;
    List<InfoStat> infoStats = new ArrayList<>();

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getSimulationId() {
        return simulationId;
    }

    public void setSimulationId(String simulationId) {
        this.simulationId = simulationId;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.infos.add(info);
    }

    public InfoStat getInfoStat() {
        return infoStat;
    }

    public void setInfoStat(InfoStat infoStat) {
        this.infoStats.add(infoStat);
    }

    public DataInfo(String simulationId){
        this.simulationId = simulationId;
    }
}
