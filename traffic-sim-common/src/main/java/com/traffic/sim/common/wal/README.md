# WAL 模块 (Write-Ahead Log)

## 模块说明

自研 WAL（预写日志）模块，为 MQ 消息队列提供持久化支持，实现消息可靠存储与断点续传功能。

## 核心特性

| 特性 | 说明 |
|------|------|
| 消息持久化 | 追加写日志文件，支持崩溃恢复 |
| 断点续传 | 消费偏移量持久化，重启后从上次位置继续 |
| 稀疏索引 | 每 1000 条消息建立索引，快速定位 |
| 数据校验 | CRC32 校验，保证数据完整性 |
| Magic 标识 | 文件魔数 0x57414C31，防止误读 |
| 批量刷盘 | 积攒 100 条或 100ms 后刷盘，平衡性能与安全 |
| 分段日志 | 单文件 100MB，过大时自动创建新分段 |

## 模块结构

```
traffic-sim-common/src/main/java/com/traffic/sim/common/wal/
├── WalConstants.java              # 常量定义
├── MessageRecord.java             # 消息记录实体
├── LogSegment.java                # 日志段管理
├── WALWriter.java                 # 写入器
├── WALReader.java                 # 读取器
├── ConsumerOffsetManager.java     # 消费偏移量管理
├── config/
│   ├── WalProperties.java         # 配置属性
│   └── WalAutoConfiguration.java  # Spring 自动配置
├── README.md                      # 模块说明与使用指南
└── WAL模块工作内容总结.md          # 工作总结
```

## 消息格式

```
┌─────────────────────────────────────────────────────────────┐
│              MessageRecord 磁盘存储格式                     │
├──────────────┬──────────────┬───────────────┬────────────────┤
│  Magic(4B)   │  CRC(4B)    │  Offset(8B)  │ Timestamp(8B) │
├──────────────┼──────────────┼───────────────┼────────────────┤
│  0x57414C31  │  CRC32校验   │  全局递增序号  │   写入时间     │
├──────────────┼──────────────┼───────────────┼────────────────┤
│   Length(4B) │  Payload(N) │                               │
├──────────────┼──────────────┤                                       │
│   消息体长度  │   实际数据   │                                       │
└──────────────┴──────────────┴───────────────────────────────────┘
```

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      WAL 架构                                │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐      ┌─────────────────┐              │
│  │   WALWriter     │      │   WALReader     │              │
│  └────────┬────────┘      └────────┬────────┘              │
│           │                         │                        │
│           ▼                         ▼                        │
│  ┌─────────────────────────────────────────────────┐        │
│  │              LogSegment (日志段)                  │        │
│  │  ┌─────────────┐        ┌─────────────┐         │        │
│  │  │  .log 文件  │        │  .index 文件 │         │        │
│  │  └─────────────┘        └─────────────┘         │        │
│  └─────────────────────────────────────────────────┘        │
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────┐        │
│  │    ConsumerOffsetManager (消费偏移量管理)        │        │
│  └─────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

## 配置说明

### 默认值来源

所有默认值统一在 `WalConstants.java` 中定义，修改默认值只需改这一处。

| 常量 | 默认值 |
|------|--------|
| `DEFAULT_SEGMENT_SIZE` | 100MB |
| `DEFAULT_BATCH_SIZE` | 100 条 |
| `DEFAULT_FLUSH_INTERVAL_MS` | 100ms |
| `INDEX_INTERVAL` | 1000 条 |

### 配置方式

```yaml
# 最小配置（使用默认值）
wal:
  enabled: true

# 自定义配置
wal:
  enabled: true
  directory: ./my-wal-data
  segment-size: 209715200
  batch-size: 500
  flush-interval-ms: 50
```

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `wal.enabled` | 是否启用 | true |
| `wal.directory` | 存储目录 | ./wal-data |
| `wal.segment-size` | 单段大小 | 100MB |
| `wal.batch-size` | 批量阈值 | 100 条 |
| `wal.flush-interval-ms` | 刷盘间隔 | 100ms |

## 使用示例

### 写入消息

```java
@Autowired
private WALWriter walWriter;

long offset = walWriter.write("topic", payload);
long offset = walWriter.write("topic", payload, timestamp);
```

### 读取消息

```java
@Autowired
private WALReader walReader;

MessageRecord record = walReader.read("topic", offset);
List<MessageRecord> records = walReader.readBatch("topic", startOffset, count);
```

### 消费偏移量

```java
@Autowired
private ConsumerOffsetManager offsetManager;

offsetManager.commitOffset("topic", "group", offset);
long nextOffset = offsetManager.getNextOffset("topic", "group");
```

### Spring Boot 自动配置

WAL 模块通过 Spring Boot 自动配置，只需引入依赖即可：

```yaml
wal:
  enabled: true
```

## 持久化目录结构

```
./wal-data/
├── 0000000000000000.log      # 日志文件（消息内容）
├── 0000000000000000.index   # 索引文件（稀疏索引）
├── 0000000000001000.log      # 下一个日志段
├── 0000000000001000.index
└── offsets/                  # 偏移量目录
    ├── simulation.events.consumer-group-1.offset
    └── statistics.data.default.offset
```

## 与 MQ 模块集成

WAL 模块为 MQ 提供持久化支持：

| MQ 操作 | WAL 集成点 |
|--------|------------|
| `publish()` | 调用 WALWriter.write() 持久化消息 |
| `consume()` 后 | 调用 ConsumerOffsetManager.commitOffset() 记录偏移量 |
| 启动恢复 | WALReader 读取 + ConsumerOffsetManager 恢复消费进度 |

## 扩展方向

- [ ] 与 MemoryMessageQueue 深度集成
- [ ] 支持多种序列化方式（JSON/ProtoBuf）
- [ ] 日志压缩与清理
- [ ] 故障恢复优化
