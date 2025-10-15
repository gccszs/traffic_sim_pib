package edu.uestc.iscssl.itsbackend.VO;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentEntity;
import edu.uestc.iscssl.itsbackend.domain.simulation.SimulationEntity;

import jakarta.persistence.Id;
import java.io.Serializable;
import java.util.Date;

public class ExperimentVO implements Serializable {

    @Id
    private Integer id;
    private String experimentId;
    private String experimentName;
    private Integer type;
    private String mapName;
    private String userName;
    private String simId;
    private String reportId;
    private Date createTime;
    private int mapId;
    private int laneNum;

    public ExperimentVO(ExperimentEntity experimentEntity, SimulationEntity entity){

        this.id = experimentEntity.getId();
        this.experimentId = experimentEntity.getExperimentId();
        this.experimentName = experimentEntity.getExperimentName();
        this.type = experimentEntity.getType();
        this.mapName = experimentEntity.getMapName();
        this.setUserName(entity.getUserName());
        this.simId = experimentEntity.getSimId();
        this.reportId = experimentEntity.getReportId();
        this.createTime = experimentEntity.getCreateTime();
        this.mapId = entity.getMapId();
        this.laneNum = entity.getLaneNum();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }



    public String getSimId() {
        return simId;
    }

    public void setSimId(String simId) {
        this.simId = simId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getMapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }

    public int getLaneNum() {
        return laneNum;
    }

    public void setLaneNum(int laneNum) {
        this.laneNum = laneNum;
    }
}
