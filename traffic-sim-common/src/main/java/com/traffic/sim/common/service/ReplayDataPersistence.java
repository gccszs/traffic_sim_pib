package com.traffic.sim.common.service;

import com.traffic.sim.common.dto.ReplayDataDTO;

import java.util.List;

/**
 * 回放数据持久化接口
 * 定义在 common 模块，供 plugin-engine-manager 和 plugin-engine-replay 共同使用
 * 
 * 设计说明：
 * - plugin-engine-replay 实现此接口（ReplayDataService）
 * - plugin-engine-manager 通过此接口注入，实现模块间松耦合
 * - 使用 @Autowired(required=false) 实现可选依赖
 * 
 * @author traffic-sim
 */
public interface ReplayDataPersistence {
    
    /**
     * 批量保存回放数据
     * 
     * @param taskId 任务ID（仿真任务ID）
     * @param dataList 回放数据列表
     */
    void saveReplayDataBatch(String taskId, List<ReplayDataDTO> dataList);
    
    /**
     * 删除指定任务的回放数据
     * 
     * @param taskId 任务ID
     */
    void deleteReplayData(String taskId);
    
    /**
     * 统计任务的数据条数
     * 
     * @param taskId 任务ID
     * @return 数据条数
     */
    long countReplayData(String taskId);
}
