# SimEngPIB Spring Boot 版本

这是原 Python FastAPI 项目的 Spring Boot 迁移版本，保持了相同的 API 接口和功能。

## 🚀 快速开始

### 环境要求

- Java 17 或更高版本
- Maven 3.6+
- Python 3.11.0（用于地图转换脚本）
- 原始的 Python 依赖包（numpy, sympy, gekko 等）

### 安装步骤

1. **下载项目安装包**

2. **安装 Python 依赖**

```bash
pip install -r requirements.txt
```

3. **编译 Java 项目**

```bash
mvn clean compile
```

4. **运行应用**

```bash
mvn spring-boot:run
```

或者

```bash
mvn clean package
java -jar target/simeng-pib-1.0.0.jar
```

### 配置说明

主要配置在 `src/main/resources/application.yml` 中：

```yaml
server:
  port: 3822 # 服务端口

simeng:
  cache-dir: cache # 缓存目录
  plugin-dir: plugins # 插件目录
  simeng-dir: SimEngPI # 仿真引擎目录
  python-scripts: # Python脚本路径
    mapmaker: map_convert/mapmaker.py
    mapmaker-new: map_convert/mapmaker_new.py
    osmtrans: map_convert/osmtrans.py
```

## 📁 项目结构
待更新
```
src/main/java/com/simeng/pib/
├── SimEngPibApplication.java          # 启动类
├── config/                            # 配置类
│   ├── ApplicationStartupConfig.java  # 启动初始化
│   ├── WebConfig.java                 # Web配置
│   └── WebSocketConfig.java           # WebSocket配置
├── controller/                        # 控制器
│   ├── AuthController.java            # 认证相关
│   ├── HomeController.java            # 首页
│   ├── MapController.java             # 地图处理
│   ├── PluginController.java          # 插件管理
│   └── SimulationController.java      # 仿真控制
├── service/                           # 服务层
│   ├── MapConversionService.java      # 地图转换
│   ├── PluginService.java             # 插件管理
│   └── SessionService.java            # 会话管理
├── websocket/                         # WebSocket处理
│   ├── EngineWebSocketHandler.java    # 引擎WebSocket
│   └── FrontendWebSocketHandler.java  # 前端WebSocket
├── model/                             # 数据模型
│   ├── SimInfo.java                   # 仿真信息
│   └── dto/                           # 数据传输对象
└── util/                              # 工具类
    ├── PythonScriptExecutor.java      # Python脚本执行器
    ├── TimeUtils.java                 # 时间工具
    └── XmlJsonConverter.java          # XML/JSON转换
```

## 🔧 API 接口

### 认证相关

- `GET /cookie_id` - 创建会话 ID
- `GET /del_id_info` - 删除会话信息

### 地图处理

- `POST /upload_map` - 上传地图文件
- `GET /get_map_json` - 获取地图 JSON 数据

### 插件管理

- `POST /upload_plugin` - 上传插件
- `GET /get_plugin_info/` - 获取插件信息
- `GET /del_plugin/` - 删除插件
- `POST /update_plugin_info` - 更新插件信息
- `GET /get_plugin_code/` - 获取插件代码

### 仿真控制

- `POST /create_simeng` - 创建仿真引擎

### WebSocket

- `WS /ws/frontend` - 前端 WebSocket 连接
- `WS /ws/exe/{exe_id}` - 引擎 WebSocket 连接

## 🐍 Python 脚本集成

本项目通过 `PythonScriptExecutor` 调用原有的 Python 地图转换脚本：

- **OSM 转 TXT**: `map_convert/osmtrans.py`
- **TXT 转 XML（旧版）**: `map_convert/mapmaker.py`
- **TXT 转 XML（新版）**: `map_convert/mapmaker_new.py`

确保 Python 环境中安装了所需依赖：

```bash
pip install numpy sympy gekko xmltodict
```

## 📂 文件目录说明

### 运行时创建的目录

- `cache/` - 用户上传文件的缓存目录
  - `cache/{session_id}/` - 每个会话的独立缓存
- `plugins/` - 插件存储目录
  - `plugins/{plugin_name}/` - 每个插件的独立目录
- `SimEngPI/` - 仿真引擎运行目录
  - `SimEngPI/{session_id}/` - 每个仿真实例的文件

### 前端文件

将前端构建文件放在项目根目录的 `frontend/` 文件夹中：

```
frontend/
├── index.html
├── favicon.ico
├── assets/
└── sim_imgs/
```

## 🔍 调试和日志

应用使用 Logback 进行日志记录，日志级别在 `application.yml` 中配置：

```yaml
logging:
  level:
    com.simeng: DEBUG
    org.springframework.web.socket: DEBUG
```

## ⚠️ 注意事项

1. **Python 脚本依赖**: 确保所有 Python 依赖都已正确安装
2. **文件权限**: 确保应用有权限创建和写入缓存目录
3. **端口冲突**: 默认端口 3822，如有冲突请修改配置
4. **WebSocket 连接**: 前端连接时需要正确的 Cookie ID
5. **仿真引擎**: 需要将仿真引擎可执行文件放在 `SimEngPI/` 目录下

## 🚦 与原 Python 版本的差异

### 相同点

- 保持了完全相同的 API 接口
- WebSocket 通信协议不变
- 文件存储结构相同
- 地图转换逻辑完全依赖原 Python 脚本

### 改进点

- 更好的类型安全和错误处理
- 结构化的代码组织
- 更完善的日志记录
- 更好的并发处理能力

## 🛠️ 开发和扩展

### 添加新的 API 接口

1. 在相应的 Controller 中添加方法
2. 如需要，在 Service 层添加业务逻辑
3. 更新相应的数据模型

### 修改 Python 脚本调用

修改 `MapConversionService` 中的脚本调用逻辑，或者在 `application.yml` 中更新脚本路径。

### 插件系统扩展

当前插件系统保持了与原版的兼容性，如需要支持 Java 插件，可以扩展 `PluginService`。

## 📝 许可证

与原项目保持相同的许可证。
