# 测试 Spec — Controller 边界与查询规范整改
> status: apply
> created: 2026-07-27

## 1. 测试原则

- 按模块增量执行，不重跑无关全量 E2E。
- Mapper XML 下沉要验证条件、排序、分页和删除语义，不仅验证编译。
- 不连接真实数据库，不执行流程数据清理。

## 2. P0 范围

| 场景 | 预期 |
|------|------|
| 仅 `pageNum` | 三个端点使用该页码 |
| 仅旧 `page` | 兼容且页码不变 |
| 两者同时传入 | 旧 `page` 优先，与 `FlowFormController` 一致 |
| Controller 业务校验 | 抛 `BusinessException`，不存在裸 `RuntimeException` |
| Excel 底层异常 | 日志有堆栈，响应只包含稳定错误文案 |
| AI 默认项切换 | 清理和设置在单个事务内 |
| 流程 Forge 记录清理 | 2 类逻辑删除，5 类显式物理删除 |

## 3. P1 静态合同

- 14 个基线 Controller 不再 import/构造 Query/Update Wrapper，不再调用 Service `lambdaQuery/lambdaUpdate`。
- Controller 不直接注入 Mapper。
- 自定义查询和更新均有 Mapper XML 对应语句。
- Mapper XML 通过 `xmllint --noout`。
- `rg -n 'throw new RuntimeException' forge-server --glob '*Controller.java'` 无输出。

## 4. 模块验证

| 批次 | 必跑 |
|------|------|
| Task 1 | System/Flow/Excel 相关模块 compile/package，静态协议检查 |
| Task 2 | System Reactor package + 目标测试 |
| Task 3 | AI Reactor package + 目标测试 |
| Task 4 | Generator Reactor package + 目标测试 |
| Task 5 | Data/Message Reactor package + 目标测试 |
| Task 6 | Flow Server package + 查询/清理合同测试 |
| Task 7 | 聚合静态扫描、`git diff --check`、两阶段审查 |

## 5. 跳过项

- 真实 MySQL 的 Mapper SQL 执行：本地不修改真实数据库。
- 真实 Flowable 删除 E2E：属于不可逆管理操作，仅做代码/合同验证。
- 全量前端构建：本变更默认不修改前端；如产生前端差异则升级为必跑。

## 6. 执行证据

所有命令、结果、警告、跳过原因和服务清理情况追加到 `execution-log.md`。

## 7. 本轮增量验证（Task 4）

- Generator 四个目标 Controller 的 Wrapper、`lambda*` 和 Mapper 直连扫描。
- Generator 四份新增/修改 Mapper XML 的 `xmllint` 解析。
- Generator Reactor `package -DskipTests`，覆盖接口、实现和 Mapper 装配的编译合同。
- 真实数据库分页、用途范围筛选和字段重置 SQL 留待集成环境验证。

## 8. 本轮增量验证（Task 5）

- Data/Message 两个目标 Controller 和对应 Service 的 Wrapper/`lambda*` 扫描。
- `DataDatasetMapper.xml`、`SysMessageBizTypeMapper.xml` 的 `xmllint` 解析。
- Data/Message Reactor `package -DskipTests`。
- 真实数据库租户隔离、逻辑删除和分页 SQL 留待集成环境验证。

## 9. 本轮增量验证（Task 6）

- 三个 Flow 目标 Controller 的 Wrapper 和 Mapper 直连扫描。
- 监控分页、详情、趋势、分布、错误统计和状态更新必须通过 `FlowBusinessMapper.xml`/`FlowErrorLogMapper.xml`。
- `FlowControllerBoundaryContractTest` 固化确认文本、无事务回滚承诺、2 类逻辑删除和 5 类物理删除合同。
- 七份 Flow Mapper XML 执行 `xmllint --noout`。
- Flow Server 依赖 Reactor `package -DskipTests`，并单独编译/执行合同测试。
- 真实 MySQL 查询和不可逆 Flowable 清理仍留待受控集成环境验证。

## 10. 本轮增量验证（Spec Review 修复）

- `FlowControllerBoundaryContractTest` 增加 `page`/`pageNum` 四组合和流程分布首个非空标题合同。
- `AsyncExportSecurityContractTest` 固化异步任务公开副本、`filePath` 隐藏和稳定失败文案。
- Flow 子模块受本机 `.m2` 旧快照限制时，使用当前 Reactor `target/classes` 作为附加类路径编译并执行目标测试，不把环境失败写成通过。

## 11. 本轮增量验证（同步 Excel 导入泄露修复）

- `ExcelImportSecurityContractTest` 覆盖总体导入异常、监听器未知解析异常和内部错误报告路径序列化隐藏。
- `SysUserImportSecurityContractTest` 固化行级/总体稳定文案和完整异常日志合同。
- 新增安全合同测试不依赖 Mockito inline agent，避免本机 JDK attach 能力影响测试结论。
- Maven 必须启用 `enable-tests` profile；默认 `forge.tests.skip=true` 的命令只记录为跳过，不计入通过。

## 12. 本轮增量验证（Code Quality Review 修复）

- `FlowControllerBoundaryContractTest` 扩展到 9 条，新增逐实例事务、稳定失败文案和 Flow Service 不依赖 System Service 的合同。
- `AiModelProviderConcurrencyContractTest` 固化供应商行锁、模型行锁和多供应商确定性锁顺序，不依赖 Mockito inline agent。
- Flow 合同必须确认 Controller 全部系统异常分支不拼接 `e.getMessage()`；业务拒绝文案保持原有语义。
- 重新执行 Flow+AI 依赖 Reactor `package -DskipTests`，确认 SPI 适配器和 Mapper 锁查询可聚合装配。

## 13. 本轮增量验证（第二次 Code Quality Review 修复）

- `FlowCleanupTransactionExecutorTest` 使用无 Mockito 的记录型事务管理器验证三次执行均为 `PROPAGATION_REQUIRES_NEW`，成功独立提交、失败独立回滚。
- Flow 合同检查监控用户查询降级保留完整异常日志，且服务不再存在 `catch (Exception ignored)`。
- AI 并发合同验证更新/删除模型均先锁相关供应商，再锁模型并校验供应商快照。
- AI 并发合同验证供应商删除按“供应商行锁 → 关联模型计数 → 逻辑删除”在同一 Manager 事务中执行，Controller 不再做检查后删除。

## 14. 本轮增量验证（第三次 Code Quality Review 修复）

- `FlowControllerBoundaryContractTest` 增加批量清理候选查询稳定错误边界合同，内部日志保留完整堆栈，公开异常不拼接底层消息。
- `FlowCleanupTransactionExecutorTest` 增加已有外层事务场景，验证 `REQUIRES_NEW` 独立事务会挂起并恢复外层事务。
- `AiModelProviderConcurrencyContractTest` 增加默认供应商专用 Manager 路径、全量供应商升序行锁和通用保存忽略 `isDefault` 的合同。
- `AiProviderMapper.xml` 执行 `xmllint --noout`，并重新执行 Flow+AI 依赖 Reactor `package -DskipTests`。

## 15. 本轮增量验证（第四次 Code Quality Review 修复）

- `FlowControllerBoundaryContractTest` 扩展到 12 条，覆盖两个清理端点的专用权限、移除类级租户忽略、`V1.0.55` 资源合同和不自动授权。
- 清理候选必须由 `selectBusinessesForCleanup(tenantId, ...)` 返回，禁止再按 `modelKey/status` 扩大到未满足发起人和时间筛选的 Flowable 实例。
- 单条与逐实例清理均校验 `processInstanceId + tenantId` 归属；七张 Forge 表的物理/逻辑删除和无实例业务记录删除都显式带 `tenant_id`。
- AI 默认供应商合同要求 `requireTenantId()`，并将 `tenantId` 贯穿全量锁、清默认和设默认 Mapper XML。
- 新增 Flyway 只做静态防重复/权限边界检查；真实 MySQL 和不可逆 Flowable 清理仍为部署门禁。

## 16. 本轮增量验证（第五次 Code Quality Review 修复）

- `FlowControllerBoundaryContractTest` 扩展到 14 条，覆盖查询、管理、清理三级强权限，以及流程实例/任务在访问 Flowable 前的当前租户归属守卫。
- 精确截取七份 Mapper XML 的目标 statement，验证表单逻辑清理和业务/表单状态更新均显式使用 `tenantId`，避免整份 XML 搜索产生假阳性。
- `V1.0.55` 验证 API 权限标识唯一，已有监控菜单角色只回填查询 API，管理和不可逆清理权限不自动授权。
- 重新执行 `FlowCleanupTransactionExecutorTest`、七份 Flow Mapper XML 解析和 Flow Server 依赖 Reactor 构建；真实 MySQL/Flyway、权限角色矩阵和不可逆 Flowable 清理仍为部署门禁。

## 17. 本轮增量验证（第六次 Code Quality Review 修复）

- `FlowControllerBoundaryContractTest` 最终扩展到 18 条，覆盖错误日志查询/重试/解决的权限与租户边界、监控聚合显式租户参数、管理/清理事务行锁与统一锁顺序、PUT API 权限和迁移碰撞 UPDATE 清除。
- `forge-plugin-flow` 只执行目标模块编译；`FlowBusinessMapper.xml`、`FlowErrorLogMapper.xml` 执行 XML 解析，监控页只执行目标 ESLint。
- 按用户要求不重复运行已通过的全 Reactor 和前端生产构建；真实 MySQL/Flyway、角色矩阵、并发清理和不可逆 Flowable 清理继续作为部署门禁。
