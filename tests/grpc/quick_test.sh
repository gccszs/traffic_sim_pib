#!/bin/bash
# ============================================
# 快速 gRPC 测试 - 仅测试地图转换服务
# ============================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo "============================================"
echo "  Quick gRPC Test - Map Convert Service"
echo "============================================"

# 检查地图服务是否运行
echo -e "\n[1/3] Checking if Map Service is running..."
if ! nc -z localhost 50052 2>/dev/null; then
    echo "Map Service not running, starting..."
    cd "$PROJECT_ROOT/map_convert_services"
    python3 grpc_server.py &
    sleep 3
fi

# 运行简单测试
echo -e "\n[2/3] Running quick test..."
cd "$SCRIPT_DIR"
export PYTHONPATH="$PROJECT_ROOT/map_convert_services:$PYTHONPATH"

python3 -c "
import sys
sys.path.insert(0, '$PROJECT_ROOT/map_convert_services')

import grpc
from proto import map_service_pb2, map_service_pb2_grpc

print('Connecting to Map Service...')
channel = grpc.insecure_channel('localhost:50052')
stub = map_service_pb2_grpc.MapConvertServiceStub(channel)

# 测试预览
print('Testing PreviewMap...')
request = map_service_pb2.PreviewMapRequest(
    file_content=b'NODE 1 0 0\\nROAD 1 1 2',
    file_name='test.txt',
    user_id='quick_test'
)

try:
    response = stub.PreviewMap(request, timeout=10)
    if response.success:
        print(f'✓ PreviewMap successful')
        print(f'  Roads: {response.road_count}, Intersections: {response.intersection_count}')
    else:
        print(f'✗ PreviewMap failed: {response.message}')
except Exception as e:
    print(f'✗ Error: {e}')

channel.close()
print('\\nQuick test completed!')
"

echo -e "\n[3/3] Done"

