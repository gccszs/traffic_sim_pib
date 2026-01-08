#!/bin/bash
# ============================================
# gRPC 服务测试脚本
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  gRPC Service Test Runner${NC}"
echo -e "${BLUE}============================================${NC}"

# 检查 Python 环境
check_python() {
    echo -e "\n${YELLOW}[1/4] Checking Python environment...${NC}"
    
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}Error: Python3 not found${NC}"
        exit 1
    fi
    
    # 检查依赖
    python3 -c "import grpc" 2>/dev/null || {
        echo -e "${YELLOW}Installing grpcio...${NC}"
        pip3 install grpcio grpcio-tools
    }
    
    echo -e "${GREEN}Python environment OK${NC}"
}

# 启动地图转换服务
start_map_service() {
    echo -e "\n${YELLOW}[2/4] Starting Map Convert Service...${NC}"
    
    cd "$PROJECT_ROOT/map_convert_services"
    
    # 检查服务是否已在运行
    if lsof -i:50052 &>/dev/null; then
        echo -e "${GREEN}Map Convert Service already running on port 50052${NC}"
    else
        echo -e "${BLUE}Starting gRPC server on port 50052...${NC}"
        python3 grpc_server.py &
        MAP_PID=$!
        sleep 3
        
        if kill -0 $MAP_PID 2>/dev/null; then
            echo -e "${GREEN}Map Convert Service started (PID: $MAP_PID)${NC}"
        else
            echo -e "${RED}Failed to start Map Convert Service${NC}"
            exit 1
        fi
    fi
}

# 运行测试
run_tests() {
    echo -e "\n${YELLOW}[3/4] Running tests...${NC}"
    
    cd "$SCRIPT_DIR"
    
    # 设置 Python 路径
    export PYTHONPATH="$PROJECT_ROOT/map_convert_services:$PYTHONPATH"
    
    # 运行测试
    python3 test_grpc_services.py \
        --map-host localhost \
        --map-port 50052 \
        --sim-host localhost \
        --sim-port 50051 \
        --timeout 30
    
    TEST_RESULT=$?
    
    return $TEST_RESULT
}

# 清理
cleanup() {
    echo -e "\n${YELLOW}[4/4] Cleanup...${NC}"
    
    # 停止服务（可选）
    # kill $MAP_PID 2>/dev/null || true
    
    echo -e "${GREEN}Done${NC}"
}

# 主流程
main() {
    check_python
    start_map_service
    run_tests
    TEST_RESULT=$?
    cleanup
    
    if [ $TEST_RESULT -eq 0 ]; then
        echo -e "\n${GREEN}============================================${NC}"
        echo -e "${GREEN}  All tests passed!${NC}"
        echo -e "${GREEN}============================================${NC}"
    else
        echo -e "\n${RED}============================================${NC}"
        echo -e "${RED}  Some tests failed!${NC}"
        echo -e "${RED}============================================${NC}"
    fi
    
    exit $TEST_RESULT
}

# 处理参数
case "${1:-}" in
    --map-only)
        check_python
        start_map_service
        cd "$SCRIPT_DIR"
        export PYTHONPATH="$PROJECT_ROOT/map_convert_services:$PYTHONPATH"
        python3 test_grpc_services.py --map-host localhost --map-port 50052 --sim-host localhost --sim-port 50051
        ;;
    --help)
        echo "Usage: $0 [--map-only|--help]"
        echo ""
        echo "Options:"
        echo "  --map-only  Only test map convert service"
        echo "  --help      Show this help"
        ;;
    *)
        main
        ;;
esac

