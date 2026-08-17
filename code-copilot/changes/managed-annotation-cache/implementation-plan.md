# Managed Annotation Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Forge-owned annotation cache runtime with local, Redis and multi-level modes plus a centrally managed policy UI without enabling Spring Cache.

**Architecture:** `forge-starter-cache` owns annotations, AOP, key isolation, transaction coordination, Redisson/Caffeine handles and Redis policy distribution. `forge-plugin-system` persists operational overrides and exposes super-admin APIs; the existing cache page adds a managed-policy view while preserving Redis diagnostics.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring AOP/SpEL, Caffeine, Redisson 3.50, MyBatis-Plus/XML, Flyway, Vue 3, Naive UI, Vitest.

---

### Task 1: Freeze SDD contracts

**Files:**
- Create: `code-copilot/changes/managed-annotation-cache/spec.md`
- Create: `code-copilot/changes/managed-annotation-cache/tasks.md`
- Create: `code-copilot/changes/managed-annotation-cache/test-spec.md`
- Create: `code-copilot/changes/managed-annotation-cache/execution-log.md`
- Create: `code-copilot/changes/managed-annotation-cache/implementation-plan.md`

- [x] Write functional, security, consistency and rollback boundaries in `spec.md`.
- [x] Define P0 unit tests and build commands in `test-spec.md`.
- [x] Run `git diff --check -- code-copilot/changes/managed-annotation-cache` and fix every whitespace error.
- [ ] Commit only the five SDD files with message `[managed-annotation-cache] 建立受管缓存SDD基线`.

### Task 2: Add the annotation runtime with failing tests first

**Files:**
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/main/java/com/mdframe/forge/starter/cache/annotation/ForgeCacheConfig.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/main/java/com/mdframe/forge/starter/cache/annotation/ForgeCacheConfigs.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/main/java/com/mdframe/forge/starter/cache/annotation/ForgeCacheable.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/main/java/com/mdframe/forge/starter/cache/annotation/ForgeCachePut.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/main/java/com/mdframe/forge/starter/cache/annotation/ForgeCacheEvict.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/main/java/com/mdframe/forge/starter/cache/enums/CacheMode.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/main/java/com/mdframe/forge/starter/cache/enums/CacheScope.java`
- Create focused model, runtime, AOP and auto-configuration classes under `.../cache/managed/`.
- Test: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/test/java/com/mdframe/forge/starter/cache/managed/`
- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/pom.xml`
- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] Write `ForgeCacheKeyResolverTest` for SpEL, default argument hashing and missing identity bypass; run the class and verify it fails because the resolver does not exist.
- [ ] Implement `ForgeCacheKeyResolver` with `MethodBasedEvaluationContext`, trusted `SessionHelper` dimensions and SHA-256 output; rerun the test to green.
- [ ] Write `ForgeManagedCacheManagerTest` for LOCAL hit/put/evict and policy validation; verify red, then implement definition registration, effective policy and local handle.
- [ ] Add REDIS using `RMapCache` and MULTI using `RLocalCachedMapCache` configured with `CAFFEINE`, `INVALIDATE`, `CLEAR`, independent local TTL and remote entry TTL.
- [ ] Write and implement Redis policy map/topic refresh tests with mocked Redisson APIs; infrastructure exceptions must produce misses instead of escaping.
- [ ] Write `CacheTransactionExecutorTest` proving commit runs and rollback skips; implement Spring transaction synchronization.
- [ ] Write `ForgeCacheAspectTest` with a proxied sample service and prove Cacheable/Put/Evict behavior; implement the aspect and auto-configuration.
- [ ] Run the exact starter command from `test-spec.md`, then compile the module.
- [ ] Commit Task 2 files with message `[managed-annotation-cache] 实现注解缓存运行时`.

### Task 3: Add the system policy control plane

**Files:**
- Create entity/DTO/query/VO/mapper/service/controller classes named `SysCachePolicy*` or `SysManagedCache*` in `forge-plugin-system`.
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysCachePolicyMapper.xml`
- Test: `.../forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/service/impl/SysManagedCachePolicyServiceTest.java`
- Test: `.../forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/mapper/SysCachePolicyMapperContractTest.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/pom.xml`

- [ ] Write failing service tests for merge, insert, optimistic update conflict, mode validation, reset and clear.
- [ ] Implement the entity with explicit `@TableLogic(value = "0", delval = "id")` and DTO validation.
- [ ] Implement Mapper XML methods `selectActivePolicies`, `selectByIdentity` and `updateWithVersion`; every query must include trusted tenant and `del_flag = 0`.
- [ ] Implement in-memory pagination over the small registered definition set, merging DB override fields over code defaults.
- [ ] Schedule Redis runtime propagation only after transaction commit.
- [ ] Implement super-admin controller endpoints and operation logs without exposing keys or values.
- [ ] Add an application-ready bootstrap that reapplies active DB policies into the Redis runtime policy map.
- [ ] Run system tests and compile, then commit with message `[managed-annotation-cache] 增加缓存策略控制面`.

### Task 4: Add Flyway and permissions

**Files:**
- Create: `forge-server/db/migration/V1.0.120__add_managed_cache_policy.sql`

- [ ] Create `sys_cache_policy` with all Forge audit fields, `tenant_id DEFAULT 1`, BIGINT tombstone logic delete and `UNIQUE (tenant_id, application_code, cache_name, del_flag)`.
- [ ] Insert four API resources below the existing cache menu using `INSERT ... SELECT ... WHERE NOT EXISTS`; use `tenant_id=1`.
- [ ] Run XML, placeholder and duplicate-version static checks from `test-spec.md`.
- [ ] Commit with message `[managed-annotation-cache] 增加缓存策略迁移脚本`.

### Task 5: Build the managed cache workbench

**Files:**
- Modify: `forge-admin-ui/src/views/system/cache.vue`
- Create: `forge-admin-ui/src/views/system/cache/ManagedCachePolicies.vue`
- Create: `forge-admin-ui/src/views/system/cache/managed-cache-policy.js`
- Test: `forge-admin-ui/src/views/system/cache/managed-cache-policy.spec.js`

- [ ] Extract policy normalization and validation into pure functions; write failing Vitest cases for allowed modes, positive TTL and MULTI L1 <= L2.
- [ ] Implement the pure functions and rerun the test.
- [ ] Add line Tabs with managed cache as default and the existing Redis diagnostic surface preserved under its own tab.
- [ ] Implement a compact table, application/cache filters, edit modal, clear confirmation and reset action; do not expose entry keys or values.
- [ ] Run target ESLint, Vitest and production build under Node `v20.19.0`.
- [ ] Commit with message `[managed-annotation-cache] 增加缓存策略管理页`.

### Task 6: Migrate dictionary caching

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysDictDataServiceImpl.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/listener/DictChangeEventListener.java`
- Test: relevant system dictionary tests and `ForgeCacheAspectTest`.

- [ ] Add `@ForgeCacheConfig(name = "system:dict-data", mode = MULTI, scope = TENANT, localTtlSeconds = 60, redisTtlSeconds = 1800, localMaxSize = 2000)`.
- [ ] Replace the handwritten read path with `@ForgeCacheable(cacheName = "system:dict-data", key = "#dictType")` and a direct XML/Mapper-backed load.
- [ ] Replace both clear methods with `@ForgeCacheEvict` contracts and remove Redis/manual local fields and constants.
- [ ] Change the listener to `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`.
- [ ] Run dictionary and system control-plane tests, then commit with message `[managed-annotation-cache] 迁移字典缓存到受管运行时`.

### Task 7: Close verification and documentation

**Files:**
- Modify: `code-copilot/changes/managed-annotation-cache/spec.md`
- Modify: `code-copilot/changes/managed-annotation-cache/tasks.md`
- Modify: `code-copilot/changes/managed-annotation-cache/execution-log.md`
- Modify: `code-copilot/memory/decisions.md`

- [ ] Run target tests, Admin aggregate compile, XML/SQL checks, target frontend lint/test/build and `git diff --check`.
- [ ] Record exact commands, counts, warnings, skipped real-service checks and service cleanup in `execution-log.md`.
- [ ] Mark only actually completed tasks and spec acceptance items.
- [ ] Record the accepted “custom AOP + Redisson/Caffeine + DB override” architecture decision.
- [ ] Commit documentation only with message `[managed-annotation-cache] 完成验证与决策沉淀`.

### Task 8: Fix review findings in the managed cache runtime

**Files:**
- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/main/java/com/mdframe/forge/starter/cache/managed/ForgeManagedCacheManager.java`
- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/main/java/com/mdframe/forge/starter/cache/managed/definition/CacheDefinitionResolver.java`
- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/test/java/com/mdframe/forge/starter/cache/managed/ForgeManagedCacheManagerTest.java`
- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-cache/src/test/java/com/mdframe/forge/starter/cache/managed/ForgeCacheAspectTest.java`
- Create: focused typed-codec round-trip test under `.../src/test/java/com/mdframe/forge/starter/cache/managed/`
- Modify: `code-copilot/changes/managed-annotation-cache/{tasks.md,test-spec.md,execution-log.md}`

- [x] Use explicit `TypedJsonJacksonCodec` instances for managed values, definitions, policies and control events; prove every record can round-trip with the configured Jackson modules.
- [x] Reject an unmatched cache name when the target class declares one or more `@ForgeCacheConfig` annotations; retain the global fallback only for classes without declarations.
- [x] Register each local definition once, publish it with Redis `putIfAbsent`, and reject incompatible remote definitions while ignoring `source` in the compatibility comparison.
- [x] Replace the mutable policy override map with atomically published immutable snapshots and consistently apply copy-on-write updates for local control events.
- [x] Run the incremental starter/system/aggregate verification commands and append exact results to `execution-log.md`.
