package com.traffic.sim.plugin.user.service;

import com.traffic.sim.common.service.PermissionService;
import com.traffic.sim.plugin.user.entity.Permission;
import com.traffic.sim.plugin.user.entity.Role;
import com.traffic.sim.plugin.user.entity.User;
import com.traffic.sim.plugin.user.repository.RoleRepository;
import com.traffic.sim.plugin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 * 
 * @author traffic-sim
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    
    @Override
    public List<String> getPermissionsByRoleName(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return Collections.emptyList();
        }
        
        Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
        if (roleOpt.isEmpty()) {
            log.warn("角色不存在: {}", roleName);
            return Collections.emptyList();
        }
        
        return extractPermissionCodes(roleOpt.get());
    }
    
    @Override
    public List<String> getPermissionsByRoleId(Integer roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        
        Optional<Role> roleOpt = roleRepository.findById(roleId.longValue());
        if (roleOpt.isEmpty()) {
            log.warn("角色不存在: {}", roleId);
            return Collections.emptyList();
        }
        
        return extractPermissionCodes(roleOpt.get());
    }
    
    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("用户不存在: {}", userId);
            return Collections.emptyList();
        }
        
        User user = userOpt.get();
        if (user.getRoleId() == null) {
            return Collections.emptyList();
        }
        
        return getPermissionsByRoleId(user.getRoleId());
    }
    
    /**
     * 从角色中提取权限代码列表
     */
    private List<String> extractPermissionCodes(Role role) {
        List<Permission> permissions = role.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        
        return permissions.stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toList());
    }
}

