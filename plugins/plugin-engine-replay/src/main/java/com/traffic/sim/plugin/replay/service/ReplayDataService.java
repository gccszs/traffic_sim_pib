package com.traffic.sim.plugin.replay.service;

import com.traffic.sim.common.dto.ReplayDataDTO;
import com.traffic.sim.common.service.ReplayDataPersistence;
import com.traffic.sim.plugin.replay.document.ReplayDataDocument;
import com.traffic.sim.plugin.replay.repository.ReplayDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 回放数据服务
 * 负责MongoDB中回放数据的存储和查询
 * 实现 ReplayDataPersistence 接口，供其他模块（如 plugin-engine-manager）调用
 * 
 * 数据来源说明：
 * - 回放数据来自 simulation_data 集合（仿真过程中保存的数据）
 * - 不再使用独立的 replay_data 集合
 * 
 * @author traffic-sim
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReplayDataService implements ReplayDataPersistence {
    
    private final ReplayDataRepository replayDataRepository;
    private final MongoTemplate mongoTemplate;
    
    /**
     * 保存回放数据（已废弃，数据由仿真过程直接保存到 simulation_data）
     * 
     * @param taskId 任务ID
     * @param step 步数
     * @param simData 仿真数据
     * @param statistics 统计数据
     */
    @Deprecated
    public void saveReplayData(String taskId, Long step, 
                               Map<String, Object> simData,
                               Map<String, Object> statistics) {
        log.warn("saveReplayData is deprecated, data should be saved during simulation");
        ReplayDataDocument document = new ReplayDataDocument();
        document.setTaskId(taskId);
        document.setStep(step);
        document.setTimestamp(System.currentTimeMillis());
        document.setSimData(simData);
        document.setStatistics(statistics);
        
        replayDataRepository.save(document);
    }
    
    /**
     * 批量保存回放数据（已废弃，数据由仿真过程直接保存到 simulation_data）
     * 
     * @param taskId 任务ID
     * @param dataList 数据列表
     */
    @Override
    @Deprecated
    public void saveReplayDataBatch(String taskId, List<ReplayDataDTO> dataList) {
        log.warn("saveReplayDataBatch is deprecated, data should be saved during simulation");
        List<ReplayDataDocument> documents = dataList.stream()
                .map(dto -> {
                    ReplayDataDocument doc = new ReplayDataDocument();
                    doc.setTaskId(taskId);
                    doc.setStep(dto.getStep());
                    doc.setTimestamp(dto.getTimestamp());
                    doc.setSimData(dto.getSimData());
                    doc.setStatistics(dto.getStatistics());
                    return doc;
                })
                .collect(Collectors.toList());
        
        replayDataRepository.saveAll(documents);
    }
    
    /**
     * 获取回放数据（从 simulation_data 集合读取）
     * 
     * @param simulationTaskId 仿真任务ID
     * @param startStep 起始步数
     * @param endStep 结束步数
     * @return 回放数据列表
     */
    @SuppressWarnings("unchecked")
    public List<ReplayDataDTO> getReplayData(String simulationTaskId, Long startStep, Long endStep) {
        log.info("Loading replay data from simulation_data collection for task: {}, steps: {}-{}", 
                simulationTaskId, startStep, endStep);
        
        try {
            // 从 simulation_data 集合查询数据，使用 Document 类型避免类型转换问题
            Query query = Query.query(Criteria.where("simulationTaskId").is(simulationTaskId));
            org.bson.Document simulationData = mongoTemplate.findOne(query, org.bson.Document.class, "simulation_data");
            
            if (simulationData == null) {
                log.warn("No simulation data found for task: {}", simulationTaskId);
                return new ArrayList<>();
            }
            
            // 提取 steps 数组
            List<?> steps = simulationData.get("steps", List.class);
            if (steps == null || steps.isEmpty()) {
                log.warn("No steps data found in simulation_data for task: {}", simulationTaskId);
                return new ArrayList<>();
            }
            
            log.info("Found {} steps in simulation_data for task: {}", steps.size(), simulationTaskId);
            
            // 过滤步数范围并转换为 DTO
            List<ReplayDataDTO> result = new ArrayList<>();
            for (Object stepObj : steps) {
                Long step = extractStep(stepObj);
                if (step != null && step >= startStep && step <= endStep) {
                    ReplayDataDTO dto = convertStepDataToDTO(stepObj);
                    if (dto != null) {
                        result.add(dto);
                    }
                }
            }
            
            log.info("Loaded {} steps after filtering (range: {}-{})", result.size(), startStep, endStep);
            return result;
                    
        } catch (Exception e) {
            log.error("Error loading replay data from simulation_data for task: {}", simulationTaskId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 从步数据中提取步数（兼容 Map、Document 和对象三种格式）
     */
    private Long extractStep(Object stepData) {
        if (stepData == null) {
            return null;
        }
        
        // 如果是 BSON Document 格式
        if (stepData instanceof org.bson.Document) {
            org.bson.Document doc = (org.bson.Document) stepData;
            Object stepObj = doc.get("step");
            if (stepObj instanceof Number) {
                return ((Number) stepObj).longValue();
            }
        }
        
        // 如果是 Map 格式
        if (stepData instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) stepData;
            Object stepObj = map.get("step");
            if (stepObj instanceof Number) {
                return ((Number) stepObj).longValue();
            }
        }
        
        // 如果是对象格式，使用反射获取 step 字段
        try {
            java.lang.reflect.Field stepField = stepData.getClass().getDeclaredField("step");
            stepField.setAccessible(true);
            Object stepObj = stepField.get(stepData);
            if (stepObj instanceof Number) {
                return ((Number) stepObj).longValue();
            }
        } catch (Exception e) {
            log.debug("Failed to extract step from object: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 删除回放数据（从 simulation_data 集合删除）
     * 
     * @param simulationTaskId 仿真任务ID
     */
    @Override
    public void deleteReplayData(String simulationTaskId) {
        try {
            // 从 simulation_data 集合删除
            Query query = Query.query(Criteria.where("simulationTaskId").is(simulationTaskId));
            mongoTemplate.remove(query, "simulation_data");
            log.info("Deleted simulation data for task: {}", simulationTaskId);
        } catch (Exception e) {
            log.error("Error deleting simulation data for task: {}", simulationTaskId, e);
        }
    }
    
    /**
     * 统计任务的数据条数（从 simulation_data 集合统计）
     * 
     * @param simulationTaskId 仿真任务ID
     * @return 步数
     */
    @Override
    @SuppressWarnings("unchecked")
    public long countReplayData(String simulationTaskId) {
        try {
            // 从 simulation_data 集合查询
            Query query = Query.query(Criteria.where("simulationTaskId").is(simulationTaskId));
            Map<String, Object> simulationData = mongoTemplate.findOne(query, Map.class, "simulation_data");
            
            if (simulationData == null) {
                log.warn("No simulation data found for task: {}", simulationTaskId);
                return 0;
            }
            
            // 获取 totalSteps 字段
            Object totalStepsObj = simulationData.get("totalSteps");
            if (totalStepsObj instanceof Number) {
                long totalSteps = ((Number) totalStepsObj).longValue();
                log.info("Found {} steps for task: {}", totalSteps, simulationTaskId);
                return totalSteps;
            }
            
            // 如果没有 totalSteps 字段，统计 steps 数组长度
            List<Map<String, Object>> steps = (List<Map<String, Object>>) simulationData.get("steps");
            if (steps != null) {
                log.info("Counted {} steps from steps array for task: {}", steps.size(), simulationTaskId);
                return steps.size();
            }
            
            return 0;
        } catch (Exception e) {
            log.error("Error counting replay data for task: {}", simulationTaskId, e);
            return 0;
        }
    }
    
    /**
     * 将 simulation_data 中的 step 数据转换为 ReplayDataDTO
     * 兼容 BSON Document、Map 和对象三种格式
     */
    @SuppressWarnings("unchecked")
    private ReplayDataDTO convertStepDataToDTO(Object stepData) {
        ReplayDataDTO dto = new ReplayDataDTO();
        
        if (stepData == null) {
            return null;
        }
        
        // 如果是 BSON Document 格式（MongoDB 原生格式）
        if (stepData instanceof org.bson.Document) {
            org.bson.Document doc = (org.bson.Document) stepData;
            
            // 提取步数
            Object stepObj = doc.get("step");
            if (stepObj instanceof Number) {
                dto.setStep(((Number) stepObj).longValue());
            }
            
            // 提取时间戳
            Object timestampObj = doc.get("timestamp");
            if (timestampObj instanceof Number) {
                dto.setTimestamp(((Number) timestampObj).longValue());
            }
            
            // 提取仿真数据
            Object simDataObj = doc.get("simData");
            if (simDataObj instanceof Map) {
                dto.setSimData((Map<String, Object>) simDataObj);
            } else if (simDataObj instanceof org.bson.Document) {
                dto.setSimData(((org.bson.Document) simDataObj));
            }
            
            // 提取统计数据
            Object statsDataObj = doc.get("statsData");
            if (statsDataObj instanceof Map) {
                dto.setStatistics((Map<String, Object>) statsDataObj);
            } else if (statsDataObj instanceof org.bson.Document) {
                dto.setStatistics(((org.bson.Document) statsDataObj));
            }
            
            return dto;
        }
        
        // 如果是 Map 格式（标准 MongoDB 文档）
        if (stepData instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) stepData;
            
            // 提取步数
            Object stepObj = map.get("step");
            if (stepObj instanceof Number) {
                dto.setStep(((Number) stepObj).longValue());
            }
            
            // 提取时间戳
            Object timestampObj = map.get("timestamp");
            if (timestampObj instanceof Number) {
                dto.setTimestamp(((Number) timestampObj).longValue());
            }
            
            // 提取仿真数据
            Object simDataObj = map.get("simData");
            if (simDataObj instanceof Map) {
                dto.setSimData((Map<String, Object>) simDataObj);
            }
            
            // 提取统计数据
            Object statsDataObj = map.get("statsData");
            if (statsDataObj instanceof Map) {
                dto.setStatistics((Map<String, Object>) statsDataObj);
            }
            
            return dto;
        }
        
        // 如果是 StepData 对象格式（使用反射提取字段）
        try {
            Class<?> clazz = stepData.getClass();
            
            // 提取 step 字段
            try {
                java.lang.reflect.Field stepField = clazz.getDeclaredField("step");
                stepField.setAccessible(true);
                Object stepObj = stepField.get(stepData);
                if (stepObj instanceof Number) {
                    dto.setStep(((Number) stepObj).longValue());
                }
            } catch (NoSuchFieldException e) {
                log.debug("No step field found in object");
            }
            
            // 提取 timestamp 字段
            try {
                java.lang.reflect.Field timestampField = clazz.getDeclaredField("timestamp");
                timestampField.setAccessible(true);
                Object timestampObj = timestampField.get(stepData);
                if (timestampObj instanceof Number) {
                    dto.setTimestamp(((Number) timestampObj).longValue());
                }
            } catch (NoSuchFieldException e) {
                log.debug("No timestamp field found in object");
            }
            
            // 提取 simData 字段
            try {
                java.lang.reflect.Field simDataField = clazz.getDeclaredField("simData");
                simDataField.setAccessible(true);
                Object simDataObj = simDataField.get(stepData);
                if (simDataObj instanceof Map) {
                    dto.setSimData((Map<String, Object>) simDataObj);
                }
            } catch (NoSuchFieldException e) {
                log.debug("No simData field found in object");
            }
            
            // 提取 statsData 字段
            try {
                java.lang.reflect.Field statsDataField = clazz.getDeclaredField("statsData");
                statsDataField.setAccessible(true);
                Object statsDataObj = statsDataField.get(stepData);
                if (statsDataObj instanceof Map) {
                    dto.setStatistics((Map<String, Object>) statsDataObj);
                }
            } catch (NoSuchFieldException e) {
                log.debug("No statsData field found in object");
            }
            
            return dto;
            
        } catch (Exception e) {
            log.error("Failed to convert step data to DTO using reflection", e);
            return null;
        }
    }
    
    /**
     * 转换为DTO（从 replay_data 集合，已废弃）
     */
    @Deprecated
    private ReplayDataDTO convertToDTO(ReplayDataDocument document) {
        ReplayDataDTO dto = new ReplayDataDTO();
        dto.setStep(document.getStep());
        dto.setTimestamp(document.getTimestamp());
        dto.setSimData(document.getSimData());
        dto.setStatistics(document.getStatistics());
        return dto;
    }
}

