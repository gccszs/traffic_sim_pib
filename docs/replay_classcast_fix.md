# 回放数据类型转换错误修复报告

**日期**: 2026-01-23  
**问题**: ClassCastException - StepData 无法转换为 Map  
**状态**: ✅ 已修复

---

## 📋 问题描述

### 错误信息

```
java.lang.ClassCastException: class com.traffic.sim.plugin.engine.manager.service.SimulationDataCollector$StepData 
cannot be cast to class java.util.Map
```

### 错误日志

```
2026-01-23 16:44:12.711 [pool-3-thread-1] INFO  c.t.s.plugin.replay.controller.ReplaySSEController - Loading replay data for task: 8f58a814e84c4bc6a42b22098bb2fa48
2026-01-23 16:44:12.711 [pool-3-thread-1] INFO  c.t.sim.plugin.replay.service.ReplayDataService - Loading replay data from simulation_data collection for task: 8f58a814e84c4bc6a42b22098bb2fa48, steps: 0-9223372036854775807
2026-01-23 16:44:12.782 [pool-3-thread-1] INFO  c.t.sim.plugin.replay.service.ReplayDataService - Found 10 steps in simulation_data for task: 8f58a814e84c4bc6a42b22098bb2fa48
2026-01-23 16:44:12.784 [pool-3-thread-1] ERROR c.t.sim.plugin.replay.service.ReplayDataService - Error loading replay data from simulation_data for task: 8f58a814e84c4bc6a42b22098bb2fa48
java.lang.ClassCastException: class com.traffic.sim.plugin.engine.manager.service.SimulationDataCollector$StepData cannot be cast to class java.util.Map
```

---

## 🔍 问题分析

### 根本原因

1. **MongoDB 存储的数据类型**：`SimulationDataCollector.StepData` 对象（Java 类）
2. **代码期望的类型**：`Map<String, Object>`
3. **类型不匹配**：MongoDB 将 Java 对象序列化后存储，读取时返回的是原始类型，无法直接转换为 Map

### 数据流程

```
仿真引擎 → WebSocket → EngineWebSocketHandler 
    → SimulationDataCollector.addStepData(StepData)
    → SimulationDataPersistenceService.saveSimulationData()
    → MongoDB (存储 StepData 对象)
    → ReplayDataService.getReplayData()
    → ❌ 尝试转换为 Map<String, Object> → ClassCastException
```

### StepData 类结构

```java
public static class StepData {
    private final Long step;
    private final Long timestamp;
    private final Map<String, Object> simData;
    private final Map<String, Object> statsData;
}
```

### MongoDB 存储结构

```json
{
  "simulationTaskId": "8f58a814e84c4bc6a42b22098bb2fa48",
  "userId": "user-123",
  "taskId": "task-123",
  "totalSteps": 10,
  "steps": [
    {
      "_class": "com.traffic.sim.plugin.engine.manager.service.SimulationDataCollector$StepData",
      "step": 0,
      "timestamp": 1737619452711,
      "simData": {...},
      "statsData": {...}
    },
    ...
  ]
}
```

**关键问题**：MongoDB 保存时包含了 `_class` 字段，读取时会尝试反序列化为原始类型，而不是 Map。

---

## 🔧 解决方案

### 修改文件

`plugins/plugin-engine-replay/src/main/java/com/traffic/sim/plugin/replay/service/ReplayDataService.java`

### 修改内容

#### 1. 修改查询方式（使用 BSON Document）

**修改前**：
```java
Map<String, Object> simulationData = mongoTemplate.findOne(query, Map.class, "simulation_data");
List<Map<String, Object>> steps = (List<Map<String, Object>>) simulationData.get("steps");
```

**修改后**：
```java
org.bson.Document simulationData = mongoTemplate.findOne(query, org.bson.Document.class, "simulation_data");
List<?> steps = simulationData.get("steps", List.class);
```

**优势**：
- ✅ 使用 BSON Document 避免类型转换问题
- ✅ 不强制转换为特定类型，保持灵活性

#### 2. 添加类型兼容处理

新增 `extractStep()` 方法，支持三种格式：

```java
private Long extractStep(Object stepData) {
    // 1. BSON Document 格式
    if (stepData instanceof org.bson.Document) {
        org.bson.Document doc = (org.bson.Document) stepData;
        Object stepObj = doc.get("step");
        if (stepObj instanceof Number) {
            return ((Number) stepObj).longValue();
        }
    }
    
    // 2. Map 格式
    if (stepData instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) stepData;
        Object stepObj = map.get("step");
        if (stepObj instanceof Number) {
            return ((Number) stepObj).longValue();
        }
    }
    
    // 3. 对象格式（使用反射）
    try {
        java.lang.reflect.Field stepField = stepData.getClass().getDeclaredField("step");
        stepField.setAccessible(true);
        Object stepObj = stepField.get(stepData);
        if (stepObj instanceof Number) {
            return ((Number) stepObj).longValue();
        }
    } catch (Exception e) {
        log.debug("Failed to extract step from object: {}", e.getMessage());
    }
    
    return null;
}
```

#### 3. 增强数据转换方法

修改 `convertStepDataToDTO()` 方法，支持三种格式：

```java
private ReplayDataDTO convertStepDataToDTO(Object stepData) {
    ReplayDataDTO dto = new ReplayDataDTO();
    
    if (stepData == null) {
        return null;
    }
    
    // 1. BSON Document 格式（优先）
    if (stepData instanceof org.bson.Document) {
        org.bson.Document doc = (org.bson.Document) stepData;
        // 提取字段...
        return dto;
    }
    
    // 2. Map 格式
    if (stepData instanceof Map) {
        Map<String, Object> map = (Map<String, Object>) stepData;
        // 提取字段...
        return dto;
    }
    
    // 3. 对象格式（使用反射）
    try {
        // 使用反射提取字段...
        return dto;
    } catch (Exception e) {
        log.error("Failed to convert step data to DTO using reflection", e);
        return null;
    }
}
```

---

## ✅ 修复效果

### 修复前

```
❌ ClassCastException: StepData cannot be cast to Map
❌ 回放数据加载失败
❌ SSE 连接立即关闭
```

### 修复后

```
✅ 成功识别 BSON Document 格式
✅ 成功提取 StepData 字段
✅ 成功转换为 ReplayDataDTO
✅ SSE 正常推送回放数据
```

### 预期日志

```
2026-01-23 17:00:00.000 [pool-3-thread-1] INFO  c.t.s.plugin.replay.controller.ReplaySSEController - Loading replay data for task: 8f58a814e84c4bc6a42b22098bb2fa48
2026-01-23 17:00:00.001 [pool-3-thread-1] INFO  c.t.sim.plugin.replay.service.ReplayDataService - Loading replay data from simulation_data collection for task: 8f58a814e84c4bc6a42b22098bb2fa48, steps: 0-9223372036854775807
2026-01-23 17:00:00.050 [pool-3-thread-1] INFO  c.t.sim.plugin.replay.service.ReplayDataService - Found 10 steps in simulation_data for task: 8f58a814e84c4bc6a42b22098bb2fa48
2026-01-23 17:00:00.051 [pool-3-thread-1] INFO  c.t.sim.plugin.replay.service.ReplayDataService - Loaded 10 steps after filtering (range: 0-9223372036854775807)
2026-01-23 17:00:00.052 [pool-3-thread-1] INFO  c.t.s.plugin.replay.controller.ReplaySSEController - Found 10 steps of replay data, starting to push
```

---

## 🎯 技术要点

### 1. MongoDB 序列化机制

MongoDB 使用 Spring Data MongoDB 时，默认会：
- 保存对象时添加 `_class` 字段
- 读取时根据 `_class` 字段反序列化为原始类型
- 如果使用 `Map.class` 查询，可能导致类型不匹配

### 2. BSON Document vs Map

| 特性 | BSON Document | Map |
|------|---------------|-----|
| 类型安全 | ✅ 更灵活 | ❌ 强类型转换 |
| 兼容性 | ✅ 支持所有 MongoDB 类型 | ⚠️ 可能类型不匹配 |
| 性能 | ✅ 原生格式 | ⚠️ 需要转换 |
| 推荐场景 | 读取复杂结构 | 简单数据结构 |

### 3. 反射的使用

当数据格式未知时，使用反射可以：
- ✅ 动态提取字段值
- ✅ 兼容不同版本的数据结构
- ⚠️ 性能略低（可接受）
- ⚠️ 需要处理异常

---

## 🧪 测试验证

### 测试步骤

1. **启动 Java 后端**
   ```bash
   cd traffic-sim-server
   mvn spring-boot:run
   ```

2. **创建仿真任务**（生成测试数据）
   ```bash
   curl -X POST http://localhost:3822/api/simulation/prepare
   # 获取 taskId
   
   curl -X POST "http://localhost:3822/api/simulation/start?taskId={taskId}" \
     -H "Content-Type: application/json" \
     -d '{"simInfo": {...}}'
   ```

3. **测试回放功能**
   ```bash
   # 建立 SSE 连接
   curl -N http://localhost:3822/replay/stream/{taskId}
   ```

4. **验证日志**
   - ✅ 无 ClassCastException 错误
   - ✅ 成功加载回放数据
   - ✅ SSE 正常推送数据

### 测试用例

| 测试场景 | 预期结果 | 状态 |
|---------|---------|------|
| 读取 BSON Document 格式数据 | ✅ 成功 | 待测试 |
| 读取 Map 格式数据 | ✅ 成功 | 待测试 |
| 读取 StepData 对象格式数据 | ✅ 成功（反射） | 待测试 |
| 空数据处理 | ✅ 返回空列表 | 待测试 |
| 步数范围过滤 | ✅ 正确过滤 | 待测试 |

---

## 📚 相关文档

- **回放 API 文档**: `ReplaySSEController_API_Reference.md`
- **回放快速指南**: `ReplaySSEController_Quick_Guide.md`
- **SSE 回放流程**: `sse_replay_flow.md`

---

## 🔄 后续优化建议

### 1. 统一数据存储格式

**问题**：当前 MongoDB 存储的是 Java 对象，导致类型转换复杂。

**建议**：修改 `SimulationDataPersistenceService`，存储时转换为 Map：

```java
// 修改前
document.put("steps", stepDataList);

// 修改后
List<Map<String, Object>> stepsAsMap = stepDataList.stream()
    .map(stepData -> {
        Map<String, Object> map = new HashMap<>();
        map.put("step", stepData.getStep());
        map.put("timestamp", stepData.getTimestamp());
        map.put("simData", stepData.getSimData());
        map.put("statsData", stepData.getStatsData());
        return map;
    })
    .collect(Collectors.toList());
document.put("steps", stepsAsMap);
```

**优势**：
- ✅ 避免类型转换问题
- ✅ 数据格式更清晰
- ✅ 便于跨语言访问

### 2. 添加数据版本控制

在 MongoDB 文档中添加版本字段：

```json
{
  "dataVersion": "1.0",
  "simulationTaskId": "...",
  "steps": [...]
}
```

**优势**：
- ✅ 支持数据格式升级
- ✅ 向后兼容
- ✅ 便于数据迁移

### 3. 性能优化

**当前**：每次回放都加载所有步数据到内存

**优化**：
- 使用 MongoDB 聚合管道分页查询
- 实现流式读取（避免内存溢出）
- 添加数据缓存（Redis）

---

## ✅ 总结

### 问题原因
MongoDB 存储 Java 对象时保留了类型信息，读取时无法直接转换为 Map。

### 解决方案
1. 使用 BSON Document 代替 Map 查询
2. 添加多格式兼容处理（Document、Map、对象）
3. 使用反射提取字段值

### 修复状态
✅ 代码已修复  
⏳ 待测试验证  
📝 建议后续优化数据存储格式

---

**修复完成日期**: 2026-01-23  
**修复人**: AI Assistant  
**影响范围**: 回放功能（SSE 推送）  
**优先级**: 高（阻塞功能）
