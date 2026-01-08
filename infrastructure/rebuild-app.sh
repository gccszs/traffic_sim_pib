#!/bin/bash
# ============================================
# 重新构建并启动应用服务脚本
# 用于代码更新后重新部署
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$SCRIPT_DIR"

echo "============================================"
echo "Traffic Simulation - 重新构建应用"
echo "============================================"

# 步骤 1: Maven 打包
echo ""
echo "[1/3] Maven 打包..."
cd "$PROJECT_ROOT"
mvn clean package -DskipTests -q
echo "Maven 打包完成 ✓"

# 步骤 2: 停止旧的应用服务
echo ""
echo "[2/3] 停止旧的应用服务..."
cd "$SCRIPT_DIR"
docker-compose -f docker-compose.app.simple.yml down 2>/dev/null || true

# 步骤 3: 重新构建并启动
echo ""
echo "[3/3] 重新构建并启动应用服务..."
docker-compose -f docker-compose.app.simple.yml up -d --build

echo ""
echo "============================================"
echo "应用服务已重新部署！"
echo "============================================"
echo ""
echo "查看日志: docker logs -f traffic-sim-server"
echo ""

