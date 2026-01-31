import os
import shutil
import time
import logging
from collections import defaultdict
from io import BytesIO
from pathlib import Path
from typing import  Optional
from zipfile import ZipFile

from fastapi import UploadFile, File
from fastapi.responses import JSONResponse
import uvicorn
from fastapi import FastAPI
from pydantic import BaseModel
from starlette.websockets import WebSocketDisconnect, WebSocket

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
logger.info("Starting FastAPI Service...")
logger.info("=" * 60)

import sim_plugin
from utils.command_runner import RunExe
from utils.file_response import map_convert_to_binary, get_safe_path
from utils.json_utils import json_to_xml
from utils.socket_handler import handle_frontend_message, handle_backend_message
from vo.request_vo import CreateSimengRequest
from vo.sim_data_vo import SimInfo

# 导入配置
from config import settings

# 使用配置中的值
host = settings.host
LOG_HOME = settings.log_home
port = settings.port
client_socket_ip = settings.client_socket_ip

logger.info("Configuration loaded:")
logger.info(f"  - Host: {host}")
logger.info(f"  - Port: {port}")
logger.info(f"  - Client Socket IP: {client_socket_ip}")
logger.info(f"  - Log Home: {LOG_HOME}")

app = FastAPI()

logger.info("FastAPI application created successfully")

# 保存缓存的目录
CACHE_DIR = Path("cache/")
CACHE_DIR.mkdir(exist_ok=True)  # 创建文件夹，如果不存在
logger.info(f"Cache directory: {CACHE_DIR} (exists: {CACHE_DIR.exists()})")

# 存放插件的目录
PLUGIN_DIR = Path("plugins/")
PLUGIN_DIR.mkdir(exist_ok=True)
logger.info(f"Plugin directory: {PLUGIN_DIR} (exists: {PLUGIN_DIR.exists()})")

try:
    sim_plugin.init_plugin_info(str(PLUGIN_DIR))  # 初始化插件信息
    logger.info("Plugin initialization successful")
except Exception as e:
    logger.error(f"Plugin initialization failed: {e}")

# 仿真进程目录
SIMENG_DIR = Path("SimEngPI/")
SIMENG_DIR.mkdir(exist_ok=True)
logger.info(f"Simulation engine directory: {SIMENG_DIR} (exists: {SIMENG_DIR.exists()})")


@app.get("/test")
async def test():
    logger.info("Test endpoint called")
    return 'hello'


@app.get("/get_plugin_info")
async def get_plugin_info_all():
    """获取所有插件信息"""
    logger.info("Get all plugin info request")
    try:
        plugins = sim_plugin.get_plugin_info()
        # 转换为可序列化的格式
        plugin_list = []
        for p in plugins:
            plugin_list.append({
                "name": p.storage_dir,
                "manifest": p.manifest_content
            })
        return {
            "res": "ERR_OK",
            "msg": "get plugin info ok",
            "plugins": plugin_list
        }
    except Exception as e:
        logger.error(f"Get plugin info error: {e}", exc_info=True)
        return {
            "res": "ERR_FAIL",
            "msg": str(e)
        }


@app.get("/get_plugin_info/{plugin_name}")
async def get_plugin_info_by_name(plugin_name: str):
    """获取指定插件信息"""
    logger.info(f"Get plugin info request for: {plugin_name}")
    try:
        plugin = sim_plugin.get_plugin_info(plugin_name)
        if plugin is None:
            return {
                "res": "ERR_NOT_FOUND",
                "msg": f"Plugin not found: {plugin_name}"
            }
        return {
            "res": "ERR_OK",
            "msg": "get plugin info ok",
            "plugin": {
                "name": plugin.storage_dir,
                "manifest": plugin.manifest_content
            }
        }
    except Exception as e:
        logger.error(f"Get plugin info error: {e}", exc_info=True)
        return {
            "res": "ERR_FAIL",
            "msg": str(e)
        }


class UpdatePluginRequest(BaseModel):
    pluginName: str
    updateInfos: list
    applyDisk: bool = False

@app.post("/update_plugin_info")
async def update_plugin_info(request: UpdatePluginRequest):
    """更新插件信息"""
    logger.info(f"Update plugin info request: {request.pluginName}")
    try:
        result = sim_plugin.update_plugin_info(
            request.pluginName, 
            request.updateInfos, 
            request.applyDisk
        )
        if result:
            return {
                "res": "ERR_OK",
                "msg": "update plugin info ok"
            }
        else:
            return {
                "res": "ERR_FAIL",
                "msg": "update plugin info failed"
            }
    except Exception as e:
        logger.error(f"Update plugin info error: {e}", exc_info=True)
        return {
            "res": "ERR_FAIL",
            "msg": str(e)
        }


@app.post("/fileupload")
async def map_file_upload(upload_file: UploadFile, user_id: str):
    """文件上传和转换 - 返回二进制流"""
    try:
        logger.info(f"File upload request - user_id: {user_id}, file: {upload_file.filename}")
        workDir = str(CACHE_DIR / user_id)
        logger.info(f"Work directory: {workDir}")
        # 创建工作目录
        os.makedirs(workDir, exist_ok=True)
        # 安全的文件路径
        file_path = get_safe_path(workDir, upload_file.filename)

        # 保存上传文件
        with open(file_path, 'wb') as f:
            content = await upload_file.read()
            f.write(content)

        logger.info(f"File saved successfully: {file_path}")

        # 文件转换并返回二进制流
        result = await map_convert_to_binary(upload_file, workDir)
        logger.info("File conversion completed")
        return result
    except Exception as e:
        logger.error(f"File upload error: {e}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={
                "success": False,
                "error": str(e),
                "code": 500
            }
        )


from shared_state import id_infos  # 用于存储认证ID和仿真实例信息的对应关系

# 定义响应数据模型，对应Java的ApiResponse<String>
class ApiResponse(BaseModel):
    res: str  # 响应状态码，如 "ERR_OK"
    msg: str  # 响应消息
    addition: Optional[str] = None  # 附加数据（可选）
@app.post("/init_simeng", response_model=ApiResponse)
async def create_simeng(request: CreateSimengRequest):
    try:
        logger.info(f"Init simulation request - user_id: {request.userId}")
        sim_info = request.simInfo
        user_id = request.userId
        control_views = request.controlViews
        cur_sim_name = sim_info['name']
        logger.info(f"Simulation name: {cur_sim_name}")
        id_infos[user_id].name = cur_sim_name
        id_infos[user_id].sim_info = sim_info
        id_infos[user_id].control_views = control_views

        cur_sim_files_dir = SIMENG_DIR / user_id  # 存放当前仿真所需的文件(路网xml, OD, 插件文件等)
        id_infos[user_id].sim_dir = str(cur_sim_files_dir)
        cur_sim_files_dir.mkdir(exist_ok=True)

        cur_sim_plugin_dir = cur_sim_files_dir / "plugins"
        cur_sim_plugin_dir.mkdir(exist_ok=True)

        cur_user_sim_xml_path = CACHE_DIR / user_id / sim_info['map_xml_name']
        road_xml_file_path = Path(cur_user_sim_xml_path)
        shutil.copy(road_xml_file_path, cur_sim_files_dir)  # 创建路网文件
        id_infos[user_id].map_xml_path = road_xml_file_path


        od_xml_file_path = Path(cur_sim_files_dir) / "od.xml"
        od_xml_file_path.touch(exist_ok=True)  # 创建OD文件

        # 写入OD文件
        # 先修改传过来的json格式, 以对应需要的OD格式
        convert_od_json = sim_info['fixed_od']
        frontend_od = sim_info['fixed_od']
        correct_orgin_fmt = {"orgin": []}
        for orgin in frontend_od['od']:
            correct_orgin_fmt["orgin"].append(orgin)
        convert_od_json['od'] = correct_orgin_fmt

        correct_signal_fmt = {"signal": []}
        for signal in frontend_od['sg']:
            correct_signal_fmt["signal"].append(signal)
        convert_od_json['sg'] = correct_signal_fmt

        od_content = json_to_xml(convert_od_json)  # 前端od json转xml
        if od_content.startswith("<?xml"):  # 删除第一行
            od_content = "\n".join(od_content.splitlines()[1:])

        # 前端的OD数据命名和实际需要的有些不同 直接替换转一下
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
        }  # 省事了, 多个'>'就不会从字符串中间匹配了

        for old, new in replace_dict.items():
            od_content = od_content.replace(old, new)

        with od_xml_file_path.open(mode='w', encoding='utf-8') as f:
            f.write(od_content)

        # 解析插件设置并复制
        cur_use_plugin = []
        for c_setting in control_views:
            if c_setting['use_plugin']:
                cur_use_plugin.append(c_setting['active_plugin'])
        unique_use_plugin = list(set(cur_use_plugin))
        copy_result = sim_plugin.copy_plugin(unique_use_plugin, str(cur_sim_plugin_dir))
        if not copy_result:
            return {"res": "ERR_FILE", "msg": "copy plugin files error"}

            # 启动引擎 设置命令行参数
        arg_plugin = ""
        if len(cur_use_plugin) == 0:  # 没有使用插件
            arg_plugin = "--noplugin"
        else:
            py_home = os.path.join(os.getcwd(), 'pyenv')
            arg_plugin = "--pyhome=\"" + py_home + "\""
        arg_sid = "--sid=" + cur_sim_name
        arg_simfile = "--sfile=" + user_id
        arg_roadfile = "--road=" + Path(id_infos[user_id].map_xml_path).name
        
        # 引擎直接连接 Java 后端的 WebSocket（不再通过 Python 中介）
        backend_host = settings.backend_host  # Java 后端地址
        backend_port = settings.backend_port  # Java 后端 WebSocket 端口 (3822)
        
        # 构建 WebSocket URL：ws://backend_host:backend_port/ws/exe/{user_id}
        ws_path = f"{settings.backend_ws_path}/ws/exe/{user_id}".lstrip('/')
        ws_url = f"ws://{backend_host}:{backend_port}/{ws_path}"
        
        sim_cmd = ['./SimEngPI/SimulationEngine.exe', '--log=0', arg_sid, arg_simfile, arg_roadfile, 
                   f'--ip={backend_host}', f'--port={backend_port}', '--ws=' + ws_url, arg_plugin]
        user_log_file = LOG_HOME + '' + user_id + '.txt'
        logger.info(f"Simulation command: {' '.join(sim_cmd)}")
        logger.info(f"WebSocket URL: {ws_url}")
        logger.info(f"User log file: {user_log_file}")
        res = await RunExe(sim_cmd, log_file=user_log_file, output_mode="file")
        logger.info(f"Simulation engine started with result: {res}")

        return {"res": res, 'code': 200, "eng_msg": 'test_info', "msg": "ok"}
    except Exception as e:
        logger.error(f"Init simulation error: {e}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={
                "success": False,
                "error": str(e),
                "code": 500
            }
        )

@app.post("/upload_plugin")
async def upload_plugin(file: UploadFile = File(...)):
    """前端上传的是一个zip包, 此方法会检测zip目录结构

    Args:
        file (UploadFile, optional): plugin.zip. Defaults to File(...).
    """
    logger.info(f"Plugin upload request - file: {file.filename}, type: {file.content_type}")
    # 验证文件类型
    if file.content_type not in ["application/zip", "application/x-zip-compressed"]:
        return {"res": "ERR_FILE", "msg": "not a zip file"}

    # 读取ZIP文件
    contents = await file.read()
    with ZipFile(BytesIO(contents)) as zip_file:
        # 获取根目录下的文件和文件夹列表
        root_files = [f for f in zip_file.namelist() if '/' not in f.strip('/')]
        # 检查根目录下是否存在 .json 文件
        json_files = [f for f in root_files if f.endswith(".json")]
        if len(json_files) != 1:
            return {"res": "ERR_CONTENT", "msg": "not find or find more manifest file"}

        # 检测是否存在同名文件夹
        plugin_name = Path(json_files[0]).stem  # 要把插件存放在同名文件夹内
        plugin_parent_dir = PLUGIN_DIR / plugin_name
        if plugin_parent_dir.exists() and plugin_parent_dir.is_dir():
            return {"res": "ERR_EXIST", "msg": "a plugin folder with the same name already exists"}

        # 如果结构符合要求，解压到指定目录
        zip_file.extractall(plugin_parent_dir)

        # 将描述文件添加到内存中
        sim_plugin.ope_plugin(plugin_name)
    return {"res": "ERR_OK", "msg": "upload plugin ok"}




@app.websocket("/ws/exe/{exe_id}")
async def exe_websocket(websocket: WebSocket, exe_id: str):
    """
    WebSocket 端点 - 已废弃，保留用于兼容性
    
    注意：在新架构中，引擎直接连接到 Java 后端的 WebSocket (端口 3822)，
    不再连接到此 Python 服务的 WebSocket。
    
    此端点保留是为了：
    1. 向后兼容（如果有旧版引擎仍在使用）
    2. 测试和调试目的
    
    正常情况下，引擎应该连接到：ws://localhost:3822/ws/exe/{exe_id}
    """
    cookie_id = exe_id
    logger.warning(f"[DEPRECATED] WebSocket connection to Python service - exe_id: {exe_id}")
    logger.warning(f"[DEPRECATED] Engine should connect to Java backend at ws://localhost:3822/ws/exe/{exe_id}")
    await websocket.accept()
    
    # Store the WebSocket connection in the simulation info for later use
    if exe_id in id_infos:
        id_infos[exe_id].simeng_connection = websocket
        id_infos[exe_id].simeng_init_ok = True
        logger.info(f"Simulation engine connection stored for exe_id: {exe_id}")
    else:
        logger.warning(f"No simulation info found for exe_id: {exe_id}, creating new entry")
        id_infos[exe_id].simeng_connection = websocket
        id_infos[exe_id].simeng_init_ok = True
    
    try:
        while True:
            data = await websocket.receive_json()
            logger.debug(f"Received from simulation engine: {data}")

            if data['type'] == 'frontend':
                await handle_frontend_message(websocket, cookie_id, data)
            elif data['type'] == 'backend':
                await handle_backend_message(websocket, cookie_id, data)
            else:
                logger.warning(f"Unknown message type: {data['type']}")

    except WebSocketDisconnect as e:
        logger.info(f"WebSocket connection closed: Code={e.code}, Reason={e.reason}")
        # Clean up the connection reference when disconnected
        if exe_id in id_infos:
            id_infos[exe_id].simeng_connection = None
            id_infos[exe_id].simeng_init_ok = False
    except Exception as e:
        logger.error(f"WebSocket error: {str(e)}", exc_info=True)
        # Clean up the connection reference on error
        if exe_id in id_infos:
            id_infos[exe_id].simeng_connection = None
            id_infos[exe_id].simeng_init_ok = False


def get_current_timestamp_ms():
    """获取当前时间的毫秒时间戳"""
    return int(time.time() * 1000)




if __name__ == '__main__':
    logger.info("=" * 60)
    logger.info(f"Starting Uvicorn server on {host}:{port}")
    logger.info("=" * 60)
    uvicorn.run(app, host=host, port=port)
