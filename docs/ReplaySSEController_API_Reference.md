# ReplaySSEController API 接口文档

## 📋 概述

`ReplaySSEController` 提供基于 **SSE (Server-Sent Events)** 的回放功能，支持实时推送历史仿真数据，并提供动态控制能力（播放、暂停、倍速、跳转）。

**核心特性**：
- ✅ **SSE 流式推送**：单向推送，浏览器原生支持
- ✅ **即时控制**：所有控制操作立即生效（< 100ms）
- ✅ **配置化延迟**：通过 `application.yml` 配置回放速度
- ✅ **sessionId = taskId**：使用仿真任务ID作为会话ID

---

## 🔧 配置说明

### application.yml 配置

```yaml
plugin:
  replay:
    sse:
      base-delay-ms: 100              # 基础延迟（实际延迟 = base-delay-ms / speed）
      timeout-ms: 1800000             # 连接超时（30分钟）
      pause-check-interval-ms: 100    # 暂停检查间隔
```

### 延迟计算公式

```
实际延迟 = base-delay-ms / speed
```

**示例**（base-delay-ms = 100）：
- speed = 0.5 → 延迟 200ms（慢放）
- speed = 1.0 → 延迟 100ms（正常）
- speed = 2.0 → 延迟 50ms（2倍速）
- speed = 5.0 → 延迟 20ms（5倍速）

---

## 📡 API 接口列表

### 1. 建立 SSE 连接（开始回放）

**接口**: `GET /replay/stream/{taskId}`

**描述**: 建立 SSE 连接，开始推送回放数据。支持动态控制（播放、暂停、倍速、跳转）。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | String | 是 | 仿真任务ID（simulation_task 表的 task_id，也是 sessionId） |

**响应类型**: `text/event-stream`

**SSE 事件类型**:

| 事件名 | 说明 | 数据格式 |
|--------|------|----------|
| `start` | 回放开始 | `{"totalSteps": 1000}` |
| `data` | 步数据 | `ReplayDataDTO` 对象 |
| `seeked` | 跳转完成 | `{"currentStep": 500}` |
| `stopped` | 已停止 | `{"currentStep": 300}` |
| `end` | 回放完成 | `{"message": "回放完成", "totalSteps": 1000}` |
| `error` | 错误 | `"没有找到回放数据"` |

**前端示例**:

```javascript
// 建立连接
const taskId = 'task-123';
const eventSource = new EventSource(`/replay/stream/${taskId}`);

// 监听开始事件
eventSource.addEventListener('start', (event) => {
  const data = JSON.parse(event.data);
  console.log('回放开始，总步数:', data.totalSteps);
});

// 监听数据事件
eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  console.log('收到步数据:', stepData);
  updateMap(stepData);  // 更新地图显示
});

// 监听跳转事件
eventSource.addEventListener('seeked', (event) => {
  const data = JSON.parse(event.data);
  console.log('跳转完成，当前步数:', data.currentStep);
});

// 监听结束事件
eventSource.addEventListener('end', (event) => {
  const data = JSON.parse(event.data);
  console.log('回放完成:', data.message);
  eventSource.close();
});

// 监听错误事件
eventSource.addEventListener('error', (event) => {
  console.error('回放错误:', event.data);
  eventSource.close();
});

// 连接错误处理
eventSource.onerror = (error) => {
  console.error('SSE 连接错误:', error);
  eventSource.close();
};
```

---

### 2. 播放/继续回放

**接口**: `POST /replay/control/{sessionId}/play`

**描述**: 开始或继续播放回放（立即生效）。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 回放会话ID（等于 taskId） |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "播放成功",
  "data": null,
  "timestamp": 1737619200000
}
```

**curl 示例**:
```bash
curl -X POST http://localhost:3822/replay/control/task-123/play
```

---

### 3. 暂停回放

**接口**: `POST /replay/control/{sessionId}/pause`

**描述**: 暂停当前回放（立即生效，保持当前步数）。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 回放会话ID（等于 taskId） |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "暂停成功",
  "data": null,
  "timestamp": 1737619200000
}
```

**curl 示例**:
```bash
curl -X POST http://localhost:3822/replay/control/task-123/pause
```

---

### 4. 停止回放

**接口**: `POST /replay/control/{sessionId}/stop`

**描述**: 停止当前回放并关闭 SSE 连接。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 回放会话ID（等于 taskId） |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "停止成功",
  "data": null,
  "timestamp": 1737619200000
}
```

**curl 示例**:
```bash
curl -X POST http://localhost:3822/replay/control/task-123/stop
```

---

### 5. 设置回放速度

**接口**: `POST /replay/control/{sessionId}/speed?speed={speed}`

**描述**: 动态调整回放速度（立即生效）。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 回放会话ID（等于 taskId） |

**查询参数**:
| 参数 | 类型 | 必填 | 说明 | 范围 |
|------|------|------|------|------|
| speed | double | 是 | 回放速度倍数 | 0.1 ~ 10.0 |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "速度设置成功",
  "data": null,
  "timestamp": 1737619200000
}
```

**curl 示例**:
```bash
# 2倍速
curl -X POST "http://localhost:3822/replay/control/task-123/speed?speed=2.0"

# 0.5倍速（慢放）
curl -X POST "http://localhost:3822/replay/control/task-123/speed?speed=0.5"
```

---

### 6. 跳转到指定步数

**接口**: `POST /replay/control/{sessionId}/seek?targetStep={step}`

**描述**: 跳转到指定的仿真步数（立即生效）。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 回放会话ID（等于 taskId） |

**查询参数**:
| 参数 | 类型 | 必填 | 说明 | 范围 |
|------|------|------|------|------|
| targetStep | long | 是 | 目标步数 | >= 0 |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "跳转成功",
  "data": null,
  "timestamp": 1737619200000
}
```

**curl 示例**:
```bash
# 跳转到第500步
curl -X POST "http://localhost:3822/replay/control/task-123/seek?targetStep=500"
```

**注意事项**:
- ⚠️ 跳转不会重新发送之前的数据
- ⚠️ 跳转后会发送 `seeked` 事件通知前端

---

### 7. 获取回放状态

**接口**: `GET /replay/control/{sessionId}/status`

**描述**: 获取当前回放会话的状态信息。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 回放会话ID（等于 taskId） |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "success",
  "data": {
    "sessionId": "task-123",
    "status": "PLAYING",
    "speed": 1.5,
    "currentStep": 250,
    "targetStep": 0,
    "seekRequested": false
  },
  "timestamp": 1737619200000
}
```

**状态字段说明**:
| 字段 | 类型 | 说明 | 可能值 |
|------|------|------|--------|
| sessionId | String | 会话ID | - |
| status | String | 回放状态 | PLAYING / PAUSED / STOPPED |
| speed | double | 当前速度 | 0.1 ~ 10.0 |
| currentStep | long | 当前步数 | >= 0 |
| targetStep | long | 跳转目标步数 | >= 0 |
| seekRequested | boolean | 是否有跳转请求 | true / false |

**curl 示例**:
```bash
curl http://localhost:3822/replay/control/task-123/status
```

---

### 8. 获取回放地图信息

**接口**: `GET /replay/map/{taskId}`

**描述**: 通过仿真任务ID获取地图JSON数据（addition字段）。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | String | 是 | 仿真任务ID |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "获取地图信息成功",
  "data": {
    "mapId": "1",
    "mapName": "测试地图",
    "addition": {
      "nodes": [...],
      "edges": [...],
      "lanes": [...]
    }
  },
  "timestamp": 1737619200000
}
```

---

### 9. 获取回放信息

**接口**: `GET /replay/info/{taskId}`

**描述**: 获取仿真任务的回放数据统计信息（总步数等）。

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | String | 是 | 仿真任务ID |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "获取回放信息成功",
  "data": {
    "totalSteps": 1000,
    "startTime": "2026-01-23 10:00:00",
    "endTime": "2026-01-23 10:16:40",
    "duration": 1000
  },
  "timestamp": 1737619200000
}
```

---

## 🎯 完整使用示例

### 前端完整代码

```javascript
class ReplayPlayer {
  constructor(taskId) {
    this.taskId = taskId;
    this.eventSource = null;
  }

  // 开始回放
  async start() {
    // 1. 获取回放信息
    const info = await this.getReplayInfo();
    console.log('回放信息:', info);

    // 2. 获取地图信息
    const mapInfo = await this.getMapInfo();
    console.log('地图信息:', mapInfo);
    this.initMap(mapInfo);

    // 3. 建立 SSE 连接
    this.eventSource = new EventSource(`/replay/stream/${this.taskId}`);

    this.eventSource.addEventListener('start', (event) => {
      const data = JSON.parse(event.data);
      console.log('回放开始，总步数:', data.totalSteps);
    });

    this.eventSource.addEventListener('data', (event) => {
      const stepData = JSON.parse(event.data);
      this.updateMap(stepData);
    });

    this.eventSource.addEventListener('end', (event) => {
      console.log('回放完成');
      this.eventSource.close();
    });

    this.eventSource.onerror = (error) => {
      console.error('SSE 错误:', error);
      this.eventSource.close();
    };
  }

  // 暂停
  async pause() {
    await fetch(`/replay/control/${this.taskId}/pause`, { method: 'POST' });
  }

  // 播放
  async play() {
    await fetch(`/replay/control/${this.taskId}/play`, { method: 'POST' });
  }

  // 停止
  async stop() {
    await fetch(`/replay/control/${this.taskId}/stop`, { method: 'POST' });
    if (this.eventSource) {
      this.eventSource.close();
    }
  }

  // 设置速度
  async setSpeed(speed) {
    await fetch(`/replay/control/${this.taskId}/speed?speed=${speed}`, {
      method: 'POST'
    });
  }

  // 跳转
  async seekTo(targetStep) {
    await fetch(`/replay/control/${this.taskId}/seek?targetStep=${targetStep}`, {
      method: 'POST'
    });
  }

  // 获取状态
  async getStatus() {
    const response = await fetch(`/replay/control/${this.taskId}/status`);
    const result = await response.json();
    return result.data;
  }

  // 获取回放信息
  async getReplayInfo() {
    const response = await fetch(`/replay/info/${this.taskId}`);
    const result = await response.json();
    return result.data;
  }

  // 获取地图信息
  async getMapInfo() {
    const response = await fetch(`/replay/map/${this.taskId}`);
    const result = await response.json();
    return result.data;
  }

  // 初始化地图
  initMap(mapInfo) {
    // 实现地图初始化逻辑
  }

  // 更新地图
  updateMap(stepData) {
    // 实现地图更新逻辑
  }
}

// 使用示例
const player = new ReplayPlayer('task-123');

// 开始回放
await player.start();

// 控制回放
await player.pause();        // 暂停
await player.play();         // 继续
await player.setSpeed(2.0);  // 2倍速
await player.seekTo(500);    // 跳转到第500步
await player.stop();         // 停止

// 查询状态
const status = await player.getStatus();
console.log('当前状态:', status);
```

---

## 📊 数据模型

### ReplayDataDTO

```json
{
  "step": 100,
  "timestamp": 1737619200000,
  "vehicles": [
    {
      "id": "vehicle-1",
      "position": { "x": 100.5, "y": 200.3 },
      "speed": 15.5,
      "angle": 90.0
    }
  ],
  "signals": [
    {
      "id": "signal-1",
      "state": "GREEN",
      "remainingTime": 30
    }
  ]
}
```

---

## ⚡ 即时控制特性

所有控制操作在回放的**任何时刻**都能**立即生效**：

| 操作 | 响应时间 | 说明 |
|------|----------|------|
| 暂停 | < 100ms | 立即停止推送，保持当前步数 |
| 播放 | < 100ms | 立即恢复推送 |
| 倍速 | < 100ms | 立即调整推送速度（每次循环重新读取） |
| 跳转 | < 100ms | 立即跳转到目标步数 |

---

## 🔍 常见问题

### Q1: sessionId 和 taskId 的关系？
**A**: sessionId = taskId，它们是同一个值，都是仿真任务ID。

### Q2: 浏览器 SSE 连接数限制？
**A**: 大多数浏览器对同一域名的 SSE 连接数有限制（通常6个）。如果需要同时回放多个任务，建议使用连接池管理。

### Q3: 速度范围是多少？
**A**: 0.1 ~ 10.0。超出范围会返回错误。

### Q4: 跳转会重新发送之前的数据吗？
**A**: 不会。跳转只是改变当前推送位置，不会重新发送历史数据。

### Q5: 如何知道回放已经结束？
**A**: 监听 `end` 事件，收到该事件表示回放完成。

### Q6: 暂停后如何恢复？
**A**: 调用 `/play` 接口即可恢复播放。

---

## 📝 注意事项

1. ✅ sessionId = taskId（仿真任务ID）
2. ✅ 所有控制操作即时生效（< 100ms）
3. ✅ 可在 yml 中配置延迟时间
4. ⚠️ 浏览器 SSE 连接数限制（通常6个）
5. ⚠️ 速度范围：0.1 ~ 10.0
6. ⚠️ 跳转不会重新发送之前的数据
7. ⚠️ SSE 是单向通信，只能服务器推送到客户端

---

**文档版本**: 1.0  
**最后更新**: 2026-01-23  
**作者**: AI Assistant
