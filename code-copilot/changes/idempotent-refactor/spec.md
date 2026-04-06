# 分布式幂等模块优化 Spec

## 变更概述

**变更名称**: idempotent-refactor  
**变更类型**: 功能重构  
**优先级**: 高  
**影响范围**: forge-starter-idempotent 模块（破坏性升级）  
**预计工作量**: 3-5人日  

---

## 一、背景与动机

### 1.1 业务背景

在企业级应用中，分布式幂等性是保障数据一致性和系统可靠性的核心能力。Forge Admin框架作为企业级后台管理系统，需要处理大量关键业务操作（如订单创建、支付处理、库存扣减等），这些操作必须具备严格的幂等性保障，防止重复提交导致的业务异常。

### 1.2 当前问题

现有幂等模块（`forge-starter-idempotent`）存在以下关键缺陷：

| 问题类别 | 具体问题 | 业务影响 |
|---------|---------|---------|
| **功能缺失** | 无结果缓存能力，重复请求仅拒绝无法返回原结果 | 用户无法得知上次操作结果，体验差 |
| **安全不足** | 缺少Token机制，无法防范CSRF和恶意重放攻击 | 存在安全隐患，可被恶意攻击 |
| **监控缺失** | 无监控统计，无法评估幂等效果和性能 | 无法发现问题和优化 |
| **锁实现简单** | 仅使用SETNX，无看门狗续期，极端情况可能失效 | 并发场景可靠性不足 |
| **策略单一** | 仅支持拒绝重复，无多种幂等策略 | 灵活性不足，不同场景需求无法满足 |
| **存储单一** | 仅支持Redis，无降级方案 | Redis故障时系统不可用 |

### 1.3 优化目标

参考主流开源项目（如美团Leaf、goku-framework、yue-library）的设计理念，构建企业级幂等解决方案：

1. **增强功能完整性**：支持结果缓存、Token机制、多种幂等策略
2. **提升可靠性**：集成Redisson分布式锁，支持看门狗自动续期
3. **完善可观测性**：集成Prometheus监控，提供全链路追踪
4. **优化用户体验**：重复请求返回缓存结果，提升响应速度
5. **增强安全性**：Token机制防范恶意重放和CSRF攻击

---

## 二、现状分析

### 2.1 现有架构

```
现有实现：
├── IdempotentAutoConfiguration    # 自动配置
├── IdempotentAspect               # AOP切面拦截
├── @Idempotent                    # 注解（功能简单）
├── RedisIdempotentStorageService  # Redis存储（仅SETNX）
├── DefaultIdempotentKeyGenerator  # Key生成（SpEL支持）
└── IdempotentException            # 异常定义

缺失组件：
❌ 结果缓存服务
❌ Token服务
❌ 分布式锁管理（Redisson）
❌ 幂等策略抽象
❌ 监控指标
❌ Token API
```

### 2.2 现有注解能力

```java
@Idempotent(
    prefix = "order:",         // 前缀
    expire = 600,              // 过期时间
    key = "#orderId",          // SpEL表达式
    message = "请勿重复提交",   // 提示消息
    deleteKeyAfterSuccess = false
)
```

**缺陷分析**：
- ✅ 支持SpEL自定义Key
- ❌ 无幂等策略配置（仅拒绝）
- ❌ 无结果缓存开关
- ❌ 无Token验证配置
- ❌ 无监控开关

### 2.3 现有流程

```
请求 → Aspect拦截 → 生成Key → SETNX尝试获取
  ↓
成功 → 执行业务 → 返回结果
  ↓
失败 → 抛异常（拒绝）
```

**问题**：
- 无法返回上次执行结果
- 无Token验证环节
- 无监控记录
- 异常时直接释放锁（无降级）

---

## 三、需求规格

### 3.1 核心功能需求

#### 功能1：幂等结果缓存

**需求描述**：当重复请求发生时，系统应能返回上次成功执行的业务结果，而非直接拒绝。

**业务价值**：
- 提升用户体验（无需重新提交）
- 减少系统负载（避免重复执行业务逻辑）
- 支持查询类操作的快速响应

**功能规格**：
- 缓存存储：Redis Hash结构
- 缓存内容：业务执行结果 + 执行时间 + 状态
- 缓存有效期：可配置（默认3600秒）
- 缓存命中率监控：支持Prometheus指标

#### 功能2：Token机制

**需求描述**：提供Token生成、验证、消费机制，防范前端重复提交和恶意重放攻击。

**业务价值**：
- 防范CSRF攻击（Token绑定用户）
- 防止恶意重放（Token单次消费）
- 前端防重复提交（Token提前获取）

**功能规格**：
- Token生成：UUID + Redis存储
- Token验证：存在性 + 用户归属 + 消费状态
- Token消费：使用后标记为已消费，短TTL保留
- Token API：提供REST接口供前端调用

#### 功能3：多种幂等策略

**需求描述**：支持不同业务场景的幂等处理策略，而非单一的拒绝模式。

**策略定义**：

| 策略名称 | 处理逻辑 | 适用场景 |
|---------|---------|---------|
| **STRICT** | 严格拒绝重复请求 | 关键操作，必须唯一执行 |
| **RETURN_CACHE** | 返回上次缓存结果 | 查询类、可重复操作 |
| **TOKEN_REQUIRED** | Token验证优先 | 防恶意重放、前端防重复 |

#### 功能4：Redisson分布式锁

**需求描述**：使用Redisson实现增强型分布式锁，支持看门狗自动续期，提升并发可靠性。

**业务价值**：
- 长时间业务执行时锁不失效（看门狗续期）
- 公平锁机制，避免锁竞争不公平
- 锁释放可靠性提升

**功能规格**：
- 锁实现：Redisson RLock
- 看门狗：自动续期（默认30秒）
- 锁等待：可配置等待时间（默认3秒）
- 锁租约：可配置租约时间（默认5秒）

#### 功能5：监控统计

**需求描述**：集成Prometheus/Micrometer，提供幂等效果的监控指标。

**监控指标**：

| 指标 | 类型 | 说明 |
|-----|------|------|
| `idempotent.requests.total` | Counter | 请求总数 |
| `idempotent.requests.success` | Counter | 成功次数 |
| `idempotent.requests.duplicate` | Counter | 重复次数 |
| `idempotent.cache.returned` | Counter | 缓存返回次数 |
| `idempotent.cache.hit.rate` | Gauge | 缓存命中率 |
| `idempotent.execution.time` | Timer | 执行耗时分布 |
| `idempotent.lock.acquire.time` | Timer | 锁获取耗时 |

---

## 四、技术设计

### 4.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                Idempotent Framework v2.0                 │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Annotation  │  │   Aspect     │  │   Context    │  │
│  │  @Idempotent │  │  Intercept   │  │   Manager    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Token      │  │   Result     │  │    Lock      │  │
│  │   Service    │  │   Cache      │  │   Manager    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Monitor    │  │   Storage    │  │   Strategy   │  │
│  │   Metrics    │  │   (Redis)    │  │   Provider   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### 4.2 核心注解设计

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    
    /**
     * 幂等策略
     */
    IdempotentStrategy strategy() default IdempotentStrategy.RETURN_CACHE;
    
    /**
     * 幂等Key前缀
     */
    String prefix() default "idempotent:";
    
    /**
     * 自定义幂等Key（支持SpEL）
     */
    String key() default "";
    
    /**
     * 幂等有效期（秒）
     */
    int expire() default 600;
    
    /**
     * 结果缓存有效期（秒）
     */
    int cacheExpire() default 3600;
    
    /**
     * 重复请求提示消息
     */
    String message() default "请勿重复提交";
    
    /**
     * 成功后删除幂等Key
     */
    boolean deleteKeyAfterSuccess() default false;
    
    /**
     * 是否缓存执行结果
     */
    boolean cacheResult() default true;
    
    /**
     * 是否开启监控
     */
    boolean enableMetrics() default true;
}
```

### 4.3 幂等策略枚举

```java
public enum IdempotentStrategy {
    
    /**
     * 严格模式：拒绝重复请求
     */
    STRICT,
    
    /**
     * 缓存模式：返回上次执行结果
     */
    RETURN_CACHE,
    
    /**
     * Token模式：必须携带有效Token
     */
    TOKEN_REQUIRED
}
```

### 4.4 Redis存储结构

```redis
# 幂等Key（标记请求是否已处理）
Key: idempotent:{prefix}:{businessKey}
Value: {
  "requestId": "uuid",
  "createTime": timestamp,
  "status": "PROCESSING|SUCCESS|FAILED"
}
TTL: expire秒

# 结果缓存
Key: idempotent:cache:{prefix}:{businessKey}
Value: {
  "requestId": "uuid",
  "result": "序列化结果",
  "executeTime": timestamp,
  "status": "SUCCESS|FAILED"
}
TTL: cacheExpire秒

# Token存储
Key: idempotent:token:{prefix}:{tokenValue}
Value: {
  "createTime": timestamp,
  "userId": "用户ID",
  "status": "UNUSED|CONSUMED"
}
TTL: tokenExpire秒

# 分布式锁（Redisson）
Key: idempotent:lock:{prefix}:{businessKey}
Value: Redisson内部管理（看门狗续期）
```

### 4.5 核心流程设计

```
请求进入
    ↓
[1] 解析注解配置
    ↓
[2] 生成幂等Key（SpEL + Hash）
    ↓
[3] Token验证（如果策略=TOKEN_REQUIRED）
    ├─ 提取Token Header
    ├─ 验证Token存在性
    ├─ 验证Token用户归属
    ├─ 验证Token未消费
    └─ 消费Token
    ↓
[4] 根据策略执行
    ├─ STRICT: tryAcquire → 执行 → 返回
    ├─ RETURN_CACHE: 检查缓存 → 有则返回 → 无则获取锁 → 执行 → 缓存结果
    └─ TOKEN_REQUIRED: Token验证 → 执行上述逻辑
    ↓
[5] 执行业务逻辑
    ├─ 成功 → 缓存结果 → deleteKey判断 → 返回
    └─ 失败 → 释放锁 → 抛异常
    ↓
[6] 记录监控指标
    ↓
请求结束
```

### 4.6 Token API设计

```java
@RestController
@RequestMapping("/api/idempotent/token")
public class IdempotentTokenController {
    
    /**
     * 获取Token
     */
    @PostMapping("/generate")
    public RespInfo<TokenInfo> generateToken(@RequestParam(required = false) String prefix);
    
    /**
     * 批量获取Token
     */
    @PostMapping("/batch-generate")
    public RespInfo<List<TokenInfo>> batchGenerateToken(@RequestParam int count);
    
    /**
     * 验证Token（前端预检查）
     */
    @PostMapping("/validate")
    public RespInfo<Boolean> validateToken(@RequestParam String token);
}
```

---

## 五、数据模型变更

### 5.1 Redis Key结构变更

| Key类型 | 变更类型 | 说明 |
|--------|---------|------|
| `idempotent:{prefix}:{key}` | 修改 | Value结构增强，增加requestId、status |
| `idempotent:cache:{prefix}:{key}` | **新增** | 存储幂等结果缓存 |
| `idempotent:token:{prefix}:{token}` | **新增** | 存储Token元数据 |
| `idempotent:lock:{prefix}:{key}` | **新增** | Redisson分布式锁 |

### 5.2 配置模型变更

**新增配置属性**（`forge.idempotent.*`）：

```yaml
forge:
  idempotent:
    # 基础配置
    enabled: true
    prefix: "idempotent:"
    expire: 600
    message: "请勿重复提交"
    
    # 结果缓存配置
    cache:
      enabled: true
      expire: 3600
      max-size: 10000
    
    # Token配置
    token:
      enabled: true
      expire: 300
      header: "X-Idempotent-Token"
    
    # 分布式锁配置
    lock:
      enabled: true
      wait-time: 3000      # 等待时间（ms）
      lease-time: 5000     # 租约时间（ms）
    
    # 监控配置
    metrics:
      enabled: true
      sample-rate: 100     # 采样率（%）
```

---

## 六、接口变更

### 6.1 破坏性变更

#### 变更1：注解属性扩展

**原注解**：
```java
@Idempotent(prefix="order:", expire=600, key="#orderId", message="...")
```

**新注解**：
```java
@Idempotent(
    strategy = IdempotentStrategy.RETURN_CACHE,  // 新增
    prefix = "order:",
    expire = 600,
    cacheExpire = 3600,                           // 新增
    key = "#orderId",
    message = "...",
    cacheResult = true,                           // 新增
    enableMetrics = true                          // 新增
)
```

**兼容性影响**：
- 现有代码无需修改（新属性有默认值）
- 如需使用新功能，需添加策略配置

#### 变更2：异常携带缓存结果

**原异常**：
```java
throw new IdempotentException("请勿重复提交");
```

**新异常**：
```java
IdempotentException exception = new IdempotentException("请勿重复提交");
exception.setCachedResult(result);  // 可携带缓存结果
exception.setStrategy(strategy);    // 携带策略信息
```

**兼容性影响**：
- 异常处理逻辑需考虑缓存结果提取

### 6.2 新增接口

#### API1：Token生成接口

```http
POST /api/idempotent/token/generate
Request: { "prefix": "order" }
Response: { 
  "token": "abc123...", 
  "expireSeconds": 300,
  "createTime": 1234567890
}
```

#### API2：批量Token生成

```http
POST /api/idempotent/token/batch-generate
Request: { "count": 10, "prefix": "order" }
Response: [
  { "token": "abc1...", "expireSeconds": 300 },
  { "token": "abc2...", "expireSeconds": 300 },
  ...
]
```

#### API3：Token验证接口

```http
POST /api/idempotent/token/validate
Request: { "token": "abc123..." }
Response: { "valid": true }
```

---

## 七、风险评估

### 7.1 技术风险

| 风险项 | 风险等级 | 影响分析 | 缓解措施 |
|-------|---------|---------|---------|
| Redis故障 | 高 | 幂等功能失效，系统可用性下降 | 增加降级策略，异常时跳过幂等 |
| Redisson依赖冲突 | 中 | 可能与现有Redis配置冲突 | 提供Redisson配置兼容方案 |
| 序列化兼容性 | 中 | 结果缓存序列化可能失败 | 提供Jackson配置扩展点 |
| 性能影响 | 中 | 监控和缓存可能增加延迟 | 提供采样率和缓存开关 |
| 看门狗续期失败 | 低 | 长时间业务锁可能失效 | 增加锁超时告警 |

### 7.2 业务风险

| 风险项 | 风险等级 | 影响分析 | 缓解措施 |
|-------|---------|---------|---------|
| 缓存结果过期 | 中 | 过期后重复请求无法返回结果 | 合理配置cacheExpire |
| Token泄露 | 高 | Token被盗用可能绕过幂等 | Token绑定用户ID，短TTL |
| 并发冲突 | 中 | RETURN_CACHE模式可能并发冲突 | 增加短暂等待和二次检查 |
| 缓存一致性问题 | 中 | 业务结果更新后缓存未同步 | 提供手动清除缓存接口 |

### 7.3 兼容性风险

| 风险项 | 风险等级 | 影响分析 | 缓解措施 |
|-------|---------|---------|---------|
| 注解API变更 | 低 | 新增属性有默认值，现有代码无需修改 | 提供迁移文档 |
| Redis Key结构变更 | 中 | 新旧Key格式不同，需清理旧数据 | 提供清理脚本 |
| 异常类扩展 | 低 | 异常新增属性，不影响现有catch | 提供异常处理文档 |
| 配置路径变更 | 中 | 配置从idempotent.*改为forge.idempotent.* | 提供配置迁移指南 |

---

## 八、测试策略

### 8.1 单元测试

**测试范围**：
- Key生成器测试（SpEL解析、Hash计算）
- Token服务测试（生成、验证、消费）
- 结果缓存测试（读写、过期、序列化）
- 分布式锁测试（获取、续期、释放）
- 策略处理器测试（STRICT、RETURN_CACHE、TOKEN_REQUIRED）

**测试用例清单**：

| 测试类 | 核心测试方法 | 验证点 |
|-------|-------------|--------|
| `DefaultIdempotentKeyGeneratorTest` | `testSpelKeyGeneration` | SpEL解析正确性 |
| `RedisTokenServiceTest` | `testTokenGeneration` | Token生成唯一性 |
| `RedisTokenServiceTest` | `testTokenValidation` | Token验证逻辑 |
| `RedisTokenServiceTest` | `testTokenConsumption` | Token消费状态 |
| `RedisResultCacheServiceTest` | `testCacheResult` | 结果缓存读写 |
| `RedisResultCacheServiceTest` | `testCacheExpiry` | 缓存过期处理 |
| `RedissonLockManagerTest` | `testLockAcquire` | 锁获取成功 |
| `RedissonLockManagerTest` | `testLockWatchdog` | 看门狗续期 |
| `StrictStrategyHandlerTest` | `testStrictRejectDuplicate` | 严格拒绝重复 |
| `ReturnCacheStrategyHandlerTest` | `testReturnCachedResult` | 返回缓存结果 |
| `TokenRequiredStrategyHandlerTest` | `testTokenValidation` | Token验证流程 |

### 8.2 集成测试

**测试场景**：

1. **端到端幂等流程测试**
   - 标记`@Idempotent`的Controller方法
   - 模拟重复请求验证幂等效果
   - 验证结果缓存返回

2. **Token流程测试**
   - 获取Token → 提交请求 → Token消费
   - Token过期 → 验证失败
   - Token跨用户使用 → 验证失败

3. **并发场景测试**
   - 多线程并发请求同一Key
   - 验证锁竞争和结果缓存

4. **异常场景测试**
   - Redis故障 → 降级逻辑
   - 业务异常 → 锁释放
   - Token无效 → 异常抛出

**测试工具**：
- Testcontainers（Redis容器）
- MockMvc（Controller测试）
- CompletableFuture（并发测试）

### 8.3 性能测试

**测试指标**：

| 指标 | 目标值 | 测试方法 |
|-----|-------|---------|
| 单次幂等检查耗时 | < 5ms | JMH Benchmark |
| 缓存命中率 | > 70% | 模拟重复请求 |
| 锁获取耗时 | < 10ms | 并发锁竞争测试 |
| Token生成耗时 | < 2ms | 批量生成测试 |
| 内存占用 | < 50MB | 结果缓存压测 |

### 8.4 兼容性测试

**测试范围**：
- 现有注解功能保持正常
- 新旧配置路径兼容
- Redis Key格式兼容
- 异常处理兼容

---

## 九、实施计划

### 9.1 开发任务拆解

| 任务ID | 任务描述 | 预估工时 | 优先级 |
|-------|---------|---------|-------|
| T1 | 新增注解属性和枚举定义 | 0.5人日 | P0 |
| T2 | 实现Token服务和API | 1人日 | P0 |
| T3 | 实现结果缓存服务 | 1人日 | P0 |
| T4 | 集成Redisson分布式锁 | 0.5人日 | P0 |
| T5 | 实现幂等策略处理器 | 1人日 | P0 |
| T6 | 重构IdempotentAspect切面 | 1人日 | P0 |
| T7 | 集成Prometheus监控 | 0.5人日 | P1 |
| T8 | 编写单元测试 | 1人日 | P1 |
| T9 | 编写集成测试 | 1人日 | P1 |
| T10 | 性能测试和优化 | 0.5人日 | P2 |
| T11 | 文档编写（使用指南） | 0.5人日 | P2 |

**总工时**: 7人日  
**建议团队**: 1-2人  

### 9.2 实施里程碑

**Phase 1：核心功能实现（3人日）**
- T1-T6完成，核心幂等功能可用
- 支持三种策略、Token、缓存、Redisson锁

**Phase 2：测试与监控（2人日）**
- T7-T9完成，监控集成和测试覆盖
- 监控指标可用，测试通过率>90%

**Phase 3：优化与文档（2人日）**
- T10-T11完成，性能优化和文档完善
- 性能达标，文档完整

---

## 十、待澄清问题

### 10.1 技术决策待确认

| 问题ID | 问题描述 | 影响范围 | 建议方案 |
|-------|---------|---------|---------|
| Q1 | Redisson版本选择：3.24.3还是最新版？ | Maven依赖 | 使用3.24.3（稳定版） |
| Q2 | 结果序列化：Jackson还是FastJson？ | 性能和兼容性 | 使用Jackson（Spring Boot默认） |
| Q3 | 监控采样率：100%还是可配置？ | 性能影响 | 默认100%，可配置降低 |
| Q4 | Token Header名称是否固定？ | 前端集成 | 固定为`X-Idempotent-Token`，可配置 |

### 10.2 业务规则待确认

| 问题ID | 问题描述 | 影响范围 | 建议方案 |
|-------|---------|---------|---------|
| Q5 | 缓存结果是否包含异常信息？ | 用户体验 | 仅缓存成功结果，失败不缓存 |
| Q6 | Token是否需要绑定具体业务方法？ | 精准控制 | 仅绑定前缀，不绑定方法 |
| Q7 | 并发冲突时是否自动重试？ | 用户体验 | 不重试，直接抛异常 |
| Q8 | 是否提供手动清除缓存接口？ | 灵活性 | 提供管理接口清除缓存 |

---

## 十一、验收标准

### 11.1 功能验收标准

| 验收项 | 验收标准 | 验证方法 |
|-------|---------|---------|
| 幂等结果缓存 | 重复请求返回上次结果 | 单元测试 + 集成测试 |
| Token机制 | Token验证和消费正常 | 单元测试 + API测试 |
| 多策略支持 | STRICT/RETURN_CACHE/TOKEN_REQUIRED均正常 | 策略测试 |
| Redisson锁 | 看门狗续期正常，锁释放可靠 | 锁测试 + 并发测试 |
| 监控指标 | Prometheus指标正常采集 | 监控端点验证 |

### 11.2 性能验收标准

| 验收项 | 验收标准 | 验证方法 |
|-------|---------|---------|
| 幂等检查耗时 | < 5ms（P95） | JMH Benchmark |
| 锁获取耗时 | < 10ms（P95） | 并发测试 |
| 缓存命中率 | > 70%（重复场景） | 监控指标 |
| 内存占用 | < 50MB（10000缓存） | 堆内存监控 |

### 11.3 兼容性验收标准

| 集收项 | 验收标准 | 验证方法 |
|-------|---------|---------|
| 现有注解兼容 | 现有`@Idempotent`无需修改即可运行 | 兼容性测试 |
| 配置兼容 | 新旧配置路径均支持 | 配置测试 |
| 异常兼容 | 现有异常处理逻辑正常 | 异常测试 |

---

## 十二、知识沉淀

### 12.1 参考项目

| 项目名称 | 参考内容 | GitHub |
|---------|---------|--------|
| goku-framework | 幂等框架设计理念 | https://github.com/goku-framework |
| 美团Leaf | Token机制设计 | https://github.com/Meituan-Dianping/Leaf |
| yue-library | 模块化架构设计 | https://github.com/yl-yue/yue-library |
| Redisson | 分布式锁实现 | https://github.com/redisson/redisson |

### 12.2 关键技术点

1. **Redisson看门狗机制**：自动续期防止锁过期
2. **Token单次消费**：防范重放攻击
3. **结果缓存序列化**：Jackson配置优化
4. **SpEL表达式解析**：参数动态绑定
5. **Prometheus指标集成**：Micrometer配置

---

## 十三、附录

### 13.1 配置示例

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
      max-size: 10000
    
    token:
      enabled: true
      expire: 300
      header: "X-Idempotent-Token"
    
    lock:
      enabled: true
      wait-time: 3000
      lease-time: 5000
    
    metrics:
      enabled: true
      sample-rate: 100
```

### 13.2 使用示例

**示例1：订单创建（严格幂等）**

```java
@PostMapping("/order/create")
@Idempotent(
    strategy = IdempotentStrategy.STRICT,
    prefix = "order:",
    key = "#orderRequest.orderId",
    expire = 600,
    message = "订单正在处理中，请勿重复提交"
)
public RespInfo<Order> createOrder(@RequestBody OrderRequest orderRequest) {
    return RespInfo.success(orderService.create(orderRequest));
}
```

**示例2：订单查询（缓存幂等）**

```java
@GetMapping("/order/{orderId}")
@Idempotent(
    strategy = IdempotentStrategy.RETURN_CACHE,
    prefix = "order:query:",
    key = "#orderId",
    expire = 60,
    cacheExpire = 300,
    cacheResult = true
)
public RespInfo<Order> queryOrder(@PathVariable String orderId) {
    return RespInfo.success(orderService.getById(orderId));
}
```

**示例3：支付处理（Token幂等）**

```java
@PostMapping("/payment/process")
@Idempotent(
    strategy = IdempotentStrategy.TOKEN_REQUIRED,
    prefix = "payment:",
    key = "#paymentRequest.paymentId",
    expire = 1200
)
public RespInfo<PaymentResult> processPayment(@RequestBody PaymentRequest paymentRequest) {
    return RespInfo.success(paymentService.process(paymentRequest));
}

// 前端调用流程：
// 1. POST /api/idempotent/token/generate → 获取Token
// 2. POST /payment/process，Header携带 X-Idempotent-Token: xxx
```

---

**文档版本**: v1.0  
**编写日期**: 2026-04-06  
**编写人**: AI Assistant  
**审核状态**: 待用户审核