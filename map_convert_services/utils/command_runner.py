import subprocess
from typing import List


def RunExe(cmd: List[str], wait: bool = False) -> str:
    """启动一个进程

    Args:
        cmd (List[str]): 启动命令 例如['python3', 'your_script.py', 'arg1', 'arg2']
        wait (bool, optional): 是否阻塞等待并返回进程输出. Defaults to False.

    Returns:
        str: 进程输出
    """
    process = None
    if wait:
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   creationflags=subprocess.CREATE_NEW_CONSOLE)
        stdout, stderr = process.communicate()  #等待进程结束
        return "stdout: " + stdout + "\nstderr: " + stderr
    else:
        process = subprocess.Popen(cmd, creationflags=subprocess.CREATE_NEW_CONSOLE)
    return "ok"
