"""
仿真服务 gRPC 实现
提供仿真引擎创建、控制等 gRPC 接口
对应 Java 端的 SimulationPythonGrpcClient 调用
"""
import os
import json
import shutil
import logging
from pathlib import Path
from collections import defaultdict
from io import BytesIO
from zipfile import ZipFile

import grpc
from grpc import aio

# 导入生成的 gRPC 代码
from proto import python_service_pb2
from proto import python_service_pb2_grpc

# 导入现有工具
import sim_plugin
from utils.command_runner import RunExe
from utils.json_utils import json_to_xml
from vo.sim_data_vo import SimInfo
from config import settings

# 配置日志
logger = logging.getLogger(__name__)

# 缓存目录
CACHE_DIR = Path("cache/")
CACHE_DIR.mkdir(exist_ok=True)

# 仿真进程目录
SIMENG_DIR = Path("SimEngPI/")
SIMENG_DIR.mkdir(exist_ok=True)

# 插件目录
PLUGIN_DIR = Path("plugins/")
PLUGIN_DIR.mkdir(exist_ok=True)

# 日志目录
LOG_HOME = settings.log_home

# 客户端Socket IP
client_socket_ip = settings.client_socket_ip

# 存储仿真实例信息
id_infos = defaultdict(SimInfo)


class PythonServiceServicer(python_service_pb2_grpc.PythonServiceServicer):
    """仿真服务的gRPC实现 - 对应Java端的SimulationPythonGrpcClient调用"""

    async def CreateSimeng(
        self,
        request: python_service_pb2.CreateSimengRequest,
        context: grpc.aio.ServicerContext
    ) -> python_service_pb2.ApiResponse:
        """
        创建仿真引擎
        
        Args:
            request: 包含仿真信息、控制视图和用户ID的请求
            context: gRPC上下文
            
        Returns:
            API响应
        """
        try:
            user_id = request.userId
            sim_info = request.simInfo
            control_views = request.controlViews

            logger.info(f"CreateSimeng request - user_id: {user_id}")
            
            # 提取仿真信息
            cur_sim_name = sim_info.name
            map_xml_name = sim_info.mapXmlName
            map_xml_path = sim_info.mapXmlPath
            fixed_od = sim_info.fixedOd
            
            logger.info(f"Simulation name: {cur_sim_name}, map: {map_xml_name}")
            
            # 更新仿真实例信息
            id_infos[user_id].name = cur_sim_name
            id_infos[user_id].sim_info = {
                'name': cur_sim_name,
                'map_xml_name': map_xml_name,
                'map_xml_path': map_xml_path,
                'fixed_od': self._convert_fixed_od(fixed_od)
            }
            id_infos[user_id].control_views = [
                {'use_plugin': cv.usePlugin, 'active_plugin': cv.activePlugin}
                for cv in control_views
            ]

            # 创建仿真文件目录
            cur_sim_files_dir = SIMENG_DIR / user_id
            id_infos[user_id].sim_dir = str(cur_sim_files_dir)
            cur_sim_files_dir.mkdir(exist_ok=True)

            # 创建插件目录
            cur_sim_plugin_dir = cur_sim_files_dir / "plugins"
            cur_sim_plugin_dir.mkdir(exist_ok=True)

            # 复制路网文件
            # 支持两种路径格式：
            # 1. 相对路径：{userId}/{xmlFileName}（从地图转换服务返回）
            # 2. 文件名：仅文件名（兼容旧版）
            if map_xml_path and os.path.sep in map_xml_path:
                # 相对路径格式：userId/{xmlFileName}
                cur_user_sim_xml_path = CACHE_DIR / map_xml_path
            else:
                # 文件名格式：直接使用 map_xml_name
                cur_user_sim_xml_path = CACHE_DIR / user_id / map_xml_name
            
            if cur_user_sim_xml_path.exists():
                # 复制文件到仿真目录
                shutil.copy(cur_user_sim_xml_path, cur_sim_files_dir)
                id_infos[user_id].map_xml_path = str(cur_user_sim_xml_path)
                logger.info(f"Map file copied successfully: {cur_user_sim_xml_path}")
            else:
                logger.warning(f"Map file not found: {cur_user_sim_xml_path}")
                # 如果找不到文件，尝试使用map_xml_path作为文件名
                cur_user_sim_xml_path = CACHE_DIR / user_id / map_xml_path
                if cur_user_sim_xml_path.exists():
                    shutil.copy(cur_user_sim_xml_path, cur_sim_files_dir)
                    id_infos[user_id].map_xml_path = str(cur_user_sim_xml_path)
                    logger.info(f"Map file copied successfully using map_xml_path as filename: {cur_user_sim_xml_path}")
                else:
                    logger.error(f"Map file not found in any format: map_xml_path={map_xml_path}, map_xml_name={map_xml_name}")

            # 创建 OD 文件
            od_xml_file_path = cur_sim_files_dir / "od.xml"
            od_xml_file_path.touch(exist_ok=True)

            # 写入 OD 文件
            od_content = self._generate_od_xml(fixed_od)
            with od_xml_file_path.open(mode='w', encoding='utf-8') as f:
                f.write(od_content)

            # 解析插件设置并复制
            cur_use_plugin = []
            for cv in control_views:
                if cv.usePlugin:
                    cur_use_plugin.append(cv.activePlugin)
            
            unique_use_plugin = list(set(cur_use_plugin))
            if unique_use_plugin:
                copy_result = sim_plugin.copy_plugin(unique_use_plugin, str(cur_sim_plugin_dir))
                if not copy_result:
                    return python_service_pb2.ApiResponse(
                        res="ERR_FILE",
                        msg="copy plugin files error",
                        data=""
                    )

            # 启动引擎
            arg_plugin = ""
            if len(cur_use_plugin) == 0:
                arg_plugin = "--noplugin"
            else:
                py_home = os.path.join(os.getcwd(), 'pyenv')
                arg_plugin = f'--pyhome="{py_home}"'

            arg_sid = f"--sid={cur_sim_name}"
            arg_simfile = f"--sfile={user_id}"
            # 使用实际的地图文件名称，而不是map_xml_name（mapId）
            actual_map_filename = Path(cur_user_sim_xml_path).name
            arg_roadfile = f"--road={actual_map_filename}"
            # 引擎直接连接 Java 后端的 WebSocket，使用 Java 后端端口 3822
            backend_host = settings.backend_host
            backend_port = 3822

            sim_cmd = [
                './SimEngPI/SimulationEngine.exe',
                '--log=0',
                arg_sid,
                arg_simfile,
                arg_roadfile,
                f'--ip={backend_host}',
                f'--port={backend_port}',  # Java后端端口 3822
                arg_plugin   # 使用正确的插件参数
            ]
            
            user_log_file = f"{LOG_HOME}{user_id}.txt"
            logger.info(f"Simulation command: {' '.join(sim_cmd)}")
            logger.info(f"User log file: {user_log_file}")
            
            res = await RunExe(sim_cmd, log_file=user_log_file, output_mode="file")
            logger.info(f"Simulation engine started with result: {res}")

            # 检查引擎启动结果
            if isinstance(res, str) and ("成功" in res or "ok" in res.lower()):
                # 引擎启动成功
                return python_service_pb2.ApiResponse(
                    res="ERR_OK",
                    msg="Simulation engine started successfully",
                    data=cur_sim_name
                )
            else:
                # 引擎启动失败
                return python_service_pb2.ApiResponse(
                    res="ERR_ENGINE",
                    msg=f"Failed to start simulation engine: {res}",
                    data=cur_sim_name
                )

        except Exception as e:
            logger.error(f"CreateSimeng error: {e}", exc_info=True)
            return python_service_pb2.ApiResponse(
                res="ERR_EXCEPTION",
                msg=str(e),
                data=""
            )

    async def ControlGreenRatio(
        self,
        request: python_service_pb2.GreenRatioControlRequest,
        context: grpc.aio.ServicerContext
    ) -> python_service_pb2.ApiResponse:
        """
        绿信比控制
        
        Args:
            request: 包含绿信比值的请求
            context: gRPC上下文
            
        Returns:
            API响应
        """
        try:
            green_ratio = request.greenRatio
            logger.info(f"ControlGreenRatio request - green_ratio: {green_ratio}")
            
            # TODO: 实现实际的绿信比控制逻辑
            # 这里需要与正在运行的仿真引擎通信
            
            return python_service_pb2.ApiResponse(
                res="ERR_OK",
                msg=f"Green ratio set to {green_ratio}",
                data=str(green_ratio)
            )

        except Exception as e:
            logger.error(f"ControlGreenRatio error: {e}", exc_info=True)
            return python_service_pb2.ApiResponse(
                res="ERR_EXCEPTION",
                msg=str(e),
                data=""
            )

    async def TestConnection(
        self,
        request: python_service_pb2.Empty,
        context: grpc.aio.ServicerContext
    ) -> python_service_pb2.TestResponse:
        """
        测试连接
        
        Args:
            request: 空请求
            context: gRPC上下文
            
        Returns:
            测试响应
        """
        logger.info("TestConnection request received")
        return python_service_pb2.TestResponse(
            connected=True,
            message="Python gRPC service is running"
        )

    def _convert_fixed_od(self, fixed_od) -> dict:
        """将 gRPC FixedOD 转换为字典格式"""
        result = {'od': [], 'sg': []}
        
        for origin in fixed_od.od:
            origin_dict = {
                'originId': origin.originId,
                'dist': [
                    {'destId': d.destId, 'rate': d.rate}
                    for d in origin.dist
                ]
            }
            result['od'].append(origin_dict)
        
        for sg in fixed_od.sg:
            sg_dict = {
                'crossId': sg.crossId,
                'cycleTime': sg.cycleTime,
                'ewStraight': sg.ewStraight,
                'snStraight': sg.snStraight,
                'snLeft': sg.snLeft
            }
            result['sg'].append(sg_dict)
        
        return result

    def _generate_od_xml(self, fixed_od) -> str:
        """生成 OD XML 内容"""
        # 转换为 JSON 格式
        convert_od_json = self._convert_fixed_od(fixed_od)
        
        # 调整格式
        correct_origin_fmt = {"orgin": []}
        for origin in convert_od_json['od']:
            correct_origin_fmt["orgin"].append(origin)
        convert_od_json['od'] = correct_origin_fmt
        
        correct_signal_fmt = {"signal": []}
        for signal in convert_od_json['sg']:
            correct_signal_fmt["signal"].append(signal)
        convert_od_json['sg'] = correct_signal_fmt
        
        # JSON 转 XML
        od_content = json_to_xml(convert_od_json)
        if od_content.startswith("<?xml"):
            od_content = "\n".join(od_content.splitlines()[1:])
        
        # 替换命名
        replace_dict = {
            "road_num>": "roadNum>",
            "lane_num>": "laneNum>",
            "controller_num>": "controllerNum>",
            "follow_model>": "vehicleFollowModelNum>",
            "change_lane_model>": "vehicleChangeLaneModelNum>",
            "flows>": "flow>",
            "road_id>": "roadID>",
            "od>": "OD>",
            "orgin_id>": "orginID>",
            "sg>": "SG>",
            "cross_id>": "crossID>",
            "cycle_time>": "cycleTime>",
            "ew_left>": "ewLeft>",
            "ew_straight>": "ewStraight>",
            "sn_left>": "snLeft>",
            "sn_straight>": "snStraight>"
        }
        
        for old, new in replace_dict.items():
            od_content = od_content.replace(old, new)
        
        return od_content

    # ========== 插件管理接口 ==========

    async def GetPluginInfo(
        self,
        request: python_service_pb2.GetPluginInfoRequest,
        context: grpc.aio.ServicerContext
    ) -> python_service_pb2.GetPluginInfoResponse:
        """
        获取插件信息
        
        Args:
            request: 包含可选的插件名称的请求
            context: gRPC上下文
            
        Returns:
            插件信息响应
        """
        try:
            plugin_name = request.pluginName if request.pluginName else None
            logger.info(f"GetPluginInfo request - plugin_name: {plugin_name}")
            
            if plugin_name:
                # 获取指定插件
                plugin = sim_plugin.get_plugin_info(plugin_name)
                if plugin is None:
                    return python_service_pb2.GetPluginInfoResponse(
                        res="ERR_NOT_FOUND",
                        msg=f"Plugin not found: {plugin_name}",
                        plugins=[]
                    )
                plugins = [plugin]
            else:
                # 获取所有插件
                plugins = sim_plugin.get_plugin_info()
            
            # 转换为 gRPC 消息格式
            plugin_infos = []
            for p in plugins:
                plugin_info = python_service_pb2.PluginInfo(
                    name=p.storage_dir,
                    manifestJson=json.dumps(p.manifest_content, ensure_ascii=False)
                )
                plugin_infos.append(plugin_info)
            
            logger.info(f"GetPluginInfo success - found {len(plugin_infos)} plugins")
            return python_service_pb2.GetPluginInfoResponse(
                res="ERR_OK",
                msg="get plugin info ok",
                plugins=plugin_infos
            )

        except Exception as e:
            logger.error(f"GetPluginInfo error: {e}", exc_info=True)
            return python_service_pb2.GetPluginInfoResponse(
                res="ERR_EXCEPTION",
                msg=str(e),
                plugins=[]
            )

    async def UploadPlugin(
        self,
        request: python_service_pb2.UploadPluginRequest,
        context: grpc.aio.ServicerContext
    ) -> python_service_pb2.ApiResponse:
        """
        上传插件
        
        Args:
            request: 包含文件名和文件内容的请求
            context: gRPC上下文
            
        Returns:
            API响应
        """
        try:
            file_name = request.fileName
            file_content = request.fileContent
            
            logger.info(f"UploadPlugin request - file_name: {file_name}, size: {len(file_content)} bytes")
            
            # 验证文件类型（简单检查zip魔数）
            if not file_content.startswith(b'PK'):
                return python_service_pb2.ApiResponse(
                    res="ERR_FILE",
                    msg="not a zip file",
                    data=""
                )
            
            # 读取ZIP文件
            with ZipFile(BytesIO(file_content)) as zip_file:
                # 获取根目录下的文件和文件夹列表
                root_files = [f for f in zip_file.namelist() if '/' not in f.strip('/')]
                # 检查根目录下是否存在 .json 文件
                json_files = [f for f in root_files if f.endswith(".json")]
                if len(json_files) != 1:
                    return python_service_pb2.ApiResponse(
                        res="ERR_CONTENT",
                        msg="not find or find more manifest file",
                        data=""
                    )

                # 检测是否存在同名文件夹
                plugin_name = Path(json_files[0]).stem
                plugin_parent_dir = PLUGIN_DIR / plugin_name
                if plugin_parent_dir.exists() and plugin_parent_dir.is_dir():
                    return python_service_pb2.ApiResponse(
                        res="ERR_EXIST",
                        msg="a plugin folder with the same name already exists",
                        data=""
                    )

                # 解压到指定目录
                zip_file.extractall(plugin_parent_dir)

                # 将描述文件添加到内存中
                sim_plugin.ope_plugin(plugin_name)
            
            logger.info(f"UploadPlugin success - plugin: {plugin_name}")
            return python_service_pb2.ApiResponse(
                res="ERR_OK",
                msg="upload plugin ok",
                data=plugin_name
            )

        except Exception as e:
            logger.error(f"UploadPlugin error: {e}", exc_info=True)
            return python_service_pb2.ApiResponse(
                res="ERR_EXCEPTION",
                msg=str(e),
                data=""
            )

    async def UpdatePluginInfo(
        self,
        request: python_service_pb2.UpdatePluginInfoRequest,
        context: grpc.aio.ServicerContext
    ) -> python_service_pb2.ApiResponse:
        """
        更新插件信息
        
        Args:
            request: 包含插件名、更新内容和是否写盘的请求
            context: gRPC上下文
            
        Returns:
            API响应
        """
        try:
            plugin_name = request.pluginName
            update_infos_json = request.updateInfosJson
            apply_disk = request.applyDisk
            
            logger.info(f"UpdatePluginInfo request - plugin: {plugin_name}, apply_disk: {apply_disk}")
            
            # 解析 JSON
            update_infos = json.loads(update_infos_json)
            
            # 调用插件更新方法
            result = sim_plugin.update_plugin_info(plugin_name, update_infos, apply_disk)
            
            if result:
                logger.info(f"UpdatePluginInfo success - plugin: {plugin_name}")
                return python_service_pb2.ApiResponse(
                    res="ERR_OK",
                    msg="update plugin info ok",
                    data=""
                )
            else:
                return python_service_pb2.ApiResponse(
                    res="ERR_FAIL",
                    msg="update plugin info failed",
                    data=""
                )

        except Exception as e:
            logger.error(f"UpdatePluginInfo error: {e}", exc_info=True)
            return python_service_pb2.ApiResponse(
                res="ERR_EXCEPTION",
                msg=str(e),
                data=""
            )
