#!/bin/bash

# 交通仿真系统基础设施启动脚本

echo "=========================================="
echo "交通仿真系统基础设施启动脚本"
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

# 启动服务
echo "正在启动服务..."
docker-compose up -d

# 等待服务启动
echo ""
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "=========================================="
echo "服务状态:"
echo "=========================================="
docker-compose ps

echo ""
echo "=========================================="
echo "服务访问地址:"
echo "=========================================="
echo "MySQL:        localhost:3306"
echo "MongoDB:      localhost:27017"
echo "Redis:        localhost:6379"
echo "Kafka:        localhost:9092"
echo ""
echo "管理工具:"
echo "phpMyAdmin:   http://localhost:8083"
echo "Mongo Express: http://localhost:8084"
echo "Redis Commander: http://localhost:8082"
echo "Kafka UI:     http://localhost:8081"
echo ""
echo "=========================================="
echo "✅ 所有服务已启动"
echo "=========================================="

