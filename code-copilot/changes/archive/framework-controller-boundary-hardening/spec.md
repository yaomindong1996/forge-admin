# Controller 边界与查询规范整改
> status: review
> created: 2026-07-27
> complexity: 🔴复杂

## 1. 背景与目标

`output/框架问题细化整改清单.md` 指出 Controller 层存在查询构造、裸异常和分页参数不一致。本轮重新按当前工作树扫描，在客户端凭据整改已清理 `SysClientController` 后，基线为：

- 14 个 Controller 文件、35 个 Wrapper 或 `lambdaQuery/lambdaUpdate` 数据操作。
- 4 个 Controller 文件、9 处 `throw new RuntimeException`。
- 3 个分页端点仍只接收 `page`；`FlowFormController` 已支持 `pageNum/page` 双读，作为兼容基线。

本变更的目标是让 Controller 只负责协议转换和简单编排，查询、更新、统计和清理数据的逻辑进入 Service/Manager，所有自定义 SQL 进入 Mapper XML，同时保持现有 API 业务语义。

## 2. 代码现状（Research Findings）

### 2.1 Controller 越层基线

| 模块 | Controller | 主要操作 |
|------|------------|----------|
| System | `SysLoginLogController` | 登录日志分页 |
| System | `LoginTenantAssetController` | 租户直接 Mapper 查询 |
| AI | `AiAgentController` | 智能体分页/启用列表 |
| AI | `AiModelController` | 模型列表与默认供应商更新 |
| AI | `AiProviderController` | 默认供应商更新 |
| Generator | `GenController` | 表元数据分页 |
| Generator | `GenDatasourceController` | 数据源分页/列表 |
| Generator | `GenTemplateController` | 模板分页/列表 |
| Generator | `GenTableColumnController` | 字段查询/删除前查询 |
| Data | `DataDatasetController` | 数据集状态更新 |
| Message | `MessageBizTypeController` | 启用业务类型列表 |
| Flow | `FlowInstanceController` | 流程业务查询 |
| Flow | `FlowErrorLogController` | 错误统计 Wrapper |
| Flow | `FlowMonitorController` | 监控查询、统计、批量清理 |

### 2.2 异常和分页

- `SysUserController` 4 处导入必填校验裸异常。
- `SysExcelExportConfigController` 1 处配置不存在裸异常。
- `FlowModelVersionController` 1 处 BPMN 内容不存在裸异常。
- `ExcelEnhancedController` 3 处下载异常把底层消息拼接给客户端，同文件另有 2 处错误响应也直接回传底层消息。
- `SysCacheController`、`FlowMonitorController`、`FlowErrorLogController` 只接收 `page`，前端标准 `pageNum` 会被忽略。

### 2.3 流程清理语义

`FlowMonitorController#deleteForgeFlowRecords` 包含 7 个删除操作，但原清单将它们全部视为物理删除并不准确：

- `FlowFormInstance`、`FlowFillBatchItem` 实体已显式声明 `@TableLogic`，现有 BaseMapper `delete` 执行逻辑删除。
- `FlowTask`、`FlowComment`、`FlowCc`、`FlowErrorLog`、`FlowBusiness` 没有逻辑删除字段。该端点是需人工确认文本的管理员流程数据不可逆清理，且同时删除 Flowable 运行时和历史数据。本变更保留这 5 类物理清理语义，但要求下沉到专用 Service + Mapper XML，保留确认门禁和审计日志。该操作无数据库回滚能力，只能通过备份恢复。

## 3. 功能点

- [x] 三个剩余分页端点以 `pageNum` 为标准参数，暂时保留可选 `page` 别名。
- [x] 9 处 Controller 裸 `RuntimeException` 归零，Excel 失败响应不回传底层异常消息。
- [x] System 的 Controller 查询进入 Service/Mapper XML。
- [x] AI 的 Controller 查询和默认项更新进入事务 Service/Mapper XML。
- [x] Generator 的 Controller 查询进入 Service/Mapper XML。
- [x] Data/Message 的 Controller 更新和查询进入 Service/Mapper XML。
- [x] Flow 的 Controller 查询、统计和 Forge 关联记录清理进入 Service/Mapper XML。
- [x] 14 个目标 Controller 的 Wrapper/`lambda*` 数据操作扫描归零。

## 4. 业务规则

1. Controller 可做请求参数解析和跨 Service 简单编排，不得构造 MyBatis-Plus Wrapper 或直接调 Mapper。
2. 自定义查询、统计、批量更新/删除 SQL 写在 Mapper XML；单行 `selectById/insert/updateById/deleteById` 仅可在 Service 使用。
3. 新分页请求优先使用 `pageNum`；兼容期同时传入 `page` 时，旧别名 `page` 优先，与已上线 `FlowFormController` 行为一致。
4. 业务校验异常使用 `BusinessException`；系统异常记录完整堆栈后仅向客户端返回稳定文案。
5. AI 默认模型/供应商的“清除旧默认 + 设置新默认”必须位于同一事务。
6. 流程清理保持既有不可逆管理语义：2 类 Forge 记录逻辑删除，5 类 Forge 记录及 Flowable 运行/历史记录物理清理。
7. 不改变现有路径、HTTP 方法和响应主体关键字段；不可逆流程清理新增独立强权限，不自动扩散到普通角色。

## 5. 数据变更

本变更不新增数据库字段。`V1.0.55__secure_flow_monitor_cleanup.sql` 将流程监控菜单权限统一为 `flow:monitor:view`，新增 `flow:monitor:manage`、`flow:monitor:cleanup` 两个按钮权限，并分别创建唯一权限标识的查询、管理和清理 API 资源。脚本使用显式列、`tenant_id=1` 和 `NOT EXISTS`；仅为原已拥有监控菜单的角色回填查询 API，管理和清理权限不自动授权。流程清理的逻辑/物理语义按现有表结构保持；如后续需要将 5 张表改为可恢复业务数据，必须单独 Proposal + Flyway，不在本轮隐式改变。

## 6. 接口变更

| 接口 | 变更 |
|------|------|
| `/system/cache/page` | 新增标准 `pageNum`，保留可选 `page` |
| `/api/flow/monitor/instances` | 新增标准 `pageNum`，保留可选 `page` |
| `/api/flow/monitor/error-logs` | 新增标准 `pageNum`，保留可选 `page` |
| Excel 下载/导入/导出错误 | 保留 HTTP 错误语义，不再返回底层异常文本 |

## 7. 影响范围

- `forge-plugin-system`、`forge-plugin-ai`、`forge-plugin-generator`、`forge-plugin-data`、`forge-plugin-message`。
- `forge-flow-server`。
- `forge-starter-excel`。
- 相关 Service、Mapper 接口/ XML 和增量测试。

## 8. 风险与关注点

- Mapper XML 下沉不得丢失租户、数据权限和排序条件。
- 分页兼容不得让旧调用方翻页回归。
- 流程清理无法通过数据库事务恢复已删除的 Flowable 历史数据，必须保留确认文本、失败清单和完整日志。
- 当前工作树包含未提交的 crypto/client 变更，验证和后续提交必须精确指定文件。

## 8.5 测试策略

- 分页参数双读单测/协议静态检查。
- Service 查询条件、排序、事务更新和流程清理语义增量测试。
- 所有新增/修改 Mapper XML 进行 `xmllint`。
- 相关 Maven 模块 package，加上 Wrapper、RuntimeException、分页参数静态扫描。
- 本地不启动真实数据库或执行不可逆流程清理。

## 9. 待澄清

无。依据用户对分阶段整改方案的整体授权继续执行。

## 10. 技术决策

1. 按 System/AI/Generator/Data+Message/Flow 分批下沉，每批独立编译和扫描。
2. 优先扩展已有 Service，只在流程监控编排等职责明显独立时增加 Manager/Service。
3. 查询条件不用 Service Wrapper 替代 Controller Wrapper，而是进入 Mapper XML。
4. 管理员显式流程清理保留物理删除例外，原因和无回滚性由本 Spec 记录。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|----------------|------|
| Proposal | 完成 | 本变更四份 SDD 文档 | 已重新扫描当前基线 |
| Task 1 | 完成 | System/Flow/Excel 目标 Controller | 分页双读和异常边界已整改 |
| Task 2 | 完成 | System Controller、Service、Mapper/XML | 登录日志和租户资产查询已下沉 |
| Task 3 | 完成 | AI Controller、Service/Manager、Mapper/XML | 查询和默认项更新已下沉；Manager 单测受本机 Mockito attach 限制 |
| Task 4 | 完成 | Generator Controller、Service、Mapper/XML | 四个 Controller 已无 Wrapper/Mapper 直连，Generator Reactor 构建通过 |
| Task 5 | 完成 | Data/Message Controller、Service、Mapper/XML | 发布状态更新、分页和启用列表查询已下沉，Data/Message Reactor 构建通过 |
| Task 6 | 完成 | Flow Controller、Service、Mapper/XML、合同测试 | 监控查询/统计/状态同步/清理已下沉；2 逻辑 + 5 物理删除合同和确认门禁通过静态验证 |
| Task 7 | 完成 | Flow 事务/SPI/权限与租户边界、AI 并发锁、增量合同和聚合构建 | Spec Review 与最终 Code Quality Review 均已通过；部署 E2E 门禁保留 |

## 12. 审查结论

- Spec Compliance Review：PASS。
- 首次 Code Quality Review：FAIL，无 Critical，4 个 Important。
- 首次修复内容：流程清理按实例使用事务模板；系统异常统一稳定公开文案；Flow 通过 `FlowMonitorUserLookup` SPI 解耦 System Service；AI 模型摘要同步增加供应商/模型行锁。
- 第二次 Code Quality Review：FAIL，无 Critical，4 个 Important；发现清理事务未固定 `REQUIRES_NEW`、AI 模型/供应商锁顺序反转、供应商删除检查竞态和用户查询降级吞异常。
- 第二次修复内容：新增 `FlowCleanupTransactionExecutor` 固定 `REQUIRES_NEW` 并记录降级异常堆栈；AI 统一供应商先锁、模型后锁并校验快照；供应商删除的锁、关联计数和删除收敛到同一 Manager 事务。
- 第三次 Code Quality Review：FAIL，无 Critical，2 个 Important、1 个 Minor；发现批量流程清理前置查询仍可能泄露原始异常、默认供应商切换和通用保存仍绕过统一锁协议，且外层事务挂起/恢复缺少合同覆盖。
- 第三次修复内容：批量流程清理候选查询和后续非业务异常统一记录完整堆栈并抛稳定文案；默认供应商切换进入 Manager，按供应商 ID 升序锁定当前租户全部有效供应商后再切换；通用新增/更新不再接受 `isDefault` 写入；事务测试补充外层事务挂起/恢复。
- 第四次 Code Quality Review：FAIL，2 个 Critical、1 个 Important；发现不可逆清理缺专用权限/租户边界、批量清理按模型扩大筛选范围，以及超级管理员默认供应商切换可能跨租户。
- 第四次修复内容：清理端点增加 `flow:monitor:cleanup`，移除 Controller 类级 `@IgnoreTenant` 并新增 `V1.0.55` 权限资源；候选集仅使用当前租户 `sys_flow_business` 筛选结果，单条和逐实例事务内均校验租户归属，七份清理 SQL 显式带租户；AI 默认切换将必填租户贯穿锁、清默认和设默认 SQL。
- 修复验证：Flow 事务执行器 2/2、Flow 合同 12/12、AI 并发合同 1/1、9 份相关 Mapper XML 解析通过、Flow+AI 32 模块 Reactor 退出码 0。
- 第五次 Code Quality Review：FAIL，3 个 Critical、2 个 Important、1 个 Minor；发现权限资源唯一键冲突、表单清理遗漏租户条件、监控查询/管理缺少分级强权限和 Flowable 访问前的租户归属守卫、表单状态 Mapper 参数不匹配，以及合同测试对整份 XML 搜索造成假阳性。
- 第五次修复内容：`V1.0.55` 拆分查询/管理/清理权限并为 API 使用唯一权限标识，仅回填既有菜单角色的查询 API；表单状态回调和逻辑清理显式传递 `tenantId`；流程实例与任务操作在访问 Flowable 前通过 `sys_flow_business + tenantId` 校验归属；合同测试精确截取目标 Mapper statement 后断言。
- 第五次修复验证：Flow Controller 合同 14/14、独立清理事务测试 2/2、七份 Flow Mapper XML 解析通过、Flow Server 依赖 Reactor 32 模块构建成功；Wrapper、裸异常、Flyway placeholder/版本重复和差异格式扫描均通过。
- 第六次 Code Quality Review：FAIL，2 个 Critical、2 个 Important、1 个 Minor；发现错误日志接口保留租户旁路且缺少分级权限、监控聚合未显式限定租户、迁移兼容 UPDATE 存在权限唯一键碰撞、管理归属守卫与 Flowable 修改未处于同一事务，以及只读角色仍可见管理/清理按钮。
- 第六次修复内容：错误日志 Controller/Service/Mapper 全链路增加 `view/manage` 权限、当前租户条件和业务归属行锁；监控列表、统计、趋势、分布显式传递 `tenantId`；移除碰撞式兼容 UPDATE 并补 PUT API 权限；终止、回退、转派、挂起、激活及清理在事务内锁定当前租户业务行；前端按 `manage/cleanup` 隐藏操作。
- 第六次修复验证：`forge-plugin-flow` 编译通过，Flow Controller 合同最终 18/18，两份本轮 Mapper XML 解析通过，监控页 ESLint 和目标差异格式检查通过；按用户要求不重复执行全量 Reactor、前端生产构建或部署 E2E。
- 最终 Code Quality Review：PASS。错误日志与清理统一采用业务记录优先的锁顺序，UI 权限信息缺失时失败关闭；未发现剩余或新增 Critical、Important、Minor。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-27
- **确认人**：用户
- **确认内容**：用户明确要求“按照你的思路 继续进行”，授权按已拆分的剩余整改阶段继续 `/apply`。
