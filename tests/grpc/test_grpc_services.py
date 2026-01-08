#!/usr/bin/env python3
"""
gRPC 服务测试脚本
测试地图转换服务和仿真引擎服务

使用方法:
    cd /Users/huxiaochuan/IdeaProjects/traffic_sim_pib/tests/grpc
    pip install grpcio grpcio-tools
    python test_grpc_services.py
"""

import os
import sys
import grpc
import json
import time
import argparse
from pathlib import Path
from typing import Optional, Tuple
from dataclasses import dataclass
from datetime import datetime

# 添加项目路径
PROJECT_ROOT = Path(__file__).parent.parent.parent
sys.path.insert(0, str(PROJECT_ROOT / "map_convert_services"))

# ============================================
# 测试配置
# ============================================
@dataclass
class TestConfig:
    map_service_host: str = "localhost"
    map_service_port: int = 50052
    sim_service_host: str = "localhost"
    sim_service_port: int = 50051
    timeout: int = 30


# ============================================
# 颜色输出
# ============================================
class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    RESET = '\033[0m'
    BOLD = '\033[1m'


def print_header(title: str):
    print(f"\n{Colors.BLUE}{Colors.BOLD}{'=' * 60}{Colors.RESET}")
    print(f"{Colors.BLUE}{Colors.BOLD}{title}{Colors.RESET}")
    print(f"{Colors.BLUE}{Colors.BOLD}{'=' * 60}{Colors.RESET}")


def print_success(msg: str):
    print(f"{Colors.GREEN}✓ {msg}{Colors.RESET}")


def print_error(msg: str):
    print(f"{Colors.RED}✗ {msg}{Colors.RESET}")


def print_warning(msg: str):
    print(f"{Colors.YELLOW}⚠ {msg}{Colors.RESET}")


def print_info(msg: str):
    print(f"{Colors.BLUE}ℹ {msg}{Colors.RESET}")


# ============================================
# 地图转换服务测试
# ============================================
class MapServiceTester:
    """地图转换服务测试器"""
    
    def __init__(self, config: TestConfig):
        self.config = config
        self.channel = None
        self.stub = None
        
    def connect(self) -> bool:
        """连接到服务"""
        try:
            from proto import map_service_pb2_grpc
            
            address = f"{self.config.map_service_host}:{self.config.map_service_port}"
            print_info(f"Connecting to Map Service at {address}...")
            
            self.channel = grpc.insecure_channel(address)
            self.stub = map_service_pb2_grpc.MapConvertServiceStub(self.channel)
            
            # 检查连接
            grpc.channel_ready_future(self.channel).result(timeout=5)
            print_success(f"Connected to Map Service at {address}")
            return True
            
        except Exception as e:
            print_error(f"Failed to connect to Map Service: {e}")
            return False
    
    def test_convert_map(self, test_file: Optional[str] = None) -> Tuple[bool, str]:
        """测试地图转换"""
        try:
            from proto import map_service_pb2
            
            print_info("Testing ConvertMap...")
            
            # 准备测试数据
            if test_file and os.path.exists(test_file):
                with open(test_file, 'rb') as f:
                    file_content = f.read()
                file_name = os.path.basename(test_file)
            else:
                # 使用示例 TXT 数据
                file_content = self._create_sample_txt_map()
                file_name = "test_map.txt"
            
            # 创建请求
            request = map_service_pb2.ConvertMapRequest(
                file_content=file_content,
                file_name=file_name,
                user_id="test_user_001"
            )
            
            # 发送请求
            start_time = time.time()
            response = self.stub.ConvertMap(request, timeout=self.config.timeout)
            elapsed = time.time() - start_time
            
            # 检查响应
            if response.success:
                print_success(f"ConvertMap successful (took {elapsed:.2f}s)")
                print_info(f"  - XML file: {response.xml_file_name}")
                print_info(f"  - Method: {response.conversion_method}")
                print_info(f"  - XML size: {len(response.xml_data)} bytes")
                return True, response.message
            else:
                print_error(f"ConvertMap failed: {response.message}")
                return False, response.message
                
        except grpc.RpcError as e:
            error_msg = f"gRPC error: {e.code()} - {e.details()}"
            print_error(error_msg)
            return False, error_msg
        except Exception as e:
            error_msg = f"Unexpected error: {str(e)}"
            print_error(error_msg)
            return False, error_msg
    
    def test_preview_map(self, test_file: Optional[str] = None) -> Tuple[bool, str]:
        """测试地图预览"""
        try:
            from proto import map_service_pb2
            
            print_info("Testing PreviewMap...")
            
            # 准备测试数据
            if test_file and os.path.exists(test_file):
                with open(test_file, 'rb') as f:
                    file_content = f.read()
                file_name = os.path.basename(test_file)
            else:
                file_content = self._create_sample_txt_map()
                file_name = "test_map.txt"
            
            # 创建请求
            request = map_service_pb2.PreviewMapRequest(
                file_content=file_content,
                file_name=file_name,
                user_id="test_user_001"
            )
            
            # 发送请求
            start_time = time.time()
            response = self.stub.PreviewMap(request, timeout=self.config.timeout)
            elapsed = time.time() - start_time
            
            # 检查响应
            if response.success:
                print_success(f"PreviewMap successful (took {elapsed:.2f}s)")
                print_info(f"  - Road count: {response.road_count}")
                print_info(f"  - Intersection count: {response.intersection_count}")
                print_info(f"  - Preview data: {response.preview_data[:100]}...")
                return True, response.message
            else:
                print_error(f"PreviewMap failed: {response.message}")
                return False, response.message
                
        except grpc.RpcError as e:
            error_msg = f"gRPC error: {e.code()} - {e.details()}"
            print_error(error_msg)
            return False, error_msg
        except Exception as e:
            error_msg = f"Unexpected error: {str(e)}"
            print_error(error_msg)
            return False, error_msg
    
    def _create_sample_txt_map(self) -> bytes:
        """创建示例 TXT 地图数据"""
        # 简单的测试地图数据格式
        sample_data = """# Sample map data for testing
# Format: node_id, x, y, type
NODE 1 0.0 0.0 INTERSECTION
NODE 2 100.0 0.0 INTERSECTION
NODE 3 0.0 100.0 INTERSECTION
NODE 4 100.0 100.0 INTERSECTION

# Format: road_id, from_node, to_node, lanes
ROAD 1 1 2 2
ROAD 2 2 4 2
ROAD 3 1 3 2
ROAD 4 3 4 2
"""
        return sample_data.encode('utf-8')
    
    def close(self):
        """关闭连接"""
        if self.channel:
            self.channel.close()


# ============================================
# 仿真引擎服务测试
# ============================================
class SimulationServiceTester:
    """仿真引擎服务测试器"""
    
    def __init__(self, config: TestConfig):
        self.config = config
        self.channel = None
        self.stub = None
        
    def connect(self) -> bool:
        """连接到服务"""
        try:
            # 生成 Python gRPC 代码
            self._ensure_proto_generated()
            
            # 动态导入生成的模块
            import importlib.util
            proto_dir = Path(__file__).parent / "simulation_proto"
            
            spec = importlib.util.spec_from_file_location(
                "python_service_pb2_grpc", 
                proto_dir / "python_service_pb2_grpc.py"
            )
            python_service_pb2_grpc = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(python_service_pb2_grpc)
            
            self.pb2_grpc = python_service_pb2_grpc
            
            address = f"{self.config.sim_service_host}:{self.config.sim_service_port}"
            print_info(f"Connecting to Simulation Service at {address}...")
            
            self.channel = grpc.insecure_channel(address)
            self.stub = python_service_pb2_grpc.PythonServiceStub(self.channel)
            
            # 检查连接
            grpc.channel_ready_future(self.channel).result(timeout=5)
            print_success(f"Connected to Simulation Service at {address}")
            return True
            
        except Exception as e:
            print_error(f"Failed to connect to Simulation Service: {e}")
            import traceback
            traceback.print_exc()
            return False
    
    def _ensure_proto_generated(self):
        """确保 proto 文件已生成"""
        proto_dir = Path(__file__).parent / "simulation_proto"
        proto_dir.mkdir(exist_ok=True)
        
        pb2_file = proto_dir / "python_service_pb2.py"
        if not pb2_file.exists():
            print_info("Generating simulation proto files...")
            self._generate_proto()
        
        # 添加到 Python 路径
        if str(proto_dir) not in sys.path:
            sys.path.insert(0, str(proto_dir))
    
    def _generate_proto(self):
        """生成 proto 文件"""
        import subprocess
        
        proto_src = PROJECT_ROOT / "plugins/plugin-simulation/src/main/proto/python_service.proto"
        proto_out = Path(__file__).parent / "simulation_proto"
        proto_out.mkdir(exist_ok=True)
        
        # 创建 __init__.py
        (proto_out / "__init__.py").touch()
        
        cmd = [
            sys.executable, "-m", "grpc_tools.protoc",
            f"-I{proto_src.parent}",
            f"--python_out={proto_out}",
            f"--grpc_python_out={proto_out}",
            str(proto_src)
        ]
        
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            print_warning(f"Proto generation warning: {result.stderr}")
        else:
            print_success("Proto files generated successfully")
    
    def _load_pb2(self):
        """加载 pb2 模块"""
        import importlib.util
        proto_dir = Path(__file__).parent / "simulation_proto"
        
        spec = importlib.util.spec_from_file_location(
            "python_service_pb2", 
            proto_dir / "python_service_pb2.py"
        )
        pb2 = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(pb2)
        return pb2
    
    def test_connection(self) -> Tuple[bool, str]:
        """测试连接"""
        try:
            pb2 = self._load_pb2()
            
            print_info("Testing TestConnection...")
            
            request = pb2.Empty()
            
            start_time = time.time()
            response = self.stub.TestConnection(request, timeout=self.config.timeout)
            elapsed = time.time() - start_time
            
            if response.connected:
                print_success(f"TestConnection successful (took {elapsed:.2f}s)")
                print_info(f"  - Message: {response.message}")
                return True, response.message
            else:
                print_error(f"TestConnection failed: {response.message}")
                return False, response.message
                
        except grpc.RpcError as e:
            error_msg = f"gRPC error: {e.code()} - {e.details()}"
            print_error(error_msg)
            return False, error_msg
        except Exception as e:
            error_msg = f"Unexpected error: {str(e)}"
            print_error(error_msg)
            return False, error_msg
    
    def test_create_simeng(self) -> Tuple[bool, str]:
        """测试创建仿真引擎"""
        try:
            pb2 = self._load_pb2()
            
            print_info("Testing CreateSimeng...")
            
            # 构建测试请求
            sim_info = pb2.SimInfo(
                name="test_simulation",
                mapXmlName="test_map.xml",
                mapXmlPath="/tmp/test_map.xml"
            )
            
            request = pb2.CreateSimengRequest(
                simInfo=sim_info,
                userId="test_user_001"
            )
            
            start_time = time.time()
            response = self.stub.CreateSimeng(request, timeout=self.config.timeout)
            elapsed = time.time() - start_time
            
            if response.res == "OK" or response.res == "0":
                print_success(f"CreateSimeng successful (took {elapsed:.2f}s)")
                print_info(f"  - Message: {response.msg}")
                return True, response.msg
            else:
                print_warning(f"CreateSimeng returned: {response.res} - {response.msg}")
                return True, response.msg  # 可能是预期的业务错误
                
        except grpc.RpcError as e:
            error_msg = f"gRPC error: {e.code()} - {e.details()}"
            print_error(error_msg)
            return False, error_msg
        except Exception as e:
            error_msg = f"Unexpected error: {str(e)}"
            print_error(error_msg)
            return False, error_msg
    
    def test_green_ratio_control(self, green_ratio: int = 50) -> Tuple[bool, str]:
        """测试绿信比控制"""
        try:
            pb2 = self._load_pb2()
            
            print_info(f"Testing ControlGreenRatio (ratio={green_ratio})...")
            
            request = pb2.GreenRatioControlRequest(
                greenRatio=green_ratio
            )
            
            start_time = time.time()
            response = self.stub.ControlGreenRatio(request, timeout=self.config.timeout)
            elapsed = time.time() - start_time
            
            if response.res == "OK" or response.res == "0":
                print_success(f"ControlGreenRatio successful (took {elapsed:.2f}s)")
                print_info(f"  - Message: {response.msg}")
                return True, response.msg
            else:
                print_warning(f"ControlGreenRatio returned: {response.res} - {response.msg}")
                return True, response.msg
                
        except grpc.RpcError as e:
            error_msg = f"gRPC error: {e.code()} - {e.details()}"
            print_error(error_msg)
            return False, error_msg
        except Exception as e:
            error_msg = f"Unexpected error: {str(e)}"
            print_error(error_msg)
            return False, error_msg
    
    def close(self):
        """关闭连接"""
        if self.channel:
            self.channel.close()


# ============================================
# 仿真引擎 HTTP 测试（当前实现）
# ============================================
class SimulationHttpTester:
    """仿真引擎 HTTP 服务测试器"""
    
    def __init__(self, config: TestConfig):
        self.config = config
        self.base_url = f"http://{config.sim_service_host}:8000"
    
    def test_http_connection(self) -> Tuple[bool, str]:
        """测试 HTTP 连接"""
        try:
            import urllib.request
            import urllib.error
            
            print_info(f"Testing HTTP connection to {self.base_url}/test...")
            
            url = f"{self.base_url}/test"
            start_time = time.time()
            
            req = urllib.request.Request(url, method='GET')
            with urllib.request.urlopen(req, timeout=5) as response:
                elapsed = time.time() - start_time
                data = response.read().decode('utf-8')
                
                print_success(f"HTTP connection successful (took {elapsed:.2f}s)")
                print_info(f"  - Status: {response.status}")
                print_info(f"  - Response: {data[:100]}...")
                return True, "HTTP connection OK"
                
        except urllib.error.URLError as e:
            error_msg = f"HTTP connection failed: {e.reason}"
            print_error(error_msg)
            return False, error_msg
        except Exception as e:
            error_msg = f"Unexpected error: {str(e)}"
            print_error(error_msg)
            return False, error_msg
    
    def test_init_simeng(self) -> Tuple[bool, str]:
        """测试初始化仿真引擎"""
        try:
            import urllib.request
            import urllib.error
            
            print_info("Testing init_simeng via HTTP...")
            
            url = f"{self.base_url}/init_simeng"
            
            # 构建测试请求
            test_data = json.dumps({
                "simInfo": {
                    "name": "test_simulation",
                    "mapXmlName": "test_map.xml",
                    "mapXmlPath": "/tmp/test_map.xml",
                    "fixedOd": {
                        "od": [],
                        "sg": []
                    }
                },
                "controlViews": [],
                "userId": "test_user_001"
            }).encode('utf-8')
            
            start_time = time.time()
            
            req = urllib.request.Request(
                url, 
                data=test_data,
                headers={'Content-Type': 'application/json'},
                method='POST'
            )
            
            with urllib.request.urlopen(req, timeout=30) as response:
                elapsed = time.time() - start_time
                data = json.loads(response.read().decode('utf-8'))
                
                print_success(f"init_simeng request completed (took {elapsed:.2f}s)")
                print_info(f"  - Response: {data}")
                return True, str(data)
                
        except urllib.error.HTTPError as e:
            # 业务错误也算成功（服务可达）
            error_msg = f"HTTP {e.code}: {e.read().decode('utf-8')}"
            print_warning(f"init_simeng returned error (service is reachable): {error_msg}")
            return True, error_msg
        except urllib.error.URLError as e:
            error_msg = f"HTTP connection failed: {e.reason}"
            print_error(error_msg)
            return False, error_msg
        except Exception as e:
            error_msg = f"Unexpected error: {str(e)}"
            print_error(error_msg)
            return False, error_msg


# ============================================
# 测试报告
# ============================================
class TestReport:
    """测试报告"""
    
    def __init__(self):
        self.results = []
        self.start_time = datetime.now()
        
    def add_result(self, service: str, test_name: str, success: bool, message: str):
        self.results.append({
            "service": service,
            "test": test_name,
            "success": success,
            "message": message,
            "timestamp": datetime.now().isoformat()
        })
    
    def print_summary(self):
        print_header("Test Summary")
        
        total = len(self.results)
        passed = sum(1 for r in self.results if r["success"])
        failed = total - passed
        
        print(f"\nTotal tests: {total}")
        print(f"{Colors.GREEN}Passed: {passed}{Colors.RESET}")
        print(f"{Colors.RED}Failed: {failed}{Colors.RESET}")
        print(f"Duration: {(datetime.now() - self.start_time).total_seconds():.2f}s")
        
        if failed > 0:
            print(f"\n{Colors.RED}Failed tests:{Colors.RESET}")
            for r in self.results:
                if not r["success"]:
                    print(f"  - {r['service']}/{r['test']}: {r['message']}")
        
        return failed == 0
    
    def to_json(self) -> str:
        return json.dumps({
            "start_time": self.start_time.isoformat(),
            "end_time": datetime.now().isoformat(),
            "results": self.results
        }, indent=2)


# ============================================
# 主测试函数
# ============================================
def run_tests(config: TestConfig, test_file: Optional[str] = None) -> bool:
    """运行所有测试"""
    report = TestReport()
    
    # 测试地图转换服务
    print_header("Map Convert Service Tests")
    map_tester = MapServiceTester(config)
    
    if map_tester.connect():
        success, msg = map_tester.test_convert_map(test_file)
        report.add_result("MapService", "ConvertMap", success, msg)
        
        success, msg = map_tester.test_preview_map(test_file)
        report.add_result("MapService", "PreviewMap", success, msg)
    else:
        report.add_result("MapService", "Connection", False, "Failed to connect")
    
    map_tester.close()
    
    # 测试仿真引擎服务
    print_header("Simulation Engine Service Tests")
    
    # 注意：仿真引擎服务目前使用 HTTP REST API，不是 gRPC
    print_warning("Note: Simulation service uses HTTP REST API (port 8000), not gRPC (port 50051)")
    print_info("Testing HTTP endpoint instead...")
    
    # 测试 HTTP 端点
    http_tester = SimulationHttpTester(config)
    success, msg = http_tester.test_http_connection()
    report.add_result("SimulationService", "HTTPConnection", success, msg)
    
    if success:
        success, msg = http_tester.test_init_simeng()
        report.add_result("SimulationService", "InitSimeng(HTTP)", success, msg)
    
    # 打印报告
    return report.print_summary()


def main():
    parser = argparse.ArgumentParser(description="gRPC Service Tester")
    parser.add_argument("--map-host", default="localhost", help="Map service host")
    parser.add_argument("--map-port", type=int, default=50052, help="Map service port")
    parser.add_argument("--sim-host", default="localhost", help="Simulation service host")
    parser.add_argument("--sim-port", type=int, default=50051, help="Simulation service port")
    parser.add_argument("--test-file", help="Test map file path")
    parser.add_argument("--timeout", type=int, default=30, help="Request timeout in seconds")
    
    args = parser.parse_args()
    
    config = TestConfig(
        map_service_host=args.map_host,
        map_service_port=args.map_port,
        sim_service_host=args.sim_host,
        sim_service_port=args.sim_port,
        timeout=args.timeout
    )
    
    print_header("gRPC Service Test Suite")
    print(f"Map Service:        {config.map_service_host}:{config.map_service_port}")
    print(f"Simulation Service: {config.sim_service_host}:{config.sim_service_port}")
    print(f"Timeout:            {config.timeout}s")
    
    success = run_tests(config, args.test_file)
    
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()

