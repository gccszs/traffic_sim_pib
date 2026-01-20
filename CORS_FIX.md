# CORS 跨域问题修复说明

## 问题描述
前端在局域网中访问后端时出现跨域错误。

## 修复内容

### 1. 修改 `WebConfig.java`

**文件**: `traffic-sim-server/src/main/java/com/traffic/sim/config/WebConfig.java`

**修改内容**:
- 默认允许所有来源 (`*`)
- 使用 `allowedOriginPatterns` 代替 `allowedOrigins` 以支持通配符
- 同时更新了 `addCorsMappings` 和 `corsFilter` 两个方法

**关键代码**:
```java
@Value("${app.cors.allowed-origins:*}")
private String allowedOrigins;

// 使用 allowedOriginPatterns 支持通配符
registry.addMapping("/**")
    .allowedOriginPatterns("*")
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
    .allowedHeaders("*")
    .allowCredentials(true)
    .maxAge(3600);
```

### 2. 修改 `application.yml`

**文件**: `traffic-sim-server/src/main/resources/application.yml`

**修改内容**:
```yaml
app:
  cors:
    # 开发环境：使用 * 允许所有来源
    # 生产环境：指定具体的域名
    allowed-origins: "*"
```

### 3. WebSocket 配置

**文件**: `plugin-engine-manager/config/WebSocketConfig.java`

**状态**: ✅ 已正确配置
- 已使用 `setAllowedOriginPatterns` 支持通配符
- `application.yml` 中已配置 `allowed-origins: ["*"]`

## 配置说明

### 开发环境（当前配置）
```yaml
app:
  cors:
    allowed-origins: "*"
```
- 允许所有来源访问
- 适用于开发和测试环境
- 支持局域网内任意 IP 访问

### 生产环境（推荐配置）
```yaml
app:
  cors:
    allowed-origins: "http://192.168.1.100:7144,http://example.com,https://example.com"
```
- 指定具体的允许来源
- 多个来源用逗号分隔
- 提高安全性

## 支持的访问方式

修复后，以下所有方式都可以访问：

1. **本地访问**:
   - `http://localhost:3822`
   - `http://127.0.0.1:3822`

2. **局域网访问**:
   - `http://192.168.1.x:3822`
   - `http://10.0.0.x:3822`
   - 任意局域网 IP

3. **WebSocket 连接**:
   - 前端: `ws://[任意IP]:3822/ws/frontend/{sessionId}`
   - 引擎: `ws://[任意IP]:3822/ws/exe/{sessionId}`

## 重启服务

修改配置后需要重启 Java 后端：

```bash
# 停止当前服务（Ctrl+C）
# 重新启动
cd traffic-sim-server
mvn spring-boot:run
```

## 验证方法

### 1. 检查启动日志
```
WebSocket Configuration
Allowed origins: *
```

### 2. 测试 CORS
```bash
# 使用 curl 测试
curl -H "Origin: http://192.168.1.100:7144" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS \
     http://localhost:3822/api/test
```

应该返回：
```
Access-Control-Allow-Origin: http://192.168.1.100:7144
Access-Control-Allow-Credentials: true
```

### 3. 浏览器测试
在前端浏览器控制台检查：
- 不应该有 CORS 错误
- Network 标签中的请求应该有 `Access-Control-Allow-Origin` 响应头

## 常见问题

### Q: 为什么使用 `*` 不安全？
**A**: 在生产环境中，`*` 允许任何网站访问你的 API，可能导致 CSRF 攻击。开发环境可以使用，生产环境应该指定具体域名。

### Q: 如何配置多个允许的来源？
**A**: 在 `application.yml` 中用逗号分隔：
```yaml
allowed-origins: "http://192.168.1.100:7144,http://192.168.1.101:7144,http://example.com"
```

### Q: WebSocket 连接仍然失败？
**A**: 检查：
1. 防火墙是否开放 3822 端口
2. WebSocket 路径是否正确
3. 浏览器控制台是否有其他错误

## 安全建议

1. **开发环境**: 使用 `*` 方便开发
2. **测试环境**: 指定测试服务器的 IP
3. **生产环境**: 
   - 只允许前端域名
   - 使用 HTTPS
   - 配置防火墙规则
   - 启用 Spring Security

---

**修复日期**: 2026-01-20  
**修复人**: AI Assistant  
**状态**: ✅ 已完成，需要重启 Java 后端
