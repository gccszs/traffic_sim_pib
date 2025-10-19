# 定义请求数据模型，对应Java的CreateSimengRequest
from typing import Dict, Any, List

from pydantic import BaseModel


class CreateSimengRequest(BaseModel):
    simInfo: Dict[str, Any]  # 仿真信息
    controlViews: List[Dict[str, Any]]  # 控制视图列表
    userId: str  # 用户ID