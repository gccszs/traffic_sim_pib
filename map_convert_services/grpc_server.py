"""
gRPC服务端 - 统一gRPC服务入口
提供两个服务:
1. MapConvertService (50052端口) - 地图文件转换和预览
2. PythonService (50051端口) - 仿真引擎管理（对应Java端 SimulationPythonGrpcClient）
"""
import os
import asyncio
import logging
import uuid
from concurrent import futures
from pathlib import Path
from typing import Tuple

import grpc
from grpc import aio

# 导入生成的 gRPC 代码 - 地图服务
from proto import map_service_pb2
from proto import map_service_pb2_grpc

# 导入生成的 gRPC 代码 - 仿真服务
from proto import python_service_pb2_grpc

# 导入地图转换工具
from map_utils import osmtrans, mapmaker, mapmaker_new
from config import settings

# 导入仿真服务实现
from simulation_service import PythonServiceServicer

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),  # 输出到控制台
    ]
)
logger = logging.getLogger(__name__)

logger.info("=" * 60)
logger.info("Starting gRPC Service...")
logger.info("=" * 60)

# 缓存目录
CACHE_DIR = Path("cache/")
CACHE_DIR.mkdir(exist_ok=True)
logger.info(f"Cache directory: {CACHE_DIR} (exists: {CACHE_DIR.exists()})")


class MapConvertServiceServicer(map_service_pb2_grpc.MapConvertServiceServicer):
    """地图转换服务的gRPC实现"""

    async def ConvertMap(
        self,
        request: map_service_pb2.ConvertMapRequest,
        context: grpc.aio.ServicerContext
    ) -> map_service_pb2.ConvertMapResponse:
        """
        转换地图文件
        
        Args:
            request: 包含文件内容、文件名和用户ID的请求
            context: gRPC上下文
            
        Returns:
            转换结果，包含XML数据或错误信息
        """
        try:
            logger.info(f"ConvertMap request - user_id: {request.user_id}, file_name: {request.file_name}, file_size: {len(request.file_content)} bytes")
            
            # 创建用户工作目录
            work_dir = CACHE_DIR / request.user_id
            work_dir.mkdir(parents=True, exist_ok=True)
            
            # 保存上传的文件
            file_path = work_dir / request.file_name
            with open(file_path, 'wb') as f:
                f.write(request.file_content)
            
            # 执行转换
            xml_data, xml_file_name, conversion_method, xml_file_path = await self._convert_file(
                str(file_path), 
                str(work_dir),
                request.file_name,
                request.user_id
            )
            
            logger.info(f"Conversion successful - xml_file: {xml_file_name}, method: {conversion_method}, xml_size: {len(xml_data)} bytes, xml_path: {xml_file_path}")
            
            return map_service_pb2.ConvertMapResponse(
                success=True,
                message="转换成功",
                xml_data=xml_data,
                xml_file_name=xml_file_name,
                conversion_method=conversion_method,
                xml_file_path=xml_file_path
            )
            
        except Exception as e:
            logger.error(f"ConvertMap error: {str(e)}", exc_info=True)
            logger.error(f"Request details - user_id: {request.user_id}, file_name: {request.file_name}")
            return map_service_pb2.ConvertMapResponse(
                success=False,
                message=f"转换失败: {str(e)}",
                xml_data=b"",
                xml_file_name="",
                conversion_method=""
            )

    async def PreviewMap(
        self,
        request: map_service_pb2.PreviewMapRequest,
        context: grpc.aio.ServicerContext
    ) -> map_service_pb2.PreviewMapResponse:
        """
        预览地图文件
        
        Args:
            request: 包含文件内容、文件名和用户ID的请求
            context: gRPC上下文
            
        Returns:
            预览结果，包含简化的地图信息
        """
        try:
            logger.info(f"PreviewMap request - user_id: {request.user_id}, file_name: {request.file_name}, file_size: {len(request.file_content)} bytes")
            
            # 创建临时工作目录
            work_dir = CACHE_DIR / f"preview_{request.user_id}"
            work_dir.mkdir(parents=True, exist_ok=True)
            
            # 保存上传的文件
            file_path = work_dir / request.file_name
            with open(file_path, 'wb') as f:
                f.write(request.file_content)
            
            # 执行转换获取预览数据（预览不需要添加UUID）
            xml_data, xml_file_name, _, _ = await self._convert_file(
                str(file_path),
                str(work_dir),
                request.file_name,
                request.user_id,
                add_uuid=False  # 预览不需要UUID
            )
            
            # 解析XML获取统计信息
            road_count, intersection_count = self._parse_map_stats(xml_data)
            
            # 生成预览JSON
            preview_data = f'{{"road_count": {road_count}, "intersection_count": {intersection_count}}}'
            
            logger.info(f"Preview successful: roads={road_count}, intersections={intersection_count}")
            
            return map_service_pb2.PreviewMapResponse(
                success=True,
                message="预览成功",
                preview_data=preview_data,
                road_count=road_count,
                intersection_count=intersection_count
            )
            
        except Exception as e:
            logger.error(f"PreviewMap error: {str(e)}", exc_info=True)
            logger.error(f"Request details - user_id: {request.user_id}, file_name: {request.file_name}")
            return map_service_pb2.PreviewMapResponse(
                success=False,
                message=f"预览失败: {str(e)}",
                preview_data="",
                road_count=0,
                intersection_count=0
            )

    async def _convert_file(
        self, 
        file_path: str, 
        work_dir: str, 
        original_filename: str,
        user_id: str,
        add_uuid: bool = True
    ) -> Tuple[bytes, str, str, str]:
        """
        执行文件转换
        
        Args:
            file_path: 文件路径
            work_dir: 工作目录
            original_filename: 原始文件名
            user_id: 用户ID
            add_uuid: 是否添加UUID防止覆盖（默认True）
            
        Returns:
            (XML数据, XML文件名, 转换方法, XML文件路径)
        """
        name_parts = original_filename.split('.')
        if len(name_parts) < 2:
            raise ValueError("无效的文件名")
        
        file_name = name_parts[0]
        file_extension = name_parts[-1].lower()
        
        # 添加UUID防止文件名覆盖
        if add_uuid:
            unique_id = uuid.uuid4().hex[:8]  # 取前8位，足够区分
            output_name = f"{file_name}_{unique_id}"
        else:
            output_name = file_name
        
        new_file_location = os.path.join(work_dir, output_name)
        
        # 根据文件类型处理
        if file_extension == 'osm':
            txt_file_path = new_file_location + '.txt'
            logger.info(f"Converting OSM to TXT: {file_path} -> {txt_file_path}")
            result = osmtrans.osm_to_txt(file_path, txt_file_path)
            if not result:
                raise RuntimeError("OSM转TXT失败")
            logger.info("OSM to TXT conversion successful")
            xml_file_path, conversion_method = await self._convert_txt_to_xml(txt_file_path, new_file_location)
        elif file_extension == 'txt':
            logger.info(f"Converting TXT to XML: {file_path}")
            xml_file_path, conversion_method = await self._convert_txt_to_xml(file_path, new_file_location)
        elif file_extension == 'xml':
            # XML文件直接使用，不需要转换
            logger.info(f"XML file detected, using directly: {file_path}")
            xml_file_path = file_path
            conversion_method = 'direct'
        else:
            raise ValueError(f"不支持的文件格式: {file_extension}")
        
        # 读取转换后的文件
        with open(xml_file_path, 'rb') as f:
            xml_data = f.read()
        
        xml_file_name = os.path.basename(xml_file_path)
        
        # 返回相对路径（相对于 cache 目录）
        # 格式：{userId}/{xmlFileName}
        relative_path = os.path.join(user_id, xml_file_name)
        
        return xml_data, xml_file_name, conversion_method, relative_path

    async def _convert_txt_to_xml(self, txt_file_path: str, new_file_location: str) -> Tuple[str, str]:
        """
        TXT转XML
        
        Args:
            txt_file_path: TXT文件路径
            new_file_location: 输出文件位置（不含扩展名）
            
        Returns:
            (XML文件路径, 转换方法)
        """
        xml_file_path = new_file_location + '.xml'
        
        logger.info(f"Converting TXT to XML: {txt_file_path} -> {xml_file_path}")
        
        if not os.path.exists(txt_file_path):
            raise FileNotFoundError(f"源文件不存在: {txt_file_path}")
        
        # 尝试旧方法转换
        logger.info("Trying old conversion method...")
        result = mapmaker.txt_to_xml(txt_file_path, xml_file_path)
        conversion_method = 'old'
        
        if not result:
            # 尝试新方法转换
            logger.info("Old method failed, trying new conversion method...")
            conversion_method = 'new'
            result = mapmaker_new.txt_to_xml_new(txt_file_path, xml_file_path)
        
        if not result:
            raise RuntimeError("TXT转XML失败")
        
        if not os.path.exists(xml_file_path):
            raise RuntimeError("输出文件未创建")
        
        logger.info(f"TXT to XML conversion successful using {conversion_method} method")
        
        return xml_file_path, conversion_method

    def _parse_map_stats(self, xml_data: bytes) -> Tuple[int, int]:
        """
        解析地图XML获取统计信息
        
        Args:
            xml_data: XML数据
            
        Returns:
            (道路数量, 交叉口数量)
        """
        try:
            import xml.etree.ElementTree as ET
            root = ET.fromstring(xml_data)
            
            # 统计道路和交叉口数量
            roads = root.findall('.//road') or root.findall('.//Road')
            intersections = root.findall('.//cross') or root.findall('.//Cross') or root.findall('.//intersection')
            
            return len(roads), len(intersections)
        except Exception as e:
            logger.warning(f"解析地图统计信息失败: {e}")
            return 0, 0


async def serve_map_service(port: int = 50052):
    """
    启动地图转换gRPC服务器
    
    Args:
        port: 服务端口，默认50052
    """
    logger.info("=" * 60)
    logger.info(f"Initializing MapConvertService gRPC server on port {port}")
    logger.info("=" * 60)
    
    # 添加优化配置
    options = [
        ('grpc.max_concurrent_streams', 100),            # 最大并发流数
        ('grpc.max_receive_message_length', 1024 * 1024 * 100),  # 最大接收消息大小 (100MB)
        ('grpc.max_send_message_length', 1024 * 1024 * 100),     # 最大发送消息大小 (100MB)
        ('grpc.so_reuseport', True),                    # 启用端口复用
    ]
    
    server = aio.server(futures.ThreadPoolExecutor(max_workers=10), options=options)
    map_service_pb2_grpc.add_MapConvertServiceServicer_to_server(
        MapConvertServiceServicer(), server
    )
    
    listen_addr = f'[::]:{port}'
    server.add_insecure_port(listen_addr)
    
    logger.info(f"Starting MapConvertService gRPC server...")
    await server.start()
    
    logger.info(f"MapConvertService gRPC server started successfully")
    logger.info(f"Listening on: {listen_addr}")
    logger.info("=" * 60)
    
    return server


async def serve_simulation_service(port: int = 50051):
    """
    启动仿真服务gRPC服务器
    对应Java端的 SimulationPythonGrpcClient 调用
    
    Args:
        port: 服务端口，默认50051
    """
    logger.info("=" * 60)
    logger.info(f"Initializing PythonService gRPC server on port {port}")
    logger.info("=" * 60)
    
    # 添加优化配置
    options = [
        ('grpc.max_concurrent_streams', 100),            # 最大并发流数
        ('grpc.max_receive_message_length', 1024 * 1024 * 100),  # 最大接收消息大小 (100MB)
        ('grpc.max_send_message_length', 1024 * 1024 * 100),     # 最大发送消息大小 (100MB)
        ('grpc.so_reuseport', True),                    # 启用端口复用
    ]
    
    server = aio.server(futures.ThreadPoolExecutor(max_workers=10), options=options)
    python_service_pb2_grpc.add_PythonServiceServicer_to_server(
        PythonServiceServicer(), server
    )
    
    listen_addr = f'[::]:{port}'
    server.add_insecure_port(listen_addr)
    
    logger.info(f"Starting PythonService gRPC server...")
    await server.start()
    
    logger.info(f"PythonService gRPC server started successfully")
    logger.info(f"Listening on: {listen_addr}")
    logger.info("=" * 60)
    
    return server


async def serve(map_port: int = 50052, sim_port: int = 50051):
    """
    启动所有gRPC服务
    
    Args:
        map_port: 地图服务端口，默认50052
        sim_port: 仿真服务端口，默认50051
    """
    logger.info("=" * 60)
    logger.info("Starting All gRPC Services...")
    logger.info("=" * 60)
    
    # 启动地图转换服务
    map_server = await serve_map_service(map_port)
    
    # 启动仿真服务
    sim_server = await serve_simulation_service(sim_port)
    
    logger.info("=" * 60)
    logger.info("All gRPC services started successfully!")
    logger.info(f"  - MapConvertService: port {map_port}")
    logger.info(f"  - PythonService: port {sim_port}")
    logger.info("Server is ready to accept connections")
    logger.info("=" * 60)
    
    # 等待服务终止
    await asyncio.gather(
        map_server.wait_for_termination(),
        sim_server.wait_for_termination()
    )


def main():
    """主入口"""
    # 地图服务端口 - 对应 Java 端 plugin-map 的 MapPythonGrpcClient
    map_grpc_port = int(os.environ.get('MAP_GRPC_PORT', 50052))
    # 仿真服务端口 - 对应 Java 端 plugin-simulation 的 SimulationPythonGrpcClient
    sim_grpc_port = int(os.environ.get('SIM_GRPC_PORT', 50051))
    
    logger.info(f"Map gRPC port: {map_grpc_port}")
    logger.info(f"Simulation gRPC port: {sim_grpc_port}")
    
    asyncio.run(serve(map_grpc_port, sim_grpc_port))


if __name__ == '__main__':
    main()

