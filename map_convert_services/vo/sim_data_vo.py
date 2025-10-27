from dataclasses import field
from typing import Optional

from pydantic import BaseModel
from starlette.websockets import WebSocket


class SimInfo:
    name:str = field(default='test', metadata={"description": "仿真实例名"})
    map_xml_name:str = field(default='', metadata={"description": "路网地图内部路径"})
    sim_dir:str = field(default='', metadata={"description": "仿真文件目录"})
    sim_info:dict = field(default_factory=dict, metadata={"description": "用户设置的仿真信息"})
    control_views:dict = field(default_factory=dict, metadata={"description": "用户设置的插件信息"})
    frontend_connection:Optional[WebSocket] = None
    simeng_connection:Optional[WebSocket] = None
    frontend_init_ok:Optional[bool] = False
    simeng_init_ok:Optional[bool] = False


