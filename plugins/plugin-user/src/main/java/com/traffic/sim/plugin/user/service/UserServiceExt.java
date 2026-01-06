package com.traffic.sim.plugin.user.service;

import com.traffic.sim.common.dto.UserDTO;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.plugin.user.dto.UserCreateRequest;
import com.traffic.sim.plugin.user.dto.UserUpdateRequest;

/**
 * 用户服务扩展接口
 * 提供带密码处理的用户操作方法
 * 
 * @author traffic-sim
 */
public interface UserServiceExt {
    
    /**
     * 创建用户（带密码）
     */
    UserDTO createUserWithPassword(UserCreateRequest request);
    
    /**
     * 更新用户（带密码）
     */
    UserDTO updateUserWithPassword(Long userId, UserUpdateRequest request);
    
    /**
     * 更新用户密码
     */
    void updatePassword(Long userId, String newPassword);
    
    /**
     * 分页获取用户列表
     */
    PageResult<UserDTO> getUserList(int page, int size, String status);
}

