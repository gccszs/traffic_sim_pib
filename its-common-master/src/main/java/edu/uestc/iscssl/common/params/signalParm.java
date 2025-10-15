package edu.uestc.iscssl.common.params;

import java.io.Serializable;

public class signalParm implements Serializable {
    int crossID;
    int cycleTime;
    int ewStraight;
    int ewLeft;
    int snStraight;
    int snLeft;

    public int getCrossID() {
        return crossID;
    }

    public void setCrossID(int crossID) {
        this.crossID = crossID;
    }

    public int getCycleTime() {
        return cycleTime;
    }

    public void setCycleTime(int cycleTime) {
        this.cycleTime = cycleTime;
    }

    public int getEwStraight() {
        return ewStraight;
    }

    public void setEwStraight(int ewStraight) {
        this.ewStraight = ewStraight;
    }

    public int getEwLeft() {
        return ewLeft;
    }

    public void setEwLeft(int ewLeft) {
        this.ewLeft = ewLeft;
    }

    public int getSnStraight() {
        return snStraight;
    }

    public void setSnStraight(int snStraight) {
        this.snStraight = snStraight;
    }

    public int getSnLeft() {
        return snLeft;
    }

    public void setSnLeft(int snLeft) {
        this.snLeft = snLeft;
    }
}
