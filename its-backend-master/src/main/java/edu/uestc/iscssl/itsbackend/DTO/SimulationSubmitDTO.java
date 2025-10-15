package edu.uestc.iscssl.itsbackend.DTO;

import edu.uestc.iscssl.common.params.ODParm;
import edu.uestc.iscssl.common.params.distributionParm;
import edu.uestc.iscssl.common.params.signalParm;

import java.util.List;

/**
 * 仿真任务提交DTO
 * 用于Controller层接收前端提交的仿真任务参数
 */
public class SimulationSubmitDTO {
    private String experimentId;
    private long userId;
    private int mapId;
    private int step;
    private int type; // 实验类型
    private String note;
    private List<ODParm> od;
    private List<signalParm> signal;
    private int laneNum;
    
    // 以下字段由后端处理，前端无需提供
    private int controllerNum;
    private String filePath;
    
    // Getter methods
    public String getExperimentId() {
        return experimentId;
    }
    
    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public int getMapId() {
        return mapId;
    }
    
    public void setMapId(int mapId) {
        this.mapId = mapId;
    }
    
    public int getStep() {
        return step;
    }
    
    public void setStep(int step) {
        this.step = step;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public List<ODParm> getOd() {
        return od;
    }
    
    public void setOd(List<ODParm> od) {
        this.od = od;
    }
    
    public List<signalParm> getSignal() {
        return signal;
    }
    
    public void setSignal(List<signalParm> signal) {
        this.signal = signal;
    }
    
    public int getLaneNum() {
        return laneNum;
    }
    
    public void setLaneNum(int laneNum) {
        this.laneNum = laneNum;
    }
    
    public int getControllerNum() {
        return controllerNum;
    }
    
    public void setControllerNum(int controllerNum) {
        this.controllerNum = controllerNum;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}