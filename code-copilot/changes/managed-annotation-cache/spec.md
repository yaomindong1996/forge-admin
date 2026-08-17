# 受管注解驱动缓存
> status: apply
> created: 2026-08-17
> complexity: 复杂
> change: `managed-annotation-cache`

## 1. 背景与目标

Forge 已有 `forge-starter-cache`，底层同时依赖 Redisson 与 Caffeine，但当前只提供手工 Redis 原语。字典、数据集、外部接口、AI 客户端和权限配置分别维护自己的 TTL、键格式、本地缓存与失效逻辑，管理端只能浏览 Redis 原始键，无法按受管缓存统一启停、切换层级或调整 TTL。

本变更在不启用 Spring Cache 的前提下，建设 Forge 自有的轻量注解缓存运行时。业务方法通过 `@ForgeCacheable`、`@ForgeCachePut`、`@ForgeCacheEvict` 接入，缓存定义通过 `@ForgeCacheConfig` 声明；运行时复用 Spring AOP/SpEL、Caffeine 和 Redisson，管理端保存并分发策略覆盖。

完成后必须满足：

1. 同一套注解支持 `LOCAL`、`REDIS`、`MULTI` 三种模式，MULTI 使用 Caffeine 本地层和 Redis 远端层。
2. 每个命名缓存独立设置本地 TTL、Redis TTL、本地容量、空值缓存和空值 TTL。
3. 管理端可以查看代码注册的缓存定义，修改允许范围内的运行策略、停用、恢复默认和清空缓存。
4. 管理策略优先级固定为“管理端覆盖 > 注解默认 > starter 全局默认”，运行方法不查询数据库。
5. 缓存键默认包含可信租户/用户/当前组织作用域并对业务键做摘要，管理端不暴露缓存值和原始业务键。
6. 写入与失效在事务提交后生效；缓存基础设施异常默认穿透业务方法，不影响数据库主链路。
7. 字典数据缓存成为首个真实迁移用例，移除其手写本地 + Redis 两级实现。

## 2. 范围

### 2.1 P0 范围

- 新增 `@ForgeCacheConfig`、`@ForgeCacheable`、`@ForgeCachePut`、`@ForgeCacheEvict`。
- 新增缓存模式、作用域、定义、策略覆盖、查找结果和统计快照模型。
- 新增 AOP、SpEL 键解析、SHA-256 键摘要、事务提交后动作协调。
- LOCAL 使用 Caffeine；REDIS 使用 Redisson `RMapCache`；MULTI 使用 `RLocalCachedMapCache`，本地提供方固定为 Caffeine，跨实例同步固定为失效通知，重连固定清空本地层。
- 运行时将代码定义与策略覆盖注册到 Redis 控制面，本地持有有效策略快照；策略变更通过 Redisson Topic 分发。
- 新增 `sys_cache_policy` 逻辑删除表、Mapper XML、Service、Controller、Flyway 和资源权限。
- 管理端缓存页增加“受管缓存”与“Redis 诊断”两个视图，受管视图不展示 value。
- 迁移 `SysDictDataServiceImpl#selectDictDataByType` 及其清理方法。

### 2.2 非目标

- 不引入或启用 Spring Cache，不实现 `org.springframework.cache.CacheManager`。
- 不迁移锁、幂等 Token、OAuth code、重放防护、验证码、会话密钥、限流器等状态存储。
- 不缓存响应流、文件、游标、异步 Publisher、`CompletableFuture` 或不可序列化对象到 Redis。
- 首版不提供按原始业务键搜索和查看缓存值，不提供管理端编辑 SpEL、作用域、缓存名或序列化类型。
- 首版不承诺 MULTI 强一致；严格实时数据应配置为 REDIS 或停用缓存。
- 不自动迁移所有已有手写缓存，后续按风险逐个迁移。

## 3. 架构与边界

```text
业务方法注解
    -> ForgeCacheAspect
        -> ForgeCacheKeyResolver（SpEL + 可信身份作用域 + SHA-256）
        -> ForgeManagedCacheManager（代码定义 + 管理覆盖）
            -> LOCAL: Caffeine
            -> REDIS: RMapCache
            -> MULTI: RLocalCachedMapCache(CAFFEINE, INVALIDATE, CLEAR)

管理端
    -> sys_cache_policy（权威覆盖）
    -> ForgeManagedCacheManager.applyOverride
    -> Redis policy map + control topic
    -> 各实例替换本地策略快照并重建缓存句柄
```

### 3.1 模块职责

- `forge-starter-cache`：注解、AOP、运行时、Caffeine/Redisson 后端、Redis 控制面和本地统计；不得依赖 MyBatis 或 `forge-plugin-system`。
- `forge-plugin-system`：策略表、Mapper XML、管理 Service/API、启动同步；通过 starter 公共模型调用运行时。
- `forge-admin-ui`：策略列表、编辑、恢复默认和清空操作；Redis 原始键诊断作为独立页签保留。

### 3.2 注解合同

- `@ForgeCacheConfig` 作用于类，可重复；声明缓存名、描述、默认模式、允许模式、作用域、L1/L2 TTL、容量和空值策略。
- `@ForgeCacheable` 在调用前查缓存，命中直接返回；未命中执行业务方法，成功后按事务边界写入。
- `@ForgeCachePut` 始终执行业务方法，成功后按事务边界写入返回值。
- `@ForgeCacheEvict` 成功后按事务边界删除指定 key 或整个命名缓存。
- 同一应用和缓存名只能有一个兼容定义；冲突定义失败关闭并记录明确错误。
- 类上已经声明一个或多个 `@ForgeCacheConfig` 时，方法注解引用的缓存名必须精确匹配；拼写错误不得降级为隐式全局定义。
- Spring 代理自调用不触发注解，文档和测试必须明确该边界。

### 3.3 键与租户安全

- Redis 对象名格式：`forge:managed-cache:data:{application}:{cacheName}`。
- entry key 只保存 `SHA-256(scopeMaterial + evaluatedKey)`，不保存手机号、Token、查询参数等原始材料。
- `GLOBAL` 不附加身份；`TENANT` 要求 tenantId；`TENANT_USER` 要求 tenantId/userId；`TENANT_USER_ORG` 要求 tenantId/userId，并包含 activeOrgId 或稳定空标记。
- 缺失注解要求的可信上下文时，本次调用绕过缓存，禁止写入共享 `null` 作用域。
- 管理端只能覆盖运行参数，不能改变 scope、key 表达式、允许模式或代码来源。

### 3.4 一致性与故障策略

- MULTI 使用 Redisson 本地缓存失效同步，Redis 重连后清空本地层；它属于有界最终一致。
- `@ForgeCachePut` 与 `@ForgeCacheEvict` 在活动事务提交后执行；回滚时不改变缓存。
- `@ForgeCacheable` 在活动事务内加载的值同样延迟到提交后写入，避免缓存未提交数据。
- 读取、写入、删除、注册或策略通知失败时记录受控日志和失败计数，业务调用继续执行。
- 管理端停用、恢复默认或改变关键策略时清理当前缓存，避免重新启用后读取旧值。
- Redis TTL 必须大于 0；MULTI 模式本地 TTL 必须小于等于 Redis TTL。
- 数据、定义、策略和控制事件必须使用显式类型化 codec，并复用应用 `ObjectMapper` 模块；不得依赖全局 codec 推断 final record 类型。
- 缓存定义只在本地首次注册时通过 Redis `putIfAbsent` 发布；运行与隔离字段不兼容时拒绝使用且不得覆盖远端定义，描述和声明来源不影响兼容性。
- 本地策略覆盖以不可变 Map 快照原子发布，刷新期间不得暴露临时空状态。

## 4. 数据模型

`sys_cache_policy` 保存平台级运行覆盖，业务内置数据 `tenant_id=1`：

- `id`：主键。
- `application_code + cache_name`：受管缓存唯一身份。
- `enabled`：是否启用。
- `cache_mode`：LOCAL/REDIS/MULTI。
- `local_ttl_seconds`、`redis_ttl_seconds`、`local_max_size`。
- `cache_null`、`null_ttl_seconds`。
- `policy_version`：乐观锁版本。
- `del_flag BIGINT`：有效值 0，删除后写当前主键；唯一索引为 `(tenant_id, application_code, cache_name, del_flag)`。
- 标准审计字段：`tenant_id/create_by/create_time/create_dept/update_by/update_time`。

代码注册定义保存在 Redis 控制面，不直接写数据库。管理页合并“注册定义 + 数据库覆盖”；没有覆盖时显示注解默认值。

## 5. API

- `GET /system/cache/policy/page?pageNum=1&pageSize=10&applicationCode=&cacheName=`：分页查询受管定义和有效策略。
- `POST /system/cache/policy/edit`：新增或按 `policyVersion` 更新覆盖。
- `POST /system/cache/policy/reset?applicationCode=&cacheName=`：逻辑删除覆盖并恢复注解默认。
- `POST /system/cache/policy/clear?applicationCode=&cacheName=`：清空一个受管缓存。
- 原 `/system/cache/page|getInfo|remove|clear|metrics` 保留为 Redis 诊断接口。

所有新接口仅允许平台超级管理员，写操作记录操作日志。接口不返回缓存 entry key/value。

## 6. 管理端交互

- 页面默认进入“受管缓存”，顶部以线型 Tabs 切换“受管缓存 / Redis 诊断”。
- 受管列表支持应用编码、缓存名搜索，显示来源类、作用域、允许模式、有效模式、L1/L2 TTL、容量、状态和是否存在覆盖。
- 编辑使用中等宽度弹窗，只编辑运行字段；模式使用单选按钮，启用/空值缓存使用开关，TTL/容量使用数字输入。
- 行操作固定为“编辑、清空、恢复默认”；没有覆盖时禁用恢复默认。
- 切换模式超出代码允许范围、MULTI 的 L1 TTL 大于 L2 TTL、TTL 非正数时前后端均阻止提交。

## 7. 验收标准

1. 本地模式同 key 第二次调用不执行目标方法，TTL 后重新加载。
2. Redis 模式使用独立 Redis TTL；Redis 异常时目标方法仍正常返回。
3. MULTI 模式使用 Caffeine 本地提供方，单 key/全量失效能同步清理当前实例缓存。
4. SpEL key、默认参数 key、租户/用户/组织作用域和缺失上下文绕过均有单测。
5. 活动事务回滚不写缓存、不失效缓存；提交后执行写入/失效。
6. 管理覆盖只能选择代码允许的模式；版本冲突返回 409；重置后恢复注解默认。
7. `sys_cache_policy` 有完整审计字段、显式 `@TableLogic` 和 active-only 唯一索引。
8. 字典查询移除手写两级缓存，修改后提交成功才失效。
9. starter 定向测试、system 定向测试、Admin 聚合编译、Flyway 静态检查、前端构建均通过或记录真实环境阻断。
10. 四类 Redis envelope 通过真实 codec 往返；缓存名拼写错误、定义重复/冲突和策略快照替换均有回归测试。

## 8. 风险与回滚

- 风险：Redis 本地失效通知不是事务消息。缓解：MULTI 明确为最终一致，重连清空本地层，严格缓存使用 REDIS。
- 风险：策略变更导致短时冷缓存。缓解：变更后主动清空，业务失败开放。
- 风险：错误 scope 造成数据串读。缓解：scope 只能由代码声明，缺失上下文绕过，键作用域单测覆盖。
- 风险：返回值不能远程序列化。缓解：允许模式由代码限制，远程写失败穿透并记录；不可序列化对象声明 LOCAL-only。
- 回滚：停用 `forge.cache.annotation-enabled` 后所有注解穿透；恢复字典原实现可独立回滚。数据库脚本只新增表和资源，不删除旧缓存接口或旧数据。

## 9. 当前状态

- [x] 代码现状与架构分析完成。
- [x] Spec、任务和测试基线建立。
- [x] starter 运行时实现。
- [x] 控制面、Flyway 与 API 实现。
- [x] 管理端实现。
- [x] 字典迁移。
- [x] 增量验证与审查。
- [x] Review 发现的 codec、定义解析/注册和策略快照问题已修复并完成增量验证。
- [ ] 真实 MySQL/Redis/Admin、双实例失效同步和普通管理员 403 E2E 由用户执行。

代码实现和 Review 修复阶段已完成，下一步应重新执行 `/review managed-annotation-cache`。自动化验证覆盖 starter、系统控制面、字典迁移、Mapper/Flyway 静态合同、Admin 聚合编译和前端测试/构建；真实环境 E2E 不在本轮自动执行范围内，不能据此宣称运行态验收通过。
