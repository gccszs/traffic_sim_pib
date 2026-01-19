# ============================================================================
# Python 引擎服务 Dockerfile
# ============================================================================
# 
# 【服务说明】
# 本镜像包含 Python 引擎服务，提供以下功能：
# 1. gRPC 服务:
#    - MapConvertService (端口 50052) - 地图文件转换和预览
#    - PythonService (端口 50051) - 仿真引擎管理
# 2. HTTP/WebSocket 服务 (端口 8000):
#    - WebSocket /ws/exe/{exe_id} - 引擎和 Java 后端通信
#    - HTTP API - 文件上传、插件管理等
#
# 【部署架构】
# 
# ┌─────────────────────────────────────────────────────────────────────┐
# │                         部署选项                                     │
# ├─────────────────────────────────────────────────────────────────────┤
# │                                                                     │
# │  选项1: 同主机部署 (开发/测试环境)                                    │
# │  ┌──────────────┐      localhost       ┌──────────────┐            │
# │  │ Java 主服务   │ ─────────────────>  │ Python 引擎   │            │
# │  │ (port 3822)  │   50051/50052/8000   │ (本镜像)      │            │
# │  └──────────────┘                      └──────────────┘            │
# │                                                                     │
# │  选项2: Docker 网络部署 (推荐生产环境)                                │
# │  ┌──────────────┐   docker network     ┌──────────────┐            │
# │  │ Java 主服务   │ ────────────────>   │ Python 引擎   │            │
# │  │ traffic-sim  │  python-engine:port  │ python-engine │            │
# │  └──────────────┘                      └──────────────┘            │
# │                                                                     │
# │  选项3: 分布式部署 (生产环境/高可用)                                  │
# │  ┌──────────────┐       网络           ┌──────────────┐            │
# │  │ Java 主服务   │ ────────────────>   │ Python 引擎   │            │
# │  │ 主机 A        │   IP:50051/50052    │ 主机 B        │            │
# │  └──────────────┘                      └──────────────┘            │
# │                                                                     │
# └─────────────────────────────────────────────────────────────────────┘
#
# 【构建命令】
# docker build -t traffic-sim-python-engine:latest .
#
# 【运行命令 - 示例】
# 
# 1. 基本运行:
# docker run -d --name python-engine \
#   -p 8000:8000 -p 50051:50051 -p 50052:50052 \
#   traffic-sim-python-engine:latest
#
# 2. 指定环境变量:
# docker run -d --name python-engine \
#   -e APP_HOST=0.0.0.0 \
#   -e APP_PORT=8000 \
#   -e SIM_GRPC_PORT=50051 \
#   -e MAP_GRPC_PORT=50052 \
#   -e CLIENT_SOCKET_IP=192.168.1.100 \
#   -p 8000:8000 -p 50051:50051 -p 50052:50052 \
#   traffic-sim-python-engine:latest
#
# 3. 使用 Docker Compose (推荐):
# 参见 docker-compose.python-engine.yml
#
# 【环境变量说明】
# APP_HOST           - HTTP/WebSocket 监听地址 (默认: 0.0.0.0)
# APP_PORT           - HTTP/WebSocket 端口 (默认: 8000)
# SIM_GRPC_PORT      - 仿真服务 gRPC 端口 (默认: 50051)
# MAP_GRPC_PORT      - 地图服务 gRPC 端口 (默认: 50052)
# CLIENT_SOCKET_IP   - 仿真引擎连接的客户端 IP
# LOG_HOME           - 日志目录 (默认: /app/engine_sim_logs/)
#
# 【端口映射】
# 8000  - HTTP/WebSocket 服务 (FastAPI)
# 50051 - gRPC 仿真服务 (PythonService)
# 50052 - gRPC 地图服务 (MapConvertService)
#
# 【持久化目录】
# /data/logs   - 仿真日志
# /data/config - 配置文件
#
# ============================================================================

FROM python:3.9-slim

# ============================================================================
# 安装系统依赖
# Wine 用于运行 Windows 仿真引擎 (SimulationEngine.exe)
# ============================================================================
RUN dpkg --add-architecture i386 && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
        wine \
        wine32 \
        wine64 \
        libwine \
        libwine:i386 \
        xvfb && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 拷贝项目文件
COPY . .

# 设置 Wine 环境变量
ENV WINEPREFIX=/root/.wine
ENV WINEARCH=win64
ENV WINEDEBUG=-all
ENV DISPLAY=:99

# 简化 Wine 初始化
RUN echo "Testing Wine installation..." && \
    wine --version && \
    echo "Wine installed successfully"

# 设置可执行权限
RUN chmod +x /app/SimEngPI/SimulationEngine.exe

# 安装 Python 依赖
RUN pip3 install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple/ --upgrade pip && \
    pip3 install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple/ -r requirements.txt

# ============================================================================
# 环境变量配置
# 
# 【运维人员注意】
# 这些变量可以在运行时通过 -e 参数覆盖
# 例如: docker run -e APP_PORT=9000 -e SIM_GRPC_PORT=60051 ...
# ============================================================================

# HTTP/WebSocket 服务配置
ARG APP_HOST=0.0.0.0
ARG APP_PORT=8000
ARG CLIENT_SOCKET_IP=192.168.1.212
ARG LOG_HOME=/app/engine_sim_logs/

# gRPC 服务端口配置
# SIM_GRPC_PORT: 仿真服务端口，Java 端 SimulationPythonGrpcClient 连接此端口
# MAP_GRPC_PORT: 地图服务端口，Java 端 PythonGrpcClient (plugin-map) 连接此端口
ARG MAP_GRPC_PORT=50052
ARG SIM_GRPC_PORT=50051

# 将 ARG 转换为 ENV
ENV APP_HOST=$APP_HOST
ENV APP_PORT=$APP_PORT
ENV CLIENT_SOCKET_IP=$CLIENT_SOCKET_IP
ENV LOG_HOME=$LOG_HOME
ENV MAP_GRPC_PORT=$MAP_GRPC_PORT
ENV SIM_GRPC_PORT=$SIM_GRPC_PORT

# 数据目录
ENV SIM_LOG_DIR=/data/logs
ENV SIM_CONFIG_DIR=/data/config

# 创建并挂载持久化目录
RUN mkdir -p /data/logs /data/config /app/engine_sim_logs /app/cache
VOLUME ["/data/logs", "/data/config"]

# ============================================================================
# 暴露端口
# 
# 【运维人员注意】
# 根据部署架构选择需要映射的端口:
# - 如果 Java 和 Python 在同一 Docker 网络，无需映射端口
# - 如果分布式部署，需要映射相应端口
# ============================================================================

# HTTP/WebSocket 端口 - FastAPI 服务
# 用途: WebSocket 连接 (/ws/exe/{exe_id}), HTTP API
EXPOSE ${APP_PORT}

# gRPC 端口 - 地图转换服务 (MapConvertService)
# Java 客户端: plugin-map 的 PythonGrpcClient
# 方法: ConvertMap, PreviewMap
EXPOSE 50052

# gRPC 端口 - 仿真引擎服务 (PythonService)
# Java 客户端: plugin-simulation 的 SimulationPythonGrpcClient
# 方法: CreateSimeng, ControlGreenRatio, TestConnection
EXPOSE 50051

# 安装 gRPC 依赖
RUN pip3 install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple/ grpcio grpcio-tools protobuf

# 启动脚本
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

# ============================================================================
# 容器启动
# start.sh 会启动以下服务:
# 1. FastAPI (web_app.py) - HTTP/WebSocket 服务
# 2. gRPC (grpc_server.py) - gRPC 服务 (包含 MapConvertService 和 PythonService)
# ============================================================================
CMD ["/app/start.sh"]

# ============================================================================
# 健康检查 (可选)
# 如需启用，取消下面的注释
# ============================================================================
# HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
#   CMD curl -f http://localhost:${APP_PORT}/test || exit 1
