package com.traffic.sim.plugin.replay.repository;

import com.traffic.sim.plugin.replay.entity.ReplayTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 回放任务Repository
 * 
 * @author traffic-sim
 */
@Repository
public interface ReplayTaskRepository extends JpaRepository<ReplayTask, String> {
    
    /**
     * 根据用户ID查询回放任务列表
     */
    Page<ReplayTask> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据仿真任务ID查询回放任务
     */
    List<ReplayTask> findBySimulationTaskId(String simulationTaskId);
    
    /**
     * 根据用户ID和状态查询回放任务
     */
    Page<ReplayTask> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
    
    /**
     * 根据任务ID和用户ID查询（用于权限验证）
     */
    Optional<ReplayTask> findByTaskIdAndUserId(String taskId, Long userId);
    
    /**
     * 统计用户回放任务数量
     */
    @Query("SELECT COUNT(r) FROM ReplayTask r WHERE r.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
}

