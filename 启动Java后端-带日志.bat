@echo off
chcp 65001 >nul
cd /d "%~dp0traffic-sim-server"

echo ============================================================
echo    启动 Java 后端 - 日志输出到文件
echo ============================================================
echo.

set LOG_FILE=..\logs\java-backend-console.log
mkdir ..\logs 2>nul

echo [%date% %time%] Java 后端启动中... > %LOG_FILE%
echo.
echo 日志文件: %LOG_FILE%
echo 按 Ctrl+C 停止服务
echo.

java -jar target\traffic-sim-server-1.0.0-SNAPSHOT.jar 2>&1 | tee -Append %LOG_FILE%
