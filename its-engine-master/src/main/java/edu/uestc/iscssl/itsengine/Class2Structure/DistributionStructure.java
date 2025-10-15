package edu.uestc.iscssl.itsengine.Class2Structure;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public  class DistributionStructure extends Structure {
    public Integer dest;
    public Float percent;

    @Override
    protected List getFieldOrder() {
        return Arrays.asList(new String[]{"dest", "percent"});
    }


}
