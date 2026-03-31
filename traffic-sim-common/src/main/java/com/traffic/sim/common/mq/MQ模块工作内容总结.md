# MQ 模块工作内容总结

> 负责模块：`traffic-sim-common/src/main/java/com/traffic/sim/common/mq`

---

## 一、模块文件清单

### 1.1 核心接口层

| 文件 | 说明 |
|------|------|
| `MessageQueue.java` | 消息队列核心接口，定义 publish/subscribe/unsubscribe 等操作 |
| `MessageProducer.java` | 消息生产者接口，支持同步/异步/延迟/广播发送 |
| `MessageQueueConsumer.java` | 消息消费者接口，支持消费成/失败回调 |

### 1.2 核心实现层

| 文件 | 说明 |
|------|------|
| `MemoryMessageQueue.java` | 内存消息队列实现，基于 JVM 内存，支持主题订阅、延迟消息、广播 |
| `DefaultMessageProducer.java` | 默认消息生产者实现，封装对 MessageQueue 的调用 |

### 1.3 配置层

| 文件 | 说明 |
|------|------|
| `MessageQueueConfig.java` | Spring Boot 自动配置类，注册 MQ Bean |
| `MessageQueueProperties.java` | 配置属性类，支持 mq.enabled / mq.queueCapacity 等配置 |

### 1.4 数据模型

| 文件 | 说明 |
|------|------|
| `Message.java` | 消息实体类，支持 NORMAL / DELAYED / BROADCAST 三种类型 |

### 1.5 测试

| 文件 | 说明 |
|------|------|
| `test/MemoryMessageQueueTest.java` | 单元测试 |

---

## 二、架构设计

### 2.1 核心数据结构

```
┌─────────────────────────────────────────────────────────────┐
│                    MemoryMessageQueue                        │
├─────────────────────────────────────────────────────────────┤
│  normalQueues     : ConcurrentHashMap<Topic, BlockingQueue> │
│  delayedQueue     : PriorityBlockingQueue (按时间排序)      │
│  consumers        : ConcurrentHashMap<Topic, List<Consumer>> │
│  consumerExecutors: ConcurrentHashMap<Topic, ExecutorService>│
│  queueStats       : ConcurrentHashMap<Topic, AtomicLong>    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 线程模型

```
┌─────────────────────────────────────────────────────────────┐
│  主线程/发布者 → publish() → dispatchMessage()              │
│                         │                                   │
│                         ├──→ normalQueues.offer()           │
│                         │                                   │
│                         └──→ delayedQueue.offer()            │
│                                                             │
│  延迟消息处理线程 (单线程调度器)                              │
│         ↓                                                    │
│    每10ms扫描 delayedQueue，取出到期消息直接分发              │
│                                                             │
│  消费者线程池 (FixedThreadPool)                              │
│         ↓                                                    │
│    poll(normalQueues) → dispatchToConsumer()                │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 消息生命周期

| 消息类型 | 路径 |
|----------|------|
| 普通消息 | `publish()` → `normalQueues` → 消费者 `poll()` → 分发 |
| 延迟消息 | `publish()` → `delayedQueue` → 延迟线程扫描 → 直接分发 |
| 广播消息 | `publish()` → `normalQueues` → 所有消费者逐一收到 |

---

## 三、Spring Boot 集成

### 3.1 自动配置

通过 `MessageQueueConfig` + Spring Boot AutoConfiguration 机制：

```java
@Configuration
@EnableConfigurationProperties(MessageQueueProperties.class)
@ConditionalOnProperty(prefix = "mq", name = "enabled", havingValue = "true")
public class MessageQueueConfig {
    @Bean @ConditionalOnMissingBean
    public MessageQueue messageQueue(MessageQueueProperties properties) {...}

    @Bean @ConditionalOnMissingBean
    public MessageProducer messageProducer(MessageQueue messageQueue) {...}
}
```

### 3.2 配置文件

```yaml
# application.yml
mq:
  enabled: true
  queue-capacity: 10000
  consumer-threads: 4
  delayed-message-enabled: true
```

### 3.3 自动注册

通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动加载。

---

## 四、项目内使用情况

### 4.1 模块依赖关系

```
traffic-sim-common (MQ模块所在)
├── MQ接口层
│   ├── MessageQueue.java
│   ├── MessageProducer.java
│   └── MessageQueueConsumer.java
├── MQ实现层
│   ├── MemoryMessageQueue.java
│   └── DefaultMessageProducer.java
└── MQ配置层
    ├── MessageQueueConfig.java
    └── MessageQueueProperties.java

traffic-sim-server (依赖 MQ 模块)
├── MessageQueueExampleService.java  → 使用 MessageQueue / MessageProducer
└── MQDemoController.java            → 提供 REST API 演示 MQ 功能

plugin-engine-manager (依赖 MQ 模块)
├── SimulationDataCollector.java     → 使用 MessageProducer 发送数据
└── SimulationDataMessageHandler.java → 订阅 MQ 消息，处理持久化
```

### 4.2 使用的主题

| 主题名 | 用途 | 所在模块 |
|--------|------|----------|
| `simulation.events` | 仿真事件消息 | traffic-sim-server |
| `statistics.data` | 统计数据消息 | traffic-sim-server |
| `broadcast.test` | 广播测试消息 | traffic-sim-server |
| `simulation.data.save` | 仿真数据保存请求 | plugin-engine-manager |
| `simulation.data.batch.save` | 批量保存请求 | plugin-engine-manager |

---

## 五、技术特性

### 5.1 并发安全

- `ConcurrentHashMap` 保证多线程访问 Map 安全
- `CopyOnWriteArrayList` 保证消费者列表操作安全
- `PriorityBlockingQueue` 保证延迟队列线程安全
- `LinkedBlockingQueue` 作为普通消息队列

### 5.2 消息分发策略

| 消息类型 | 分发策略 |
|----------|----------|
| 普通消息 | 哈希负载均衡：`hash(message.id) % consumers.size()` |
| 广播消息 | 所有消费者逐一调用 |
| 延迟消息 | 直接分发，不走消费者轮询 |

### 5.3 背压保护

- `defaultQueueCapacity` 限制普通队列容量
- `MAX_DELAYED_MESSAGE_SCAN = 100` 每轮延迟消息处理上限
- 队列满时抛出 `IllegalStateException`

---

## 六、已识别的问题与优化点

| 问题 | 描述 | 建议 |
|------|------|------|
| Lambda 嵌套过深 | `subscribe()` 中的 ThreadFactory 创建代码过长 | 提取为独立方法 |
| 延迟队列无容量限制 | `delayedQueue` 无限增长 | 增加容量检查 |
| 代码风格 | 部分匿名内部类可简化 | 使用 JDK 自带工具类 |

---

## 七、后续扩展方向

1. **持久化层**：实现 WAL（预写日志）+ 故障恢复
2. **网络通信层**：Netty 实现跨服务通信
3. **集群支持**：主从复制 + 故障转移
4. **消息确认**：ACK 机制 + 重试队列

---

## 八、关键代码位置

| 功能 | 文件:行号 |
|------|----------|
| publish 主逻辑 | `MemoryMessageQueue.java:143-158` |
| 延迟消息处理 | `MemoryMessageQueue.java:80-100` |
| 消费者订阅 | `MemoryMessageQueue.java:168-207` |
| 消息分发 | `MemoryMessageQueue.java:219-249` |
| Spring 配置 | `MessageQueueConfig.java:20-45` |

---

## 九、快速参考

```java
// 发送消息
messageProducer.send("topic", payload);

// 发送延迟消息
messageProducer.sendDelayed("topic", payload, 5000); // 5秒后

// 广播消息
messageProducer.broadcast("topic", payload);

// 订阅消息
messageQueue.subscribe("topic", message -> {
    System.out.println(message.getPayload());
});

// 带回调订阅
messageQueue.subscribe("topic", new MessageQueueConsumer<T>() {
    @Override
    public void consume(Message<T> message) { ... }
    @Override
    public void onSuccess(Message<T> message) { ... }
    @Override
    public void onFailure(Message<T> message, Throwable e) { ... }
});
```
