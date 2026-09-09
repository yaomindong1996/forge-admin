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

## Task 8：Review 增量修复

- [x] 为缓存值、定义、策略和控制消息配置显式类型化 Redis codec，并补充真实序列化往返测试。
- [x] 类上存在缓存声明时拒绝未匹配的缓存名，避免拼写错误降级为隐式 TENANT/REDIS 缓存。
- [x] 定义只在本地首次注册时原子写入 Redis，并拒绝远端不兼容定义。
- [x] 将策略覆盖改为不可变快照原子替换，消除刷新过程中的空窗口。
- [x] 执行本轮增量测试、聚合编译和静态检查并回填执行日志。

## Task 9：缓存管理页规整与诊断下线

- [x] 移除“Redis 诊断”页签和全部原始 key/value 前端调用，缓存路由只承载受管缓存工作台。
- [x] 合并表格信息列，规整筛选、分页、操作和策略编辑弹窗，消除常用桌面宽度下的列覆盖。
- [x] 删除 `SysCacheController` 与专用 `CacheInfoDTO`，关闭枚举、读取和任意删除 Redis 数据的 HTTP 攻击面。
- [x] 新增 Flyway，清理旧诊断按钮/API 的角色授权并按主键墓碑逻辑删除资源。
- [x] 更新受影响的静态合同测试，执行前端定向测试、ESLint、构建、Admin 聚合编译和安全扫描。
- [x] 使用 Playwright 验证 1440x900 与 390x844 视口，无重叠、无 Redis 诊断入口、无新增控制台错误。
- [x] 将命令、结果、跳过项和服务状态回填 `execution-log.md`。

## Task 10：字典消费链路收口与失败统计

- [x] 通用 `useDict` 改用 `/system/dict/data/type/{dictType}`，字典管理页继续使用 `/list`。
- [x] 移除 `SytemDictValueProvider` 独立的 30 分钟 Map 缓存，字典翻译统一委托受管 Service。
- [x] 字典变更只失效 `system:dict-data`，不再维护第二套清理协议。
- [x] 桌面和移动端统计同时展示命中、未命中、写入和失败，非零失败使用当前主题错误色。
- [x] 补充前后端回归测试，完成 ESLint、生产构建、Admin 聚合编译和运行页面验证。

## Task 11：运行态泛型缓存兼容修复

- [x] 在 `ForgeCacheAspect` 命中边界按业务方法声明返回类型恢复集合/Map/数组元素类型。
- [x] 类型恢复失败时清理异常 entry 并穿透业务方法，避免坏缓存持续触发运行时强转异常。
- [x] `SytemDictValueProvider` 兼容历史 `Map` 字典项，去除增强 `for` 循环的裸泛型强转风险。
- [x] 新增 AOP 泛型集合和字典历史缓存回归测试，完成 Admin 45 模块聚合编译。
- [ ] 用户重启 Admin 后在真实 Redis 命中场景复验 `SysOrgTreeVO.orgType` 翻译。

## Task 12：Redisson 社区版多级缓存兼容修复

- [x] 确认 `RLocalCachedMapCache` 在 Redisson 3.50 社区版直接抛出 PRO 功能异常，并冻结社区版兼容实现边界。
- [x] 将 MULTI 句柄替换为 Caffeine 本地层、`RMapCache` Redis 层和类型化 Topic 失效通知的组合实现。
- [x] 补充本地/远端 TTL、单 key/全量跨实例失效、重订阅清空和 codec 往返回归测试。
- [x] 执行 starter 定向测试与差异检查，回填执行日志；不执行完整项目构建。
