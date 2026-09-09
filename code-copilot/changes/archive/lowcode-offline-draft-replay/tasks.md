# 任务清单

## 阶段 A：本地草稿协议

- [x] A1 新增通用 `offline-draft-runtime.js`，实现受控键、JSON 清洗、大小/数量上限、保存/读取/删除。
- [x] A2 支持草稿发布版本、schemaHash、recordId/baseRecordVersion 元数据和状态转换。

## 阶段 B：重放与冲突

- [x] B1 增加幂等键唯一的 replay log，支持追加、成功、失败和停止。
- [x] B2 增加发布版本/记录版本/记录可用性冲突检测；冲突只标记，不自动执行。
- [x] B3 提供显式 `replayDraft`，要求调用方注入 `loadCurrent` 和 `execute`。

## 阶段 C：状态、金额与审计治理

- [x] C1 增加 `TRANSITION_STATUS` 结构化步骤、发布白名单和同 SQL expected-status 条件更新。
- [x] C2 增加 `MONEY` 输入类型、scale/precision 严格校验、最小货币单位转换和禁止静默舍入。
- [x] C3 为动作日志增加结构化状态审计字段、脱敏变更摘要和 retentionUntil，保留专用归档边界。

## 阶段 D：运行态草稿接入

- [x] D1 发布态 governance 配置投影到 AiCrudPage，形成安全 namespace 和版本/schemaHash 元数据。
- [x] D2 AiCrudPage 自动保存/恢复草稿；断网提交仅保存本地，配置 replayActionCode 时追加受控重放意图。
- [x] D3 在线恢复提供显式校验与用户确认重放入口，冲突时只展示摘要，不自动覆盖。

## 阶段 E：测试与验证

- [x] E1 补充状态迁移、MONEY 精度、审计脱敏、离线恢复/冲突/显式重放测试。
- [x] E2 更新 Spec、测试规格、执行日志和决策记录。
- [x] E3 执行目标 Java/前端定向测试、构建、静态检查和 `git diff --check`。
