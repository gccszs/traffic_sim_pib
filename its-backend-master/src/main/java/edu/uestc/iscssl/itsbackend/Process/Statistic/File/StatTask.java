package edu.uestc.iscssl.itsbackend.Process.Statistic.File;

import edu.uestc.iscssl.itsbackend.Process.Statistic.BO.BO_BufferList;
import edu.uestc.iscssl.itsbackend.Process.Statistic.BO.BO_Math;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;
import edu.uestc.iscssl.itsbackend.Process.Statistic.Stat.Stat_Base;
import edu.uestc.iscssl.itsbackend.Process.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: ma
 * @Description: 仿真统计任务
 * @Date: 01:48:35 2019-5-18
 */
public class StatTask {

    public final double CAR_LENGTH = 8.0;
    private final int BUFFER_SIZE = 10;

    private SMap map;//地图文件 目前没用上 以后统计排队用的到(现在已经在统计排队了)
    public int e_steps = 0;//总步数，现在没在用


    /*St_InAndOut*/
    public double roadCap;//道路面车容量
    public Map<String,List<Integer>>map_flows;//每条车道的车流量
    public Map<Integer,List<Integer>>map_flowsCr;//每条道口的车流量
    public List<Integer> car_Flow;//近帧的车流量
    public List<Integer> car_Load;//近帧的车流量

    /*St_WaitInLine*/
    public Map<String,Double> incomp_wait;//未完成的队长
    public Map<String,Integer> incomp_wait_time;//未完成的队时
    public Map<String,double[]>last_car;//队列中最后一辆车坐标
    public List<Double> comp_wait;//完成的队长
    public List<Integer> comp_wait_time;//完成的队时
    public Map<Integer,Integer> com_delay;//对已发生的延迟车辆时间的统计

    /*St_Stop*/
    public Map<Integer,Integer> xid_stops;//停车次数统计

    /*St_InAndOut,St_Stop*/
    public List<Vehicle> mid1 ; //上一个info的车组

    public StatTask(SMap_XML xml){
        this.map = new SMap(xml);
        this._construct();
    }
    public StatTask(SMap_XML xml,int lanes){
        this.map = new SMap(xml,lanes);
        this._construct();
    }
    private void _construct(){
        /*Global*/
        e_steps = 0;

        /*St_InAndOut*/
        roadCap = (AllLength()/CAR_LENGTH);
        map_flows = new HashMap<String, List<Integer>>();
        map_flowsCr = new HashMap<Integer, List<Integer>>();
        car_Flow = new BO_BufferList<Integer>(BUFFER_SIZE);
        car_Load = new BO_BufferList<Integer>(BUFFER_SIZE);

        incomp_wait = new HashMap<String, Double>();
        incomp_wait_time = new HashMap<String, Integer>();
        last_car = new HashMap<String, double[]>();
        comp_wait = new ArrayList<Double>();
        comp_wait_time = new ArrayList<Integer>();
        com_delay = new HashMap<Integer, Integer>();

        /*St_Stop*/
        xid_stops = new HashMap<Integer, Integer>();

        /*St_InAndOut,St_Stop*/
        mid1 = new ArrayList<Vehicle>();

        for(Baseline b:map.getBaselines()){
            for(int i = 0;i<b.getMaxRight();i++){
                map_flows.put(""+b.getNum()+","+(2*i),new BO_BufferList<Integer>(BUFFER_SIZE));
            }
            for(int i = 0;i<b.getMaxLeft();i++) {
                map_flows.put("" + b.getNum() + "," + (2 * i +1 ), new BO_BufferList<Integer>(BUFFER_SIZE));
            }
        }
        for (BaseCross b:map.getBaseCrosses()){
            map_flowsCr.put(b.getNum(),new BO_BufferList<Integer>(BUFFER_SIZE));
        }//按照车道号初始化map
    }

    /**
     * @Description: 统计工作入口
     * @Params: info 输入一步仿真数据
     * @Return: 输出一步statistic数据，是json格式的字符串
     * */
    public InfoStat Execute(Info info){
        InfoStat rtn = new InfoStat();
        for(Stat_Base s:Stat_Base.stat_bases){  //已经在Stat_base处注册过所有的统计内容了，现在会挨个统计出来。
            try {
                rtn = s.Execute(info,this,rtn);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Stat_Base.Refresh(info,this);
        return rtn;
    }

    public SMap getMap(){
        return map;
    }

    /**
     * 计算容量的一部分
     */
    private double AllLength(){
        double rtn = 0.0;
        for(Baseline b:map.getBaselines()){
            int rds = b.getMaxLeft();
            rds += b.getMaxRight();
            double bLength = BO_Math.pArray_length(b.getPoints());
            rtn +=(rds*bLength);
        }
        return rtn;
    }
}