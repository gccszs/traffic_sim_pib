@echo off
chcp 65001 >nul
echo ============================================================
echo    启动 Traffic Sim Java 后端服务
echo ============================================================
echo.

cd /d "%~dp0traffic-sim-server"

echo [1/4] 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Java，请先安装 JDK 17 或更高版本
    echo.
    echo 下载地址: https://adoptium.net/
    pause
    exit /b 1
)
echo [OK] Java 环境正常
echo.

echo [2/4] 检查端口 3822 是否被占用...
netstat -ano | findstr :3822 >nul
if not errorlevel 1 (
    echo [警告] 端口 3822 已被占用
    echo.
    echo 占用情况:
    netstat -ano | findstr :3822
    echo.
    set /p choice="是否要结束占用进程并继续？(Y/N): "
    if /i "%choice%"=="Y" (
        for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3822') do (
            echo 正在结束进程 %%a...
            taskkill /PID %%a /F
        )
        timeout /t 2 >nul
    ) else (
        echo 已取消启动
        pause
        exit /b 1
    )
)
echo [OK] 端口 3822 可用
echo.

echo [3/4] 检查 JAR 文件...
if not exist "target\traffic-sim-server-1.0.0-SNAPSHOT.jar" (
    echo [警告] 未找到编译好的 JAR 文件
    echo.
    echo 正在使用 Maven 编译项目...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo [错误] 编译失败，请检查错误信息
        pause
        exit /b 1
    )
)
echo [OK] JAR 文件存在
echo.

echo [4/4] 启动 Java 后端服务...
echo.
echo ============================================================
echo    服务信息
echo ============================================================
echo  端口: 3822
echo  API 路径: http://localhost:3822/api
echo  Swagger: http://localhost:3822/api/swagger-ui.html
echo  WebSocket (前端): ws://localhost:3822/api/ws/frontend
echo  WebSocket (引擎): ws://localhost:3822/api/ws/exe/{sessionId}
echo ============================================================
echo.
echo [提示] 按 Ctrl+C 可以停止服务
echo.

java -jar target\traffic-sim-server-1.0.0-SNAPSHOT.jar

pause
