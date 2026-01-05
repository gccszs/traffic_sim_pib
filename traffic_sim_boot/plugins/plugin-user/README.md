# plugin-user 模块

## 模块说明

`plugin-user` 是用户管理插件模块，提供用户管理、角色权限管理等核心功能。

## 模块职责

1. **用户管理**
   - 用户CRUD操作
   - 用户信息查询
   - 用户状态管理

2. **角色权限管理**
   - 角色定义和管理
   - 权限分配
   - 角色权限查询

3. **用户数据访问**
   - MySQL数据访问（用户基本信息）
   - 数据持久化

## 技术栈

- Spring Boot 3.2.0
- Spring Data JPA
- Spring Security (用于密码加密)
- MySQL
- Lombok

## 模块结构

```
plugin-user/
├── src/main/java/com/traffic/sim/plugin/user/
│   ├── controller/          # REST API控制器
│   │   └── UserController.java
│   ├── service/             # 服务层
│   │   ├── UserServiceExt.java      # 扩展服务接口
│   │   └── UserServiceImpl.java    # 服务实现（实现UserService和UserServiceExt）
│   ├── repository/          # 数据访问层
│   │   ├── UserRepository.java
│   │   └── RoleRepository.java
│   ├── entity/              # 实体类
│   │   ├── User.java
│   │   ├── Role.java
│   │   └── Permission.java
│   ├── dto/                 # 数据传输对象
│   │   ├── UserCreateRequest.java
│   │   └── UserUpdateRequest.java
│   └── config/              # 配置类
│       └── UserPluginConfig.java
└── src/main/resources/
    └── application.yml      # 模块配置
```

## API接口

### 用户管理接口

- `GET /api/user/{id}` - 根据ID获取用户信息
- `GET /api/user/username/{username}` - 根据用户名获取用户信息
- `POST /api/user` - 创建用户
- `PUT /api/user/{id}` - 更新用户信息
- `PUT /api/user/{id}/password` - 更新用户密码
- `DELETE /api/user/{id}` - 删除用户
- `GET /api/user/list` - 分页获取用户列表

## 数据库表结构

### user 表
- `id` - 主键
- `username` - 用户名（唯一）
- `password` - 密码（加密存储）
- `email` - 邮箱
- `phone_number` - 电话
- `institution` - 机构
- `role_id` - 角色ID
- `status` - 状态（NORMAL/BANNED/BLOCKED）
- `create_time` - 创建时间
- `update_time` - 更新时间

### role 表
- `id` - 主键
- `role_name` - 角色名称
- `role_code` - 角色代码（唯一）
- `description` - 描述

### permission 表
- `id` - 主键
- `permission_name` - 权限名称
- `permission_code` - 权限代码（唯一）
- `description` - 描述

### role_permission 表（关联表）
- `role_id` - 角色ID
- `permission_id` - 权限ID

## 配置说明

在 `application.yml` 中配置：

```yaml
plugin:
  user:
    default-role: 1        # 默认角色ID
    password-encryption: BCrypt  # 密码加密方式
```

## 依赖关系

- 依赖 `traffic-sim-common` 模块
- 实现 `common` 模块中定义的 `UserService` 接口
- 提供 `UserServiceExt` 扩展接口用于密码处理

## 使用说明

1. 确保数据库表已创建
2. 在 `traffic-sim-server` 模块的 `pom.xml` 中添加依赖：
   ```xml
   <dependency>
       <groupId>com.traffic.sim</groupId>
       <artifactId>plugin-user</artifactId>
   </dependency>
   ```
3. 配置数据库连接信息
4. 启动应用后，可通过 REST API 访问用户管理功能

## 注意事项

1. 密码使用 BCrypt 加密存储
2. 用户状态包括：NORMAL（正常）、BANNED（已禁用）、BLOCKED（已锁定）
3. 创建用户时必须提供密码
4. 用户名和邮箱必须唯一
5. 分页查询的页码从1开始

