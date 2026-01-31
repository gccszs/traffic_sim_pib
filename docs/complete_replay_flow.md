# 完整的回放流程实现方案

## 📋 概述

本方案实现了完整的仿真回放功能，包括：
1. 上传地图时保存地图 JSON 到 MongoDB
2. 仿真时保存完整的车辆行为数据
3. 回放时获取地图信息和仿真数据
4. 通过 SSE 推送回放数据到前端

## 🗂️ 数据存储结构

### 1. MySQL 数据库

#### `map` 表
```sql
- id: 主键
- map_id: 地图UUID（用于关联）
- name: 地图名称
- map_image: 地图图片（Base64，LONGTEXT）
- xml_file_path: XML文件路径
- owner_id: 所有者ID
- ...
```

#### `simulation_task` 表
```sql
- task_id: 仿真任务ID
- name: 仿真名称
- map_id: 关联的地图ID
- map_name: 地图名称
- sim_config: 仿真配置（LONGTEXT，不包含 map_pic 和 map_json）
- ...
```

#### `replay_task` 表
```sql
- task_id: 回放任务ID
- simulation_task_id: 关联的仿真任务ID
- name: 回放名称
- status: 回放状态
- total_steps: 总步数
- ...
```

### 2. MongoDB 数据库

#### `map` 集合
```json
{
  "_id": "map_uuid_123",
  "mapId": "map_uuid_123",
  "mapName": "北京市中心路网",
  "userId": 1,
  "addition": {
    // 地图 JSON 数据（从 XML 解析）
    "demand": [...],
    "marginalPoint": [...],
    "cross": [...],
    "link": [...],
    "lane": [...],
    "controller": [...],
    "baseline": [...]
  },
  "createdAt": 1737532800000,
  "updatedAt": 1737532800000
}
```

#### `simulation_data` 集合
```json
{
  "_id": ObjectId("..."),
  "simulationTaskId": "session_123",
  "userId": "1",
  "taskId": "task_456",
  "totalSteps": 1000,
  "startTime": 1737532800000,
  "endTime": 1737536400000,
  "steps": [
    {
      "step": 1,
      "timestamp": 1737532800000,
      "simData": {
        "step": 1,
        "vehicles": [
          {
            "id": "veh_001",
            "x": 123.45,
            "y": 678.90,
            "speed": 15.5,
            "acceleration": 0.5,
            "roadId": "road_1",
            "laneId": "lane_1"
          }
        ],
        "signals": [...]
      },
      "statsData": {
        "speed_ave": 15.5,
        "car_number": 50,
        "jam_index": 0.2
      }
    }
  ]
}
```

## 🔄 完整流程

### 阶段 1：上传地图

```
前端上传地图文件
    ↓
POST /api/map/upload
    ↓
MapServiceImpl.uploadMapWithData()
    ├─ 调用 Python 服务转换文件
    ├─ 解析 XML 为 JSON
    ├─ 保存到 MySQL (map 表)
    │   └─ map_id, name, map_image (Base64)
    └─ 保存到 MongoDB (map 集合)
        └─ addition 字段存储地图 JSON
    ↓
返回 mapId 给前端
```

**关键代码：**
```java
// MapServiceImpl.java
private void saveMapJsonToMongoDB(String mapId, Map<String, Object> mapJsonData, 
                                  String mapName, Long userId) {
    Map<String, Object> document = new HashMap<>();
    document.put("_id", mapId);
    document.put("mapId", mapId);
    document.put("mapName", mapName);
    document.put("userId", userId);
    document.put("addition", mapJsonData);  // ✅ 地图 JSON 数据
    document.put("createdAt", System.currentTimeMillis());
    
    mongoTemplate.save(document, "map");
}
```

### 阶段 2：运行仿真

```
前端创建仿真任务
    ↓
POST /api/simulation/start
    ↓
SimulationServiceImpl.startSimulation()
    ├─ 保存到 MySQL (simulation_task 表)
    │   └─ task_id, map_id, sim_config (不含 map_pic)
    └─ 启动仿真引擎
    ↓
仿真运行中
    ├─ WebSocket 接收车辆数据
    ├─ EngineWebSocketHandler 处理
    └─ 每一步保存到 MongoDB (simulation_data 集合)
        └─ steps 数组包含完整车辆数据
```

**关键代码：**
```java
// EngineWebSocketHandler.java
Map<String, Object> completeStepData = buildCompleteStepData(
        currentStep, stepCollector.getMessages());

SimulationDataCollector.StepData stepData = new SimulationDataCollector.StepData(
        currentStep,
        stepCollector.getTimestamp(),
        completeStepData,  // ✅ 完整的车辆数据
        infoStat
);

dataPersistenceService.saveStepDataAsync(simulationTaskId, userId, taskId, stepData);
```

### 阶段 3：创建回放任务

```
前端创建回放任务
    ↓
POST /api/replay/create
    ↓
ReplayServiceImpl.createReplayTask()
    ├─ 查询 simulation_data 集合
    ├─ 统计总步数
    └─ 保存到 MySQL (replay_task 表)
    ↓
返回 replayTaskId
```

### 阶段 4：获取回放地图信息

```
前端获取地图信息
    ↓
GET /api/replay/{taskId}/map
    ↓
ReplayServiceImpl.getReplayMapInfo()
    ├─ 从 replay_task 表获取 simulation_task_id
    ├─ 从 simulation_data 集合获取 taskId
    ├─ 从 simulation_task 表获取 mapId
    └─ 从 MongoDB map 集合获取地图数据
    ↓
返回地图 JSON (addition 字段)
```

**关键代码：**
```java
// ReplayServiceImpl.java
public Map<String, Object> getReplayMapInfo(String taskId, Long userId) {
    // 1. 获取回放任务
    ReplayTask replayTask = replayTaskRepository.findByTaskIdAndUserId(taskId, userId)
            .orElseThrow(() -> new BusinessException("回放任务不存在"));
    
    // 2. 从 simulation_data 获取 taskId
    String simulationTaskId = replayTask.getSimulationTaskId();
    Map<String, Object> simulationData = mongoTemplate.findOne(
        Query.query(Criteria.where("simulationTaskId").is(simulationTaskId)), 
        Map.class, "simulation_data");
    
    String simTaskId = (String) simulationData.get("taskId");
    
    // 3. 从 simulation_task 获取 mapId
    var simTask = simulationService.getSimulationTask(simTaskId);
    String mapId = simTask.getMapId();
    
    // 4. 从 MongoDB map 集合获取地图数据
    Map<String, Object> mapDocument = mongoTemplate.findOne(
        Query.query(Criteria.where("_id").is(mapId)), 
        Map.class, "map");
    
    return mapDocument;  // 包含 addition 字段
}
```

### 阶段 5：SSE 回放

```
前端建立 SSE 连接
    ↓
GET /api/replay/{taskId}/stream?speed=1.0
    ↓
ReplaySSEController.streamReplayData()
    ├─ 从 simulation_data 集合加载所有步数据
    ├─ 发送 start 事件
    ├─ 逐步推送 data 事件
    │   └─ 每步包含完整车辆数据
    └─ 发送 end 事件
```

## 🎨 前端实现示例

### 1. 获取地图信息并渲染

```javascript
// 获取回放地图信息
async function loadReplayMap(taskId) {
  const response = await fetch(`/api/replay/${taskId}/map`, {
    headers: {
      'Authorization': 'Bearer ' + token
    }
  });
  
  const result = await response.json();
  const mapData = result.data;
  
  // mapData 结构：
  // {
  //   _id: "map_uuid_123",
  //   mapId: "map_uuid_123",
  //   mapName: "北京市中心路网",
  //   addition: {
  //     demand: [...],
  //     cross: [...],
  //     link: [...],
  //     lane: [...],
  //     ...
  //   }
  // }
  
  // 渲染地图
  renderMap(mapData.addition);
  
  return mapData;
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
```

### 2. 建立 SSE 连接并回放

```javascript
async function startReplay(taskId) {
  // 1. 先加载地图
  await loadReplayMap(taskId);
  
  // 2. 建立 SSE 连接
  const eventSource = new EventSource(
    `/api/replay/${taskId}/stream?speed=1.0`,
    { withCredentials: true }
  );
  
  // 3. 监听开始事件
  eventSource.addEventListener('start', (event) => {
    const data = JSON.parse(event.data);
    console.log('回放开始，总步数:', data.totalSteps);
    initProgressBar(data.totalSteps);
  });
  
  // 4. 监听数据事件（每一步）
  eventSource.addEventListener('data', (event) => {
    const stepData = JSON.parse(event.data);
    
    // stepData 结构：
    // {
    //   step: 100,
    //   timestamp: 1737532800000,
    //   simData: {
    //     vehicles: [
    //       { id: "veh_001", x: 123.45, y: 678.90, speed: 15.5, ... }
    //     ],
    //     signals: [...]
    //   },
    //   statistics: {
    //     speed_ave: 15.5,
    //     car_number: 50,
    //     ...
    //   }
    // }
    
    // 渲染车辆
    renderVehicles(stepData.simData.vehicles);
    
    // 更新统计信息
    updateStatistics(stepData.statistics);
    
    // 更新进度
    updateProgress(stepData.step);
  });
  
  // 5. 监听结束事件
  eventSource.addEventListener('end', (event) => {
    console.log('回放完成');
    eventSource.close();
    showCompletionMessage();
  });
  
  // 6. 错误处理
  eventSource.onerror = (error) => {
    console.error('SSE 错误:', error);
    eventSource.close();
  };
}

function renderVehicles(vehicles) {
  // 清除旧车辆或更新位置
  vehicles.forEach(vehicle => {
    let vehicleEl = document.getElementById(`vehicle-${vehicle.id}`);
    
    if (!vehicleEl) {
      // 创建新车辆
      vehicleEl = document.createElement('div');
      vehicleEl.id = `vehicle-${vehicle.id}`;
      vehicleEl.className = 'vehicle';
      document.getElementById('map-canvas').appendChild(vehicleEl);
    }
    
    // 更新位置
    vehicleEl.style.left = vehicle.x + 'px';
    vehicleEl.style.top = vehicle.y + 'px';
    
    // 根据速度设置颜色
    const speedColor = getColorBySpeed(vehicle.speed);
    vehicleEl.style.backgroundColor = speedColor;
  });
}
```

## 📊 数据流图

```
┌─────────────────────────────────────────────────────────────┐
│                     1. 上传地图                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  MySQL: map 表   │
                    │  - map_id        │
                    │  - map_image     │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ MongoDB: map 集合 │
                    │  - addition      │
                    └──────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     2. 运行仿真                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │ MySQL: simulation_task 表     │
              │  - task_id                    │
              │  - map_id (关联地图)          │
              └───────────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │ MongoDB: simulation_data 集合  │
              │  - steps[] (完整车辆数据)      │
              └───────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     3. 回放                                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                ┌──────────────────────────┐
                │ GET /replay/{id}/map     │
                │ 获取地图 JSON (addition)  │
                └──────────────────────────┘
                              │
                              ▼
                      渲染地图到前端
                              │
                              ▼
                ┌──────────────────────────┐
                │ GET /replay/{id}/stream  │
                │ SSE 推送车辆数据          │
                └──────────────────────────┘
                              │
                              ▼
                      逐步渲染车辆
```

## ✅ 关键修改总结

### 1. MapServiceImpl.java
- ✅ 添加 `mapPic` 参数到 `uploadMapWithData()`
- ✅ 保存地图图片到 MySQL `map_image` 字段
- ✅ 添加 `saveMapJsonToMongoDB()` 方法
- ✅ 添加 `getMapJsonFromMongoDB()` 方法

### 2. ReplayController.java
- ✅ 添加 `GET /replay/{taskId}/map` 接口

### 3. ReplayService.java & ReplayServiceImpl.java
- ✅ 添加 `getReplayMapInfo()` 方法
- ✅ 实现从 MongoDB 获取地图数据的逻辑

### 4. SimulationServiceImpl.java
- ✅ 添加 `mapId` 到列表返回字段
- ✅ 从 `simConfig` 中移除 `map_pic` 和 `map_json`

### 5. EngineWebSocketHandler.java
- ✅ 保存完整的车辆数据到 MongoDB

## 🚀 使用流程

1. **上传地图**：`POST /api/map/upload` (带 `mapPic` 参数)
2. **创建仿真**：`POST /api/simulation/start` (使用 `mapId`)
3. **运行仿真**：WebSocket 自动保存数据
4. **创建回放**：`POST /api/replay/create`
5. **获取地图**：`GET /api/replay/{taskId}/map`
6. **开始回放**：`GET /api/replay/{taskId}/stream`

现在整个回放流程已经完全打通！🎉

