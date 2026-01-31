# 回放功能 API 速查表

## 🎮 回放控制 API

### SSE 连接

```
GET /replay/stream/{taskId}
```

**SSE 事件**:
- `start` → 总步数
- `ready` → 已就绪，等待播放指令 ⭐
- `data` → 步数据
- `seeked` → 跳转完成
- `stopped` → 已停止
- `end` → 回放完成
- `error` → 错误

### 播放控制

```bash
# 播放/继续
POST /replay/control/{sessionId}/play

# 暂停
POST /replay/control/{sessionId}/pause

# 停止
POST /replay/control/{sessionId}/stop

# 设置速度（0.1~10.0）
POST /replay/control/{sessionId}/speed?speed=2.0

# 跳转
POST /replay/control/{sessionId}/seek?targetStep=500

# 获取状态
GET /replay/control/{sessionId}/status
```

---

## 📊 回放历史 API

### 查询

```bash
# 获取回放历史列表（分页）
GET /replay/history/list?page=0&size=10

# 获取指定仿真任务的回放历史
GET /replay/history/simulation/{simulationTaskId}

# 获取回放历史详情
GET /replay/history/{replayTaskId}

# 统计用户回放次数
GET /replay/history/stats
```

### 管理

```bash
# 删除回放历史记录
DELETE /replay/history/{replayTaskId}
```

---

## 📍 其他 API

```bash
# 获取回放地图信息
GET /replay/map/{taskId}

# 获取回放统计信息
GET /replay/info/{taskId}
```

---

## 🔄 正确的使用流程

```javascript
// 1. 建立 SSE 连接
const eventSource = new EventSource(`/replay/stream/${taskId}`);

// 2. 等待 ready 事件
eventSource.addEventListener('ready', (event) => {
  console.log('✅ 已就绪，可以开始播放');
});

// 3. 调用播放接口（必须！）⭐
await fetch(`/replay/control/${taskId}/play`, { method: 'POST' });

// 4. 接收数据
eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  // 更新地图
});

// 5. 控制回放
await fetch(`/replay/control/${taskId}/pause`, { method: 'POST' });
await fetch(`/replay/control/${taskId}/speed?speed=2.0`, { method: 'POST' });
await fetch(`/replay/control/${taskId}/play`, { method: 'POST' });
```

---

## ⚠️ 重要提示

1. **必须先调用 play 接口才会开始推送数据**
2. **sessionId = taskId（仿真任务ID）**
3. **所有控制操作立即生效（< 100ms）**
4. **回放历史自动记录，无需手动创建**

---

## 📊 回放状态

| 状态 | 说明 | 触发条件 |
|------|------|----------|
| `CREATED` | 已创建 | SSE 连接建立 |
| `PAUSED` | 已暂停 | 初始状态 / 调用 pause |
| `PLAYING` | 播放中 | 调用 play |
| `STOPPED` | 已停止 | 调用 stop |
| `FINISHED` | 已完成 | 回放结束 |

---

**版本**: 2.0  
**更新日期**: 2026-01-23
