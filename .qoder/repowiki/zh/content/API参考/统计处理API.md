# 统计处理API

<cite>
**本文档引用的文件**   
- [StatisticsServiceImpl.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/service/StatisticsServiceImpl.java)
- [SimulationStepData.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/model/SimulationStepData.java)
- [StatisticsResult.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/model/StatisticsResult.java)
- [StatisticsCalculatorRegistry.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/calculator/StatisticsCalculatorRegistry.java)
- [StatisticsContextFactory.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/service/StatisticsContextFactory.java)
- [AccelerationCalculator.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/calculator/impl/AccelerationCalculator.java)
- [FlowCalculator.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/calculator/impl/FlowCalculator.java)
- [SimulationDataParser.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/parser/SimulationDataParser.java)
- [StatisticsContext.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/model/StatisticsContext.java)
- [StatisticsBuffer.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/model/StatisticsBuffer.java)
- [UnitConverter.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/util/UnitConverter.java)
- [StatisticsService.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/service/StatisticsService.java)
- [StatisticsData.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/model/StatisticsData.java)
- [EngineWebSocketHandler.java](file://plugins/plugin-engine-manager/src/main/java/com/traffic/sim/plugin/engine/manager/websocket/EngineWebSocketHandler.java)
- [WebSocketHandlerConfig.java](file://plugins/plugin-engine-manager/src/main/java/com/traffic/sim/plugin/engine/manager/config/WebSocketHandlerConfig.java)
</cite>

## 目录
1. [简介](#简介)
2. [核心组件](#核心组件)
3. [数据处理流程](#数据处理流程)
4. [数据结构说明](#数据结构说明)
5. [计算器注册机制](#计算器注册机制)
6. [聚合算法](#聚合算法)
7. [WebSocket推送机制](#websocket推送机制)
8. [缓存设计](#缓存设计)
9. [输入输出示例](#输入输出示例)

## 简介
本文档详细描述了交通仿真系统中统计处理服务的核心功能。虽然StatisticsServiceImpl是服务类而非Controller，但其processSimulationStep和aggregateStatistics方法构成了统计功能的核心接口。文档将详细说明processSimulationStep方法如何接收仿真数据Map并输出StatisticsData，包括数据解析、上下文创建、计算器链执行和结果聚合的完整流程。

## 核心组件

**Section sources**
- [StatisticsServiceImpl.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/service/StatisticsServiceImpl.java#L27-L195)
- [StatisticsService.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/service/StatisticsService.java#L14-L33)

## 数据处理流程

```mermaid
flowchart TD
Start([processSimulationStep入口]) --> ParseData["解析仿真数据"]
ParseData --> CheckNull{"数据有效?"}
CheckNull --> |否| ReturnEmpty["返回空统计数据"]
CheckNull --> |是| ExtractSession["提取会话ID"]
ExtractSession --> GetPrevious["获取上一步数据"]
GetPrevious --> CreateContext["创建统计上下文"]
CreateContext --> ExecuteCalculators["执行所有计算器"]
ExecuteCalculators --> BuildResult["构建统计数据结构"]
BuildResult --> UpdateCache["更新缓存"]
UpdateCache --> ReturnResult["返回StatisticsData"]
subgraph "计算器执行"
ExecuteCalculators --> CalcLoop["遍历计算器注册表"]
CalcLoop --> TryCalc["尝试执行计算器"]
TryCalc --> CalcSuccess{"执行成功?"}
CalcSuccess --> |是| MergeResult["合并计算结果"]
CalcSuccess --> |否| LogError["记录错误日志"]
MergeResult --> NextCalc["下一个计算器"]
NextCalc --> CalcLoop
end
ReturnEmpty --> End([方法结束])
ReturnResult --> End
LogError --> NextCalc
```

**Diagram sources **
- [StatisticsServiceImpl.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/service/StatisticsServiceImpl.java#L37-L79)

## 数据结构说明

### 仿真步数据结构

```mermaid
classDiagram
class SimulationStepData {
+Long step
+Long timestamp
+List<Vehicle> vehicles
+List<Signal> signals
+Map<String, Object> rawData
}
class Vehicle {
+Integer id
+Double speed
+Double acceleration
+Double x
+Double y
+Integer roadId
+Integer laneId
+String type
+Map<String, Object> attributes
}
class Signal {
+Integer crossId
+String state
+Integer phase
+Long cycleTime
+Map<String, Object> attributes
}
SimulationStepData "1" *-- "0..*" Vehicle : 包含
SimulationStepData "1" *-- "0..*" Signal : 包含
```

**Diagram sources **
- [SimulationStepData.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/model/SimulationStepData.java#L15-L71)

### 统计结果数据结构

```mermaid
classDiagram
class StatisticsResult {
+Map<String, Object> data
+set(String, Object)
+get(String)
+merge(StatisticsResult)
+isEmpty()
}
class StatisticsData {
+Long step
+Long timestamp
+Integer vehicleCount
+Double averageSpeed
+Double congestionIndex
+List<SignalState> signalStates
+Map<String, Object> custom
}
StatisticsResult --> StatisticsData : 构建
```

**Diagram sources **
- [StatisticsResult.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/model/StatisticsResult.java#L14-L56)
- [StatisticsData.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/model/StatisticsData.java#L15-L45)

## 计算器注册机制

```mermaid
classDiagram
class StatisticsCalculatorRegistry {
+Map<String, StatisticsCalculator> calculators
+register(StatisticsCalculator)
+getAll()
+get(String)
+contains(String)
}
class StatisticsCalculator {
<<interface>>
+calculate(SimulationStepData, SimulationStepData, StatisticsContext)
+getName()
+getCalculatedFields()
}
class AccelerationCalculator {
+calculate(SimulationStepData, SimulationStepData, StatisticsContext)
+getName()
+getCalculatedFields()
}
class FlowCalculator {
+calculate(SimulationStepData, SimulationStepData, StatisticsContext)
+getName()
+getCalculatedFields()
}
StatisticsCalculatorRegistry "1" -- "0..*" StatisticsCalculator : 管理
StatisticsCalculator <|-- AccelerationCalculator : 实现
StatisticsCalculator <|-- FlowCalculator : 实现
```

**Diagram sources **
- [StatisticsCalculatorRegistry.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/calculator/StatisticsCalculatorRegistry.java#L18-L54)
- [AccelerationCalculator.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/calculator/impl/AccelerationCalculator.java#L20-L74)
- [FlowCalculator.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/calculator/impl/FlowCalculator.java#L21-L114)

## 聚合算法

```mermaid
flowchart TD
Start([aggregateStatistics入口]) --> CheckNull{"数据有效?"}
CheckNull --> |否| ReturnEmpty["返回空统计数据"]
CheckNull --> |是| Initialize["初始化聚合变量"]
Initialize --> LoopStats["遍历每步统计数据"]
LoopStats --> CheckStat{"当前统计有效?"}
CheckStat --> |否| NextStat["下一个统计"]
CheckStat --> |是| AggregateVehicle["累加车辆数"]
AggregateVehicle --> AggregateSpeed["累加平均速度"]
AggregateSpeed --> AggregateCongestion["累加拥堵指数"]
AggregateCongestion --> NextStat
NextStat --> LoopEnd{"遍历结束?"}
LoopEnd --> |否| LoopStats
LoopEnd --> |是| CalculateAverage["计算平均值"]
CalculateAverage --> BuildAggregated["构建聚合数据"]
BuildAggregated --> ReturnResult["返回聚合结果"]
ReturnEmpty --> End([方法结束])
ReturnResult --> End
```

**Diagram sources **
- [StatisticsServiceImpl.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/service/StatisticsServiceImpl.java#L83-L118)

## WebSocket推送机制

```mermaid
sequenceDiagram
participant Engine as "仿真引擎"
participant EngineWS as "EngineWebSocketHandler"
participant Statistics as "StatisticsService"
participant FrontendWS as "FrontendWebSocketHandler"
participant Frontend as "前端应用"
Engine->>EngineWS : 发送仿真数据
EngineWS->>Statistics : processSimulationStep(simData)
Statistics-->>EngineWS : 返回StatisticsData
EngineWS->>FrontendWS : 转发统计结果
FrontendWS->>Frontend : WebSocket推送StatisticsData
Frontend-->>用户 : 实时显示统计信息
Note over Engine,Frontend : 统计结果通过WebSocket推送而非REST响应
```

**Diagram sources **
- [EngineWebSocketHandler.java](file://plugins/plugin-engine-manager/src/main/java/com/traffic/sim/plugin/engine/manager/websocket/EngineWebSocketHandler.java#L68-L216)
- [WebSocketHandlerConfig.java](file://plugins/plugin-engine-manager/src/main/java/com/traffic/sim/plugin/engine/manager/config/WebSocketHandlerConfig.java#L28-L37)

## 缓存设计

```mermaid
classDiagram
class StatisticsContextFactory {
+ConcurrentMap<String, StatisticsContext> contextCache
+create(String)
+remove(String)
+clear()
}
class StatisticsContext {
+String sessionId
+Double roadCapacity
+StatisticsBuffer buffer
+Map<String, Object> mapInfo
+Map<String, Object> custom
}
class StatisticsBuffer {
+List<Integer> inFlowHistory
+List<Integer> outFlowHistory
+int windowSize
+addInFlow(int)
+addOutFlow(int)
+getAverageInFlow()
+getAverageOutFlow()
}
StatisticsContextFactory "1" *-- "0..*" StatisticsContext : 创建
StatisticsContext "1" --> "1" StatisticsBuffer : 包含
```

**Diagram sources **
- [StatisticsContextFactory.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/service/StatisticsContextFactory.java#L17-L49)
- [StatisticsContext.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/model/StatisticsContext.java#L14-L47)
- [StatisticsBuffer.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/model/StatisticsBuffer.java#L15-L87)

## 输入输出示例

### 仿真数据输入样例

```json
{
  "sessionId": "sim_001",
  "step": 10,
  "timestamp": 1700000000000,
  "vehicles": [
    {
      "id": 1,
      "speed": 15.5,
      "acceleration": 2.3,
      "x": 100.0,
      "y": 200.0,
      "roadId": 101,
      "laneId": 1,
      "type": "car"
    },
    {
      "id": 2,
      "speed": 0.0,
      "acceleration": -1.5,
      "x": 150.0,
      "y": 250.0,
      "roadId": 102,
      "laneId": 2,
      "type": "bus"
    }
  ],
  "signals": [
    {
      "crossId": 1,
      "state": "RED",
      "phase": 1,
      "cycleTime": 60
    }
  ]
}
```

### 对应统计输出

```json
{
  "step": 10,
  "timestamp": 1700000000000,
  "vehicleCount": 2,
  "averageSpeed": 7.75,
  "congestionIndex": 0.3,
  "custom": {
    "car_number": 2,
    "speed_ave": 7.75,
    "jam_index": 0.3,
    "acc_min": -1.5,
    "acc_max": 2.3,
    "acc_ave": 0.4,
    "flow_rd_ave": 72000,
    "flow_la_ave": 36000,
    "flow_cross_ave": 36000,
    "flow_details": {
      "roadFlow": {"101": 1, "102": 1},
      "laneFlow": {"101_1": 1, "102_2": 1},
      "crossFlow": {"1": 1}
    }
  }
}
```

**Section sources**
- [StatisticsServiceImpl.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/service/StatisticsServiceImpl.java#L69-L74)
- [SimulationDataParser.java](file://plugins/plugin-statistics/src/main/java/com/traffic/sim/plugin/statistics/parser/SimulationDataParser.java#L24-L56)