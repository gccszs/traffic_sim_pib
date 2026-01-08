# gRPC 服务测试指南

## 概述

本测试套件用于测试交通仿真系统的 gRPC 服务，包括：

1. **地图转换服务** (MapConvertService) - 端口 50052
   - `ConvertMap`: 将 OSM/TXT 地图文件转换为 XML 格式
   - `PreviewMap`: 预览地图文件，获取道路和交叉口统计信息

2. **仿真引擎服务** (PythonService) - 端口 50051
   - `TestConnection`: 测试连接
   - `CreateSimeng`: 创建仿真引擎
   - `ControlGreenRatio`: 绿信比控制

## 环境准备

### 1. 安装 Python 依赖

```bash
pip3 install grpcio grpcio-tools protobuf
```

### 2. 启动服务

#### 方式一：本地启动

```bash
# 启动地图转换服务
cd /Users/huxiaochuan/IdeaProjects/traffic_sim_pib/map_convert_services
python3 grpc_server.py

# 在另一个终端启动仿真服务（如果需要）
# python3 simulation_grpc_server.py
```

#### 方式二：Docker 启动

```bash
cd /Users/huxiaochuan/IdeaProjects/traffic_sim_pib
docker-compose -f docker-compose.full.yml up -d python-service
```

## 运行测试

### 方式一：使用测试脚本（推荐）

```bash
cd /Users/huxiaochuan/IdeaProjects/traffic_sim_pib/tests/grpc
chmod +x run_tests.sh
./run_tests.sh
```

### 方式二：直接运行 Python 测试

```bash
cd /Users/huxiaochuan/IdeaProjects/traffic_sim_pib/tests/grpc

# 设置 Python 路径
export PYTHONPATH="/Users/huxiaochuan/IdeaProjects/traffic_sim_pib/map_convert_services:$PYTHONPATH"

# 运行测试
python3 test_grpc_services.py
```

### 方式三：使用 grpcurl 手动测试

```bash
# 安装 grpcurl
brew install grpcurl

# 列出服务
grpcurl -plaintext localhost:50052 list

# 测试地图转换服务
grpcurl -plaintext -d '{
  "file_content": "dGVzdCBkYXRh",
  "file_name": "test.txt",
  "user_id": "test_user"
}' localhost:50052 com.traffic.sim.plugin.map.grpc.MapConvertService/PreviewMap
```

## 测试参数

```bash
python3 test_grpc_services.py --help

# 可用参数:
#   --map-host     地图服务主机 (默认: localhost)
#   --map-port     地图服务端口 (默认: 50052)
#   --sim-host     仿真服务主机 (默认: localhost)
#   --sim-port     仿真服务端口 (默认: 50051)
#   --test-file    测试地图文件路径
#   --timeout      请求超时时间 (默认: 30秒)
```

## 测试用例

### 地图转换服务测试

| 测试用例 | 描述 | 预期结果 |
|---------|------|---------|
| ConvertMap | 转换 TXT/OSM 文件为 XML | 返回 XML 数据和文件名 |
| PreviewMap | 预览地图获取统计信息 | 返回道路数和交叉口数 |

### 仿真引擎服务测试

| 测试用例 | 描述 | 预期结果 |
|---------|------|---------|
| TestConnection | 测试服务连接 | connected=true |
| CreateSimeng | 创建仿真引擎 | res=OK 或业务错误 |
| ControlGreenRatio | 控制绿信比 | res=OK 或业务错误 |

## 测试报告

测试完成后会生成摘要报告：

```
============================================
Test Summary
============================================

Total tests: 5
Passed: 4
Failed: 1
Duration: 2.35s

Failed tests:
  - SimulationService/CreateSimeng: Map file not found
```

## 故障排查

### 1. 连接失败

```
Failed to connect to Map Service: Connection refused
```

**解决方案：**
- 确认服务已启动
- 检查端口是否正确
- 检查防火墙设置

### 2. 服务未响应

```
gRPC error: DEADLINE_EXCEEDED
```

**解决方案：**
- 增加超时时间：`--timeout 60`
- 检查服务是否正常运行
- 查看服务日志

### 3. Proto 文件错误

```
ImportError: No module named 'proto'
```

**解决方案：**
```bash
cd /Users/huxiaochuan/IdeaProjects/traffic_sim_pib/map_convert_services
python3 generate_grpc.py
```

## Docker 环境测试

在 Docker 环境中测试时，使用容器名作为主机：

```bash
python3 test_grpc_services.py \
    --map-host python-service \
    --map-port 50052 \
    --sim-host python-service \
    --sim-port 50051
```

## 持续集成

可以将测试集成到 CI/CD 流程中：

```yaml
# GitHub Actions 示例
test-grpc:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v2
    - name: Start services
      run: docker-compose -f docker-compose.full.yml up -d python-service
    - name: Wait for services
      run: sleep 10
    - name: Run tests
      run: |
        pip install grpcio grpcio-tools
        cd tests/grpc
        python test_grpc_services.py --map-host localhost --map-port 50052
```

## 联系方式

如有问题，请联系开发团队。

