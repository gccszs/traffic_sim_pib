# 回放接口文档 - 前端开发参考

## 📖 文档说明

本文档提供完整的回放功能 API 接口说明，包括请求参数、响应格式和使用示例。

**基础 URL**: `http://localhost:3822`

**核心概念**: 
- **回放 = 仿真历史**：直接使用仿真任务ID（taskId）进行回放
- **sessionId = taskId**：回放会话ID就是仿真任务ID
- **数据源**: MongoDB 的 `simulation_data` 集合

---

## 📋 目录

1. [获取仿真任务列表](#1-获取仿真任务列表)
2. [获取回放地图信息](#2-获取回放地图信息)
3. [获取回放统计信息](#3-获取回放统计信息)
4. [建立 SSE 连接开始回放](#4-建立-sse-连接开始回放)
5. [控制回放 - 播放](#5-控制回放---播放)
6. [控制回放 - 暂停](#6-控制回放---暂停)
7. [控制回放 - 停止](#7-控制回放---停止)
8. [控制回放 - 设置速度](#8-控制回放---设置速度)
9. [控制回放 - 跳转步数](#9-控制回放---跳转步数)
10. [获取回放状态](#10-获取回放状态)
11. [完整使用示例](#11-完整使用示例)

---

## 1. 获取仿真任务列表

获取所有可回放的仿真任务列表。

### 请求

```http
GET /simulation/list?page={page}&size={size}
```

### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 10 | 每页数量 |

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "taskId": "task-20260122-001",
        "name": "城市交通仿真",
        "mapId": "map-001",
        "mapName": "北京市中心区域",
        "status": "COMPLETED",
        "createTime": "2026-01-22 10:00:00",
        "updateTime": "2026-01-22 10:16:40"
      },
      {
        "taskId": "task-20260122-002",
        "name": "高速公路仿真",
        "mapId": "map-002",
        "mapName": "京沪高速",
        "status": "COMPLETED",
        "createTime": "2026-01-22 11:00:00",
        "updateTime": "2026-01-22 11:20:30"
      }
    ],
    "total": 25,
    "page": 1,
    "size": 10
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | string | 仿真任务ID（用于回放） |
| name | string | 任务名称 |
| mapId | string | 地图ID |
| mapName | string | 地图名称 |
| status | string | 任务状态（COMPLETED 表示可回放） |
| createTime | string | 创建时间 |
| updateTime | string | 更新时间 |

---

## 2. 获取回放地图信息

获取仿真任务的地图 JSON 数据，用于初始化地图。

### 请求

```http
GET /replay/map/{taskId}
```

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | string | 是 | 仿真任务ID |

### 响应示例

```json
{
  "code": 200,
  "message": "获取地图信息成功",
  "data": {
    "taskId": "task-20260122-001",
    "mapId": "map-001",
    "mapName": "北京市中心区域",
    "mapJson": {
      "nodes": [
        {
          "id": "node-1",
          "x": 116.397428,
          "y": 39.90923,
          "type": "intersection"
        }
      ],
      "edges": [
        {
          "id": "edge-1",
          "from": "node-1",
          "to": "node-2",
          "lanes": 3,
          "length": 500
        }
      ],
      "junctions": [],
      "connections": []
    }
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | string | 仿真任务ID |
| mapId | string | 地图ID |
| mapName | string | 地图名称 |
| mapJson | object | 地图 JSON 数据（SUMO 格式） |

---

## 3. 获取回放统计信息

获取回放数据的统计信息，如总步数、时间范围等。

### 请求

```http
GET /replay/info/{taskId}
```

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | string | 是 | 仿真任务ID |

### 响应示例

```json
{
  "code": 200,
  "message": "获取回放信息成功",
  "data": {
    "taskId": "task-20260122-001",
    "totalSteps": 1000,
    "startTime": "2026-01-22 10:00:00",
    "endTime": "2026-01-22 10:16:40",
    "duration": 1000,
    "dataCount": 1000
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | string | 仿真任务ID |
| totalSteps | long | 总步数 |
| startTime | string | 开始时间 |
| endTime | string | 结束时间 |
| duration | long | 持续时间（秒） |
| dataCount | long | 数据记录数 |

---

## 4. 建立 SSE 连接开始回放

通过 Server-Sent Events (SSE) 建立连接，实时接收回放数据。

### 请求

```http
GET /replay/stream/{taskId}
```

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | string | 是 | 仿真任务ID（也是 sessionId） |

### SSE 事件类型

#### 1. start 事件

回放开始时触发，包含总步数信息。

```json
{
  "totalSteps": 1000
}
```

#### 2. data 事件

每一步的仿真数据，包含车辆位置、速度等信息。

```json
{
  "step": 100,
  "timestamp": 1737561600000,
  "vehicles": [
    {
      "id": "vehicle-1",
      "x": 116.397428,
      "y": 39.90923,
      "speed": 15.5,
      "angle": 90,
      "lane": "edge-1_0",
      "type": "car"
    },
    {
      "id": "vehicle-2",
      "x": 116.398428,
      "y": 39.91023,
      "speed": 12.3,
      "angle": 180,
      "lane": "edge-2_1",
      "type": "bus"
    }
  ],
  "statistics": {
    "totalVehicles": 150,
    "averageSpeed": 14.2,
    "waitingVehicles": 5
  }
}
```

#### 3. seeked 事件

跳转完成时触发。

```json
{
  "currentStep": 500
}
```

#### 4. stopped 事件

回放停止时触发。

```json
{
  "currentStep": 300
}
```

#### 5. end 事件

回放完成时触发。

```json
{
  "message": "回放完成",
  "totalSteps": 1000
}
```

#### 6. error 事件

发生错误时触发。

```
"没有找到回放数据"
```

### 前端示例

```javascript
const taskId = 'task-20260122-001';
const eventSource = new EventSource(`/replay/stream/${taskId}`);

// 监听开始事件
eventSource.addEventListener('start', (event) => {
  const data = JSON.parse(event.data);
  console.log('回放开始，总步数:', data.totalSteps);
  initProgressBar(data.totalSteps);
});

// 监听数据事件
eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  console.log('当前步数:', stepData.step);
  updateVehicles(stepData.vehicles);
  updateStatistics(stepData.statistics);
});

// 监听跳转事件
eventSource.addEventListener('seeked', (event) => {
  const data = JSON.parse(event.data);
  console.log('跳转完成，当前步数:', data.currentStep);
});

// 监听停止事件
eventSource.addEventListener('stopped', (event) => {
  const data = JSON.parse(event.data);
  console.log('回放已停止，当前步数:', data.currentStep);
  eventSource.close();
});

// 监听结束事件
eventSource.addEventListener('end', (event) => {
  console.log('回放完成');
  eventSource.close();
});

// 监听错误事件
eventSource.addEventListener('error', (event) => {
  console.error('SSE 错误:', event);
  eventSource.close();
});
```

---

## 5. 控制回放 - 播放

开始或继续播放回放（即时生效）。

### 请求

```http
POST /replay/control/{sessionId}/play
```

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话ID（等于 taskId） |

### 响应示例

```json
{
  "code": 200,
  "message": "播放成功",
  "data": null
}
```

### 前端示例

```javascript
const taskId = 'task-20260122-001';

async function play() {
  const response = await fetch(`/replay/control/${taskId}/play`, {
    method: 'POST'
  });
  const result = await response.json();
  console.log(result.message);
}
```

---

## 6. 控制回放 - 暂停

暂停当前回放（即时生效）。

### 请求

```http
POST /replay/control/{sessionId}/pause
```

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话ID（等于 taskId） |

### 响应示例

```json
{
  "code": 200,
  "message": "暂停成功",
  "data": null
}
```

### 前端示例

```javascript
async function pause() {
  const response = await fetch(`/replay/control/${taskId}/pause`, {
    method: 'POST'
  });
  const result = await response.json();
  console.log(result.message);
}
```

---

## 7. 控制回放 - 停止

停止当前回放并关闭 SSE 连接（即时生效）。

### 请求

```http
POST /replay/control/{sessionId}/stop
```

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话ID（等于 taskId） |

### 响应示例

```json
{
  "code": 200,
  "message": "停止成功",
  "data": null
}
```

### 前端示例

```javascript
async function stop() {
  const response = await fetch(`/replay/control/${taskId}/stop`, {
    method: 'POST'
  });
  const result = await response.json();
  console.log(result.message);
  eventSource.close();  // 关闭 SSE 连接
}
```

---

## 8. 控制回放 - 设置速度

动态调整回放速度（即时生效）。

### 请求

```http
POST /replay/control/{sessionId}/speed?speed={speed}
```

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话ID（等于 taskId） |

### 查询参数

| 参数 | 类型 | 必填 | 范围 | 说明 |
|------|------|------|------|------|
| speed | double | 是 | 0.1 ~ 10.0 | 播放速度倍数 |

### 速度说明

| 速度值 | 说明 |
|--------|------|
| 0.5 | 0.5倍速（慢放） |
| 1.0 | 正常速度 |
| 2.0 | 2倍速 |
| 5.0 | 5倍速 |
| 10.0 | 10倍速（最快） |

### 响应示例

```json
{
  "code": 200,
  "message": "速度设置成功",
  "data": null
}
```

### 前端示例

```javascript
async function setSpeed(speed) {
  const response = await fetch(`/replay/control/${taskId}/speed?speed=${speed}`, {
    method: 'POST'
  });
  const result = await response.json();
  console.log(result.message);
}

// 使用示例
setSpeed(0.5);  // 慢放
setSpeed(1.0);  // 正常
setSpeed(2.0);  // 2倍速
setSpeed(5.0);  // 5倍速
```

---

## 9. 控制回放 - 跳转步数

跳转到指定的仿真步数（即时生效）。

### 请求

```http
POST /replay/control/{sessionId}/seek?targetStep={targetStep}
```

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话ID（等于 taskId） |

### 查询参数

| 参数 | 类型 | 必填 | 范围 | 说明 |
|------|------|------|------|------|
| targetStep | long | 是 | >= 0 | 目标步数 |

### 响应示例

```json
{
  "code": 200,
  "message": "跳转成功",
  "data": null
}
```

### 前端示例

```javascript
async function seekTo(targetStep) {
  const response = await fetch(`/replay/control/${taskId}/seek?targetStep=${targetStep}`, {
    method: 'POST'
  });
  const result = await response.json();
  console.log(result.message);
}

// 使用示例
seekTo(500);   // 跳转到第500步
seekTo(0);     // 跳转到开始
seekTo(999);   // 跳转到第999步
```

---

## 10. 获取回放状态

获取当前回放会话的状态信息。

### 请求

```http
GET /replay/control/{sessionId}/status
```

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 会话ID（等于 taskId） |

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "task-20260122-001",
    "status": "PLAYING",
    "speed": 2.0,
    "currentStep": 250,
    "targetStep": 0,
    "seekRequested": false
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | string | 会话ID（等于 taskId） |
| status | string | 回放状态：PLAYING（播放中）、PAUSED（已暂停）、STOPPED（已停止） |
| speed | double | 当前播放速度 |
| currentStep | long | 当前步数 |
| targetStep | long | 跳转目标步数 |
| seekRequested | boolean | 是否有跳转请求 |

### 前端示例

```javascript
async function getStatus() {
  const response = await fetch(`/replay/control/${taskId}/status`);
  const result = await response.json();
  console.log('当前状态:', result.data);
  return result.data;
}

// 使用示例
const status = await getStatus();
console.log('当前步数:', status.currentStep);
console.log('播放速度:', status.speed);
console.log('播放状态:', status.status);
```

---

## 11. 完整使用示例

### 完整的回放流程

```javascript
class ReplayPlayer {
  constructor() {
    this.taskId = null;
    this.eventSource = null;
    this.totalSteps = 0;
  }

  // 1. 初始化回放
  async init(taskId) {
    this.taskId = taskId;

    try {
      // 获取地图信息
      const mapResponse = await fetch(`/replay/map/${taskId}`);
      const mapResult = await mapResponse.json();
      this.initMap(mapResult.data.mapJson);

      // 获取回放信息
      const infoResponse = await fetch(`/replay/info/${taskId}`);
      const infoResult = await infoResponse.json();
      this.totalSteps = infoResult.data.totalSteps;
      console.log('总步数:', this.totalSteps);

      // 建立 SSE 连接
      this.connectSSE();
    } catch (error) {
      console.error('初始化失败:', error);
    }
  }

  // 2. 建立 SSE 连接
  connectSSE() {
    this.eventSource = new EventSource(`/replay/stream/${this.taskId}`);

    this.eventSource.addEventListener('start', (event) => {
      const data = JSON.parse(event.data);
      console.log('回放开始，总步数:', data.totalSteps);
    });

    this.eventSource.addEventListener('data', (event) => {
      const stepData = JSON.parse(event.data);
      this.updateVehicles(stepData.vehicles);
      this.updateProgress(stepData.step);
    });

    this.eventSource.addEventListener('end', (event) => {
      console.log('回放完成');
      this.eventSource.close();
    });

    this.eventSource.addEventListener('error', (event) => {
      console.error('SSE 错误:', event);
      this.eventSource.close();
    });
  }

  // 3. 控制方法
  async play() {
    await fetch(`/replay/control/${this.taskId}/play`, { method: 'POST' });
  }

  async pause() {
    await fetch(`/replay/control/${this.taskId}/pause`, { method: 'POST' });
  }

  async stop() {
    await fetch(`/replay/control/${this.taskId}/stop`, { method: 'POST' });
    this.eventSource.close();
  }

  async setSpeed(speed) {
    await fetch(`/replay/control/${this.taskId}/speed?speed=${speed}`, {
      method: 'POST'
    });
  }

  async seekTo(targetStep) {
    await fetch(`/replay/control/${this.taskId}/seek?targetStep=${targetStep}`, {
      method: 'POST'
    });
  }

  async getStatus() {
    const response = await fetch(`/replay/control/${this.taskId}/status`);
    const result = await response.json();
    return result.data;
  }

  // 4. UI 更新方法
  initMap(mapJson) {
    // 初始化地图
    console.log('初始化地图:', mapJson);
  }

  updateVehicles(vehicles) {
    // 更新车辆位置
    console.log('更新车辆:', vehicles.length);
  }

  updateProgress(currentStep) {
    // 更新进度条
    const progress = (currentStep / this.totalSteps) * 100;
    console.log('进度:', progress.toFixed(2) + '%');
  }
}

// 使用示例
const player = new ReplayPlayer();

// 初始化并开始回放
await player.init('task-20260122-001');

// 控制回放
await player.pause();           // 暂停
await player.setSpeed(2.0);     // 2倍速
await player.play();            // 继续播放
await player.seekTo(500);       // 跳转到第500步
const status = await player.getStatus();  // 获取状态
await player.stop();            // 停止
```

---

## 📌 重要说明

### 1. sessionId = taskId

回放会话ID就是仿真任务ID，前端使用同一个 `taskId` 进行所有操作。

### 2. 即时控制

所有控制操作（播放、暂停、倍速、跳转）在回放的任何时刻都能立即生效（延迟 < 100ms）。

### 3. SSE 连接管理

- 浏览器对同一域名的 SSE 连接数有限制（通常6个）
- 页面关闭或组件卸载时记得调用 `eventSource.close()`
- 网络断开后需要重新建立连接

### 4. 错误处理

所有接口都返回统一的响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

错误响应：

```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

### 5. 配置说明

回放延迟可在服务端配置文件中调整：

```yaml
plugin:
  replay:
    sse:
      base-delay-ms: 100              # 基础延迟（实际延迟 = base-delay-ms / speed）
      timeout-ms: 1800000             # 连接超时（30分钟）
      pause-check-interval-ms: 100    # 暂停检查间隔
```

---

## 🔗 相关文档

- [回放 API 迁移指南](./replay_api_migration_guide.md)
- [SSE 回放控制流程](./sse_replay_control_flow.md)
- [SSE 回放快速参考](./sse_replay_quick_reference.md)
- [正确的回放流程](./correct_replay_flow.md)

---

## 📞 技术支持

如有问题，请联系后端开发团队或查阅相关文档。






