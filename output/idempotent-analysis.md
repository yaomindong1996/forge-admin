# 分布式幂等落地实战：从 Token 到 Redisson 锁

> 基于 forge-starter-idempotent 模块源码深度分析

## 一、背景与痛点

在分布式系统中，网络超时、用户重复点击、消息队列 at-least-once 投递等场景都会导致**重复请求**。如果业务接口不具备幂等性，就会出现：
- 用户重复提交表单 → 生成两笔相同订单
- 支付回调重复到达 → 用户被重复扣款
- MQ 消费重试 → 库存被多次扣减

本文基于 forge 框架的 `forge-starter-idempotent` 模块，拆解一个可落地的分布式幂等方案的设计与实现。

---

## 二、模块整体架构

```
forge-starter-idempotent/
├── annotation/          @Idempotent 注解定义
├── aop/                IdempotentAspect 切面（入口）
├── config/             IdempotentAutoConfiguration 自动配置
├── constant/           常量定义
├── controller/         Token 获取接口
├── dto/                DTO 对象（TokenInfoDTO / IdempotentResult）
├── enums/              IdempotentStrategy 策略枚举
├── exception/          异常定义
├── generator/          幂等键生成器（含 SpEL 支持）
├── lock/               Redisson 分布式锁管理器
├── metrics/            Prometheus 监控指标
├── properties/         配置属性类
├── service/            Token / 结果缓存 / 存储服务
├── strategy/           策略处理器（STRICT / RETURN_CACHE / TOKEN_REQUIRED）
└── util/              SpEL 解析工具
```

**核心流程**：

```
请求进入
  → @Idempotent 注解标记的方法
    → IdempotentAspect 拦截
      → DefaultIdempotentKeyGenerator 生成幂等键（支持 SpEL）
        → 根据 strategy 选择对应 StrategyHandler
          → 严格模式：Redisson 锁判断重复 → 直接抛异常
          → 缓存模式：Redisson 锁 + 结果缓存 → 返回缓存结果
          → Token 模式：验证 Token → 消费 Token → 执行缓存模式逻辑
```

---

## 三、@Idempotent 注解设计

### 3.1 注解定义

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String prefix() default "idempotent:";       // 幂等键前缀
    int expire() default 600;                     // 幂等键过期时间（秒）
    String key() default "";                      // SpEL 表达式，动态生成幂等键
    String message() default "请勿重复提交";      // 重复提交提示消息
    boolean deleteKeyAfterSuccess() default false;// 成功后是否删除幂等键
    IdempotentStrategy strategy() default RETURN_CACHE; // 幂等策略
    int cacheExpire() default 3600;              // 结果缓存过期时间（秒）
    boolean cacheResult() default true;           // 是否缓存执行结果
    boolean enableMetrics() default true;         // 是否开启监控指标
}
```

### 3.2 使用示例

```java
// 示例1：基础用法（返回缓存结果）
@Idempotent(key = "'order:' + #orderId")
public OrderDTO createOrder(Long orderId) {
    // 业务逻辑
}

// 示例2：严格模式（直接拒绝重复请求）
@Idempotent(
    key = "'payment:' + #request.paymentId",
    strategy = IdempotentStrategy.STRICT,
    message = "支付请求正在处理中，请勿重复提交"
)
public PaymentResult pay(PaymentRequest request) {
    // 支付逻辑
}

// 示例3：Token 模式（适用于表单提交场景）
@Idempotent(
    key = "'transfer:' + #requestId",
    strategy = IdempotentStrategy.TOKEN_REQUIRED,
    deleteKeyAfterSuccess = true   // 一次性操作，成功后删除键
)
public TransferResult transfer(TransferRequest request) {
    // 转账逻辑
}
```

---

## 四、幂等键生成：SpEL 动态表达式

### 4.1 核心实现（DefaultIdempotentKeyGenerator）

```java
public String generate(ProceedingJoinPoint joinPoint, String prefix, String key) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    Object[] args = joinPoint.getArgs();
    String[] paramNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);

    String keyValue;
    if (key != null && !key.isEmpty()) {
        // 使用 SpEL 解析动态键
        Object spelResult = SpelUtil.parse(key, args, paramNames);
        keyValue = spelResult != null ? spelResult.toString() : "";
    } else {
        // 兜底方案：方法签名 + 参数 MD5
        String methodSign = method.getDeclaringClass().getName() + ":" + method.getName();
        String argsStr = Arrays.stream(args)
            .map(arg -> arg != null ? arg.toString() : "null")
            .collect(Collectors.joining(","));
        keyValue = DigestUtil.md5Hex(methodSign + ":" + argsStr);
    }
    return prefix + keyValue;
}
```

### 4.2 SpEL 工具类（SpelUtil）

```java
public static Object parse(String expression, Object[] args, String[] paramNames) {
    try {
        EvaluationContext context = new StandardEvaluationContext();
        if (args != null && paramNames != null) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        Expression exp = PARSER.parseExpression(expression);
        return exp.getValue(context);
    } catch (Exception e) {
        log.warn("SpEL表达式解析失败: {}", expression, e);
        return null;
    }
}
```

### 4.3 SpEL 表达式实战技巧

| 场景 | SpEL 表达式 | 说明 |
|------|-------------|------|
| 按单个参数生成键 | `'order:' + #orderId` | 直接引用方法参数名 |
| 引用对象属性 | `'payment:' + #request.paymentId` | 访问参数对象的属性 |
| 多参数组合 | `'user:' + #userId + ':action:' + #actionType` | 多字段拼接 |
| 常量键（全量防重） | `'global:submit'` | 同一接口所有请求共享一个键 |

> **注意**：SpEL 解析失败时（`parse` 返回 `null`），`keyValue` 会变为空字符串，最终键为 `prefix + ""`，可能导致所有请求键相同。生产环境建议配合参数校验使用。

---

## 五、三种幂等策略详解

### 5.1 策略枚举

```java
public enum IdempotentStrategy {
    STRICT("strict", "严格拒绝重复请求"),
    RETURN_CACHE("return_cache", "返回上次缓存结果"),
    TOKEN_REQUIRED("token_required", "必须携带有效Token");
}
```

### 5.2 严格模式（STRICT）—— 直接拒绝

**适用场景**：支付、转账等不允许任何重复的业务。

**核心逻辑（StrictStrategyHandler）**：

```java
public Object handle(ProceedingJoinPoint joinPoint, Idempotent annotation, String idempotentKey) throws Throwable {
    // 1. 尝试获取 Redisson 分布式锁
    if (!lockManager.tryLock(idempotentKey, waitTime, leaseTime)) {
        // 获取锁失败 = 已有同名请求正在处理 = 重复请求
        throw new IdempotentException(annotation.message());
    }
    try {
        // 2. 获取锁成功，执行业务逻辑
        Object result = joinPoint.proceed();
        // 3. 如果配置了一键删除，执行后释放锁
        if (annotation.deleteKeyAfterSuccess()) {
            lockManager.unlock(idempotentKey);
        }
        return result;
    } catch (Throwable e) {
        lockManager.unlock(idempotentKey);
        throw e;
    }
}
```

**流程图**：

```
请求A（key=order:123）到来
  → tryLock("idempotent:lock:idempotent:order:123") 成功
    → 执行业务
      → 返回结果

请求B（相同key=order:123）在请求A处理期间到来
  → tryLock(...) 失败（锁被请求A持有）
    → 抛出 IdempotentException("请勿重复提交")
```

### 5.3 缓存模式（RETURN_CACHE）—— 返回缓存结果

**适用场景**：查询接口、允许返回旧结果的幂等写操作。

**核心逻辑（ReturnCacheStrategyHandler）**：

```java
public Object handle(ProceedingJoinPoint joinPoint, Idempotent annotation, String idempotentKey) throws Throwable {
    // 1. 先查缓存，如果已有成功结果直接返回
    IdempotentResult cachedResult = resultCacheService.getCachedResult(idempotentKey);
    if (cachedResult != null && "SUCCESS".equals(cachedResult.getStatus())) {
        return cachedResult.getResult();  // 直接返回缓存结果
    }

    // 2. 尝试获取分布式锁
    if (!lockManager.tryLock(idempotentKey, waitTime, leaseTime)) {
        // 获取锁失败，等待100ms后再次查询缓存（处理中的请求可能已完成）
        Thread.sleep(100);
        cachedResult = resultCacheService.getCachedResult(idempotentKey);
        if (cachedResult != null && "SUCCESS".equals(cachedResult.getStatus())) {
            return cachedResult.getResult();
        }
        throw new IdempotentException("并发冲突，请稍后重试");
    }

    try {
        // 3. 获取锁成功，执行业务
        Object result = joinPoint.proceed();

        // 4. 缓存结果（如果开启了 cacheResult）
        if (annotation.cacheResult()) {
            resultCacheService.cacheResult(idempotentKey, result, annotation.cacheExpire());
        }
        return result;
    } catch (Throwable e) {
        lockManager.unlock(idempotentKey);
        throw e;
    }
}
```

**关键设计**：获取锁失败后等待 100ms 再查一次缓存，可以覆盖「请求A正在执行，请求B同时到来」的并发场景，避免不必要的报错。

### 5.4 Token 模式（TOKEN_REQUIRED）—— 前端先拿号，后端凭号入场

**适用场景**：表单提交、需要用户主动触发获取 Token 的场景（如订单确认页）。

**完整流程**：

```
[前端] 进入表单页面
  → 调用 GET /api/idempotent/token/generate?prefix=order
    → 后端生成 Token 写入 Redis（状态 UNUSED，TTL=300s）
      → 返回 Token 给前端

[前端] 用户填写表单，点击提交
  → 请求头携带 X-Idempotent-Token: <token>
    → IdempotentAspect 拦截
      → TokenRequiredStrategyHandler 处理
        → 1. 从请求头提取 Token
        → 2. 调用 tokenService.validateToken(token, prefix) 验证
          → 检查 Redis 中 Token 是否存在且状态为 UNUSED
        → 3. 调用 tokenService.consumeToken(token, prefix) 消费
          → 将 Redis 中 Token 状态改为 CONSUMED，TTL 改为 60s
        → 4. 委托给 RETURN_CACHE 策略继续处理
```

**Token 的 Redis 存储结构**：

```
Key:   idempotent:token:{prefix}:{tokenValue}
Value: Hash
  - createTime: 生成时间戳
  - status:     UNUSED / CONSUMED
TTL:   300s（未消费） / 60s（已消费，给足够时间让请求处理完）
```

**消费后 TTL 改为 60s 的原因**：Token 被消费后，对应的幂等键可能还在 Redis 中（缓存模式），给 60s 让后续重复请求能正确识别为重复，之后自动过期，避免 Redis 内存泄漏。

---

## 六、Redisson 分布式锁实现

### 6.1 RedissonLockManager

```java
public class RedissonLockManager implements LockManager {
    private final RedissonClient redissonClient;
    private static final String LOCK_KEY_PREFIX = "idempotent:lock:";

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
        String fullLockKey = LOCK_KEY_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        // waitTime: 最多等待多久（默认 3000ms）
        // leaseTime: 锁自动释放时间（默认 5000ms）
        return lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unlock(String lockKey) {
        String fullLockKey = LOCK_KEY_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### 6.2 锁的 Key 生成规则

| 原始幂等键 | Redisson 锁键 |
|------------|--------------|
| `idempotent:order:123` | `idempotent:lock:idempotent:order:123` |

锁键 = `LOCK_KEY_PREFIX + 幂等键`，即固定前缀 `idempotent:lock:` 拼接完整幂等键。

### 6.3 锁超时配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `forge.idempotent.lock.wait-time` | 3000ms | 获取锁的最大等待时间 |
| `forge.idempotent.lock.lease-time` | 5000ms | 锁的自动释放时间（防止死锁） |

> **注意**：`leaseTime` 必须大于业务的最大执行时间，否则锁提前释放会导致幂等失效。如果业务执行时间不可预测，建议不设置 `leaseTime`（设为 -1），使用 Redisson 的 Watch Dog 自动续期机制。

---

## 七、结果缓存机制

### 7.1 Redis 缓存结构

```
Key:   idempotent:cache:{幂等键}
Value: Hash
  - requestId:   请求唯一ID（UUID）
  - result:      序列化后的执行结果（JSON）
  - executeTime: 执行时间戳
  - status:      SUCCESS / FAILED / PROCESSING
TTL:   cacheExpire（默认 3600s，可通过 @Idempotent(cacheExpire=...) 配置）
```

### 7.2 缓存模式的处理流程

```
请求到来
  → 查询缓存：idempotent:cache:{key} 是否存在且 status=SUCCESS
    → 存在：直接返回缓存的 result（不执行业务）
    → 不存在：
        → 尝试获取分布式锁
          → 获取成功：
              → 执行业务
              → 缓存结果到 idempotent:cache:{key}
              → 返回结果
          → 获取失败（并发）：
              → 等待 100ms
              → 再次查询缓存
                → 有结果：返回缓存
                → 无结果：抛异常"并发冲突"
```

### 7.3 缓存模式 vs 严格模式对比

| 维度 | 缓存模式（RETURN_CACHE） | 严格模式（STRICT） |
|------|------------------------|-------------------|
| 重复请求处理 | 返回上次执行结果 | 直接抛异常 |
| 是否需要缓存 | 是（需要 `resultCacheService`） | 否 |
| 适用场景 | 查询、允许返回旧数据的写操作 | 支付、转账等严格防重场景 |
| 用户体验 | 好（无感知） | 差（需要用户手动重试） |

---

## 八、自动配置与配置属性

### 8.1 自动配置类（IdempotentAutoConfiguration）

通过 Spring Boot 的自动配置机制，所有 Bean 都支持用户自定义覆盖（`@ConditionalOnMissingBean`）。

```java
@AutoConfiguration
@EnableConfigurationProperties({
    IdempotentProperties.class,
    TokenProperties.class,
    CacheProperties.class,
    LockProperties.class
})
@ConditionalOnProperty(prefix = "forge.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotentAutoConfiguration {

    // 1. 幂等键生成器（支持 SpEL）
    @Bean @ConditionalOnMissingBean
    public IdempotentKeyGenerator idempotentKeyGenerator() {
        return new DefaultIdempotentKeyGenerator();
    }

    // 2. Token 服务（基于 Redis）
    @Bean @ConditionalOnMissingBean
    public TokenService tokenService(StringRedisTemplate redisTemplate, TokenProperties properties) {
        return new RedisTokenService(redisTemplate, properties);
    }

    // 3. 结果缓存服务（基于 Redis + Jackson）
    @Bean @ConditionalOnMissingBean
    public ResultCacheService resultCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisResultCacheService(redisTemplate, objectMapper);
    }

    // 4. 分布式锁管理器（基于 Redisson，条件装配）
    @Bean @ConditionalOnMissingBean @ConditionalOnClass(RedissonClient.class)
    public LockManager lockManager(RedissonClient redissonClient) {
        return new RedissonLockManager(redissonClient);
    }

    // 5. 三种策略处理器
    @Bean
    public IdempotentStrategyHandler strictStrategyHandler(...) { ... }
    @Bean
    public IdempotentStrategyHandler returnCacheStrategyHandler(...) { ... }
    @Bean
    public IdempotentStrategyHandler tokenRequiredStrategyHandler(...) { ... }

    // 6. 策略处理器 Map（注入到 IdempotentAspect）
    @Bean
    public Map<IdempotentStrategy, IdempotentStrategyHandler> strategyHandlers(...) { ... }

    // 7. 幂等切面（核心入口）
    @Bean
    public IdempotentAspect idempotentAspect(...) { ... }

    // 8. Token 控制器（可选，通过 forge.idempotent.token.enabled 控制）
    @Bean
    @ConditionalOnProperty(prefix = "forge.idempotent.token", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IdempotentTokenController idempotentTokenController(...) { ... }
}
```

### 8.2 配置属性一览

```yaml
forge:
  idempotent:
    enabled: true                    # 是否开启幂等组件（默认 true）
    prefix: "idempotent:"            # 幂等键默认前缀
    expire: 600                      # 幂等键默认过期时间（秒）
    message: "请勿重复提交"           # 默认重复提交提示消息
    token:
      enabled: true                  # 是否开启 Token 控制器（默认 true）
      expire: 300                    # Token 默认过期时间（秒）
      header: "X-Idempotent-Token"   # Token 请求头名称
    cache:
      enabled: true                  # 是否开启结果缓存（默认 true）
      expire: 3600                   # 结果缓存默认过期时间（秒）
      maxSize: 10000                 # 最大缓存条数（预留，当前未使用）
    lock:
      enabled: true                  # 是否开启分布式锁（默认 true）
      wait-time: 3000                # 获取锁最大等待时间（毫秒）
      lease-time: 5000               # 锁自动释放时间（毫秒）
```

---

## 九、Token 机制完整流程

### 9.1 前端获取 Token

**接口**：`POST /api/idempotent/token/generate`

```bash
# 请求
curl -X POST "http://localhost:8080/api/idempotent/token/generate?prefix=order"

# 响应
{
  "code": 200,
  "data": {
    "token": "a1b2c3d4e5f6...",
    "expire": 300,
    "createTime": 1716800000000
  }
}
```

**批量获取**（适用于批量操作场景）：

```bash
curl -X POST "http://localhost:8080/api/idempotent/token/batch-generate?count=5&prefix=order"
```

### 9.2 前端提交时携带 Token

```javascript
// 获取 Token（进入表单页时调用）
const { data } = await axios.post('/api/idempotent/token/generate', { prefix: 'order' });
const token = data.token;

// 提交表单时携带 Token
await axios.post('/api/order/create', orderData, {
    headers: { 'X-Idempotent-Token': token }
});
```

### 9.3 后端验证与消费 Token

```java
// TokenRequiredStrategyHandler 核心逻辑
String token = extractToken();  // 从请求头 X-Idempotent-Token 提取

if (!tokenService.validateToken(token, prefix)) {
    throw new TokenInvalidException("Token无效或已过期");
}

tokenService.consumeToken(token, prefix);  // 将 Token 状态改为 CONSUMED

return delegateHandler.handle(joinPoint, annotation, idempotentKey);  // 继续走缓存模式逻辑
```

---

## 十、监控指标（Prometheus）

模块内置了完整的 Prometheus 监控指标，通过 `IdempotentMetrics` 类实现：

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `idempotent_requests_total` | Counter | 幂等请求总数 |
| `idempotent_requests_success` | Counter | 成功执行的幂等请求数 |
| `idempotent_requests_duplicate` | Counter | 重复请求次数 |
| `idempotent_cache_returned` | Counter | 返回缓存结果的次数 |
| `idempotent_token_invalid` | Counter | Token 无效次数 |
| `idempotent_requests_failed` | Counter | 失败的幂等请求数 |
| `idempotent_execution_time` | Timer | 幂等执行耗时 |
| `idempotent_lock_acquire_time` | Timer | 获取锁耗时 |
| `idempotent_cache_lookup_time` | Timer | 缓存查询耗时 |
| `idempotent_cache_hit_rate` | Gauge | 缓存命中率 |

**开启方式**：在 `@Idempotent` 注解中设置 `enableMetrics = true`（默认开启），并确保项目中引入了 `micrometer-registry-prometheus` 依赖。

---

## 十一、适用场景总结

| 业务场景 | 推荐策略 | 配置示例 |
|---------|---------|---------|
| 表单重复提交 | TOKEN_REQUIRED | `@Idempotent(strategy = TOKEN_REQUIRED)` |
| 支付接口 | STRICT + deleteKeyAfterSuccess | `@Idempotent(strategy = STRICT, deleteKeyAfterSuccess = true)` |
| 审批操作 | STRICT | `@Idempotent(strategy = STRICT, key = "'approve:' + #approvalId")` |
| 查询接口 | RETURN_CACHE | `@Idempotent(strategy = RETURN_CACHE, key = "'query:' + #queryId")` |
| 转账接口 | TOKEN_REQUIRED + deleteKeyAfterSuccess | `@Idempotent(strategy = TOKEN_REQUIRED, deleteKeyAfterSuccess = true)` |
| MQ 消费幂等 | RETURN_CACHE | `@Idempotent(strategy = RETURN_CACHE, key = "'mq:' + #messageId")` |

---

## 十二、使用注意事项

### 12.1 SpEL 表达式为空时的兜底行为

当 `@Idempotent(key = "")`（默认值）时，使用「方法签名 + 参数 toString」的 MD5 作为幂等键。这意味着：
- 如果方法参数没有正确实现 `toString()`，不同请求可能生成相同的键
- **建议**：关键业务接口始终显式指定 `key` 属性

### 12.2 Redisson 锁的 leaseTime 配置

`leaseTime` 默认 5000ms，如果业务执行时间超过 5s，锁会提前释放，导致幂等失效。解决方案：
```yaml
forge:
  idempotent:
    lock:
      lease-time: 30000   # 改为 30s，或设为 -1 启用 Watch Dog
```

### 12.3 Token 模式的并发安全

Token 验证和消费是两个操作，存在极小概率的竞态条件。当前实现中 `validateToken` 和 `consumeToken` 是两个独立的 Redis 操作，高并发场景下理论上可能两个请求同时通过 `validateToken`。**建议**在 `consumeToken` 中使用 Lua 脚本保证原子性：

```lua
-- 原子消费 Token 的 Lua 脚本（当前版本未实现，可作为优化方向）
if redis.call('HGET', KEYS[1], 'status') == 'UNUSED' then
    redis.call('HSET', KEYS[1], 'status', 'CONSUMED')
    redis.call('EXPIRE', KEYS[1], 60)
    return 1
else
    return 0
end
```

### 12.4 缓存模式的序列化限制

`RedisResultCacheService` 使用 `ObjectMapper.writeValueAsString(result)` 缓存结果，要求返回值必须可序列化。如果返回值是 `InputStream`、数据库连接等资源对象，缓存会失败（已被 try-catch 吞掉异常，仅打日志）。

---

## 十三、与同类方案对比

| 方案 | 幂等键生成 | 分布式锁 | Token 支持 | 结果缓存 | 监控指标 |
|------|-----------|---------|-----------|---------|---------|
| **forge-starter-idempotent** | SpEL 动态表达式 | Redisson（可配置） | 完整支持 | 支持（可配置过期时间） | Prometheus 完整指标 |
| Spring Retry | 不支持 | 不支持 | 不支持 | 不支持 | 无 |
| Redis SETNX 手写 | 手动实现 | 需手写 | 需手写 | 需手写 | 无 |
| 美团 Leaf | 仅 ID 生成 | 不支持 | 不支持 | 不支持 | 有 |

---

## 十四、总结

`forge-starter-idempotent` 提供了一个**生产级**的分布式幂等解决方案，核心亮点：

1. **SpEL 动态幂等键**：支持通过方法参数动态生成幂等键，灵活适配各种业务场景
2. **三种策略可切换**：STRICT（严格拒绝）、RETURN_CACHE（返回缓存）、TOKEN_REQUIRED（Token 模式），覆盖大部分幂等场景
3. **Redisson 分布式锁**：基于 Redisson 实现，支持可配置的等待时间和自动释放，避免死锁
4. **结果缓存**：重复请求直接返回缓存结果，对调用方透明
5. **Token 机制**：前端先获取 Token，提交时携带，有效防止表单重复提交
6. **Prometheus 监控**：内置完整的监控指标，方便观测幂等拦截效果和系统健康度
7. **自动配置**：Spring Boot Starter 方式接入，零代码接入（仅需添加注解）

**接入方式**：

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-idempotent</artifactId>
</dependency>
```

```java
// 在需要幂等的方法上添加注解即可
@Idempotent(key = "'order:' + #orderId", strategy = IdempotentStrategy.RETURN_CACHE)
public OrderDTO createOrder(Long orderId) {
    // ...
}
```
