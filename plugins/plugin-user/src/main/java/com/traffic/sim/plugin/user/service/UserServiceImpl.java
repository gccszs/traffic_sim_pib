package com.traffic.sim.plugin.user.service;

import com.traffic.sim.common.constant.ErrorCode;
import com.traffic.sim.common.constant.UserStatus;
import com.traffic.sim.common.dto.UserDTO;
import com.traffic.sim.common.exception.BusinessException;
import com.traffic.sim.common.service.UserService;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.plugin.user.dto.UserCreateRequest;
import com.traffic.sim.plugin.user.dto.UserUpdateRequest;
import com.traffic.sim.plugin.user.entity.Role;
import com.traffic.sim.plugin.user.entity.User;
import com.traffic.sim.plugin.user.repository.RoleRepository;
import com.traffic.sim.plugin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 
 * @author traffic-sim
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService, UserServiceExt {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_NOT_FOUND, "用户不存在"));
        return convertToDTO(user);
    }
    
    @Override
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_NOT_FOUND, "用户不存在"));
        return convertToDTO(user);
    }
    
    @Override
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new BusinessException(ErrorCode.ERR_EXIST, "用户名已存在");
        }
        
        // 检查邮箱是否已存在（如果提供了邮箱）
        if (userDTO.getEmail() != null && !userDTO.getEmail().isEmpty()) {
            if (userRepository.existsByEmail(userDTO.getEmail())) {
                throw new BusinessException(ErrorCode.ERR_EXIST, "邮箱已被使用");
            }
        }
        
        // 创建用户实体
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        
        // 注意：UserDTO 不包含密码字段，密码应该通过其他方式传递（如 UserCreateRequest）
        // 这里假设密码已经通过其他方式设置到 user 对象中，或者需要扩展接口
        
        // 设置默认状态
        if (user.getStatus() == null || user.getStatus().isEmpty()) {
            user.setStatus(UserStatus.NORMAL);
        }
        
        // 如果提供了角色ID，验证角色是否存在
        if (user.getRoleId() != null) {
            Optional<Role> role = roleRepository.findById(user.getRoleId().longValue());
            if (role.isPresent()) {
                userDTO.setRoleName(role.get().getRoleName());
            }
        }
        
        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("创建用户成功: username={}, id={}", savedUser.getUsername(), savedUser.getId());
        
        return convertToDTO(savedUser);
    }
    
    @Override
    @Transactional
    public UserDTO updateUser(Long userId, UserDTO userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_NOT_FOUND, "用户不存在"));
        
        // 更新邮箱（如果提供了且与现有邮箱不同）
        if (userDTO.getEmail() != null && !userDTO.getEmail().isEmpty() 
                && !userDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDTO.getEmail())) {
                throw new BusinessException(ErrorCode.ERR_EXIST, "邮箱已被使用");
            }
            user.setEmail(userDTO.getEmail());
        }
        
        // 更新其他字段
        if (userDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(userDTO.getPhoneNumber());
        }
        if (userDTO.getInstitution() != null) {
            user.setInstitution(userDTO.getInstitution());
        }
        if (userDTO.getRoleId() != null) {
            user.setRoleId(userDTO.getRoleId());
            // 更新角色名称
            Optional<Role> role = roleRepository.findById(userDTO.getRoleId().longValue());
            if (role.isPresent()) {
                userDTO.setRoleName(role.get().getRoleName());
            }
        }
        if (userDTO.getStatus() != null && !userDTO.getStatus().isEmpty()) {
            user.setStatus(userDTO.getStatus());
        }
        
        // 注意：UserDTO 不包含密码字段，密码更新应该通过其他方式处理
        // 如果需要更新密码，应该扩展接口或使用专门的密码更新方法
        
        User updatedUser = userRepository.save(user);
        log.info("更新用户成功: username={}, id={}", updatedUser.getUsername(), updatedUser.getId());
        
        return convertToDTO(updatedUser);
    }
    
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.ERR_NOT_FOUND, "用户不存在");
        }
        userRepository.deleteById(userId);
        log.info("删除用户成功: id={}", userId);
    }
    
    @Override
    public boolean validatePassword(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        // 检查用户状态
        if (!UserStatus.NORMAL.equals(user.getStatus())) {
            log.warn("用户状态异常，无法验证密码: username={}, status={}", username, user.getStatus());
            return false;
        }

        return passwordEncoder.matches(password, user.getPassword());
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional
    public UserDTO createUserWithPassword(String username, String password, String email,
                                         String phoneNumber, String institution) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.ERR_EXIST, "用户名已存在");
        }

        // 检查邮箱是否已存在（如果提供了邮箱）
        if (email != null && !email.isEmpty()) {
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException(ErrorCode.ERR_EXIST, "邮箱已被使用");
            }
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setInstitution(institution);
        user.setRoleId(1); // 默认角色ID

        // 加密密码
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        } else {
            throw new BusinessException(ErrorCode.ERR_ARG, "密码不能为空");
        }

        // 设置默认状态
        user.setStatus(UserStatus.NORMAL);

        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("创建用户成功: username={}, id={}", savedUser.getUsername(), savedUser.getId());

        return convertToDTO(savedUser);
    }
    
    /**
     * 将User实体转换为UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        
        // 设置角色名称（如果角色ID存在）
        if (user.getRoleId() != null) {
            Optional<Role> role = roleRepository.findById(user.getRoleId().longValue());
            if (role.isPresent()) {
                dto.setRoleName(role.get().getRoleName());
            }
        }
        
        return dto;
    }
    
    // ========== UserServiceExt 接口实现 ==========
    
    @Override
    @Transactional
    public UserDTO createUserWithPassword(UserCreateRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.ERR_EXIST, "用户名已存在");
        }
        
        // 检查邮箱是否已存在（如果提供了邮箱）
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(ErrorCode.ERR_EXIST, "邮箱已被使用");
            }
        }
        
        // 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setInstitution(request.getInstitution());
        user.setRoleId(request.getRoleId());
        
        // 加密密码
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else {
            throw new BusinessException(ErrorCode.ERR_ARG, "密码不能为空");
        }
        
        // 设置默认状态
        user.setStatus(UserStatus.NORMAL);
        
        // 如果提供了角色ID，验证角色是否存在
        if (user.getRoleId() != null) {
            Optional<Role> role = roleRepository.findById(user.getRoleId().longValue());
            if (role.isEmpty()) {
                throw new BusinessException(ErrorCode.ERR_NOT_FOUND, "角色不存在");
            }
        }
        
        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("创建用户成功: username={}, id={}", savedUser.getUsername(), savedUser.getId());
        
        return convertToDTO(savedUser);
    }
    
    @Override
    @Transactional
    public UserDTO updateUserWithPassword(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_NOT_FOUND, "用户不存在"));
        
        // 更新邮箱（如果提供了且与现有邮箱不同）
        if (request.getEmail() != null && !request.getEmail().isEmpty() 
                && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(ErrorCode.ERR_EXIST, "邮箱已被使用");
            }
            user.setEmail(request.getEmail());
        }
        
        // 更新其他字段
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getInstitution() != null) {
            user.setInstitution(request.getInstitution());
        }
        if (request.getRoleId() != null) {
            user.setRoleId(request.getRoleId());
            // 验证角色是否存在
            Optional<Role> role = roleRepository.findById(request.getRoleId().longValue());
            if (role.isEmpty()) {
                throw new BusinessException(ErrorCode.ERR_NOT_FOUND, "角色不存在");
            }
        }
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            user.setStatus(request.getStatus());
        }
        
        // 更新密码（如果提供了新密码）
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        User updatedUser = userRepository.save(user);
        log.info("更新用户成功: username={}, id={}", updatedUser.getUsername(), updatedUser.getId());
        
        return convertToDTO(updatedUser);
    }
    
    @Override
    @Transactional
    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_NOT_FOUND, "用户不存在"));
        
        if (newPassword == null || newPassword.isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_ARG, "新密码不能为空");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("更新用户密码成功: id={}", userId);
    }
    
    @Override
    public PageResult<UserDTO> getUserList(int page, int size, String status) {
        // 分页参数处理（page从1开始，需要转换为从0开始）
        int pageIndex = page > 0 ? page - 1 : 0;
        int pageSize = size > 0 ? size : 10;
        
        // 创建分页对象，按创建时间倒序
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        
        Page<User> userPage;
        if (status != null && !status.isEmpty()) {
            // 按状态查询
            userPage = userRepository.findByStatus(status, pageable);
        } else {
            // 查询所有
            userPage = userRepository.findAll(pageable);
        }
        
        // 转换为DTO列表
        List<UserDTO> userDTOList = userPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        // 构建分页结果
        return new PageResult<>(
                userDTOList,
                userPage.getTotalElements(),
                page,
                pageSize
        );
    }
}

