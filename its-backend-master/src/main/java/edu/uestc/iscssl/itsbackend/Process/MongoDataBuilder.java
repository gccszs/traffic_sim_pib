package edu.uestc.iscssl.itsbackend.Process;

import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.Info;
import edu.uestc.iscssl.itsbackend.Process.Statistic.POJO.InfoStat;
import edu.uestc.iscssl.itsbackend.Process.sink.MongoDbSink;
import edu.uestc.iscssl.itsbackend.service.DataInfoService;
import edu.uestc.iscssl.itsbackend.utils.DataInfo;

import java.util.List;

public class MongoDataBuilder extends SimpleDataBuilder{
    protected MongoDbSink mongoDbSink;
    protected int i;
    DataInfo dataInfo = new DataInfo(this.simulationId);
    public MongoDataBuilder(String simulationId, String filePath, DataInfoService dataInfoService,int step) {
        super(simulationId, filePath, dataInfoService,step);
        mongoDbSink = new MongoDbSink( simulationId,dataInfoService);
    }


    @Override
    protected void setDataSink(Object o) {
        this.dataSink=new MongoDbSink(simulationId,dataInfoService);
    }

    protected MongoDbSink getMongoDbSink(){
        return this.mongoDbSink;
    }

    public void outputStep(SimulationStep step) {
        Info info = new Info(step.getStep());
        List<Element> elements = step.getVehicles();
        for(Element e:elements)
            info.push(e.getVehicle());
        elements = step.getPhases();
        for(Element e:elements)
            info.push(e.getPhase());
        dataInfo.setInfo(info);
        InfoStat infoStat = statTask.Execute(info);
        step.pushStat(infoStat);
        dataInfo.setInfoStat(infoStat);
        if( i < this.step) {
            i++;
        }
        else {
            mongoDbSink = getMongoDbSink();
            mongoDbSink.sink2(dataInfo);
        }
    }
}
