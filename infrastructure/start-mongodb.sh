#!/bin/bash

# MongoDB 单独启动脚本

echo "=========================================="
echo "MongoDB 数据库启动脚本"
echo "=========================================="

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ 错误: Docker 未运行，请先启动 Docker"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "❌ 错误: Docker Compose 未安装"
    exit 1
fi

echo "✅ Docker 环境检查通过"
echo ""

# 启动 MongoDB 服务
echo "正在启动 MongoDB 服务..."
docker-compose -f docker-compose.mongodb.yml up -d

# 等待服务启动
echo ""
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "=========================================="
echo "服务状态:"
echo "=========================================="
docker-compose -f docker-compose.mongodb.yml ps

echo ""
echo "=========================================="
echo "MongoDB 访问信息:"
echo "=========================================="
echo "MongoDB:       localhost:27017"
echo "数据库名:      traffic_sim"
echo "用户名:        root"
echo "密码:          root"
echo ""
echo "连接字符串:"
echo "mongodb://root:root@localhost:27017/traffic_sim?authSource=admin"
echo ""
echo "管理工具:"
echo "Mongo Express: http://localhost:8084"
echo "  用户名:      admin"
echo "  密码:        admin"
echo ""
echo "=========================================="
echo "✅ MongoDB 服务已启动"
echo "=========================================="



