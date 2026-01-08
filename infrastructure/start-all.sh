#!/bin/bash
# ============================================
# 启动所有服务脚本
# 包括基础设施和应用服务
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================"
echo "Traffic Simulation - 启动所有服务"
echo "============================================"

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "错误: Docker 未运行，请先启动 Docker"
    exit 1
fi

# 步骤 1: 启动基础设施
echo ""
echo "[1/3] 启动基础设施服务 (MySQL, MongoDB, Redis, Kafka)..."
docker-compose up -d

# 等待基础设施就绪
echo ""
echo "[2/3] 等待基础设施服务就绪..."
sleep 10

# 检查 MySQL 健康状态
echo "等待 MySQL 就绪..."
until docker exec traffic-sim-mysql mysqladmin ping -h localhost -u root -proot --silent 2>/dev/null; do
    echo "MySQL 正在启动..."
    sleep 5
done
echo "MySQL 已就绪 ✓"

# 检查 MongoDB 健康状态
echo "等待 MongoDB 就绪..."
until docker exec traffic-sim-mongodb mongosh --eval "db.adminCommand('ping')" --quiet 2>/dev/null; do
    echo "MongoDB 正在启动..."
    sleep 5
done
echo "MongoDB 已就绪 ✓"

# 步骤 3: 启动应用服务
echo ""
echo "[3/3] 启动应用服务 (Java Server, Python Service)..."
docker-compose -f docker-compose.app.simple.yml up -d --build

echo ""
echo "============================================"
echo "所有服务已启动！"
echo "============================================"
echo ""
echo "服务访问地址:"
echo "  - Java API:     http://localhost:3822/api"
echo "  - Swagger UI:   http://localhost:3822/api/swagger-ui.html"
echo "  - Python API:   http://localhost:8000"
echo "  - Kafka UI:     http://localhost:8081"
echo ""
echo "查看日志:"
echo "  - Java 服务:   docker logs -f traffic-sim-server"
echo "  - Python 服务: docker logs -f traffic-sim-python"
echo ""

