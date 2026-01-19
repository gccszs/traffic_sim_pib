#!/usr/bin/env python3
"""
生成gRPC Python代码
运行此脚本以从.proto文件生成Python gRPC代码
支持多个服务: MapConvertService 和 PythonService
"""
import subprocess
import sys
from pathlib import Path


# 需要生成的 proto 文件列表
PROTO_FILES = [
    "map_service.proto",      # 地图转换服务
    "python_service.proto",   # 仿真引擎服务
]


def main():
    # 获取当前目录
    current_dir = Path(__file__).parent
    proto_dir = current_dir / "proto"
    
    print("="*60)
    print("Generating gRPC Python code...")
    print("="*60)
    
    for proto_file_name in PROTO_FILES:
        proto_file = proto_dir / proto_file_name
        
        if not proto_file.exists():
            print(f"Warning: Proto file not found: {proto_file}, skipping...")
            continue
        
        print(f"\nGenerating gRPC code from {proto_file_name}")
        
        # 运行 protoc 生成 Python 代码
        cmd = [
            sys.executable, "-m", "grpc_tools.protoc",
            f"--proto_path={proto_dir}",
            f"--python_out={proto_dir}",
            f"--grpc_python_out={proto_dir}",
            str(proto_file)
        ]
        
        print(f"Running: {' '.join(cmd)}")
        result = subprocess.run(cmd, capture_output=True, text=True)
        
        if result.returncode != 0:
            print(f"Error generating gRPC code for {proto_file_name}:")
            print(result.stderr)
            continue
        
        print(f"gRPC code generated successfully for {proto_file_name}!")
    
    # 修复导入路径
    fix_imports(proto_dir)
    
    print("\n" + "="*60)
    print("All import paths fixed!")
    print("="*60)


def fix_imports(proto_dir: Path):
    """修复生成代码中的导入路径"""
    # 修复 map_service
    map_grpc_file = proto_dir / "map_service_pb2_grpc.py"
    if map_grpc_file.exists():
        content = map_grpc_file.read_text(encoding='utf-8')
        content = content.replace(
            "import map_service_pb2 as map__service__pb2",
            "from . import map_service_pb2 as map__service__pb2"
        )
        map_grpc_file.write_text(content, encoding='utf-8')
        print(f"Fixed imports in {map_grpc_file.name}")
    
    # 修复 python_service
    python_grpc_file = proto_dir / "python_service_pb2_grpc.py"
    if python_grpc_file.exists():
        content = python_grpc_file.read_text(encoding='utf-8')
        content = content.replace(
            "import python_service_pb2 as python__service__pb2",
            "from . import python_service_pb2 as python__service__pb2"
        )
        python_grpc_file.write_text(content, encoding='utf-8')
        print(f"Fixed imports in {python_grpc_file.name}")


if __name__ == "__main__":
    main()

