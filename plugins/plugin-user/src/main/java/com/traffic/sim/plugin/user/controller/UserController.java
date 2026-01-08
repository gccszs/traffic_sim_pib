package com.traffic.sim.plugin.user.controller;

import com.traffic.sim.common.dto.UserDTO;
import com.traffic.sim.common.response.ApiResponse;
import com.traffic.sim.common.response.PageResult;
import com.traffic.sim.common.service.UserService;
import com.traffic.sim.plugin.auth.annotation.RequirePermission;
import com.traffic.sim.plugin.auth.annotation.RequireRole;
import com.traffic.sim.plugin.user.dto.ChangePasswordRequest;
import com.traffic.sim.plugin.user.dto.UserBanRequest;
import com.traffic.sim.plugin.user.dto.UserCreateRequest;
import com.traffic.sim.plugin.user.dto.UserUpdateByAdminRequest;
import com.traffic.sim.plugin.user.dto.UserUpdateRequest;
import com.traffic.sim.plugin.user.service.UserServiceExt;
import com.traffic.sim.common.util.RequestContext;
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
@RequestMapping("/user")
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
        String currentUserId = RequestContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("401", "未认证"));
        }

        Long currentUserIdLong = Long.parseLong(currentUserId);
        String currentRole = RequestContext.getCurrentUser().getRole();

        if (!"ADMIN".equals(currentRole) && !currentUserIdLong.equals(id)) {
            return ResponseEntity.status(403).body(ApiResponse.error("403", "无权访问其他用户信息"));
        }

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
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserDTO createdUser = userServiceExt.createUserWithPassword(request);
        return ResponseEntity.ok(ApiResponse.success("用户创建成功", createdUser));
    }

    /**
     * 更新用户信息（用户自己）
     */
    @PostMapping("/update")
    @Operation(summary = "更新用户信息", description = "更新当前用户的信息")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @Valid @RequestBody UserUpdateRequest request) {
        String currentUserId = RequestContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("401", "未认证"));
        }

        Long currentUserIdLong = Long.parseLong(currentUserId);
        UserDTO updatedUser = userServiceExt.updateUserWithPassword(currentUserIdLong, request);
        return ResponseEntity.ok(ApiResponse.success("用户更新成功", updatedUser));
    }

    /**
     * 管理员更新用户信息
     */
    @PostMapping("/admin/update")
    @Operation(summary = "管理员更新用户", description = "管理员更新指定用户的信息")
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserByAdmin(
            @RequestParam Long userId,
            @Valid @RequestBody UserUpdateByAdminRequest request) {
        UserDTO updatedUser = userServiceExt.updateUserByAdmin(userId, request);
        return ResponseEntity.ok(ApiResponse.success("用户更新成功", updatedUser));
    }

    /**
     * 修改自己的密码
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "用户修改自己的密码")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        String currentUserId = RequestContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("401", "未认证"));
        }

        Long currentUserIdLong = Long.parseLong(currentUserId);
        userServiceExt.changePassword(currentUserIdLong, request);
        return ResponseEntity.ok(ApiResponse.success("密码修改成功"));
    }

    /**
     * 管理员重置用户密码
     */
    @PostMapping("/admin/reset-password")
    @Operation(summary = "重置用户密码", description = "管理员重置指定用户的密码")
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestParam Long userId,
            @RequestParam String newPassword) {
        userServiceExt.updatePassword(userId, newPassword);
        return ResponseEntity.ok(ApiResponse.success("密码重置成功"));
    }

    /**
     * 封禁/解封用户
     */
    @PostMapping("/ban")
    @Operation(summary = "封禁/解封用户", description = "管理员封禁或解封用户")
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<String>> banUser(@Valid @RequestBody UserBanRequest request) {
        userServiceExt.banUser(request);
        String action = request.getAction().toUpperCase();
        String message = "BAN".equals(action) || "BLOCK".equals(action) ?
            "用户已封禁" : "用户已解封";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * 解封用户
     */
    @PostMapping("/unban")
    @Operation(summary = "解封用户", description = "管理员解封用户")
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<String>> unbanUser(@RequestParam Long userId) {
        userServiceExt.unbanUser(userId);
        return ResponseEntity.ok(ApiResponse.success("用户已解封"));
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
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<PageResult<UserDTO>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        PageResult<UserDTO> result = userServiceExt.getUserList(page, size, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

