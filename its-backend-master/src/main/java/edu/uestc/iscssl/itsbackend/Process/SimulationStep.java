package edu.uestc.iscssl.itsbackend.Process;

import com.google.gson.annotations.Expose;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class SimulationStep implements Serializable {
    @Expose
    private  int step;
    @Expose
    private List<Element> vehicles=new LinkedList<>();
    @Expose
    private List<Element> phases=new LinkedList<>();
    @Expose(serialize = false)
    private int finishedCount;
    @Expose(serialize = false)
    private boolean hasFinished=false;
    @Expose(serialize = false,deserialize = false)
    private SimpleDataBuilder db;
    @Expose
    private InfoStat infoStat;
    /**/

    public SimulationStep(int step, SimpleDataBuilder db) {
        this.step = step;
        this.db=db;
    }


    public InfoStat popStat(){
        return infoStat;
    }

    public void pushStat(InfoStat infoStat){
        this.infoStat = infoStat;
    }


    public int getStep(){
        return step;
    }

    public List<Element>getVehicles(){
        return vehicles;
    }

    public List<Element> getPhases(){
        return phases;
    }

    public void add(Element e){
        if (e.getVehicle()!=null)
            vehicles.add(e);
        else if (e.getPhase()!=null)
            phases.add(e);
        else throw new Error("仿真数据出错，请联系管理员");
    }
    public void setFinished() {
        if (this.finishedCount==0)
            this.finishedCount++;
        else{
            this.hasFinished=true;
            onFinished();
        }

    }
    public boolean hasFinished(){
        return this.hasFinished;
    }
    protected void onFinished()  {
       this.db.outputStep(this);
    }

}
