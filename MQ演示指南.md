# MQ 模块演示指南

## 目标

演示项目中集成MQ模块的效果，让你直观地看到异步消息传递的过程，以及与同步处理的性能差异。

## 前提条件

确保已完成以下步骤：
1. Docker 基础设施服务已启动（MySQL, MongoDB, Redis）
2. Spring Boot 应用已启动
3. 应用运行在 http://localhost:3822

---

## 接口列表

| 接口 | 用途 | 模式 |
|------|------|------|
| `POST /mq-demo/send` | 发送普通消息 | 异步（MQ） |
| `POST /mq-demo/send-sync` | 发送同步消息（对比用） | 同步（无MQ） |
| `POST /mq-demo/send-delayed` | 发送延迟消息 | 异步（MQ） |
| `POST /mq-demo/broadcast` | 发送广播消息 | 异步（MQ） |
| `POST /mq-demo/send-batch` | 批量发送消息 | 异步（MQ） |
| `POST /mq-demo/send-batch-sync` | 批量同步处理（对比用） | 同步（无MQ） |
| `POST /mq-demo/send-statistics` | 发送统计数据 | 异步（MQ） |
| `GET /mq-demo/received` | 查看接收到的消息 | - |
| `GET /mq-demo/status` | 查看 MQ 队列状态 | - |
| `POST /mq-demo/clear` | 清除接收的消息记录 | - |

---

## 核心测试：性能对比

### 场景1：单条消息对比

**目标**：对比使用MQ与不使用MQ的响应时间差异

**使用 MQ（异步）**
```bash
curl -X POST http://localhost:3822/mq-demo/send \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello MQ!"}'
```
**预期结果**：
- API **立即返回**（< 10ms）
- 响应包含 `sendDuration: "1ms"`

**不使用 MQ（同步）**
```bash
curl -X POST http://localhost:3822/mq-demo/send-sync \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello Sync!"}'
```
**预期结果**：
- API **等待处理完成**后才返回（~120ms）
- 响应包含 `processingTime: "120ms"`

**对比结论**：
| 模式 | 响应时间 | 说明 |
|------|---------|------|
| 异步（MQ） | < 10ms | 立即返回，消息后台处理 |
| 同步（无MQ） | ~120ms | 等待所有操作完成 |

---

### 场景2：批量消息对比

**目标**：放大性能差异，观察高并发场景下的效果

**使用 MQ（异步）**
```bash
curl -X POST http://localhost:3822/mq-demo/send-batch \
  -H "Content-Type: application/json" \
  -d '{"count": 10, "prefix": "MQ-Test"}'
```
**预期**：返回 < 50ms

**不使用 MQ（同步）**
```bash
curl -X POST http://localhost:3822/mq-demo/send-batch-sync \
  -H "Content-Type: application/json" \
  -d '{"count": 10, "prefix": "Sync-Test"}'
```
**预期**：返回 ~1200ms（10 × 120ms）

**对比结论**：
| 模式 | 10条消息总耗时 | 每条平均 |
|------|---------------|---------|
| 异步（MQ） | < 50ms | < 5ms |
| 同步（无MQ） | ~1200ms | ~120ms |

**性能提升**：20-40倍

---

### 场景3：并发处理观察

**目标**：观察多条消息是否同时被处理

**操作**：
```bash
curl -X POST http://localhost:3822/mq-demo/send-batch \
  -H "Content-Type: application/json" \
  -d '{"count": 20, "prefix": "Batch"}'
```

**观察要点**：
- API **立即返回**
- 控制台日志会**陆续显示**多条接收记录
- 注意线程名称：`[mq-consumer-demo.test-0]`、`[mq-consumer-demo.test-1]` 等
- 不同线程处理不同消息

**控制台日志示例**：
```
[SEND] Batch messages sent - Count=20, Topic=demo.test, Duration=5ms
[RECV] Topic=demo.test, Payload: Batch message 1      # 线程0
[RECV] Topic=demo.test, Payload: Batch message 3      # 线程1
[RECV] Topic=demo.test, Payload: Batch message 2      # 线程0
[RECV] Topic=demo.test, Payload: Batch message 5      # 线程2
...（消息交错出现）
```

---

### 场景4：系统解耦演示

**MQ的核心理念**：生产者不需要知道消费者是谁

**操作**：
1. 发送一条消息到 Topic
2. 消费者可以在任何时候启动/停止
3. 消息在中间层（MQ）传递

**控制台观察**：
- `[SEND]` 日志显示消息已发送
- `[RECV]` 日志显示消息被消费
- 两者**完全独立**，不互相等待

---

## 其他功能演示

### 延迟消息

**目标**：演示延迟消息队列功能（定时任务场景）

**操作**：
```bash
curl -X POST http://localhost:3822/mq-demo/send-delayed \
  -H "Content-Type: application/json" \
  -d '{"content": "Delayed message", "delay": 3000}'
```

**预期结果**：
- API **立即返回**
- 3秒后，控制台才出现接收日志

**控制台日志**：
```
[SEND] Delayed message sent - ID=xxx, Topic=demo.delayed, Content=Delayed message, Delay=3000ms
# ... 3秒后 ...
[DELAY] Topic=demo.delayed, Payload: Delayed message
```

**注意**：`delay` 参数单位是毫秒（ms）

---

### 广播消息

**目标**：演示消息广播功能（通知场景）

**操作**：
```bash
curl -X POST http://localhost:3822/mq-demo/broadcast \
  -H "Content-Type: application/json" \
  -d '{"content": "Important announcement!"}'
```

**预期结果**：
- API 返回发送成功
- 所有订阅 `demo.broadcast` Topic 的消费者都会收到

**控制台日志**：
```
[SEND] Broadcast sent - Topic=demo.broadcast, Content=Important announcement!
[BROAD] Topic=demo.broadcast, Payload: Important announcement!
```

**广播 vs 普通消息**：
| 类型 | 发送方式 | 消费者行为 |
|------|---------|-----------|
| 普通消息 | `send()` | 每条消息只被**1个**消费者处理（负载均衡） |
| 广播消息 | `broadcast()` | 每条消息被**所有**消费者处理 |

---

### 发送统计数据

**目标**：模拟真实业务场景（监控数据上报）

**操作**：
```bash
curl -X POST http://localhost:3822/mq-demo/send-statistics \
  -H "Content-Type: application/json" \
  -d '{
    "cpu_usage": 75.5,
    "memory_usage": 62.3,
    "active_connections": 42
  }'
```

**预期结果**：
```json
{
  "success": true,
  "messageId": "xxx",
  "data": {
    "cpu_usage": 75.5,
    "memory_usage": 62.3,
    "active_connections": 42
  },
  "sendDuration": "2ms",
  "topic": "demo.statistics"
}
```

---

## 查看运行状态

### 查看接收到的消息

**操作**：
```bash
curl http://localhost:3822/mq-demo/received?topic=demo.test
```

**预期结果**：
```json
{
  "count": 25,
  "topic": "demo.test",
  "messages": [
    {
      "messageId": "xxx",
      "payload": "Hello!",
      "timestamp": "15:30:45.123",
      "threadName": "mq-consumer-demo.test-0"
    },
    ...
  ]
}
```

---

### 查看 MQ 队列状态

**操作**：
```bash
curl http://localhost:3822/mq-demo/status
```

**预期结果**：
```json
{
  "timestamp": "15:31:00.456",
  "messageQueueSize": 5,
  "delayedQueueSize": 2,
  "totalReceived": 103,
  "topicsWithMessages": 4,
  "messageCountsByTopic": {
    "demo.test": 100,
    "demo.delayed": 3
  }
}
```

**字段说明**：
| 字段 | 含义 |
|------|------|
| `messageQueueSize` | 普通消息队列当前消息数 |
| `delayedQueueSize` | 延迟队列当前消息数 |
| `totalReceived` | 累计已消费的消息总数 |
| `topicsWithMessages` | 有消息的 Topic 数量 |

---

### 清除消息记录

**操作**：
```bash
curl -X POST http://localhost:3822/mq-demo/clear
```

**用途**：测试前清空记录，方便观察

---

## 在 Swagger UI 中测试

### 步骤

1. 打开浏览器访问：http://localhost:3822/swagger-ui.html
2. 找到 **MQ Demo Controller** 分组
3. 展开各个接口查看说明
4. 点击 **Try it out**
5. 填写参数（注意格式）
6. 点击 **Execute**
7. 观察返回结果和响应时间

### Swagger UI 注意事项

- Request Body 格式：JSON
- 参数示例：
  ```json
  {"content": "Hello!"}
  ```
- 不要使用 Swagger 自动生成的示例

---

## 控制台日志说明

### 日志标记

| 标记 | 含义 | 示例 |
|------|------|------|
| `[SEND]` | 消息发送 | `[SEND] Message sent - ID=xxx, Topic=demo.test` |
| `[RECV]` | 普通消息接收 | `[RECV] Topic=demo.test, Payload: Hello!` |
| `[DELAY]` | 延迟消息接收 | `[DELAY] Topic=demo.delayed, Payload: Delayed` |
| `[BROAD]` | 广播消息接收 | `[BROAD] Topic=demo.broadcast, Payload: Alert!` |
| `[STATS]` | 统计数据接收 | `[STATS] Topic=demo.statistics, Payload: {...}` |
| `[SYNC]` | 同步处理（无MQ） | `[SYNC] Duration=120ms` |

### 日志观察技巧

1. **时间戳**：观察消息发送和接收的时间差
2. **线程名**：`[mq-consumer-xxx-N]` 显示是哪个消费者线程处理
3. **交错出现**：多条消息的日志会交错显示，表示并发处理

---

## MQ 核心价值总结

### 1. 异步处理
- **不用MQ**：请求等待处理完成才返回
- **用MQ**：请求立即返回，后台处理

### 2. 削峰填谷
- 高并发时，请求缓冲在队列中
- 消费者按自己节奏处理
- 避免系统过载崩溃

### 3. 系统解耦
- 生产者不需要知道消费者是谁
- 消费者可以独立扩展或重启
- 消息在中间层传递

### 4. 负载均衡
- 多个消费者分担消息处理
- 每条消息只被处理一次
- 提高系统吞吐量

---

## 演示流程建议

### 快速演示（约5分钟）

1. **对比单条**：测试 send vs send-sync，展示基本性能差异
2. **对比批量**：测试 10 条消息，放大效果
3. **观察日志**：展示异步处理过程

### 完整演示（约15分钟）

1. **对比测试**：单条 + 批量
2. **延迟消息**：演示定时场景
3. **广播消息**：演示通知场景
4. **状态监控**：展示队列运行状态
5. **并发观察**：展示多线程处理
6. **解耦说明**：讲解架构设计

---

## 成功标志

当你看到以下现象时，说明MQ模块工作正常：

1. ✅ `/mq-demo/send` 响应时间 < 10ms
2. ✅ `/mq-demo/send-sync` 响应时间 ~120ms
3. ✅ 批量对比：MQ 明显快于同步（20-40倍）
4. ✅ 延迟消息按预期延迟时间后才被消费
5. ✅ 批量消息并发处理（观察线程名）
6. ✅ `/mq-demo/status` 显示队列正常

---

## 注意事项

1. **延迟消息**：`delay` 参数单位是毫秒（ms）
   - 1000ms = 1秒
   - 最大延迟时间可配置

2. **批量消息**：
   - `count` 参数表示发送数量
   - `prefix` 参数是消息前缀

3. **消息记录**：
   - 接收消息保存在内存中
   - 默认保留最近 1000 条
   - 调用 `/mq-demo/clear` 可清空

4. **性能对比**：
   - 同步接口模拟了 120ms 的处理时间
   - 实际项目中可能更复杂，差异更大

---

## 提示

- **建议顺序**：先测试普通消息 → 再测试对比 → 最后测试高级功能
- **观察重点**：注意消息发送和接收的时间戳差异
- **并发测试**：批量发送时观察线程名变化
- **状态监控**：定期查看 `/mq-demo/status` 了解队列状态

---

现在你可以开始演示了！

打开浏览器和两个终端：
- 一个终端调用 API
- 另一个终端观察日志

你会清楚地看到 MQ 的异步消息传递过程！🎊
