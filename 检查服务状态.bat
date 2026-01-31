@echo off
chcp 65001 >nul
echo ============================================================
echo    Traffic Sim 服务状态检查
echo ============================================================
echo.

echo [1] 检查 Java 后端 (端口 3822)
echo ----------------------------------------
netstat -ano | findstr :3822
if errorlevel 1 (
    echo [❌] Java 后端未运行
    echo.
    echo 解决方法: 运行 "启动Java后端.bat"
) else (
    echo [✅] Java 后端正在运行
)
echo.

echo [2] 检查 Python 仿真服务 (端口 50051)
echo ----------------------------------------
netstat -ano | findstr :50051
if errorlevel 1 (
    echo [❌] Python 仿真服务未运行
    echo.
    echo 解决方法: cd map_convert_services ^&^& start.bat
) else (
    echo [✅] Python 仿真服务正在运行
)
echo.

echo [3] 检查 Python 地图服务 (端口 50052)
echo ----------------------------------------
netstat -ano | findstr :50052
if errorlevel 1 (
    echo [❌] Python 地图服务未运行
    echo.
    echo 解决方法: cd map_convert_services ^&^& start.bat
) else (
    echo [✅] Python 地图服务正在运行
)
echo.

echo [4] 检查 MySQL (端口 3306)
echo ----------------------------------------
netstat -ano | findstr :3306
if errorlevel 1 (
    echo [❌] MySQL 未运行
    echo.
    echo 解决方法: cd infrastructure ^&^& docker-compose up -d mysql
) else (
    echo [✅] MySQL 正在运行
)
echo.

echo [5] 检查 MongoDB (端口 27017)
echo ----------------------------------------
netstat -ano | findstr :27017
if errorlevel 1 (
    echo [⚠️] MongoDB 未运行 (可选)
    echo.
    echo 解决方法: cd infrastructure ^&^& docker-compose up -d mongodb
) else (
    echo [✅] MongoDB 正在运行
)
echo.

echo [6] 检查 Redis (端口 6379)
echo ----------------------------------------
netstat -ano | findstr :6379
if errorlevel 1 (
    echo [⚠️] Redis 未运行 (可选)
    echo.
    echo 解决方法: cd infrastructure ^&^& docker-compose up -d redis
) else (
    echo [✅] Redis 正在运行
)
echo.

echo ============================================================
echo    检查完成
echo ============================================================
echo.
echo 提示: [✅] 表示正常, [❌] 表示必须启动, [⚠️] 表示可选
echo.

pause
