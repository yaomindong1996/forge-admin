# 离线草稿与重放测试规格

| ID | 场景 | 预期 |
|---|---|---|
| DRAFT-01 | 保存/读取合法草稿 | 数据可恢复并带发布版本、schemaHash 和时间戳 |
| DRAFT-02 | token/password/secret/header 等敏感键 | 不进入本地草稿 |
| DRAFT-03 | 循环引用、函数、超长数据 | 失败关闭并返回 `DRAFT_INVALID_DATA`/`DRAFT_TOO_LARGE` |
| DRAFT-04 | 超过数量上限 | 只清理明确的最旧草稿，不影响其它存储 |
| REPLAY-01 | 相同幂等键追加两次 | 只保留一条意图 |
| REPLAY-02 | 按日志顺序全部成功 | 每条只执行一次，草稿完成 |
| REPLAY-03 | 中途失败 | 标记 `REPLAY_FAILED`，后续意图不执行 |
| CONFLICT-01 | 发布版本变化 | 标记 `PUBLISHED_VERSION_CONFLICT`，不执行 |
| CONFLICT-02 | 记录版本变化 | 标记 `RECORD_VERSION_CONFLICT`，不执行 |
| CONFLICT-03 | 记录不可用/无权限 | 标记 `RECORD_UNAVAILABLE`，不执行 |
| SECURITY-01 | 浏览器自报身份/凭据 | 不作为重放可信上下文，也不写入本地草稿 |
| STATUS-01 | DRAFT -> SUBMITTED 合法迁移 | 单 SQL expected-status 命中并记录 from/to |
| STATUS-02 | 非法状态跳转/并发已变更 | 副作用失败关闭，事务回滚，不产生伪成功审计 |
| MONEY-01 | MONEY 两位小数合法输入 | 转换为 long 最小货币单位 |
| MONEY-02 | MONEY 超过 scale 或负数 | 拒绝且不静默舍入 |
| AUDIT-01 | 状态动作成功/失败 | 记录结构化状态字段、版本、幂等键和可信操作者引用 |
| AUDIT-02 | 审计脱敏 | 不包含手机号、表单原值、SQL、凭据或外部响应 |
| OFFLINE-01 | 发布态启用草稿 | 使用租户/用户/应用/对象/表单安全 namespace 自动保存并恢复 |
| OFFLINE-02 | 断网提交且未配置重放动作 | 只保存本地草稿，不发服务端请求 |
| OFFLINE-03 | 恢复在线并显式确认重放 | 先检查版本/记录版本，确认后每条意图按幂等键执行 |
| OFFLINE-04 | 恢复时发生冲突 | 只标记冲突并展示摘要，不自动覆盖 |

## 2026-08-10 基线结果

- 离线草稿定向 Vitest：1 个文件、5 个测试全部通过。
- 与第 6 阶段合并定向 Vitest：6 个文件、34 个测试全部通过。
- 目标 ESLint：0 errors；仅保留 `AiForm.vue` 既有 required/default warning。
- 前端生产构建：成功（8891 modules transformed）；保留仓库既有动态导入、组件命名冲突、CSS 注释和 chunk size 警告。
- `git diff --check`：通过。

## 2026-08-11 增量验证

- P0：状态迁移白名单、expected-status 并发失败、MONEY scale/最小单位转换和审计脱敏。
- P0：发布态 offlineDraft 投影、AiCrudPage 自动保存/恢复、断网只存本地和显式重放确认。
- P1：新增 Flyway 字段防重复和占位符扫描；前端组件定向测试、ESLint、生产构建、差异检查。

## 2026-08-11 最终结果

- 后端定向测试：6 个测试类、30 个测试全部通过；覆盖状态迁移成功/冲突审计、MONEY、动作执行留存与脱敏、发布门禁和协议快照。
- 前端定向 Vitest：6 个测试文件、36 个测试全部通过；覆盖字段事件、事务动作响应、离线草稿/表单运行时、设计器协议与表单治理归一化。
- 目标 ESLint：0 errors。
- 前端生产构建：成功（8892 modules transformed，1m28s）；保留仓库既有组件命名冲突、动态/静态导入、CSS `//` 注释和大 chunk 提示。
- Flyway 静态检查：`V1.0.104` 无 `${...}` 占位符，迁移版本无重复；未连接真实 MySQL 执行迁移。
- `git diff --check`：通过。
- 跳过项：真实数据库/Redis/Flow/外部系统、弱网浏览器 E2E 和多标签页并发按约束未启动，需部署环境补验。
