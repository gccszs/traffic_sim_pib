package edu.uestc.iscssl.itsbackend.Process.Statistic.Stat;

import edu.uestc.iscssl.itsbackend.Process.Statistic.BO.BO_Math;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.StatTask;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;
import edu.uestc.iscssl.itsbackend.Process.Vehicle;

import java.util.List;

/**
 * @Author: ma
 * @Description: 统计平均速度
 * @Date: 23:06:54 2019-5-18
 */
public class St_AveSpeed extends Stat_Base {

    public InfoStat Execute(Info info, StatTask statTask, InfoStat oldInfoStat){
        InfoStat infoStat = oldInfoStat;

        double speed_ave=0.0;
        double speed_max=0.0;
        double speed_min=999999.99;

        List<Vehicle> vehicles = info.getVehicles();

        for (Vehicle v:vehicles){
            double s = v.getSpeed();
            if(speed_max<s)speed_max=s;
            if(speed_min>s)speed_min=s;
            speed_ave+=s;
        }
        speed_ave/= vehicles.size();
        infoStat.speed_min = BO_Math.ppt_to_kmph(speed_min);
        infoStat.speed_max = BO_Math.ppt_to_kmph(speed_max);
        infoStat.speed_ave = BO_Math.ppt_to_kmph(speed_ave);
        return infoStat;
    }
}