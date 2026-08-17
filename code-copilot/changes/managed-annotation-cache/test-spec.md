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
  -Dtest=ForgeManagedCacheManagerTest,ForgeCacheKeyResolverTest,ForgeCacheAspectTest,CacheTransactionExecutorTest \
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
  -Dtest=SysManagedCachePolicyServiceTest,SysCachePolicyMapperContractTest \
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
- 所有跳过项、环境告警和服务清理情况已写入 `execution-log.md`。
