#!/bin/bash
# ============================================
# 停止所有服务脚本
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================"
echo "Traffic Simulation - 停止所有服务"
echo "============================================"

# 停止应用服务
echo ""
echo "[1/2] 停止应用服务..."
docker-compose -f docker-compose.app.simple.yml down 2>/dev/null || true

# 停止基础设施
echo ""
echo "[2/2] 停止基础设施服务..."
docker-compose down

echo ""
echo "============================================"
echo "所有服务已停止！"
echo "============================================"

