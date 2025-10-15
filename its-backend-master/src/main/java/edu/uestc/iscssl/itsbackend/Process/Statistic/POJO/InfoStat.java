package edu.uestc.iscssl.itsbackend.Process.Statistic.POJO;

import com.google.gson.annotations.Expose;

/**
 * @Author: ma
 * @Description: 统计内容，需要输出
 * @Date: 16:59:15 2019年6月4日
 */
public class InfoStat
{
    //private int step;不知道用不用
    @Expose
    public double speed_min;
    @Expose
    public double speed_max;
    @Expose
    public double speed_ave;
    @Expose
    public double acc_min;
    @Expose
    public double acc_max;
    @Expose
    public double acc_ave;

    @Expose
    public int car_number;
    @Expose
    public int car_in;
    @Expose
    public int car_out;

    @Expose
    public int low_speed;
    @Expose
    public double jam_index;

    @Expose
    public InfoStatGlobal global = new InfoStatGlobal();




    public String format(int step){
        String rtn = ("{\"step\":"+step+",\"statistic\":{");
        rtn+= ("\"speed_min\":"+String.format("%.2f",speed_min)+",");
        rtn+= ("\"speed_max\":"+String.format("%.2f",speed_max)+",");
        rtn+= ("\"speed_ave\":"+String.format("%.2f",speed_ave)+",");
        rtn+= ("\"acc_min\":"+String.format("%.2f",acc_min)+",");
        rtn+= ("\"acc_max\":"+String.format("%.2f",acc_max)+",");
        rtn+= ("\"acc_ave\":"+String.format("%.2f",acc_ave)+",");

        rtn+= ("\"car_number \":"+car_number +",");
        rtn+= ("\"car_in \":"+car_in +",");
        rtn+= ("\"car_out \":"+car_out +",");

        rtn+= ("\"low_speed \":"+low_speed +",");
        rtn+= ("\"jam_index \":"+String.format("%.2f",jam_index) +",");


        rtn+= ("\"Global \":"+global +"}}");

        return rtn;
    }


}