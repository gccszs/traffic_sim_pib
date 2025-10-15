package edu.uestc.iscssl.itsengine;

import edu.uestc.iscssl.common.params.*;

public class Class2Structures {
    public static Structures.WebParamStructure.ByReference transform(webParm webParm){
        Structures.WebParamStructure.ByReference webParamStructure=new Structures.WebParamStructure.ByReference();
        webParamStructure.roadNum=webParm.getRoadNum();
        webParamStructure.laneNum=webParm.getLaneNum();
        webParamStructure.controllerNum=webParm.getControllerNum();
        webParamStructure.vehicleChangeLaneModelNum = webParm.getVehicleChangeLaneModelNum();
        webParamStructure.vehicleFollowModelNum=webParm.getVehicleFollowModelNum();
        //TrafficFlowParmStructure[] trafficFlowParms=new TrafficFlowParmStructure[webParm.getFlow().length];

        int i=0;
        for (trafficFlowParm parm:webParm.getFlow()){
            webParamStructure.flow[i]=new Structures.TrafficFlowParmStructure();
            webParamStructure.flow[i].demand=parm.getDemand();
            webParamStructure.flow[i].policy=parm.getPolicy();
            webParamStructure.flow[i].roadID=parm.getRoadID();
            webParamStructure.flow[i].extra=parm.getExtra();
            i++;
        }
        //webParamStructure.flow=trafficFlowParms;
        //OdStructure[] odStructures=new OdStructure[webParm.getOd().length];
        i=0;
        for (ODParm odParm:webParm.getOd()){
            webParamStructure.od[i]=new Structures.OdStructure();
            webParamStructure.od[i].orgin=odParm.getOrgin();
           // DistributionStructure[] distributionStructure=new DistributionStructure[odParm.getDist().length];
            int j=0;
            for (distributionParm distributionParm:odParm.getDist()){
                webParamStructure.od[i].dist[j]=new Structures.DistributionStructure();
                webParamStructure.od[i].dist[j].dest=distributionParm.getDest();
                webParamStructure.od[i].dist[j].percent=distributionParm.getPercent();
                j++;
            }
            i++;
           // odStructures[i].dist=distributionStructure;
        }
        //webParamStructure.od=odStructures;
       // SignalStructure[] signalStructures=new SignalStructure[webParm.getSignal().length];
        i=0;
        for (signalParm signalParm:webParm.getSignal()){
            webParamStructure.signal[i]=new Structures.SignalStructure();
            webParamStructure.signal[i].crossID=signalParm.getCrossID();
            webParamStructure.signal[i].cycleTime=signalParm.getCycleTime();
            webParamStructure.signal[i].ewLeft=signalParm.getEwLeft();
            webParamStructure.signal[i].ewStraight=signalParm.getEwStraight();
            webParamStructure.signal[i].snLeft=signalParm.getSnLeft();
            webParamStructure.signal[i].snStraight=signalParm.getSnStraight();
            i++;
        }
        //webParamStructure.signal=signalStructures;
        return webParamStructure;
    }
}
