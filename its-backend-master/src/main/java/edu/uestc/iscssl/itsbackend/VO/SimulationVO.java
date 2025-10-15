package edu.uestc.iscssl.itsbackend.VO;

import edu.uestc.iscssl.common.common.SIMULATION_STATUS;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import java.util.Date;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 20:04 2019/6/24
 */
public class SimulationVO {

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
    private String SimulationId;
    private String mapName;
    private String userName;
    private Date createTime;
    private int laneNum;

    public int getLaneNum() {
        return laneNum;
    }

    public void setLaneNum(int laneNum) {
        this.laneNum = laneNum;
    }

    public String getSimulationId() {
        return SimulationId;
    }

    public void setSimulationId(String simulationId) {
        SimulationId = simulationId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SIMULATION_STATUS getStatus() {
        return status;
    }

    public void setStatus(SIMULATION_STATUS status) {
        this.status = status;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getEngineManagerId() {
        return engineManagerId;
    }

    public void setEngineManagerId(String engineManagerId) {
        this.engineManagerId = engineManagerId;
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


    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }
}
