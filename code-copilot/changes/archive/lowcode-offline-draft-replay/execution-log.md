# 执行日志

## 2026-08-10

- 阶段启动：确认现有业务动作已具备幂等键和发布快照，离线层只做本地草稿与受控重放编排。
- 新增 `forge-admin-ui/src/utils/offline-draft-runtime.js` 并从 `@/utils` 导出：本地草稿元数据绑定发布版本/schemaHash/记录版本，敏感键清洗，大小/数量上限。
- `replayDraft` 要求调用方显式注入 `loadCurrent` 与 `execute`，冲突和失败均在副作用前或当前步骤失败后停止，不自动覆盖服务端记录。
- 定向 Vitest：1 个文件、5 个测试全部通过。
- 目标 ESLint：0 errors。
- 前端生产构建：成功（前序扫码阶段构建已验证，本阶段只新增无副作用工具）。
- `git diff --check`：通过。

## 2026-08-11 重新打开审计

- 发现原阶段只完成了离线草稿纯工具，未接入 `AiCrudPage` 发布态，也未覆盖 PRD 要求的普通单据状态迁移、金额严格精度和结构化长期审计字段。
- 本轮按同一阶段增量扩展，不新增预售/会员/商品/收款专用类；真实 MySQL、Flyway 实跑和弱网浏览器 E2E 继续按既定分工由部署环境补验。

## 2026-08-11 增量实现

- 后端新增 `TRANSITION_STATUS`，把 `fromValue` 放入同一条条件 UPDATE；新增 `MONEY` 展示单位到最小货币单位的精确转换，超 scale、负数和 long 溢出失败关闭。
- 动作日志新增 `audit_event_type/status_field/status_from/status_to/change_summary/retention_until`，Flyway 为字段和审计索引提供 `information_schema` 防重复保护。
- 审计复核发现条件更新冲突会在写摘要前抛错，已调整为成功与失败均记录 `outcome`；摘要只保留事件、状态字段、from/to、目标配置和结果，不保存目标记录 ID 或业务表单原值。
- `AiCrudPage` 接入离线草稿：按租户、用户、应用、对象和表单组成 namespace；主子表自动保存/恢复；断网提交只写本地；恢复在线只提示，重新读取最新发布配置和服务端记录后由用户显式确认重放。
- 表单设计器新增“本地草稿与断网提交”普通入口；发布协议快照新增 offlineDraft 治理覆盖，避免下载/发布链路静默丢字段。

## 2026-08-11 验证记录

- 首轮后端定向命令：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Penable-tests -Dtest=BusinessActionCommandPolicyTest,TransitionStatusActionStepExecutorTest,AdjustNumberActionStepExecutorTest,BusinessActionExecutionServiceTest,BusinessObjectPublishServiceCommandTest,LowcodeProtocolSnapshotBuilderTest test`（JDK 17）。实际执行 28 个测试，27 通过、1 错误；根因是单测配置 `record.id` 却只设置客户端 request，未模拟服务端权威 `recordData`。
- 修正测试上下文并补充失败审计、留存与脱敏测试后重跑同一命令：30 个测试全部通过，`BUILD SUCCESS`；编译保留既有 deprecated/unchecked 警告和 Commons Logging 提示。
- 前端定向命令：`pnpm vitest run` 加 6 个目标测试文件（Node v20.19.0）。6 个文件、36 个测试全部通过。
- 目标 ESLint：离线运行时、AiCrudPage、动作/字段事件运行时、设计器协议和相关测试共 16 个文件，0 errors。
- 前端生产构建：`NODE_OPTIONS=--max-old-space-size=8192 pnpm build`（Node v20.19.0），8892 modules transformed，1m28s，成功；保留既有 `UserSelectModal` 命名冲突、动态/静态导入、CSS `//` 注释和大 chunk 提示。
- Flyway 静态检查：`V1.0.104__add_lowcode_audit_governance.sql` 无 `${...}` 占位符，迁移版本无重复；未运行真实数据库迁移。
- 差异检查：`git diff --check` 通过。
- 服务清理：本轮未启动数据库、Redis、Flow、后端服务、Vite dev server 或外部系统，无残留服务需要清理。
