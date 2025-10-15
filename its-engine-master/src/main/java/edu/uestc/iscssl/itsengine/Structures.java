package edu.uestc.iscssl.itsengine;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class Structures {
    public static class WebParamStructure extends Structure {
        public Integer roadNum;
        public Integer laneNum;
        public Integer vehicleFollowModelNum;
        public Integer vehicleChangeLaneModelNum;
        public Integer controllerNum;
        public TrafficFlowParmStructure[] flow;
        public OdStructure[] od;
        public SignalStructure[] signal;
        public static class ByReference extends WebParamStructure implements Structure.ByReference{
        }

        public static class ByValue extends WebParamStructure implements Structure.ByValue{
        }


        public WebParamStructure() {
            this.flow =(TrafficFlowParmStructure[])new TrafficFlowParmStructure().toArray(100);
            this.od =(OdStructure[]) new OdStructure().toArray(100);
            this.signal =(SignalStructure[])new SignalStructure().toArray(100);
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"roadNum", "laneNum","controllerNum","vehicleChangeLaneModelNum","vehicleFollowModelNum","flow","od","signal"});
        }
    }
    public static class TrafficFlowParmStructure extends Structure {
        public Integer roadID;
        public Integer policy;
        public Integer demand;
        public double extra;
        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"roadID", "policy", "demand","extra"});
        }
    }
    public static class SignalStructure extends Structure {
        public  Integer crossID;
        public  Integer cycleTime;
        public  Integer ewStraight;
        public Integer ewLeft;
        public Integer snStraight;
        public Integer snLeft;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"crossID", "cycleTime","ewStraight","ewLeft","snStraight","snLeft"});
        }
    }
    public static class OdStructure extends Structure {
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
    public  static class DistributionStructure extends Structure {
        public Integer dest;
        public Float percent;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"dest", "percent"});
        }
    }
}
