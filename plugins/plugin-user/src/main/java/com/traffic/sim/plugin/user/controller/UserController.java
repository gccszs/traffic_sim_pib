package com.traffic.sim.plugin.user.controller;

import com.traffic.sim.common.dto.UserDTO;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.service.UserService;
import com.traffic.sim.plugin.user.dto.UserCreateRequest;
import com.traffic.sim.plugin.user.dto.UserUpdateRequest;
import com.traffic.sim.plugin.user.service.UserServiceExt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理Controller
 * 
 * @author traffic-sim
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {
    
    private final UserService userService;
    private final UserServiceExt userServiceExt;
    
    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息", description = "根据用户ID获取用户详细信息")
    public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    /**
     * 根据用户名获取用户信息
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名获取用户信息", description = "根据用户名获取用户详细信息")
    public ResponseEntity<ApiResponse<UserDTO>> getUserByUsername(@PathVariable String username) {
        UserDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserDTO createdUser = userServiceExt.createUserWithPassword(request);
        return ResponseEntity.ok(ApiResponse.success("用户创建成功", createdUser));
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息", description = "更新指定用户的信息")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        UserDTO updatedUser = userServiceExt.updateUserWithPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("用户更新成功", updatedUser));
    }
    
    /**
     * 更新用户密码
     */
    @PutMapping("/{id}/password")
    @Operation(summary = "更新用户密码", description = "更新指定用户的密码")
    public ResponseEntity<ApiResponse<String>> updatePassword(
            @PathVariable Long id,
            @RequestParam String newPassword) {
        userServiceExt.updatePassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("密码更新成功"));
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除指定用户")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("用户删除成功"));
    }
    
    /**
     * 获取用户列表（分页）
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户列表", description = "分页获取用户列表")
    public ResponseEntity<ApiResponse<PageResult<UserDTO>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        // 使用扩展服务进行分页查询
        PageResult<UserDTO> result = userServiceExt.getUserList(page, size, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

