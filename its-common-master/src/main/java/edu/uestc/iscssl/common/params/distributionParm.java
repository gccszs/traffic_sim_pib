package edu.uestc.iscssl.common.params;

import java.io.Serializable;

public class distributionParm implements Serializable {
    int dest;
    float percent;

    public int getDest() {
        return dest;
    }

    public void setDest(int dest) {
        this.dest = dest;
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }
}
