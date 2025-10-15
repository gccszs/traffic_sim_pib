package edu.uestc.iscssl.itsbackend.Process.Statistic.POJO;

import com.google.gson.annotations.Expose;

/**
 * Created by cv_shandong on 2019/6/5.
 */
public class InfoStatGlobal {

    @Expose
    public double cars_in;
    @Expose
    public double cars_out;

    @Expose
    public double queue_length_min;
    @Expose
    public double queue_length_max;
    @Expose
    public double queue_length_ave;
    @Expose
    public double queue_time_min;
    @Expose
    public double queue_time_max;
    @Expose
    public double queue_time_ave;

    @Expose
    public int stop_max;
    @Expose
    public int stop_min;
    @Expose
    public double stop_ave;


    @Expose
    public double delay_max;
    @Expose
    public double delay_min;
    @Expose
    public double delay_ave;


    @Expose
    public Info_CrossFlow cross_flow = new Info_CrossFlow();
    @Expose
    public Info_Flow flow = new Info_Flow();

    @Override
    public String toString(){
        String rtn = ("{");

        rtn+=  ("\"cars_in \":"+String.format("%.2f",cars_in ));
        rtn+= (",\"cars_out \":"+String.format("%.2f",cars_out ));

        rtn+= (",\"queue_length_min \":"+String.format("%.2f",queue_length_min));
        rtn+= (",\"queue_length_max \":"+String.format("%.2f",queue_length_max));
        rtn+= (",\"queue_length_ave \":"+String.format("%.2f",queue_length_ave));
        rtn+= (",\"queue_time_min \":"+String.format("%.2f",queue_time_min));
        rtn+= (",\"queue_time_max \":"+String.format("%.2f",queue_time_max));
        rtn+= (",\"queue_time_ave \":"+String.format("%.2f",queue_time_ave));

        rtn+= (",\"stop_max \":"+stop_max);
        rtn+= (",\"stop_min \":"+stop_min);
        rtn+= (",\"stop_ave \":"+String.format("%.2f",stop_ave));


        rtn+= (",\"delay_max \":"+String.format("%.2f",delay_max));
        rtn+= (",\"delay_min \":"+String.format("%.2f",delay_min));
        rtn+= (",\"delay_ave \":"+String.format("%.2f",delay_ave));

        rtn+= (",\"cross_flow \":"+cross_flow);
        rtn+= (",\"flow \":"+flow);

        rtn+= ("}");

        return rtn;
    }
}