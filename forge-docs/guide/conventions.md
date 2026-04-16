# 项目编码规范

本文档定义了 Forge Admin 项目的编码规范和约定，所有贡献者应严格遵守。

## 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 类名 | 大驼峰，见名知意 | `UserService`, `OrderController` |
| 方法名 | 小驼峰，动词开头 | `getUserById()`, `createOrder()` |
| 常量 | 全大写下划线分隔 | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| 抽象类 | 以 `Abstract` 或 `Base` 开头 | `BaseEntity`, `AbstractHandler` |
| 测试类 | 以被测类名开头 + `Test` 结尾 | `UserServiceTest` |

**禁止**：使用拼音或中英文混拼命名。

## 异常处理

| 规则 | 说明 |
|------|------|
| 业务异常 | 使用 `BusinessException`，携带错误码 |
| 系统异常 | 向上抛出，由 `GlobalExceptionHandler` 统一兜底 |
| 禁止吞异常 | 不允许空的 catch 块 |
| catch 中必须记录日志 | 使用 `log.error()` 记录完整堆栈 |

### 全局异常处理

项目通过 `@RestControllerAdvice` + `@ExceptionHandler` 实现统一异常处理，覆盖以下异常类型：

- `BusinessException` → 返回业务错误信息
- `MethodArgumentNotValidException` → 参数校验失败
- `ConstraintViolationException` → 约束违反
- `AccessDeniedException` → 权限不足
- `NullPointerException` → 空指针异常
- 其他 RuntimeException / Exception → 系统内部错误

所有异常统一返回 `RespInfo` 格式。

## 日志规范

| 场景 | 级别 | 要求 |
|------|------|------|
| Controller 入口 | `INFO` | 包含请求关键参数 |
| 异常 | `ERROR` | 包含完整堆栈 |
| 调试信息 | `DEBUG` | 仅在开发环境开启 |

**禁止**：在日志中打印用户敏感信息（手机号、身份证、银行卡等）。

## 通用约定

- **幂等性**：所有写接口必须考虑幂等性
- **并发安全**：涉及并发场景必须说明同步策略
- **魔法值**：必须定义为常量

## 数据库规范

| 规则 | 说明 |
|------|------|
| 字符集 | `utf8mb4` |
| 引擎 | `InnoDB` |
| 审计字段 | 所有业务表必须包含 `id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time` |
| 索引 | 频繁查询的字段必须创建索引，复合索引遵循最左前缀原则 |

## 金额与时间规范

| 类型 | 规则 |
|------|------|
| 金额 | 使用 `long` 类型，单位为 **分** |
| 时间 | 统一使用 `LocalDateTime` |

## 外部接口调用

- 必须设置超时时间（默认 **3s**）
- 必须做降级处理

## 状态变更

- **必须通过状态机**，禁止直接 set 状态字段
- 涉及状态流转的逻辑必须检查状态机合法性

## Git 提交规范

| 规则 | 说明 |
|------|------|
| 分支 | 禁止 master 分支变更，必须使用 feature 分支 |
| Commit 频率 | 每个 task/fix 完成后自动 commit |
| Commit 质量 | 每次提交必须保证编译通过 |
| Push | 禁止自动 push，由用户手动触发 |

### Commit Message 格式

```
[<变更名>] <中文简述>
```

**示例**：
```
[idempotent-refactor] 重构幂等模块，支持多策略和 Redisson 分布式锁
[client-management] 新增客户端管理功能
```

## 按钮样式规范

前端操作按钮使用 UnoCSS 语义化颜色类进行区分：

| 颜色类 | 颜色 | 适用场景 |
|--------|------|---------|
| `text-primary` | 蓝色 | 编辑、查看、授权等主要操作 |
| `text-info` | 灰蓝色 | 查看详情、在线用户、统计信息等 |
| `text-warning` | 黄色 | 刷新缓存、重置、封禁等警告操作 |
| `text-error` | 红色 | 删除、强制下线等危险操作 |
| `text-success` | 绿色 | 启用、发布、通过等成功操作 |

### 使用示例

```vue
<template #table-action="{ row }">
  <a class="text-primary cursor-pointer hover:text-primary-hover">编辑</a>
  <a class="text-info cursor-pointer hover:text-info-hover">在线用户</a>
  <a class="text-warning cursor-pointer hover:text-warning-hover">刷新缓存</a>
  <a class="text-error cursor-pointer hover:text-error-hover">删除</a>
</template>
```

**规则**：同一行的操作按钮应使用不同颜色区分，保证一眼可辨识。

## 安全红线

### 代码安全

- ❌ 禁止在代码中硬编码密钥、AK/SK、数据库密码
- ❌ 禁止提交包含用户个人信息的测试数据
- ❌ 禁止在日志中打印手机号、身份证、银行卡等敏感信息

### 业务安全

- ⚠️ 涉及资金变更 → 必须在 Spec 中标注，人工审查后方可编码
- ⚠️ 涉及状态流转 → 必须检查状态机合法性
- ⚠️ 涉及权限变更 → 必须显式校验操作人权限

## 常用注解

| 注解 | 包路径 | 说明 |
|------|--------|------|
| `@ApiEncrypt` / `@ApiDecrypt` | `core.annotation.crypto` | API 请求/响应加解密 |
| `@OperationLog` | `core.annotation.log` | 操作日志记录 |
| `@ApiPermissionIgnore` | `core.annotation.api` | 跳过权限校验 |
| `@IgnoreTenant` | `core.annotation.tenant` | 跳过租户隔离 |
| `@RefreshScope` | `core.annotation.config` | 动态配置刷新 |
| `@Idempotent` | `starter.idempotent` | 分布式幂等 |
| `@FlowBind` | `flow.client` | 绑定流程模型 |
| `@FlowStart` | `flow.client` | 自动启动流程 |
| `@FlowCallback` | `flow.client` | 流程事件回调 |
