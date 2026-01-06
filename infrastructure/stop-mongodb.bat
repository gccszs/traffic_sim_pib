@echo off
REM MongoDB 单独停止脚本 (Windows)

echo ==========================================
echo MongoDB 数据库停止脚本
echo ==========================================

REM 停止 MongoDB 服务
echo 正在停止 MongoDB 服务...
docker-compose -f docker-compose.mongodb.yml stop

echo.
echo ==========================================
echo [成功] MongoDB 服务已停止
echo ==========================================
echo.
echo 提示: 使用 'docker-compose -f docker-compose.mongodb.yml down' 可以删除容器
echo 提示: 使用 'docker-compose -f docker-compose.mongodb.yml down -v' 可以删除容器和数据卷（⚠️ 危险）
pause



