package edu.uestc.iscssl.itsbackend.Process;

import com.google.gson.annotations.Expose;

public class Element {
    @Expose(serialize = false,deserialize = false)
    private String simId;
    @Expose(serialize = false)
    private int stepNum;
    @Expose(serialize = false)
    private int correctFlag;
    @Expose
    private Vehicle vehicle;
    @Expose
    private Phase phase;
    @Expose(serialize = false)
    private boolean finished=false;
    public Element(String simId, int stepNum, int correctFlag, Vehicle vehicle, Phase phase) {
        this.simId = simId;
        this.stepNum = stepNum;
        this.correctFlag = correctFlag;
        this.vehicle = vehicle;
        this.phase = phase;

    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getSimId() {
        return simId;
    }

    public void setSimId(String simId) {
        this.simId = simId;
    }

    public int getStepNum() {
        return stepNum;
    }

    public void setStepNum(int stepNum) {
        this.stepNum = stepNum;
    }

    public int getCorrectFlag() {
        return correctFlag;
    }

    public void setCorrectFlag(int correctFlag) {
        this.correctFlag = correctFlag;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }
}
