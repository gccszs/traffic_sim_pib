#!/bin/bash
# ============================================
# Python 服务启动脚本
# 启动 FastAPI (HTTP) 和 gRPC 服务
# ============================================

set -e

echo "Starting Traffic Simulation Python Services..."

# 启动 FastAPI 服务（后台运行）
echo "Starting FastAPI HTTP service on port ${APP_PORT:-8000}..."
python3 web_app.py &
FASTAPI_PID=$!

# 等待 FastAPI 启动
sleep 3

# 检查 gRPC 服务文件是否存在
if [ -f "grpc_server.py" ]; then
    echo "Starting gRPC service on port ${GRPC_PORT:-50052}..."
    python3 grpc_server.py &
    GRPC_PID=$!
else
    echo "gRPC server not found, skipping..."
fi

echo "Services started successfully!"
echo "  - FastAPI PID: $FASTAPI_PID"
[ -n "$GRPC_PID" ] && echo "  - gRPC PID: $GRPC_PID"

# 等待任意进程退出
wait -n

# 如果有进程退出，终止所有进程
echo "A service has stopped, shutting down..."
kill $FASTAPI_PID 2>/dev/null || true
[ -n "$GRPC_PID" ] && kill $GRPC_PID 2>/dev/null || true

exit 1

