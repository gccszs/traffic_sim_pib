# Python 服务配置说明

## 环境变量配置

请在 `map_convert_services` 目录下创建 `.env` 文件，或者在启动脚本中设置以下环境变量：

```bash
# FastAPI HTTP/WebSocket 服务配置
APP_HOST=localhost
APP_PORT=8000

# Java 后端 WebSocket 配置（引擎连接目标）
# 引擎将直接连接到 Java 后端的 WebSocket，而不是 Python 服务
BACKEND_HOST=localhost
BACKEND_PORT=3822
BACKEND_WS_PATH=

# gRPC 服务端口配置
# 地图转换服务端口 - 对应 Java 端 plugin-map 的 MapPythonGrpcClient
MAP_GRPC_PORT=50052
# 仿真服务端口 - 对应 Java 端 plugin-simulation 的 SimulationPythonGrpcClient
SIM_GRPC_PORT=50051

# 日志配置
LOG_HOME=./engine_sim_logs/
```

## 架构说明

### 修复后的架构流程：

1. **Java 后端** 通过 gRPC (50051端口) 调用 Python 的 `simulation_service.py`
2. **Python 服务** 启动仿真引擎，引擎直接连接到 **Java 后端的 WebSocket** (3822端口)
3. **引擎** 通过 WebSocket 发送仿真数据到 Java 后端的 `EngineWebSocketHandler`
4. **Java 后端** 接收引擎数据，进行统计处理，然后转发给前端

### WebSocket 连接路径：

- **引擎 → Java 后端**: `ws://localhost:3822/ws/exe/{sessionId}`
- **前端 → Java 后端**: `ws://localhost:3822/ws/frontend/{sessionId}`

### 端口说明：

- **8000**: Python FastAPI HTTP 服务（保留，用于旧版兼容或其他 HTTP 接口）
- **3822**: Java 后端 WebSocket 服务（引擎和前端连接）
- **50051**: Python gRPC 仿真服务（Java 调用创建引擎）
- **50052**: Python gRPC 地图转换服务（Java 调用转换地图）

## 问题修复说明

### 原问题：

引擎启动时尝试连接 Python 的 8000 端口 WebSocket，但该端口没有正确的 WebSocket 处理逻辑，导致连接被拒绝。

### 修复方案：

修改了 `simulation_service.py` 和 `web_app.py` 中的引擎启动命令，让引擎直接连接到 Java 后端的 WebSocket (3822端口)，恢复了原有的架构设计。

### 修改的文件：

1. `simulation_service.py` - 修改引擎启动命令，使用 `settings.backend_host` 和 `settings.backend_port`
2. `web_app.py` - 同样修改引擎启动命令
3. `config.py` - 已有正确的配置项

## 启动顺序

1. 启动 Java 后端服务（端口 3822）
2. 启动 Python 服务（运行 `start.sh` 或 `python grpc_server.py`）
3. 通过 Java 后端 API 创建仿真任务
4. 引擎自动连接到 Java 后端 WebSocket

## 验证连接

启动后，检查日志：

- **Python 日志**: 应该看到 "Simulation command: ..." 包含 `--ip=localhost --port=3822 --ws=ws://localhost:3822/ws/exe/...`
- **Java 日志**: 应该看到 "Engine WebSocket connected: {sessionId}"
- **引擎日志**: 应该看到成功连接到 WebSocket 的消息
