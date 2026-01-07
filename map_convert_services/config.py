from pydantic.v1 import BaseSettings, Field


class Settings(BaseSettings):
    """应用配置类"""

    # 服务器配置 - 从环境变量读取，提供默认值
    host: str = Field(default="localhost", env="APP_HOST")
    port: int = Field(default=8000, env="APP_PORT")
    client_socket_ip: str = Field(default="192.168.1.212", env="CLIENT_SOCKET_IP")

    # gRPC配置
    grpc_port: int = Field(default=50052, env="GRPC_PORT")

    # 日志配置
    log_home: str = Field(default="./engine_sim_logs/", env="LOG_HOME")



# 创建全局配置实例
settings = Settings()