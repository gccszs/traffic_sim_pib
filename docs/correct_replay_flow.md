# 正确的回放流程设计

## 🎯 核心概念

**回放 = 仿真的历史**

- 回放不是一个独立的"回放任务"，而是直接基于仿真任务的历史数据
- 不需要创建 `replay_task`，直接使用 `simulation_task` 的 `task_id`
- 所有数据都从 `simulation_data` 集合读取

## 📊 数据结构

### MongoDB 集合

#### 1. `map` 集合
```json
{
  "userId": 1,
  "taskId": "task_456",           // ✅ 仿真任务ID（关键字段）
  "mapId": "map_123",
  "mapName": "仿真任务1",
  "addition": {                   // ✅ 地图 JSON 数据
    "demand": [...],
    "cross": [...],
    "link": [...],
    "lane": [...]
  },
  "createdAt": 1737532800000
}
```

#### 2. `simulation_data` 集合
```json
{
  "simulationTaskId": "session_123",
  "userId": "1",
  "taskId": "task_456",           // ✅ 仿真任务ID（关键字段）
  "totalSteps": 1000,
  "startTime": 1737532800000,
  "endTime": 1737536400000,
  "steps": [
    {
      "step": 1,
      "timestamp": 1737532800000,
      "simData": {
        "vehicles": [...]         // ✅ 完整车辆数据
      },
      "statsData": {...}
    }
  ]
}
```

## 🔄 完整流程

### 1. 仿真阶段

```
前端创建仿真
  ↓
POST /api/simulation/start
  Body: {
    simInfo: {
      mapId: "...",
      map_json: { ... }  // 地图 JSON 数据
    }
  }
  ↓
SimulationServiceImpl.startSimulation()
  ├─ 保存到 MySQL (simulation_task 表)
  │   └─ task_id: "task_456"
  └─ 调用 saveMapJsonToMongoDB()
      └─ 保存到 MongoDB (map 集合)
          {
            userId: 1,
            taskId: "task_456",  // ✅ 使用仿真任务ID
            addition: { ... }
          }
  ↓
仿真运行
  └─ 保存到 MongoDB (simulation_data 集合)
      {
        taskId: "task_456",      // ✅ 使用仿真任务ID
        steps: [ ... ]
      }
```

### 2. 回放阶段

#### 步骤 1：获取回放信息

```
GET /api/replay/info/{simulationTaskId}
  ↓
ReplayServiceImpl.getReplayInfo()
  └─ 从 simulation_data 集合查询
      WHERE taskId = {simulationTaskId}
  ↓
返回：
{
  "simulationTaskId": "task_456",
  "totalSteps": 1000,
  "startTime": 1737532800000,
  "endTime": 1737536400000
}
```

#### 步骤 2：获取地图信息

```
GET /api/replay/map/{simulationTaskId}
  ↓
ReplayServiceImpl.getReplayMapInfo()
  └─ 从 map 集合查询
      WHERE taskId = {simulationTaskId}
  ↓
返回：
{
  "taskId": "task_456",
  "addition": {
    "demand": [...],
    "cross": [...],
    "link": [...],
    "lane": [...]
  }
}
```

#### 步骤 3：SSE 回放数据流

```
GET /api/replay/stream/{simulationTaskId}?speed=1.0
  ↓
ReplaySSEController.streamReplayData()
  └─ 从 simulation_data 集合读取所有步数据
  ↓
逐步推送：
  event: start
  data: {totalSteps: 1000}
  
  event: data
  data: {step: 1, simData: {...}, statsData: {...}}
  
  event: data
  data: {step: 2, simData: {...}, statsData: {...}}
  
  ...
  
  event: end
  data: {message: "回放完成"}
```

## 🎨 前端实现

### 完整回放流程

```javascript
// 1. 获取仿真任务列表
async function loadSimulationList() {
  const response = await fetch('/api/simulation/list?page=1&size=10');
  const result = await response.json();
  
  // result.data.records 包含所有仿真任务
  // 每个任务都有 taskId，可以直接用于回放
  return result.data.records;
}

// 2. 选择一个仿真任务进行回放
async function startReplay(simulationTaskId) {
  // 2.1 获取回放信息
  const infoResponse = await fetch(`/api/replay/info/${simulationTaskId}`);
  const infoResult = await infoResponse.json();
  const replayInfo = infoResult.data;
  
  console.log('总步数:', replayInfo.totalSteps);
  
  // 2.2 获取地图信息
  const mapResponse = await fetch(`/api/replay/map/${simulationTaskId}`);
  const mapResult = await mapResponse.json();
  const mapData = mapResult.data;
  
  // 2.3 渲染地图
  renderMap(mapData.addition);
  
  // 2.4 建立 SSE 连接
  const eventSource = new EventSource(
    `/api/replay/stream/${simulationTaskId}?speed=1.0`
  );
  
  // 监听开始事件
  eventSource.addEventListener('start', (event) => {
    const data = JSON.parse(event.data);
    console.log('回放开始，总步数:', data.totalSteps);
    initProgressBar(data.totalSteps);
  });
  
  // 监听数据事件
  eventSource.addEventListener('data', (event) => {
    const stepData = JSON.parse(event.data);
    
    // 渲染车辆
    renderVehicles(stepData.simData.vehicles);
    
    // 更新统计
    updateStatistics(stepData.statistics);
    
    // 更新进度
    updateProgress(stepData.step);
  });
  
  // 监听结束事件
  eventSource.addEventListener('end', (event) => {
    console.log('回放完成');
    eventSource.close();
  });
  
  // 错误处理
  eventSource.onerror = (error) => {
    console.error('SSE 错误:', error);
    eventSource.close();
  };
}

function renderMap(mapJson) {
  // 渲染道路
  mapJson.link.forEach(link => {
    drawRoad(link);
  });
  
  // 渲染交叉口
  mapJson.cross.forEach(cross => {
    drawIntersection(cross);
  });
  
  // 渲染车道
  mapJson.lane.forEach(lane => {
    drawLane(lane);
  });
}

function renderVehicles(vehicles) {
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
}
```

## 📡 API 接口

### 新的回放接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/replay/info/{simulationTaskId}` | GET | 获取回放信息（总步数等） |
| `/api/replay/map/{simulationTaskId}` | GET | 获取地图 JSON 数据 |
| `/api/replay/stream/{simulationTaskId}?speed=1.0` | GET (SSE) | SSE 回放数据流 |

### 参数说明

- `simulationTaskId`: 仿真任务ID（`simulation_task` 表的 `task_id`）
- `speed`: 播放速度倍数（0.5 = 慢放，1.0 = 正常，2.0 = 快进）

## ✅ 关键优势

1. **简化流程**：不需要创建回放任务，直接使用仿真任务ID
2. **无需权限验证**：任何人都可以查看仿真历史（如果需要权限，可以在 Controller 层添加）
3. **数据一致性**：地图数据和仿真数据通过 `taskId` 关联
4. **性能优化**：直接从 MongoDB 查询，无需跨表关联

## 🔧 关键修改总结

### 1. SimulationServiceImpl.java
- ✅ 添加 `saveMapJsonToMongoDB()` 方法
- ✅ 保存地图 JSON 到 MongoDB，使用 `taskId` 作为关键字段

### 2. ReplayController.java
- ✅ 新增 `GET /replay/map/{simulationTaskId}` 接口
- ✅ 新增 `GET /replay/info/{simulationTaskId}` 接口
- ✅ 移除用户权限验证

### 3. ReplayService.java & ReplayServiceImpl.java
- ✅ 新增 `getReplayMapInfo(String simulationTaskId)` 方法
- ✅ 新增 `getReplayInfo(String simulationTaskId)` 方法
- ✅ 直接从 MongoDB 查询，使用 `taskId` 作为条件

### 4. ReplaySSEController.java
- ✅ 修改为 `GET /replay/stream/{simulationTaskId}`
- ✅ 移除回放任务验证，直接使用仿真任务ID

### 5. ReplayDataService.java
- ✅ 修改 `getReplayData()` 方法，从 `simulation_data` 集合读取

## 🎯 数据流图

```
┌─────────────────────────────────────────────────────────────┐
│                   仿真阶段                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ MySQL            │
                    │ simulation_task  │
                    │ task_id: "456"   │
                    └──────────────────┘
                              │
                ┌─────────────┴─────────────┐
                ▼                           ▼
      ┌──────────────────┐      ┌──────────────────┐
      │ MongoDB: map     │      │ MongoDB:         │
      │ taskId: "456"    │      │ simulation_data  │
      │ addition: {...}  │      │ taskId: "456"    │
      └──────────────────┘      │ steps: [...]     │
                                └──────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   回放阶段                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
              GET /replay/map/{taskId}
                              │
                              ▼
                    查询 MongoDB map 集合
                    WHERE taskId = "456"
                              │
                              ▼
                    返回 addition 字段
                              │
                              ▼
                      前端渲染地图
                              │
                              ▼
              GET /replay/stream/{taskId}
                              │
                              ▼
                查询 MongoDB simulation_data
                    WHERE taskId = "456"
                              │
                              ▼
                    SSE 推送车辆数据
                              │
                              ▼
                      前端逐步渲染
```

现在回放流程已经完全正确了！🎉

