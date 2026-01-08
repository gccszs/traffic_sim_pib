# MongoDB配置

<cite>
**本文引用的文件**
- [docker-compose.yml](file://infrastructure/docker-compose.yml)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js)
- [README.md](file://infrastructure/README.md)
- [start-mongodb.sh](file://infrastructure/start-mongodb.sh)
- [stop-mongodb.sh](file://infrastructure/stop-mongodb.sh)
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml)
</cite>

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [依赖关系分析](#依赖关系分析)
7. [性能考量](#性能考量)
8. [故障排查指南](#故障排查指南)
9. [结论](#结论)
10. [附录](#附录)

## 简介
本文件面向Docker环境下的MongoDB服务配置，围绕docker-compose.yml中的MongoDB服务进行系统化解读，涵盖镜像版本选择、平台兼容性、认证与初始化机制、数据卷持久化、健康检查、容器间网络与端口映射、客户端连接配置，以及初始化失败排查、权限错误处理与性能监控建议。目标是帮助读者快速理解并安全地部署与运维MongoDB服务。

## 项目结构
MongoDB相关配置集中在infrastructure目录下，核心文件包括：
- docker-compose主配置与MongoDB专用配置
- 初始化脚本init-mongo.js
- 服务启动/停止脚本
- 应用侧连接配置示例

```mermaid
graph TB
subgraph "基础设施"
DC["docker-compose.yml"]
DCM["docker-compose.mongodb.yml"]
DCC["docker-compose.core.yml"]
INIT["init-mongo.js"]
START["start-mongodb.sh"]
STOP["stop-mongodb.sh"]
DOC["README.md"]
end
subgraph "应用"
APPYML["application.yml"]
end
DC --> DCM
DC --> DCC
DCM --> INIT
DCC --> INIT
START --> DCM
STOP --> DCM
APPYML --> DC
DOC --> DC
```

图表来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L11-L33)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L43-L65)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L1-L24)
- [start-mongodb.sh](file://infrastructure/start-mongodb.sh#L25-L38)
- [stop-mongodb.sh](file://infrastructure/stop-mongodb.sh#L10-L18)
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L28-L39)

章节来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L1-L253)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L1-L73)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L1-L168)
- [README.md](file://infrastructure/README.md#L1-L488)

## 核心组件
- MongoDB服务容器：基于mongo:7.0镜像，启用认证，映射27017端口，使用本地数据卷持久化。
- 初始化脚本：在首次启动时创建traffic_sim数据库、traffic_sim用户并授予readWrite权限，并预创建maps/simulation_data/replay_data集合。
- 健康检查：通过mongosh执行ping命令检测数据库可用性。
- 网络与端口：使用bridge网络traffic-sim-network，将容器内27017映射到宿主机27017。
- 客户端连接：应用侧通过URI或分离配置连接MongoDB，使用admin作为认证数据库。

章节来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L11-L33)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L28-L39)

## 架构总览
下图展示MongoDB服务在Docker环境中的部署与交互关系：

```mermaid
graph TB
subgraph "宿主机"
HOST["27017端口映射"]
NET["traffic-sim-network桥接网络"]
VOL_DB["mongodb_data数据卷"]
VOL_CFG["mongodb_config数据卷"]
end
subgraph "容器"
MONGO["traffic-sim-mongodb<br/>mongo:7.0<br/>--auth"]
INIT["/docker-entrypoint-initdb.d/init-mongo.js"]
EXP["mongo-express(可选)"]
end
HOST --- MONGO
NET --- MONGO
VOL_DB --- MONGO
VOL_CFG --- MONGO
INIT --- MONGO
EXP -.-> MONGO
```

图表来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L11-L33)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L43-L65)

## 详细组件分析

### 镜像与平台选择
- 镜像版本：mongo:7.0，确保与当前应用生态兼容并具备稳定特性。
- 平台指定：linux/amd64，保证在非amd64架构（如Apple Silicon）上也能正确拉取与运行，避免跨平台镜像拉取失败或兼容性问题。

章节来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L44-L45)

### 认证与初始化机制
- 环境变量：
  - MONGO_INITDB_ROOT_USERNAME/MONGO_INITDB_ROOT_PASSWORD：设置初始管理员账户，用于容器启动时的root用户。
  - MONGO_INITDB_DATABASE：指定初始化时创建的数据库名称（traffic_sim）。
- 初始化脚本：
  - 在首次启动时，容器会在/docker-entrypoint-initdb.d路径下执行init-mongo.js。
  - 脚本创建traffic_sim数据库用户traffic_sim并授予readWrite角色；同时预创建maps、simulation_data、replay_data集合。
- 启动参数：command: mongod --auth，强制开启认证，确保后续连接必须提供凭据。

```mermaid
flowchart TD
Start(["容器启动"]) --> Env["加载环境变量<br/>MONGO_INITDB_*"]
Env --> Entrypoint["进入入口点脚本"]
Entrypoint --> InitJS["发现并执行 init-mongo.js"]
InitJS --> CreateUser["创建数据库用户 traffic_sim<br/>授予 readWrite 权限"]
CreateUser --> CreateColls["预创建集合 maps/simulation_data/replay_data"]
CreateColls --> AuthOn["启动 mongod --auth"]
AuthOn --> Ready(["服务就绪"])
```

图表来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L48-L58)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L15-L25)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)

章节来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L48-L58)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L15-L25)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)

### 数据卷与持久化策略
- mongodb_data：存储MongoDB数据文件，实现容器重启后数据不丢失。
- mongodb_config：存储配置数据库数据，保障分片/副本集等场景的配置一致性。
- 挂载方式：通过本地驱动（driver: local）将宿主机路径映射到容器内的/data/db与/data/configdb。

```mermaid
erDiagram
MONGODB_DATA {
string driver
string name
}
MONGODB_CONFIG {
string driver
string name
}
CONTAINER_DB {
string path "/data/db"
}
CONTAINER_CFG {
string path "/data/configdb"
}
MONGODB_DATA ||--|| CONTAINER_DB : "挂载"
MONGODB_CONFIG ||--|| CONTAINER_CFG : "挂载"
```

图表来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L229-L243)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L59-L63)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L145-L159)

章节来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L229-L243)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L59-L63)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L145-L159)

### 健康检查
- 命令：通过mongosh对test数据库执行db.runCommand("ping")，输出结果为1表示健康。
- 配置：每10秒检查一次，超时5秒，重试5次。
- 作用：自动化监控容器内MongoDB实例的可用性，便于编排系统进行故障恢复。

```mermaid
sequenceDiagram
participant Compose as "Docker Compose"
participant Container as "traffic-sim-mongodb"
participant Mongosh as "mongosh"
participant DB as "MongoDB"
Compose->>Container : 执行 healthcheck
Container->>Mongosh : 执行 ping 命令
Mongosh->>DB : db.runCommand("ping")
DB-->>Mongosh : 返回 ok=1
Mongosh-->>Container : 输出结果
Container-->>Compose : 健康状态
```

图表来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L62-L66)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L29-L33)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L61-L65)

章节来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L62-L66)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L29-L33)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L61-L65)

### 容器间网络与端口映射
- 网络：使用bridge网络traffic-sim-network，便于同一项目内服务互通。
- 端口：将容器内27017映射到宿主机27017，支持本地直连与外部访问。
- 可选管理工具：mongo-express（可选），通过独立容器提供Web管理界面。

```mermaid
graph LR
subgraph "Docker网络"
NET["traffic-sim-network"]
end
subgraph "容器"
MONGO["mongodb"]
ME["mongo-express(可选)"]
end
NET --- MONGO
NET --- ME
MONGO --> |"27017:27017"| HOST["宿主机"]
```

图表来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L60-L66)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L27-L28)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L59-L65)

章节来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L60-L66)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L27-L28)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L59-L65)

### 客户端连接配置示例
- URI方式（推荐）：在应用配置中使用mongodb://root:root@localhost:27017/traffic_sim?authSource=admin，明确指定认证数据库为admin。
- 分离配置：host/port/database/username/password/authentication-database分别配置。
- 应用侧示例：traffic-sim-server的application.yml展示了MongoDB连接配置，包含URI与分离配置两种方式。

```mermaid
sequenceDiagram
participant App as "应用(Java/Spring)"
participant Mongo as "MongoDB(localhost : 27017)"
App->>App : 解析连接配置(URI或分离配置)
App->>Mongo : 建立连接(authSource=admin)
Mongo-->>App : 认证通过/拒绝
App-->>App : 继续业务操作
```

图表来源
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L28-L39)
- [README.md](file://infrastructure/README.md#L202-L205)

章节来源
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L28-L39)
- [README.md](file://infrastructure/README.md#L202-L205)

## 依赖关系分析
- 配置文件依赖：
  - docker-compose.yml为主配置，包含MongoDB服务定义、数据卷与网络。
  - docker-compose.mongodb.yml为MongoDB专用配置，便于单独启动。
  - docker-compose.core.yml为核心服务配置，包含MongoDB、Redis、Kafka等。
- 初始化脚本依赖：
  - init-mongo.js依赖于容器入口点机制，在首次启动时自动执行。
- 应用依赖：
  - 应用侧通过application.yml连接MongoDB，需与容器内认证配置保持一致。

```mermaid
graph TB
DC["docker-compose.yml"] --> MONGO["mongodb服务"]
DCM["docker-compose.mongodb.yml"] --> MONGO
DCC["docker-compose.core.yml"] --> MONGO
MONGO --> INIT["init-mongo.js"]
APP["application.yml"] --> MONGO
```

图表来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L11-L25)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L43-L57)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L28-L39)

章节来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L11-L25)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L43-L57)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L28-L39)

## 性能考量
- 认证开销：启用--auth会带来少量认证开销，通常可忽略；建议在生产环境配合连接池与索引优化。
- 数据卷I/O：mongodb_data与mongodb_config使用本地驱动，注意宿主机磁盘性能与容量规划。
- 健康检查频率：默认10秒一次，对生产环境可根据负载调整间隔与超时。
- 网络隔离：通过bridge网络隔离，避免不必要的外网暴露，减少潜在攻击面。

## 故障排查指南
- 初始化失败
  - 症状：容器启动后未创建traffic_sim数据库或用户。
  - 排查：确认init-mongo.js已挂载到/docker-entrypoint-initdb.d且权限正确；查看容器日志以定位错误。
  - 参考：README中关于MongoDB初始化的说明。
- 权限错误
  - 症状：连接时报认证失败或权限不足。
  - 排查：确认应用配置中的authentication-database为admin；核对用户名/密码；检查init-mongo.js是否成功创建用户。
  - 参考：README中“MongoDB 认证失败”的排查步骤。
- 健康检查失败
  - 症状：Docker Compose显示mongodb容器不健康。
  - 排查：检查mongosh命令可用性、端口可达性、认证配置；查看healthcheck配置与日志。
- 端口冲突
  - 症状：27017端口被占用导致容器无法启动。
  - 排查：变更映射端口或释放占用端口；确认防火墙策略。

章节来源
- [README.md](file://infrastructure/README.md#L454-L465)
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L62-L66)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L29-L33)

## 结论
本配置以mongo:7.0为基础，结合--auth强制认证、init-mongo.js自动化初始化、本地数据卷持久化与健康检查，构建了安全、可靠且易于维护的MongoDB服务。通过统一的docker-compose配置与脚本化管理，能够快速在Docker环境中完成部署与运维。生产环境建议进一步强化安全策略（如TLS、最小权限、网络隔离）与监控告警体系。

## 附录
- 启停脚本
  - 单独启动MongoDB：start-mongodb.sh（或对应Windows脚本）。
  - 停止MongoDB：stop-mongodb.sh。
- 管理工具
  - mongo-express（可选）：通过Web界面管理数据库与集合。
- 文档参考
  - infrastructure/README.md提供了服务说明、连接信息、常用命令与故障排查。

章节来源
- [start-mongodb.sh](file://infrastructure/start-mongodb.sh#L25-L58)
- [stop-mongodb.sh](file://infrastructure/stop-mongodb.sh#L10-L18)
- [README.md](file://infrastructure/README.md#L184-L211)