package com.traffic.sim.plugin.engine.manager.service;

import com.traffic.sim.common.mq.Message;
import com.traffic.sim.common.mq.MessageProducer;
import com.traffic.sim.common.mq.MessageQueue;
import com.traffic.sim.plugin.engine.manager.service.SimulationDataCollector.StepData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;

/**
 * 仿真数据MQ消息处理器
 * 订阅MQ消息，异步处理仿真数据保存请求
 * 
 * @author traffic-sim
 */
@Slf4j
@Service
public class SimulationDataMessageHandler {

    @Autowired
    private MessageQueue messageQueue;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private SimulationDataPersistenceService persistenceService;

    public static final String TOPIC_SIMULATION_DATA_SAVE = "simulation.data.save";
    public static final String TOPIC_SIMULATION_DATA_BATCH_SAVE = "simulation.data.batch.save";

    @PostConstruct
    public void init() {
        log.info("Initializing SimulationDataMessageHandler...");
        subscribeToTopics();
        log.info("SimulationDataMessageHandler initialized successfully");
    }

    private void subscribeToTopics() {
        messageQueue.subscribe(TOPIC_SIMULATION_DATA_SAVE, (Message<SimulationDataSaveRequest> message) -> {
            handleSimulationDataSave(message);
        });

        messageQueue.subscribe(TOPIC_SIMULATION_DATA_BATCH_SAVE, (Message<SimulationDataBatchSaveRequest> message) -> {
            handleSimulationDataBatchSave(message);
        });

        log.info("Subscribed to topics: {}, {}", TOPIC_SIMULATION_DATA_SAVE, TOPIC_SIMULATION_DATA_BATCH_SAVE);
    }

    private void handleSimulationDataSave(Message<SimulationDataSaveRequest> message) {
        SimulationDataSaveRequest request = message.getPayload();
        log.info("MQ: Received simulation data save request for task: {}, step: {}", 
                request.getSimulationTaskId(), request.getStepData().getStep());

        try {
            persistenceService.saveStepData(
                    request.getSimulationTaskId(),
                    request.getUserId(),
                    request.getTaskId(),
                    request.getStepData()
            );
            log.debug("MQ: Successfully saved simulation data for task: {}", request.getSimulationTaskId());
        } catch (Exception e) {
            log.error("MQ: Failed to save simulation data for task: {}", request.getSimulationTaskId(), e);
        }
    }

    private void handleSimulationDataBatchSave(Message<SimulationDataBatchSaveRequest> message) {
        SimulationDataBatchSaveRequest request = message.getPayload();
        log.info("MQ: Received simulation data batch save request for task: {}, steps: {}", 
                request.getSimulationTaskId(), request.getStepDataList().size());

        try {
            persistenceService.saveSimulationData(
                    request.getSimulationTaskId(),
                    request.getUserId(),
                    request.getTaskId(),
                    request.getStepDataList()
            );
            log.info("MQ: Successfully saved {} steps of simulation data for task: {}", 
                    request.getStepDataList().size(), request.getSimulationTaskId());
        } catch (Exception e) {
            log.error("MQ: Failed to save batch simulation data for task: {}", request.getSimulationTaskId(), e);
        }
    }

    public void sendSimulationDataSaveRequest(String simulationTaskId, String userId, String taskId, StepData stepData) {
        SimulationDataSaveRequest request = new SimulationDataSaveRequest(
                simulationTaskId, userId, taskId, stepData
        );
        String messageId = messageProducer.send(TOPIC_SIMULATION_DATA_SAVE, request);
        log.debug("Sent simulation data save request with ID: {}", messageId);
    }

    public void sendSimulationDataBatchSaveRequest(String simulationTaskId, String userId, String taskId, List<StepData> stepDataList) {
        SimulationDataBatchSaveRequest request = new SimulationDataBatchSaveRequest(
                simulationTaskId, userId, taskId, stepDataList
        );
        String messageId = messageProducer.send(TOPIC_SIMULATION_DATA_BATCH_SAVE, request);
        log.debug("Sent simulation data batch save request with ID: {}, steps: {}", messageId, stepDataList.size());
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up SimulationDataMessageHandler...");
        messageQueue.unsubscribe(TOPIC_SIMULATION_DATA_SAVE);
        messageQueue.unsubscribe(TOPIC_SIMULATION_DATA_BATCH_SAVE);
        log.info("SimulationDataMessageHandler cleanup completed");
    }

    public static class SimulationDataSaveRequest {
        private String simulationTaskId;
        private String userId;
        private String taskId;
        private StepData stepData;

        public SimulationDataSaveRequest() {}

        public SimulationDataSaveRequest(String simulationTaskId, String userId, String taskId, StepData stepData) {
            this.simulationTaskId = simulationTaskId;
            this.userId = userId;
            this.taskId = taskId;
            this.stepData = stepData;
        }

        public String getSimulationTaskId() { return simulationTaskId; }
        public void setSimulationTaskId(String simulationTaskId) { this.simulationTaskId = simulationTaskId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public StepData getStepData() { return stepData; }
        public void setStepData(StepData stepData) { this.stepData = stepData; }
    }

    public static class SimulationDataBatchSaveRequest {
        private String simulationTaskId;
        private String userId;
        private String taskId;
        private List<StepData> stepDataList;

        public SimulationDataBatchSaveRequest() {}

        public SimulationDataBatchSaveRequest(String simulationTaskId, String userId, String taskId, List<StepData> stepDataList) {
            this.simulationTaskId = simulationTaskId;
            this.userId = userId;
            this.taskId = taskId;
            this.stepDataList = stepDataList;
        }

        public String getSimulationTaskId() { return simulationTaskId; }
        public void setSimulationTaskId(String simulationTaskId) { this.simulationTaskId = simulationTaskId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public List<StepData> getStepDataList() { return stepDataList; }
        public void setStepDataList(List<StepData> stepDataList) { this.stepDataList = stepDataList; }
    }
}
