import json
from dataclasses import dataclass, field
from typing import List, Union
from pathlib import Path
import shutil


@dataclass
class PluginInfo:
    storage_dir: str = ""  # 插件存放目录(相对于顶级插件目录, 所以也是插件名)
    manifest_content: dict = field(default_factory=dict)  # 插件描述文件的内容 json格式


@dataclass
class PluginManage:
    root_path: str = ""  # 存放插件目录的顶级目录
    plugin_all: List[PluginInfo] = field(default_factory=list)  # 每一个插件


plugin_manage: PluginManage = PluginManage()


def get_plugin_info(name: str = None) -> Union[List[PluginInfo], PluginInfo]:
    """获取所有插件信息

    Returns:
        List[PluginInfo] | PluginInfo: 插件信息(列表)
    """
    if (name == None):
        plugin_all = plugin_manage.plugin_all
        return plugin_all.copy()  # 返回浅拷贝 不影响原值

    p_info = None
    for info in plugin_manage.plugin_all:
        if info.storage_dir == name:
            p_info = info
            break
    return p_info


def init_plugin_info(plugin_dir: str) -> bool:
    """从plugin_dir中初始化插件信息

    Args:
        plugin_dir (str): 插件目录

    Returns:
        bool: 是否获取成功
    """
    plugin_path = Path(plugin_dir)
    if (not plugin_path.exists()):
        return False

    # 更新插件顶级目录
    plugin_manage.root_path = plugin_dir
    plugin_manage.plugin_all = []

    # 拿到plugins文件夹下的所有文件夹的Path
    plugin_folders = [item for item in Path(plugin_path).iterdir() if item.is_dir()]

    plugin_all = []
    for plugin_p in plugin_folders:
        manifest_file_name = plugin_p.name + ".json"
        manifest_file_path = plugin_p / manifest_file_name
        if manifest_file_path.exists():
            plugin_info = PluginInfo()
            plugin_info.storage_dir = plugin_p.name
            with manifest_file_path.open(mode="r", encoding='utf-8') as file:
                plugin_info.manifest_content = json.load(file)
            plugin_all.append(plugin_info)
    plugin_manage.plugin_all = plugin_all

    return True


def ope_plugin(plugin_name: str, ope_del: bool = False) -> bool:
    """增加或删除一个插件从plugin_manage中

    Args:
        plugin_name (str): 插件名
        ope_del (bool, optional): 是否是删除操作. Defaults to False.

    Returns:
        bool: _description_
    """
    if not ope_del:  # 增加一个插件(到内存中)
        cur_all_names = [plugin.storage_dir for plugin in plugin_manage.plugin_all]
        if plugin_name in cur_all_names:
            return False  # 存在同名的

        manifest_file_name = plugin_name + ".json"
        manifest_file_path = Path(plugin_manage.root_path + "/" + plugin_name + "/" + manifest_file_name)
        if manifest_file_path.exists():
            plugin_info = PluginInfo()
            plugin_info.storage_dir = plugin_name
            with manifest_file_path.open(mode="r", encoding='utf-8') as file:
                plugin_info.manifest_content = json.load(file)
            plugin_manage.plugin_all.append(plugin_info)
        else:  # 插件目录下不存在plugin_name的插件
            return False
    else:  # 删除一个插件(从内存中)
        plugin_manage.plugin_all = [plugin for plugin in plugin_manage.plugin_all if plugin.storage_dir != plugin_name]
    return True


def update_plugin_info(plugin_name: str, update_infos: List[dict], apply_disk: bool = False) -> bool:
    """更新某个插件的信息

    Args:
        plugin_name (str): 插件名
        update_infos (dict): [{"type": "enable_main", "enable": true}, {"type": "pv", "frequency": 0, "enable": true}, ..] 只允许更新control和enable_main字段
        apply_disk (bool): 是否写入到磁盘中

    Returns:
        bool: 是否操作成功
    """
    # 先根据plugin_name拿到插件信息
    cur_plugin = None
    for plugin_info in plugin_manage.plugin_all:
        if plugin_info.storage_dir == plugin_name:
            cur_plugin = plugin_info
            break
    if not cur_plugin:
        return False  # 未找到

    for update_info in update_infos:
        if "type" not in update_info:
            continue

        update_type = update_info["type"]
        if update_type == "enable_main":
            cur_plugin.manifest_content["enable_main"] = update_info["enable"]
        elif update_type in cur_plugin.manifest_content["control"].keys():
            if "frequency" in update_info.keys():
                cur_plugin.manifest_content["control"][update_type]["frequency"] = update_info["frequency"]
            if "enable" in update_info.keys():
                cur_plugin.manifest_content["control"][update_type]["enable"] = update_info["enable"]

    if apply_disk:
        manifest_file_path = Path(plugin_manage.root_path + "/" + cur_plugin.storage_dir + "/" + plugin_name + ".json")
        if manifest_file_path.exists():
            with manifest_file_path.open(mode="w", encoding='utf-8') as file:
                json.dump(cur_plugin.manifest_content, file)

    return True


def copy_plugin(name: Union[str, List[str]], new_folder: str) -> bool:
    """(拷贝)复制(多个)插件文件到一个新目录下

    Args:
        name (str): 插件名
        new_folder (str): 新目录

    Returns:
        bool: 是否复制成功
    """
    name_list = name
    if isinstance(name, str):
        name_list = [name]

    for one_name in name_list:
        p_info = get_plugin_info(one_name)
        if p_info is None:
            continue
        try:
            # 复制源文件夹到目标文件夹
            src = plugin_manage.root_path + "\\" + p_info.storage_dir
            dst = new_folder + "\\" + p_info.storage_dir
            shutil.copytree(src, dst, dirs_exist_ok=True)
        except FileExistsError as e:
            print("copy_plugin err: " + str(e))
            return False
        except Exception as e:
            print("copy_plugin err: " + str(e))
            return False
    return True


