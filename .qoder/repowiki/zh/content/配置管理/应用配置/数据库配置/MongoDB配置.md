# MongoDB配置

<cite>
**本文引用的文件**
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml)
- [application-dev.yml](file://traffic-sim-server/src/main/resources/application-dev.yml)
- [application-prod.yml](file://traffic-sim-server/src/main/resources/application-prod.yml)
- [docker-compose.yml](file://infrastructure/docker-compose.yml)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js)
- [start-mongodb.sh](file://infrastructure/start-mongodb.sh)
- [stop-mongodb.sh](file://infrastructure/stop-mongodb.sh)
</cite>

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [依赖关系分析](#依赖关系分析)
7. [性能考虑](#性能考虑)
8. [故障排查指南](#故障排查指南)
9. [结论](#结论)
10. [附录](#附录)

## 简介
本文件围绕项目中的MongoDB配置进行系统化梳理，重点解析Spring Boot应用中spring.data.mongodb.uri的连接字符串结构与语义，对比URI方式与分离式配置（host/port/database等）的优劣与适用场景；结合Docker Compose中的mongodb服务定义，说明容器启动时通过环境变量设置管理员凭据、通过命令行启用认证的安全实践，以及init-mongo.js在首次启动时自动创建数据库与用户的流程。最后提供常见连接认证问题的解决方案与生产环境最佳实践建议。

## 项目结构
本项目采用多模块与基础设施编排相结合的方式：
- 应用配置位于traffic-sim-server模块的resources目录下，包含开发、生产与通用配置文件。
- 基础设施编排位于infrastructure目录，包含完整的Docker Compose定义与MongoDB初始化脚本。
- MongoDB服务通过Compose在容器内启用认证，并在首次启动时执行初始化脚本创建数据库与用户。

```mermaid
graph TB
subgraph "应用层"
APP["traffic-sim-server<br/>Spring Boot 应用"]
end
subgraph "基础设施层"
DC["docker-compose.yml<br/>完整编排"]
DCCore["docker-compose.core.yml<br/>核心服务编排"]
DCMDB["docker-compose.mongodb.yml<br/>MongoDB独立编排"]
INIT["init-mongo.js<br/>初始化脚本"]
end
subgraph "数据库层"
MONGO["MongoDB 服务容器"]
end
APP --> |"读写"| MONGO
DC --> MONGO
DCCore --> MONGO
DCMDB --> MONGO
MONGO --> INIT
```

图表来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L43-L65)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L11-L33)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L1-L25)

章节来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L1-L253)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L1-L168)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L1-L73)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L1-L25)

## 核心组件
- Spring Boot MongoDB配置
  - 在通用配置中明确使用URI方式配置MongoDB连接，包含认证信息与authSource参数，同时保留分离式配置作为备选方案注释。
  - 开发环境配置中仅指定数据库名，便于本地快速切换。
  - 生产环境配置中使用环境变量注入，避免硬编码敏感信息。
- Docker Compose MongoDB服务
  - 通过环境变量设置管理员用户名与密码，以及默认数据库名。
  - 通过命令行参数启用认证，确保容器启动即强制认证。
  - 将初始化脚本挂载到初始化目录，实现首次启动自动执行。
- 初始化脚本
  - 在首次启动时创建目标数据库与用户，并授予读写权限；同时创建常用集合。

章节来源
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L28-L40)
- [application-dev.yml](file://traffic-sim-server/src/main/resources/application-dev.yml#L8-L12)
- [application-prod.yml](file://traffic-sim-server/src/main/resources/application-prod.yml#L8-L16)
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L43-L65)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L11-L33)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)

## 架构总览
下图展示应用与MongoDB容器之间的交互关系，以及容器启动时的初始化流程。

```mermaid
sequenceDiagram
participant App as "Spring Boot 应用"
participant MongoSvc as "MongoDB 容器"
participant Init as "init-mongo.js"
participant Vol as "数据卷"
App->>MongoSvc : "通过URI连接含认证信息"
MongoSvc->>Vol : "挂载数据与配置卷"
MongoSvc->>Init : "首次启动执行初始化脚本"
Init-->>MongoSvc : "创建数据库与用户"
MongoSvc-->>App : "建立认证后的连接"
```

图表来源
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L31-L32)
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)

## 详细组件分析

### 连接字符串结构解析（URI方式）
- URI模式：spring.data.mongodb.uri=mongodb://root:root@localhost:27017/traffic_sim?authSource=admin
  - mongodb://：协议前缀，标识使用MongoDB驱动。
  - root:root：认证用户名与密码，分别对应username与password。
  - localhost:27017：主机与端口，对应host与port。
  - traffic_sim：目标数据库名，对应database。
  - authSource=admin：认证数据库来源，用于指定在哪个数据库上验证用户身份。
- 分离式配置（host/port/database等）
  - 优点：字段清晰、易于按需覆盖；适合复杂网络拓扑或需要灵活组合参数的场景。
  - 缺点：需要显式维护多个键值，容易遗漏关键参数（如authSource），且不便于一次性传递认证信息。
- 适用场景
  - URI方式：配置简洁、一次性传递认证信息，适合大多数开发与生产环境；便于版本控制与CI/CD注入。
  - 分离式配置：当需要动态拼接参数或与外部工具联动时使用；但需确保所有必要参数齐全。

章节来源
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L31-L39)

### Docker Compose中的MongoDB服务定义
- 环境变量
  - MONGO_INITDB_ROOT_USERNAME/MONGO_INITDB_ROOT_PASSWORD：初始化阶段的管理员凭据，容器首次启动时生效。
  - MONGO_INITDB_DATABASE：初始化阶段默认数据库名。
- 命令行参数
  - command: mongod --auth：强制启用认证，确保后续连接必须通过认证。
- 挂载与健康检查
  - 挂载数据卷与配置卷，保证持久化与配置隔离。
  - 健康检查通过mongosh执行ping命令，验证服务可用性。
- 初始化脚本
  - 将init-mongo.js挂载到初始化目录，容器首次启动时自动执行，完成数据库与用户创建。

章节来源
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L43-L65)
- [docker-compose.mongodb.yml](file://infrastructure/docker-compose.mongodb.yml#L11-L33)

### 初始化脚本（init-mongo.js）工作流
- 脚本作用
  - 切换到目标数据库并创建用户，授予读写权限。
  - 可选地创建常用集合，便于应用直接使用。
- 执行时机
  - 容器首次启动时，当检测到数据卷为空时自动执行。
- 输出
  - 打印初始化成功日志，便于运维确认。

```mermaid
flowchart TD
Start(["容器首次启动"]) --> CheckVol["检查数据卷是否为空"]
CheckVol --> |是| ExecInit["执行 init-mongo.js"]
ExecInit --> SwitchDB["切换到目标数据库"]
SwitchDB --> CreateUser["创建用户并授权"]
CreateUser --> CreateColl["可选：创建集合"]
CreateColl --> PrintOK["打印初始化成功日志"]
PrintOK --> End(["初始化完成"])
CheckVol --> |否| End
```

图表来源
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)

章节来源
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L1-L25)

### 应用侧配置与环境变量注入（生产环境）
- 生产环境配置
  - 使用环境变量注入host、port、database、username、password，避免在代码中硬编码敏感信息。
  - 通过占位符与默认值实现灵活部署，便于在不同环境间切换。
- 开发环境配置
  - 仅指定数据库名，简化本地开发体验；若需要连接MongoDB，可在IDE或外部工具中单独配置连接串。
- 通用配置
  - 推荐使用URI方式集中管理认证信息与参数，提升可维护性与一致性。

章节来源
- [application-prod.yml](file://traffic-sim-server/src/main/resources/application-prod.yml#L8-L16)
- [application-dev.yml](file://traffic-sim-server/src/main/resources/application-dev.yml#L8-L12)
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L31-L39)

### 独立启动MongoDB服务的脚本
- 启动脚本
  - 检查Docker与Docker Compose环境，拉起MongoDB服务并等待其启动。
  - 输出访问信息与连接字符串，便于快速验证。
- 停止脚本
  - 提供停止与清理容器的便捷操作提示，注意删除数据卷的风险。

章节来源
- [start-mongodb.sh](file://infrastructure/start-mongodb.sh#L1-L63)
- [stop-mongodb.sh](file://infrastructure/stop-mongodb.sh#L1-L23)

## 依赖关系分析
- 组件耦合
  - 应用配置依赖于Docker Compose提供的MongoDB服务；服务端口、认证与数据库名称需与应用配置保持一致。
  - 初始化脚本与环境变量共同决定首次启动时的数据库与用户状态。
- 外部依赖
  - Docker与Docker Compose版本需满足服务定义要求。
  - MongoDB镜像版本与命令行参数需兼容。

```mermaid
graph LR
AppCfg["应用配置<br/>application.yml"] --> MongoSvc["MongoDB 服务"]
EnvVars["环境变量<br/>MONGO_INITDB_*"] --> MongoSvc
InitJS["init-mongo.js"] --> MongoSvc
MongoSvc --> Vol["数据卷"]
```

图表来源
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L28-L40)
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)

章节来源
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L28-L40)
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)

## 性能考虑
- 连接池与超时
  - 应用层可通过连接池参数优化连接复用与超时行为，减少频繁连接带来的开销。
- 认证与网络
  - 在容器内启用认证可降低未授权访问风险；跨网络访问时建议使用安全通道与最小权限原则。
- 初始化脚本
  - 首次启动时执行初始化脚本可能影响启动时间，建议在生产环境中提前准备数据卷，避免重复初始化。

## 故障排查指南
- 连接认证失败
  - 检查URI中的用户名、密码与authSource是否正确；确认容器已通过命令行启用认证。
  - 确认初始化脚本已成功创建用户并授予相应权限。
- 权限不足
  - 确认用户角色包含对目标数据库的读写权限；必要时调整初始化脚本中的角色配置。
- 端口或主机不可达
  - 检查容器端口映射与防火墙设置；确认应用侧host与port与容器暴露端口一致。
- 首次启动无数据
  - 确认数据卷未被意外清理；检查初始化脚本是否挂载到正确路径并具备执行权限。
- 生产环境敏感信息泄露
  - 使用环境变量注入而非硬编码；在CI/CD中通过密钥管理工具注入敏感参数。

章节来源
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L31-L39)
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L7-L16)

## 结论
- URI方式在配置简洁性与安全性方面具有明显优势，推荐在大多数场景下使用。
- 分离式配置适用于需要灵活组合参数的特殊场景，但需确保参数完整性。
- Docker Compose通过环境变量与命令行参数实现了“先认证后业务”的安全基线，配合初始化脚本可实现自动化部署。
- 生产环境应坚持使用环境变量注入敏感信息，结合最小权限原则与严格的密钥管理策略，确保系统安全与可维护性。

## 附录
- 关键配置要点速览
  - 应用侧：spring.data.mongodb.uri包含认证信息与authSource。
  - Compose侧：MONGO_INITDB_ROOT_USERNAME/PASSWORD、MONGO_INITDB_DATABASE、command: mongod --auth。
  - 初始化：init-mongo.js创建用户与集合。
- 启动与验证
  - 使用独立脚本快速启动MongoDB服务并输出连接信息，便于快速验证。

章节来源
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L31-L39)
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L42-L66)
- [init-mongo.js](file://infrastructure/mongodb/init-mongo.js#L4-L21)
- [start-mongodb.sh](file://infrastructure/start-mongodb.sh#L49-L50)