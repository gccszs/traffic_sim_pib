import os
import shutil
from collections import defaultdict
from io import BytesIO
from pathlib import Path
from typing import Annotated, Dict, List, Optional
from zipfile import ZipFile

from fastapi import UploadFile, File, Cookie
from fastapi.responses import JSONResponse
import uvicorn
from fastapi import FastAPI
from httpcore import Request
from pydantic import BaseModel

from map_convert_services import sim_plugin
from map_convert_services.utils.command_runner import RunExe
from map_convert_services.utils.file_response import map_convert_to_binary, get_safe_path
from map_convert_services.utils.json_utils import json_to_xml
from map_convert_services.vo.request_vo import CreateSimengRequest
from map_convert_services.vo.sim_data_vo import SimInfo

app = FastAPI()

# 保存缓存的目录
CACHE_DIR = Path("cache/")
CACHE_DIR.mkdir(exist_ok=True)  # 创建文件夹，如果不存在
# 存放插件的目录
PLUGIN_DIR = Path("plugins/")
PLUGIN_DIR.mkdir(exist_ok=True)
sim_plugin.init_plugin_info(str(PLUGIN_DIR))  #初始化插件信息
# 仿真进程目录
SIMENG_DIR = Path("SimEngPI/")
SIMENG_DIR.mkdir(exist_ok=True)


@app.get("/test")
async def test():
    return 'hello'


@app.post("/fileupload")
async def map_file_upload(upload_file: UploadFile, user_id: str):
    """文件上传和转换 - 返回二进制流"""
    try:
        print(user_id)
        workDir = str(CACHE_DIR / user_id)
        print(workDir)
        # 创建工作目录
        os.makedirs(workDir, exist_ok=True)
        # 安全的文件路径
        file_path = get_safe_path(workDir, upload_file.filename)

        # 保存上传文件
        with open(file_path, 'wb') as f:
            content = await upload_file.read()
            f.write(content)

        # 文件转换并返回二进制流
        return await map_convert_to_binary(upload_file, workDir)
    except Exception as e:
        print(e)
        return JSONResponse(
            status_code=500,
            content={
                "success": False,
                "error": str(e),
                "code": 500
            }
        )


id_infos = defaultdict(SimInfo)  #用于存储认证ID和仿真实例信息的对应关系

# 定义响应数据模型，对应Java的ApiResponse<String>
class ApiResponse(BaseModel):
    res: str  # 响应状态码，如 "ERR_OK"
    msg: str  # 响应消息
    addition: Optional[str] = None  # 附加数据（可选）
@app.post("/init_simeng", response_model=ApiResponse)
async def create_simeng(request: CreateSimengRequest):
    # try:
        sim_info = request.simInfo
        print(sim_info)
        user_id = request.userId
        print(user_id)
        control_views = request.controlViews
        cur_sim_name = sim_info['name']
        id_infos[user_id].name = cur_sim_name
        id_infos[user_id].sim_info = sim_info
        id_infos[user_id].control_views = control_views

        cur_sim_files_dir = SIMENG_DIR / user_id  # 存放当前仿真所需的文件(路网xml, OD, 插件文件等)
        id_infos[user_id].sim_dir = str(cur_sim_files_dir)
        cur_sim_files_dir.mkdir(exist_ok=True)

        cur_sim_plugin_dir = cur_sim_files_dir / "plugins"
        cur_sim_plugin_dir.mkdir(exist_ok=True)

        print(id_infos[user_id].map_xml_name)

        cur_user_sim_xml_path = CACHE_DIR / user_id / id_infos[user_id].map_xml_name
        road_xml_file_path = Path(cur_user_sim_xml_path)
        print(road_xml_file_path)
        print(road_xml_file_path.name)
        shutil.copy(road_xml_file_path, cur_sim_files_dir)  # 创建路网文件



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
        arg_roadfile = "--road=" + Path(id_infos[user_id].map_xml_name).name
        sim_cmd = ['./SimEngPI/SimulationEngine.exe', '--log=0', arg_sid, arg_simfile, arg_roadfile, '--ip=127.0.0.1',
                   '--port=3822', "--noplugin"]  # debug用
        RunExe(sim_cmd)

        return {"res": "ERR_OK", "msg": "ok"}
    # except Exception as e:
    #     return JSONResponse(
    #         status_code=500,
    #         content={
    #             "success": False,
    #             "error": str(e),
    #             "code": 500
    #         }
    #     )

@app.post("/upload_plugin")
async def upload_plugin(file: UploadFile = File(...)):
    """前端上传的是一个zip包, 此方法会检测zip目录结构

    Args:
        file (UploadFile, optional): plugin.zip. Defaults to File(...).
    """
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


if __name__ == '__main__':
    uvicorn.run(app, host='210.41.102.17', port=8000)
