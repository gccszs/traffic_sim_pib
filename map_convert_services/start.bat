@echo off
REM ============================================
REM Python Services Startup Script (Windows)
REM 启动以下服务:
REM 1. FastAPI (HTTP/WebSocket) - 用于引擎和Java后端的WebSocket连接
REM 2. gRPC Services - 包含两个服务:
REM    - MapConvertService (50052) - 地图转换服务
REM    - PythonService (50051) - 仿真引擎管理服务
REM ============================================

setlocal enabledelayedexpansion

REM 设置环境变量（如果需要自定义，请修改这里）
if not defined APP_HOST set APP_HOST=localhost
if not defined APP_PORT set APP_PORT=8000
if not defined BACKEND_HOST set BACKEND_HOST=localhost
if not defined BACKEND_PORT set BACKEND_PORT=3822
if not defined BACKEND_WS_PATH set BACKEND_WS_PATH=
if not defined MAP_GRPC_PORT set MAP_GRPC_PORT=50052
if not defined SIM_GRPC_PORT set SIM_GRPC_PORT=50051
if not defined LOG_HOME set LOG_HOME=./engine_sim_logs/

REM 日志目录
set LOG_DIR=service_logs
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo =========================================
echo Starting Traffic Simulation Python Services...
echo   - FastAPI (HTTP/WebSocket): port %APP_PORT%
echo   - MapConvertService (gRPC): port %MAP_GRPC_PORT%
echo   - PythonService (gRPC): port %SIM_GRPC_PORT%
echo.
echo Backend Configuration:
echo   - Backend Host: %BACKEND_HOST%
echo   - Backend Port: %BACKEND_PORT%
echo   - Engine will connect to: ws://%BACKEND_HOST%:%BACKEND_PORT%/ws/exe/{sessionId}
echo =========================================
echo.

REM 生成时间戳
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set TIMESTAMP=%datetime:~0,8%_%datetime:~8,6%

REM 日志文件路径
set FASTAPI_LOG=%LOG_DIR%\fastapi_%TIMESTAMP%.log
set GRPC_LOG=%LOG_DIR%\grpc_%TIMESTAMP%.log
set MAIN_LOG=%LOG_DIR%\main_%TIMESTAMP%.log

echo [%date% %time%] Starting services... >> "%MAIN_LOG%"
echo FastAPI log: %FASTAPI_LOG% >> "%MAIN_LOG%"
echo gRPC log: %GRPC_LOG% >> "%MAIN_LOG%"
echo ========================================= >> "%MAIN_LOG%"

REM 启动 FastAPI 服务
echo [%date% %time%] Starting FastAPI HTTP service on port %APP_PORT%...
echo [%date% %time%] Starting FastAPI HTTP service on port %APP_PORT%... >> "%MAIN_LOG%"
start "FastAPI Service" /B python web_app.py > "%FASTAPI_LOG%" 2>&1

REM 等待 FastAPI 启动
timeout /t 3 /nobreak > nul

echo [%date% %time%] FastAPI started successfully
echo [%date% %time%] FastAPI started successfully >> "%MAIN_LOG%"

REM 检查 gRPC 服务文件是否存在
if exist "grpc_server.py" (
    echo [%date% %time%] Starting gRPC services...
    echo   - MapConvertService on port %MAP_GRPC_PORT%
    echo   - PythonService on port %SIM_GRPC_PORT%
    echo [%date% %time%] Starting gRPC services... >> "%MAIN_LOG%"
    echo   - MapConvertService on port %MAP_GRPC_PORT% >> "%MAIN_LOG%"
    echo   - PythonService on port %SIM_GRPC_PORT% >> "%MAIN_LOG%"
    
    start "gRPC Service" /B python grpc_server.py > "%GRPC_LOG%" 2>&1
    
    REM 等待 gRPC 启动
    timeout /t 3 /nobreak > nul
    
    echo [%date% %time%] gRPC started successfully
    echo [%date% %time%] gRPC started successfully >> "%MAIN_LOG%"
) else (
    echo [%date% %time%] WARNING: gRPC server not found, skipping...
    echo [%date% %time%] WARNING: gRPC server not found, skipping... >> "%MAIN_LOG%"
)

echo.
echo =========================================
echo [%date% %time%] Services started successfully!
echo   - FastAPI log: %FASTAPI_LOG%
echo   - gRPC log: %GRPC_LOG%
echo   - Main log: %MAIN_LOG%
echo =========================================
echo.
echo NOTE: WebSocket service at /ws/exe/{exe_id} is handled by FastAPI
echo NOTE: Engine connects to Java backend at ws://%BACKEND_HOST%:%BACKEND_PORT%/ws/exe/{sessionId}
echo =========================================
echo.
echo Services are running. Press Ctrl+C to stop...
echo [%date% %time%] Services started successfully! >> "%MAIN_LOG%"

REM 保持窗口打开
pause
