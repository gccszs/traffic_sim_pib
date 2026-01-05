package com.traffic.sim.plugin.replay.repository;

import com.traffic.sim.plugin.replay.document.ReplayDataDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 回放数据MongoDB Repository
 * 
 * @author traffic-sim
 */
@Repository
public interface ReplayDataRepository extends MongoRepository<ReplayDataDocument, String> {
    
    /**
     * 根据任务ID和步数范围查询回放数据
     */
    @Query("{ 'taskId': ?0, 'step': { $gte: ?1, $lte: ?2 } }")
    List<ReplayDataDocument> findByTaskIdAndStepRange(String taskId, Long startStep, Long endStep, Pageable pageable);
    
    /**
     * 根据任务ID查询所有回放数据
     */
    List<ReplayDataDocument> findByTaskId(String taskId);
    
    /**
     * 根据任务ID删除所有回放数据
     */
    void deleteByTaskId(String taskId);
    
    /**
     * 统计任务的数据条数
     */
    long countByTaskId(String taskId);
}

