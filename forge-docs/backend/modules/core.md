# 核心工具（forge-starter-core）

项目核心基础设施模块，提供统一响应、异常处理、会话管理、常用注解等基础能力。

## 统一响应

### `RespInfo<T>`

所有 API 统一返回 `RespInfo<T>` 格式：

```java
public class RespInfo<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
}
```

#### 静态工厂方法

```java
// 成功
RespInfo.success();
RespInfo.success(data);
RespInfo.success("操作成功", data);

// 失败
RespInfo.error();
RespInfo.error("操作失败");
RespInfo.error(500, "系统错误");

// 自定义
RespInfo.build(code, message, data);
```

## 异常处理

### `BusinessException`

业务异常，携带错误码和消息：

```java
throw new BusinessException(40001, "用户不存在");
throw new BusinessException("操作失败");
```

### `GlobalExceptionHandler`

通过 `@RestControllerAdvice` 统一捕获和处理异常，返回 `RespInfo` 格式。

支持的异常类型：

| 异常类型 | HTTP 状态 | 说明 |
|----------|-----------|------|
| `BusinessException` | 400 | 业务异常 |
| `MethodArgumentNotValidException` | 400 | 参数校验失败 |
| `BindException` | 400 | 绑定异常 |
| `ConstraintViolationException` | 400 | 约束违反 |
| `HttpRequestMethodNotSupportedException` | 405 | 请求方法不支持 |
| `AccessDeniedException` | 403 | 权限不足 |
| `MaxUploadSizeExceededException` | 413 | 上传文件过大 |
| `NullPointerException` | 500 | 空指针 |
| `IllegalArgumentException` | 400 | 非法参数 |
| 其他 `Exception` | 500 | 系统内部错误 |

## 会话管理

### `LoginUser`

当前登录用户信息：

| 字段 | 说明 |
|------|------|
| `userId` | 用户 ID |
| `tenantId` | 租户 ID |
| `username` | 用户名 |
| `realName` | 真实姓名 |
| `userType` | 用户类型（0=系统管理员，1=租户管理员，2=普通用户） |
| `phone` | 手机号 |
| `email` | 邮箱 |
| `roleIds` | 角色 ID 列表 |
| `roleKeys` | 角色标识列表 |
| `permissions` | 权限列表 |
| `apiPermissions` | API 权限映射 |
| `orgIds` | 所属组织 ID 列表 |
| `mainOrgId` | 主组织 ID |
| `userClient` | 客户端标识 |

#### 便捷方法

```java
loginUser.isAdmin();       // 是否系统管理员
loginUser.isTenantAdmin(); // 是否租户管理员
```

### `SessionHelper`

基于 Sa-Token 的会话工具类，所有方法为静态：

```java
// 获取当前登录用户
LoginUser user = SessionHelper.getLoginUser();

// 获取用户信息
Long userId = SessionHelper.getUserId();
String username = SessionHelper.getUsername();
Long tenantId = SessionHelper.getTenantId();

// 权限校验
boolean hasRole = SessionHelper.hasRole("admin");
boolean hasPerm = SessionHelper.hasPermission("system:user:edit");
boolean hasAny = SessionHelper.hasAnyPermission("system:user:edit", "system:user:view");
boolean hasAll = SessionHelper.hasAllPermissions("system:user:edit", "system:user:delete");

// 角色/组织信息
List<Long> roleIds = SessionHelper.getRoleIds();
List<String> roleKeys = SessionHelper.getRoleKeys();
List<Long> orgIds = SessionHelper.getOrgIds();

// 管理员判断
boolean isAdmin = SessionHelper.isAdmin();
boolean isTenantAdmin = SessionHelper.isTenantAdmin();
```

## 常用注解

### `@ApiEncrypt` / `@ApiDecrypt`

API 请求/响应加解密：

```java
@ApiDecrypt  // 解密请求体
@ApiEncrypt  // 加密响应体
@PostMapping("/sensitive")
public RespInfo<User> getSensitiveInfo(@RequestBody DecryptRequest request) {
    // ...
}
```

### `@OperationLog`

操作日志记录：

```java
@OperationLog(
    module = "用户管理",
    type = OperationType.ADD,
    desc = "新增用户",
    saveRequestParams = true,
    saveResponseResult = true
)
@PostMapping("/user")
public RespInfo createUser(@RequestBody UserCreateRequest request) {
    // ...
}
```

### `@ApiPermissionIgnore`

跳过 API 权限校验：

```java
@ApiPermissionIgnore
@GetMapping("/public/info")
public RespInfo getPublicInfo() {
    // ...
}
```

### `@IgnoreTenant`

跳过多租户隔离：

```java
@IgnoreTenant
@GetMapping("/health")
public RespInfo health() {
    return RespInfo.success("ok");
}
```

### `@RefreshScope`

动态配置刷新（详见 [动态配置中心](./config.md)）：

```java
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    private String name;
}
```

## 分页查询

### `PageQuery`

通用分页查询参数：

```java
public class PageQuery {
    private Integer pageNum = 1;     // 当前页码（默认 1，最小 1）
    private Integer pageSize = 10;   // 每页大小（默认 10，最大 100）
    private String orderByColumn;    // 排序字段
    private String isAsc;            // 排序方向（asc/desc）

    public <T> Page<T> toPage();     // 转为 MyBatis-Plus Page
}
```

**使用示例**：

```java
@GetMapping("/page")
public RespInfo<Page<User>> list(PageQuery pageQuery) {
    Page<User> page = userMapper.selectPage(
        pageQuery.toPage(),
        Wrappers.<User>lambdaQuery().eq(User::getStatus, 1)
    );
    return RespInfo.success(page);
}
```

## 大数序列化

`BigNumberSerializer` 自动将 `Long`/`BigInteger` 序列化为字符串，避免前端 JavaScript 精度丢失。

## 配置属性

### `AuthProperties`（`forge.auth`）

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `enableApiPermission` | true | 是否启用 API 权限校验 |
| `apiPermissionExcludePaths` | ["/auth/**"] | 排除路径 |
| `enableLoginLock` | true | 是否启用登录锁定 |
| `maxLoginAttempts` | 4 | 最大登录失败次数 |
| `lockDuration` | 30m | 锁定时长 |
| `sameAccountLoginStrategy` | "replace_old" | 同账号登录策略 |

### `CryptoProperties`（`forge.crypto`）

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `enabled` | true | 是否启用加解密 |
| `algorithm` | "SM4" | 加密算法 |
| `enableDynamicKey` | true | 是否启用动态密钥 |
| `enableDesensitize` | true | 是否启用数据脱敏 |

### `LogProperties`（`forge.log`）

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `enableOperationLog` | true | 是否启用操作日志 |
| `enableLoginLog` | true | 是否启用登录日志 |
| `excludePaths` | [...] | 排除路径列表 |
| `printOperationLog` | false | 是否打印操作日志到控制台 |
