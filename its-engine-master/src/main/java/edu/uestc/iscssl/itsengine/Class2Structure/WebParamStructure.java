package edu.uestc.iscssl.itsengine.Class2Structure;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;


public class WebParamStructure extends Structure {
    public Integer roadNum;
    public Integer laneNum;
    public Integer vehicleChangeLaneModelNum;
    public Integer controllerNum;
    public TrafficFlowParmStructure[] flow;
    public OdStructure[] od;
    public SignalStructure[] signal;


    public WebParamStructure() {
        this.flow =(TrafficFlowParmStructure[])new TrafficFlowParmStructure().toArray(100);
        this.od =(OdStructure[]) new OdStructure().toArray(100);
        this.signal =(SignalStructure[])new SignalStructure().toArray(100);
    }

    @Override
    protected List getFieldOrder() {
        return Arrays.asList(new String[]{"roadNum", "laneNum","controllerNum","vehicleChangeLaneModelNum","flow","od","signal"});
    }


}
