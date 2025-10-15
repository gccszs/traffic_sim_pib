package edu.uestc.iscssl.itsbackend.Process.sink;

import edu.uestc.iscssl.itsbackend.service.DataInfoService;
import edu.uestc.iscssl.itsbackend.utils.DataInfo;
import org.springframework.data.mongodb.core.mapping.Field;

public class MongoDbSink implements DataSink{
    private DataInfoService dataInfoService;
    private String simulationId;
    public MongoDbSink(String simulationId,DataInfoService dataInfoService) {
        this.dataInfoService = dataInfoService;
        this.simulationId=simulationId;
    }

    @Override
    public void sink(String data) {
    }

    @Override
    @Field
    public void sink2(DataInfo dataInfo) {
/*        KafkaReader kafkaReader = (KafkaReader) KafkaReader.currentThread();
        kafkaReader.shutdown();*/
        DataInfo dataInfo1 = dataInfoService.addDataInfo(dataInfo);
        System.out.println(dataInfo1);
    }
}
