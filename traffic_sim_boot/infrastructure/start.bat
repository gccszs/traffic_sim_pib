@echo off
REM 交通仿真系统基础设施启动脚本 (Windows)

echo ==========================================
echo 交通仿真系统基础设施启动脚本
echo ==========================================

REM 检查 Docker 是否运行
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker 未运行，请先启动 Docker Desktop
    pause
    exit /b 1
)

REM 检查 Docker Compose 是否安装
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker Compose 未安装
    pause
    exit /b 1
)

echo [成功] Docker 环境检查通过
echo.

REM 启动服务
echo 正在启动服务...
docker-compose up -d

REM 等待服务启动
echo.
echo 等待服务启动...
timeout /t 10 /nobreak >nul

REM 检查服务状态
echo.
echo ==========================================
echo 服务状态:
echo ==========================================
docker-compose ps

echo.
echo ==========================================
echo 服务访问地址:
echo ==========================================
echo MySQL:        localhost:3306
echo MongoDB:      localhost:27017
echo Redis:        localhost:6379
echo Kafka:        localhost:9092
echo.
echo 管理工具:
echo phpMyAdmin:   http://localhost:8083
echo Mongo Express: http://localhost:8084
echo Redis Commander: http://localhost:8082
echo Kafka UI:     http://localhost:8081
echo.
echo ==========================================
echo [成功] 所有服务已启动
echo ==========================================
pause

