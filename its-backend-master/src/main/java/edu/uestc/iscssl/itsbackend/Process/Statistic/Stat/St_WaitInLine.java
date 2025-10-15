package edu.uestc.iscssl.itsbackend.Process.Statistic.Stat;

import edu.uestc.iscssl.itsbackend.Process.Statistic.BO.BO_Math;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.StatTask;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStatGlobal;
import edu.uestc.iscssl.itsbackend.Process.Vehicle;

import java.util.*;

/**
 * @Author: ma
 * @Description: 统计排队长度
 * @Date: 22:50:46 2019-5-20
 */
public class St_WaitInLine extends Stat_Base {

    public InfoStat Execute(Info info, StatTask statTask, InfoStat oldInfoStat){
        InfoStat infoStat = oldInfoStat ;
        List<Vehicle> vehicles = info.getVehicles();//当前步的车辆集合
        int countLow = 0;//统计低速车数
        double countLineMax = 0;//最长队长
        double countLineMin = 99999.99;//最短队长
        double countLineAve = 0.0;//平均队长统计低速车数
        int countTimeMax = 0;//最长队长
        int countTimeMin = 99999;//最短队长
        double countTimeAve = 0.0;//平均队长

        int delayTimeMax = 99999;//最大延迟时间
        int delayTimeMin = 0;//最小延迟时间
        double delayTimeAve = 0.0;//平均延迟时间

        Map<String,Double> countWait = new HashMap<String, Double>();//每个车道的队长集合
        //Map<String,Integer> countWait_Time = new HashMap<String, Integer>();//每个车道的排队时间集合
        Map<Integer,Integer> countDelay_Time = new HashMap<Integer, Integer>();//延迟时间

        Map<String,List<double[]>>dpdl = new HashMap<String, List<double[]>>();//当前车道的低速车位置
        for(Vehicle v:vehicles){ //这个for是收集低速车的
            //条件：速度够低，而且在正路上
            if(statTask.getMap().getStatus(v.getRoadId())==1){
                int la = v.getLane(statTask);//获得车道号（不分道路号）
                if (v.getSpeed()<LOW_SPEED){//只统计低速车
                    int rd = v.getRoadId();//没有偏离则开始统计，首先获得道路号
                    String roadLane = ""+rd+","+la;//统一标识： 道路号,车道号
                    if(!dpdl.containsKey(roadLane))dpdl.put(roadLane,new ArrayList<double[]>());
                    dpdl.get(roadLane).add(new double[]{v.getxPosition(),v.getyPosition()});
                    countLow++;//总的低速车辆数统计+1
                }

            }
            //if (la<0)continue;//这是车辆偏离道路的情况，不统计

        }
        for (String k:dpdl.keySet()){
            double put = BO_Math.maxQueue(dpdl.get(k));
            if(put<0.1)continue;
            countWait.put(k,put);
        }


        for(Vehicle v:vehicles){ //这个for是收集车辆延误时间的
            //条件：速度低于LOW_SPEED
            if (v.getSpeed()<LOW_SPEED){
                countDelay_Time.put(v.getVehicleId(),1);
            }
        }

        for(int key:countDelay_Time.keySet()){//这里的key即车辆Id
            if(statTask.com_delay.keySet().contains(key)){
                statTask.com_delay.put(key,statTask.com_delay.get(key)+1);
            }
            else{
                statTask.com_delay.put(key,1);
            }
        }

        if(statTask.com_delay.size() == 0){
            delayTimeMax = 0;//最大延迟时间
            delayTimeMin = 0;//最小延迟时间
            delayTimeAve = 0.0;//平均延迟时间
        }
        if(statTask.com_delay.size() != 0){
            Collection delay_value = statTask.com_delay.values();
            Object[] obj = delay_value.toArray();
            Arrays.sort(obj);
            delayTimeMin = Integer.parseInt(obj[0].toString());
            delayTimeMax = Integer.parseInt(obj[obj.length-1].toString());
            int sumTime = 0;
            for(int i=0;i<obj.length;i++){
                sumTime += Integer.parseInt(obj[i].toString());
            }
            delayTimeAve = sumTime/obj.length;

        }

        Map<String,Double>temp = new HashMap<String, Double>();
        Map<String,Integer>temp_T = new HashMap<String, Integer>();
        for (String s:statTask.incomp_wait.keySet()){//上一步未完成的排队中
            if(!countWait.containsKey(s)||countWait.get(s)<0.1) {//在本次排队中没有数据,或者没排队
                statTask.comp_wait.add(statTask.incomp_wait.get(s));//则判断排队完成，计入已完成的排队
                statTask.comp_wait_time.add(statTask.incomp_wait_time.get(s));//则判断排队完成，计入已完成的排队时间
            }
            else {
                temp.put(s,statTask.incomp_wait.get(s));//否则继续加入未完成的队列中处理
                temp_T.put(s,statTask.incomp_wait_time.get(s));//否则继续加入未完成的队列中处理
            }

        }
        statTask.incomp_wait = temp;//新队列赋值给上一步排队数据
        statTask.incomp_wait_time = temp_T;//新队列赋值给上一步排队数据
        for(String s:countWait.keySet()){//本步排队中
            if(!statTask.incomp_wait.containsKey(s)){
                statTask.incomp_wait.put(s,countWait.get(s));
                statTask.incomp_wait_time.put(s,1);
            }
            //如果没有则长度加进去，时间放个1
            else {
                statTask.incomp_wait_time.put(s,statTask.incomp_wait_time.get(s)+1);
                if(statTask.incomp_wait.get(s)<countWait.get(s))statTask.incomp_wait.put(s,countWait.get(s));
            }
            //有的话判断是不是更大，更大才更新，时间总是+1
        }


        for (double si:statTask.incomp_wait.values()){
            double i = si;
            countLineAve+=i;
            if(i>=countLineMax)countLineMax=i;
            if(i<countLineMin)countLineMin=i;
        }  //所有的队长，队时，在一起算最大最小平均值
        for (double si:statTask.comp_wait){
            double i = si;
            countLineAve+=i;
            if(i>=countLineMax)countLineMax=i;
            if(i<countLineMin)countLineMin=i;
        }
        for (Integer i:statTask.incomp_wait_time.values()){
            countTimeAve+=i;
            if(i>=countTimeMax)countTimeMax=i;
            if(i<countTimeMin)countTimeMin=i;
        }
        for (Integer i:statTask.comp_wait_time){
            countTimeAve+=i;
            if(i>=countTimeMax)countTimeMax=i;
            if(i<countTimeMin)countTimeMin=i;
        }
        if((!statTask.incomp_wait.isEmpty())||(!statTask.comp_wait.isEmpty())){
            countLineAve/=(statTask.incomp_wait.size()+statTask.comp_wait.size());
            countTimeAve/=(statTask.incomp_wait_time.size()+statTask.comp_wait_time.size());
        }
        else{
            countLineAve = 0.0;
            countTimeAve = 0.0;
        }
        if (countLineMin>99000)countLineMin=0;
        if (countTimeMin>99000)countTimeMin=0;

        infoStat.low_speed = countLow;
        infoStat.global.queue_length_min = BO_Math.p_to_m(countLineMin);
        infoStat.global.queue_length_max = BO_Math.p_to_m(countLineMax);
        infoStat.global.queue_length_ave = BO_Math.p_to_m(countLineAve);
        infoStat.global.queue_time_min = BO_Math.t_to_s(countTimeMin*1.0);
        infoStat.global.queue_time_max = BO_Math.t_to_s(countTimeMax*1.0);
        infoStat.global.queue_time_ave = BO_Math.t_to_s(countTimeAve);

        infoStat.global.delay_max = BO_Math.t_to_s(delayTimeMax*1.0);
        infoStat.global.delay_min = BO_Math.t_to_s(delayTimeMin*1.0);
        infoStat.global.delay_ave = BO_Math.t_to_s(delayTimeAve);
        return infoStat;
    }
}