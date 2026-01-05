# plugin-statistics 模块

## 📋 模块说明

`plugin-statistics` 模块负责实时统计每个仿真步的统计信息，包括车辆数量、速度、加速度、流量等各类统计指标的计算和处理。

## 🎯 核心功能

1. **实时统计计算**
   - 从引擎WebSocket接收每个仿真步的原始数据
   - 实时计算各类统计指标
   - 序列化统计结果

2. **统计指标计算**
   - 速度统计（最小/最大/平均）
   - 加速度统计（最小/最大/平均）
   - 车辆进出统计
   - 拥堵指数计算
   - 累计流量统计

3. **可扩展的计算器架构**
   - 采用策略模式，易于扩展
   - 支持多个计算器并行计算
   - 计算器可插拔

## 📦 模块结构

```
plugin-statistics/
├── config/                    # 配置类
│   ├── StatisticsPluginAutoConfiguration.java
│   └── StatisticsPluginProperties.java
├── calculator/                # 统计计算器
│   ├── StatisticsCalculator.java          # 计算器接口
│   ├── StatisticsCalculatorRegistry.java  # 计算器注册表
│   └── impl/                 # 计算器实现
│       ├── SpeedCalculator.java
│       ├── AccelerationCalculator.java
│       └── InOutCalculator.java
├── model/                     # 数据模型
│   ├── StatisticsResult.java
│   ├── SimulationStepData.java
│   ├── StatisticsContext.java
│   └── StatisticsBuffer.java
├── parser/                    # 数据解析器
│   └── SimulationDataParser.java
├── service/                   # 服务实现
│   ├── StatisticsServiceImpl.java
│   └── StatisticsContextFactory.java
└── util/                      # 工具类
    └── UnitConverter.java
```

## 🔧 配置说明

在 `application.yml` 中配置：

```yaml
plugin:
  statistics:
    async:
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 100
    performance:
      max-processing-time-ms: 100
      enable-caching: true
      cache-size: 1000
    calculators:
      enabled:
        - speed
        - acceleration
        - in-out
```

## 🔌 使用方式

### 1. 自动配置

模块通过 Spring Boot 自动配置机制自动加载，无需手动配置。

### 2. 接口调用

```java
@Autowired
private StatisticsService statisticsService;

// 处理单个仿真步
StatisticsData stats = statisticsService.processSimulationStep(simData);

// 聚合多个仿真步
StatisticsData aggregated = statisticsService.aggregateStatistics(stepStatsList);
```

### 3. 扩展计算器

实现 `StatisticsCalculator` 接口并添加 `@Component` 注解：

```java
@Component
public class CustomCalculator implements StatisticsCalculator {
    @Override
    public StatisticsResult calculate(SimulationStepData currentStep, 
                                     SimulationStepData previousStep,
                                     StatisticsContext context) {
        // 实现计算逻辑
    }
    
    @Override
    public String getName() {
        return "CustomCalculator";
    }
    
    @Override
    public List<String> getCalculatedFields() {
        return Arrays.asList("custom_field");
    }
}
```

## 📊 统计指标

### 基础统计
- `speed_min/max/ave`: 速度统计（km/h）
- `acc_min/max/ave`: 加速度统计（m/s²）
- `car_number`: 车辆总数
- `car_in`: 进入车辆数
- `car_out`: 离开车辆数
- `low_speed`: 低速车辆数
- `jam_index`: 拥堵指数（0-100）

### 全局统计
- `global_cars_in`: 累计进入流量（辆/小时）
- `global_cars_out`: 累计离开流量（辆/小时）

## 🔄 数据流

```
引擎WebSocket
  ↓ 发送仿真数据
plugin-engine-manager
  ↓ 调用统计服务
StatisticsService.processSimulationStep()
  ↓ 解析数据
SimulationDataParser
  ↓ 执行计算器
StatisticsCalculator(s)
  ↓ 合并结果
StatisticsResult
  ↓ 构建统计数据
StatisticsData
  ↓ 返回
plugin-engine-manager
  ↓ 转发给前端
前端WebSocket
```

## ⚡ 性能优化

1. **异步处理**: 使用 `@Async` 异步处理统计计算，不阻塞主线程
2. **缓存机制**: 缓存上一步数据和统计上下文
3. **并行计算**: 多个计算器并行执行
4. **错误隔离**: 单个计算器失败不影响其他计算器

## 🧪 测试

模块提供了完整的单元测试和集成测试支持。

## 📝 注意事项

1. 统计处理是异步的，不会阻塞仿真数据流
2. 如果统计处理失败，会降级处理，不影响数据转发
3. 计算器应该保持轻量级，避免复杂计算
4. 统计数据会序列化为JSON格式传输

---

**文档版本**: v1.0  
**创建日期**: 2024年

