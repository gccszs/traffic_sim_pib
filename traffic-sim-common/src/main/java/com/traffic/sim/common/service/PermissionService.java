package com.traffic.sim.common.service;

import java.util.List;

/**
 * 权限服务接口
 * 定义在common模块，由plugin-user模块实现
 * 
 * @author traffic-sim
 */
public interface PermissionService {
    
    /**
     * 根据角色名称获取权限列表
     * 
     * @param roleName 角色名称
     * @return 权限代码列表
     */
    List<String> getPermissionsByRoleName(String roleName);
    
    /**
     * 根据角色ID获取权限列表
     * 
     * @param roleId 角色ID
     * @return 权限代码列表
     */
    List<String> getPermissionsByRoleId(Integer roleId);
    
    /**
     * 根据用户ID获取权限列表
     * 
     * @param userId 用户ID
     * @return 权限代码列表
     */
    List<String> getPermissionsByUserId(Long userId);
}

