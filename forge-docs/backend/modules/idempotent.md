# 分布式幂等（forge-starter-idempotent）

基于注解的分布式幂等框架，支持多种策略和 Redisson 分布式锁。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-idempotent</artifactId>
</dependency>
```

### 2. 使用 `@Idempotent` 注解

```java
@PostMapping("/order")
@Idempotent(prefix = "order:create", expire = 600, message = "订单创建中，请勿重复提交")
public RespInfo<Order> createOrder(@RequestBody OrderCreateRequest request) {
    // ...
}
```

## 核心功能

### 幂等策略

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| `STRICT` | 严格拒绝，重复请求直接抛出异常 | 支付、下单等严格幂等场景 |
| `RETURN_CACHE`（默认） | 重复请求返回缓存的上次执行结果 | 查询后写、耗时的计算 |
| `TOKEN_REQUIRED` | 必须先获取幂等 Token，携带 Token 才能执行 | 前端表单提交 |

### 注解属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `prefix` | String | `"idempotent:"` | Redis Key 前缀 |
| `expire` | int | 600 | 幂等 Key 过期时间（秒） |
| `key` | String | `""` | SpEL 表达式，用于自定义幂等 Key |
| `message` | String | `"请勿重复提交"` | 重复提交时的提示信息 |
| `deleteKeyAfterSuccess` | boolean | false | 成功后是否删除幂等 Key |
| `strategy` | IdempotentStrategy | `RETURN_CACHE` | 幂等策略 |
| `cacheExpire` | int | 3600 | 缓存结果过期时间（秒） |
| `cacheResult` | boolean | true | 是否缓存执行结果 |
| `enableMetrics` | boolean | true | 是否启用 Prometheus 指标 |

### TOKEN_REQUIRED 策略 — Token API

使用 `TOKEN_REQUIRED` 策略时，需要先获取幂等 Token：

```
POST /api/idempotent/token/generate
{
  "prefix": "order:create",
  "expire": 300
}

# 返回
{
  "token": "a1b2c3d4-..."
}
```

执行请求时在请求头中携带 Token：

```
POST /api/order
X-Idempotent-Token: a1b2c3d4-...
Content-Type: application/json

{...}
```

### SpEL 表达式自定义 Key

```java
@Idempotent(prefix = "user:update", key = "#userId", expire = 300)
public RespInfo updateUser(@PathVariable Long userId, @RequestBody UserUpdateRequest request) {
    // 幂等 Key = "user:update:" + userId
}
```

## 配置项

```yaml
forge:
  idempotent:
    enabled: true
    prefix: "idempotent:"
    expire: 600
    message: "请勿重复提交"
    cache:
      enabled: true
      expire: 3600
      maxSize: 10000
    lock:
      enabled: true
      waitTime: 3000   # 分布式锁等待时间（ms）
      leaseTime: 5000  # 分布式锁持有时间（ms）
    token:
      enabled: true
      expire: 300      # Token 过期时间（秒）
      header: "X-Idempotent-Token"
```

## 核心组件

| 组件 | 说明 |
|------|------|
| `IdempotentAspect` | AOP 切面，拦截 `@Idempotent` 注解的方法 |
| `IdempotentKeyGenerator` | 幂等 Key 生成器，支持 SpEL 表达式 |
| `RedisTokenService` | Redis Token 服务，管理 Token 生命周期 |
| `RedisResultCacheService` | 结果缓存服务，缓存执行结果 |
| `RedissonLockManager` | Redisson 分布式锁管理器（带 Watchdog） |
| `StrictStrategyHandler` | STRICT 策略处理器 |
| `ReturnCacheStrategyHandler` | RETURN_CACHE 策略处理器 |
| `TokenRequiredStrategyHandler` | TOKEN_REQUIRED 策略处理器 |

## 执行流程

```
请求进入 → 生成/解析幂等 Key → 选择策略
  → STRICT: 尝试获取锁 → 失败则抛异常
  → RETURN_CACHE: 查缓存 → 有则返回，无则执行并缓存
  → TOKEN_REQUIRED: 验证 Token → 通过后走 RETURN_CACHE 流程
```
