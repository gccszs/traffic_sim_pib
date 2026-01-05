# plugin-engine-manager 模块 Issue 报告

## 📋 问题概述

本文档记录了 `plugin-engine-manager` 模块在实现过程中与设计文档不一致的问题。

---

## 🔴 严重问题

### 1. StatisticsService依赖注入方式不符合设计

**问题描述**：
- 在 `EngineWebSocketHandler.java` 中，`StatisticsService` 使用了setter注入方式（第35、48-50行）
- 设计文档要求使用标准的 `@Autowired` 注解注入

**当前实现**：
```java
private StatisticsService statisticsService; // 可选依赖，由 plugin-statistics 模块提供

/**
 * 设置统计服务（可选，由 plugin-statistics 模块提供）
 */
public void setStatisticsService(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
}
```

**设计文档要求**：
- `plugin-engine-manager模块详细设计.md` 第2.2.2节示例代码中明确使用 `@Autowired`：
```java
@Autowired
private StatisticsService statisticsService; // 接口，在common中定义
```

**影响**：
- ❌ 依赖注入不标准，可能导致StatisticsService无法正确注入
- ❌ 如果StatisticsService未注入，统计功能将无法工作
- ❌ 不符合Spring Boot最佳实践

**修复建议**：
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class EngineWebSocketHandler implements WebSocketHandler {
    
    private final SessionService sessionService;
    private FrontendWebSocketHandler frontendWebSocketHandler;
    
    // 使用Optional注入，因为StatisticsService可能不存在（如果plugin-statistics未加载）
    @Autowired(required = false)
    private StatisticsService statisticsService;
    
    // 或者使用构造器注入（推荐）
    // private final Optional<StatisticsService> statisticsService;
    
    // 移除setter方法
}
```

**注意**：由于StatisticsService是可选的（plugin-statistics模块可能未加载），可以使用：
- `@Autowired(required = false)`
- 或使用 `Optional<StatisticsService>`
- 或在配置类中通过条件注入

---

## ⚠️ 中等问题

### 2. AutoConfiguration注解缺失

**问题描述**：
- `EngineManagerAutoConfiguration.java` 使用了 `@Configuration` 注解
- 但其他插件（如plugin-auth、plugin-statistics）使用了 `@AutoConfiguration` 注解
- 虽然通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件注册，但注解不一致

**当前实现**：
```java
@Configuration
@EnableConfigurationProperties(EngineManagerProperties.class)
@ComponentScan(basePackages = "com.traffic.sim.plugin.engine.manager")
public class EngineManagerAutoConfiguration {
    // ...
}
```

**其他插件实现**（plugin-auth）：
```java
@AutoConfiguration
@ComponentScan(basePackages = "com.traffic.sim.plugin.auth")
public class AuthPluginAutoConfiguration {
    // ...
}
```

**影响**：
- ⚠️ 插件自动配置方式不一致，可能影响加载顺序
- ⚠️ 不符合Spring Boot 3.x的自动配置最佳实践

**修复建议**：
```java
@AutoConfiguration
@EnableConfigurationProperties(EngineManagerProperties.class)
@ComponentScan(basePackages = "com.traffic.sim.plugin.engine.manager")
public class EngineManagerAutoConfiguration {
    // 自动配置类，启用配置属性扫描和组件扫描
}
```

---

### 3. StatisticsService调用缺少空值检查

**问题描述**：
- 在 `processSimulationData` 方法中，虽然检查了 `statisticsService != null`，但处理逻辑可以优化
- 如果StatisticsService未注入，统计功能会静默失败

**当前实现**：
```java
StatisticsData statistics = null;
if (statisticsService != null) {
    try {
        statistics = statisticsService.processSimulationStep(simData);
    } catch (Exception e) {
        log.warn("Statistics service failed, forwarding raw data: {}", e.getMessage());
    }
}
```

**建议**：
- 当前实现已经处理了可选依赖的情况，但建议添加更明确的日志
- 如果StatisticsService是必需的，应该在启动时检查并报错

---

## 📝 其他问题

### 4. FrontendWebSocketHandler循环依赖处理

**问题描述**：
- `EngineWebSocketHandler` 和 `FrontendWebSocketHandler` 之间存在相互依赖
- 当前使用setter注入解决循环依赖

**当前实现**：
```java
private FrontendWebSocketHandler frontendWebSocketHandler;

public void setFrontendWebSocketHandler(FrontendWebSocketHandler frontendWebSocketHandler) {
    this.frontendWebSocketHandler = frontendWebSocketHandler;
}
```

**说明**：
- 这种方式可以工作，但不是最佳实践
- 建议考虑使用事件机制或消息总线来解耦

---

## ✅ 检查清单

- [ ] 修改StatisticsService为标准的@Autowired注入（required = false）
- [ ] 统一使用@AutoConfiguration注解
- [ ] 添加StatisticsService未注入时的明确日志
- [ ] 考虑优化循环依赖的处理方式

---

**报告日期**: 2024年  
**检查人员**: 首席检查负责官

