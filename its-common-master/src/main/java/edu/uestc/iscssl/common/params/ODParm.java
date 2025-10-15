package edu.uestc.iscssl.common.params;

import java.io.Serializable;

public class ODParm implements Serializable {
    int orgin;
    distributionParm[] dist;

    public int getOrgin() {
        return orgin;
    }

    public void setOrgin(int orgin) {
        this.orgin = orgin;
    }

    public distributionParm[] getDist() {
        return dist;
    }

    public void setDist(distributionParm[] dist) {
        this.dist = dist;
    }
}
