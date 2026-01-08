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
cd traffic_sim_boot
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

详细设计文档请参考 `../boot-design/` 目录：

- `boot设计需求.md` - 设计需求文档
- `新版引擎交互接口文档.md` - 引擎交互接口文档
- `plugin-*-模块详细设计.md` - 各插件模块详细设计

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
**最后更新**: 2025年

