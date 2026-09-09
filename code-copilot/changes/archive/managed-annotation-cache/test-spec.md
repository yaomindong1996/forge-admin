# 受管注解驱动缓存测试规格

## 1. 验证范围

本变更触及共享 starter、Redis 多级缓存、系统配置表、管理 API、Flyway、前端管理页和字典查询，风险等级为高。采用单元测试、静态合同、聚合编译和前端构建组合验证；默认不启动真实 MySQL/Redis/Admin，不把未执行的 E2E 表述为通过。

## 2. P0 测试

### 2.1 starter 运行时

- LOCAL：首次加载、命中、空值策略、单 key 失效、全量失效、策略重建。
- key：SpEL 参数、默认参数摘要、TENANT/TENANT_USER/TENANT_USER_ORG、缺失上下文绕过。
- AOP：Cacheable 命中不执行目标、Put 始终执行并覆盖、Evict 成功后删除。
- 事务：提交后执行动作，回滚不执行动作。
- 校验：TTL 非正数、MULTI 本地 TTL 大于 Redis TTL、模式不在 allowedModes 时拒绝。
- 故障：Redis 注册、读取、写入或通知异常时业务方法继续执行。

命令：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-cache \
  -Dtest=ForgeManagedCacheManagerTest,ForgeCacheKeyResolverTest,ForgeCacheAspectTest,CacheTransactionExecutorTest,MultiLevelCacheHandleTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

### 2.2 系统控制面

- 合并代码定义和数据库覆盖，未覆盖时使用注解默认。
- 新增、版本匹配更新、版本冲突、恢复默认和清空。
- 禁止超出 allowedModes，校验 MULTI TTL 关系。
- Mapper XML 显式过滤 `tenant_id` 和 `del_flag=0`。

命令：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system \
  -Dtest=SysManagedCachePolicyServiceTest,SysCachePolicyMapperContractTest,SysCacheDiagnosticsRemovalContractTest,SysDictDataServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

### 2.3 字典迁移

- `selectDictDataByType` 只执行数据库加载逻辑，缓存由注解代理处理。
- 指定字典与全部字典清理均声明正确的 Evict 合同。
- 事务内字典修改仅在提交后触发失效。

## 3. P1 静态与构建验证

```bash
cd forge-server
xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysCachePolicyMapper.xml
rg -n '\$\{[^}]+\}' db/migration
find db/migration -maxdepth 1 -name 'V*.sql' -print | sed 's/.*\///' | cut -d_ -f1 | sort | uniq -d
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -pl forge-admin-server -am -DskipTests compile
```

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
cd forge-admin-ui
pnpm exec eslint src/views/system/cache.vue src/views/system/cache/*.vue src/views/system/cache/*.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

### 3.1 Redis 诊断下线安全合同

```bash
test ! -e forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysCacheController.java
test ! -e forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/CacheInfoDTO.java
! rg -n "/system/cache/(metrics|page|getInfo|removeBatch|remove|clear)(['\"]|$)" forge-admin-ui/src forge-server/forge-framework/*/*/src/main
rg -n "/system/cache/policy/(page|edit|reset|clear)" forge-admin-ui/src forge-server/forge-framework/*/*/src/main
```

Flyway 下线脚本必须先物理清理 `sys_role_resource` 关系，再将旧按钮/API `sys_resource.del_flag` 写为资源主键；不得删除缓存菜单和四个受管策略 API 资源。

### 3.2 页面视觉回归

- Playwright 使用已运行的本地前端，登录后访问 `/system/cache`。
- 桌面视口 `1440x900`：筛选区单行可用，缓存身份和策略信息可读，操作列不覆盖 TTL/状态/统计列。
- 移动视口 `390x844`：筛选区纵向排列，策略切换为紧凑列表，弹窗不超出视口且页面正文不产生横向溢出。
- 两个视口均不得出现“Redis 诊断”，控制台不得出现新增 error。

## 4. E2E 与跳过边界

有可用 MySQL、Redis 和 Admin 服务时追加：

1. Flyway 执行后检查 `forge_schema_history` 和 `sys_cache_policy`。
2. 登录超级管理员，查询策略列表，修改字典缓存为 LOCAL/REDIS/MULTI 并确认实例应用。
3. 两个 Admin 实例验证单 key 和全量失效通知。
4. 普通管理员调用策略写接口应返回 403。

若本轮未启动真实服务，必须在执行日志明确这些项未覆盖。

## 5. 完成标准

- P0 定向测试全部通过。
- Admin 聚合编译成功。
- Flyway 静态检查、重复版本检查、XML 解析和差异空白检查无新增错误。
- 前端目标 ESLint 与生产构建成功。
- Redis 诊断生产入口安全扫描无匹配，受管策略接口仍存在。
- Playwright 桌面与移动视口无布局重叠和新增控制台错误。
- 所有跳过项、环境告警和服务清理情况已写入 `execution-log.md`。

## 6. 最终验证结果

- starter：5 个测试类、15 个测试，0 failures/errors/skipped。
- system：3 个测试类、15 个测试，0 failures/errors/skipped。
- 前端策略纯函数：1 个测试文件、5 个测试通过。
- Admin 聚合编译：45 个模块 `BUILD SUCCESS`。
- 前端目标 ESLint 和生产构建通过。
- `SysCachePolicyMapper.xml`、`SysDictDataMapper.xml` 解析通过；受管缓存 Flyway 无 placeholder，`V1.0.122` 与 `V1.0.123` 版本唯一。
- 未执行真实 MySQL/Redis/Admin、双实例通知和普通管理员 403 E2E。

## 7. Review 修复增量测试

- codec：使用 `TypedJsonJacksonCodec` 的真实 encoder/decoder 往返验证 `ManagedCacheValue`、`CacheDefinition`、`CachePolicyOverride` 和 `CacheControlMessage`，覆盖 `LocalDateTime` 业务值。
- 定义解析：目标类声明缓存配置但注解缓存名拼写错误时必须旁路，连续调用不得产生缓存命中；未声明配置的类仍允许使用全局默认定义。
- 定义注册：同一定义重复使用只执行一次 Redis `putIfAbsent`；远端定义的 `source` 不同但运行字段相同视为兼容，任一运行或安全字段不同则拒绝注册且不得覆盖远端值。
- 策略刷新：使用不可变 Map 快照一次替换；刷新和控制事件不得暴露临时空快照，策略变化时关闭旧句柄。
- 回归：复用第 2 节 starter/system 定向测试和第 3 节 Admin 聚合编译；前端未改动时复用已通过的前端基线并在执行日志中说明。

增量结果：starter 7 个测试类、25 个测试全部通过；system 3 个测试类、15 个测试全部通过；Admin 聚合编译 45 个模块通过；差异空白检查通过。前端无代码差异，复用第 6 节已通过的前端基线。

## 8. 管理页规整与诊断下线增量结果

- 前端策略纯函数 1 个文件、5 个测试通过；目标 ESLint 和生产构建通过。
- system 4 个测试类、17 个测试通过；新增诊断下线合同覆盖 Controller/DTO 不存在和资源迁移边界。
- Flow Controller 边界合同 18 个测试通过；旧缓存 Controller 不再作为分页兼容样例。
- Admin 聚合编译 45 个模块通过；Mapper XML、Flyway placeholder、版本唯一性、安全入口扫描和差异空白检查通过。
- Playwright 在 `1440x900` 检测 6 个桌面列无重叠，页面宽度 `1440/1440`；在 `390x844` 检测移动策略列表和 `366px` 弹窗均在视口内，页面宽度 `390/390`。两个视口的 Redis 诊断文案计数和控制台错误数均为 0。
- 未执行真实 MySQL Flyway、服务重启后旧诊断接口 404 和生产角色资源结果检查。

## 9. 字典消费链路与失败统计增量结果

- 新增 `useDict.spec.js`，验证业务字典类型编码后请求 `/system/dict/data/type/{dictType}`，并保留排序与字段归一化合同。
- 新增 `SytemDictValueProviderTest`，连续翻译必须每次委托受管 `ISysDictDataService`，禁止被翻译器内部 Map 短路。
- 前端定向 Vitest 2 个文件、7 个测试通过；目标 ESLint 通过；system 5 个测试类、18 个测试通过；Admin 45 模块聚合编译通过；前端生产构建通过。
- 本地运行态打开组织管理后，实际请求 `sys_org_type`、`sys_normal_disable`、`sys_post_type`、`sys_user_status` 的 `/type/{dictType}` 接口；受管统计出现 miss/put/hit，后续页面请求中 hit 继续增长而 failure 保持 13 不变。
- `1440x900` 桌面和 `390x844` 移动页面均展示命中、未命中、写入和失败；移动页面 `clientWidth/scrollWidth=390/390`，非零失败色为当前主题 `rgb(208, 48, 80)`，前端控制台无新增错误。

## 10. 运行态泛型缓存兼容回归

- `ForgeCacheAspectTest#shouldRestoreGenericCollectionElementsFromJsonCacheHit`：模拟 Redis 将 `List<T>` 元素反序列化为 Map，命中后必须恢复为方法声明的元素类型，且不得重新调用业务方法。
- `SytemDictValueProviderLegacyCacheTest`：模拟历史字典缓存返回 `List<Map>`，正向和反向翻译均不得抛 `ClassCastException`。
- `SysDictDataServiceImplTest`：继续验证字典加载和受管缓存注解合同。

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-cache \
  -Dtest=ForgeCacheKeyResolverTest,ForgeCacheAspectTest,CacheTransactionExecutorTest,MultiLevelCacheHandleTest,ManagedCacheCodecTest,CacheDefinitionResolverTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system \
  -Dtest=SytemDictValueProviderLegacyCacheTest,SysDictDataServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

真实 Redis 验收应在重启 Admin 加载新代码后访问组织树，确认 `SysOrgTreeVO.orgType` 翻译成功；无需以手工清空 Redis 作为永久修复手段。

## 11. Redisson 社区版 MULTI 兼容回归

- 生产代码不得调用 `RedissonClient#getLocalCachedMapCache`，避免社区版运行时抛出 PRO 功能异常。
- MULTI 使用 Caffeine 保存 L1，使用 `RMapCache` 保存带独立 TTL 的 L2；普通值和空值按 `ManagedCacheValue.localTtlNanos` 独立过期。
- 两个 MULTI 句柄共享同一远端 Map 和 Topic 时，一方覆盖/删除单 key 或清空缓存后，另一方必须失效本地值并读取最新远端状态。
- Topic 初次订阅或重连订阅时清空当前 L1，避免断线期间丢失失效消息后继续使用旧值。
- 多级缓存失效事件使用应用 `ObjectMapper` 的显式类型化 codec 完成真实 encoder/decoder 往返。

本轮只执行 starter 定向测试和差异检查，不执行完整 Maven/Vite 构建：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-cache \
  -Dtest=MultiLevelCacheHandleTest,ManagedCacheCodecTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

! rg -n "getLocalCachedMapCache|RLocalCachedMapCache|LocalCachedMapCacheOptions" \
  forge-framework/forge-starter-parent/forge-starter-cache/src/main
```
