# 基础设施部署文档

本目录包含交通仿真系统所需的所有数据持久化服务和中间件的 Docker Compose 配置。

## 📋 目录结构

```
infrastructure/
├── docker-compose.yml          # Docker Compose 主配置文件（所有服务）
├── docker-compose.mongodb.yml  # MongoDB 单独启动配置
├── init.sql                    # MySQL 数据库初始化脚本（建库建表）
├── README.md                   # 本文档
├── start.sh / start.bat        # 启动所有服务脚本
├── start-mongodb.sh / start-mongodb.bat  # 单独启动 MongoDB 脚本
├── stop.sh / stop.bat          # 停止所有服务脚本
├── stop-mongodb.sh / stop-mongodb.bat    # 单独停止 MongoDB 脚本
├── mysql/
│   └── conf/
│       └── my.cnf              # MySQL 配置文件
├── mongodb/
│   └── init-mongo.js           # MongoDB 初始化脚本
└── redis/
    └── redis.conf              # Redis 配置文件
```

## 🚀 快速开始

### 前置要求

- Docker Desktop 或 Docker Engine 20.10+
- Docker Compose 2.0+

### 一键启动所有服务

```bash
# 进入 infrastructure 目录
cd infrastructure

# 启动所有服务（后台运行）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f
```

### 停止服务

```bash
# 停止所有服务（保留数据）
docker-compose stop

# 停止并删除容器（保留数据卷）
docker-compose down

# 停止并删除容器和数据卷（⚠️ 危险：会删除所有数据）
docker-compose down -v
```

### 单独启动 MongoDB（适用于宿主机已有 MySQL 的情况）

如果您的 MySQL 数据库已经在宿主机上运行，只需要启动 MongoDB：

**Windows:**
```bash
# 使用脚本启动
start-mongodb.bat

# 或手动启动
docker-compose -f docker-compose.mongodb.yml up -d
```

**Linux/Mac:**
```bash
# 使用脚本启动
chmod +x start-mongodb.sh
./start-mongodb.sh

# 或手动启动
docker-compose -f docker-compose.mongodb.yml up -d
```

**停止 MongoDB:**
```bash
# Windows
stop-mongodb.bat

# Linux/Mac
./stop-mongodb.sh

# 或手动停止
docker-compose -f docker-compose.mongodb.yml stop
```

## 📦 服务说明

### 0. 单独启动 MongoDB

如果您只需要启动 MongoDB（MySQL 已在宿主机运行），可以使用：

- **配置文件**: `docker-compose.mongodb.yml`
- **启动脚本**: `start-mongodb.sh` / `start-mongodb.bat`
- **停止脚本**: `stop-mongodb.sh` / `stop-mongodb.bat`

该配置只包含 MongoDB 和 Mongo Express 管理工具，不会启动其他服务。

### 1. MySQL 数据库

- **容器名**: `traffic-sim-mysql`
- **端口**: `3306`
- **数据库名**: `traffic_sim`
- **用户名**: `root` / `traffic_sim`
- **密码**: `root` / `traffic_sim`
- **版本**: MySQL 8.0

**连接信息**:
```
Host: localhost
Port: 3306
Database: traffic_sim
Username: root
Password: root
```

**初始化**:
- 自动执行 `init.sql` 脚本创建数据库和表结构
- 包含用户、角色、权限、地图、仿真任务、回放任务等表

**管理工具**:
- phpMyAdmin: http://localhost:8083
  - 用户名: `root`
  - 密码: `root`

### 2. MongoDB 数据库

- **容器名**: `traffic-sim-mongodb`
- **端口**: `27017`
- **数据库名**: `traffic_sim`
- **用户名**: `root` / `traffic_sim`
- **密码**: `root` / `traffic_sim`
- **版本**: MongoDB 7.0

**连接信息**:
```
Host: localhost
Port: 27017
Database: traffic_sim
Username: root
Password: root
```

**连接字符串**:
```
mongodb://root:root@localhost:27017/traffic_sim?authSource=admin
```

**管理工具**:
- MongoDB Express: http://localhost:8084
  - 用户名: `admin`
  - 密码: `admin`

### 3. Redis 缓存

- **容器名**: `traffic-sim-redis`
- **端口**: `6379`
- **密码**: `redis123`
- **版本**: Redis 7.2

**连接信息**:
```
Host: localhost
Port: 6379
Password: redis123
```

**管理工具**:
- Redis Commander: http://localhost:8082

### 4. Kafka 消息队列

- **Zookeeper 容器名**: `traffic-sim-zookeeper`
- **Kafka 容器名**: `traffic-sim-kafka`
- **Zookeeper 端口**: `2181`
- **Kafka 端口**: `9092`
- **版本**: Kafka 7.5.0

**连接信息**:
```
Bootstrap Servers: localhost:9092
Zookeeper: localhost:2181
```

**管理工具**:
- Kafka UI: http://localhost:8081

## 🔧 配置说明

### MySQL 配置

配置文件位置: `mysql/conf/my.cnf`

主要配置项:
- 字符集: `utf8mb4`
- 最大连接数: `1000`
- InnoDB 缓冲池: `512M`
- 慢查询日志: 开启（>2秒）

### Redis 配置

配置文件位置: `redis/redis.conf`

主要配置项:
- 密码: `redis123`
- AOF 持久化: 开启
- 最大内存: `512MB`
- 内存淘汰策略: `allkeys-lru`

### Kafka 配置

- 自动创建主题: 开启
- 副本因子: 1（单节点）
- 事务状态日志: 单副本

## 📊 数据持久化

所有数据都存储在 Docker 数据卷中，即使删除容器也不会丢失数据：

- `mysql_data`: MySQL 数据文件
- `mongodb_data`: MongoDB 数据文件
- `redis_data`: Redis 数据文件
- `kafka_data`: Kafka 数据文件
- `zookeeper_data`: Zookeeper 数据文件

查看数据卷:
```bash
docker volume ls | grep traffic-sim
```

备份数据卷:
```bash
# 备份 MySQL
docker run --rm -v traffic-sim-boot_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql_backup.tar.gz /data

# 备份 MongoDB
docker run --rm -v traffic-sim-boot_mongodb_data:/data -v $(pwd):/backup alpine tar czf /backup/mongodb_backup.tar.gz /data
```

## 🔍 常用命令

### 查看服务日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f mysql
docker-compose logs -f mongodb
docker-compose logs -f redis
docker-compose logs -f kafka
```

### 进入容器

```bash
# 进入 MySQL 容器
docker exec -it traffic-sim-mysql bash
mysql -uroot -proot traffic_sim

# 进入 MongoDB 容器
docker exec -it traffic-sim-mongodb bash
mongosh -u root -p root --authenticationDatabase admin

# 进入 Redis 容器
docker exec -it traffic-sim-redis redis-cli -a redis123
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart mysql
```

### 查看资源使用

```bash
# 查看容器资源使用情况
docker stats

# 查看特定容器资源使用
docker stats traffic-sim-mysql
```

## 🛠️ 数据库初始化

### MySQL 初始化

MySQL 容器启动时会自动执行 `init.sql` 脚本，创建：
- 数据库: `traffic_sim`
- 表结构: 用户、角色、权限、地图、仿真任务、回放任务等
- 初始数据: 默认角色和权限

如果需要重新初始化数据库：

```bash
# 停止 MySQL 服务
docker-compose stop mysql

# 删除数据卷（⚠️ 会删除所有数据）
docker volume rm traffic-sim-boot_mysql_data

# 重新启动
docker-compose up -d mysql
```

### MongoDB 初始化

MongoDB 容器启动时会自动执行 `mongodb/init-mongo.js` 脚本，创建：
- 数据库: `traffic_sim`
- 用户: `traffic_sim`
- 集合: maps, simulation_data, replay_data

## 🔐 安全建议

### 生产环境配置

⚠️ **重要**: 当前配置仅适用于开发环境，生产环境请修改以下配置：

1. **修改默认密码**
   - MySQL: 修改 `MYSQL_ROOT_PASSWORD` 和 `MYSQL_PASSWORD`
   - MongoDB: 修改 `MONGO_INITDB_ROOT_PASSWORD`
   - Redis: 修改 `requirepass` 配置

2. **限制网络访问**
   - 移除不必要的端口映射
   - 使用 Docker 网络隔离
   - 配置防火墙规则

3. **启用 SSL/TLS**
   - MySQL: 配置 SSL 证书
   - MongoDB: 启用 TLS/SSL
   - Redis: 启用 TLS

4. **备份策略**
   - 定期备份数据库
   - 配置自动备份脚本
   - 测试恢复流程

## 📝 应用配置

在应用配置文件中使用以下连接信息：

### application.yml 示例

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/traffic_sim?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root
  
  data:
    mongodb:
      host: localhost
      port: 27017
      database: traffic_sim
      username: root
      password: root
      authentication-database: admin

# Redis 配置
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: redis123
      database: 0

# Kafka 配置
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: traffic-sim-group
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

## 🐛 故障排查

### MySQL 无法连接

1. 检查容器是否运行: `docker-compose ps`
2. 查看日志: `docker-compose logs mysql`
3. 检查端口是否被占用: `netstat -an | grep 3306`
4. 验证密码是否正确

### MongoDB 认证失败

1. 检查用户名和密码
2. 确认 `authentication-database` 设置为 `admin`
3. 查看日志: `docker-compose logs mongodb`

### Redis 连接失败

1. 检查密码是否正确: `redis123`
2. 查看日志: `docker-compose logs redis`
3. 测试连接: `docker exec -it traffic-sim-redis redis-cli -a redis123 ping`

### Kafka 无法启动

1. 确保 Zookeeper 先启动
2. 查看日志: `docker-compose logs kafka`
3. 检查端口是否被占用: `netstat -an | grep 9092`

## 📚 相关文档

- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [MongoDB 官方文档](https://docs.mongodb.com/)
- [Redis 官方文档](https://redis.io/documentation)
- [Kafka 官方文档](https://kafka.apache.org/documentation/)

## 📞 支持

如有问题，请查看项目文档或联系开发团队。

---

**最后更新**: 2024年

