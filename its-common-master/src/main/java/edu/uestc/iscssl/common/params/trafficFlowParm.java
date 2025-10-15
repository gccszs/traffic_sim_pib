package edu.uestc.iscssl.common.params;

import java.io.Serializable;

public class trafficFlowParm implements Serializable {
    int roadID;
    int policy;
    int demand;
    double extra;
    public int getRoadID() {
        return roadID;
    }

    public void setRoadID(int roadID) {
        this.roadID = roadID;
    }

    public int getPolicy() {
        return policy;
    }

    public void setPolicy(int policy) {
        this.policy = policy;
    }

    public int getDemand() {
        return demand;
    }

    public void setDemand(int demand) {
        this.demand = demand;
    }

    public double getExtra() {
        return extra;
    }

    public void setExtra(double extra) {
        this.extra = extra;
    }
}
