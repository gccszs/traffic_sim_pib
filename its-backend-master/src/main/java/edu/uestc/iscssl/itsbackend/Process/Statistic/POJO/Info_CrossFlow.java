package edu.uestc.iscssl.itsbackend.Process.Statistic.POJO;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cv_shandong on 2019/6/18.
 */
public class Info_CrossFlow {
    public static class Info_Flow_BaseCross {

        @Expose
        public int Cross_ID;

        @Expose
        public double flow;

        public Info_Flow_BaseCross(){}

        @Override
        public String toString(){

            String rtn = ("{");
            rtn+=  ("\"Road_Id \":"+Cross_ID );
            rtn+=  (",\"flow \":"+String.format("%.2f",flow ));
            rtn+= ("}");
            return rtn;

        }

    }

    @Expose
    public double flow_ave = 0;
    //@Expose
    public List<Info_Flow_BaseCross> baseCrosses = new ArrayList<Info_Flow_BaseCross>();
    /*
    * 覆盖Object
    * */
    @Override
    public String toString(){
        String data = ("[");
        for(Info_Flow_BaseCross l:baseCrosses ){
            if(!data .equals("["))data +=",";
            data+=l;
        }
        data +="]";
        String rtn = "{\"flow_ave\":"+flow_ave+",";
        rtn += "\"data\":"+data+"}";

        return rtn;
    }

    public void put(int Cross_ID,double flow){
        int size = baseCrosses.size();
        for(Info_Flow_BaseCross i:baseCrosses){
            if(Cross_ID==i.Cross_ID){
                i.flow=flow;
                break;
            }
        }
        flow_ave +=(flow/(size*1.0));
    }
}