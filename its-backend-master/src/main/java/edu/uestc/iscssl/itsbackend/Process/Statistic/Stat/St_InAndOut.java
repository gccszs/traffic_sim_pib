package edu.uestc.iscssl.itsbackend.Process.Statistic.Stat;

import edu.uestc.iscssl.itsbackend.Process.Statistic.BO.BO_Math;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.BaseCross;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.Baseline;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.StatTask;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info_CrossFlow;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info_Flow;
import edu.uestc.iscssl.itsbackend.Process.Vehicle;

import java.util.*;

/**
 * @Author: ma
 * @Description: 统计出入车辆
 * @Date: 23:06:37 2019-5-18
 */
public class St_InAndOut extends Stat_Base {

    public InfoStat Execute(Info info, StatTask statTask,InfoStat oldInfoStat){



        InfoStat infoStat = oldInfoStat; //待填写的infoStat
        Map<String,Integer>map_F1 = new HashMap<String, Integer>();//车道和流量的映射
        Map<Integer,Integer>map_C1 = new HashMap<Integer, Integer>();//道口和流量的映射
        for(Baseline b:statTask.getMap().getBaselines()){//统计前先把infoStat里的车道位置划分好，都创建好新的对象
            Info_Flow.Info_Flow_Baseline baseline = new Info_Flow.Info_Flow_Baseline();
            baseline.Road_Id = b.getNum();
            baseline.flow  = 0.0;
            for(int i = 0;i<b.getMaxRight();i++){
                Info_Flow.Info_Flow_Lane lane = new Info_Flow.Info_Flow_Lane();
                lane.Lane_Id = 2*i;
                lane.flow = 0.0;
                baseline.lanes.add(lane);
                map_F1.put(""+baseline.Road_Id+","+lane.Lane_Id,0);
            }
            for(int i = 0;i<b.getMaxLeft();i++){
                Info_Flow.Info_Flow_Lane lane = new Info_Flow.Info_Flow_Lane();
                lane.Lane_Id = 2*i+1;
                lane.flow = 0.0;
                baseline.lanes.add(lane);
                map_F1.put(""+baseline.Road_Id+","+lane.Lane_Id,0);
            }
            infoStat.global.flow.baselines.add(baseline);
        }
        for (BaseCross b:statTask.getMap().getBaseCrosses()){//根据道路，根据道口也要
            Info_CrossFlow.Info_Flow_BaseCross baseCross = new Info_CrossFlow.Info_Flow_BaseCross();
            baseCross.Cross_ID = b.getNum();
            baseCross.flow = 0.0;
            map_C1.put(baseCross.Cross_ID,0);
            infoStat.global.cross_flow.baseCrosses.add(baseCross);
        }
        int in; //本帧入车数
        int out; //本帧出车数
        int cars = 0; //本帧车数
        List<Vehicle> vehicles = info.getVehicles();//本帧车辆集合
        List<Integer> map1 = new LinkedList<Integer>(); //本帧车辆集合 待计算
        List<Integer> map0 = new LinkedList<Integer>(); //上一帧车辆集合 待计算


        for(Vehicle v:statTask.mid1){

            String roadLane = (""+v.getRoadId()+","+v.getLane(statTask)); //把当前车的车道号标注出来："<道路号>,<车道号>"
            int getStatus = statTask.getMap().getStatus(v.getRoadId()); //车辆行驶状态 1为在路上 2在道口上 -1远离地图
            int roadID = v.getRoadId();
            if(getStatus==-1 || (getStatus==1 && roadLane.contains("-")))continue;//不统计不在道上的车 lane为-1也能表示远离道路
            map0.add(v.getVehicleId());//上一步对象集合//1
            boolean flag_continue = false; //是否两步跳出
            for(Vehicle j:vehicles){
                if(v.equals(j)){  //当前帧车辆与上一帧车辆重合
                    if(v.getRoadId()!=j.getRoadId()){ //道路号发生改变时统计道路流量
                        if (getStatus == 1)
                            map_F1.put(roadLane, map_F1.get(roadLane) + 1);
                        else if (getStatus == 2)
                            map_C1.put(roadID, map_C1.get(roadID) + 1);

                    }
                    flag_continue = true; //如果重合，不进行驶出地图判断
                    break;//不进行下一步判断，两步跳出
                }
            }
            if(flag_continue || getStatus==-1){
                continue;
            }//这里是驶出地图判断
            if( getStatus==1) //上一帧与当前帧车辆号不重合的时候
                map_F1.put(roadLane,map_F1.get(roadLane)+1);
            else if(getStatus==2)
                map_C1.put(roadID,map_C1.get(roadID)+1);//应该不会从道口驶出地图吧。。。道口四周都是道路

        }

        for(Vehicle v:vehicles){
            map1.add(v.getVehicleId());//当前对象集合//1
            cars++;
        }
        for (String k:map_F1.keySet()){
            statTask.map_flows.get(k).add(map_F1.get(k));
        }
        for (Integer k:map_C1.keySet()){
            statTask.map_flowsCr.get(k).add(map_C1.get(k));
        }
        int count1 = map1.size();
        int count0 = map0.size();
        for(int a:map0)
            for(int i=0;i<map1.size();i++)
                if(a==map1.get(i)){
                    map1.remove(i);//继续用重合方法统计入车数量
                    break;//remove完之后本帧map1集合里只剩下新车号
                }
        in = map1.size();
        out = (count0 - count1 + in);
        /*
        * 通过当次info和上一次info比较，车号，
        * 上次有这次没有的车号，记为驶出，
        * 上次没有这次有的车号，记为驶入
        * */

        double capasity = 0.0;
        double load = 0.0;
        statTask.car_Flow.add((Integer)out);
        for (Integer i:statTask.car_Flow){
            capasity += (i*1.0);
        }
        capasity/=statTask.car_Flow.size();
        statTask.car_Load.add((Integer)in);
        for (Integer i:statTask.car_Load){
            load += (i*1.0);
        }
        load/=statTask.car_Load.size();//通过缓冲区求平均值算出即时流量

        //把结果写进去
        for (String k:statTask.map_flows.keySet()){
            double ins = 0.0;
            List<Integer>Ie = statTask.map_flows.get(k);
            if(!Ie.isEmpty()){
                for (Integer a:Ie){
                    ins+=(a*1.0);
                }
                ins/=Ie.size();
            }
            ins = BO_Math.flowpt_to_(ins,60);
            infoStat.global.flow.put(k,ins);
        }
        for (Integer k:statTask.map_flowsCr.keySet()){
            double ins = 0.0;
            List<Integer>Ie = statTask.map_flowsCr.get(k);
            if(!Ie.isEmpty()){
                for (Integer a:Ie){
                    ins+=(a*1.0);
                }
                ins/=Ie.size();
            }
            ins = BO_Math.flowpt_to_(ins,60);
            infoStat.global.cross_flow.put(k,ins);
        }
        infoStat.car_number = cars;
        infoStat.car_in = in;
        infoStat.car_out = out;
        infoStat.jam_index = cars*100.0/statTask.roadCap;
        infoStat.global.cars_in = BO_Math.flowpt_to_(load,60);
        infoStat.global.cars_out = BO_Math.flowpt_to_(capasity,60);
        return infoStat;
    }
}