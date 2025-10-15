package edu.uestc.iscssl.itsbackend.Process.Statistic.Stat;

import edu.uestc.iscssl.itsbackend.Process.Statistic.File.StatTask;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;
import edu.uestc.iscssl.itsbackend.Process.Vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: ma
 * @Description: 所有统计功能
 * @Date: 23:06:48 2019-5-18
 */
public abstract class Stat_Base {

    protected static final double LOW_SPEED = 0.1;


    public static volatile List<Stat_Base> stat_bases; //这里储存已经注册的所有统计内容

    /**
     * @Description: 刷新一次上一步车辆数据
     * @Params: info 当前仿真数据
     * */
    public static void Refresh(Info info,StatTask statTask) {
        statTask.mid1 = new ArrayList<Vehicle>();
        for(Vehicle v:info.getVehicles())
            statTask.mid1.add(v);

    }
    /**
     * @Description: 注册所有的统计内容，启动统计服务器的时候，这个静态过程要被调用
     * */
    public static void init(){
        stat_bases = new ArrayList<Stat_Base>();
        stat_bases.add(new St_AveSpeed());
        stat_bases.add(new St_AveAcc());
        stat_bases.add(new St_InAndOut());
        stat_bases.add(new St_WaitInLine());
        stat_bases.add(new St_Stop());
    }

    /**
     * @Description: 输出数据
     * @Params: info 输入一步仿真数据 , statTask 进行该统计的任务
     * @Return: 输出一个或多个statistic数据，与其他部分组成json格式的字符串
     * */
    public abstract InfoStat Execute(Info info, StatTask statTask,InfoStat oldInfoStat);

}
