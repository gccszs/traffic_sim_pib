#!/usr/bin/env python3
"""
统一启动脚本 - 同时启动FastAPI和gRPC服务
"""
import asyncio
import os
import sys
import threading
import logging
from concurrent import futures

import uvicorn
from grpc import aio

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def start_fastapi_server():
    """启动FastAPI服务器"""
    from config import settings
    logger.info(f"Starting FastAPI server on {settings.host}:{settings.port}")
    uvicorn.run(
        "web_app:app",
        host=settings.host,
        port=settings.port,
        log_level="info"
    )


async def start_grpc_server():
    """启动gRPC服务器"""
    from proto import map_service_pb2_grpc
    from grpc_server import MapConvertServiceServicer
    
    grpc_port = int(os.environ.get('GRPC_PORT', 50052))
    
    server = aio.server(futures.ThreadPoolExecutor(max_workers=10))
    map_service_pb2_grpc.add_MapConvertServiceServicer_to_server(
        MapConvertServiceServicer(), server
    )
    
    listen_addr = f'[::]:{grpc_port}'
    server.add_insecure_port(listen_addr)
    
    logger.info(f"Starting gRPC server on port {grpc_port}")
    await server.start()
    await server.wait_for_termination()


def main():
    """主入口 - 同时启动两个服务"""
    logger.info("Starting map conversion services...")
    
    # 在单独的线程中启动 FastAPI
    fastapi_thread = threading.Thread(target=start_fastapi_server, daemon=True)
    fastapi_thread.start()
    
    # 在主线程中运行 gRPC（使用asyncio）
    try:
        asyncio.run(start_grpc_server())
    except KeyboardInterrupt:
        logger.info("Shutting down services...")


if __name__ == "__main__":
    main()

