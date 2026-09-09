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
- XML/Flyway 静态检查：`SysCachePolicyMapper.xml`、`SysDictDataMapper.xml` 均通过 `xmllint --noout`；新迁移脚本 placeholder 扫描无匹配，`rg` 返回 1 属于预期；重复版本扫描仅报告存量 `V1.0.109`、`V1.0.110`。该脚本后续因版本占用从 `V1.0.120` 调整为当前 `V1.0.122`。
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

## 2026-08-17 管理页规整与 Redis 诊断安全下线

- 变更范围：缓存路由只保留受管缓存工作台；桌面表格合并为 6 个复合信息列并移除固定操作列，移动端使用紧凑策略列表；编辑弹窗改为显式异步保存操作区。
- 安全下线：删除 `SysCacheController` 和 `CacheInfoDTO`，关闭 Redis key 枚举、value 读取、任意 key 删除、pattern 批量清理和服务指标 HTTP 入口；保留 `/system/cache/policy/*` 命名缓存治理接口。
- 权限资源：新增 `V1.0.123__remove_redis_cache_diagnostics.sql`，先物理清理关系重建表 `sys_role_resource` 的旧授权，再将旧诊断按钮/API 的 `sys_resource.del_flag` 写为资源主键；缓存菜单和四个受管策略 API 不在匹配范围。
- 前端验证使用 Node `v20.19.0`：

```bash
cd forge-admin-ui
pnpm exec vitest run src/views/system/cache/__tests__/managed-cache-policy.spec.js
pnpm exec eslint src/views/system/cache.vue src/views/system/cache/*.vue src/views/system/cache/*.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

  结果：Vitest 1 个文件、5 个测试通过；目标 ESLint 通过；生产构建通过，耗时 58.66 秒。
- system 定向测试：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system \
  -Dtest=SysManagedCachePolicyServiceTest,SysCachePolicyMapperContractTest,SysCacheDiagnosticsRemovalContractTest,SysDictDataServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

  结果：4 个测试类、17 个测试，0 failures/errors/skipped。无 Redis 的策略清理用例仍输出既有失败开放警告，不计为测试失败。
- Flow 静态合同：`FlowControllerBoundaryContractTest` 共 18 个测试通过，已移除对下线 Controller 文件的读取。
- Admin 聚合编译：`mvn -pl forge-admin-server -am -DskipTests compile` 共 45 个模块 `BUILD SUCCESS`，耗时 45.565 秒。
- 静态检查：`SysCachePolicyMapper.xml` 解析、`V1.0.122/V1.0.123` placeholder、Flyway 版本唯一性、旧生产入口缺失、新策略入口存在和 `git diff --check` 均通过。首次安全扫描把 `src/test` 也纳入生产面，命中了合同测试中的预期旧 URL 字符串；将范围纠正为 `src/main` 后通过。
- Playwright 桌面验证：`1440x900` 下表格 6 列边界连续，`overlap=false`，页面 `clientWidth/scrollWidth=1440/1440`，Redis 诊断文案计数 0，控制台错误 0。
- Playwright 移动验证：`390x844` 下策略项宽 336px，页面 `clientWidth/scrollWidth=390/390`；编辑弹窗宽 366px、高 778px，完整位于视口内；Redis 诊断文案计数 0，控制台错误 0。
- 预期构建警告：Vite 原有 native config loader 提示、`UserSelectModal` 组件命名冲突、存量 CSS `//` 注释、动态/静态 import 重叠和 UnoCSS 插件耗时提示；均非本次文件引入，构建结果成功。
- 跳过项：未执行真实 MySQL Flyway、服务重启后旧诊断接口 404、生产角色资源结果检查和双实例缓存同步。
- 环境清理：Playwright 浏览器已关闭；未启动数据库、Redis 或后端服务。端口 3000 的用户已有 Vite 服务保持运行，未停止也未重启。

## 2026-08-17 字典消费链路收口与失败统计

- 变更范围：通用 `useDict` 从 `/system/dict/data/list?dictType=` 切换到受管 `/system/dict/data/type/{dictType}`；删除 `SytemDictValueProvider` 中的 30 分钟正向/反向 Map 缓存；字典变更监听只失效 `system:dict-data`；管理页桌面/移动端补充写入和失败统计。字典管理页的 `/list` 管理查询保持不变。
- 红测证据：前端新增端点合同首次实际收到 `/list + params.dictType`，按预期失败；后端 `SytemDictValueProviderTest` 要求连续两次翻译委托两次 Service，当前实现仅委托 1 次，按预期失败。实现后两项转绿。
- 前端定向验证：

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run src/composables/__tests__/useDict.spec.js src/views/system/cache/__tests__/managed-cache-policy.spec.js
pnpm exec eslint src/composables/useDict.js src/composables/__tests__/useDict.spec.js src/views/system/cache/ManagedCachePolicies.vue src/views/system/cache/__tests__/managed-cache-policy.spec.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

  结果：Vitest 2 个文件、7 个测试通过；ESLint 通过；生产构建通过，主题色修正后最终复跑耗时 48.19 秒。构建仅保留 Vite native config、存量 CSS `//` 注释、组件名冲突、动态/静态 import 和 UnoCSS 耗时等既有警告。
- system 定向验证：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system \
  -Dtest=SysManagedCachePolicyServiceTest,SysCachePolicyMapperContractTest,SysCacheDiagnosticsRemovalContractTest,SysDictDataServiceImplTest,SytemDictValueProviderTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

  结果：5 个测试类、18 个测试全部通过。`SysManagedCachePolicyServiceTest` 的无 Redis 管理器清理用例继续输出预期失败开放警告，不计为测试失败。
- Admin 聚合编译：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -pl forge-admin-server -am -DskipTests compile
```

  结果：45 个模块 `BUILD SUCCESS`，耗时 59.882 秒。
- 运行态请求验证：复用用户已运行的 Vite `3000` 和 Admin `8580`，登录后打开组织管理，捕获到 4 个 `200` 请求：`/type/sys_org_type`、`/type/sys_normal_disable`、`/type/sys_post_type`、`/type/sys_user_status`，未再请求业务字典 `/list`。首次回到缓存页观察为 hit/miss/put/failure=`64/4/4/13`，后续页面请求中 hit 增长到 72，miss/put 保持 4，failure 保持 13，证明当前请求已稳定命中，13 为本轮之前的当前 JVM 历史累计。
- 页面验证：`1440x900` 桌面表格完整展示四项运行统计；`390x844` 移动列表展示 6 个策略/统计项，页面正文 `clientWidth/scrollWidth=390/390`；桌面和移动失败数的计算颜色均为主题 `rgb(208, 48, 80)`，控制台错误数为 0。首选内置浏览器因环境缺少沙箱元数据无法连接，按技能降级流程使用本地 Playwright 完成同等只读检查。
- 跳过项：本轮未执行真实 MySQL Flyway、双 Admin 实例失效同步和普通管理员 403。
- 环境清理：本轮 Playwright 浏览器已关闭；未启动或停止数据库、Redis、Vite 或 Admin，用户已有 PID `18363`/`29964` 保持运行。

## 2026-08-18 运行态泛型缓存兼容修复

- 用户运行组织树时出现 `LinkedHashMap cannot be cast to SysDictData`；根因为受管 Redis codec 恢复了外层 `List`，但运行时 JSON 不包含 `List<SysDictData>` 的元素泛型，命中值进入 `SytemDictValueProvider` 后在增强 `for` 隐式强转处失败。
- 通用修复：`ForgeCacheAspect` 使用应用 `ObjectMapper` 和被缓存方法的 `getGenericReturnType()` 恢复集合、Map、数组元素；已有本地正确类型不会重复转换。转换失败时删除该 entry 并穿透业务方法。
- 边界兼容：`SytemDictValueProvider` 同时接受 `SysDictData` 和历史 Map 字典项，只提取 `dictValue/dictLabel`，未知类型跳过并记录类型名。
- 红测证据：新增 AOP 用例首次运行报 `Map1 cannot be cast to SamplePayload`；实现后 `ForgeCacheAspectTest` 6 tests、0 failures、0 errors，并覆盖类型恢复失败后的清理、穿透和正确值回填。
- starter 增量测试：排除需要 Mockito agent 的既有 Manager 测试后，6 个测试类、17 tests 全部通过；覆盖 AOP、key、事务、MULTI、codec 和定义解析。
- system 增量测试：`SysDictDataServiceImplTest` 7 tests 与 `SytemDictValueProviderLegacyCacheTest` 1 test 全部通过。
- 完整 starter 定向套件尝试执行 27 tests，其中 `ForgeManagedCacheManagerTest` 4 个既有 Mockito 用例因当前 JDK 无法 self-attach Byte Buddy agent 报环境错误；其余无失败。该阻断与本轮生产代码无关，未记为通过。
- Admin 聚合编译：`mvn -pl forge-admin-server -am -DskipTests compile`，45/45 模块 `BUILD SUCCESS`，耗时 53.869 秒。
- 未启动或重启真实 Admin/Redis，未执行组织树接口运行态复验；本轮无服务需要清理。

## 2026-08-23 Redisson 社区版 MULTI 兼容修复

- 运行态根因：当前 Redisson `3.50.0` 社区版 JAR 的 `getLocalCachedMapCache` 实现直接抛出 `UnsupportedOperationException`，原 MULTI 句柄创建必然失败；业务数据源 Hikari 连接池此前已经正常启动，与本异常无关。
- 实现范围：MULTI 改为 Caffeine L1 + `RMapCache` L2 + 每个命名缓存独立的类型化失效 Topic；写入、单 key 删除和全量清理均通知其他实例清理 L1，Topic 初次订阅或重连订阅时清空当前 L1。保留普通值/空值独立本地 TTL 和 Redis entry TTL。
- codec：新增 `ManagedCacheInvalidationMessage` 显式类型化 codec，继续复用应用 `ObjectMapper`，不依赖全局 codec 推断。
- starter 定向验证执行两轮，最终命令：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-cache \
  -Dtest=MultiLevelCacheHandleTest,ManagedCacheCodecTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

  结果：2 个测试类、6 个测试，0 failures/errors/skipped，`BUILD SUCCESS`。覆盖普通值/空值 L1 TTL、L2 TTL 参数、双句柄单 key/全量失效、重订阅清空和失效消息 codec 往返。
- 静态检查：生产代码扫描不再包含 `getLocalCachedMapCache`、`RLocalCachedMapCache` 或 `LocalCachedMapCacheOptions`；相关文件 `git diff --check` 无输出。
- 编译告警：`RedissonCacheServiceImpl` 使用旧 API 的既有 deprecation 提示，不是本轮引入，不影响测试成功。
- 跳过项：遵循用户偏好未执行完整 Maven/Vite 构建，未启动或重启 Admin，也未执行双真实 Admin 实例的 Redis Topic E2E。
- 环境清理：本轮未启动服务，无需清理进程。
