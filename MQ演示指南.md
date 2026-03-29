# MQ 模块演示指南

## 🎯 演示目标

本指南将帮助你演示项目中集成MQ模块的效果，让你直观地看到异步消息传递的过程。

## 📋 前提条件

确保已完成以下步骤：
1. Docker 基础设施服务已启动（MySQL, MongoDB, Redis, Kafka）
2. Spring Boot 应用已启动
3. 应用运行在 http://localhost:3822

## 🚀 演示步骤

### 1️⃣ 发送普通消息

**目标**：演示消息异步发送和接收

**操作**：
```bash
curl -X POST http://localhost:3822/mq-demo/send \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello MQ!"}'
```

**预期结果**：
- API 返回发送成功和消息ID
- 控制台日志显示：`🎯 MQ Demo [demo.test] Received: Hello MQ!`

**观察要点**：
- 消息发送是**非阻塞**的，接口立即返回
- 消费者在**不同的线程**中处理消息

---

### 2️⃣ 发送延迟消息

**目标**：演示延迟消息队列功能

**操作**：
```bash
curl -X POST http://localhost:3822/mq-demo/send-delayed \
  -H "Content-Type: application/json" \
  -d '{"content": "Delayed message", "delay": 3000}'
```

**预期结果**：
- API 立即返回，消息ID已生成
- 3秒后，控制台日志显示：`⏰ MQ Demo [demo.delayed] Delayed message received`

**观察要点**：
- 延迟期间，消息被保存在延迟队列中
- 只有到达指定时间后，消息才会被消费

---

### 3️⃣ 发送广播消息

**目标**：演示消息广播功能

**操作**：
```bash
curl -X POST http://localhost:3822/mq-demo/broadcast \
  -H "Content-Type: application/json" \
  -d '{"content": "Important announcement!"}'
```

**预期结果**：
- API 返回发送成功
- 控制台日志显示：`📢 MQ Demo [demo.broadcast] Broadcast received`

**观察要点**：
- 广播消息可以被多个消费者同时接收
- 适合通知类场景

---

### 4️⃣ 批量发送消息

**目标**：演示高吞吐量消息发送

**操作**：
```bash
curl -X POST http://localhost:3822/mq-demo/send-batch \
  -H "Content-Type: application/json" \
  -d '{"count": 20, "prefix": "Batch"}'
```

**预期结果**：
- API 立即返回
- 20条消息被快速发送到队列
- 控制台会陆续显示20条接收日志

**观察要点**：
- 所有消息**异步并发处理**
- 批量发送的总时间远小于单个消息处理时间之和

---

### 5️⃣ 查看接收到的消息

**目标**：查看消息的传递历史

**操作**：
```bash
curl http://localhost:3822/mq-demo/received
```

**预期结果**：
```json
{
  "allTopics": ["demo.test", "demo.delayed", "demo.broadcast", "demo.statistics"],
  "totalTopics": 4,
  "topics": {
    "demo.test": {
      "count": 21,
      "messages": [...]
    }
  }
}
```

---

### 6️⃣ 查看 MQ 状态

**目标**：监控消息队列的运行状态

**操作**：
```bash
curl http://localhost:3822/mq-demo/status
```

**预期结果**：
```json
{
  "timestamp": "22:30:15.234",
  "messageQueueSize": 5,
  "delayedQueueSize": 2,
  "totalReceived": 26,
  "topicsWithMessages": 4,
  "messageCountsByTopic": {
    "demo.test": 21,
    "demo.delayed": 2,
    "demo.broadcast": 1,
    "demo.statistics": 2
  }
}
```

---

### 7️⃣ 发送统计数据

**目标**：模拟真实业务场景

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
  "messageId": "msg-xxx",
  "data": {
    "cpu_usage": 75.5,
    "memory_usage": 62.3,
    "active_connections": 42
  },
  "sendTime": "22:30:25.123",
  "topic": "demo.statistics",
  "sendDuration": "2ms"
}
```

---

## 📊 MQ 核心效果演示

### ✨ 异步处理效果

**演示方法**：
1. 发送一个延迟消息（延迟5秒）
2. 立即发送一个普通消息
3. 观察日志时间戳

**预期观察**：
```
📤 Sent delayed message: ...  # 立即返回
📤 Sent message: ...          # 也立即返回
⏰ Delayed message received: ...  # 5秒后才出现
🎯 Message received: ...      # 普通消息立即被消费
```

### 🔄 消息持久化效果

MQ模块支持消息持久化，即使消费者暂时不可用，消息也不会丢失。

### 📈 削峰填谷效果

当有大量请求时，MQ会将请求缓冲在队列中，消费者按自己的节奏处理，避免系统崩溃。

---

## 🎓 演示场景建议

### 场景1：性能对比
- **不用MQ**：在Controller中直接处理耗时操作
- **用MQ**：将耗时操作放到消息队列中
- **对比**：接口响应时间明显缩短

### 场景2：并发处理
- 批量发送100条消息
- 观察多条消息是否同时被处理
- 不同线程处理不同消息

### 场景3：系统解耦
- 生产者不需要知道消费者是谁
- 消费者可以独立扩展或重启
- 消息在中间层传递

---

## 🔍 在 Swagger UI 中测试

1. 打开浏览器访问：http://localhost:3822/swagger-ui.html
2. 找到 **MQ Demo Controller** 分组
3. 展开各个接口
4. 点击 **Try it out**
5. 填写参数并执行
6. 观察返回结果和控制台日志

---

## 📝 控制台日志观察

应用运行时，观察控制台日志中的以下标记：

- `📤` - 消息发送
- `🎯` - 消息接收
- `⏰` - 延迟消息
- `📢` - 广播消息
- `📊` - 统计数据

---

## ⚠️ 注意事项

1. **延迟消息**：`delay` 参数单位是毫秒（ms）
   - 1000ms = 1秒
   - 最大延迟时间可配置

2. **批量消息**：
   - `count` 参数表示发送数量
   - `prefix` 参数是消息前缀

3. **消息追踪**：
   - 所有接收到的消息都会保存
   - 默认保留最近100条
   - 可以调用 `/mq-demo/clear` 清空

---

## 🎉 成功标志

当你看到以下现象时，说明MQ模块工作正常：

1. ✅ 消息发送接口立即返回（< 10ms）
2. ✅ 控制台日志异步显示消息接收
3. ✅ 延迟消息按预期延迟时间后才被消费
4. ✅ 批量消息并发处理
5. ✅ `/mq-demo/status` 显示队列正常

---

## 💡 提示

- **建议**：先测试普通消息，熟悉后再测试高级功能
- **观察**：注意消息发送和接收的时间戳差异
- **对比**：关闭MQ相关代码，对比性能差异
- **监控**：定期查看 `/mq-demo/status` 了解队列状态

---

现在你可以开始演示了！打开浏览器和两个终端，一个调用API，一个观察日志，你会清楚地看到MQ的异步消息传递过程！🎊
