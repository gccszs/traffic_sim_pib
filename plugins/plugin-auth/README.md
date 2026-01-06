# plugin-auth 模块

## 模块说明

`plugin-auth` 是认证授权插件模块，负责用户认证、JWT令牌管理、权限验证等核心功能。

## 功能特性

- ✅ 用户登录/注册
- ✅ JWT令牌生成与验证
- ✅ 令牌刷新机制
- ✅ 验证码生成与验证
- ✅ 认证拦截器
- ✅ 权限注解（@RequirePermission, @RequireRole）
- ✅ 请求上下文管理

## 模块结构

```
plugin-auth/
├── src/main/java/com/traffic/sim/plugin/auth/
│   ├── annotation/          # 权限注解
│   │   ├── RequirePermission.java
│   │   └── RequireRole.java
│   ├── config/              # 配置类
│   │   ├── AuthPluginProperties.java
│   │   └── AuthPluginAutoConfiguration.java
│   ├── controller/          # REST控制器
│   │   └── AuthController.java
│   ├── interceptor/         # 拦截器
│   │   ├── AuthenticationInterceptor.java
│   │   └── PermissionInterceptor.java
│   ├── service/             # 服务实现
│   │   ├── AuthServiceImpl.java
│   │   ├── JwtTokenService.java
│   │   └── CaptchaService.java
│   └── util/                # 工具类
│       └── RequestContext.java
└── src/main/resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 配置说明

在 `application.yml` 中配置：

```yaml
plugin:
  auth:
    jwt:
      secret: "your-secret-key-change-in-production"
      expire: 3600  # 访问令牌过期时间（秒）
      refresh-expire: 86400  # 刷新令牌过期时间（秒）
    password:
      min-length: 6
      require-uppercase: false
      require-lowercase: false
      require-digit: false
      require-special: false
    captcha:
      enabled: true
      width: 120
      height: 40
      length: 4
      expire-seconds: 300
```

## API接口

### 登录
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password",
  "captcha": "ABCD",
  "captchaId": "captcha_xxx"
}
```

### 注册
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "password",
  "email": "user@example.com",
  "phoneNumber": "13800138000",
  "institution": "Example Institution"
}
```

### 获取验证码
```
GET /api/auth/captcha
Response: PNG图片
Headers: X-Captcha-Id: captcha_xxx
```

### 刷新令牌
```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "xxx"
}
```

### 登出
```
POST /api/auth/logout
Headers: Authorization: Bearer xxx
```

## 使用说明

### 1. 在Controller中使用权限注解

```java
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminController {
    
    @GetMapping("/users")
    @RequirePermission("user:query")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsers() {
        // ...
    }
}
```

### 2. 获取当前用户信息

```java
import com.traffic.sim.plugin.auth.util.RequestContext;

String userId = RequestContext.getCurrentUserId();
String username = RequestContext.getCurrentUsername();
TokenInfo tokenInfo = RequestContext.getCurrentUser();
```

## 依赖关系

- 依赖 `traffic-sim-common` 模块
- 依赖 `plugin-user` 模块（通过 UserService 接口）

## 注意事项

1. **密码处理**：注册时，密码需要通过 `UserService.createUser()` 方法传递。当前 `UserDTO` 不包含密码字段，`plugin-user` 模块实现时需要扩展接口或使用其他方式传递密码。

2. **JWT密钥**：生产环境必须修改默认的JWT密钥。

3. **令牌存储**：当前实现使用内存存储刷新令牌和失效令牌。如需分布式部署，建议使用Redis。

4. **验证码清理**：验证码存储在内存中，建议定期清理过期验证码（可通过定时任务实现）。

## 待完善功能

- [ ] 密码加密存储（由 plugin-user 模块实现）
- [ ] 令牌黑名单（Redis实现）
- [ ] 登录失败次数限制
- [ ] 密码重置功能
- [ ] SSO登录支持

