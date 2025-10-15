package edu.uestc.iscssl.itsbackend.Process.Statistic.POJO;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cv_shandong on 2019/6/18.
 */
public class Info_Flow {
    public static class Info_Flow_Baseline {

        @Expose
        public int Road_Id;

        @Expose
        public double flow;

        @Expose
        public List<Info_Flow_Lane> lanes = new ArrayList<Info_Flow_Lane>();

        public Info_Flow_Baseline(){}

        @Override
        public String toString(){
            String array = "";
            for(Info_Flow_Lane l:lanes){
                if(!array.equals(""))array+=",";
                array+=l;
            }

            String rtn = ("{");
            rtn+=  ("\"Road_Id \":"+Road_Id );
            rtn+=  (",\"flow \":"+String.format("%.2f",flow ));
            rtn+=  (",\"lanes \":"+"[");
            rtn+= array;
            rtn+= ("]");
            rtn+= ("}");
            return rtn;

        }

    }

    public static class Info_Flow_Lane {

        @Expose
        public int Lane_Id;

        @Expose
        public double flow;

        public Info_Flow_Lane(){};

        @Override
        public String toString(){
            String rtn = ("{");
            rtn+=  ("\"Lane_Id \":"+Lane_Id);
            rtn+=  (",\"flow \":"+String.format("%.2f",flow ));
            rtn+= ("}");
            return rtn;

        }

    }


    @Expose
    public double flow_RD_ave = 0.0;

    @Expose
    public double flow_LA_ave = 0.0;

    //@Expose
    public List<Info_Flow_Baseline> baselines = new ArrayList<Info_Flow_Baseline>();
    /*
    * 覆盖Object
    * */
    @Override
    public String toString(){
        String data = ("[");
        for(Info_Flow_Baseline l:baselines){
            if(!data .equals("["))data +=",";
            data+=l;
        }
        data +="]";
        String rtn = "{\"flow_RD_ave\":"+flow_RD_ave+",";
        rtn += "\"flow_LA_ave\":"+flow_LA_ave+",";
        rtn += "\"data\":"+data+"}";


        return rtn;
    }

    public void put(String roadLane,double flow){
        int sizeR = baselines.size();
        int sizeL = 0;
        for(Info_Flow_Baseline i:baselines)
            sizeL+=i.lanes.size();

        String[] RL = roadLane.split(",");
        for(Info_Flow_Baseline i:baselines){
            if(RL[0].equals(""+i.Road_Id)){
                i.flow+=flow;
                flow_RD_ave += (flow/(sizeR*1.0));
                for(Info_Flow_Lane j:i.lanes){
                    if (RL[1].equals(""+j.Lane_Id)){
                        j.flow = flow;
                        flow_LA_ave += (flow/(sizeL*1.0));
                        break;
                    }
                }
                break;
            }
        }
    }
}