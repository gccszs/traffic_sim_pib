package com.traffic.sim.common.service;

import com.traffic.sim.common.dto.UserDTO;

/**
 * 用户服务接口
 * 定义在common模块，由plugin-user模块实现
 * 
 * @author traffic-sim
 */
public interface UserService {
    
    /**
     * 根据ID获取用户
     */
    UserDTO getUserById(Long userId);
    
    /**
     * 根据用户名获取用户
     */
    UserDTO getUserByUsername(String username);
    
    /**
     * 创建用户
     */
    UserDTO createUser(UserDTO userDTO);
    
    /**
     * 更新用户
     */
    UserDTO updateUser(Long userId, UserDTO userDTO);
    
    /**
     * 删除用户
     */
    void deleteUser(Long userId);
    
    /**
     * 验证用户密码
     */
    boolean validatePassword(String username, String password);
}

