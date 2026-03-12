# 消息队列模块 (MQ)

## 模块说明

自研消息队列中间件，为毕设设计的目标，用于解决高并发场景下的请求削峰问题。

## 核心特性

|    特性     |                      说明                       |
|:-----------:|:-----------------------------------------------:|
|  内存队列   | 基于 `ConcurrentLinkedQueue` 实现高性能内存存储 |
|  延迟消息   |              支持定时/延迟消息投递              |
|  广播模式   |            支持消息广播给所有订阅者             |
|  负载均衡   |          普通消息均匀分发到多个消费者           |
| 多线程消费  |          支持单个主题配置多个消费线程           |
| Spring 集成 |            提供 Spring Boot 自动配置            |

## 架构设计

```
┌─────────────────────────────────────────────┐
│              MemoryMessageQueue             │
├─────────────────────────────────────────────┤
│  ┌─────────────┐    ┌──────────────────┐   │
│  │ NormalQueue │    │ DelayedQueue     │   │
│  │ (普通队列)   │    │ (优先级队列)     │   │
│  └─────────────┘    └──────────────────┘   │
│         │                     │             │
│         ▼                     ▼             │
│  ┌─────────────────────────────────────┐   │
│  │        Message Dispatcher           │   │
│  │   (普通消息→负载均衡)                │   │
│  │   (广播消息→全部分发)                │   │
│  └─────────────────────────────────────┘   │
│                     │                       │
│                     ▼                       │
│  ┌─────────────────────────────────────┐   │
│  │        Consumer Executor Pool       │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

## 模块结构

```
traffic-sim-common/src/main/java/com/traffic/sim/common/mq/
├── Message.java                    # 消息实体
├── MessageQueue.java               # 消息队列核心接口
├── MessageProducer.java            # 生产者接口
├── MessageQueueConsumer.java       # 消费者接口
├── DefaultMessageProducer.java     # 生产者默认实现
├── MemoryMessageQueue.java         # 内存队列实现
└── config/
    ├── MessageQueueConfig.java     # Spring 配置类
    └── MessageQueueProperties.java # 配置属性类
```

## 快速开始

### 1. 配置消息队列

```yaml
# application.yml
mq:
  enabled: true
  queue-capacity: 10000
  consumer-threads: 4
  delayed-message-enabled: true
```

### 2. 发布消息

```java
@Autowired
private MessageQueue messageQueue;

// 发布普通消息
messageQueue.publish("topic-name", payload);

// 发布延迟消息（3秒后投递）
messageQueue.publishDelayed("topic-name", payload, 3000);

// 发布广播消息
messageQueue.broadcast("topic-name", payload);
```

### 3. 订阅消息

```java
// 简单订阅
messageQueue.subscribe("topic-name", new MessageConsumer<PayloadType>() {
    @Override
    public void consume(Message<PayloadType> message) {
        PayloadType data = message.getPayload();
        // 处理消息
    }
});

// 多线程订阅（提升消费能力）
messageQueue.subscribe("topic-name", consumer, 4);
```

## 消息类型

|   类型    |   说明   |          消费方式          |
|:---------:|:--------:|:--------------------------:|
|  NORMAL   | 普通消息 | 负载均衡（仅一个消费者收到） |
| DELAYYED  | 延迟消息 | 延迟到期后作为普通消息处理 |
| BROADCAST | 广播消息 |     所有消费者都会收到     |

## 性能指标

|       指标       |    数值     |
|:----------------:|:-----------:|
|   默认队列容量   |  10000 条   |
| 延迟消息扫描周期 |    10ms     |
| 单线程消费吞吐量 | ~1000 msg/s |
|   支持主题数量   |   无限制    |

## 扩展方向

- [ ] 消息持久化（文件/数据库）
- [ ] 消息确认机制 (ACK)
- [ ] 死信队列 (DLQ)
- [ ] 消息优先级
- [ ] 集群支持
