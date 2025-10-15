package edu.uestc.iscssl.itsbackend.Process.Statistic.File;

import edu.uestc.iscssl.itsbackend.Process.Statistic.BO.BO_Math;
import edu.uestc.iscssl.itsbackend.Process.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: ma
 * @Description: 地图的数据格式 目前用不上
 * @Date: 01:49:08 2019-5-18
 */
public class SMap {
    private List<Baseline> baselines;
    private List<BaseCross> baseCrosses;
    private Map<Integer,Integer> pathStatus;
    private double maxRoadRange;

    public SMap (SMap_XML input){
        maxRoadRange = 40.0;
        pathStatus = new HashMap<Integer, Integer>();
        List<String> roads = input.getKeysFromStrings("Baseline","Points");
        List<String> roadsID = input.getKeysFromStrings("Baseline","Path_ID");
        List<String> linkPathID = input.getKeysFromStrings("Link","Path_ID");
        List<String> linkRoadID = input.getKeysFromStrings("Link","Road_ID");

        List<String> crossX = input.getKeysFromStrings("Cross","x");
        List<String> crossY = input.getKeysFromStrings("Cross","y");
        List<String> crossNo = input.getKeysFromStrings("Cross","Cross_Id");

        Map<String,String> path2road = new HashMap<String, String>();

        for (int i=0;i<linkPathID.size();i++){
            path2road.put(linkPathID.get(i),linkRoadID.get(i));
        }
        baselines = new ArrayList<Baseline>();
        baseCrosses = new ArrayList<BaseCross>();
        for (int i=0;i<roads.size();i++) {
            baselines.add(new Baseline(roads.get(i), Integer.parseInt(path2road.get(roadsID.get(i)))));
            pathStatus.put(Integer.parseInt(path2road.get(roadsID.get(i))),1);
        }
        for (int i=0;i<crossNo.size();i++) {
            baseCrosses.add(new BaseCross(
                    Double.parseDouble(crossX.get(i)),
                    Double.parseDouble(crossY.get(i)),
                    Integer.parseInt(crossNo.get(i)))
            );
            pathStatus.put(Integer.parseInt(crossNo.get(i)),2);
        }

    }

    public SMap (SMap_XML input,int lanes){
        maxRoadRange = 10.0*lanes;
        pathStatus = new HashMap<Integer, Integer>();
        List<String> roads = input.getKeysFromStrings("Baseline","Points");
        List<String> roadsID = input.getKeysFromStrings("Baseline","Path_ID");
        List<String> linkPathID = input.getKeysFromStrings("Link","Path_ID");
        List<String> linkRoadID = input.getKeysFromStrings("Link","Road_ID");

        List<String> crossX = input.getKeysFromStrings("Cross","x");
        List<String> crossY = input.getKeysFromStrings("Cross","y");
        List<String> crossNo = input.getKeysFromStrings("Cross","Cross_Id");

        Map<String,String> path2road = new HashMap<String, String>();

        for (int i=0;i<linkPathID.size();i++){
            path2road.put(linkPathID.get(i),linkRoadID.get(i));
        }
        baselines = new ArrayList<Baseline>();
        baseCrosses = new ArrayList<BaseCross>();
        for (int i=0;i<roads.size();i++){
            baselines.add(
                    new Baseline(
                            roads.get(i),Integer.parseInt(path2road.get(roadsID.get(i))),
                            lanes
                    )
            );
            pathStatus.put(Integer.parseInt(path2road.get(roadsID.get(i))),1);
        }
        for (int i=0;i<crossNo.size();i++){
            baseCrosses.add(new BaseCross(
                    Double.parseDouble(crossX.get(i)),
                    Double.parseDouble(crossY.get(i)),
                    Integer.parseInt(crossNo.get(i)),
                    4)
            );
            pathStatus.put(Integer.parseInt(crossNo.get(i)),2);
        }


    }

    @Override
    public String toString(){
        String rtn = "Map:";
        for(Baseline b:baselines){
            rtn += "\n";
            rtn += b;
        }
        for(BaseCross b:baseCrosses){
            rtn += "\n";
            rtn += b;
        }
        return rtn;
    }

    public double getMaxRoadRange(){
        return maxRoadRange;
    }

    public int getStatus(int RoadID){ //返回行驶状态 -1远离道路 1道路上 2道口上
        if(!pathStatus.containsKey(RoadID))return -1;
        return pathStatus.get(RoadID);
    }

    public List<Baseline> getBaselines(){
        return baselines;
    }

    public List<BaseCross> getBaseCrosses(){
        return baseCrosses;
    }

    public double dist_from_base(Vehicle vehicle){
        int rd = vehicle.getRoadId();
        Baseline baseline = null;
        for(Baseline b:baselines){
            if (b.getNum()==rd)
                baseline = b;
        }
        return BO_Math.dist_p2vs(baseline.getPoints(),new double[]{vehicle.getxPosition(),vehicle.getyPosition()});

    }

    public int lane_base(Vehicle vehicle ){
        Baseline baseline = null;
        for(Baseline b:baselines){
            if(b.getNum()==vehicle.getRoadId());
            baseline = b;
        }
        double dis = dist_from_base(vehicle);
        boolean right = (dis>0.0);
        dis = Math.abs(dis);
        double width = baseline.getWidth();
        if(right)for (int i=0;i<baseline.getMaxRight();i++){
            if( (width*i)<dis && dis<(width*(i+1))){
                return 2*i;
            }
        }
        else for (int i=0;i<baseline.getMaxLeft();i++){
            if( (width*i)<dis && dis<(width*(i+1))){
                return 2*i+1;
            }
        }
        if(right)return -2;
        return -1;


    }

}