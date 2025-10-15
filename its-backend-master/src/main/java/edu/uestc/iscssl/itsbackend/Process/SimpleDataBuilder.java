package edu.uestc.iscssl.itsbackend.Process;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.SMap_XML;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.StatTask;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;
import edu.uestc.iscssl.itsbackend.Process.Statistic.Stat.Stat_Base;
import edu.uestc.iscssl.itsbackend.Process.sink.DataSink;
import edu.uestc.iscssl.itsbackend.Process.sink.SimpleJsonDataSink;
import edu.uestc.iscssl.itsbackend.service.DataInfoService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: 马
 * @Date: 2019年6月3日 16:39:18
 */

public class SimpleDataBuilder {
    //当前正在组装的仿真步数，对应于map的key。
    private volatile int currentbuilding=0;
    protected final String simulationId;
    private Map<Integer,SimulationStep> simulations=new HashMap<>();
    private Gson gson;
    protected DataSink dataSink;
    protected DataInfoService dataInfoService;
    protected int step;
    protected StatTask statTask;

    List<SimulationStep> simulationSteps = new ArrayList<SimulationStep>();

    public SimpleDataBuilder(String simulationId, String filePath, DataInfoService dataInfoService,int step)  {
        this.simulationId=simulationId;
        GsonBuilder gb=new GsonBuilder();
        gb.excludeFieldsWithoutExposeAnnotation();
        this.gson=gb.create();
        this.dataInfoService=dataInfoService;
        this.step=step;
        dataSink=getDataSink();

        statTask = new StatTask(new SMap_XML(filePath),4);//把这个int接上车道数就行
    }
    protected void setDataSink(Object o){
        this.dataSink=new SimpleJsonDataSink(simulationId);
    }
    private DataSink getDataSink(){
        return this.dataSink;
    }
    public void add(Element element)  {
        if (element.isFinished()){
            SimulationStep simulationStep=simulations.get(element.getStepNum());
            simulationStep.setFinished();
            if (simulationStep.hasFinished()){
                simulations.remove(element.getStepNum());
            }

        }else {
            if (!simulations.containsKey(element.getStepNum()))
                simulations.put(element.getStepNum(),new SimulationStep(element.getStepNum(),this));
            simulations.get(element.getStepNum()).add(element);

        }

    }

    public void outputStep(SimulationStep step)  {
        if(Stat_Base.stat_bases == null){
            synchronized(Stat_Base.class) {
                if(Stat_Base.stat_bases == null){
                    Stat_Base.init();
                }
            }
        }
       Info info = new Info(step.getStep());
        List<Element> elements = step.getVehicles();
        for(Element e:elements)
            info.push(e.getVehicle());
        elements = step.getPhases();
        for(Element e:elements)
            info.push(e.getPhase());
        InfoStat infoStat = statTask.Execute(info);
        step.pushStat(infoStat);
        String stepString =this.gson.toJson(step);
        setDataSink(this.dataSink);
        this.dataSink.sink(stepString);
    }

}
