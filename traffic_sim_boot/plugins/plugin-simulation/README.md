# plugin-simulation 模块

## 模块说明

`plugin-simulation` 是仿真任务管理插件模块，负责仿真任务的创建、参数配置、状态查询和绿信比控制等功能。

## 功能特性

1. **仿真任务创建**
   - 创建仿真任务
   - 调用Python服务初始化引擎
   - 任务参数验证

2. **仿真参数配置**
   - OD矩阵配置
   - 信号灯配时配置
   - 绿信比控制

3. **仿真状态查询**
   - 查询仿真任务状态
   - 查询仿真历史记录
   - 分页查询任务列表

4. **绿信比控制**
   - 实时调整信号灯绿信比值（0-100）
   - 通过gRPC调用Python服务

## 模块结构

```
plugin-simulation/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/traffic/sim/plugin/simulation/
│   │   │       ├── config/          # 配置类
│   │   │       ├── controller/      # REST API控制器
│   │   │       ├── entity/          # 数据库实体
│   │   │       ├── grpc/            # gRPC客户端
│   │   │       ├── repository/      # 数据访问层
│   │   │       └── service/        # 服务实现
│   │   └── proto/                   # Protocol Buffers定义文件
│   └── pom.xml
└── README.md
```

## 编译说明

### 1. Protocol Buffers 编译

模块包含 `python_service.proto` 文件，需要编译生成 Java 类。

**编译命令**：
```bash
mvn clean compile
```

Maven 会自动使用 `protobuf-maven-plugin` 编译 proto 文件，生成的 Java 类位于：
`target/generated-sources/protobuf/java/`

### 2. 依赖要求

- Java 17+
- Spring Boot 3.2.0
- MySQL（用于存储仿真任务记录）
- Python服务（通过gRPC调用）

## 配置说明

### application.yml 配置

```yaml
# gRPC客户端配置
grpc:
  client:
    python-service:
      address: 'static://localhost:50051'
      negotiationType: plaintext
      enableKeepAlive: true
      keepAliveTime: 30s
      keepAliveTimeout: 5s

# 仿真插件配置
plugin:
  simulation:
    python-service:
      enabled: true
      grpc-port: 50051
      timeout: 300000
```

### 数据库表结构

需要创建 `simulation_task` 表：

```sql
CREATE TABLE `simulation_task` (
  `task_id` VARCHAR(64) NOT NULL PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `map_xml_name` VARCHAR(255),
  `map_xml_path` VARCHAR(500),
  `sim_config` TEXT,
  `status` VARCHAR(20) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## API 接口

### 1. 创建仿真任务

**POST** `/api/simulation/create`

**请求体**：
```json
{
  "name": "仿真名称",
  "simInfo": {
    "name": "仿真名称",
    "mapXmlName": "map.xml",
    "mapXmlPath": "/path/to/map.xml",
    "fixedOd": {
      "od": [...],
      "sg": [...]
    }
  },
  "controlViews": [...]
}
```

**响应**：
```json
{
  "res": "ERR_OK",
  "msg": "ok",
  "data": "task_id"
}
```

### 2. 获取仿真任务列表

**GET** `/api/simulation/list?page=1&size=10`

### 3. 获取仿真任务详情

**GET** `/api/simulation/{taskId}`

### 4. 绿信比控制

**POST** `/api/simulation/control_green_ratio`

**请求体**：
```json
{
  "greenRatio": 25,
  "simulationInfo": {
    "simulation_id": "sim_123",
    "cross_id": 1,
    "session_id": "session_456"
  }
}
```

## 注意事项

1. **会话ID获取**：当前实现中，`sessionId` 从 Cookie 中获取。实际使用时可能需要从请求上下文（JWT令牌）获取用户ID。

2. **gRPC服务**：确保Python服务已启动并监听在配置的gRPC端口（默认50051）。

3. **数据库连接**：确保MySQL数据库已配置并可以连接。

4. **Proto文件编译**：首次编译前需要运行 `mvn clean compile` 以生成gRPC相关的Java类。

## 依赖关系

- `traffic-sim-common`：公共模块，包含接口定义和DTO
- `plugin-user`（可选）：用户管理插件，用于获取用户信息
- `plugin-map`（可选）：地图管理插件，用于获取地图信息

## 开发说明

### 添加新的仿真控制功能

1. 在 `SimulationService` 接口中添加新方法
2. 在 `SimulationServiceImpl` 中实现方法
3. 在 `SimulationController` 中添加REST API端点
4. 如需调用Python服务，在 `PythonGrpcClient` 中添加gRPC调用方法
5. 更新 `python_service.proto` 文件（如需要）

## 版本历史

- **v1.0.0** (2024): 初始版本
  - 仿真任务创建
  - 仿真任务查询
  - 绿信比控制

