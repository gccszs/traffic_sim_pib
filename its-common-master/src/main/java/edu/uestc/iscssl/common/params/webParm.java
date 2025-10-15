package edu.uestc.iscssl.common.params;

import java.io.Serializable;

public class webParm implements Serializable {
    String experimentId;
    int mapId;
    int userId;
    int type;
    int roadNum;
    int laneNum;
    int controllerNum;
    int vehicleChangeLaneModelNum; // 1 - 匀加速换道模型； 2 - 自适应加速度换道模型
    trafficFlowParm[] flow;
    ODParm[] od;
    signalParm[] signal;
    String simulationName;
    int step;
    String note;
    String filePath;
    int vehicleFollowModelNum;

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public int getControllerNum() {
        return controllerNum;
    }

    public void setControllerNum(int controllerNum) {
        this.controllerNum = controllerNum;
    }

    public int getVehicleChangeLaneModelNum() {
        return vehicleChangeLaneModelNum;
    }

    public void setVehicleChangeLaneModelNum(int vehicleChangeLaneModelNum) {
        this.vehicleChangeLaneModelNum = vehicleChangeLaneModelNum;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getMapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSimulationName() {
        return simulationName;
    }

    public void setSimulationName(String simulationName) {
        this.simulationName = simulationName;
    }



    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getRoadNum() {
        return roadNum;
    }

    public void setRoadNum(int roadNum) {
        this.roadNum = roadNum;
    }

    public int getLaneNum() {
        return laneNum;
    }

    public void setLaneNum(int laneNum) {
        this.laneNum = laneNum;
    }

    public trafficFlowParm[] getFlow() {
        return flow;
    }

    public void setFlow(trafficFlowParm[] flow) {
        this.flow = flow;
    }

    public ODParm[] getOd() {
        return od;
    }

    public void setOd(ODParm[] od) {
        this.od = od;
    }

    public signalParm[] getSignal() {
        return signal;
    }

    public void setSignal(signalParm[] signal) {
        this.signal = signal;
    }

    public int getVehicleFollowModelNum() {
        return vehicleFollowModelNum;
    }

    public void setVehicleFollowModelNum(int vehicleFollowModelNum) {
        this.vehicleFollowModelNum = vehicleFollowModelNum;
    }

    public int getType() { return type; }

    public void setType(int experimentType) { this.type = experimentType; }
}
