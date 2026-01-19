from pydantic.v1 import BaseSettings, Field


class Settings(BaseSettings):
    """应用配置类"""

    # HTTP 服务器配置 - FastAPI/WebSocket 服务
    host: str = Field(default="localhost", env="APP_HOST")
    port: int = Field(default=8000, env="APP_PORT")
    client_socket_ip: str = Field(default="localhost", env="CLIENT_SOCKET_IP")

    # Java 后端配置 - 仿真引擎连接后端 WebSocket
    backend_host: str = Field(default="localhost", env="BACKEND_HOST")
    backend_port: int = Field(default=3822, env="BACKEND_PORT")
    # 注意：Spring WebSocket 路径不受 servlet context-path 影响，所以这里为空
    backend_ws_path: str = Field(default="", env="BACKEND_WS_PATH")

    # gRPC 服务配置
    # 地图转换服务端口 - 对应 Java 端 plugin-map 的 MapPythonGrpcClient
    map_grpc_port: int = Field(default=50052, env="MAP_GRPC_PORT")
    # 仿真服务端口 - 对应 Java 端 plugin-simulation 的 SimulationPythonGrpcClient
    sim_grpc_port: int = Field(default=50051, env="SIM_GRPC_PORT")

    # 日志配置
    log_home: str = Field(default="./engine_sim_logs/", env="LOG_HOME")


# 创建全局配置实例
settings = Settings()