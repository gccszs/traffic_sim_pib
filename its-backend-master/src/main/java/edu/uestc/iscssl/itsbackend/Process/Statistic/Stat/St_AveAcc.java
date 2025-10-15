package edu.uestc.iscssl.itsbackend.Process.Statistic.Stat;

import edu.uestc.iscssl.itsbackend.Process.Statistic.BO.BO_Math;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.StatTask;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;
import edu.uestc.iscssl.itsbackend.Process.Vehicle;

import java.util.List;

/**
 * @Author: ma
 * @Description: 统计加速度
 * @Date: 13:17:06 2019-5-23
 */
public class St_AveAcc extends Stat_Base{

    public InfoStat Execute(Info info, StatTask statTask,InfoStat oldInfoStat){
        InfoStat infoStat = oldInfoStat;
        double acc_ave=0.0;
        double acc_max=0.0;
        double acc_min=999999.99;
        List<Vehicle> vehicles = info.getVehicles();

        for (Vehicle v:vehicles){
            double s = v.getAcceleration();
            if(acc_max<s)acc_max=s;
            if(acc_min>s)acc_min=s;
            acc_ave+=s;
        }
        acc_ave/= vehicles.size();
        infoStat.acc_min = BO_Math.ppst_to_mpss(acc_min);
        infoStat.acc_max = BO_Math.ppst_to_mpss(acc_max);
        infoStat.acc_ave = BO_Math.ppst_to_mpss(acc_ave);
        return infoStat;
    }
}