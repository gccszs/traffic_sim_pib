# SSE 回放控制流程说明

## 概述

回放功能通过 **Server-Sent Events (SSE)** 实现实时数据推送，并通过 **RESTful API** 实现动态控制（播放、暂停、倍速、跳转）。

**核心概念**：
- **sessionId = taskId**：回放会话ID就是仿真任务ID（simulation_task 表的 task_id）
- **即时控制**：所有控制操作（暂停、倍速、跳转）在回放的任何时刻都能立即生效
- **配置化延迟**：回放推送延迟可在 `application.yml` 中配置

## 配置说明

在 `application.yml` 中配置回放参数：

```yaml
plugin:
  replay:
    sse:
      # SSE 回放基础延迟时间（毫秒）
      # 每推送一步数据的基础延迟，实际延迟 = base-delay-ms / speed
      # 例如：base-delay-ms=100, speed=2.0, 实际延迟=50ms
      base-delay-ms: 100
      # SSE 连接超时时间（毫秒）
      timeout-ms: 1800000  # 30分钟
      # 暂停时的状态检查间隔（毫秒）
      pause-check-interval-ms: 100
```

**配置说明**：
- `base-delay-ms`: 基础延迟时间，控制回放的基础速度
- `timeout-ms`: SSE 连接超时时间，防止长时间占用连接
- `pause-check-interval-ms`: 暂停时的状态检查频率，平衡响应性和CPU占用

## 架构设计

### 核心组件

1. **ReplayProperties**: 配置属性类
   - 从 `application.yml` 读取回放配置
   - 提供配置化的延迟时间和超时设置

2. **SseReplayControlService**: 管理回放会话状态
   - 存储每个会话的控制状态（播放/暂停/停止、速度、当前步数等）
   - **sessionId = taskId**（仿真任务ID）
   - 提供控制接口（play、pause、stop、setSpeed、seekTo）

3. **ReplaySSEController**: SSE 数据推送
   - 建立 SSE 连接，推送回放数据
   - **即时响应**控制状态变化（每次循环都检查状态）
   - 使用配置化的延迟时间

4. **ReplayController**: 控制接口
   - 提供 RESTful API 控制回放行为
   - 修改会话状态，影响 SSE 推送逻辑

## API 接口

### 1. 建立 SSE 连接

```http
GET /replay/stream/{taskId}
```

**功能**: 建立 SSE 连接，开始推送回放数据

**参数**:
- `taskId`: 仿真任务ID（simulation_task 表的 task_id，也是 sessionId）

**重要说明**：
- **sessionId = taskId**：回放会话ID就是仿真任务ID
- 前端使用同一个 taskId 来建立 SSE 连接和进行控制操作

**SSE 事件类型**:
- `start`: 回放开始，包含总步数
  ```json
  {
    "totalSteps": 1000
  }
  ```

- `data`: 每一步的数据
  ```json
  {
    "step": 100,
    "timestamp": 1234567890,
    "vehicles": [...]
  }
  ```

- `seeked`: 跳转完成
  ```json
  {
    "currentStep": 500
  }
  ```

- `stopped`: 回放停止
  ```json
  {
    "currentStep": 300
  }
  ```

- `end`: 回放完成
  ```json
  {
    "message": "回放完成",
    "totalSteps": 1000
  }
  ```

- `error`: 错误信息
  ```json
  "没有找到回放数据"
  ```

### 2. 播放/继续

```http
POST /replay/control/{sessionId}/play
```

**功能**: 开始或继续播放回放（**即时生效**）

**参数**:
- `sessionId`: 会话ID（**等于 taskId**，仿真任务ID）

**即时控制特性**：
- 调用后立即恢复数据推送
- 无延迟，下一个循环即生效

**响应**:
```json
{
  "code": 200,
  "message": "播放成功"
}
```

### 3. 暂停

```http
POST /replay/control/{sessionId}/pause
```

**功能**: 暂停当前回放（**即时生效**）

**参数**:
- `sessionId`: 会话ID（**等于 taskId**）

**即时控制特性**：
- 调用后立即停止推送数据
- 保持当前步数不变
- SSE 连接保持打开状态

**响应**:
```json
{
  "code": 200,
  "message": "暂停成功"
}
```

### 4. 停止

```http
POST /replay/control/{sessionId}/stop
```

**功能**: 停止当前回放（**即时生效**，会关闭 SSE 连接）

**参数**:
- `sessionId`: 会话ID（**等于 taskId**）

**即时控制特性**：
- 调用后立即停止推送
- 发送 `stopped` 事件
- 关闭 SSE 连接并清理会话

**响应**:
```json
{
  "code": 200,
  "message": "停止成功"
}
```

### 5. 设置速度

```http
POST /replay/control/{sessionId}/speed?speed={speed}
```

**功能**: 动态调整回放速度（**即时生效**）

**参数**:
- `sessionId`: 会话ID（**等于 taskId**）
- `speed`: 播放速度（0.1 ~ 10.0）
  - 0.5 = 0.5倍速（慢放）
  - 1.0 = 正常速度
  - 2.0 = 2倍速（快放）

**即时控制特性**：
- 调用后立即调整推送速度
- 实际延迟 = base-delay-ms / speed
- 例如：base-delay-ms=100, speed=2.0, 实际延迟=50ms

**响应**:
```json
{
  "code": 200,
  "message": "速度设置成功"
}
```

### 6. 跳转步数

```http
POST /replay/control/{sessionId}/seek?targetStep={targetStep}
```

**功能**: 跳转到指定的仿真步数（**即时生效**）

**参数**:
- `sessionId`: 会话ID（**等于 taskId**）
- `targetStep`: 目标步数（>= 0）

**即时控制特性**：
- 调用后立即跳转到目标步数
- 发送 `seeked` 事件
- 从目标步数继续推送

**响应**:
```json
{
  "code": 200,
  "message": "跳转成功"
}
```

### 7. 获取回放状态

```http
GET /replay/control/{sessionId}/status
```

**功能**: 获取当前回放会话的状态信息

**参数**:
- `sessionId`: 会话ID（**等于 taskId**）

**响应**:
```json
{
  "code": 200,
  "data": {
    "sessionId": "task-123",
    "status": "PLAYING",
    "speed": 1.5,
    "currentStep": 250,
    "targetStep": 0,
    "seekRequested": false
  }
}
```

**状态说明**:
- `sessionId`: 会话ID（**等于 taskId**）
- `status`: 回放状态
  - `PLAYING`: 播放中
  - `PAUSED`: 已暂停
  - `STOPPED`: 已停止
- `speed`: 当前播放速度
- `currentStep`: 当前步数
- `targetStep`: 跳转目标步数
- `seekRequested`: 是否有跳转请求

## 使用流程

### 前端完整流程示例

```javascript
// ============================================================
// 核心概念：sessionId = taskId（仿真任务ID）
// ============================================================

// 1. 建立 SSE 连接（使用仿真任务ID）
const taskId = 'task-123';  // 从仿真任务列表获取的 taskId
const eventSource = new EventSource(`/replay/stream/${taskId}`);

// 2. 监听 SSE 事件
eventSource.addEventListener('start', (event) => {
  const data = JSON.parse(event.data);
  console.log('回放开始，总步数:', data.totalSteps);
});

eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  console.log('收到步数据:', stepData);
  // 更新地图上的车辆位置
  updateVehicles(stepData.vehicles);
});

eventSource.addEventListener('seeked', (event) => {
  const data = JSON.parse(event.data);
  console.log('跳转完成，当前步数:', data.currentStep);
});

eventSource.addEventListener('stopped', (event) => {
  const data = JSON.parse(event.data);
  console.log('回放已停止，当前步数:', data.currentStep);
  eventSource.close();
});

eventSource.addEventListener('end', (event) => {
  console.log('回放完成');
  eventSource.close();
});

eventSource.addEventListener('error', (event) => {
  console.error('SSE 错误:', event.data);
  eventSource.close();
});

// 3. 控制回放（使用同一个 taskId 作为 sessionId）

// 暂停（即时生效）
async function pauseReplay() {
  await fetch(`/replay/control/${taskId}/pause`, {
    method: 'POST'
  });
  console.log('暂停指令已发送，立即生效');
}

// 继续播放（即时生效）
async function playReplay() {
  await fetch(`/replay/control/${taskId}/play`, {
    method: 'POST'
  });
  console.log('播放指令已发送，立即生效');
}

// 设置2倍速（即时生效）
async function setSpeed2x() {
  await fetch(`/replay/control/${taskId}/speed?speed=2.0`, {
    method: 'POST'
  });
  console.log('倍速指令已发送，立即生效');
}

// 设置0.5倍速（慢放，即时生效）
async function setSpeed05x() {
  await fetch(`/replay/control/${taskId}/speed?speed=0.5`, {
    method: 'POST'
  });
  console.log('慢放指令已发送，立即生效');
}

// 跳转到第500步（即时生效）
async function seekToStep500() {
  await fetch(`/replay/control/${taskId}/seek?targetStep=500`, {
    method: 'POST'
  });
  console.log('跳转指令已发送，立即生效');
}

// 停止回放（即时生效）
async function stopReplay() {
  await fetch(`/replay/control/${taskId}/stop`, {
    method: 'POST'
  });
  console.log('停止指令已发送，立即生效');
  eventSource.close();
}

// 获取当前状态
async function getStatus() {
  const response = await fetch(`/replay/control/${taskId}/status`);
  const result = await response.json();
  console.log('当前状态:', result.data);
  // 输出示例：
  // {
  //   sessionId: "task-123",
  //   status: "PLAYING",
  //   speed: 2.0,
  //   currentStep: 150,
  //   targetStep: 0,
  //   seekRequested: false
  // }
}

// 4. 实时控制示例：在回放过程中随时调整

// 示例：播放 -> 暂停 -> 调整倍速 -> 继续播放 -> 跳转
async function demoRealtimeControl() {
  // 开始播放
  await playReplay();
  
  // 播放5秒后暂停
  await sleep(5000);
  await pauseReplay();
  console.log('已暂停，可以查看当前画面');
  
  // 暂停3秒后继续，并设置为2倍速
  await sleep(3000);
  await setSpeed2x();
  await playReplay();
  console.log('继续播放，2倍速');
  
  // 播放5秒后跳转到第1000步
  await sleep(5000);
  await seekToStep(1000);
  console.log('已跳转到第1000步');
  
  // 继续播放...
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
```

## 技术实现细节

### 1. 会话管理

- 每个 SSE 连接创建一个回放会话
- **sessionId = taskId**（仿真任务ID）
- 会话状态存储在内存中（ConcurrentHashMap）
- SSE 连接关闭时自动清理会话

### 2. 状态同步（即时控制）

```
前端控制请求 → ReplayController → SseReplayControlService (更新内存状态)
                                            ↓
                                    ReplaySSEController (每次循环读取状态)
                                            ↓
                                    立即调整推送行为 → 前端 SSE 接收
```

**即时控制的实现**：
- SSE 推送线程在每次循环中都检查控制状态
- 状态存储在内存中（ConcurrentHashMap），读写极快
- 控制操作修改状态后，下一次循环立即生效
- 延迟极低（< 100ms，取决于 pause-check-interval-ms 配置）

### 3. 控制逻辑

**暂停**:
- 设置状态为 `PAUSED`
- SSE 推送线程检测到暂停状态后，进入等待循环
- 不推送数据，但保持连接
- 每 `pause-check-interval-ms` 检查一次状态

**播放**:
- 设置状态为 `PLAYING`
- SSE 推送线程继续推送数据

**倍速**:
- 修改 `speed` 字段
- SSE 推送线程根据速度动态计算延迟时间
- 延迟时间 = `base-delay-ms` / `speed`
- 每次循环都重新读取 `speed`，实现即时调整

**跳转**:
- 设置 `targetStep` 和 `seekRequested` 标志
- SSE 推送线程检测到跳转请求后，直接修改 `currentStep`
- 清除跳转标志，继续推送

**停止**:
- 设置状态为 `STOPPED`
- SSE 推送线程检测到停止状态后，发送 `stopped` 事件并关闭连接

### 4. 配置化延迟

```java
// 从配置读取基础延迟时间
final long baseDelayMs = replayProperties.getSse().getBaseDelayMs();

// 根据当前速度动态计算延迟
double currentSpeed = controlState.getSpeed();
long delayMs = (long) (baseDelayMs / currentSpeed);
Thread.sleep(delayMs);
```

**延迟计算示例**：
- `base-delay-ms=100`, `speed=1.0` → 延迟 100ms（正常速度）
- `base-delay-ms=100`, `speed=2.0` → 延迟 50ms（2倍速）
- `base-delay-ms=100`, `speed=0.5` → 延迟 200ms（0.5倍速）
- `base-delay-ms=100`, `speed=5.0` → 延迟 20ms（5倍速）

### 5. 性能优化

- **异步推送**: 使用线程池异步推送数据，不阻塞主线程
- **即时响应**: 每次循环都检查状态，控制操作立即生效
- **状态检查频率**: 暂停时每 `pause-check-interval-ms` 检查一次状态，避免CPU空转
- **内存管理**: SSE 连接关闭时自动清理会话状态
- **超时设置**: SSE 连接超时时间可配置（默认30分钟）
- **配置化**: 延迟时间可在 yml 中配置，无需重新编译

## 注意事项

1. **sessionId = taskId**: 前端需要使用仿真任务的 `taskId` 作为会话ID进行控制
2. **即时控制**: 所有控制操作在回放的任何时刻都能立即生效（延迟 < 100ms）
3. **配置化延迟**: 可在 `application.yml` 中调整 `base-delay-ms` 来控制回放速度
4. **SSE 连接限制**: 浏览器对同一域名的 SSE 连接数有限制（通常6个）
5. **网络断开**: SSE 连接断开后，前端需要重新建立连接
6. **状态一致性**: 控制操作是异步的，但延迟极低（通常 < 100ms）
7. **跳转行为**: 跳转会立即生效，但不会重新发送之前的数据
8. **速度范围**: 支持 0.1x ~ 10x 倍速，超出范围会返回错误

## 与旧版本的区别

### 旧版本（已废弃）
- 控制接口直接操作 `replay_task` 表
- 需要用户认证
- 控制状态存储在数据库中
- 无法实时响应控制操作
- 延迟硬编码在代码中

### 新版本
- **sessionId = taskId**：直接使用仿真任务ID
- 控制接口操作内存中的会话状态
- 不需要用户认证
- **即时响应**控制操作（延迟 < 100ms）
- SSE 推送线程直接读取状态，延迟极低
- **配置化延迟**：可在 yml 中配置
- 支持在回放的任何时刻进行控制

## 测试建议

1. **基础播放**: 建立 SSE 连接，验证数据正常推送
2. **暂停/继续**: 测试暂停和继续功能，验证即时生效
3. **倍速**: 测试不同速度（0.5x, 1x, 2x, 5x），验证即时调整
4. **跳转**: 测试跳转到不同步数，验证即时跳转
5. **停止**: 测试停止功能，验证连接正常关闭
6. **状态查询**: 测试状态查询接口，验证状态实时更新
7. **即时控制**: 在回放过程中随时调整倍速、暂停、跳转，验证立即生效
8. **配置测试**: 修改 yml 配置，验证延迟时间生效
9. **边界情况**: 测试无效的速度、步数等
10. **并发**: 测试多个回放会话同时运行
11. **sessionId**: 验证使用 taskId 作为 sessionId 能正常工作

## 配置调优建议

### 基础延迟时间 (base-delay-ms)

```yaml
# 快速回放（适合长时间仿真）
base-delay-ms: 50

# 正常回放（默认）
base-delay-ms: 100

# 慢速回放（适合详细观察）
base-delay-ms: 200
```

### 暂停检查间隔 (pause-check-interval-ms)

```yaml
# 高响应性（CPU占用稍高）
pause-check-interval-ms: 50

# 平衡（默认）
pause-check-interval-ms: 100

# 低CPU占用（响应性稍低）
pause-check-interval-ms: 200
```

### 超时时间 (timeout-ms)

```yaml
# 短时仿真（10分钟）
timeout-ms: 600000

# 中等仿真（30分钟，默认）
timeout-ms: 1800000

# 长时仿真（1小时）
timeout-ms: 3600000
```

## 未来扩展

1. **断点续播**: 记录上次播放位置，支持从断点继续
2. **播放列表**: 支持连续播放多个仿真任务
3. **实时统计**: 在 SSE 中推送实时统计数据
4. **录制功能**: 支持录制回放片段
5. **分享功能**: 生成回放分享链接
6. **多速率预设**: 提供常用速率快捷按钮（0.5x, 1x, 2x, 5x）
7. **进度条拖动**: 支持通过进度条拖动跳转
8. **关键帧标记**: 支持标记和跳转到关键时刻
9. **性能监控**: 监控 SSE 推送性能和延迟
10. **自动调速**: 根据网络状况自动调整推送速度

## 总结

新的 SSE 回放控制系统具有以下核心特性：

1. ✅ **sessionId = taskId**: 简化了会话管理，直接使用仿真任务ID
2. ✅ **即时控制**: 所有控制操作在回放的任何时刻都能立即生效（< 100ms）
3. ✅ **配置化延迟**: 可在 yml 中灵活配置延迟时间，无需重新编译
4. ✅ **动态倍速**: 支持 0.1x ~ 10x 倍速，即时调整
5. ✅ **精确跳转**: 可跳转到任意步数，立即生效
6. ✅ **状态查询**: 随时查询当前播放状态
7. ✅ **无需认证**: 简化了使用流程
8. ✅ **自动清理**: 连接关闭时自动清理会话

这套系统为前端提供了强大而灵活的回放控制能力，用户体验接近视频播放器的流畅度！🎉

