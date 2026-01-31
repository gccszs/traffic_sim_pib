# 回放历史记录功能说明

**日期**: 2026-01-23  
**功能**: 回放完成后自动生成历史记录 + 修复控制流程  
**状态**: ✅ 已实现

---

## 📋 功能概述

### 新增功能

1. **自动记录回放历史**：每次建立 SSE 回放连接时，自动在 MySQL 的 `replay_task` 表中创建记录
2. **实时更新回放状态**：回放过程中实时更新状态、当前步数、播放速度等信息
3. **回放历史管理**：提供完整的 CRUD 接口管理回放历史记录
4. **修复控制流程**：SSE 连接建立后等待前端播放指令，而不是立即开始推送数据

---

## 🔧 核心修改

### 1. 修复初始状态问题 ⭐ 重要

**问题**：SSE 连接建立后立即开始推送数据，无法控制

**原因**：`SseReplayControlService.createSession()` 初始状态为 `PLAYING`

**修复**：

```java
// 修改前 ❌
state.setStatus(ReplayStatus.PLAYING);  // 立即开始播放

// 修改后 ✅
state.setStatus(ReplayStatus.PAUSED);   // 等待前端播放指令
```

**效果**：
- ✅ SSE 连接建立后处于暂停状态
- ✅ 发送 `ready` 事件通知前端已就绪
- ✅ 等待前端调用 `/replay/control/{taskId}/play` 开始播放
- ✅ 所有控制接口（暂停、倍速、跳转）正常工作

---

## 📊 数据库表结构

### replay_task 表

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| task_id | VARCHAR(64) | 回放任务ID（主键） | `a1b2c3d4...` |
| simulation_task_id | VARCHAR(64) | 关联的仿真任务ID | `8f58a814...` |
| name | VARCHAR(255) | 回放任务名称 | `回放-8f58a814` |
| status | VARCHAR(20) | 状态 | `CREATED/PLAYING/PAUSED/STOPPED/FINISHED` |
| current_step | BIGINT | 当前步数 | `250` |
| total_steps | BIGINT | 总步数 | `1000` |
| playback_speed | DOUBLE | 播放速度（倍速） | `2.0` |
| user_id | BIGINT | 用户ID | `1` |
| create_time | DATETIME | 创建时间 | `2026-01-23 16:00:00` |
| update_time | DATETIME | 更新时间 | `2026-01-23 16:05:00` |

---

## 🎯 回放流程（修复后）

### 完整流程图

```
前端                          后端                          数据库
 |                             |                             |
 |--1. 建立 SSE 连接----------->|                             |
 |   GET /replay/stream/{id}   |                             |
 |                             |--创建回放会话（PAUSED）----->|
 |                             |--创建 replay_task 记录------>|
 |<----2. start 事件-----------|                             |
 |     {totalSteps: 1000}      |                             |
 |<----3. ready 事件-----------|                             |
 |     {status: "PAUSED"}      |                             |
 |                             |                             |
 |   ⏸️ 等待播放指令...         |   ⏸️ 暂停状态，不推送数据    |
 |                             |                             |
 |--4. 调用播放接口------------>|                             |
 |   POST /control/{id}/play   |                             |
 |                             |--更新状态为 PLAYING--------->|
 |<----5. data 事件（开始推送）-|                             |
 |     {step: 0, ...}          |                             |
 |<----6. data 事件------------|                             |
 |     {step: 1, ...}          |                             |
 |                             |--实时更新 current_step------>|
 |                             |                             |
 |--7. 暂停------------------>|                             |
 |   POST /control/{id}/pause  |                             |
 |                             |--更新状态为 PAUSED---------->|
 |   ⏸️ 停止接收数据            |   ⏸️ 停止推送数据            |
 |                             |                             |
 |--8. 2倍速----------------->|                             |
 |   POST /control/{id}/speed  |                             |
 |   ?speed=2.0                |                             |
 |                             |--更新 playback_speed-------->|
 |                             |                             |
 |--9. 继续播放--------------->|                             |
 |   POST /control/{id}/play   |                             |
 |<----data 事件（2倍速）-------|                             |
 |                             |                             |
 |<----10. end 事件------------|                             |
 |     {message: "回放完成"}    |                             |
 |                             |--更新状态为 FINISHED-------->|
```

---

## 📡 SSE 事件类型（新增 ready 事件）

| 事件名 | 触发时机 | 数据格式 | 说明 |
|--------|----------|----------|------|
| `start` | 连接建立，数据加载完成 | `{"totalSteps": 1000}` | 告知总步数 |
| **`ready`** ⭐ | 数据准备完成 | `{"message": "...", "status": "PAUSED"}` | **等待播放指令** |
| `data` | 每一步数据 | `{step: 100, vehicles: [...]}` | 仿真数据 |
| `seeked` | 跳转完成 | `{"currentStep": 500}` | 跳转结果 |
| `stopped` | 用户停止 | `{"currentStep": 300}` | 停止位置 |
| `end` | 回放完成 | `{"message": "回放完成"}` | 正常结束 |
| `error` | 发生错误 | `"没有找到回放数据"` | 错误信息 |

---

## 🆕 新增 API 接口

### 1. 获取回放历史记录列表

**接口**: `GET /replay/history/list?page=0&size=10`

**描述**: 分页查询当前用户的回放历史记录

**查询参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 0 | 页码（从0开始） |
| size | int | 否 | 10 | 每页数量 |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "success",
  "data": {
    "content": [
      {
        "taskId": "a1b2c3d4e5f6...",
        "simulationTaskId": "8f58a814e84c4bc6...",
        "name": "回放-8f58a814",
        "status": "FINISHED",
        "currentStep": 1000,
        "totalSteps": 1000,
        "playbackSpeed": 2.0,
        "userId": 1,
        "createTime": "2026-01-23T16:00:00",
        "updateTime": "2026-01-23T16:05:00"
      }
    ],
    "totalElements": 50,
    "totalPages": 5,
    "number": 0,
    "size": 10
  },
  "timestamp": 1737619200000
}
```

---

### 2. 获取指定仿真任务的回放历史

**接口**: `GET /replay/history/simulation/{simulationTaskId}`

**描述**: 查询某个仿真任务的所有回放记录

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| simulationTaskId | String | 是 | 仿真任务ID |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "success",
  "data": [
    {
      "taskId": "replay-1",
      "simulationTaskId": "8f58a814e84c4bc6...",
      "name": "回放-8f58a814",
      "status": "FINISHED",
      "currentStep": 1000,
      "totalSteps": 1000,
      "playbackSpeed": 1.0,
      "userId": 1,
      "createTime": "2026-01-23T16:00:00",
      "updateTime": "2026-01-23T16:05:00"
    },
    {
      "taskId": "replay-2",
      "simulationTaskId": "8f58a814e84c4bc6...",
      "name": "回放-8f58a814",
      "status": "STOPPED",
      "currentStep": 500,
      "totalSteps": 1000,
      "playbackSpeed": 2.0,
      "userId": 1,
      "createTime": "2026-01-23T17:00:00",
      "updateTime": "2026-01-23T17:02:30"
    }
  ],
  "timestamp": 1737619200000
}
```

---

### 3. 获取回放历史详情

**接口**: `GET /replay/history/{replayTaskId}`

**描述**: 根据回放任务ID获取详细信息

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| replayTaskId | String | 是 | 回放任务ID |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "success",
  "data": {
    "taskId": "a1b2c3d4e5f6...",
    "simulationTaskId": "8f58a814e84c4bc6...",
    "name": "回放-8f58a814",
    "status": "FINISHED",
    "currentStep": 1000,
    "totalSteps": 1000,
    "playbackSpeed": 2.0,
    "userId": 1,
    "createTime": "2026-01-23T16:00:00",
    "updateTime": "2026-01-23T16:05:00"
  },
  "timestamp": 1737619200000
}
```

---

### 4. 删除回放历史记录

**接口**: `DELETE /replay/history/{replayTaskId}`

**描述**: 删除指定的回放历史记录（需要权限验证）

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| replayTaskId | String | 是 | 回放任务ID |

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "删除成功",
  "data": null,
  "timestamp": 1737619200000
}
```

---

### 5. 统计用户回放次数

**接口**: `GET /replay/history/stats`

**描述**: 获取当前用户的回放统计信息

**响应**:
```json
{
  "res": "ERR_OK",
  "msg": "success",
  "data": {
    "totalReplayCount": 50,
    "userId": 1
  },
  "timestamp": 1737619200000
}
```

---

## 💻 前端集成示例

### 完整回放控制示例（修复后）

```javascript
class ReplayPlayer {
  constructor(taskId) {
    this.taskId = taskId;
    this.eventSource = null;
    this.isReady = false;
  }

  // 建立连接
  async connect() {
    return new Promise((resolve, reject) => {
      this.eventSource = new EventSource(`/replay/stream/${this.taskId}`);

      // 监听 start 事件
      this.eventSource.addEventListener('start', (event) => {
        const data = JSON.parse(event.data);
        console.log('✅ 回放初始化，总步数:', data.totalSteps);
        this.totalSteps = data.totalSteps;
      });

      // 监听 ready 事件（新增）⭐
      this.eventSource.addEventListener('ready', (event) => {
        const data = JSON.parse(event.data);
        console.log('✅ 回放已就绪:', data.message);
        console.log('⏸️ 当前状态:', data.status);
        this.isReady = true;
        resolve();  // 连接就绪
      });

      // 监听数据事件
      this.eventSource.addEventListener('data', (event) => {
        const stepData = JSON.parse(event.data);
        this.onData(stepData);
      });

      // 监听结束事件
      this.eventSource.addEventListener('end', (event) => {
        console.log('✅ 回放完成');
        this.onEnd();
      });

      // 错误处理
      this.eventSource.onerror = (error) => {
        console.error('❌ SSE 连接错误:', error);
        reject(error);
      };

      // 超时处理
      setTimeout(() => {
        if (!this.isReady) {
          reject(new Error('连接超时'));
        }
      }, 10000);
    });
  }

  // 开始播放（必须先调用此方法）⭐
  async play() {
    if (!this.isReady) {
      console.warn('⚠️ 回放尚未就绪，请先建立连接');
      return;
    }
    
    const response = await fetch(`/replay/control/${this.taskId}/play`, {
      method: 'POST'
    });
    const result = await response.json();
    console.log('▶️ 开始播放:', result.msg);
  }

  // 暂停
  async pause() {
    const response = await fetch(`/replay/control/${this.taskId}/pause`, {
      method: 'POST'
    });
    const result = await response.json();
    console.log('⏸️ 暂停:', result.msg);
  }

  // 设置速度
  async setSpeed(speed) {
    const response = await fetch(`/replay/control/${this.taskId}/speed?speed=${speed}`, {
      method: 'POST'
    });
    const result = await response.json();
    console.log(`⚡ 设置速度为 ${speed}x:`, result.msg);
  }

  // 跳转
  async seekTo(targetStep) {
    const response = await fetch(`/replay/control/${this.taskId}/seek?targetStep=${targetStep}`, {
      method: 'POST'
    });
    const result = await response.json();
    console.log(`⏩ 跳转到步数 ${targetStep}:`, result.msg);
  }

  // 停止
  async stop() {
    const response = await fetch(`/replay/control/${this.taskId}/stop`, {
      method: 'POST'
    });
    this.eventSource?.close();
    console.log('⏹️ 停止回放');
  }

  // 数据回调
  onData(stepData) {
    console.log('📊 步数:', stepData.step);
    // 更新地图显示
  }

  // 结束回调
  onEnd() {
    this.eventSource?.close();
  }
}

// 使用示例（修复后的正确流程）⭐
async function startReplay(taskId) {
  const player = new ReplayPlayer(taskId);

  try {
    // 1. 建立连接（等待 ready 事件）
    console.log('1️⃣ 建立 SSE 连接...');
    await player.connect();
    console.log('✅ 连接已就绪，等待播放指令');

    // 2. 用户点击播放按钮后，调用 play 方法
    console.log('2️⃣ 开始播放...');
    await player.play();

    // 3. 控制回放
    setTimeout(() => player.pause(), 5000);      // 5秒后暂停
    setTimeout(() => player.setSpeed(2.0), 7000); // 7秒后2倍速
    setTimeout(() => player.play(), 8000);        // 8秒后继续
    setTimeout(() => player.seekTo(500), 10000);  // 10秒后跳转

  } catch (error) {
    console.error('❌ 回放失败:', error);
  }
}
```

---

## 🎨 前端 UI 示例

### 回放控制面板

```html
<div class="replay-player">
  <!-- 状态显示 -->
  <div class="status">
    <span id="status-text">未连接</span>
    <span id="progress-text">0 / 0</span>
  </div>

  <!-- 播放控制 -->
  <div class="controls">
    <button id="btn-connect" onclick="connectReplay()">🔗 连接</button>
    <button id="btn-play" onclick="playReplay()" disabled>▶️ 播放</button>
    <button id="btn-pause" onclick="pauseReplay()" disabled>⏸️ 暂停</button>
    <button id="btn-stop" onclick="stopReplay()" disabled>⏹️ 停止</button>
  </div>

  <!-- 速度控制 -->
  <div class="speed-control">
    <label>速度:</label>
    <select id="speed-selector" onchange="changeSpeed(this.value)" disabled>
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
      onchange="seekToStep(this.value)"
      disabled
    />
  </div>
</div>

<script>
let player = null;

async function connectReplay() {
  const taskId = 'your-task-id';
  player = new ReplayPlayer(taskId);
  
  try {
    document.getElementById('status-text').textContent = '连接中...';
    await player.connect();
    
    // 连接成功，启用控制按钮
    document.getElementById('status-text').textContent = '已就绪 ⏸️';
    document.getElementById('btn-play').disabled = false;
    document.getElementById('btn-stop').disabled = false;
    document.getElementById('speed-selector').disabled = false;
    document.getElementById('progress-slider').disabled = false;
    document.getElementById('btn-connect').disabled = true;
    
  } catch (error) {
    document.getElementById('status-text').textContent = '连接失败 ❌';
    alert('连接失败: ' + error.message);
  }
}

async function playReplay() {
  await player.play();
  document.getElementById('status-text').textContent = '播放中 ▶️';
  document.getElementById('btn-play').disabled = true;
  document.getElementById('btn-pause').disabled = false;
}

async function pauseReplay() {
  await player.pause();
  document.getElementById('status-text').textContent = '已暂停 ⏸️';
  document.getElementById('btn-play').disabled = false;
  document.getElementById('btn-pause').disabled = true;
}

async function stopReplay() {
  await player.stop();
  document.getElementById('status-text').textContent = '已停止 ⏹️';
  // 禁用所有控制按钮
  document.getElementById('btn-play').disabled = true;
  document.getElementById('btn-pause').disabled = true;
  document.getElementById('btn-stop').disabled = true;
  document.getElementById('speed-selector').disabled = true;
  document.getElementById('progress-slider').disabled = true;
  document.getElementById('btn-connect').disabled = false;
}

async function changeSpeed(speed) {
  await player.setSpeed(parseFloat(speed));
}

async function seekToStep(step) {
  await player.seekTo(parseInt(step));
}
</script>
```

---

## 📊 回放历史管理界面示例

```javascript
// 获取回放历史列表
async function loadReplayHistory() {
  const response = await fetch('/replay/history/list?page=0&size=10');
  const result = await response.json();
  
  if (result.res === 'ERR_OK') {
    const replayList = result.data.content;
    displayReplayHistory(replayList);
  }
}

// 显示回放历史
function displayReplayHistory(replayList) {
  const tbody = document.getElementById('replay-history-tbody');
  tbody.innerHTML = '';
  
  replayList.forEach(replay => {
    const row = `
      <tr>
        <td>${replay.name}</td>
        <td>${replay.status}</td>
        <td>${replay.currentStep} / ${replay.totalSteps}</td>
        <td>${replay.playbackSpeed}x</td>
        <td>${replay.createTime}</td>
        <td>
          <button onclick="viewReplay('${replay.simulationTaskId}')">查看</button>
          <button onclick="deleteReplay('${replay.taskId}')">删除</button>
        </td>
      </tr>
    `;
    tbody.innerHTML += row;
  });
}

// 删除回放历史
async function deleteReplay(replayTaskId) {
  if (!confirm('确定要删除这条回放记录吗？')) {
    return;
  }
  
  const response = await fetch(`/replay/history/${replayTaskId}`, {
    method: 'DELETE'
  });
  const result = await response.json();
  
  if (result.res === 'ERR_OK') {
    alert('删除成功');
    loadReplayHistory();  // 刷新列表
  } else {
    alert('删除失败: ' + result.msg);
  }
}
```

---

## ✅ 测试验证

### 测试步骤

1. **测试初始暂停状态**
   ```bash
   # 建立 SSE 连接
   curl -N http://localhost:3822/replay/stream/8f58a814e84c4bc6a42b22098bb2fa48
   
   # 应该收到 start 和 ready 事件，但不会立即推送 data 事件
   ```

2. **测试播放控制**
   ```bash
   # 调用播放接口
   curl -X POST http://localhost:3822/replay/control/8f58a814e84c4bc6a42b22098bb2fa48/play
   
   # 现在应该开始接收 data 事件
   ```

3. **测试暂停控制**
   ```bash
   # 调用暂停接口
   curl -X POST http://localhost:3822/replay/control/8f58a814e84c4bc6a42b22098bb2fa48/pause
   
   # data 事件应该停止推送
   ```

4. **测试回放历史记录**
   ```bash
   # 查询回放历史
   curl http://localhost:3822/replay/history/list?page=0&size=10
   
   # 应该看到刚才的回放记录
   ```

5. **验证数据库记录**
   ```sql
   SELECT * FROM replay_task ORDER BY create_time DESC LIMIT 10;
   ```

---

## 🎯 关键改进总结

### 修复前 ❌

```
1. SSE 连接建立
2. 立即开始推送数据（无法控制）
3. 控制接口无效
```

### 修复后 ✅

```
1. SSE 连接建立
2. 发送 ready 事件，等待播放指令
3. 前端调用 play 接口
4. 开始推送数据
5. 所有控制接口正常工作
6. 自动记录回放历史
```

---

## 📝 注意事项

1. ✅ **必须先建立 SSE 连接，等待 ready 事件**
2. ✅ **收到 ready 事件后，调用 play 接口开始播放**
3. ✅ **所有控制操作（暂停、倍速、跳转）立即生效**
4. ✅ **回放历史自动记录，无需手动创建**
5. ✅ **回放状态实时更新到数据库**
6. ⚠️ **删除回放历史需要权限验证（只能删除自己的记录）**

---

## 📚 相关文档

- **回放 API 文档**: `ReplaySSEController_API_Reference.md`
- **回放快速指南**: `ReplaySSEController_Quick_Guide.md`
- **类型转换修复**: `replay_classcast_fix.md`

---

**功能完成日期**: 2026-01-23  
**作者**: AI Assistant  
**版本**: 2.0
