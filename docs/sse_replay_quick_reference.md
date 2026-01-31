# SSE 回放控制 - 快速参考

## 核心概念

- **sessionId = taskId**：回放会话ID就是仿真任务ID
- **即时控制**：所有控制操作立即生效（< 100ms）
- **配置化延迟**：在 `application.yml` 中配置回放速度

## 配置文件

```yaml
plugin:
  replay:
    sse:
      base-delay-ms: 100              # 基础延迟（实际延迟 = base-delay-ms / speed）
      timeout-ms: 1800000             # 连接超时（30分钟）
      pause-check-interval-ms: 100    # 暂停检查间隔
```

## API 快速参考

| 操作 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 建立连接 | GET | `/replay/stream/{taskId}` | 开始 SSE 推送 |
| 播放 | POST | `/replay/control/{taskId}/play` | 开始/继续播放 |
| 暂停 | POST | `/replay/control/{taskId}/pause` | 暂停播放 |
| 停止 | POST | `/replay/control/{taskId}/stop` | 停止并关闭连接 |
| 设置速度 | POST | `/replay/control/{taskId}/speed?speed={speed}` | 调整倍速（0.1~10.0） |
| 跳转 | POST | `/replay/control/{taskId}/seek?targetStep={step}` | 跳转到指定步数 |
| 查询状态 | GET | `/replay/control/{taskId}/status` | 获取当前状态 |

## 前端示例

```javascript
// 1. 建立连接（taskId 从仿真任务列表获取）
const taskId = 'task-123';
const eventSource = new EventSource(`/replay/stream/${taskId}`);

// 2. 监听数据
eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  updateMap(stepData);  // 更新地图
});

// 3. 控制回放（使用同一个 taskId）
// 暂停
fetch(`/replay/control/${taskId}/pause`, { method: 'POST' });

// 2倍速
fetch(`/replay/control/${taskId}/speed?speed=2.0`, { method: 'POST' });

// 跳转到第500步
fetch(`/replay/control/${taskId}/seek?targetStep=500`, { method: 'POST' });
```

## SSE 事件类型

| 事件名 | 说明 | 数据示例 |
|--------|------|----------|
| `start` | 回放开始 | `{"totalSteps": 1000}` |
| `data` | 步数据 | `{"step": 100, "vehicles": [...]}` |
| `seeked` | 跳转完成 | `{"currentStep": 500}` |
| `stopped` | 已停止 | `{"currentStep": 300}` |
| `end` | 回放完成 | `{"message": "回放完成"}` |
| `error` | 错误 | `"没有找到回放数据"` |

## 即时控制特性

所有控制操作在回放的**任何时刻**都能**立即生效**：

- ⚡ **暂停**：立即停止推送，保持当前步数
- ⚡ **播放**：立即恢复推送
- ⚡ **倍速**：立即调整推送速度（每次循环重新读取）
- ⚡ **跳转**：立即跳转到目标步数

## 延迟计算

```
实际延迟 = base-delay-ms / speed
```

示例（base-delay-ms = 100）：
- speed = 0.5 → 延迟 200ms（慢放）
- speed = 1.0 → 延迟 100ms（正常）
- speed = 2.0 → 延迟 50ms（2倍速）
- speed = 5.0 → 延迟 20ms（5倍速）

## 状态说明

```json
{
  "sessionId": "task-123",      // 会话ID（等于 taskId）
  "status": "PLAYING",          // PLAYING | PAUSED | STOPPED
  "speed": 1.5,                 // 当前速度
  "currentStep": 250,           // 当前步数
  "targetStep": 0,              // 跳转目标
  "seekRequested": false        // 是否有跳转请求
}
```

## 注意事项

1. ✅ sessionId = taskId（仿真任务ID）
2. ✅ 所有控制操作即时生效（< 100ms）
3. ✅ 可在 yml 中配置延迟时间
4. ⚠️ 浏览器 SSE 连接数限制（通常6个）
5. ⚠️ 速度范围：0.1 ~ 10.0
6. ⚠️ 跳转不会重新发送之前的数据

