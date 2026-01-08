# 认证API

<cite>
**本文档引用的文件**
- [AuthController.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/controller/AuthController.java)
- [LoginRequest.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/dto/LoginRequest.java)
- [RegisterRequest.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/dto/RegisterRequest.java)
- [ErrorCode.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/constant/ErrorCode.java)
- [JwtTokenService.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/JwtTokenService.java)
- [LoginResponse.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/dto/LoginResponse.java)
- [ApiResponse.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/response/ApiResponse.java)
- [CaptchaService.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/CaptchaService.java)
- [AuthServiceImpl.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java)
- [AuthPluginProperties.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/config/AuthPluginProperties.java)
- [UserService.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/service/UserService.java)
- [UserDTO.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/dto/UserDTO.java)
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml)
</cite>

## 目录
1. [简介](#简介)
2. [认证流程概述](#认证流程概述)
3. [端点详细说明](#端点详细说明)
   1. [用户登录](#用户登录)
   2. [用户注册](#用户注册)
   3. [获取验证码](#获取验证码)
   4. [刷新令牌](#刷新令牌)
   5. [用户登出](#用户登出)
4. [请求体Schema](#请求体schema)
   1. [LoginRequest](#loginrequest)
   2. [RegisterRequest](#registerrequest)
5. [响应体Schema](#响应体schema)
   1. [统一响应格式](#统一响应格式)
   2. [LoginResponse](#loginresponse)
6. [JWT令牌管理](#jwt令牌管理)
7. [错误码说明](#错误码说明)
8. [使用示例](#使用示例)

## 简介
本文档详细描述了交通仿真系统中的认证API，涵盖了用户登录、注册、令牌管理等核心功能。认证API通过JWT（JSON Web Token）实现安全的身份验证机制，支持验证码保护和令牌刷新功能，确保系统的安全性和用户体验。

**Section sources**
- [AuthController.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/controller/AuthController.java#L1-L118)

## 认证流程概述
系统采用基于JWT的无状态认证机制，整个认证流程包括：
1. 用户通过登录接口提交凭证，服务器验证后返回包含JWT令牌的响应
2. 客户端在后续请求中通过Authorization头携带JWT令牌进行身份验证
3. 当访问令牌即将过期时，客户端可使用刷新令牌获取新的访问令牌
4. 用户登出时，令牌被标记为失效，确保安全性

验证码机制在登录和注册过程中提供额外的安全保护，验证码通过响应头`X-Captcha-Id`传递，客户端需要在后续请求中同时提供验证码值和验证码ID。

```mermaid
sequenceDiagram
participant Client as "客户端"
participant AuthController as "AuthController"
participant AuthService as "AuthService"
participant JwtTokenService as "JwtTokenService"
participant CaptchaService as "CaptchaService"
participant UserService as "UserService"
Client->>AuthController : POST /api/auth/login
AuthController->>CaptchaService : validateCaptcha(captchaId, captcha)
CaptchaService-->>AuthController : 验证结果
AuthController->>UserService : validatePassword(username, password)
UserService-->>AuthController : 验证结果
AuthController->>UserService : getUserByUsername(username)
UserService-->>AuthController : UserDTO
AuthController->>JwtTokenService : generateAccessToken(tokenInfo)
JwtTokenService-->>AuthController : accessToken
AuthController->>JwtTokenService : generateRefreshToken(tokenInfo)
JwtTokenService-->>AuthController : refreshToken
AuthController->>AuthController : 构建LoginResponse
AuthController-->>Client : 200 OK + LoginResponse
Note over Client,AuthController : 用户登录流程
```

**Diagram sources**
- [AuthController.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/controller/AuthController.java#L39-L44)
- [AuthServiceImpl.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java#L47-L89)
- [JwtTokenService.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/JwtTokenService.java#L32-L63)
- [CaptchaService.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/CaptchaService.java#L65-L92)
- [UserService.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/service/UserService.java#L41-L42)

## 端点详细说明

### 用户登录
用户登录端点用于验证用户凭据并返回JWT令牌。

**端点信息**
- **HTTP方法**: POST
- **URL路径**: /api/auth/login
- **请求头**: Content-Type: application/json
- **请求体**: LoginRequest对象
- **响应体**: ApiResponse<LoginResponse>

**验证流程**
1. 验证验证码（如果启用）
2. 验证用户名和密码
3. 检查用户状态是否为激活状态
4. 生成访问令牌和刷新令牌
5. 返回包含令牌和用户信息的响应

**成功响应示例**
```json
{
  "res": "ERR_OK",
  "msg": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "user1",
      "email": "user1@example.com",
      "status": "ACTIVE"
    },
    "expiresIn": 3600
  },
  "timestamp": 1700000000000
}
```

**Section sources**
- [AuthController.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/controller/AuthController.java#L39-L44)
- [AuthServiceImpl.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java#L47-L89)

### 用户注册
用户注册端点用于创建新用户账户。

**端点信息**
- **HTTP方法**: POST
- **URL路径**: /api/auth/register
- **请求头**: Content-Type: application/json
- **请求体**: RegisterRequest对象
- **响应体**: ApiResponse<String>

**验证流程**
1. 验证密码强度（根据配置）
2. 检查用户名是否已存在
3. 创建用户记录
4. 返回注册成功消息

**成功响应示例**
```json
{
  "res": "ERR_OK",
  "msg": "ok",
  "data": "注册成功",
  "timestamp": 1700000000000
}
```

**Section sources**
- [AuthController.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/controller/AuthController.java#L49-L54)
- [AuthServiceImpl.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java#L93-L123)

### 获取验证码
获取验证码端点用于生成并返回验证码图片。

**端点信息**
- **HTTP方法**: GET
- **URL路径**: /api/auth/captcha
- **请求头**: 无
- **响应头**: 
  - Content-Type: image/png
  - X-Captcha-Id: 验证码ID
- **响应体**: PNG格式的验证码图片

**响应头说明**
- `X-Captcha-Id`: 包含生成的验证码ID，客户端需要在登录或注册请求中使用此ID和对应的验证码值

**验证码配置**
- **启用状态**: true（默认）
- **图片尺寸**: 120x40像素
- **字符长度**: 4个字符
- **过期时间**: 300秒
- **字符集**: ABCDEFGHJKLMNPQRSTUVWXYZ23456789（排除易混淆字符）

**Section sources**
- [AuthController.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/controller/AuthController.java#L59-L74)
- [CaptchaService.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/CaptchaService.java#L43-L60)

### 刷新令牌
刷新令牌端点用于使用刷新令牌获取新的访问令牌。

**端点信息**
- **HTTP方法**: POST
- **URL路径**: /api/auth/refresh
- **请求头**: Content-Type: application/json
- **请求体**: RefreshTokenRequest对象
- **响应体**: ApiResponse<LoginResponse>

**刷新流程**
1. 验证刷新令牌的有效性
2. 从存储中获取原始TokenInfo
3. 生成新的访问令牌和刷新令牌
4. 更新刷新令牌存储
5. 返回包含新令牌的响应

**成功响应示例**
```json
{
  "res": "ERR_OK",
  "msg": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "user1",
      "email": "user1@example.com",
      "status": "ACTIVE"
    },
    "expiresIn": 3600
  },
  "timestamp": 1700000000000
}
```

**Section sources**
- [AuthController.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/controller/AuthController.java#L79-L85)
- [AuthServiceImpl.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java#L146-L182)

### 用户登出
用户登出端点用于使当前用户的令牌失效。

**端点信息**
- **HTTP方法**: POST
- **URL路径**: /api/auth/logout
- **请求头**: 
  - Authorization: Bearer <access_token>（可选）
- **请求体**: 无
- **响应体**: ApiResponse<String>

**登出流程**
1. 从Authorization头中提取访问令牌
2. 将令牌添加到失效令牌存储中
3. 返回登出成功消息

**成功响应示例**
```json
{
  "res": "ERR_OK",
  "msg": "ok",
  "data": "登出成功",
  "timestamp": 1700000000000
}
```

**Section sources**
- [AuthController.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/controller/AuthController.java#L90-L97)
- [AuthServiceImpl.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java#L184-L201)

## 请求体Schema

### LoginRequest
登录请求DTO，包含用户登录所需的信息。

**字段说明**
- **username** (string): 用户名，不能为空
- **password** (string): 密码，不能为空
- **captcha** (string): 验证码值，当验证码启用时需要提供
- **captchaId** (string): 验证码ID，当验证码启用时需要提供

**验证规则**
- username: 必填，不能为空
- password: 必填，不能为空
- captcha: 当验证码启用时，必须提供且正确
- captchaId: 当验证码启用时，必须提供

**JSON示例**
```json
{
  "username": "user1",
  "password": "password123",
  "captcha": "ABCD",
  "captchaId": "captcha_1700000000_1234"
}
```

**Section sources**
- [LoginRequest.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/dto/LoginRequest.java#L1-L29)

### RegisterRequest
注册请求DTO，包含用户注册所需的信息。

**字段说明**
- **username** (string): 用户名，不能为空
- **password** (string): 密码，不能为空
- **email** (string): 邮箱，格式必须正确
- **phoneNumber** (string): 手机号码，可选
- **institution** (string): 所属机构，可选

**验证规则**
- username: 必填，不能为空
- password: 必填，不能为空，且需满足密码强度要求
- email: 必填，必须是有效的邮箱格式
- phoneNumber: 可选，无特殊格式要求
- institution: 可选，无特殊格式要求

**密码强度配置**
- **最小长度**: 6位
- **大写字母**: 不需要
- **小写字母**: 不需要
- **数字**: 不需要
- **特殊字符**: 不需要

**JSON示例**
```json
{
  "username": "newuser",
  "password": "password123",
  "email": "newuser@example.com",
  "phoneNumber": "13800138000",
  "institution": "交通研究所"
}
```

**Section sources**
- [RegisterRequest.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/dto/RegisterRequest.java#L1-L32)
- [AuthPluginProperties.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/config/AuthPluginProperties.java#L48-L74)

## 响应体Schema

### 统一响应格式
所有API响应都遵循统一的响应格式，便于客户端处理。

**字段说明**
- **res** (string): 响应码，如"ERR_OK"表示成功
- **msg** (string): 响应消息，描述响应结果
- **data** (object): 响应数据，具体结构根据接口而定
- **timestamp** (long): 时间戳，毫秒级

**JSON示例**
```json
{
  "res": "ERR_OK",
  "msg": "ok",
  "data": {},
  "timestamp": 1700000000000
}
```

**Section sources**
- [ApiResponse.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/response/ApiResponse.java#L1-L85)

### LoginResponse
登录响应DTO，包含认证成功后的令牌和用户信息。

**字段说明**
- **accessToken** (string): 访问令牌，用于后续请求的身份验证
- **refreshToken** (string): 刷新令牌，用于获取新的访问令牌
- **user** (UserDTO): 用户信息对象
- **expiresIn** (long): 令牌过期时间（秒）

**UserDTO字段说明**
- **id** (long): 用户ID
- **username** (string): 用户名
- **email** (string): 邮箱
- **phoneNumber** (string): 手机号码
- **institution** (string): 所属机构
- **roleId** (integer): 角色ID
- **roleName** (string): 角色名称
- **status** (string): 用户状态
- **createTime** (date): 创建时间
- **updateTime** (date): 更新时间

**JSON示例**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "user1",
    "email": "user1@example.com",
    "phoneNumber": "13800138000",
    "institution": "交通研究所",
    "roleId": 1,
    "roleName": "USER",
    "status": "ACTIVE"
  },
  "expiresIn": 3600
}
```

**Section sources**
- [LoginResponse.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/dto/LoginResponse.java#L1-L30)
- [UserDTO.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/dto/UserDTO.java#L1-L30)

## JWT令牌管理
系统使用JWT（JSON Web Token）实现无状态的身份验证机制。

**令牌配置**
- **密钥**: "traffic-sim-jwt-secret-key-change-in-production"
- **访问令牌过期时间**: 3600秒（1小时）
- **刷新令牌过期时间**: 86400秒（24小时）

**令牌结构**
- **Header**: 包含算法和令牌类型
- **Payload**: 包含以下声明：
  - sub: 用户ID
  - username: 用户名
  - role: 角色
  - permissions: 权限列表
  - iat: 签发时间
  - exp: 过期时间
- **Signature**: 使用HMAC SHA256算法生成的签名

**权限分配规则**
- **ADMIN角色**: 拥有user:create, user:update, user:delete, user:query权限
- **USER角色**: 拥有user:query权限

**令牌存储**
- **刷新令牌**: 存储在内存中的ConcurrentHashMap，键为刷新令牌，值为TokenInfo
- **失效令牌**: 存储在内存中的ConcurrentHashMap，用于登出后的令牌失效

```mermaid
classDiagram
class JwtTokenService {
+generateAccessToken(TokenInfo) String
+generateRefreshToken(TokenInfo) String
+parseToken(String) TokenInfo
+validateToken(String) boolean
}
class TokenInfo {
+String userId
+String username
+String role
+String[] permissions
+Long issuedAt
+Long expiresAt
}
class AuthServiceImpl {
+refreshTokenStore ConcurrentHashMap~String,TokenInfo~
+invalidatedTokens ConcurrentHashMap~String,Long~
}
JwtTokenService --> TokenInfo : "生成/解析"
AuthServiceImpl --> JwtTokenService : "使用"
AuthServiceImpl --> TokenInfo : "创建"
note right of JwtTokenService
JWT令牌服务负责生成、
解析和验证JWT令牌
end note
note right of AuthServiceImpl
认证服务实现类维护
刷新令牌和失效令牌的存储
end note
```

**Diagram sources**
- [JwtTokenService.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/JwtTokenService.java#L25-L116)
- [AuthServiceImpl.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java#L29-L39)
- [TokenInfo.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/service/TokenInfo.java)

## 错误码说明
系统使用统一的错误码机制来处理各种错误情况。

**错误码列表**
- **ERR_OK**: 成功
- **ERR_AUTH**: 认证失败
- **ERR_ARG**: 参数错误
- **ERR_EXIST**: 资源已存在
- **ERR_NOT_FOUND**: 资源不存在
- **ERR_CREATE**: 创建失败
- **ERR_UPDATE**: 更新失败
- **ERR_DELETE**: 删除失败
- **ERR_ENGINE**: 引擎未初始化
- **ERR_UNKNOWN**: 未知错误
- **ERR_INTERNAL**: 服务器内部错误
- **ERR_PERMISSION**: 权限不足
- **ERR_FORBIDDEN**: 操作不允许

**常见错误场景**
- **登录失败**: 返回ERR_AUTH，消息为"验证码错误或已过期"或"用户名或密码错误"
- **用户名已存在**: 返回ERR_EXIST，消息为"用户名已存在"
- **密码强度不足**: 返回ERR_ARG，消息描述具体强度要求
- **刷新令牌无效**: 返回ERR_AUTH，消息为"刷新令牌无效或已过期"

**错误响应示例**
```json
{
  "res": "ERR_AUTH",
  "msg": "验证码错误或已过期",
  "data": null,
  "timestamp": 1700000000000
}
```

**Section sources**
- [ErrorCode.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/constant/ErrorCode.java#L1-L54)
- [AuthServiceImpl.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/service/AuthServiceImpl.java#L51-L52)

## 使用示例
以下是一些常见的API调用示例。

### 获取验证码
```bash
curl -X GET "http://localhost:3822/api/auth/captcha" \
     -H "accept: image/png" \
     --output captcha.png
```

获取响应头中的X-Captcha-Id：
```bash
curl -X GET "http://localhost:3822/api/auth/captcha" \
     -H "accept: image/png" \
     -v
```

### 用户登录
```bash
curl -X POST "http://localhost:3822/api/auth/login" \
     -H "Content-Type: application/json" \
     -d '{
           "username": "user1",
           "password": "password123",
           "captcha": "ABCD",
           "captchaId": "captcha_1700000000_1234"
         }'
```

### 用户注册
```bash
curl -X POST "http://localhost:3822/api/auth/register" \
     -H "Content-Type: application/json" \
     -d '{
           "username": "newuser",
           "password": "password123",
           "email": "newuser@example.com",
           "phoneNumber": "13800138000",
           "institution": "交通研究所"
         }'
```

### 刷新令牌
```bash
curl -X POST "http://localhost:3822/api/auth/refresh" \
     -H "Content-Type: application/json" \
     -d '{
           "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
         }'
```

### 用户登出
```bash
curl -X POST "http://localhost:3822/api/auth/logout" \
     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Section sources**
- [AuthController.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/controller/AuthController.java)
- [application.yml](file://traffic-sim-server/src/main/resources/application.yml#L49-L50)