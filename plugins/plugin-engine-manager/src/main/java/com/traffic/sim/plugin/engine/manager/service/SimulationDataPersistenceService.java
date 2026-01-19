package com.traffic.sim.plugin.engine.manager.service;

import com.traffic.sim.common.dto.ReplayDataDTO;
import com.traffic.sim.common.service.ReplayDataPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 仿真数据持久化服务
 * 负责将收集的仿真数据保存到MongoDB
 * 通过 ReplayDataPersistence 接口调用 plugin-engine-replay 的服务
 * 
 * 设计说明：
 * - 使用接口注入而非反射，实现类型安全的模块解耦
 * - 使用 @Autowired(required=false) 实现可选依赖
 * - 如果 replay 模块未加载，服务仍可正常启动
 * 
 * @author traffic-sim
 */
@Slf4j
@Service
public class SimulationDataPersistenceService {
    
    /**
     * 回放数据持久化接口（可选依赖）
     * 由 plugin-engine-replay 模块的 ReplayDataService 实现
     */
    private final ReplayDataPersistence replayDataPersistence;
    
    /**
     * 构造函数，使用可选依赖注入
     * 如果 ReplayDataPersistence 实现不存在，则注入 null
     */
    @Autowired
    public SimulationDataPersistenceService(
            @Autowired(required = false) ReplayDataPersistence replayDataPersistence) {
        this.replayDataPersistence = replayDataPersistence;
        if (replayDataPersistence != null) {
            log.info("ReplayDataPersistence is available for data persistence");
        } else {
            log.warn("ReplayDataPersistence is not available, replay data will not be persisted");
        }
    }
    
    /**
     * 保存仿真数据到MongoDB
     * 
     * @param simulationTaskId 仿真任务ID
     * @param stepDataList 步数据列表
     * @return 是否保存成功
     */
    public boolean saveSimulationData(String simulationTaskId, 
                                     List<SimulationDataCollector.StepData> stepDataList) {
        if (!isReplayServiceAvailable()) {
            log.warn("ReplayDataPersistence not available, cannot save simulation data");
            return false;
        }
        
        if (stepDataList == null || stepDataList.isEmpty()) {
            log.warn("No data to save for simulation task: {}", simulationTaskId);
            return false;
        }
        
        try {
            log.info("Saving {} steps of simulation data for task: {}", 
                stepDataList.size(), simulationTaskId);
            
            // 转换为ReplayDataDTO列表
            List<ReplayDataDTO> replayDataList = convertToReplayDataDTOList(stepDataList);
            
            // 直接调用接口方法，无需反射
            replayDataPersistence.saveReplayDataBatch(simulationTaskId, replayDataList);
            
            log.info("Successfully saved {} steps of simulation data for task: {}", 
                stepDataList.size(), simulationTaskId);
            return true;
        } catch (Exception e) {
            log.error("Failed to save simulation data for task: {}", simulationTaskId, e);
            return false;
        }
    }
    
    /**
     * 转换StepData列表为ReplayDataDTO列表
     */
    private List<ReplayDataDTO> convertToReplayDataDTOList(
            List<SimulationDataCollector.StepData> stepDataList) {
        
        return stepDataList.stream()
            .map(stepData -> {
                ReplayDataDTO dto = new ReplayDataDTO();
                dto.setStep(stepData.getStep());
                dto.setTimestamp(stepData.getTimestamp());
                dto.setSimData(stepData.getSimData());
                dto.setStatistics(stepData.getStatistics());
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 检查回放服务是否可用
     */
    public boolean isReplayServiceAvailable() {
        return replayDataPersistence != null;
    }
}
