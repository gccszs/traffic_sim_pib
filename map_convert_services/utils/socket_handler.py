import time


async def handle_frontend_message(websocket, cookie_id, data):
    """处理从前端转发过来的消息"""
    try:
        ope = data.get('ope', '')

        if ope == 'start':
            # 开始仿真
            await handle_start_simulation(websocket, cookie_id, data)
        elif ope == 'stop':
            # 停止仿真
            await handle_stop_simulation(websocket, cookie_id, data)
        elif ope == 'pause':
            # 暂停仿真
            await handle_pause_simulation(websocket, cookie_id, data)
        elif ope == 'set_params':
            # 设置参数
            await handle_set_parameters(websocket, cookie_id, data)
        else:
            # 其他前端操作
            await handle_other_operations(websocket, cookie_id, data)

    except Exception as e:
        print(f"Error handling frontend message: {str(e)}")
        error_msg = {
            "type": "eng",
            "ope": "err",
            "time": time.time(),
            "data": {"msg": f"Error processing message: {str(e)}"}
        }
        await websocket.send_json(error_msg)


async def handle_backend_message(websocket, cookie_id, data):
    """处理Java后端的控制消息"""
    ope = data.get('ope', '')

    if ope == 'hello':
        # 响应Java后端的握手
        hi_msg = {
            "type": "eng",
            "ope": "hi",
            "time": time.time()
        }
        await websocket.send_json(hi_msg)
        print(f"Sent hi message to Java backend for session: {cookie_id}")

    elif ope == 'status_check':
        # 状态检查
        status_msg = {
            "type": "eng",
            "ope": "status",
            "time": time.time(),
            "data": {"status": "running"}  # 根据实际状态调整
        }
        await websocket.send_json(status_msg)

    else:
        print(f"Unknown backend operation: {ope}")


async def handle_start_simulation(websocket, cookie_id, data):
    """处理开始仿真"""
    print(f"Starting simulation for session: {cookie_id}")

    # 发送确认消息给Java后端
    ack_msg = {
        "type": "frontend",
        "ope": "simulation_started",
        "time": time.time(),
        "data": {"session_id": cookie_id}
    }
    await websocket.send_json(ack_msg)

    # 这里添加实际的仿真启动逻辑
    # 例如：start_msg = {...}
    # await websocket.send_json(start_msg)

async def handle_stop_simulation(websocket, cookie_id, data):
    """处理停止仿真"""
    print(f"Stopping simulation for session: {cookie_id}")

    stop_msg = {
        "type": "frontend",
        "ope": "simulation_stopped",
        "time": time.time(),
        "data": {"session_id": cookie_id}
    }
    await websocket.send_json(stop_msg)


async def handle_pause_simulation(websocket, cookie_id, data):
    """处理暂停仿真"""
    print(f"Pausing simulation for session: {cookie_id}")

    pause_msg = {
        "type": "frontend",
        "ope": "simulation_paused",
        "time": time.time(),
        "data": {"session_id": cookie_id}
    }
    await websocket.send_json(pause_msg)


async def handle_set_parameters(websocket, cookie_id, data):
    """处理参数设置"""
    parameters = data.get('data', {})
    print(f"Setting parameters for session {cookie_id}: {parameters}")

    # 应用参数并返回确认
    ack_msg = {
        "type": "frontend",
        "ope": "parameters_set",
        "time": time.time(),
        "data": {"session_id": cookie_id, "parameters": parameters}
    }
    await websocket.send_json(ack_msg)


async def handle_other_operations(websocket, cookie_id, data):
    """处理其他操作"""
    ope = data.get('ope', '')
    operation_data = data.get('data', {})

    print(f"Handling operation '{ope}' for session {cookie_id} with data: {operation_data}")

    # 根据具体操作类型处理
    response_msg = {
        "type": "frontend",
        "ope": f"{ope}_response",
        "time": time.time(),
        "data": {"session_id": cookie_id, "result": "success"}
    }
    await websocket.send_json(response_msg)