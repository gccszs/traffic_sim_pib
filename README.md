# Traffic Simulation Boot

交通仿真系统 - Spring Boot 插件化架构版本

## 📋 项目说明

本项目是基于 Spring Boot 3.x 的单体应用，采用插件化架构设计。所有模块打包为一个 JAR 文件，通过 Maven 依赖引入插件模块。

## 🏗️ 项目结构

```
traffic-sim-boot/
├── pom.xml                          # 根POM（聚合所有模块）
├── traffic-sim-common/             # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/traffic/sim/common/
│       ├── constant/                # 常量定义
│       ├── dto/                     # 数据传输对象
│       ├── exception/               # 异常定义
│       ├── model/                   # 数据模型
│       ├── response/                # 响应格式
│       ├── service/                 # 服务接口定义
│       ├── util/                    # 工具类
│       └── config/                  # 配置类
├── traffic-sim-server/             # 主启动模块
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/traffic/sim/
│       │   └── TrafficSimApplication.java  # 主启动类
│       └── resources/
│           └── application.yml      # 配置文件
└── plugins/                         # 插件父模块
    ├── pom.xml
    ├── plugin-auth/                 # 认证授权插件
    ├── plugin-user/                 # 用户管理插件
    ├── plugin-map/                  # 地图管理插件
    ├── plugin-simulation/           # 仿真任务插件
    ├── plugin-engine-manager/       # 引擎管理插件
    ├── plugin-statistics/           # 实时统计分析插件
    └── plugin-engine-replay/        # 回放功能插件
```

## 🔧 技术栈

- **Spring Boot**: 3.2.0
- **Java**: 17
- **数据库**: MySQL + MongoDB
- **通信**: WebSocket + gRPC
- **构建工具**: Maven
- **API文档**: SpringDoc OpenAPI 3.0

## 📦 模块说明

### traffic-sim-common（公共模块）

提供所有插件模块共享的基础功能：

- **常量定义**: 错误码、用户状态、WebSocket消息类型等
- **异常定义**: BusinessException、ServiceException
- **响应格式**: ApiResponse、PageResult
- **数据模型**: WebSocketInfo、SimInfo、StatisticsData等
- **DTO**: UserDTO、LoginRequest、LoginResponse等
- **服务接口**: UserService、AuthService、StatisticsService（接口定义）
- **工具类**: StringUtils、JsonUtils

### traffic-sim-server（主启动模块）

Spring Boot 主启动模块，负责：

- 应用启动
- 全局配置管理
- 插件依赖引入（通过Maven依赖）
- WebSocket配置
- 数据库配置

### plugins（插件模块）

业务功能插件，每个插件独立开发：

- **plugin-auth**: 认证授权（登录、注册、JWT令牌）
- **plugin-user**: 用户管理（用户CRUD、角色权限）
- **plugin-map**: 地图管理（地图上传、转换、存储）
- **plugin-simulation**: 仿真任务（任务创建、状态查询）
- **plugin-engine-manager**: 引擎管理（WebSocket连接、消息转发）
- **plugin-statistics**: 实时统计分析（统计计算、数据推送）
- **plugin-engine-replay**: 回放功能（历史数据回放）

## 🚀 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- MongoDB 4.4+

### 2. 数据库配置

修改 `traffic-sim-server/src/main/resources/application.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/traffic_sim
    username: root
    password: root
  data:
    mongodb:
      uri: mongodb://localhost:27017/traffic_sim
```

### 3. 编译项目

```bash
mvn clean install
```

### 4. 运行项目

```bash
cd traffic-sim-server
mvn spring-boot:run
```

或者直接运行主类：`com.traffic.sim.TrafficSimApplication`

### 5. 访问API文档

启动后访问：http://localhost:3822/swagger-ui.html

## 📝 设计文档

详细设计说明请参考仓库中的以下文档：

- `infrastructure/README.md` - 基础设施与数据服务部署
- `plugins/*/README.md` - 各业务插件的模块说明
- `plugins/*-Issue.md` - 插件待办与问题记录

## 🔌 插件机制

### 插件加载方式

通过 Maven 依赖引入，编译时打包为一个 JAR：

```xml
<!-- traffic-sim-server/pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.traffic.sim</groupId>
        <artifactId>plugin-auth</artifactId>
    </dependency>
    <!-- 其他插件... -->
</dependencies>
```

### 插件间通信

- 通过 Spring Bean 注入
- 通过 common 模块中的接口定义
- 避免插件间直接依赖

### 插件配置隔离

使用配置前缀隔离：

```yaml
plugin:
  auth:
    jwt:
      secret: xxx
  user:
    default-role: USER
```

## 📌 注意事项

1. **插件不是运行时动态加载**，而是在编译时打包为一个 JAR
2. **插件通过类路径自动加载**，使用标准 Spring 注解（@Service、@Controller等）
3. **插件间通信**通过 common 模块中的接口定义，避免直接依赖
4. **数据库按服务拆分**，但统一使用 MySQL + MongoDB

## 🔗 相关文档

- [Boot化设计方案](../boot-design/README.md)
- [设计需求文档](../boot-design/boot设计需求.md)
- [新版引擎交互接口文档](../boot-design/新版引擎交互接口文档.md)

---

**项目版本**: 1.0.0-SNAPSHOT  
**最后更新**: 2026年

## 🧭 附加模块

### map_convert_services（Python服务）

- 基于 `FastAPI` 与 `uvicorn`，负责地图上传转换、仿真引擎初始化、插件管理以及与引擎的 WebSocket 通信
- 默认端口由环境变量 `APP_PORT` 控制，缺省为 `8000`（参见 `map_convert_services/config.py:4-18`）
- 关键接口：
  - `POST /fileupload`：上传地图（OSM/自定义），转换为引擎 `map.xml`，返回二进制流（`map_convert_services/web_app.py:52-81`）
  - `POST /init_simeng`：依据前端提交的仿真配置生成 `map.xml`、`od.xml`，复制所选插件并启动 `SimulationEngine.exe`（`map_convert_services/web_app.py:90-189`）
  - `POST /upload_plugin`：校验并接收插件 ZIP 包，落盘后加载清单（`map_convert_services/web_app.py:200-232`）
  - `WS /ws/exe/{exe_id}`：与前端/后端的双向消息通道（`map_convert_services/web_app.py:237-258`）
- 引擎启动流程由 `utils/command_runner.py` 实现，支持文件日志与控制台输出（`map_convert_services/utils/command_runner.py:11-199`）
- 插件管理由 `sim_plugin.py` 提供，支持插件描述清单加载、更新与复制（`map_convert_services/sim_plugin.py:8-177`）

### SimEngPI（仿真引擎与资源）

- 目录内包含 `SimulationEngine.exe` 及所需 DLL，按会话用户隔离仿真文件（`map_convert_services/web_app.py:101-113`）
- 每次仿真初始化会生成 `map.xml` 与 `od.xml`（OD与信号数据由前端 JSON 转换，见 `json_utils.py` 与 `web_app.py:117-160`）

### frontend（前端资源）

- 打包后的静态资源与 `index.html`，用于构建交互式仿真控制界面
- 静态图像位于 `frontend/sim_imgs/`

### infrastructure（基础设施）

- 使用 Docker Compose 管理 `MySQL`、`MongoDB`、`Redis`、`Kafka`、管理UI等（详见 `infrastructure/README.md:1-400`）
- 提供 `start.sh/.bat` 与 `stop.sh/.bat` 一键启动/停止脚本

## 🧩 Java 服务与插件

### traffic-sim-server（主服务）

- 端口 `8080`，上下文路径 `/api`（`traffic-sim-server/src/main/resources/application.yml:48-57`）
- 跨域配置（`traffic-sim-server/src/main/java/com/traffic/sim/config/WebConfig.java:14-27`）
- 全局异常处理（`traffic-sim-server/src/main/java/com/traffic/sim/exception/GlobalExceptionHandler.java:25-107`）
- OpenAPI 文档路径：`/api/swagger-ui.html`（`traffic-sim-server/src/main/resources/application.yml:59-67`）

### traffic-sim-common（公共模块）

- 提供 DTO、响应封装、错误码、服务接口与工具类，供各插件与主服务共享

### plugins（业务插件）

- `plugin-simulation`：仿真任务管理与控制，REST 接口例如：
  - 创建仿真任务 `POST /api/simulation/create`（`plugins/plugin-simulation/src/main/java/com/traffic/sim/plugin/simulation/controller/SimulationController.java:37-60`）
  - 任务列表 `GET /api/simulation/list`（`plugins/plugin-simulation/src/main/java/com/traffic/sim/plugin/simulation/controller/SimulationController.java:65-79`）
  - 绿信比控制 `POST /api/simulation/control_green_ratio`（`plugins/plugin-simulation/src/main/java/com/traffic/sim/plugin/simulation/controller/SimulationController.java:102-131`）
- `plugin-engine-manager`：前端与仿真引擎的 WebSocket 桥接与配置（`plugins/plugin-engine-manager/src/main/java/com/traffic/sim/plugin/engine/manager/config/WebSocketConfig.java:16-39`，`EngineManagerProperties.java:15-41`）
- `plugin-auth`、`plugin-user`、`plugin-map`、`plugin-statistics`、`plugin-engine-replay`：分别负责认证、用户、地图、统计与回放等功能，按需引入

## 🚀 完整启动流程

- 启动基础设施：在 `infrastructure/` 执行 `docker-compose up -d`（详见其 README）
- 安装并启动 Python 服务：
  - 安装依赖：在 `map_convert_services/` 执行 `pip install -r requirements.txt`
  - 启动服务：`python -m uvicorn map_convert_services.web_app:app --host 0.0.0.0 --port 8000`
- 编译并运行 Java 服务：在项目根目录执行 `mvn clean install`，进入 `traffic-sim-server/` 执行 `mvn spring-boot:run`
- 访问 API 文档：`http://localhost:8080/api/swagger-ui.html`

## 🕸️ 通信与数据流

- 前端通过 REST 上传地图与配置，Java 服务与 Python 服务协作生成仿真所需文件
- WebSocket 通道 `/ws/exe/{exe_id}` 用于前端与引擎的数据交互（消息类型定义见 `traffic-sim-common/common/constant/*`）
- gRPC 面向 Python 服务的调用在插件内配置，服务不可用时主服务会给出兜底提示（`TrafficSimApplication.java:33-39`）

## ⚠️ 生产建议

- 调整 `infrastructure/` 中默认凭证与端口映射，启用 TLS 与网络隔离
- 配置备份策略与监控，限制上传大小（`application.yml:99-103`）

