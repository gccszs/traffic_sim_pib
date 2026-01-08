#!/bin/bash
# ============================================
# Python Services Startup Script
# Start FastAPI (HTTP) and gRPC services
# ============================================

set -e

# 日志目录
LOG_DIR="service_logs"
mkdir -p "$LOG_DIR"

echo "========================================="
echo "Starting Traffic Simulation Python Services..."
echo "========================================="

# 日志文件路径（带时间戳）
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FASTAPI_LOG="$LOG_DIR/fastapi_${TIMESTAMP}.log"
GRPC_LOG="$LOG_DIR/grpc_${TIMESTAMP}.log"
MAIN_LOG="$LOG_DIR/main_${TIMESTAMP}.log"

# 记录启动信息
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting services..." | tee -a "$MAIN_LOG"
echo "FastAPI log: $FASTAPI_LOG" | tee -a "$MAIN_LOG"
echo "gRPC log: $GRPC_LOG" | tee -a "$MAIN_LOG"
echo "=========================================" | tee -a "$MAIN_LOG"

# Start FastAPI service (background)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting FastAPI HTTP service on port ${APP_PORT:-8000}..." | tee -a "$MAIN_LOG"
python web_app.py > "$FASTAPI_LOG" 2>&1 &
FASTAPI_PID=$!

# Wait for FastAPI to start
sleep 3

# 检查 FastAPI 是否仍在运行
if ! kill -0 $FASTAPI_PID 2>/dev/null; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: FastAPI failed to start!" | tee -a "$MAIN_LOG"
    echo "Check log file: $FASTAPI_LOG" | tee -a "$MAIN_LOG"
    cat "$FASTAPI_LOG"
    echo "按任意键继续..."
    read -n 1 -s
    exit 1
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] FastAPI started successfully (PID: $FASTAPI_PID)" | tee -a "$MAIN_LOG"

# Check if gRPC service file exists
if [ -f "grpc_server.py" ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting gRPC service on port ${GRPC_PORT:-50052}..." | tee -a "$MAIN_LOG"
    python grpc_server.py > "$GRPC_LOG" 2>&1 &
    GRPC_PID=$!

    # Wait for gRPC to start
    sleep 3

    # 检查 gRPC 是否仍在运行
    if ! kill -0 $GRPC_PID 2>/dev/null; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: gRPC failed to start!" | tee -a "$MAIN_LOG"
        echo "Check log file: $GRPC_LOG" | tee -a "$MAIN_LOG"
        cat "$GRPC_LOG"
        kill $FASTAPI_PID 2>/dev/null || true
        echo "按任意键继续..."
        read -n 1 -s
        exit 1
    fi

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] gRPC started successfully (PID: $GRPC_PID)" | tee -a "$MAIN_LOG"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] WARNING: gRPC server not found, skipping..." | tee -a "$MAIN_LOG"
fi

echo "=========================================" | tee -a "$MAIN_LOG"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Services started successfully!" | tee -a "$MAIN_LOG"
echo "  - FastAPI PID: $FASTAPI_PID (log: $FASTAPI_LOG)" | tee -a "$MAIN_LOG"
[ -n "$GRPC_PID" ] && echo "  - gRPC PID: $GRPC_PID (log: $GRPC_LOG)" | tee -a "$MAIN_LOG"
echo "  - Main log: $MAIN_LOG" | tee -a "$MAIN_LOG"
echo "=========================================" | tee -a "$MAIN_LOG"
echo "Monitoring services. Press Ctrl+C to stop..." | tee -a "$MAIN_LOG"

# 设置陷阱函数，在脚本退出时清理进程
cleanup() {
    echo ""
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Shutting down services..." | tee -a "$MAIN_LOG"
    if [ -n "$FASTAPI_PID" ]; then
        echo "Stopping FastAPI (PID: $FASTAPI_PID)..." | tee -a "$MAIN_LOG"
        kill $FASTAPI_PID 2>/dev/null || true
    fi
    if [ -n "$GRPC_PID" ]; then
        echo "Stopping gRPC (PID: $GRPC_PID)..." | tee -a "$MAIN_LOG"
        kill $GRPC_PID 2>/dev/null || true
    fi
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] All services stopped." | tee -a "$MAIN_LOG"
    echo "按任意键继续..."
    read -n 1 -s
    exit 0
}

# 捕获 Ctrl+C 和进程退出信号
trap cleanup SIGINT SIGTERM

# Wait for any process to exit
wait

# 记录哪个服务崩溃了
echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: A service has stopped unexpectedly!" | tee -a "$MAIN_LOG"

# Check which process exited
if ! kill -0 $FASTAPI_PID 2>/dev/null; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] FastAPI service crashed!" | tee -a "$MAIN_LOG"
    echo "Last 50 lines of FastAPI log:" | tee -a "$MAIN_LOG"
    tail -n 50 "$FASTAPI_LOG" | tee -a "$MAIN_LOG"
fi

if [ -n "$GRPC_PID" ] && ! kill -0 $GRPC_PID 2>/dev/null; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] gRPC service crashed!" | tee -a "$MAIN_LOG"
    echo "Last 50 lines of gRPC log:" | tee -a "$MAIN_LOG"
    tail -n 50 "$GRPC_LOG" | tee -a "$MAIN_LOG"
fi

# If any process exits, terminate all processes
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Terminating all services..." | tee -a "$MAIN_LOG"
kill $FASTAPI_PID 2>/dev/null || true
[ -n "$GRPC_PID" ] && kill $GRPC_PID 2>/dev/null || true
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Shutdown complete." | tee -a "$MAIN_LOG"
echo "按任意键继续..."
read -n 1 -s
exit 1
