package com.traffic.sim.plugin.simulation.repository;

import com.traffic.sim.plugin.simulation.entity.SimulationTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 仿真任务Repository
 * 
 * @author traffic-sim
 */
@Repository
public interface SimulationTaskRepository extends JpaRepository<SimulationTask, String> {
    
    /**
     * 根据用户ID查询仿真任务列表
     */
    Page<SimulationTask> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据状态查询仿真任务列表
     */
    Page<SimulationTask> findByStatus(String status, Pageable pageable);
    
    /**
     * 根据用户ID和状态查询仿真任务列表
     */
    Page<SimulationTask> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
    
    /**
     * 根据用户ID查询所有仿真任务
     */
    List<SimulationTask> findByUserId(Long userId);
}

