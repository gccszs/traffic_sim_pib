# ReplaySSEController 快速使用指南

## 🚀 5分钟快速上手

### 步骤1: 获取 taskId

从仿真任务列表中获取要回放的任务ID：

```javascript
// 获取仿真任务列表
const response = await fetch('/simulation/list?page=1&size=10');
const result = await response.json();
const taskId = result.data.items[0].taskId;  // 选择第一个任务
```

### 步骤2: 建立 SSE 连接

```javascript
const eventSource = new EventSource(`/replay/stream/${taskId}`);

// 监听数据
eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  console.log('步数:', stepData.step);
  // 更新地图显示
  updateMap(stepData);
});
```

### 步骤3: 控制回放

```javascript
// 暂停
await fetch(`/replay/control/${taskId}/pause`, { method: 'POST' });

// 2倍速
await fetch(`/replay/control/${taskId}/speed?speed=2.0`, { method: 'POST' });

// 跳转到第500步
await fetch(`/replay/control/${taskId}/seek?targetStep=500`, { method: 'POST' });

// 继续播放
await fetch(`/replay/control/${taskId}/play`, { method: 'POST' });

// 停止
await fetch(`/replay/control/${taskId}/stop`, { method: 'POST' });
```

---

## 📡 SSE 事件说明

### 事件类型

| 事件名 | 触发时机 | 数据示例 |
|--------|----------|----------|
| `start` | 回放开始 | `{"totalSteps": 1000}` |
| `data` | 每一步数据 | `{step: 100, vehicles: [...]}` |
| `seeked` | 跳转完成 | `{"currentStep": 500}` |
| `stopped` | 用户停止 | `{"currentStep": 300}` |
| `end` | 回放完成 | `{"message": "回放完成"}` |
| `error` | 发生错误 | `"没有找到回放数据"` |

### 监听所有事件

```javascript
const eventSource = new EventSource(`/replay/stream/${taskId}`);

eventSource.addEventListener('start', (e) => {
  const data = JSON.parse(e.data);
  console.log('✅ 回放开始，总步数:', data.totalSteps);
});

eventSource.addEventListener('data', (e) => {
  const stepData = JSON.parse(e.data);
  console.log('📊 步数:', stepData.step);
  updateMap(stepData);
});

eventSource.addEventListener('seeked', (e) => {
  const data = JSON.parse(e.data);
  console.log('⏩ 跳转完成，当前步数:', data.currentStep);
});

eventSource.addEventListener('stopped', (e) => {
  const data = JSON.parse(e.data);
  console.log('⏹️ 已停止，当前步数:', data.currentStep);
});

eventSource.addEventListener('end', (e) => {
  console.log('✅ 回放完成');
  eventSource.close();
});

eventSource.addEventListener('error', (e) => {
  console.error('❌ 错误:', e.data);
});

eventSource.onerror = (error) => {
  console.error('❌ 连接错误:', error);
  eventSource.close();
};
```

---

## 🎮 控制接口速查

### 播放控制

```javascript
// 暂停
POST /replay/control/{taskId}/pause

// 播放/继续
POST /replay/control/{taskId}/play

// 停止
POST /replay/control/{taskId}/stop
```

### 速度控制

```javascript
// 设置速度（0.1 ~ 10.0）
POST /replay/control/{taskId}/speed?speed=2.0

// 常用速度
speed=0.5   // 慢放
speed=1.0   // 正常
speed=2.0   // 2倍速
speed=5.0   // 5倍速
```

### 跳转控制

```javascript
// 跳转到指定步数
POST /replay/control/{taskId}/seek?targetStep=500

// 跳转到开始
POST /replay/control/{taskId}/seek?targetStep=0

// 跳转到中间
POST /replay/control/{taskId}/seek?targetStep=500
```

### 状态查询

```javascript
// 获取当前状态
GET /replay/control/{taskId}/status

// 响应示例
{
  "sessionId": "task-123",
  "status": "PLAYING",      // PLAYING / PAUSED / STOPPED
  "speed": 1.5,
  "currentStep": 250,
  "targetStep": 0,
  "seekRequested": false
}
```

---

## 🎯 实用代码片段

### 1. 简单回放播放器

```javascript
class SimpleReplayPlayer {
  constructor(taskId) {
    this.taskId = taskId;
    this.eventSource = null;
  }

  start() {
    this.eventSource = new EventSource(`/replay/stream/${this.taskId}`);
    
    this.eventSource.addEventListener('data', (event) => {
      const stepData = JSON.parse(event.data);
      this.onData(stepData);
    });
    
    this.eventSource.addEventListener('end', () => {
      this.onEnd();
      this.eventSource.close();
    });
  }

  pause() {
    fetch(`/replay/control/${this.taskId}/pause`, { method: 'POST' });
  }

  play() {
    fetch(`/replay/control/${this.taskId}/play`, { method: 'POST' });
  }

  stop() {
    fetch(`/replay/control/${this.taskId}/stop`, { method: 'POST' });
    this.eventSource?.close();
  }

  setSpeed(speed) {
    fetch(`/replay/control/${this.taskId}/speed?speed=${speed}`, { 
      method: 'POST' 
    });
  }

  seekTo(step) {
    fetch(`/replay/control/${this.taskId}/seek?targetStep=${step}`, { 
      method: 'POST' 
    });
  }

  // 回调函数（需要实现）
  onData(stepData) {
    console.log('收到数据:', stepData);
  }

  onEnd() {
    console.log('回放结束');
  }
}

// 使用
const player = new SimpleReplayPlayer('task-123');
player.start();
player.setSpeed(2.0);  // 2倍速
```

### 2. 带进度条的播放器

```javascript
class ReplayPlayerWithProgress {
  constructor(taskId) {
    this.taskId = taskId;
    this.totalSteps = 0;
    this.currentStep = 0;
  }

  async start() {
    // 获取总步数
    const info = await this.getReplayInfo();
    this.totalSteps = info.totalSteps;

    // 建立连接
    this.eventSource = new EventSource(`/replay/stream/${this.taskId}`);

    this.eventSource.addEventListener('data', (event) => {
      const stepData = JSON.parse(event.data);
      this.currentStep = stepData.step;
      this.updateProgress();
      this.onData(stepData);
    });
  }

  updateProgress() {
    const progress = (this.currentStep / this.totalSteps) * 100;
    console.log(`进度: ${progress.toFixed(1)}%`);
    // 更新进度条UI
    document.getElementById('progress-bar').style.width = `${progress}%`;
  }

  async getReplayInfo() {
    const response = await fetch(`/replay/info/${this.taskId}`);
    const result = await response.json();
    return result.data;
  }

  onData(stepData) {
    // 更新地图
  }
}
```

### 3. 速度选择器

```html
<select id="speed-selector" onchange="changeSpeed(this.value)">
  <option value="0.5">0.5x (慢放)</option>
  <option value="1.0" selected>1.0x (正常)</option>
  <option value="2.0">2.0x</option>
  <option value="5.0">5.0x</option>
  <option value="10.0">10.0x (最快)</option>
</select>

<script>
function changeSpeed(speed) {
  fetch(`/replay/control/${taskId}/speed?speed=${speed}`, {
    method: 'POST'
  });
}
</script>
```

### 4. 进度条拖动跳转

```html
<input 
  type="range" 
  id="progress-slider" 
  min="0" 
  max="1000" 
  value="0"
  onchange="seekToStep(this.value)"
/>

<script>
function seekToStep(step) {
  fetch(`/replay/control/${taskId}/seek?targetStep=${step}`, {
    method: 'POST'
  });
}
</script>
```

---

## 🔧 配置调整

### 修改回放速度

编辑 `application.yml`:

```yaml
plugin:
  replay:
    sse:
      base-delay-ms: 100  # 修改这个值
```

**效果**:
- `base-delay-ms: 50` → 更快的基础速度
- `base-delay-ms: 200` → 更慢的基础速度

**计算公式**: 实际延迟 = base-delay-ms / speed

---

## ⚠️ 常见问题

### Q1: SSE 连接立即断开？

**原因**: taskId 不存在或没有回放数据

**解决**: 
1. 确认 taskId 正确
2. 检查是否有仿真数据（调用 `/replay/info/{taskId}`）

### Q2: 控制操作不生效？

**原因**: sessionId 错误

**解决**: 确保控制接口使用的 sessionId 与 SSE 连接的 taskId 一致

### Q3: 速度设置失败？

**原因**: 速度超出范围

**解决**: 确保速度在 0.1 ~ 10.0 之间

### Q4: 跳转后没有反应？

**原因**: 目标步数超出范围

**解决**: 确保 targetStep 在 0 ~ totalSteps 之间

### Q5: 浏览器连接数限制？

**原因**: 浏览器对同一域名的 SSE 连接数有限制（通常6个）

**解决**: 
- 关闭不用的连接
- 使用连接池管理
- 考虑使用 WebSocket 替代

---

## 📊 性能优化建议

### 1. 减少数据量

如果回放数据量很大，考虑：
- 降低采样率（每N步推送一次）
- 压缩数据格式
- 只推送必要字段

### 2. 前端优化

```javascript
// 使用 requestAnimationFrame 优化渲染
let pendingUpdate = null;

eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  
  if (!pendingUpdate) {
    pendingUpdate = requestAnimationFrame(() => {
      updateMap(stepData);
      pendingUpdate = null;
    });
  }
});
```

### 3. 批量更新

```javascript
// 累积多个步数据，批量更新
let dataBuffer = [];
const BATCH_SIZE = 10;

eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  dataBuffer.push(stepData);
  
  if (dataBuffer.length >= BATCH_SIZE) {
    updateMapBatch(dataBuffer);
    dataBuffer = [];
  }
});
```

---

## 🎨 UI 组件示例

### 完整播放器 UI

```html
<div class="replay-player">
  <!-- 播放控制 -->
  <div class="controls">
    <button onclick="player.play()">▶️ 播放</button>
    <button onclick="player.pause()">⏸️ 暂停</button>
    <button onclick="player.stop()">⏹️ 停止</button>
  </div>

  <!-- 速度控制 -->
  <div class="speed-control">
    <label>速度:</label>
    <select onchange="player.setSpeed(this.value)">
      <option value="0.5">0.5x</option>
      <option value="1.0" selected>1.0x</option>
      <option value="2.0">2.0x</option>
      <option value="5.0">5.0x</option>
    </select>
  </div>

  <!-- 进度条 -->
  <div class="progress">
    <input 
      type="range" 
      id="progress-slider" 
      min="0" 
      max="1000" 
      value="0"
      onchange="player.seekTo(this.value)"
    />
    <span id="progress-text">0 / 1000</span>
  </div>

  <!-- 地图显示 -->
  <div id="map-container"></div>
</div>

<style>
.replay-player {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
}

.controls button {
  padding: 10px 20px;
  margin-right: 10px;
  font-size: 16px;
}

.progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

#progress-slider {
  flex: 1;
}

#map-container {
  width: 100%;
  height: 600px;
  border: 1px solid #ccc;
}
</style>
```

---

## 📚 相关文档

- **详细 API 文档**: `ReplaySSEController_API_Reference.md`
- **SSE 回放流程**: `sse_replay_flow.md`
- **SSE 回放控制流程**: `sse_replay_control_flow.md`
- **快速参考**: `sse_replay_quick_reference.md`

---

## 🎯 下一步

1. ✅ 阅读完整 API 文档
2. ✅ 查看前端集成示例
3. ✅ 测试回放功能
4. ✅ 根据需求调整配置

---

**文档版本**: 1.0  
**最后更新**: 2026-01-23  
**作者**: AI Assistant
