# 回放数据修复说明

## 问题描述

### 原始问题
MongoDB 中保存的仿真数据（`simulation_data` 集合）只包含**统计量信息**（如平均速度、车辆数量等），而**没有保存每一步的详细车辆行为数据**（位置、速度、加速度、车道等）。

这导致回放功能无法正常工作，因为回放需要的是每辆车在每一步的详细状态，而不仅仅是统计摘要。

### 概念澄清

**仿真（Simulation）vs 回放（Replay）：**

1. **仿真（Simulation）**：
   - 实时运行交通仿真引擎
   - 引擎计算每辆车的行为（位置、速度、加速度等）
   - 通过 WebSocket 实时推送数据到前端
   - 数据应该被保存到 MongoDB 用于后续回放

2. **回放（Replay）**：
   - 基于已保存的仿真数据
   - 重新播放历史仿真过程
   - 不需要重新运行仿真引擎
   - 从 MongoDB 读取数据并按步播放

**关键理解：** 回放的数据来源于仿真过程中保存的数据，因此仿真时必须保存完整的车辆行为数据。

## 问题根源

### 代码问题位置

在 `EngineWebSocketHandler.java` 的 `processSimulationStepEnd()` 方法中：

```java
// ❌ 错误的实现
SimulationDataCollector.StepData stepData = new SimulationDataCollector.StepData(
    currentStep,
    stepCollector.getTimestamp(),
    data,  // 只包含 sim_one_step 消息的 data，不包含车辆详细数据
    infoStat  // 统计数据
);
```

### 数据流分析

**当前的数据流：**

1. 引擎发送多个 WebSocket 消息：
   - `produce_veh`：新产生的车辆
   - `veh_run`：运行中的车辆状态
   - `controller_run`：信号灯状态
   - `sim_one_step`：步结束标记

2. `SimulationDataCollector` 收集所有消息

3. `buildCompleteStepData()` 从收集的消息中提取并整合所有车辆数据

4. **问题**：保存到 MongoDB 时，只保存了 `sim_one_step` 的 `data`，而不是 `buildCompleteStepData()` 构建的完整数据

## 解决方案

### 修改内容

**文件：** `plugins/plugin-engine-manager/src/main/java/com/traffic/sim/plugin/engine/manager/websocket/EngineWebSocketHandler.java`

**修改：** 在保存数据时，使用 `buildCompleteStepData()` 构建的完整车辆数据

```java
// ✅ 正确的实现
// 构建完整的仿真步数据（包含所有车辆的详细信息）
Map<String, Object> completeStepData = buildCompleteStepData(
        currentStep, stepCollector.getMessages());

// 构建 StepData，保存完整的车辆数据用于回放
SimulationDataCollector.StepData stepData = new SimulationDataCollector.StepData(
        currentStep,
        stepCollector.getTimestamp(),
        completeStepData,  // ✅ 保存完整的车辆数据（包含所有车辆的位置、速度等）
        infoStat  // 统计数据
);
```

### 完整数据结构

修复后，MongoDB 中保存的每一步数据包含：

```json
{
  "step": 100,
  "timestamp": 1737532800000,
  "simData": {
    "step": 100,
    "over_step:": 100,
    "vehicles": [
      {
        "id": "veh_001",
        "x": 123.45,
        "y": 678.90,
        "speed": 15.5,
        "acceleration": 0.5,
        "roadId": "road_1",
        "laneId": "lane_1",
        "cur_spd": 15.5,
        "last_spd": 15.0,
        "link_id": "road_1",
        "lane_id": "lane_1"
      },
      // ... 更多车辆
    ],
    "signals": [
      {
        "controllerId": "signal_1",
        "phase": "green",
        "remainingTime": 30
      },
      // ... 更多信号灯
    ]
  },
  "statsData": {
    "speed_min": 0.0,
    "speed_max": 25.0,
    "speed_ave": 15.5,
    "car_number": 50,
    "jam_index": 0.2,
    // ... 更多统计信息
  }
}
```

## 数据去重逻辑

`buildCompleteStepData()` 方法实现了智能的车辆数据去重：

1. **按车辆 ID 去重**：同一辆车在一步中可能出现在多个消息中（`produce_veh` 和 `veh_run`）
2. **数据合并**：后面的消息数据会覆盖前面的（`veh_run` 优先于 `produce_veh`）
3. **加速度计算**：
   - `veh_run` 消息：`acceleration = cur_spd - last_spd`
   - `produce_veh` 消息：`acceleration = 0.0`（首次出现）

## MongoDB 数据结构

### 集合：`simulation_data`

```json
{
  "_id": ObjectId("..."),
  "simulationTaskId": "session_123",
  "userId": "1",
  "taskId": "task_456",
  "totalSteps": 1000,
  "startTime": 1737532800000,
  "endTime": 1737536400000,
  "createdAt": 1737536400000,
  "steps": [
    {
      "step": 1,
      "timestamp": 1737532800000,
      "simData": {
        "step": 1,
        "vehicles": [...],
        "signals": [...]
      },
      "statsData": {...}
    },
    // ... 更多步
  ]
}
```

## 回放功能实现建议

### 1. 回放数据查询

```java
// 从 MongoDB 查询仿真数据
Query query = Query.query(Criteria.where("taskId").is(taskId));
Map<String, Object> simulationData = mongoTemplate.findOne(query, Map.class, "simulation_data");

List<Map<String, Object>> steps = (List<Map<String, Object>>) simulationData.get("steps");
```

### 2. 回放播放逻辑

```java
// 按步播放
for (Map<String, Object> stepData : steps) {
    Long step = (Long) stepData.get("step");
    Map<String, Object> simData = (Map<String, Object>) stepData.get("simData");
    Map<String, Object> statsData = (Map<String, Object>) stepData.get("statsData");
    
    // 提取车辆数据
    List<Map<String, Object>> vehicles = (List<Map<String, Object>>) simData.get("vehicles");
    
    // 构建 WebSocket 消息发送给前端
    WebSocketInfo message = new WebSocketInfo("frontend", "simdata", System.currentTimeMillis());
    message.setData(simData);
    
    // 发送给前端
    frontendWebSocketHandler.sendMessageToFrontend(sessionId, message);
    
    // 控制播放速度
    Thread.sleep(playbackSpeed);
}
```

### 3. 回放控制

- **播放/暂停**：控制是否继续发送下一步数据
- **快进/慢放**：调整 `playbackSpeed` 参数
- **跳转**：直接从 `steps` 数组中读取指定步的数据

## 验证方法

### 1. 运行仿真

启动一个仿真任务，让它运行几步。

### 2. 检查 MongoDB

```javascript
// 连接 MongoDB
use traffic_sim

// 查询最新的仿真数据
db.simulation_data.find().sort({createdAt: -1}).limit(1).pretty()

// 检查第一步的数据结构
db.simulation_data.findOne({}, {
  "steps": {$slice: 1}
}).steps[0]

// 验证车辆数据是否存在
db.simulation_data.aggregate([
  {$unwind: "$steps"},
  {$limit: 1},
  {$project: {
    step: "$steps.step",
    vehicleCount: {$size: "$steps.simData.vehicles"},
    sampleVehicle: {$arrayElemAt: ["$steps.simData.vehicles", 0]}
  }}
])
```

### 3. 预期结果

- `steps` 数组中每个元素都应该包含 `simData.vehicles` 数组
- `vehicles` 数组中每个车辆应该包含：`id`, `x`, `y`, `speed`, `acceleration`, `roadId`, `laneId` 等字段
- 车辆数量应该与统计数据中的 `car_number` 一致

## 注意事项

1. **数据量**：完整的车辆数据会比统计数据大很多，需要注意 MongoDB 存储空间
2. **性能**：异步保存机制确保不会阻塞仿真进程
3. **数据清理**：建议定期清理旧的仿真数据，或者提供数据导出功能

## 后续优化建议

1. **数据压缩**：对车辆数据进行压缩存储
2. **分片存储**：大型仿真任务可以分多个文档存储
3. **索引优化**：为 `taskId`, `userId`, `step` 等字段创建索引
4. **数据导出**：提供导出为 CSV/JSON 的功能

## 总结

这次修复解决了回放功能的核心问题：**确保仿真过程中保存完整的车辆行为数据**。修复后，MongoDB 中的数据可以完整地重现仿真过程，为回放功能提供了数据基础。

