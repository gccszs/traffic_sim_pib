package com.traffic.sim.plugin.user.repository;

import com.traffic.sim.plugin.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色Repository
 * 
 * @author traffic-sim
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * 根据角色代码查找角色
     */
    Optional<Role> findByRoleCode(String roleCode);
    
    /**
     * 根据角色名称查找角色
     */
    Optional<Role> findByRoleName(String roleName);
}

