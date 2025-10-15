package edu.uestc.iscssl.itsbackend.Process;

import com.google.gson.Gson;
import edu.uestc.iscssl.itsbackend.service.DataInfoService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class KafkaReader extends Thread{
    KafkaConsumer<String, String> consumer;
    SimpleDataBuilder db;
    private volatile boolean isShutdown=false;
    public KafkaReader(String simulationId, String filePath, DataInfoService dataInfoService,int step) {
        db=new SimpleDataBuilder(simulationId, filePath,dataInfoService,step);
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "127.0.0.1:9092");
        props.setProperty("group.id", simulationId);
        props.setProperty("auto.commit.interval.ms", "1000");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("auto.offset.reset", "earliest");
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("vehicle_"+simulationId, "phase_"+simulationId));
        consumer.beginningOffsets(Arrays.asList(new TopicPartition("vehicle_"+simulationId,0),new TopicPartition("phase_"+simulationId,0)));
        setName("kafka-data-process-thread:"+simulationId);
    //    consumer.poll(0);
    //    consumer.seekToBeginning(Arrays.asList(new TopicPartition("vehicle_"+simulationId,0),new TopicPartition("phase_"+simulationId,0)));
    }

    @Override
    public void run() {
        while (!isShutdown) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records){
                Element element=new Gson().fromJson(record.value(),Element.class);
                db.add(element);
                //System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
            }

        }
        consumer.close();
    }
    public void shutdown(){
        this.isShutdown=true;
    }
}
