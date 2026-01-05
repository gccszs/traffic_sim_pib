# plugin-simulation 模块 Issue 报告

## 📋 问题概述

本文档记录了 `plugin-simulation` 模块在实现过程中发现的问题和需要改进的地方。

---

## ⚠️ 中等问题

### 1. userId获取方式不完善 ⚠️ 待修复

**问题描述**：
- `SimulationServiceImpl.createSimulation()` 方法中，userId的获取方式不完善
- 当前使用 `sessionId` 作为临时方案，但注释说明应该从请求上下文获取

**代码位置**：
- `plugins/plugin-simulation/src/main/java/com/traffic/sim/plugin/simulation/service/SimulationServiceImpl.java:76-84`

**当前实现**：
```java
// TODO: 从请求上下文获取userId，当前使用sessionId作为临时方案
// 实际应该从RequestContext.getCurrentUserId()获取
try {
    task.setUserId(Long.parseLong(sessionId));
} catch (NumberFormatException e) {
    // 如果sessionId不是数字，使用hashCode作为临时userId
    // 实际应该从认证上下文获取
    task.setUserId((long) sessionId.hashCode());
}
```

**设计文档要求**：
- 应该从认证上下文（TokenInfo）中获取真实的userId

**解决方案**：
1. 使用 `RequestContext.getCurrentUserId()` 获取当前用户ID
2. 如果无法获取，应该抛出异常而不是使用临时方案

**修复状态**：⚠️ **待修复**

---

### 2. 缺少AutoConfiguration配置 ⚠️ 待修复

**问题描述**：
- 缺少 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件
- 可能影响插件的自动加载

**解决方案**：
1. 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件
2. 在文件中添加配置类的全限定名（如果有配置类）

**修复状态**：⚠️ **待修复**

---

## 📝 其他问题

### 3. gRPC客户端实现完整 ✅ 已实现

**问题描述**：
- `PythonGrpcClient` 已完整实现gRPC调用逻辑
- 包括请求转换、响应处理等

**代码位置**：
- `plugins/plugin-simulation/src/main/java/com/traffic/sim/plugin/simulation/grpc/PythonGrpcClient.java`

**修复状态**：✅ **已实现**

---

## ✅ 检查清单

- [x] ✅ **已解决**：仿真任务创建功能已实现
- [x] ✅ **已解决**：仿真任务查询功能已实现
- [x] ✅ **已解决**：绿信比控制功能已实现
- [x] ✅ **已解决**：gRPC客户端已完整实现
- [ ] ⚠️ **待修复**：userId获取方式不完善
- [ ] ⚠️ **待修复**：缺少AutoConfiguration配置

---

## 📊 解决状态总结

| 问题 | 状态 | 优先级 | 说明 |
|------|------|--------|------|
| 1. userId获取方式不完善 | ⚠️ 待修复 | 🟡 中 | 影响数据准确性 |
| 2. 缺少AutoConfiguration配置 | ⚠️ 待修复 | 🟡 中 | 影响插件自动加载 |
| 3. gRPC客户端实现完整 | ✅ 已实现 | - | 功能完整 |

**总体状态**: ✅ **核心功能已实现，仅需修复userId获取和AutoConfiguration配置**

---

**报告日期**: 2024年  
**检查人员**: 首席检查负责官

