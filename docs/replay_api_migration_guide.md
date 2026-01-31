# 回放 API 迁移指南

## ⚠️ 重要说明

**核心概念变更**：回放 = 仿真历史

- ❌ **旧版本**：需要创建 `replay_task`，查询 `replay_task` 表
- ✅ **新版本**：直接使用 `simulation_task` 的 `taskId`，查询 MongoDB 的 `simulation_data`

## 已废弃的接口（不要再使用！）

以下接口已完全废弃，调用会返回错误提示：

| 旧接口 | 状态 | 替代方案 |
|--------|------|----------|
| `POST /replay/create` | ❌ 已废弃 | 不需要创建，直接使用 `taskId` |
| `GET /replay/list` | ❌ 已废弃 | 使用 `GET /simulation/list` |
| `GET /replay/{taskId}` | ⚠️ 重定向 | 使用 `GET /replay/info/{taskId}` |
| `GET /replay/{taskId}/data` | ❌ 已废弃 | 使用 `GET /replay/stream/{taskId}` (SSE) |
| `POST /replay/{taskId}/control` | ❌ 已废弃 | 使用新的控制接口 |
| `DELETE /replay/{taskId}` | ❌ 已废弃 | 删除仿真任务即可 |

## 新的回放流程

### 1. 获取可回放的任务列表

**旧版本**（❌ 不要使用）：
```javascript
// 错误：查询 replay_task 表
GET /replay/list?page=1&size=10
```

**新版本**（✅ 正确）：
```javascript
// 正确：查询 simulation_task 表
GET /simulation/list?page=1&size=10

// 响应示例
{
  "code": 200,
  "data": {
    "records": [
      {
        "taskId": "task-123",
        "name": "城市交通仿真",
        "status": "COMPLETED",
        "createTime": "2026-01-22 10:00:00"
      }
    ]
  }
}
```

### 2. 获取回放信息

**旧版本**（⚠️ 已重定向）：
```javascript
// 会查询 replay_task 表（错误！）
GET /replay/{taskId}
```

**新版本**（✅ 正确）：
```javascript
// 正确：从 MongoDB 查询仿真数据统计
GET /replay/info/{taskId}

// 响应示例
{
  "code": 200,
  "data": {
    "taskId": "task-123",
    "totalSteps": 1000,
    "startTime": "2026-01-22 10:00:00",
    "endTime": "2026-01-22 10:16:40"
  }
}
```

### 3. 获取地图信息

**新接口**（✅ 必需）：
```javascript
// 从 MongoDB 获取地图 JSON
GET /replay/map/{taskId}

// 响应示例
{
  "code": 200,
  "data": {
    "mapId": "map-001",
    "mapName": "城市地图",
    "mapJson": { /* 地图数据 */ }
  }
}
```

### 4. 建立 SSE 连接开始回放

**旧版本**（❌ 不要使用）：
```javascript
// 错误：需要先创建 replay_task
POST /replay/create
// 然后获取数据
GET /replay/{taskId}/data?startStep=0&endStep=100
```

**新版本**（✅ 正确）：
```javascript
// 正确：直接使用 taskId 建立 SSE 连接
const taskId = 'task-123';  // 从仿真任务列表获取
const eventSource = new EventSource(`/replay/stream/${taskId}`);

eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  updateMap(stepData);  // 更新地图
});
```

### 5. 控制回放

**旧版本**（❌ 不要使用）：
```javascript
// 错误：旧的控制接口
POST /replay/{taskId}/control
{
  "action": "PAUSE",
  "speed": 2.0
}
```

**新版本**（✅ 正确）：
```javascript
// 正确：使用新的 SSE 控制接口
const taskId = 'task-123';

// 暂停
POST /replay/control/${taskId}/pause

// 播放
POST /replay/control/${taskId}/play

// 设置速度
POST /replay/control/${taskId}/speed?speed=2.0

// 跳转
POST /replay/control/${taskId}/seek?targetStep=500

// 停止
POST /replay/control/${taskId}/stop

// 查询状态
GET /replay/control/${taskId}/status
```

## 完整的前端迁移示例

### 旧版本代码（❌ 不要使用）

```javascript
// 1. 获取回放列表（错误：查询 replay_task）
const response = await fetch('/replay/list?page=1&size=10');
const replayList = await response.json();

// 2. 创建回放任务（错误：创建 replay_task）
await fetch('/replay/create', {
  method: 'POST',
  body: JSON.stringify({
    simulationTaskId: 'task-123',
    name: '回放任务'
  })
});

// 3. 获取回放数据（错误：轮询获取）
const dataResponse = await fetch('/replay/task-123/data?startStep=0&endStep=100');
const data = await dataResponse.json();

// 4. 控制回放（错误：旧接口）
await fetch('/replay/task-123/control', {
  method: 'POST',
  body: JSON.stringify({ action: 'PAUSE' })
});
```

### 新版本代码（✅ 正确）

```javascript
// 1. 获取仿真任务列表（正确：查询 simulation_task）
const response = await fetch('/simulation/list?page=1&size=10');
const taskList = await response.json();
const taskId = taskList.data.records[0].taskId;  // 获取 taskId

// 2. 获取回放信息（正确：从 MongoDB 查询）
const infoResponse = await fetch(`/replay/info/${taskId}`);
const replayInfo = await infoResponse.json();
console.log('总步数:', replayInfo.data.totalSteps);

// 3. 获取地图信息（正确：从 MongoDB 查询）
const mapResponse = await fetch(`/replay/map/${taskId}`);
const mapData = await mapResponse.json();
initMap(mapData.data.mapJson);  // 初始化地图

// 4. 建立 SSE 连接（正确：实时推送）
const eventSource = new EventSource(`/replay/stream/${taskId}`);

eventSource.addEventListener('start', (event) => {
  const data = JSON.parse(event.data);
  console.log('回放开始，总步数:', data.totalSteps);
});

eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  updateVehicles(stepData.vehicles);  // 更新车辆位置
});

eventSource.addEventListener('end', (event) => {
  console.log('回放完成');
  eventSource.close();
});

// 5. 控制回放（正确：新的 SSE 控制接口）
// 暂停
await fetch(`/replay/control/${taskId}/pause`, { method: 'POST' });

// 2倍速
await fetch(`/replay/control/${taskId}/speed?speed=2.0`, { method: 'POST' });

// 跳转到第500步
await fetch(`/replay/control/${taskId}/seek?targetStep=500`, { method: 'POST' });

// 查询状态
const statusResponse = await fetch(`/replay/control/${taskId}/status`);
const status = await statusResponse.json();
console.log('当前状态:', status.data);
```

## 数据流对比

### 旧版本（❌ 错误）

```
前端 → POST /replay/create → 创建 replay_task（MySQL）
     → GET /replay/list → 查询 replay_task（MySQL）
     → GET /replay/{taskId} → 查询 replay_task（MySQL）❌
     → GET /replay/{taskId}/data → 查询 replay_data（？）❌
```

### 新版本（✅ 正确）

```
前端 → GET /simulation/list → 查询 simulation_task（MySQL）
     → GET /replay/info/{taskId} → 查询 simulation_data（MongoDB）✅
     → GET /replay/map/{taskId} → 查询 map（MongoDB）✅
     → GET /replay/stream/{taskId} → SSE 推送 simulation_data（MongoDB）✅
     → POST /replay/control/{taskId}/... → 控制 SSE 推送✅
```

## 关键变更总结

| 方面 | 旧版本 | 新版本 |
|------|--------|--------|
| 数据源 | `replay_task` 表（MySQL）❌ | `simulation_data` 集合（MongoDB）✅ |
| 任务创建 | 需要创建 `replay_task` ❌ | 不需要，直接使用 `taskId` ✅ |
| 数据获取 | 轮询 REST API ❌ | SSE 实时推送 ✅ |
| 控制方式 | 单一控制接口 ❌ | 独立的控制接口 ✅ |
| 用户认证 | 需要 ❌ | 不需要 ✅ |
| 会话管理 | 数据库存储 ❌ | 内存存储 ✅ |
| 响应速度 | 慢（数据库查询）❌ | 快（内存状态）✅ |

## 迁移检查清单

- [ ] 移除所有 `POST /replay/create` 调用
- [ ] 将 `GET /replay/list` 改为 `GET /simulation/list`
- [ ] 将 `GET /replay/{taskId}` 改为 `GET /replay/info/{taskId}`
- [ ] 添加 `GET /replay/map/{taskId}` 调用获取地图
- [ ] 将数据获取改为 SSE：`GET /replay/stream/{taskId}`
- [ ] 更新控制逻辑使用新的控制接口
- [ ] 移除所有 `replay_task` 相关的数据结构
- [ ] 确保使用 `taskId`（仿真任务ID）而不是 `replayTaskId`
- [ ] 测试完整的回放流程

## 常见问题

### Q: 为什么旧接口返回错误？
A: 因为旧接口查询 `replay_task` 表，但回放数据实际存储在 MongoDB 的 `simulation_data` 中。新版本直接使用仿真任务ID查询 MongoDB。

### Q: 如何获取回放列表？
A: 直接调用 `/simulation/list` 获取仿真任务列表，所有已完成的仿真任务都可以回放。

### Q: 还需要创建回放任务吗？
A: 不需要！回放 = 仿真历史，直接使用仿真任务的 `taskId` 即可。

### Q: sessionId 是什么？
A: sessionId = taskId（仿真任务ID），用于标识回放会话。

### Q: 如何删除回放？
A: 删除对应的仿真任务即可，回放数据会随之删除。

## 技术支持

如有问题，请参考：
- 详细文档：`docs/sse_replay_control_flow.md`
- 快速参考：`docs/sse_replay_quick_reference.md`
- 正确流程：`docs/correct_replay_flow.md`

