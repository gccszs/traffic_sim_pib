package edu.uestc.iscssl.itsengine.Class2Structure;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class OdStructure extends Structure {
    public Integer orgin;
    public DistributionStructure[] dist;

    public OdStructure() {
        dist=(DistributionStructure[]) new DistributionStructure().toArray(100);
    }

    @Override
    protected List getFieldOrder() {
        return Arrays.asList(new String[]{"orgin", "dist"});
    }


}
