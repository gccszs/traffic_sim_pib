# 用户管理API参考

<cite>
**本文档引用的文件**  
- [UserController.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/controller/UserController.java)
- [UserCreateRequest.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/dto/UserCreateRequest.java)
- [UserUpdateRequest.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/dto/UserUpdateRequest.java)
- [User.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/entity/User.java)
- [UserDTO.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/dto/UserDTO.java)
- [UserServiceImpl.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/service/UserServiceImpl.java)
- [ApiResponse.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/response/ApiResponse.java)
- [PageResult.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/response/PageResult.java)
- [AuthenticationInterceptor.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/interceptor/AuthenticationInterceptor.java)
- [PermissionInterceptor.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/interceptor/PermissionInterceptor.java)
- [RequirePermission.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/annotation/RequirePermission.java)
- [RequireRole.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/annotation/RequireRole.java)
- [UserStatus.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/constant/UserStatus.java)
- [ErrorCode.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/constant/ErrorCode.java)
- [application.yml](file://plugins/plugin-user/src/main/resources/application.yml)
</cite>

## 目录
1. [API概览](#api概览)
2. [认证与权限控制](#认证与权限控制)
3. [统一响应格式](#统一响应格式)
4. [用户管理端点](#用户管理端点)
   - [GET /api/user/{id} - 获取用户信息](#get-apiusersid---获取用户信息)
   - [GET /api/user/username/{username} - 根据用户名获取用户信息](#get-apiusersusernameusername---根据用户名获取用户信息)
   - [POST /api/user - 创建用户](#post-apiusers---创建用户)
   - [PUT /api/user/{id} - 更新用户信息](#put-apiusersid---更新用户信息)
   - [PUT /api/user/{id}/password - 更新用户密码](#put-apiusersidpassword---更新用户密码)
   - [DELETE /api/user/{id} - 删除用户](#delete-apiusersid---删除用户)
   - [GET /api/user/list - 获取用户列表（分页）](#get-apiuserslist---获取用户列表分页)
5. [数据传输对象](#数据传输对象)
   - [UserCreateRequest - 用户创建请求](#usercreaterequest---用户创建请求)
   - [UserUpdateRequest - 用户更新请求](#userupdaterequest---用户更新请求)
6. [分页查询参数](#分页查询参数)
7. [示例](#示例)
   - [curl命令示例](#curl命令示例)
   - [典型请求/响应示例](#典型请求响应示例)

## API概览

用户管理API提供了一套完整的用户生命周期管理功能，包括用户创建、查询、更新、删除等操作。所有API端点均位于`/api/user`路径下，采用RESTful设计风格，使用JSON格式进行数据交换。

**API基础路径**: `/api/user`

**支持的HTTP方法**: GET, POST, PUT, DELETE

**内容类型**: `application/json`

**API版本**: v1（通过URL路径隐式版本控制）

**协议**: HTTPS

**Section sources**
- [UserController.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/controller/UserController.java#L30-L35)

## 认证与权限控制

所有用户管理API端点都需要进行身份认证和权限验证，采用JWT（JSON Web Token）令牌机制。

### 认证机制

- **认证方式**: JWT Bearer Token
- **令牌传递**: 通过HTTP请求头传递
- **请求头格式**: `Authorization: Bearer <token>`
- **令牌获取**: 通过`/api/auth/login`接口登录获取
- **令牌刷新**: 通过`/api/auth/refresh`接口使用刷新令牌获取新访问令牌

### 权限控制

系统采用基于角色和权限的访问控制（RBAC）模型：

- **角色注解**: `@RequireRole` - 限制访问所需的角色
- **权限注解**: `@RequirePermission` - 限制访问所需的权限

用户管理API的权限要求：

- **创建用户**: 需要`user:create`权限
- **读取用户**: 需要`user:query`权限
- **更新用户**: 需要`user:update`权限  
- **删除用户**: 需要`user:delete`权限

### 认证流程

```mermaid
sequenceDiagram
participant Client as "客户端"
participant AuthInterceptor as "认证拦截器"
participant PermissionInterceptor as "权限拦截器"
participant UserController as "用户控制器"
Client->>AuthInterceptor : 发送请求(Authorization : Bearer token)
AuthInterceptor->>AuthInterceptor : 提取JWT令牌
AuthInterceptor->>AuthInterceptor : 验证令牌有效性
alt 令牌无效或过期
AuthInterceptor-->>Client : 401 Unauthorized
else 令牌有效
AuthInterceptor->>PermissionInterceptor : 继续处理
PermissionInterceptor->>PermissionInterceptor : 检查方法/类级别的权限注解
alt 权限不足
PermissionInterceptor-->>Client : 403 Forbidden
else 权限足够
PermissionInterceptor->>UserController : 调用目标方法
UserController-->>Client : 返回响应
end
end
```

**Diagram sources**
- [AuthenticationInterceptor.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/interceptor/AuthenticationInterceptor.java#L45-L75)
- [PermissionInterceptor.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/interceptor/PermissionInterceptor.java#L32-L85)
- [RequirePermission.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/annotation/RequirePermission.java#L14-L24)
- [RequireRole.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/annotation/RequireRole.java#L14-L24)

**Section sources**
- [AuthenticationInterceptor.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/interceptor/AuthenticationInterceptor.java#L26-L125)
- [PermissionInterceptor.java](file://plugins/plugin-auth/src/main/java/com/traffic/sim/plugin/auth/interceptor/PermissionInterceptor.java#L27-L134)

## 统一响应格式

所有API响应均采用统一的响应格式，便于前端处理和错误处理。

### 响应结构

```json
{
  "res": "响应码",
  "msg": "响应消息",
  "data": "响应数据",
  "timestamp": "时间戳"
}
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| res | string | 是 | 响应码，成功为`ERR_OK`，失败为其他错误码 |
| msg | string | 是 | 响应消息，描述操作结果 |
| data | object | 否 | 响应数据，具体结构根据接口而定 |
| timestamp | long | 是 | 响应时间戳（毫秒） |

### 常见响应码

| 响应码 | 说明 |
|--------|------|
| ERR_OK | 操作成功 |
| ERR_AUTH | 认证失败 |
| ERR_ARG | 参数错误 |
| ERR_EXIST | 资源已存在 |
| ERR_NOT_FOUND | 资源不存在 |
| ERR_PERMISSION | 权限不足 |
| ERR_INTERNAL | 服务器内部错误 |

```mermaid
classDiagram
class ApiResponse~T~ {
+String res
+String msg
+T data
+Long timestamp
+static <T> ApiResponse~T~ success(T data)
+static <T> ApiResponse~T~ success()
+static <T> ApiResponse~T~ success(String message, T data)
+static <T> ApiResponse~T~ error(String errorCode, String message)
+static <T> ApiResponse~T~ error(String message)
+boolean isSuccess()
}
class PageResult~T~ {
+T[] records
+Long total
+Integer page
+Integer size
+Integer pages
+PageResult(T[] records, Long total, Integer page, Integer size)
}
ApiResponse~T~ <|-- ApiResponse~UserDTO~
ApiResponse~T~ <|-- ApiResponse~PageResult~UserDTO~~
ApiResponse~T~ <|-- ApiResponse~String~
```

**Diagram sources**
- [ApiResponse.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/response/ApiResponse.java#L14-L85)
- [PageResult.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/response/PageResult.java#L16-L47)

**Section sources**
- [ApiResponse.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/response/ApiResponse.java#L1-L85)
- [ErrorCode.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/constant/ErrorCode.java#L8-L54)

## 用户管理端点

### GET /api/user/{id} - 获取用户信息

根据用户ID获取用户详细信息。

#### 请求信息

| 属性 | 说明 |
|------|------|
| HTTP方法 | GET |
| URL路径 | /api/user/{id} |
| 认证要求 | 是 |
| 权限要求 | user:query |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
|--------|------|------|------|------|
| id | 路径参数 | long | 是 | 用户ID |

#### 响应格式

成功响应（HTTP 200）：
```json
{
  "res": "ERR_OK",
  "msg": "ok",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "phoneNumber": "13800138000",
    "institution": "交通仿真中心",
    "roleId": 1,
    "roleName": "管理员",
    "status": "NORMAL",
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00"
  },
  "timestamp": 1704067200000
}
```

错误响应（HTTP 401）：
```json
{
  "res": "ERR_AUTH",
  "msg": "认证令牌无效或已过期",
  "data": null,
  "timestamp": 1704067200000
}
```

错误响应（HTTP 403）：
```json
{
  "res": "ERR_PERMISSION",
  "msg": "权限不足",
  "data": null,
  "timestamp": 1704067200000
}
```

错误响应（HTTP 404）：
```json
{
  "res": "ERR_NOT_FOUND",
  "msg": "用户不存在",
  "data": null,
  "timestamp": 1704067200000
}
```

**Section sources**
- [UserController.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/controller/UserController.java#L43-L48)

### GET /api/user/username/{username} - 根据用户名获取用户信息

根据用户名获取用户详细信息。

#### 请求信息

| 属性 | 说明 |
|------|------|
| HTTP方法 | GET |
| URL路径 | /api/user/username/{username} |
| 认证要求 | 是 |
| 权限要求 | user:query |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
|--------|------|------|------|------|
| username | 路径参数 | string | 是 | 用户名 |

#### 响应格式

成功响应（HTTP 200）：
```json
{
  "res": "ERR_OK",
  "msg": "ok",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "phoneNumber": "13800138000",
    "institution": "交通仿真中心",
    "roleId": 1,
    "roleName": "管理员",
    "status": "NORMAL",
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00"
  },
  "timestamp": 1704067200000
}
```

错误响应（HTTP 404）：
```json
{
  "res": "ERR_NOT_FOUND",
  "msg": "用户不存在",
  "data": null,
  "timestamp": 1704067200000
}
```

**Section sources**
- [UserController.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/controller/UserController.java#L53-L58)

### POST /api/user - 创建用户

创建新用户。

#### 请求信息

| 属性 | 说明 |
|------|------|
| HTTP方法 | POST |
| URL路径 | /api/user |
| 认证要求 | 是 |
| 权限要求 | user:create |
| 内容类型 | application/json |

#### 请求体

请求体为`UserCreateRequest`对象，结构如下：

```json
{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "phoneNumber": "13800138000",
  "institution": "示例机构",
  "roleId": 2
}
```

#### 请求参数验证

| 字段 | 验证规则 |
|------|----------|
| username | 必填，长度3-50字符，不能重复 |
| password | 必填，长度6-100字符 |
| email | 邮箱格式，不能重复（如果提供） |
| phoneNumber | 手机号码 |
| institution | 所属机构 |
| roleId | 角色ID，必须是存在的角色 |

#### 响应格式

成功响应（HTTP 200）：
```json
{
  "res": "ERR_OK",
  "msg": "用户创建成功",
  "data": {
    "id": 2,
    "username": "newuser",
    "email": "user@example.com",
    "phoneNumber": "13800138000",
    "institution": "示例机构",
    "roleId": 2,
    "roleName": "普通用户",
    "status": "NORMAL",
    "createTime": "2024-01-02T00:00:00",
    "updateTime": "2024-01-02T00:00:00"
  },
  "timestamp": 1704153600000
}
```

错误响应（HTTP 400）：
```json
{
  "res": "ERR_ARG",
  "msg": "用户名不能为空",
  "data": null,
  "timestamp": 1704153600000
}
```

错误响应（HTTP 409）：
```json
{
  "res": "ERR_EXIST",
  "msg": "用户名已存在",
  "data": null,
  "timestamp": 1704153600000
}
```

**Section sources**
- [UserController.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/controller/UserController.java#L63-L68)
- [UserCreateRequest.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/dto/UserCreateRequest.java#L13-L34)

### PUT /api/user/{id} - 更新用户信息

更新指定用户的信息。

#### 请求信息

| 属性 | 说明 |
|------|------|
| HTTP方法 | PUT |
| URL路径 | /api/user/{id} |
| 认证要求 | 是 |
| 权限要求 | user:update |
| 内容类型 | application/json |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
|--------|------|------|------|------|
| id | 路径参数 | long | 是 | 用户ID |

#### 请求体

请求体为`UserUpdateRequest`对象，结构如下：

```json
{
  "password": "newpassword123",
  "email": "newemail@example.com",
  "phoneNumber": "13900139000",
  "institution": "新机构",
  "roleId": 3,
  "status": "NORMAL"
}
```

#### 字段说明

- 所有字段均为可选，只更新提供的字段
- 密码字段如果提供，会更新用户密码
- 邮箱更新时会检查是否已被其他用户使用
- 角色ID更新时会验证角色是否存在

#### 响应格式

成功响应（HTTP 200）：
```json
{
  "res": "ERR_OK",
  "msg": "用户更新成功",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "newemail@example.com",
    "phoneNumber": "13900139000",
    "institution": "新机构",
    "roleId": 3,
    "roleName": "高级用户",
    "status": "NORMAL",
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-03T00:00:00"
  },
  "timestamp": 1704240000000
}
```

错误响应（HTTP 404）：
```json
{
  "res": "ERR_NOT_FOUND",
  "msg": "用户不存在",
  "data": null,
  "timestamp": 1704240000000
}
```

**Section sources**
- [UserController.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/controller/UserController.java#L73-L80)
- [UserUpdateRequest.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/dto/UserUpdateRequest.java#L12-L30)

### PUT /api/user/{id}/password - 更新用户密码

更新指定用户的密码。

#### 请求信息

| 属性 | 说明 |
|------|------|
| HTTP方法 | PUT |
| URL路径 | /api/user/{id}/password |
| 认证要求 | 是 |
| 权限要求 | user:update |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
|--------|------|------|------|------|
| id | 路径参数 | long | 是 | 用户ID |
| newPassword | 查询参数 | string | 是 | 新密码 |

#### 响应格式

成功响应（HTTP 200）：
```json
{
  "res": "ERR_OK",
  "msg": "密码更新成功",
  "data": null,
  "timestamp": 1704326400000
}
```

错误响应（HTTP 400）：
```json
{
  "res": "ERR_ARG",
  "msg": "新密码不能为空",
  "data": null,
  "timestamp": 1704326400000
}
```

**Section sources**
- [UserController.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/controller/UserController.java#L85-L92)

### DELETE /api/user/{id} - 删除用户

删除指定用户。

#### 请求信息

| 属性 | 说明 |
|------|------|
| HTTP方法 | DELETE |
| URL路径 | /api/user/{id} |
| 认证要求 | 是 |
| 权限要求 | user:delete |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 说明 |
|--------|------|------|------|------|
| id | 路径参数 | long | 是 | 用户ID |

#### 响应格式

成功响应（HTTP 200）：
```json
{
  "res": "ERR_OK",
  "msg": "用户删除成功",
  "data": null,
  "timestamp": 1704412800000
}
```

错误响应（HTTP 404）：
```json
{
  "res": "ERR_NOT_FOUND",
  "msg": "用户不存在",
  "data": null,
  "timestamp": 1704412800000
}
```

**Section sources**
- [UserController.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/controller/UserController.java#L97-L102)

### GET /api/user/list - 获取用户列表（分页）

分页获取用户列表。

#### 请求信息

| 属性 | 说明 |
|------|------|
| HTTP方法 | GET |
| URL路径 | /api/user/list |
| 认证要求 | 是 |
| 权限要求 | user:query |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| page | 查询参数 | int | 否 | 1 | 页码（从1开始） |
| size | 查询参数 | int | 否 | 10 | 每页大小 |
| status | 查询参数 | string | 否 | 无 | 用户状态过滤 |

#### 响应格式

成功响应（HTTP 200）：
```json
{
  "res": "ERR_OK",
  "msg": "ok",
  "data": {
    "records": [
      {
        "id": 1,
        "username": "admin",
        "email": "admin@example.com",
        "phoneNumber": "13800138000",
        "institution": "交通仿真中心",
        "roleId": 1,
        "roleName": "管理员",
        "status": "NORMAL",
        "createTime": "2024-01-01T00:00:00",
        "updateTime": "2024-01-01T00:00:00"
      },
      {
        "id": 2,
        "username": "user1",
        "email": "user1@example.com",
        "phoneNumber": "13800138001",
        "institution": "示例机构",
        "roleId": 2,
        "roleName": "普通用户",
        "status": "NORMAL",
        "createTime": "2024-01-02T00:00:00",
        "updateTime": "2024-01-02T00:00:00"
      }
    ],
    "total": 2,
    "page": 1,
    "size": 10,
    "pages": 1
  },
  "timestamp": 1704585600000
}
```

**Section sources**
- [UserController.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/controller/UserController.java#L107-L116)
- [PageResult.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/response/PageResult.java#L16-L47)

## 数据传输对象

### UserCreateRequest - 用户创建请求

用户创建请求的数据传输对象。

#### 字段定义

| 字段 | 类型 | 必填 | 验证规则 | 说明 |
|------|------|------|----------|------|
| username | string | 是 | 长度3-50字符，不能为空 | 用户名 |
| password | string | 是 | 长度6-100字符，不能为空 | 密码 |
| email | string | 否 | 邮箱格式 | 邮箱地址 |
| phoneNumber | string | 否 | 无 | 手机号码 |
| institution | string | 否 | 无 | 所属机构 |
| roleId | integer | 否 | 必须是存在的角色ID | 角色ID |

#### Java类定义

```java
@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phoneNumber;
    
    private String institution;
    
    private Integer roleId;
}
```

**Section sources**
- [UserCreateRequest.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/dto/UserCreateRequest.java#L13-L34)

### UserUpdateRequest - 用户更新请求

用户更新请求的数据传输对象。

#### 字段定义

| 字段 | 类型 | 必填 | 验证规则 | 说明 |
|------|------|------|----------|------|
| password | string | 否 | 长度6-100字符 | 新密码（如果提供） |
| email | string | 否 | 邮箱格式 | 邮箱地址 |
| phoneNumber | string | 否 | 无 | 手机号码 |
| institution | string | 否 | 无 | 所属机构 |
| roleId | integer | 否 | 必须是存在的角色ID | 角色ID |
| status | string | 否 | 无 | 用户状态 |

#### Java类定义

```java
@Data
public class UserUpdateRequest {
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phoneNumber;
    
    private String institution;
    
    private Integer roleId;
    
    private String status;
}
```

**Section sources**
- [UserUpdateRequest.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/dto/UserUpdateRequest.java#L12-L30)

## 分页查询参数

用户列表查询支持分页功能，通过以下参数控制分页行为。

### 参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码，从1开始计数 |
| size | int | 否 | 10 | 每页显示的记录数 |
| status | string | 否 | 无 | 用户状态过滤条件 |

### 分页逻辑

- 分页从第1页开始，而不是第0页
- 每页大小默认为10条记录，最大不超过100条
- 结果按创建时间倒序排列
- 响应中包含总记录数、总页数等分页信息

### 分页结果结构

分页结果封装在`PageResult<T>`对象中，包含以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| records | List<T> | 当前页的数据列表 |
| total | long | 总记录数 |
| page | int | 当前页码 |
| size | int | 每页大小 |
| pages | int | 总页数 |

**Section sources**
- [UserController.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/controller/UserController.java#L107-L116)
- [UserServiceImpl.java](file://plugins/plugin-user/src/main/java/com/traffic/sim/plugin/user/service/UserServiceImpl.java#L299-L328)
- [PageResult.java](file://traffic-sim-common/src/main/java/com/traffic/sim/common/response/PageResult.java#L16-L47)

## 示例

### curl命令示例

#### 获取用户信息

```bash
curl -X GET "http://localhost:8080/api/user/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 创建用户

```bash
curl -X POST "http://localhost:8080/api/user" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "email": "user@example.com",
    "phoneNumber": "13800138000",
    "institution": "示例机构",
    "roleId": 2
  }'
```

#### 更新用户信息

```bash
curl -X PUT "http://localhost:8080/api/user/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newemail@example.com",
    "phoneNumber": "13900139000",
    "institution": "新机构"
  }'
```

#### 获取用户列表（分页）

```bash
curl -X GET "http://localhost:8080/api/user/list?page=1&size=5&status=NORMAL" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 典型请求/响应示例

#### 创建用户 - 成功

**请求**:
```http
POST /api/user HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "username": "testuser",
  "password": "test123",
  "email": "test@example.com",
  "roleId": 2
}
```

**响应**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "res": "ERR_OK",
  "msg": "用户创建成功",
  "data": {
    "id": 3,
    "username": "testuser",
    "email": "test@example.com",
    "phoneNumber": null,
    "institution": null,
    "roleId": 2,
    "roleName": "普通用户",
    "status": "NORMAL",
    "createTime": "2024-01-04T00:00:00",
    "updateTime": "2024-01-04T00:00:00"
  },
  "timestamp": 1704499200000
}
```

#### 获取用户列表 - 成功

**请求**:
```http
GET /api/user/list?page=1&size=2 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**响应**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "res": "ERR_OK",
  "msg": "ok",
  "data": {
    "records": [
      {
        "id": 1,
        "username": "admin",
        "email": "admin@example.com",
        "phoneNumber": "13800138000",
        "institution": "交通仿真中心",
        "roleId": 1,
        "roleName": "管理员",
        "status": "NORMAL",
        "createTime": "2024-01-01T00:00:00",
        "updateTime": "2024-01-01T00:00:00"
      },
      {
        "id": 2,
        "username": "user1",
        "email": "user1@example.com",
        "phoneNumber": "13800138001",
        "institution": "示例机构",
        "roleId": 2,
        "roleName": "普通用户",
        "status": "NORMAL",
        "createTime": "2024-01-02T00:00:00",
        "updateTime": "2024-01-02T00:00:00"
      }
    ],
    "total": 3,
    "page": 1,
    "size": 2,
    "pages": 2
  },
  "timestamp": 1704585600000
}
```

#### 创建用户 - 参数错误

**请求**:
```http
POST /api/user HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "username": "a",
  "password": "123",
  "email": "invalid-email"
}
```

**响应**:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "res": "ERR_ARG",
  "msg": "用户名长度必须在3-50个字符之间",
  "data": null,
  "timestamp": 1704672000000
}
```