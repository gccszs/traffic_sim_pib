# WebSocket 连接问题修复报告

## 问题描述

**错误信息**：
```
[21:04:23] [STDERR] [2026-01-19 21:04:23|0x0000971c][error]
[!] Error in connect: The WebSocket handshake was declined by the remote peer
[boost.beast.websocket:20 at D:\Works\GraduationDesign\SimEngPI\vcpkg_installed\x64-windows\x64-windows\include\boost\beast\websocket\impl\stream_impl.hpp:615 in function 'auto __cdecl boost::beast::websocket::<lambda_1>::operator ()(enum boost::beast::websocket::error) const']
[21:04:23] [STDERR] [2026-01-19 21:04:23|0x0000971c][fatal]
[!] Connect websocket to backend failed!
```

**问题根源**：
引擎尝试连接到 Python 服务的 8000 端口 WebSocket，但该端口没有正确的 WebSocket 处理逻辑，导致握手被拒绝。

## 架构分析

### 原始架构（没有 gRPC 时）：
```
前端 ←→ Java 后端 (OpenFeign) ←→ Python web_app.py (HTTP API)
                                        ↓
                                   启动引擎
                                        ↓
                                   引擎 WebSocket ←→ Python web_app.py
```

### 当前错误架构（引入 gRPC 后）：
```
前端 ←→ Java 后端 (gRPC) ←→ Python simulation_service.py
                                   ↓
                              启动引擎
                                   ↓
                              引擎尝试连接 Python 8000 端口 ❌ (失败)
```

### 正确架构（修复后）：
```
前端 ←→ Java 后端 (WebSocket 3822) ←→ 引擎
         ↓ (gRPC 50051)
    Python simulation_service.py
         ↓
    启动引擎（配置连接到 Java 后端 3822）
```

## 修复方案

### 1. 修改 `simulation_service.py`

**位置**：`map_convert_services/simulation_service.py` 第 230-255 行

**修改前**：
```python
# 引擎连接 Python 服务的 WebSocket，Python 服务充当中介
backend_host = "localhost"  # Python 服务本地地址
backend_port = settings.port  # Python 服务端口 (8000)

sim_cmd = [
    './SimEngPI/SimulationEngine.exe',
    '--log=0',
    arg_sid,
    arg_simfile,
    arg_roadfile,
    f'--ip={backend_host}',
    f'--port={backend_port}',  # Python 服务端口
    arg_plugin
]
```

**修改后**：
```python
# 引擎直接连接 Java 后端的 WebSocket（不再通过 Python 中介）
backend_host = settings.backend_host  # Java 后端地址
backend_port = settings.backend_port  # Java 后端 WebSocket 端口 (3822)

# 构建 WebSocket URL：ws://backend_host:backend_port/ws/exe/{user_id}
ws_path = f"{settings.backend_ws_path}/ws/exe/{user_id}".lstrip('/')
ws_url = f"ws://{backend_host}:{backend_port}/{ws_path}"

sim_cmd = [
    './SimEngPI/SimulationEngine.exe',
    '--log=0',
    arg_sid,
    arg_simfile,
    arg_roadfile,
    f'--ip={backend_host}',
    f'--port={backend_port}',  # Java 后端端口
    f'--ws={ws_url}',  # 显式指定 WebSocket URL
    arg_plugin
]
```

### 2. 修改 `web_app.py`

**位置**：`map_convert_services/web_app.py` 第 220-230 行

**修改前**：
```python
# 引擎连接 Python 服务的 WebSocket，Python 服务充当中介
ws_url = f"ws://localhost:{settings.port}/ws/exe/" + user_id
sim_cmd = ['./SimEngPI/SimulationEngine.exe', '--log=0', arg_sid, arg_simfile, arg_roadfile, '--ip=localhost',
           f'--port={settings.port}', '--ws=' + ws_url, arg_plugin]
```

**修改后**：
```python
# 引擎直接连接 Java 后端的 WebSocket（不再通过 Python 中介）
backend_host = settings.backend_host
backend_port = settings.backend_port

ws_path = f"{settings.backend_ws_path}/ws/exe/{user_id}".lstrip('/')
ws_url = f"ws://{backend_host}:{backend_port}/{ws_path}"

sim_cmd = ['./SimEngPI/SimulationEngine.exe', '--log=0', arg_sid, arg_simfile, arg_roadfile, 
           f'--ip={backend_host}', f'--port={backend_port}', '--ws=' + ws_url, arg_plugin]
```

### 3. 配置文件说明

**位置**：`map_convert_services/config.py`

已有正确的配置项：
```python
# Java 后端配置 - 仿真引擎连接后端 WebSocket
backend_host: str = Field(default="localhost", env="BACKEND_HOST")
backend_port: int = Field(default=3822, env="BACKEND_PORT")
backend_ws_path: str = Field(default="", env="BACKEND_WS_PATH")
```

### 4. 环境变量配置

创建 `.env` 文件（或在启动脚本中设置）：
```bash
# Java 后端 WebSocket 配置（引擎连接目标）
BACKEND_HOST=localhost
BACKEND_PORT=3822
BACKEND_WS_PATH=
```

## 端口说明

| 端口 | 服务 | 用途 |
|------|------|------|
| 3822 | Java 后端 | WebSocket 服务（引擎和前端连接） |
| 8000 | Python FastAPI | HTTP 服务（保留，用于旧版兼容） |
| 50051 | Python gRPC | 仿真服务（Java 调用创建引擎） |
| 50052 | Python gRPC | 地图转换服务（Java 调用转换地图） |

## WebSocket 连接路径

- **引擎 → Java 后端**: `ws://localhost:3822/ws/exe/{sessionId}`
- **前端 → Java 后端**: `ws://localhost:3822/ws/frontend/{sessionId}`

## 数据流向

### 仿真创建流程：
1. 前端 → Java 后端：创建仿真请求
2. Java 后端 → Python gRPC (50051)：调用 `CreateSimeng`
3. Python 服务：启动引擎进程，配置连接到 Java 后端 3822
4. 引擎 → Java 后端 (3822)：建立 WebSocket 连接
5. Java 后端：确认引擎连接成功

### 仿真数据流：
1. 引擎 → Java 后端 (WebSocket 3822)：发送仿真数据
2. Java 后端：接收数据，调用统计服务处理
3. Java 后端 → 前端 (WebSocket 3822)：转发处理后的数据

## 验证步骤

### 1. 启动服务

**启动 Java 后端**：
```bash
cd traffic-sim-server
mvn spring-boot:run
```

**启动 Python 服务**（Windows）：
```bash
cd map_convert_services
start.bat
```

**启动 Python 服务**（Linux/Mac）：
```bash
cd map_convert_services
bash start.sh
```

### 2. 检查日志

**Python 日志**（应该看到）：
```
Simulation command: ./SimEngPI/SimulationEngine.exe --log=0 --sid=xxx --sfile=xxx --road=xxx.xml --ip=localhost --port=3822 --ws=ws://localhost:3822/ws/exe/xxx --noplugin
WebSocket URL: ws://localhost:3822/ws/exe/xxx
```

**Java 日志**（应该看到）：
```
Engine WebSocket connected: {sessionId}
Engine initialized for session: {sessionId}
```

**引擎日志**（应该看到）：
```
[+] Connected to backend successfully
[+] WebSocket handshake completed
```

### 3. 测试仿真

1. 通过前端或 API 创建仿真任务
2. 观察 Java 后端日志，应该看到引擎连接成功
3. 启动仿真，应该能看到实时数据输出
4. 前端应该能接收到仿真数据

## 常见问题

### Q1: 引擎仍然连接失败？
**A**: 检查以下几点：
- Java 后端是否已启动（端口 3822）
- 防火墙是否阻止了 3822 端口
- 检查 Python 日志中的引擎启动命令是否包含正确的 `--port=3822`

### Q2: Python 服务启动失败？
**A**: 检查：
- 是否安装了所有依赖：`pip install -r requirements.txt`
- 端口 50051 和 50052 是否被占用
- 检查日志文件：`service_logs/grpc_*.log`

### Q3: 前端收不到仿真数据？
**A**: 检查：
- 前端 WebSocket 是否连接到 `ws://localhost:3822/ws/frontend/{sessionId}`
- Java 后端日志中是否有 "Frontend WebSocket connected" 消息
- 引擎是否成功连接（检查 Java 日志）

## 总结

此次修复恢复了原有的架构设计：
- **Java 后端**作为 WebSocket 中心，负责接收引擎数据和转发给前端
- **Python 服务**通过 gRPC 接收创建引擎的请求，启动引擎后让引擎直接连接 Java 后端
- **引擎**直接与 Java 后端通信，不再通过 Python 中介

这样的架构更加清晰，职责分明，避免了 Python 服务作为中介带来的复杂性和潜在问题。

## 修改文件清单

1. ✅ `map_convert_services/simulation_service.py` - 修改引擎启动命令
2. ✅ `map_convert_services/web_app.py` - 修改引擎启动命令
3. ✅ `map_convert_services/CONFIG.md` - 新增配置说明文档
4. ✅ `map_convert_services/start.bat` - 新增 Windows 启动脚本
5. ✅ `map_convert_services/WEBSOCKET_FIX.md` - 本文档

## 下一步

1. 重启 Python 服务
2. 重启 Java 后端（如果已启动）
3. 测试创建仿真任务
4. 验证引擎连接和数据流
