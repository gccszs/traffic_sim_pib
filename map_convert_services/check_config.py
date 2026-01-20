"""
配置验证脚本
用于检查 Python 服务配置是否正确
"""
import sys
import os
from pathlib import Path

# 设置 UTF-8 编码（Windows 兼容）
if sys.platform == 'win32':
    os.system('chcp 65001 > nul')
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

def check_config():
    """检查配置"""
    print("=" * 60)
    print("Python 服务配置检查")
    print("=" * 60)
    print()
    
    try:
        from config import settings
        
        print("[OK] 配置文件加载成功")
        print()
        print("当前配置：")
        print(f"  - FastAPI 服务地址: {settings.host}:{settings.port}")
        print(f"  - Java 后端地址: {settings.backend_host}:{settings.backend_port}")
        print(f"  - Java 后端 WebSocket 路径: {settings.backend_ws_path or '/'}")
        print(f"  - 地图 gRPC 端口: {settings.map_grpc_port}")
        print(f"  - 仿真 gRPC 端口: {settings.sim_grpc_port}")
        print(f"  - 日志目录: {settings.log_home}")
        print()
        
        # 检查关键配置
        issues = []
        
        if settings.backend_port == settings.port:
            issues.append("[WARN] backend_port 和 port 相同，引擎将连接到 Python 服务而不是 Java 后端")
        
        if settings.backend_port != 3822:
            issues.append(f"[WARN] backend_port 是 {settings.backend_port}，通常应该是 3822（Java 后端端口）")
        
        if settings.sim_grpc_port == settings.map_grpc_port:
            issues.append("[ERROR] sim_grpc_port 和 map_grpc_port 不能相同")
        
        if issues:
            print("发现问题：")
            for issue in issues:
                print(f"  {issue}")
            print()
        else:
            print("[OK] 配置检查通过")
            print()
        
        # 显示引擎连接信息
        print("引擎连接配置：")
        ws_url = f"ws://{settings.backend_host}:{settings.backend_port}{settings.backend_ws_path}/ws/exe/{{sessionId}}"
        print(f"  引擎将连接到: {ws_url}")
        print()
        
        # 显示架构说明
        print("架构说明：")
        print("  1. Java 后端通过 gRPC 调用 Python 服务创建引擎")
        print(f"     Java -> Python gRPC ({settings.sim_grpc_port})")
        print()
        print("  2. Python 服务启动引擎，引擎连接到 Java 后端")
        print(f"     引擎 -> Java WebSocket ({settings.backend_port})")
        print()
        print("  3. 引擎发送数据到 Java 后端，Java 后端转发给前端")
        print(f"     引擎 -> Java ({settings.backend_port}) -> 前端")
        print()
        
        return len(issues) == 0
        
    except ImportError as e:
        print(f"[ERROR] 无法导入配置模块")
        print(f"  {e}")
        print()
        print("请确保：")
        print("  1. 在 map_convert_services 目录下运行此脚本")
        print("  2. 已安装所有依赖: pip install -r requirements.txt")
        return False
    except Exception as e:
        print(f"[ERROR] {e}")
        return False

def check_files():
    """检查必要的文件"""
    print("=" * 60)
    print("文件检查")
    print("=" * 60)
    print()
    
    required_files = [
        "config.py",
        "web_app.py",
        "grpc_server.py",
        "simulation_service.py",
        "requirements.txt",
    ]
    
    all_exist = True
    for file in required_files:
        if Path(file).exists():
            print(f"  [OK] {file}")
        else:
            print(f"  [MISS] {file}")
            all_exist = False
    
    print()
    return all_exist

def check_dependencies():
    """检查依赖"""
    print("=" * 60)
    print("依赖检查")
    print("=" * 60)
    print()
    
    required_modules = [
        ("fastapi", "FastAPI"),
        ("uvicorn", "Uvicorn"),
        ("grpc", "gRPC"),
        ("pydantic", "Pydantic"),
    ]
    
    all_installed = True
    for module, name in required_modules:
        try:
            __import__(module)
            print(f"  [OK] {name}")
        except ImportError:
            print(f"  [MISS] {name}")
            all_installed = False
    
    print()
    
    if not all_installed:
        print("请安装缺失的依赖:")
        print("  pip install -r requirements.txt")
        print()
    
    return all_installed

def main():
    """主函数"""
    print()
    print("=" * 60)
    print("       Python 服务配置验证工具")
    print("=" * 60)
    print()
    
    # 检查文件
    files_ok = check_files()
    
    # 检查依赖
    deps_ok = check_dependencies()
    
    # 检查配置
    config_ok = check_config()
    
    # 总结
    print("=" * 60)
    print("检查总结")
    print("=" * 60)
    print()
    
    if files_ok and deps_ok and config_ok:
        print("[OK] 所有检查通过，可以启动服务")
        print()
        print("启动命令:")
        print("  Windows: start.bat")
        print("  Linux/Mac: bash start.sh")
        print()
        return 0
    else:
        print("[ERROR] 发现问题，请修复后再启动服务")
        print()
        if not files_ok:
            print("  - 缺少必要的文件")
        if not deps_ok:
            print("  - 缺少必要的依赖")
        if not config_ok:
            print("  - 配置存在问题")
        print()
        return 1

if __name__ == "__main__":
    sys.exit(main())
