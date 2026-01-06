# plugin-engine-replay 模块

## 📋 模块说明

`plugin-engine-replay` 模块提供仿真历史数据回放功能，包括回放任务创建、回放控制、回放数据查询等核心功能。

**模块名称**: plugin-engine-replay  
**模块类型**: 业务插件模块  
**依赖关系**: 依赖 `traffic-sim-common`

---

## 一、模块职责

### 1.1 核心功能

1. **回放任务管理**
   - 创建回放任务
   - 查询回放任务列表
   - 删除回放任务

2. **回放控制**
   - 播放/暂停
   - 停止
   - 跳转到指定步数
   - 设置播放速度

3. **回放数据管理**
   - 存储仿真历史数据（MongoDB）
   - 按步数范围查询回放数据
   - 数据压缩与优化

---

## 二、数据存储

### 2.1 MySQL存储

- **replay_task表**: 存储回放任务元数据
  - 任务ID、名称、状态
  - 关联的仿真任务ID
  - 当前步数、总步数
  - 播放速度
  - 用户ID

### 2.2 MongoDB存储

- **replay_data集合**: 存储回放数据
  - 任务ID
  - 仿真步数
  - 时间戳
  - 仿真数据
  - 统计数据

---

## 三、API接口

### 3.1 回放任务管理

- `POST /api/replay/create` - 创建回放任务
- `GET /api/replay/{taskId}` - 获取回放任务详情
- `GET /api/replay/list` - 获取回放任务列表（分页）
- `DELETE /api/replay/{taskId}` - 删除回放任务

### 3.2 回放控制

- `POST /api/replay/{taskId}/control` - 控制回放（播放、暂停、停止、跳转、设置速度）

### 3.3 回放数据查询

- `GET /api/replay/{taskId}/data` - 获取回放数据（按步数范围）

---

## 四、配置说明

### 4.1 配置属性

```yaml
plugin:
  replay:
    # 存储配置
    storage:
      compression-enabled: true      # 数据压缩启用
      compression-algorithm: gzip    # 压缩算法
      retention-days: 30             # 数据保留时间（天）
    
    # 回放配置
    replay:
      default-speed: 1.0             # 默认播放速度（倍速）
      max-speed: 10.0                # 最大播放速度
      min-speed: 0.1                 # 最小播放速度
      batch-step-size: 100           # 批量查询步数
```

---

## 五、使用示例

### 5.1 创建回放任务

```bash
POST /api/replay/create
Content-Type: application/json

{
  "simulationTaskId": "sim_123",
  "name": "回放任务1"
}
```

### 5.2 控制回放

```bash
POST /api/replay/{taskId}/control
Content-Type: application/json

{
  "action": "PLAY"  // PLAY, PAUSE, STOP, SEEK, SET_SPEED
}
```

### 5.3 跳转到指定步数

```bash
POST /api/replay/{taskId}/control
Content-Type: application/json

{
  "action": "SEEK",
  "targetStep": 1000
}
```

### 5.4 设置播放速度

```bash
POST /api/replay/{taskId}/control
Content-Type: application/json

{
  "action": "SET_SPEED",
  "speed": 2.0  // 2倍速
}
```

---

## 六、模块依赖

```
plugin-engine-replay
    ↓
traffic-sim-common
    ├── ApiResponse
    ├── PageResult
    └── BusinessException
```

---

## 七、数据库表结构

### 7.1 replay_task表

```sql
CREATE TABLE `replay_task` (
  `task_id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `simulation_task_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `current_step` BIGINT DEFAULT 0,
  `total_steps` BIGINT DEFAULT 0,
  `playback_speed` DOUBLE DEFAULT 1.0,
  `user_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_simulation_task_id` (`simulation_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

**文档版本**: v1.0  
**创建日期**: 2024年

