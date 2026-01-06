# plugin-auth 模块 Issue 报告

## 📋 问题概述

本文档记录了 `plugin-auth` 模块在实现过程中发现的问题和需要改进的地方。

---

## 🔴 严重问题

### 1. 用户注册时密码传递问题 ⚠️ 待修复

**问题描述**：
- 在 `AuthServiceImpl.register()` 方法中，创建用户时调用 `userService.createUser(userDTO)`
- 但是 `UserDTO` 不包含密码字段（这是正确的设计，因为DTO不应该包含敏感信息）
- 导致注册时无法传递密码给 `UserService`

**代码位置**：
- `plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java:93-123`

**当前实现**：
```java
@Override
public void register(RegisterRequest request) {
    // ...
    UserDTO userDTO = new UserDTO();
    userDTO.setUsername(request.getUsername());
    // ... 其他字段
    // TODO: 密码传递问题需要解决
    userService.createUser(userDTO);  // ❌ UserDTO不包含密码字段
}
```

**设计文档要求**（`plugin-auth模块详细设计.md`）：
- 注册流程应该能够创建用户并设置密码

**解决方案**：
1. **方案1（推荐）**：使用 `UserServiceExt.createUserWithPassword()` 方法
   ```java
   // 在 AuthServiceImpl 中注入 UserServiceExt
   private final UserServiceExt userServiceExt;
   
   // 在 register 方法中
   UserCreateRequest createRequest = new UserCreateRequest();
   createRequest.setUsername(request.getUsername());
   createRequest.setPassword(request.getPassword());
   // ... 其他字段
   userServiceExt.createUserWithPassword(createRequest);
   ```

2. **方案2**：扩展 `UserService` 接口，添加 `createUser(RegisterRequest request)` 方法

**修复状态**：⚠️ **待修复**

---

## ⚠️ 中等问题

### 2. 令牌失效机制不完整 ⚠️ 待优化

**问题描述**：
- `logout()` 方法中，只将 accessToken 标记为失效
- 但没有清理对应的 refreshToken
- 注释中提到"需要维护accessToken和refreshToken的映射关系"，但未实现

**代码位置**：
- `plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java:185-201`

**当前实现**：
```java
@Override
public void logout(String token) {
    invalidatedTokens.put(token, System.currentTimeMillis());
    TokenInfo tokenInfo = jwtTokenService.parseToken(token);
    if (tokenInfo != null) {
        // 清理刷新令牌（需要找到对应的refreshToken，这里简化处理）
        // 实际实现中可能需要维护accessToken和refreshToken的映射关系
    }
}
```

**建议**：
- 维护 `accessToken -> refreshToken` 的映射关系
- 登出时同时清理 refreshToken

**修复状态**：⚠️ **待优化**

---

## 📝 其他问题

### 3. 权限列表硬编码 ⚠️ 待改进

**问题描述**：
- 在 `createTokenInfo()` 方法中，权限列表是硬编码的
- 应该从角色权限表或配置中动态获取

**代码位置**：
- `plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java:206-228`

**当前实现**：
```java
List<String> permissions = new ArrayList<>();
if ("ADMIN".equals(user.getRoleName())) {
    permissions.add("user:create");
    permissions.add("user:update");
    // ... 硬编码的权限
} else {
    permissions.add("user:query");
}
```

**建议**：
- 从数据库或配置中动态加载角色权限
- 或通过 `plugin-user` 模块的权限服务获取

**修复状态**：⚠️ **待改进**

---

## ✅ 检查清单

- [x] ✅ **已解决**：JWT令牌生成和验证功能已实现
- [x] ✅ **已解决**：验证码功能已实现
- [x] ✅ **已解决**：登录、注册、登出接口已实现
- [x] ✅ **已解决**：令牌刷新功能已实现
- [ ] ⚠️ **待修复**：用户注册时密码传递问题
- [ ] ⚠️ **待优化**：令牌失效机制不完整
- [ ] ⚠️ **待改进**：权限列表硬编码

---

## 📊 解决状态总结

| 问题 | 状态 | 优先级 | 说明 |
|------|------|--------|------|
| 1. 用户注册时密码传递问题 | ⚠️ 待修复 | 🔴 高 | 影响用户注册功能 |
| 2. 令牌失效机制不完整 | ⚠️ 待优化 | 🟡 中 | 安全性问题 |
| 3. 权限列表硬编码 | ⚠️ 待改进 | 🟡 中 | 可维护性问题 |

**总体状态**: ⚠️ **核心功能已实现，但存在需要修复的问题**

---

**报告日期**: 2024年  
**检查人员**: 首席检查负责官

