"""
gRPC服务端 - 地图转换服务
提供地图文件转换和预览功能的gRPC接口
"""
import os
import asyncio
import logging
from concurrent import futures
from pathlib import Path
from typing import Tuple

import grpc
from grpc import aio

# 导入生成的 gRPC 代码
from proto import map_service_pb2
from proto import map_service_pb2_grpc

# 导入地图转换工具
from map_utils import osmtrans, mapmaker, mapmaker_new
from config import settings

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 缓存目录
CACHE_DIR = Path("cache/")
CACHE_DIR.mkdir(exist_ok=True)


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
            logger.info(f"ConvertMap request: user_id={request.user_id}, file_name={request.file_name}")
            
            # 创建用户工作目录
            work_dir = CACHE_DIR / request.user_id
            work_dir.mkdir(parents=True, exist_ok=True)
            
            # 保存上传的文件
            file_path = work_dir / request.file_name
            with open(file_path, 'wb') as f:
                f.write(request.file_content)
            
            # 执行转换
            xml_data, xml_file_name, conversion_method = await self._convert_file(
                str(file_path), 
                str(work_dir),
                request.file_name
            )
            
            logger.info(f"Conversion successful: {xml_file_name}, method={conversion_method}")
            
            return map_service_pb2.ConvertMapResponse(
                success=True,
                message="转换成功",
                xml_data=xml_data,
                xml_file_name=xml_file_name,
                conversion_method=conversion_method
            )
            
        except Exception as e:
            logger.error(f"ConvertMap error: {str(e)}", exc_info=True)
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
            logger.info(f"PreviewMap request: user_id={request.user_id}, file_name={request.file_name}")
            
            # 创建临时工作目录
            work_dir = CACHE_DIR / f"preview_{request.user_id}"
            work_dir.mkdir(parents=True, exist_ok=True)
            
            # 保存上传的文件
            file_path = work_dir / request.file_name
            with open(file_path, 'wb') as f:
                f.write(request.file_content)
            
            # 执行转换获取预览数据
            xml_data, xml_file_name, _ = await self._convert_file(
                str(file_path),
                str(work_dir),
                request.file_name
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
        original_filename: str
    ) -> Tuple[bytes, str, str]:
        """
        执行文件转换
        
        Args:
            file_path: 文件路径
            work_dir: 工作目录
            original_filename: 原始文件名
            
        Returns:
            (XML数据, XML文件名, 转换方法)
        """
        name_parts = original_filename.split('.')
        if len(name_parts) < 2:
            raise ValueError("无效的文件名")
        
        file_name = name_parts[0]
        file_extension = name_parts[-1].lower()
        
        new_file_location = os.path.join(work_dir, file_name)
        
        # 根据文件类型处理
        if file_extension == 'osm':
            txt_file_path = new_file_location + '.txt'
            result = osmtrans.osm_to_txt(file_path, txt_file_path)
            if not result:
                raise RuntimeError("OSM转TXT失败")
            xml_file_path, conversion_method = await self._convert_txt_to_xml(txt_file_path, new_file_location)
        elif file_extension == 'txt':
            xml_file_path, conversion_method = await self._convert_txt_to_xml(file_path, new_file_location)
        else:
            raise ValueError(f"不支持的文件格式: {file_extension}")
        
        # 读取转换后的文件
        with open(xml_file_path, 'rb') as f:
            xml_data = f.read()
        
        xml_file_name = os.path.basename(xml_file_path)
        
        return xml_data, xml_file_name, conversion_method

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
        
        if not os.path.exists(txt_file_path):
            raise FileNotFoundError(f"源文件不存在: {txt_file_path}")
        
        # 尝试旧方法转换
        result = mapmaker.txt_to_xml(txt_file_path, xml_file_path)
        conversion_method = 'old'
        
        if not result:
            # 尝试新方法转换
            conversion_method = 'new'
            result = mapmaker_new.txt_to_xml_new(txt_file_path, xml_file_path)
        
        if not result:
            raise RuntimeError("TXT转XML失败")
        
        if not os.path.exists(xml_file_path):
            raise RuntimeError("输出文件未创建")
        
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


async def serve(port: int = 50052):
    """
    启动gRPC服务器
    
    Args:
        port: 服务端口，默认50052
    """
    server = aio.server(futures.ThreadPoolExecutor(max_workers=10))
    map_service_pb2_grpc.add_MapConvertServiceServicer_to_server(
        MapConvertServiceServicer(), server
    )
    
    listen_addr = f'[::]:{port}'
    server.add_insecure_port(listen_addr)
    
    logger.info(f"Starting gRPC server on port {port}")
    await server.start()
    
    logger.info(f"gRPC server started, listening on {listen_addr}")
    await server.wait_for_termination()


def main():
    """主入口"""
    grpc_port = int(os.environ.get('GRPC_PORT', 50052))
    asyncio.run(serve(grpc_port))


if __name__ == '__main__':
    main()

