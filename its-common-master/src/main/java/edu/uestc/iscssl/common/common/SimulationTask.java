package edu.uestc.iscssl.common.common;

import edu.uestc.iscssl.common.params.webParm;

import java.io.Serializable;

public class SimulationTask implements Serializable {
    long userId;
    String simulationId;
    int priority;
    int step;
    webParm webParam;
    public SimulationTask(long userId, String simulationId,int step,webParm webParam) {
        this.userId = userId;
        this.simulationId = simulationId;
        this.priority=5;
        this.step=step;
        this.webParam=webParam;
    }

    public webParm getWebParam() {
        return webParam;
    }

    public void setWebParam(webParm webParam) {
        this.webParam = webParam;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setSimulationId(String simulationId) {
        this.simulationId = simulationId;
    }

    public long getUserId() {
        return userId;
    }

    public String getSimulationId() {
        return simulationId;
    }
}
