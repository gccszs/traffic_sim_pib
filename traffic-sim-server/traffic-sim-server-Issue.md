# traffic-sim-server 模块 Issue 报告

## 📋 问题概述

本文档记录了 `traffic-sim-server` 模块在实现过程中与设计文档不一致的问题。

---

## 🔴 严重问题

### 1. 插件依赖未启用 ✅ 已解决

**问题描述**：
- 在 `traffic-sim-server/pom.xml` 中，所有插件依赖都被注释掉了（第84-113行）
- 这导致插件模块不会被加载到应用中，插件功能无法使用

**设计文档要求**：
- `boot设计需求.md` 第3.1节明确要求：通过Maven依赖引入插件
- 所有插件都应该在 `traffic-sim-server/pom.xml` 中声明依赖

**影响**：
- ❌ 所有插件模块（plugin-auth、plugin-user、plugin-map等）无法被Spring Boot加载
- ❌ 插件中的Controller、Service、Repository等Bean不会被注册
- ❌ 应用无法正常启动或功能缺失

**修复状态**：✅ **已解决**
- 已取消注释所有插件依赖
- 已确认使用 `plugin-engine-replay` 作为统一模块名（与产品经理讨论后决定）

---

## ⚠️ 中等问题

### 2. 模块命名不一致 ✅ 已解决

**问题描述**：
- 在 `plugins/pom.xml` 中同时存在两个模块：
  - `plugin-replay`（第26行）
  - `plugin-engine-replay`（第27行）
- 在 `traffic-sim-server/pom.xml` 中只引用了 `plugin-replay`（第111行）
- 但实际目录中存在的是 `plugin-engine-replay`

**设计文档要求**：
- `boot设计需求.md` 第2.1节模块结构设计中，只定义了 `plugin-replay` 模块
- 没有 `plugin-engine-replay` 模块的定义

**影响**：
- ⚠️ 模块命名混乱，可能导致依赖错误
- ⚠️ 如果两个模块都存在，需要明确各自的职责

**修复状态**：✅ **已解决**
- 已确认 `plugin-replay` 目录下只有 Issue 文件，没有实际代码
- 已确认 `plugin-engine-replay` 是实际使用的模块，包含完整的实现
- 已在 `traffic-sim-server/pom.xml` 中使用 `plugin-engine-replay` 依赖
- 已更新 `plugins/pom.xml`，删除 `plugin-replay` 模块声明
- 已更新 `README.md`，统一使用 `plugin-engine-replay` 名称

---

## 📝 其他问题

### 3. ComponentScan配置 ✅ 已确认正确

**当前实现**：
```java
@ComponentScan(basePackages = {
    "com.traffic.sim",
    "com.traffic.sim.plugin"
})
```

**说明**：
- ✅ 当前配置是正确的，可以扫描所有插件包
- ✅ 插件使用自动配置机制（通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`），与 ComponentScan 配合工作正常

---

## ✅ 检查清单

- [x] 取消注释所有插件依赖 ✅
- [x] 确认并统一模块命名（使用 plugin-engine-replay） ✅
- [x] 更新 plugins/pom.xml，删除 plugin-replay 模块声明 ✅
- [x] 更新 README.md，统一使用 plugin-engine-replay 名称 ✅
- [ ] 验证插件能够正常加载（待测试）
- [ ] 测试应用启动后所有插件Bean是否注册成功（待测试）

---

**报告日期**: 2024年  
**检查人员**: 首席检查负责官  
**解决日期**: 2024年  
**解决状态**: ✅ 主要问题已解决，待测试验证

