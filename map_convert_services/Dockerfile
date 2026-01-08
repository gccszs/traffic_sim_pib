FROM python:3.9-slim

# 安装 Wine（使用 Debian 自带的版本，更稳定）
RUN dpkg --add-architecture i386 && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
        wine \
        wine32 \
        wine64 \
        libwine \
        libwine:i386 \
        xvfb && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 拷贝项目文件
COPY . .

# 设置 Wine 环境变量
ENV WINEPREFIX=/root/.wine
ENV WINEARCH=win64
ENV WINEDEBUG=-all
ENV DISPLAY=:99

# 简化 Wine 初始化（跳过 wineboot，直接测试）
RUN echo "测试 Wine 安装..." && \
    wine --version && \
    echo "Wine 安装成功"

# 设置可执行权限
RUN chmod +x /app/SimEngPI/SimulationEngine.exe

# 安装 Python 依赖
RUN pip3 install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple/ --upgrade pip && \
    pip3 install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple/ -r requirements.txt

# 注意：这会在构建时设置环境变量，但通常我们更推荐在运行时设置

# 或者使用 ARG 在构建时传递
ARG APP_HOST=0.0.0.0
ARG APP_PORT=8000
ARG CLIENT_SOCKET_IP=192.168.1.212
ARG LOG_HOME=/app/engine_sim_logs/

# 将 ARG 转换为 ENV
# 设置环境变量
ENV APP_HOST=$APP_HOST
ENV APP_PORT=$APP_PORT
ENV CLIENT_SOCKET_IP=$CLIENT_SOCKET_IP
ENV LOG_HOME=$LOG_HOME

ENV SIM_LOG_DIR=/data/logs
ENV SIM_CONFIG_DIR=/data/config

# 创建并挂载持久化目录
RUN mkdir -p /data/logs /data/config
VOLUME ["/data/logs", "/data/config"]

# 暴露端口
EXPOSE $APP_PORT
EXPOSE 50052

# 安装 gRPC 依赖
RUN pip3 install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple/ grpcio grpcio-tools protobuf

# 启动脚本
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

# 默认启动 Python 服务
CMD ["/app/start.sh"]