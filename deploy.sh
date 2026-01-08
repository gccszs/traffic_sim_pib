#!/bin/bash
# ============================================
# Traffic Simulation - 一键部署脚本
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}"
echo "============================================"
echo "  Traffic Simulation - 一键部署"
echo "============================================"
echo -e "${NC}"

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi

# 检查是否需要重新打包
if [ "$1" = "--rebuild" ] || [ "$1" = "-r" ]; then
    echo -e "${YELLOW}[1/3] Maven 重新打包...${NC}"
    mvn clean package -DskipTests -q
    echo -e "${GREEN}Maven 打包完成 ✓${NC}"
else
    # 检查 jar 文件是否存在
    if [ ! -f "traffic-sim-server/target/traffic-sim-server-1.0.0-SNAPSHOT.jar" ]; then
        echo -e "${YELLOW}[1/3] 未找到 jar 文件，执行 Maven 打包...${NC}"
        mvn clean package -DskipTests -q
        echo -e "${GREEN}Maven 打包完成 ✓${NC}"
    else
        echo -e "${GREEN}[1/3] 使用已存在的 jar 文件 ✓${NC}"
    fi
fi

# 停止旧服务
echo -e "${YELLOW}[2/3] 停止旧服务...${NC}"
docker compose -f docker-compose.full.yml down 2>/dev/null || true

# 启动所有服务
echo -e "${YELLOW}[3/3] 启动所有服务...${NC}"
docker compose -f docker-compose.full.yml up -d --build

# 等待服务启动
echo ""
echo -e "${YELLOW}等待服务启动...${NC}"
sleep 10

# 显示服务状态
echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
docker compose -f docker-compose.full.yml ps
echo ""
echo "服务访问地址:"
echo "  - Java API:     http://localhost:3822/api"
echo "  - Swagger UI:   http://localhost:3822/api/swagger-ui.html"
echo "  - Python API:   http://localhost:8000"
echo ""
echo "常用命令:"
echo "  查看日志:     docker-compose -f docker-compose.full.yml logs -f"
echo "  停止服务:     docker-compose -f docker-compose.full.yml down"
echo "  重新部署:     ./deploy.sh --rebuild"
echo ""

