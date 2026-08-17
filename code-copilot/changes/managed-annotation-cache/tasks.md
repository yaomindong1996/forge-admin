# 受管注解驱动缓存任务

## Task 1：SDD 与合同冻结

- [x] 创建 `spec.md`，冻结范围、非目标、作用域、一致性和管理权限边界。
- [x] 创建 `implementation-plan.md`，列出准确文件、TDD 步骤和验证命令。
- [x] 创建 `test-spec.md` 与 `execution-log.md`。
- [x] 执行文档空白和状态一致性检查。

## Task 2：starter 注解与运行时

- [x] 新增 `CacheMode`、`CacheScope`、四类注解和公共策略模型。
- [x] 新增配置属性、有效策略校验、代码定义注册与 Redis 覆盖快照。
- [x] 新增 SpEL/默认参数键解析、可信身份作用域和 SHA-256 摘要。
- [x] 新增 LOCAL、REDIS、MULTI 三种受管缓存句柄。
- [x] 新增事务提交后协调、AOP 和失败开放处理。
- [x] 新增 starter 自动配置和单元测试。

## Task 3：系统控制面

- [x] 新增 `sys_cache_policy` 实体、DTO、VO、Mapper XML、Service 和 Controller。
- [x] 实现定义与覆盖合并分页、乐观锁更新、恢复默认和清空。
- [x] 新增应用启动时数据库覆盖同步。
- [x] 新增 Service/Mapper 合同测试。

## Task 4：Flyway 与权限资源

- [x] 新增下一可用版本 Flyway，创建逻辑删除策略表和 active-only 唯一索引。
- [x] 为缓存管理菜单补充策略分页、编辑、恢复默认和清空 API 资源。
- [x] 保证所有内置数据 `tenant_id=1` 且插入具备 `NOT EXISTS` 防重复。

## Task 5：管理端

- [x] 将缓存管理页拆分为“受管缓存”和“Redis 诊断”页签。
- [x] 新增策略搜索、编辑校验、清空确认和恢复默认操作。
- [x] 保留现有 Redis 指标与原始键诊断能力。
- [x] 新增前端纯函数测试或静态合同测试。

## Task 6：首个真实迁移

- [x] 使用 `@ForgeCacheConfig/@ForgeCacheable/@ForgeCacheEvict` 迁移字典缓存。
- [x] 删除字典 Service 手写本地缓存、Redis key 和 TTL 常量。
- [x] 将字典变更监听改为事务提交后执行并保留无事务回退。
- [x] 更新字典缓存回归测试。

## Task 7：验证与回填

- [x] 执行 starter 和 system 定向测试。
- [x] 执行 Admin 聚合编译。
- [x] 执行 Mapper XML、Flyway placeholder、重复版本和 `git diff --check` 检查。
- [x] 使用 Node `v20.19.0` 执行前端定向检查与生产构建。
- [x] 回填 `execution-log.md`、`tasks.md` 和 `spec.md`，记录警告与跳过项。
- [x] 将确认后的可复用架构决策写入 `code-copilot/memory/decisions.md`。
- [ ] 由用户在真实 MySQL/Redis/Admin 环境执行 Flyway、双实例失效同步和权限 E2E。
