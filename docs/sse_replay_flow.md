# SSE 回放完整流程说明

## 📡 SSE 连接接口

### 接口地址
```
GET /replay/{taskId}/stream?speed={speed}
```

### 参数说明
- `taskId`: 回放任务ID（路径参数）
- `speed`: 播放速度倍数（查询参数，默认 1.0）
  - 0.5 = 慢放（0.5倍速）
  - 1.0 = 正常速度
  - 2.0 = 快进（2倍速）

### 响应类型
```
Content-Type: text/event-stream
```

## 🔄 完整回放流程

### 1. 前端创建回放任务

```javascript
// POST /api/replay/create
const response = await fetch('/api/replay/create', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  },
  body: JSON.stringify({
    simulationTaskId: 'simulation_task_123',  // 仿真任务ID
    name: '回放任务1'
  })
});

const replayTask = await response.json();
console.log('回放任务ID:', replayTask.data.taskId);
```

### 2. 前端建立 SSE 连接

```javascript
// 建立 SSE 连接
const taskId = replayTask.data.taskId;
const speed = 1.0;  // 播放速度

const eventSource = new EventSource(
  `/replay/${taskId}/stream?speed=${speed}`,
  {
    withCredentials: true  // 携带认证信息
  }
);

// 监听开始事件
eventSource.addEventListener('start', (event) => {
  const data = JSON.parse(event.data);
  console.log('回放开始，总步数:', data.totalSteps);
  
  // 初始化进度条
  initProgressBar(data.totalSteps);
});

// 监听数据事件（每一步的数据）
eventSource.addEventListener('data', (event) => {
  const stepData = JSON.parse(event.data);
  console.log('收到步数:', stepData.step);
  
  // 渲染车辆和信号灯
  renderSimulationStep(stepData);
  
  // 更新进度
  updateProgress(stepData.step);
});

// 监听结束事件
eventSource.addEventListener('end', (event) => {
  const data = JSON.parse(event.data);
  console.log('回放完成:', data.message);
  
  // 关闭连接
  eventSource.close();
  
  // 显示完成提示
  showCompletionMessage();
});

// 监听错误事件
eventSource.addEventListener('error', (event) => {
  console.error('SSE 连接错误:', event);
  eventSource.close();
});

// 监听通用错误消息
eventSource.addEventListener('error', (event) => {
  if (event.data) {
    const errorData = JSON.parse(event.data);
    console.error('回放错误:', errorData);
  }
});
```

### 3. 渲染仿真步数据

```javascript
function renderSimulationStep(stepData) {
  // stepData 结构：
  // {
  //   step: 100,
  //   timestamp: 1737532800000,
  //   simData: {
  //     vehicles: [...],
  //     signals: [...]
  //   },
  //   statistics: {
  //     speed_ave: 15.5,
  //     car_number: 50,
  //     ...
  //   }
  // }
  
  // 1. 渲染车辆
  const vehicles = stepData.simData.vehicles || [];
  vehicles.forEach(vehicle => {
    renderVehicle({
      id: vehicle.id,
      x: vehicle.x,
      y: vehicle.y,
      speed: vehicle.speed,
      acceleration: vehicle.acceleration,
      roadId: vehicle.roadId,
      laneId: vehicle.laneId
    });
  });
  
  // 2. 渲染信号灯
  const signals = stepData.simData.signals || [];
  signals.forEach(signal => {
    renderSignal(signal);
  });
  
  // 3. 更新统计信息
  updateStatistics(stepData.statistics);
}

function renderVehicle(vehicle) {
  // 在地图上绘制车辆
  const vehicleElement = document.getElementById(`vehicle-${vehicle.id}`);
  if (vehicleElement) {
    // 更新现有车辆位置
    vehicleElement.style.left = vehicle.x + 'px';
    vehicleElement.style.top = vehicle.y + 'px';
  } else {
    // 创建新车辆
    const newVehicle = document.createElement('div');
    newVehicle.id = `vehicle-${vehicle.id}`;
    newVehicle.className = 'vehicle';
    newVehicle.style.left = vehicle.x + 'px';
    newVehicle.style.top = vehicle.y + 'px';
    document.getElementById('map-canvas').appendChild(newVehicle);
  }
}

function updateStatistics(stats) {
  document.getElementById('avg-speed').textContent = stats.speed_ave?.toFixed(2) || '0.00';
  document.getElementById('car-count').textContent = stats.car_number || 0;
  document.getElementById('jam-index').textContent = stats.jam_index?.toFixed(2) || '0.00';
}
```

### 4. 控制回放（可选）

如果需要暂停、继续、跳转等控制，需要关闭当前 SSE 连接，调用控制接口，然后重新建立连接：

```javascript
// 暂停回放
async function pauseReplay(taskId) {
  // 关闭 SSE 连接
  eventSource.close();
  
  // 调用暂停接口
  await fetch(`/api/replay/${taskId}/control`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify({
      action: 'PAUSE'
    })
  });
}

// 继续回放
async function resumeReplay(taskId, currentStep) {
  // 调用继续接口
  await fetch(`/api/replay/${taskId}/control`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify({
      action: 'PLAY'
    })
  });
  
  // 重新建立 SSE 连接（从当前步开始）
  // 注意：当前实现会从头开始，需要改进支持断点续传
  eventSource = new EventSource(`/replay/${taskId}/stream?speed=1.0`);
  // ... 重新绑定事件监听器
}

// 调整播放速度
async function changeSpeed(taskId, newSpeed) {
  // 关闭当前连接
  eventSource.close();
  
  // 调用设置速度接口
  await fetch(`/api/replay/${taskId}/control`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify({
      action: 'SET_SPEED',
      speed: newSpeed
    })
  });
  
  // 重新建立连接（使用新速度）
  eventSource = new EventSource(`/replay/${taskId}/stream?speed=${newSpeed}`);
  // ... 重新绑定事件监听器
}
```

## 🔧 后端处理流程

### 1. SSE 控制器接收连接

```java
// ReplaySSEController.java
@GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamReplayData(
        @PathVariable String taskId,
        @RequestParam(defaultValue = "1.0") double speed) {
    
    // 1. 验证用户权限
    // 2. 查询回放任务
    // 3. 创建 SseEmitter
    // 4. 异步推送数据
    
    return emitter;
}
```

### 2. 从 MongoDB 加载数据

```java
// ReplayDataService.java
public List<ReplayDataDTO> getReplayData(String simulationTaskId, Long startStep, Long endStep) {
    // 1. 从 simulation_data 集合查询
    Query query = Query.query(Criteria.where("simulationTaskId").is(simulationTaskId));
    Map<String, Object> simulationData = mongoTemplate.findOne(query, Map.class, "simulation_data");
    
    // 2. 提取 steps 数组
    List<Map<String, Object>> steps = (List<Map<String, Object>>) simulationData.get("steps");
    
    // 3. 转换为 DTO
    return steps.stream()
            .map(this::convertStepDataToDTO)
            .collect(Collectors.toList());
}
```

### 3. 逐步推送数据

```java
// ReplaySSEController.java
private void pushReplayData(SseEmitter emitter, String simulationTaskId, double speed) {
    // 1. 加载所有步数据
    List<ReplayDataDTO> replayDataList = replayDataService.getReplayData(
        simulationTaskId, 0L, Long.MAX_VALUE);
    
    // 2. 发送开始事件
    emitter.send(SseEmitter.event()
            .name("start")
            .data("{\"totalSteps\": " + replayDataList.size() + "}"));
    
    // 3. 逐步推送
    long delayMs = (long) (1000 / speed);  // 根据速度计算延迟
    
    for (ReplayDataDTO stepData : replayDataList) {
        // 发送数据
        emitter.send(SseEmitter.event()
                .name("data")
                .data(stepData));
        
        // 延迟
        Thread.sleep(delayMs);
    }
    
    // 4. 发送结束事件
    emitter.send(SseEmitter.event()
            .name("end")
            .data("{\"message\": \"回放完成\"}"));
    
    emitter.complete();
}
```

## 📊 数据流图

```
┌─────────────┐
│   前端      │
│  (Browser)  │
└──────┬──────┘
       │
       │ 1. POST /api/replay/create
       │    创建回放任务
       ▼
┌─────────────────────┐
│ ReplayController    │
│ createReplayTask()  │
└──────┬──────────────┘
       │
       │ 2. 查询 simulation_data
       │    统计步数
       ▼
┌─────────────────────┐
│   MongoDB           │
│ simulation_data 集合 │
└──────┬──────────────┘
       │
       │ 3. 返回 taskId
       ▼
┌─────────────┐
│   前端      │
│ 获得 taskId │
└──────┬──────┘
       │
       │ 4. GET /replay/{taskId}/stream?speed=1.0
       │    建立 SSE 连接
       ▼
┌─────────────────────┐
│ ReplaySSEController │
│ streamReplayData()  │
└──────┬──────────────┘
       │
       │ 5. 异步推送数据
       │
       ├─► event: start
       │   data: {totalSteps: 1000}
       │
       ├─► event: data (step 1)
       │   data: {step: 1, simData: {...}, statistics: {...}}
       │
       ├─► event: data (step 2)
       │   data: {step: 2, simData: {...}, statistics: {...}}
       │
       │   ... (每步延迟 1000/speed ms)
       │
       ├─► event: data (step 1000)
       │   data: {step: 1000, simData: {...}, statistics: {...}}
       │
       └─► event: end
           data: {message: "回放完成"}
```

## 🎯 关键点总结

1. **SSE 接口**：`GET /replay/{taskId}/stream?speed={speed}`
2. **数据来源**：`simulation_data` 集合（仿真时保存的完整车辆数据）
3. **推送频率**：根据 `speed` 参数动态调整（1000/speed 毫秒/步）
4. **事件类型**：
   - `start`：回放开始
   - `data`：每一步的数据
   - `end`：回放结束
   - `error`：错误信息

5. **前端渲染**：接收到每一步数据后，实时更新地图上的车辆和信号灯

## 🚀 使用示例

完整的前端代码示例：

```html
<!DOCTYPE html>
<html>
<head>
  <title>交通仿真回放</title>
  <style>
    #map-canvas {
      position: relative;
      width: 800px;
      height: 600px;
      border: 1px solid #ccc;
      background: #f0f0f0;
    }
    .vehicle {
      position: absolute;
      width: 10px;
      height: 10px;
      background: red;
      border-radius: 50%;
    }
  </style>
</head>
<body>
  <div id="controls">
    <button onclick="startReplay()">开始回放</button>
    <button onclick="stopReplay()">停止回放</button>
    <select id="speed-select">
      <option value="0.5">0.5x</option>
      <option value="1.0" selected>1.0x</option>
      <option value="2.0">2.0x</option>
    </select>
  </div>
  
  <div id="stats">
    <p>当前步数: <span id="current-step">0</span></p>
    <p>总步数: <span id="total-steps">0</span></p>
    <p>平均速度: <span id="avg-speed">0</span> m/s</p>
    <p>车辆数量: <span id="car-count">0</span></p>
  </div>
  
  <div id="map-canvas"></div>
  
  <script>
    let eventSource = null;
    const taskId = 'your_replay_task_id';  // 从创建回放任务接口获取
    
    function startReplay() {
      const speed = document.getElementById('speed-select').value;
      
      eventSource = new EventSource(`/replay/${taskId}/stream?speed=${speed}`);
      
      eventSource.addEventListener('start', (event) => {
        const data = JSON.parse(event.data);
        document.getElementById('total-steps').textContent = data.totalSteps;
      });
      
      eventSource.addEventListener('data', (event) => {
        const stepData = JSON.parse(event.data);
        document.getElementById('current-step').textContent = stepData.step;
        
        // 渲染车辆
        const vehicles = stepData.simData.vehicles || [];
        vehicles.forEach(vehicle => {
          let vehicleEl = document.getElementById(`vehicle-${vehicle.id}`);
          if (!vehicleEl) {
            vehicleEl = document.createElement('div');
            vehicleEl.id = `vehicle-${vehicle.id}`;
            vehicleEl.className = 'vehicle';
            document.getElementById('map-canvas').appendChild(vehicleEl);
          }
          vehicleEl.style.left = vehicle.x + 'px';
          vehicleEl.style.top = vehicle.y + 'px';
        });
        
        // 更新统计
        if (stepData.statistics) {
          document.getElementById('avg-speed').textContent = 
            (stepData.statistics.speed_ave || 0).toFixed(2);
          document.getElementById('car-count').textContent = 
            stepData.statistics.car_number || 0;
        }
      });
      
      eventSource.addEventListener('end', (event) => {
        console.log('回放完成');
        eventSource.close();
      });
      
      eventSource.onerror = (error) => {
        console.error('SSE 错误:', error);
        eventSource.close();
      };
    }
    
    function stopReplay() {
      if (eventSource) {
        eventSource.close();
      }
    }
  </script>
</body>
</html>
```

这就是完整的 SSE 回放流程！🎉

