package edu.uestc.iscssl.itsbackend.domain.simulation;


import edu.uestc.iscssl.common.common.SIMULATION_STATUS;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Table(name = "simulation")
@Entity
public class SimulationEntity implements Serializable {
    private long userId;
    @Id
    @Column(length = 20)
    private String id;

    private SIMULATION_STATUS status;
    private int step;
    private String engineManagerId;
    private String simulationName;
    private String note;
    private int mapId;
    private String userName;
    private Date createTime;
    private int laneNum;
    private String paramId;
    public SimulationEntity() {

    }

    public String getParamId() {
        return paramId;
    }

    public void setParamId(String paramId) {
        this.paramId = paramId;
    }

    public SimulationEntity(String id, long userId, int step, String simulationName, String note, int mapId , String userName, int laneNum,String paramId) {
        this.userId = userId;
        this.id = id;
        this.status=SIMULATION_STATUS.SUBMITTED;
        this.step=step;
        this.simulationName=simulationName;
        this.note=note;
        this.mapId=mapId;
        this.userName=userName;
        createTime=new Date();
        this.laneNum=laneNum;
        this.paramId=paramId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSimulationName() {
        return simulationName;
    }

    public void setSimulationName(String simulationName) {
        this.simulationName = simulationName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getUserId() {
        return userId;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getSimulaitionId() {
        return id;
    }

    public SIMULATION_STATUS getStatus() {
        return status;
    }

    public void setStatus(SIMULATION_STATUS status) {
        this.status = status;
    }

    public String getEngineManagerId() {
        return engineManagerId;
    }

    public void setEngineManagerId(String engineManagerId) {
        this.engineManagerId = engineManagerId;
    }

    public int getMapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getLaneNum() {
        return laneNum;
    }

    public void setLaneNum(int laneNum) {
        this.laneNum = laneNum;
    }
}
