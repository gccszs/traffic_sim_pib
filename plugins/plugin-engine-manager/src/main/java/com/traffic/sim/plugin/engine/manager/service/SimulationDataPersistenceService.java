package com.traffic.sim.plugin.engine.manager.service;

import com.traffic.sim.plugin.engine.manager.service.SimulationDataCollector.StepData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 仿真数据异步持久化服务
 * 使用线程池管理异步保存任务，避免阻塞仿真进程
 *
 * @author traffic-sim
 */
@Slf4j
@Service
public class SimulationDataPersistenceService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 异步保存仿真数据到 MongoDB
     *
     * @param simulationTaskId 仿真任务ID
     * @param userId 用户ID
     * @param taskId 任务ID（来自simulation_task表）
     * @param stepDataList 仿真步数据列表
     * @return 是否保存成功
     */
    @Async("simulationDataExecutor")
    public CompletableFuture<Boolean> saveSimulationDataAsync(String simulationTaskId, String userId, String taskId, List<StepData> stepDataList) {
        return CompletableFuture.supplyAsync(() -> saveSimulationData(simulationTaskId, userId, taskId, stepDataList));
    }

    /**
     * 同步保存仿真数据到 MongoDB
     *
     * @param simulationTaskId 仿真任务ID
     * @param userId 用户ID
     * @param taskId 任务ID（来自simulation_task表）
     * @param stepDataList 仿真步数据列表
     * @return 是否保存成功
     */
    public boolean saveSimulationData(String simulationTaskId, String userId, String taskId, List<StepData> stepDataList) {
        if (mongoTemplate == null) {
            log.warn("MongoDB not configured, skipping data persistence");
            return false;
        }

        if (stepDataList == null || stepDataList.isEmpty()) {
            log.warn("No data to save for simulation task: {}", simulationTaskId);
            return false;
        }

        try {
            log.info("Saving {} steps of simulation data for task: {}, userId: {}, taskId: {}",
                    stepDataList.size(), simulationTaskId, userId, taskId);

            // 构建 MongoDB 文档
            Map<String, Object> document = new HashMap<>();
            document.put("simulationTaskId", simulationTaskId);
            document.put("userId", userId);  // 添加用户ID
            document.put("taskId", taskId);  // 添加任务ID
            document.put("totalSteps", stepDataList.size());
            document.put("startTime", stepDataList.get(0).getTimestamp());
            document.put("endTime", stepDataList.get(stepDataList.size() - 1).getTimestamp());
            document.put("steps", stepDataList);
            document.put("createdAt", System.currentTimeMillis());

            // 保存到 MongoDB 的 simulation_data 集合
            mongoTemplate.save(document, "simulation_data");

            log.info("Successfully saved simulation data for task: {}", simulationTaskId);
            return true;

        } catch (Exception e) {
            log.error("Failed to save simulation data for task: {}", simulationTaskId, e);
            return false;
        }
    }

    /**
     * 异步保存单个仿真步数据（带用户ID和任务ID）
     *
     * @param simulationTaskId 仿真任务ID
     * @param userId 用户ID
     * @param taskId 任务ID（来自simulation_task表）
     * @param stepData 仿真步数据
     * @return 是否保存成功
     */
    @Async("simulationDataExecutor")
    public CompletableFuture<Boolean> saveStepDataAsync(String simulationTaskId, String userId, String taskId, StepData stepData) {
        return CompletableFuture.supplyAsync(() -> saveStepData(simulationTaskId, userId, taskId, stepData));
    }

    /**
     * 同步保存单个仿真步数据（追加到现有文档，带用户ID和任务ID）
     *
     * @param simulationTaskId 仿真任务ID
     * @param userId 用户ID
     * @param taskId 任务ID（来自simulation_task表）
     * @param stepData 仿真步数据
     * @return 是否保存成功
     */
    public boolean saveStepData(String simulationTaskId, String userId, String taskId, StepData stepData) {
        if (mongoTemplate == null) {
            log.warn("MongoDB not configured, skipping data persistence");
            return false;
        }

        try {
            // 使用 MongoDB 的 $push 操作追加数据到数组
            // 这样可以避免每次都读取整个文档
            Query query =
                    Query.query(
                            Criteria.where("simulationTaskId").is(simulationTaskId)
                    );

            Update update =
                    new Update()
                            .push("steps", stepData)
                            .inc("totalSteps", 1)
                            .set("endTime", stepData.getTimestamp())
                            .setOnInsert("simulationTaskId", simulationTaskId)
                            .setOnInsert("userId", userId)  // 添加用户ID
                            .setOnInsert("taskId", taskId)  // 添加任务ID
                            .setOnInsert("startTime", stepData.getTimestamp())
                            .setOnInsert("createdAt", System.currentTimeMillis());

            mongoTemplate.upsert(query, update, "simulation_data");

            log.debug("Saved step {} data for task: {}", stepData.getStep(), simulationTaskId);
            return true;

        } catch (Exception e) {
            log.error("Failed to save step {} data for task: {}", stepData.getStep(), simulationTaskId, e);
            return false;
        }
    }
}
