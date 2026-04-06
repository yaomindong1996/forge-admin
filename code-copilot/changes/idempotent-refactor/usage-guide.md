# 分布式幂等模块使用指南

## 概述

Forge幂等模块提供了企业级的分布式幂等性解决方案，支持多种幂等策略、Token机制、结果缓存和分布式锁。

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-idempotent</artifactId>
</dependency>
```

### 2. 配置文件

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
      
    token:
      enabled: true
      expire: 300
      header: "X-Idempotent-Token"
      
    lock:
      enabled: true
      wait-time: 3000
      lease-time: 5000
```

## 使用方式

### 方式1：严格模式（STRICT）

**适用场景**：订单创建、支付处理等关键操作

```java
@PostMapping("/order/create")
@Idempotent(
    strategy = IdempotentStrategy.STRICT,
    prefix = "order:",
    key = "#orderRequest.orderId",
    message = "订单正在处理中，请勿重复提交"
)
public RespInfo<Order> createOrder(@RequestBody OrderRequest orderRequest) {
    return RespInfo.success(orderService.create(orderRequest));
}
```

**行为**：
- 第一次请求：执行业务逻辑
- 重复请求：抛出 `IdempotentException`

---

### 方式2：缓存模式（RETURN_CACHE）

**适用场景**：查询类操作、可重复操作

```java
@GetMapping("/order/{orderId}")
@Idempotent(
    strategy = IdempotentStrategy.RETURN_CACHE,
    prefix = "order:query:",
    key = "#orderId",
    cacheExpire = 300
)
public RespInfo<Order> queryOrder(@PathVariable String orderId) {
    return RespInfo.success(orderService.getById(orderId));
}
```

**行为**：
- 第一次请求：执行业务逻辑并缓存结果
- 重复请求：直接返回缓存结果（不执行业务）

---

### 方式3：Token模式（TOKEN_REQUIRED）

**适用场景**：防止前端重复提交、防重放攻击

#### 步骤1：前端获取Token

```javascript
// 前端调用API获取Token
const response = await axios.post('/api/idempotent/token/generate', {
  prefix: 'payment'
});

const token = response.data.data.token; // 例如: "abc123def456..."
```

#### 步骤2：提交请求携带Token

```javascript
// 提交支付请求时携带Token
await axios.post('/api/payment/process', paymentData, {
  headers: {
    'X-Idempotent-Token': token
  }
});
```

#### 步骤3：后端验证

```java
@PostMapping("/payment/process")
@Idempotent(
    strategy = IdempotentStrategy.TOKEN_REQUIRED,
    prefix = "payment:",
    key = "#paymentRequest.paymentId"
)
public RespInfo<PaymentResult> processPayment(@RequestBody PaymentRequest paymentRequest) {
    return RespInfo.success(paymentService.process(paymentRequest));
}
```

**行为**：
- Token验证成功：执行业务逻辑
- Token验证失败：抛出 `TokenInvalidException`

---

## Token API

### 1. 获取Token

**请求**：
```http
POST /api/idempotent/token/generate
Content-Type: application/json

{
  "prefix": "order"  // 可选，默认为 global
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "abc123def456ghi789...",
    "expireSeconds": 300,
    "createTime": 1678923456789
  }
}
```

### 2. 批量获取Token

**请求**：
```http
POST /api/idempotent/token/batch-generate
Content-Type: application/json

{
  "count": 10,
  "prefix": "order"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "token": "abc1...", "expireSeconds": 300 },
    { "token": "abc2...", "expireSeconds": 300 }
  ]
}
```

### 3. 验证Token

**请求**：
```http
POST /api/idempotent/token/validate
Content-Type: application/json

{
  "token": "abc123def456..."
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

---

## 注解属性说明

### @Idempotent 注解

| 属性 | 类型 | 默认值 | 说明 |
|-----|------|-------|------|
| `strategy` | IdempotentStrategy | RETURN_CACHE | 幂等策略 |
| `prefix` | String | "idempotent:" | 幂等Key前缀 |
| `key` | String | "" | 自定义Key（支持SpEL） |
| `expire` | int | 600 | 幂等有效期（秒） |
| `cacheExpire` | int | 3600 | 缓存有效期（秒） |
| `message` | String | "请勿重复提交" | 提示消息 |
| `deleteKeyAfterSuccess` | boolean | false | 成功后删除Key |
| `cacheResult` | boolean | true | 是否缓存结果 |
| `enableMetrics` | boolean | true | 是否开启监控 |

### IdempotentStrategy 枚举

| 策略 | Code | 说明 |
|-----|------|------|
| STRICT | strict | 严格拒绝重复请求 |
| RETURN_CACHE | return_cache | 返回缓存结果 |
| TOKEN_REQUIRED | token_required | 必须携带有效Token |

---

## 监控指标

模块集成了Prometheus监控，提供以下指标：

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `idempotent.requests.total` | Counter | 请求总数 |
| `idempotent.requests.success` | Counter | 成功次数 |
| `idempotent.requests.duplicate` | Counter | 重复次数 |
| `idempotent.cache.returned` | Counter | 缓存返回次数 |
| `idempotent.cache.hit.rate` | Gauge | 缓存命中率 |
| `idempotent.execution.time` | Timer | 执行耗时 |

---

## 异常处理

### IdempotentException

```java
@RestControllerAdvice
public class IdempotentExceptionHandler {
    
    @ExceptionHandler(IdempotentException.class)
    public RespInfo<Void> handleIdempotentException(IdempotentException e) {
        return RespInfo.error(e.getMessage());
    }
}
```

### TokenInvalidException

```java
@ExceptionHandler(TokenInvalidException.class)
public RespInfo<Void> handleTokenInvalidException(TokenInvalidException e) {
    return RespInfo.error("Token无效或已过期");
}
```

---

## 最佳实践

### 1. Key设计原则

- 使用业务唯一标识（如订单ID、支付ID）
- 添加业务前缀区分不同场景
- 避免使用可变参数

```java
// ✅ 好的设计
@Idempotent(key = "#order.orderId")

// ❌ 不好的设计
@Idempotent(key = "#order.createTime")  // 时间会变化
```

### 2. 过期时间设置

- `expire` 应该大于业务处理时间
- `cacheExpire` 建议 ≤ `expire`，避免缓存残留
- Token有效期建议 300秒（5分钟）

### 3. 策略选择

- **关键操作**：STRICT（订单创建、支付）
- **查询操作**：RETURN_CACHE（订单查询）
- **防重复提交**：TOKEN_REQUIRED（前端防抖）

### 4. Redis配置

确保Redis配置正确：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 3000
```

---

## 故障排查

### 问题1：Token验证失败

**原因**：
- Token过期
- Token已被消费
- Token不存在

**解决**：重新获取Token

### 问题2：缓存未命中

**原因**：
- 缓存过期时间过短
- Redis连接异常

**解决**：调整 `cacheExpire` 配置

### 问题3：锁获取失败

**原因**：
- 并发请求过多
- 锁等待时间过短

**解决**：调整 `lock.wait-time` 配置

---

## 技术栈

- Java 17
- Spring Boot 3
- Redisson 3.24.3
- Micrometer (Prometheus)
- Jackson
- Hutool

---

## 相关文档

- 需求规格：`code-copilot/changes/idempotent-refactor/spec.md`
- 实施总结：`code-copilot/changes/idempotent-refactor/implementation-summary.md`
- 项目规范：`AGENTS.md`