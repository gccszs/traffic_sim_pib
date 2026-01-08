# Redis配置

<cite>
**本文档引用文件**  
- [redis.conf](file://infrastructure/redis/redis.conf)
- [docker-compose.yml](file://infrastructure/docker-compose.yml)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml)
- [README.md](file://infrastructure/README.md)
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml)
- [plugin-auth/README.md](file://plugins/plugin-auth/README.md)
</cite>

## 目录
1. [简介](#简介)
2. [Redis配置文件详解](#redis配置文件详解)
3. [Docker Compose中的Redis服务配置](#docker-compose中的redis服务配置)
4. [Redis在系统中的用途](#redis在系统中的用途)
5. [常见问题排查](#常见问题排查)
6. [安全建议](#安全建议)

## 简介
Redis是交通仿真系统中的关键缓存组件，用于会话管理、令牌存储和临时数据缓存。本系统通过Docker容器化部署Redis，确保环境一致性并简化部署流程。Redis服务与MySQL、MongoDB、Kafka等其他中间件共同构成系统的基础设施层，为上层应用提供高性能的数据访问能力。

Redis在本系统中主要承担以下职责：
- 用户会话状态管理
- JWT令牌存储与验证
- 临时数据缓存
- 分布式锁机制支持

通过AOF持久化和密码认证等安全机制，确保数据的可靠性和安全性。系统采用Redis 7.2版本，结合Docker容器化部署，实现了高可用性和易维护性。

## Redis配置文件详解

Redis配置文件`redis.conf`定义了服务的核心参数，包括安全、持久化和性能相关设置。

### 安全配置
```conf
# 绑定地址
bind 0.0.0.0

# 密码
requirepass redis123
```
`bind 0.0.0.0`允许Redis服务接受来自任何IP地址的连接，适用于容器化部署环境。`requirepass redis123`设置了访问密码，客户端连接时必须提供此密码才能进行操作，增强了服务的安全性。

### 持久化设置
```conf
# 持久化配置
save 900 1
save 300 10
save 60 10000

# AOF 持久化
appendonly yes
appendfsync everysec
```
Redis采用RDB和AOF两种持久化机制。RDB快照配置了三个条件：900秒内至少有1个键被修改、300秒内至少有10个键被修改、60秒内至少有10000个键被修改。AOF（Append Only File）持久化已启用，通过`appendfsync everysec`配置每秒同步一次，在性能和数据安全性之间取得了良好平衡。

### 内存管理策略
```conf
# 最大内存
maxmemory 512mb
maxmemory-policy allkeys-lru
```
内存限制设置为512MB，防止Redis占用过多系统内存。当内存达到上限时，采用`allkeys-lru`（最近最少使用）策略淘汰旧数据，优先保留最近访问过的数据，优化缓存命中率。

### 其他配置
```conf
# 日志级别
loglevel notice

# 数据库数量
databases 16
```
日志级别设置为`notice`，记录重要运行信息而不产生过多日志。系统配置了16个数据库实例，可通过数字索引（0-15）进行区分使用，便于不同功能模块的数据隔离。

**Section sources**
- [redis.conf](file://infrastructure/redis/redis.conf#L1-L30)

## Docker Compose中的Redis服务配置

Docker Compose文件定义了Redis服务的容器化部署配置，包括镜像、端口映射、数据卷挂载和健康检查等。

### 服务定义
```yaml
redis:
  image: m.daocloud.io/docker.io/library/redis:7.2-alpine
  container_name: traffic-sim-redis
  restart: unless-stopped
  command: redis-server --requirepass redis123 --appendonly yes
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data
    - ./redis/redis.conf:/usr/local/etc/redis/redis.conf
  networks:
    - traffic-sim-network
  healthcheck:
    test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
    interval: 10s
    timeout: 3s
    retries: 5
```

### 配置说明
**镜像与容器**：使用`m.daocloud.io`镜像源加速国内访问，基于`redis:7.2-alpine`轻量级镜像。容器名称为`traffic-sim-redis`，重启策略为`unless-stopped`，确保服务在异常退出后自动重启。

**启动命令**：通过`command`参数覆盖默认启动命令，显式启用密码认证和AOF持久化，确保关键安全和持久化功能被激活。

**端口映射**：将容器内部的6379端口映射到主机的6379端口，允许外部应用通过localhost:6379访问Redis服务。

**数据卷挂载**：
- `redis_data:/data`：使用Docker数据卷持久化Redis数据，即使容器删除也不会丢失数据。
- `./redis/redis.conf:/usr/local/etc/redis/redis.conf`：挂载自定义配置文件，确保容器使用项目特定的配置。

**网络配置**：加入`traffic-sim-network`自定义网络，与其他服务（如MySQL、MongoDB）在同一网络中，便于服务间通信。

**健康检查**：配置健康检查机制，每10秒执行一次`redis-cli --raw incr ping`命令。该命令会递增一个计数器并返回结果，成功返回数字表示服务正常。超时3秒，连续5次失败后标记容器为不健康状态。

**Section sources**
- [docker-compose.yml](file://infrastructure/docker-compose.yml#L71-L90)
- [docker-compose.core.yml](file://infrastructure/docker-compose.core.yml#L70-L87)

## Redis在系统中的用途

Redis在交通仿真系统中扮演着多个关键角色，主要用于提升系统性能和实现分布式环境下的状态管理。

### 会话缓存
系统使用Redis存储用户会话信息，替代传统的内存存储方式。在`plugin-auth`模块的README中提到："当前实现使用内存存储刷新令牌和失效令牌。如需分布式部署，建议使用Redis。"这表明Redis将用于存储JWT刷新令牌和失效令牌黑名单，支持系统的水平扩展。

### 临时数据存储
Redis作为临时数据存储，用于缓存频繁访问但不需要持久化的数据。例如，验证码、会话状态、临时计算结果等。相比数据库，Redis提供微秒级的读写速度，显著提升用户体验。

### 分布式锁
在多实例部署场景下，Redis的原子操作特性可用于实现分布式锁，确保关键操作的线程安全性。例如，在用户注册、资源分配等场景中防止并发冲突。

### 性能优化
通过缓存数据库查询结果、API响应等，减少对后端服务的直接调用，降低系统整体负载。对于交通仿真这类计算密集型应用，合理的缓存策略可以显著提升响应速度。

**Section sources**
- [plugin-auth/README.md](file://plugins/plugin-auth/README.md#L160-L168)
- [README.md](file://infrastructure/README.md#L212-L228)

## 常见问题排查

### 认证失败
**现象**：连接Redis时返回"NOAUTH Authentication required"错误。

**原因**：未提供密码或密码错误。

**解决方案**：
1. 确认密码为`redis123`
2. 在连接字符串中包含密码：`redis://:redis123@localhost:6379/0`
3. 使用redis-cli测试：`docker exec -it traffic-sim-redis redis-cli -a redis123 ping`
4. 检查配置文件中`requirepass`参数是否正确

### 网络不通
**现象**：无法连接到Redis服务，提示连接超时或拒绝。

**原因**：服务未启动、端口未正确映射或防火墙阻止。

**解决方案**：
1. 检查服务状态：`docker-compose ps`
2. 查看Redis容器日志：`docker-compose logs redis`
3. 确认端口映射：`docker-compose port redis 6379`
4. 测试容器内连接：`docker exec -it traffic-sim-redis ping localhost`
5. 检查主机防火墙设置，确保6379端口开放

### 持久化问题
**现象**：重启后数据丢失。

**原因**：数据卷未正确挂载或AOF未启用。

**解决方案**：
1. 确认`docker-compose.yml`中已挂载`redis_data`数据卷
2. 检查`appendonly yes`配置是否生效
3. 查看Redis日志确认AOF文件创建
4. 手动触发持久化：`docker exec -it traffic-sim-redis redis-cli -a redis123 save`

### 内存不足
**现象**：写入操作失败，返回"OOM command not allowed"。

**原因**：达到`maxmemory`限制。

**解决方案**：
1. 监控内存使用：`docker stats traffic-sim-redis`
2. 优化数据结构，删除不必要的键
3. 调整`maxmemory-policy`策略
4. 增加内存限制（需评估系统资源）

**Section sources**
- [README.md](file://infrastructure/README.md#L460-L465)
- [redis.conf](file://infrastructure/redis/redis.conf#L21-L23)

## 安全建议
根据基础设施文档中的安全建议，生产环境部署时应采取以下措施：

1. **修改默认密码**：将`redis123`更换为强密码，避免使用默认或简单密码
2. **限制网络访问**：移除不必要的端口映射，使用Docker网络隔离，配置防火墙规则
3. **启用SSL/TLS**：配置TLS加密，防止数据在传输过程中被窃听
4. **定期备份**：建立定期备份策略，测试恢复流程，确保数据可恢复性
5. **监控与告警**：设置内存使用、连接数等关键指标的监控和告警

**Section sources**
- [README.md](file://infrastructure/README.md#L378-L402)