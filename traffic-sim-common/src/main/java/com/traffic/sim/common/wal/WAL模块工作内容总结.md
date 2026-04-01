# WAL 模块工作内容总结

> 负责模块：`traffic-sim-common/src/main/java/com/traffic/sim/common/wal`

---

## 一、模块文件清单

| 文件 | 说明 |
|------|------|
| `WalConstants.java` | 常量定义（单一事实来源） |
| `MessageRecord.java` | 消息记录实体（含 CRC 校验） |
| `LogSegment.java` | 日志段管理 |
| `WALWriter.java` | 消息写入器 |
| `WALReader.java` | 消息读取器 |
| `ConsumerOffsetManager.java` | 消费偏移量管理 |
| `config/WalProperties.java` | 配置属性类 |
| `config/WalAutoConfiguration.java` | Spring 自动配置 |
| `README.md` | 模块说明与使用指南 |
| `WAL模块工作内容总结.md` | 工作总结 |

---

## 二、核心能力

| 能力 | 说明 |
|------|------|
| 消息持久化 | 追加写日志文件，支持崩溃恢复 |
| 断点续传 | 消费偏移量持久化，重启后从上次位置继续 |
| 稀疏索引 | 每 1000 条消息建立索引，快速定位 |
| 数据校验 | CRC32 校验，保证数据完整性 |
| Magic 标识 | 文件魔数 0x57414C31，防止误读 |
| 批量刷盘 | 积攒 100 条或 100ms 后刷盘，平衡性能与安全 |
| 分段日志 | 单文件 100MB，过大时自动创建新分段 |

---

## 三、消息格式

```
Magic(4) + CRC(4) + Offset(8) + Timestamp(8) + Length(4) + Payload
```

---

## 四、架构设计

- **WALWriter**：负责将消息追加写入 .log 文件
- **LogSegment**：管理单个日志段及其稀疏索引
- **WALReader**：按 offset 读取消息
- **ConsumerOffsetManager**：管理消费偏移量，定时持久化

---

## 五、默认值管理

所有默认值统一在 `WalConstants.java` 中定义，修改默认值只需改这一处：

| 常量 | 默认值 |
|------|--------|
| `DEFAULT_SEGMENT_SIZE` | 100MB |
| `DEFAULT_BATCH_SIZE` | 100 条 |
| `DEFAULT_FLUSH_INTERVAL_MS` | 100ms |
| `INDEX_INTERVAL` | 1000 条 |

配置方式：`application.yml` 中通过 `wal.*` 前缀配置，未配置时使用默认值。

---

## 六、持久化目录结构

```
./wal-data/
├── {offset}.log      # 日志文件
├── {offset}.index    # 索引文件
└── offsets/          # 偏移量目录
    └── topic.group.offset
```

---

## 七、与 MQ 模块的关系

- WAL 模块为 MQ 提供持久化支持
- publish 时调用 WALWriter 持久化消息
- consume 后调用 ConsumerOffsetManager 记录偏移量
- 启动时通过 WALReader + ConsumerOffsetManager 恢复消费进度

---

## 八、文档

- **README.md**：模块说明、使用指南、配置说明
- **WAL模块工作内容总结**：工作内容概述

---

## 九、后续扩展方向

1. 与 MemoryMessageQueue 深度集成
2. 支持多种序列化方式
3. 日志压缩与清理策略
4. 故障恢复优化
