# 受管注解驱动缓存执行日志

## 2026-08-17 SDD 初始化

- 分支：从 `main` 创建 `feat/managed-annotation-cache`，避免在主分支直接编码。
- 变更范围：仅创建当前变更的 Spec、任务、测试规格和实施计划；未修改生产代码。
- 基线检查：工作区存在用户已有的低代码权限变更、memory、`.DS_Store` 和两个 bridge 新文件，本变更不触碰、不暂存、不回滚这些文件。
- 现状证据：starter 已包含 Redisson/Caffeine，但仅提供手工 `ICacheService`；字典、数据集、外部响应和 AI 客户端存在分散缓存实现；现有缓存页是 Redis 原始键诊断页。
- 文档检查：`git diff --check -- code-copilot/changes/managed-annotation-cache` 无输出；占位符扫描无结果。
- 已启动服务：无。
- 数据库/Redis 运行态变更：无。

## 2026-08-17 实现与增量验证

- 变更范围：完成 Forge 自有注解缓存运行时、LOCAL/REDIS/MULTI 句柄、事务提交后动作、系统策略控制面、Flyway/权限资源、缓存管理页和字典缓存迁移；未引入 Spring Cache。
- starter 定向测试：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-cache \
  -Dtest=ForgeManagedCacheManagerTest,ForgeCacheKeyResolverTest,ForgeCacheAspectTest,CacheTransactionExecutorTest,MultiLevelCacheHandleTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

  结果：5 个测试类、15 个测试，0 failures/errors/skipped。覆盖策略校验与降级、可信作用域键、AOP 命中/穿透/Put/Evict、事务提交与回滚、MULTI 普通值和空值的独立 L1 TTL。
- system 定向测试：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system \
  -Dtest=SysManagedCachePolicyServiceTest,SysCachePolicyMapperContractTest,SysDictDataServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

  结果：3 个测试类、15 个测试，0 failures/errors/skipped。覆盖策略合并/校验/乐观锁/重置/清空、Mapper 租户与逻辑删除合同、字典注解迁移和事务后失效。
- Admin 聚合编译：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -pl forge-admin-server -am -DskipTests compile
```

  结果：45 个模块 `BUILD SUCCESS`，耗时 34.689 秒。
- 前端验证使用 Node `v20.19.0`：

```bash
cd forge-admin-ui
pnpm exec vitest run src/views/system/cache/__tests__/managed-cache-policy.spec.js
pnpm exec eslint src/views/system/cache.vue src/views/system/cache/*.vue src/views/system/cache/*.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

  结果：Vitest 1 个文件、5 个测试通过；目标 ESLint 通过；生产构建通过，耗时约 1 分 55 秒。
- XML/Flyway 静态检查：`SysCachePolicyMapper.xml`、`SysDictDataMapper.xml` 均通过 `xmllint --noout`；新迁移脚本 placeholder 扫描无匹配，`rg` 返回 1 属于预期；重复版本扫描仅报告存量 `V1.0.109`、`V1.0.110`，新增 `V1.0.120` 唯一。
- 预期警告：非法 Redis 运行策略会降级到代码默认；非法 SpEL 会绕过缓存并只执行业务方法一次；无 Redis 的策略清理测试记录连接不可用警告。三类警告均为失败开放合同的测试证据，不阻断结果。
- 浏览器验证：Vite 已在 `http://127.0.0.1:5173/` 启动（PID `95006`）并保留供人工查看。因 Admin `8580` 未启动，访问 `/system/cache` 被登录流程拦截并显示服务不可用，未将该结果计为受管缓存页面交互通过。
- 跳过项：按用户既有分工，本轮未启动真实 MySQL/Redis/Admin，未执行 Flyway 实跑、策略接口登录调用、双 Admin 实例失效同步、普通管理员 403 和数据库结果检查。这些运行态 E2E 由用户执行。
- 环境清理：未启动或修改数据库、Redis、Admin；前端 Vite 服务按交付需要继续运行，其余无本轮遗留服务。

## 2026-08-17 Review 增量修复与验证

- 修复范围：为数据 Map、定义 Map、策略 Map 和控制 Topic 增加显式 `TypedJsonJacksonCodec`；缓存业务值使用类型信息保留具体类型，并复用自动配置中的 `ObjectMapper` 模块。
- 定义安全：类级存在 `@ForgeCacheConfig` 时，未匹配名称直接拒绝并由 AOP 穿透；首次本地注册在 identity 临界区内执行 Redis `putIfAbsent`，兼容性只比较运行与隔离字段，远端冲突不覆盖。
- 策略并发：覆盖状态改为 `AtomicReference<Map<...>>`，刷新使用不可变快照 `getAndSet`，控制事件使用 copy-on-write 更新。
- 红测证据：新增测试首次执行在测试编译阶段失败，原因为 `ManagedCacheCodecs` 尚不存在；实现后转绿。
- starter 增量测试：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-cache \
  -Dtest=ForgeManagedCacheManagerTest,ForgeCacheKeyResolverTest,ForgeCacheAspectTest,CacheTransactionExecutorTest,MultiLevelCacheHandleTest,ManagedCacheCodecTest,CacheDefinitionResolverTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

  结果：7 个测试类、25 个测试，0 failures/errors/skipped。新增覆盖真实 codec ByteBuf 往返、具体业务类型与 `LocalDateTime`、拼写错误旁路、定义一次注册/共享/冲突以及不可变策略快照替换。
- system 回归测试：复用本日志上一节命令，3 个测试类、15 个测试，0 failures/errors/skipped。
- Admin 聚合编译：复用本日志上一节命令，45 个模块 `BUILD SUCCESS`，耗时 25.900 秒。
- 静态检查：`git diff --check -- code-copilot/changes/managed-annotation-cache forge-server/forge-framework/forge-starter-parent/forge-starter-cache` 无输出。
- 预期警告：非法策略、非法 SpEL、拼写错误缓存名和无 Redis 清理场景会输出异常栈，用于验证失败开放或安全旁路合同，不计为测试失败。
- 前端：本轮无前端代码差异，复用此前 Vitest 5/5、目标 ESLint 和生产构建成功基线。
- 跳过项：本轮未启动真实 MySQL/Redis/Admin，未执行真实 Redis codec 读写、双实例事件同步和管理接口 E2E；codec 已通过 Redisson 真实 encoder/decoder 往返覆盖。
- 环境清理：本轮未启动任何新服务，无需额外清理。
