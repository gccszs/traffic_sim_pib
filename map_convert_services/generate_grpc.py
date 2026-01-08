#!/usr/bin/env python3
"""
生成gRPC Python代码
运行此脚本以从.proto文件生成Python gRPC代码
"""
import subprocess
import sys
from pathlib import Path


def main():
    # 获取当前目录
    current_dir = Path(__file__).parent
    proto_dir = current_dir / "proto"
    proto_file = proto_dir / "map_service.proto"
    
    if not proto_file.exists():
        print(f"Error: Proto file not found: {proto_file}")
        sys.exit(1)
    
    print(f"Generating gRPC code from {proto_file}")
    
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
        print(f"Error generating gRPC code:")
        print(result.stderr)
        sys.exit(1)
    
    print("gRPC code generated successfully!")
    
    # 修复导入路径
    fix_imports(proto_dir)
    
    print("Import paths fixed!")


def fix_imports(proto_dir: Path):
    """修复生成代码中的导入路径"""
    grpc_file = proto_dir / "map_service_pb2_grpc.py"
    
    if grpc_file.exists():
        # 使用 UTF-8 编码读取和写入文件
        content = grpc_file.read_text(encoding='utf-8')
        # 修复相对导入
        content = content.replace(
            "import map_service_pb2 as map__service__pb2",
            "from . import map_service_pb2 as map__service__pb2"
        )
        grpc_file.write_text(content, encoding='utf-8')
        print(f"Fixed imports in {grpc_file}")


if __name__ == "__main__":
    main()

