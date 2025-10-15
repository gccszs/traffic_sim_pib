package edu.uestc.iscssl.itsbackend.Process.Statistic.Stat;

import edu.uestc.iscssl.itsbackend.Process.Statistic.File.StatTask;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;
import edu.uestc.iscssl.itsbackend.Process.Vehicle;

import java.util.Collection;
import java.util.List;

/**
 * @Author: ma
 * @Description: 统计停测次数
 * @Date: 13:16:41 2019-5-23
 */
public class St_Stop extends Stat_Base {

    public InfoStat Execute(Info info, StatTask statTask, InfoStat oldInfoStat){
        InfoStat infoStat = oldInfoStat;
        List<Vehicle> vehicles = info.getVehicles();
        for (Vehicle v:vehicles){
            int id = v.getVehicleId();
            if(!statTask.xid_stops.containsKey(id))statTask.xid_stops.put(id,0);
            if(v.getSpeed()<LOW_SPEED){
                for(Vehicle v2:statTask.mid1){
                    if(!v2.equals(id))continue;
                    if(v2.getSpeed()>LOW_SPEED){
                        statTask.xid_stops.put(id,statTask.xid_stops.get(id)+1);
                        break;
                    }
                }
            }
        }
        int stop_max = 0;
        int stop_min = 99999;
        double stop_ave = 0.0;
        Collection<Integer> stops = statTask.xid_stops.values();
        for(int s:stops){
            if(s<stop_min)stop_min=s;
            if(s>stop_max)stop_max=s;
            stop_ave +=s;
        }
        stop_ave/=(stops.size()*1.0);
        infoStat.global.stop_max = stop_max;
        infoStat.global.stop_min = stop_min;
        infoStat.global.stop_ave = stop_ave;
        return infoStat;
    }
}