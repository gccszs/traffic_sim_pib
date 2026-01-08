# ============================================
# Traffic Simulation Server Dockerfile
# 多阶段构建 - 生产优化版本
# ============================================

# 构建阶段（可选，如果已经有 jar 包可以跳过）
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 复制 pom 文件
COPY pom.xml .
COPY traffic-sim-common/pom.xml traffic-sim-common/
COPY traffic-sim-server/pom.xml traffic-sim-server/
COPY plugins/pom.xml plugins/
COPY plugins/plugin-auth/pom.xml plugins/plugin-auth/
COPY plugins/plugin-user/pom.xml plugins/plugin-user/
COPY plugins/plugin-map/pom.xml plugins/plugin-map/
COPY plugins/plugin-simulation/pom.xml plugins/plugin-simulation/
COPY plugins/plugin-engine-manager/pom.xml plugins/plugin-engine-manager/
COPY plugins/plugin-statistics/pom.xml plugins/plugin-statistics/
COPY plugins/plugin-engine-replay/pom.xml plugins/plugin-engine-replay/

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -B

# 复制源码
COPY . .

# 构建
RUN mvn clean package -DskipTests -B

# ============================================
# 运行阶段
# ============================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="traffic-sim-team"
LABEL version="1.0.0"
LABEL description="Traffic Simulation Server"

# 安装必要工具
RUN apk add --no-cache \
    curl \
    tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# 创建应用用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /build/traffic-sim-server/target/traffic-sim-server-*.jar app.jar

# 如果已经有 jar 包，可以直接使用下面这行替代上面的 COPY
# COPY traffic-sim-server/target/traffic-sim-server-*.jar app.jar

# 创建日志和数据目录
RUN mkdir -p /app/logs /app/data /app/maps \
    && chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 环境变量
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=docker

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT:-3822}/api/auth/captcha || exit 1

# 暴露端口
EXPOSE 3822

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

