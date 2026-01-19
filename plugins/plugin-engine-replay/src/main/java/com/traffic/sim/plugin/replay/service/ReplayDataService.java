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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 回放数据服务
 * 负责MongoDB中回放数据的存储和查询
 * 实现 ReplayDataPersistence 接口，供其他模块（如 plugin-engine-manager）调用
 * 
 * @author traffic-sim
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReplayDataService implements ReplayDataPersistence {
    
    private final ReplayDataRepository replayDataRepository;
    
    /**
     * 保存回放数据
     * 
     * @param taskId 任务ID
     * @param step 步数
     * @param simData 仿真数据
     * @param statistics 统计数据
     */
    public void saveReplayData(String taskId, Long step, 
                               Map<String, Object> simData,
                               Map<String, Object> statistics) {
        ReplayDataDocument document = new ReplayDataDocument();
        document.setTaskId(taskId);
        document.setStep(step);
        document.setTimestamp(System.currentTimeMillis());
        document.setSimData(simData);
        document.setStatistics(statistics);
        
        replayDataRepository.save(document);
    }
    
    /**
     * 批量保存回放数据
     * 
     * @param taskId 任务ID
     * @param dataList 数据列表
     */
    public void saveReplayDataBatch(String taskId, List<ReplayDataDTO> dataList) {
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
     * 获取回放数据
     * 
     * @param taskId 任务ID
     * @param startStep 起始步数
     * @param endStep 结束步数
     * @return 回放数据列表
     */
    public List<ReplayDataDTO> getReplayData(String taskId, Long startStep, Long endStep) {
        // 按步数排序，限制返回数量
        Pageable pageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.ASC, "step"));
        List<ReplayDataDocument> documents = replayDataRepository
                .findByTaskIdAndStepRange(taskId, startStep, endStep, pageable);
        
        return documents.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 删除回放数据
     * 
     * @param taskId 任务ID
     */
    public void deleteReplayData(String taskId) {
        replayDataRepository.deleteByTaskId(taskId);
        log.info("Deleted replay data for task: {}", taskId);
    }
    
    /**
     * 统计任务的数据条数
     */
    public long countReplayData(String taskId) {
        return replayDataRepository.countByTaskId(taskId);
    }
    
    /**
     * 转换为DTO
     */
    private ReplayDataDTO convertToDTO(ReplayDataDocument document) {
        ReplayDataDTO dto = new ReplayDataDTO();
        dto.setStep(document.getStep());
        dto.setTimestamp(document.getTimestamp());
        dto.setSimData(document.getSimData());
        dto.setStatistics(document.getStatistics());
        return dto;
    }
}

