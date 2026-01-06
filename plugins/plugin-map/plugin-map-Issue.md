# plugin-map 模块 Issue 报告

## 📋 问题概述

本文档记录了 `plugin-map` 模块在实现过程中发现的问题和需要改进的地方。

---

## 🔴 严重问题

### 1. Python gRPC客户端未实现 ⚠️ 待修复

**问题描述**：
- `PythonGrpcClient.uploadAndConvertFile()` 方法只有TODO注释，未实现实际的gRPC调用
- 这导致地图文件上传后无法调用Python服务进行格式转换

**代码位置**：
- `plugins/plugin-map/src/main/java/com/traffic/sim/plugin/map/client/PythonGrpcClient.java:28-43`

**当前实现**：
```java
public ConvertFileResponse uploadAndConvertFile(MultipartFile file, String userId) {
    // TODO: 实现gRPC调用
    log.warn("gRPC client not implemented yet, using placeholder");
    ConvertFileResponse response = new ConvertFileResponse();
    response.setSuccess(false);
    response.setMessage("gRPC client not implemented");
    return response;
}
```

**设计文档要求**（`plugin-map模块详细设计.md`）：
- 地图上传后需要调用Python服务进行格式转换（通过gRPC）

**解决方案**：
1. 参考 `plugin-simulation` 模块的 `PythonGrpcClient` 实现
2. 定义 Protocol Buffers 文件（`.proto`）
3. 实现 gRPC 客户端调用逻辑

**修复状态**：⚠️ **待修复**

---

### 2. MapServiceImpl中gRPC调用未实现 ⚠️ 待修复

**问题描述**：
- `MapServiceImpl.uploadAndConvertMap()` 方法中，调用Python服务转换文件的部分只有TODO注释

**代码位置**：
- `plugins/plugin-map/src/main/java/com/traffic/sim/plugin/map/service/MapServiceImpl.java:92-93`

**当前实现**：
```java
// 调用Python服务转换文件（这里先简化，实际需要gRPC调用）
// TODO: 实现gRPC调用Python服务
```

**修复状态**：⚠️ **待修复**

---

## ⚠️ 中等问题

### 3. 管理员标识获取未实现 ⚠️ 待修复

**问题描述**：
- 在多个方法中，`isAdmin` 变量被硬编码为 `false`
- 注释说明需要从 `TokenInfo` 获取管理员标识，但未实现

**代码位置**：
- `plugins/plugin-map/src/main/java/com/traffic/sim/plugin/map/service/MapServiceImpl.java:163, 178, 204, 258`

**当前实现**：
```java
boolean isAdmin = false; // TODO: 从TokenInfo获取管理员标识
```

**解决方案**：
1. 从 `RequestContext` 获取当前用户的 `TokenInfo`
2. 检查 `TokenInfo` 中的角色或权限，判断是否为管理员

**修复状态**：⚠️ **待修复**

---

### 4. 地图预览功能未实现 ⚠️ 待修复

**问题描述**：
- `previewMapInfo()` 方法只有TODO注释，未实现预览逻辑

**代码位置**：
- `plugins/plugin-map/src/main/java/com/traffic/sim/plugin/map/service/MapServiceImpl.java:274-280`

**当前实现**：
```java
@Override
public MapInfoDTO previewMapInfo(String mapFile) {
    MapInfoDTO mapInfo = new MapInfoDTO();
    // TODO: 实现预览逻辑
    return mapInfo;
}
```

**设计文档要求**：
- 需要实现地图预览功能，返回地图基本信息

**修复状态**：⚠️ **待修复**

---

### 5. 缺少AutoConfiguration配置 ⚠️ 待修复

**问题描述**：
- `MapPluginConfig` 使用 `@Configuration` 而不是 `@AutoConfiguration`
- 缺少 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件

**代码位置**：
- `plugins/plugin-map/src/main/java/com/traffic/sim/plugin/map/config/MapPluginConfig.java`

**当前实现**：
```java
@Configuration  // ❌ 应该使用 @AutoConfiguration
@EnableConfigurationProperties(MapPluginProperties.class)
public class MapPluginConfig {
}
```

**解决方案**：
1. 将 `@Configuration` 改为 `@AutoConfiguration`
2. 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件
3. 在文件中添加配置类的全限定名

**修复状态**：⚠️ **待修复**

---

## 📝 其他问题

### 6. WebSocket配置问题 ⚠️ 待确认

**问题描述**：
- `WebSocketConfig` 中 `registerWebSocketHandlers()` 方法的实现可能有问题
- 第27行返回的 `handlerRegistry` 可能没有正确使用

**代码位置**：
- `plugins/plugin-engine-manager/src/main/java/com/traffic/sim/plugin/engine/manager/config/WebSocketConfig.java:26-36`

**当前实现**：
```java
WebSocketHandlerRegistry handlerRegistry = registry
        .addHandler(frontendWebSocketHandler, properties.getWebsocket().getFrontendPath())
        .setAllowedOrigins(properties.getWebsocket().getAllowedOrigins().toArray(new String[0]));

if (properties.getWebsocket().isSockjsEnabled()) {
    handlerRegistry.withSockJS();  // ⚠️ 这里可能有问题
}
```

**建议**：
- 检查 `addHandler()` 方法的返回值类型
- 确保 SockJS 配置正确应用

**修复状态**：⚠️ **待确认**

---

## ✅ 检查清单

- [x] ✅ **已解决**：地图上传功能已实现（文件保存部分）
- [x] ✅ **已解决**：地图CRUD操作已实现
- [x] ✅ **已解决**：用户配额检查已实现
- [x] ✅ **已解决**：权限验证框架已实现
- [ ] ⚠️ **待修复**：Python gRPC客户端未实现
- [ ] ⚠️ **待修复**：管理员标识获取未实现
- [ ] ⚠️ **待修复**：地图预览功能未实现
- [ ] ⚠️ **待修复**：缺少AutoConfiguration配置

---

## 📊 解决状态总结

| 问题 | 状态 | 优先级 | 说明 |
|------|------|--------|------|
| 1. Python gRPC客户端未实现 | ⚠️ 待修复 | 🔴 高 | 影响地图转换功能 |
| 2. MapServiceImpl中gRPC调用未实现 | ⚠️ 待修复 | 🔴 高 | 影响地图转换功能 |
| 3. 管理员标识获取未实现 | ⚠️ 待修复 | 🟡 中 | 影响权限控制 |
| 4. 地图预览功能未实现 | ⚠️ 待修复 | 🟡 中 | 功能不完整 |
| 5. 缺少AutoConfiguration配置 | ⚠️ 待修复 | 🟡 中 | 影响插件自动加载 |

**总体状态**: ⚠️ **核心功能框架已实现，但关键功能（gRPC调用）未完成**

---

**报告日期**: 2024年  
**检查人员**: 首席检查负责官

