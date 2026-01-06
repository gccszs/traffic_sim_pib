# plugin-user 模块 Issue 报告

## 📋 问题概述

本文档记录了 `plugin-user` 模块在实现过程中发现的问题和需要改进的地方。

---

## ⚠️ 中等问题

### 1. 缺少AutoConfiguration配置 ⚠️ 待修复

**问题描述**：
- `UserPluginConfig` 使用 `@Configuration` 而不是 `@AutoConfiguration`
- 缺少 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件

**代码位置**：
- `plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/config/UserPluginConfig.java`

**当前实现**：
```java
@Configuration  // ❌ 应该使用 @AutoConfiguration
public class UserPluginConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**解决方案**：
1. 将 `@Configuration` 改为 `@AutoConfiguration`
2. 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件
3. 在文件中添加配置类的全限定名：`com.traffic.sim.plugin.user.config.UserPluginAutoConfiguration`

**修复状态**：⚠️ **待修复**

---

## 📝 其他问题

### 2. UserService接口设计问题 ✅ 已通过扩展接口解决

**问题描述**：
- `UserService.createUser()` 方法接收 `UserDTO`，但 `UserDTO` 不包含密码字段
- 这导致无法通过标准接口创建带密码的用户

**解决方案**：
- ✅ **已解决**：通过 `UserServiceExt` 接口提供了 `createUserWithPassword()` 方法
- 其他模块（如 `plugin-auth`）应该使用 `UserServiceExt` 接口

**修复状态**：✅ **已解决**（通过扩展接口）

---

## ✅ 检查清单

- [x] ✅ **已解决**：用户CRUD操作已实现
- [x] ✅ **已解决**：密码验证功能已实现
- [x] ✅ **已解决**：密码加密功能已实现（BCrypt）
- [x] ✅ **已解决**：用户扩展接口已实现（UserServiceExt）
- [ ] ⚠️ **待修复**：缺少AutoConfiguration配置

---

## 📊 解决状态总结

| 问题 | 状态 | 优先级 | 说明 |
|------|------|--------|------|
| 1. 缺少AutoConfiguration配置 | ⚠️ 待修复 | 🟡 中 | 影响插件自动加载 |
| 2. UserService接口设计问题 | ✅ 已解决 | - | 通过扩展接口解决 |

**总体状态**: ✅ **核心功能已实现，仅需修复AutoConfiguration配置**

---

**报告日期**: 2024年  
**检查人员**: 首席检查负责官

