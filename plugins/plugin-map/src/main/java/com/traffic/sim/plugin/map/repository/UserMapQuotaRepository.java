package com.traffic.sim.plugin.map.repository;

import com.traffic.sim.plugin.map.entity.UserMapQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户地图配额Repository
 * 
 * @author traffic-sim
 */
@Repository
public interface UserMapQuotaRepository extends JpaRepository<UserMapQuota, Long> {
    
    /**
     * 根据用户ID查找配额
     */
    Optional<UserMapQuota> findByUserId(Long userId);
}

