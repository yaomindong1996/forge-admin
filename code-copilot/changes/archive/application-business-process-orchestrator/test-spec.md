# 测试规格 — 应用级业务流程编排器

> change: `application-business-process-orchestrator`
> status: active
> baseline: 2026-08-03

## 1. 验证目标与边界

本测试规格覆盖 Spec 功能 1-40，采用“协议与控制面 → 持久化运行 → Flowable 等待恢复 → 应用发布 → 迁移收口”的增量顺序。自动化验证可以执行静态检查、单元测试、模块编译和前端构建；真实 MySQL Flyway、Admin/Flow 服务启动、真实流程运行和迁移演练由用户在目标环境执行，未回填结果前不得表述为通过。

P0 阻断项：协议污染 BPMN、多个开始节点、环或无结束路径、跨租户/跨应用引用、重复审批、无可信身份回退管理员、自由 URL/Secret、回调重复消费、已发布版本原地修改、旧配置物理删除。

P1 必验项：CRUD 与草稿 hash 冲突、节点配置完整性、条件分支、动作幂等、重试/取消、应用发布与回滚、运行时间线、迁移预览和旧入口停写。

## 2. 冻结的 businessProcessJson 1.0 协议

### 2.1 根协议

- `schemaVersion`：首版固定为字符串 `1.0`。
- `processCode`：应用内稳定编码，创建后不可修改。
- `subject`：固定主业务对象；`objectId/objectVersionId/recordId` 均按字符串传输。
- `nodes`：只允许注册表节点，首版为 `START_MANUAL/START_EVENT/START_SCHEDULE/CONDITION/ACTION/APPROVAL/SUB_PROCESS/END`。
- `edges`：`source/target/sourcePort` 均引用节点注册表；条件分支最多一个 `isDefault=true`。
- `policies`：冻结审批并发、重试、调用深度和执行身份策略，画布不得覆盖可信身份。
- `dependencies`：只保存对象、Flowable 模型、表单、消息模板、业务动作、受治理能力和同应用子流程的稳定引用；发布时解析为不可变版本快照。
- 禁止键：`url/webhook/secret/token/password/privateKey/authorization/cookie/javaClass/sql/script/spel`（大小写和嵌套路径均检查）。

### 2.2 手动提交审批

```json
{
  "schemaVersion": "1.0",
  "processCode": "purchase_submit_approval",
  "subject": {
    "objectId": "1900000000000001001",
    "objectCode": "sample_purchase_order",
    "objectVersionId": null,
    "recordIdSource": "RUNTIME_RECORD"
  },
  "nodes": [
    {
      "id": "start_manual",
      "type": "START_MANUAL",
      "name": "提交审批",
      "config": {
        "positions": ["ROW", "DETAIL"],
        "permission": "ai:businessProcess:start",
        "confirmText": "确认提交当前采购单审批？",
        "visibleCondition": {"operator": "AND", "rules": [{"source": "record", "field": "status", "operator": "EQ", "value": "DRAFT"}]}
      }
    },
    {
      "id": "approval_purchase",
      "type": "APPROVAL",
      "name": "采购审批",
      "ports": ["APPROVED", "REJECTED", "CANCELED", "FAILED"],
      "config": {
        "flowModelKey": "sample_purchase_order_approval",
        "versionPolicy": "PINNED_AT_APPLICATION_PUBLISH",
        "titleTemplate": "采购审批-{orderNo}",
        "formAsset": {"formKey": "sample_purchase_order_approval_form", "formMode": "BUSINESS_CODE_FORM", "providerKey": "samplePurchaseOrder"},
        "variableMappings": [
          {"field": "id", "variable": "recordId"},
          {"field": "orderNo", "variable": "orderNo"},
          {"field": "title", "variable": "title"},
          {"field": "amountCent", "variable": "amountCent"}
        ]
      }
    },
    {"id": "mark_approved", "type": "ACTION", "name": "更新为已通过", "config": {"actionType": "UPDATE_RECORD", "objectCode": "sample_purchase_order", "fieldMappings": [{"field": "status", "valueSource": "CONSTANT", "value": "APPROVED"}]}},
    {"id": "mark_rejected", "type": "ACTION", "name": "更新为待修改", "config": {"actionType": "UPDATE_RECORD", "objectCode": "sample_purchase_order", "fieldMappings": [{"field": "status", "valueSource": "CONSTANT", "value": "NEED_MODIFY"}]}},
    {"id": "mark_canceled", "type": "ACTION", "name": "更新为已取消", "config": {"actionType": "UPDATE_RECORD", "objectCode": "sample_purchase_order", "fieldMappings": [{"field": "status", "valueSource": "CONSTANT", "value": "CANCELED"}]}},
    {"id": "end_success", "type": "END", "name": "审批完成", "config": {"result": "SUCCESS"}},
    {"id": "end_rejected", "type": "END", "name": "等待修改", "config": {"result": "REJECTED"}},
    {"id": "end_canceled", "type": "END", "name": "流程取消", "config": {"result": "CANCELED"}},
    {"id": "end_failed", "type": "END", "name": "启动失败", "config": {"result": "FAILED"}}
  ],
  "edges": [
    {"id": "e1", "source": "start_manual", "target": "approval_purchase", "sourcePort": "NEXT"},
    {"id": "e2", "source": "approval_purchase", "target": "mark_approved", "sourcePort": "APPROVED"},
    {"id": "e3", "source": "approval_purchase", "target": "mark_rejected", "sourcePort": "REJECTED"},
    {"id": "e4", "source": "approval_purchase", "target": "mark_canceled", "sourcePort": "CANCELED"},
    {"id": "e5", "source": "approval_purchase", "target": "end_failed", "sourcePort": "FAILED"},
    {"id": "e6", "source": "mark_approved", "target": "end_success", "sourcePort": "NEXT"},
    {"id": "e7", "source": "mark_rejected", "target": "end_rejected", "sourcePort": "NEXT"},
    {"id": "e8", "source": "mark_canceled", "target": "end_canceled", "sourcePort": "NEXT"}
  ],
  "policies": {"approvalConcurrency": "ONE_ACTIVE_PER_BUSINESS_KEY", "maxSubProcessDepth": 5, "retry": {"mode": "LIMITED", "maxAttempts": 3, "backoffSeconds": [30, 120, 600]}},
  "dependencies": {"objects": ["sample_purchase_order"], "flowModels": ["sample_purchase_order_approval"], "formAssets": ["sample_purchase_order_approval_form"], "businessActions": [], "messageTemplates": [], "capabilities": [], "subProcesses": []}
}
```

### 2.3 记录新增后自动审批

```json
{
  "schemaVersion": "1.0",
  "processCode": "purchase_created_auto_approval",
  "subject": {"objectId": "1900000000000001001", "objectCode": "sample_purchase_order", "objectVersionId": null, "recordIdSource": "EVENT_RECORD"},
  "nodes": [
    {"id": "start_created", "type": "START_EVENT", "name": "采购单新增", "config": {"eventType": "RECORD_CREATED", "condition": {"operator": "AND", "rules": [{"source": "record", "field": "autoSubmit", "operator": "EQ", "value": true}]}}},
    {"id": "approval_purchase", "type": "APPROVAL", "name": "采购审批", "ports": ["APPROVED", "REJECTED", "CANCELED", "FAILED"], "config": {"flowModelKey": "sample_purchase_order_approval", "versionPolicy": "PINNED_AT_APPLICATION_PUBLISH", "titleTemplate": "采购审批-{orderNo}", "formAsset": {"formKey": "sample_purchase_order_approval_form", "formMode": "BUSINESS_CODE_FORM", "providerKey": "samplePurchaseOrder"}, "variableMappings": [{"field": "id", "variable": "recordId"}, {"field": "orderNo", "variable": "orderNo"}]}},
    {"id": "notify_approved", "type": "ACTION", "name": "通知申请人", "config": {"actionType": "SEND_MESSAGE", "messageTemplateCode": "purchase_approval_approved", "channels": ["WEB"], "recipientRule": {"type": "RECORD_FIELD", "field": "applicantId"}}},
    {"id": "end_success", "type": "END", "name": "完成", "config": {"result": "SUCCESS"}},
    {"id": "end_rejected", "type": "END", "name": "驳回", "config": {"result": "REJECTED"}},
    {"id": "end_canceled", "type": "END", "name": "取消", "config": {"result": "CANCELED"}},
    {"id": "end_failed", "type": "END", "name": "失败", "config": {"result": "FAILED"}}
  ],
  "edges": [
    {"id": "e1", "source": "start_created", "target": "approval_purchase", "sourcePort": "NEXT"},
    {"id": "e2", "source": "approval_purchase", "target": "notify_approved", "sourcePort": "APPROVED"},
    {"id": "e3", "source": "approval_purchase", "target": "end_rejected", "sourcePort": "REJECTED"},
    {"id": "e4", "source": "approval_purchase", "target": "end_canceled", "sourcePort": "CANCELED"},
    {"id": "e5", "source": "approval_purchase", "target": "end_failed", "sourcePort": "FAILED"},
    {"id": "e6", "source": "notify_approved", "target": "end_success", "sourcePort": "NEXT"}
  ],
  "policies": {"approvalConcurrency": "ONE_ACTIVE_PER_BUSINESS_KEY", "maxSubProcessDepth": 5, "retry": {"mode": "LIMITED", "maxAttempts": 3, "backoffSeconds": [30, 120, 600]}},
  "dependencies": {"objects": ["sample_purchase_order"], "flowModels": ["sample_purchase_order_approval"], "formAssets": ["sample_purchase_order_approval_form"], "businessActions": [], "messageTemplates": ["purchase_approval_approved"], "capabilities": [], "subProcesses": []}
}
```

### 2.4 定时分层提醒

```json
{
  "schemaVersion": "1.0",
  "processCode": "purchase_due_tiered_reminder",
  "subject": {"objectId": "1900000000000001001", "objectCode": "sample_purchase_order", "objectVersionId": null, "recordIdSource": "SCHEDULE_SCAN_RECORD"},
  "nodes": [
    {"id": "start_due", "type": "START_SCHEDULE", "name": "到期扫描", "config": {"dueField": "expectedArrivalDate", "lookAheadDays": 3, "lookBackDays": 7, "batchSize": 100, "minimumIntervalMinutes": 1440, "serviceActor": {"mode": "CONFIGURED_USER", "userConfigKey": "business.process.schedule.service-user"}}},
    {"id": "check_overdue", "type": "CONDITION", "name": "判断是否逾期", "ports": ["OVERDUE", "DUE_SOON"], "config": {"branches": [{"port": "OVERDUE", "condition": {"operator": "AND", "rules": [{"source": "context", "field": "daysUntilDue", "operator": "LT", "value": 0}]}}, {"port": "DUE_SOON", "isDefault": true}]}},
    {"id": "notify_overdue", "type": "ACTION", "name": "发送逾期提醒", "config": {"actionType": "SEND_MESSAGE", "messageTemplateCode": "purchase_overdue_notice", "channels": ["WEB", "EMAIL"], "recipientRule": {"type": "RECORD_FIELD", "field": "ownerId"}}},
    {"id": "notify_due", "type": "ACTION", "name": "发送到期提醒", "config": {"actionType": "SEND_MESSAGE", "messageTemplateCode": "purchase_due_notice", "channels": ["WEB"], "recipientRule": {"type": "RECORD_FIELD", "field": "ownerId"}}},
    {"id": "end_success", "type": "END", "name": "提醒完成", "config": {"result": "SUCCESS"}}
  ],
  "edges": [
    {"id": "e1", "source": "start_due", "target": "check_overdue", "sourcePort": "NEXT"},
    {"id": "e2", "source": "check_overdue", "target": "notify_overdue", "sourcePort": "OVERDUE"},
    {"id": "e3", "source": "check_overdue", "target": "notify_due", "sourcePort": "DUE_SOON", "isDefault": true},
    {"id": "e4", "source": "notify_overdue", "target": "end_success", "sourcePort": "NEXT"},
    {"id": "e5", "source": "notify_due", "target": "end_success", "sourcePort": "NEXT"}
  ],
  "policies": {"approvalConcurrency": "ONE_ACTIVE_PER_BUSINESS_KEY", "maxSubProcessDepth": 5, "retry": {"mode": "LIMITED", "maxAttempts": 3, "backoffSeconds": [60, 300, 1800]}},
  "dependencies": {"objects": ["sample_purchase_order"], "flowModels": [], "formAssets": [], "businessActions": [], "messageTemplates": ["purchase_overdue_notice", "purchase_due_notice"], "capabilities": [], "subProcesses": []}
}
```

## 3. 可信身份矩阵

| 触发来源 | actor | tenant | activeOrg | 可信来源 | 失败策略 |
|---|---|---|---|---|---|
| `MANUAL` | 当前登录普通用户 | 当前 Session 租户 | 当前 Session 组织 | Sa-Token `LoginUser` | 任一上下文缺失或无权限立即拒绝 |
| `EVENT` | 原业务操作人 | 事务事件中的已验证租户 | 原操作组织 | 事务完成后发布的服务端事件快照 | actor/tenant/org 不完整则不创建 run |
| `SCHEDULE` | 配置的受限普通服务用户，或记录字段唯一解析出的普通用户 | 扫描记录的权威租户 | 服务用户配置组织或记录权威组织 | 服务端配置 + 记录读取 | 无合法普通用户时失败关闭，禁止回退 admin |
| `PROCESS_CALLBACK` | 回调系统身份；后续人工责任仍引用原 run actor | 持久化 run/link 的租户 | 持久化 run 的组织 | 已验证 Flowable 结果事件 + `processInstanceId/businessKey` | 关联不唯一、跨租户、状态不匹配或结果已消费时拒绝/幂等返回 |
| `EXTERNAL` | 可信 USER delegation；纯服务身份只允许非人工责任动作 | Token 中的权威租户 | Token 中的权威组织 | Capability/OAuth 执行身份 | 请求 Header/Body 自报身份无效；审批无普通用户时失败关闭 |

所有来源进入数据库前在最小边界建立租户和数据权限上下文，并在 `finally` 恢复。画布只能选择身份策略引用，不能保存用户 Token、Secret 或任意 actor ID。

## 4. 状态机与 CAS 基线

### 4.1 Process Run

| 当前状态 | 允许下一状态 | CAS 条件 |
|---|---|---|
| `PENDING` | `RUNNING/FAILED/CANCELED` | `tenant_id + id + status=PENDING` |
| `RUNNING` | `WAITING/SUCCESS/FAILED/CANCELED` | `tenant_id + id + status=RUNNING + current_node_id` |
| `WAITING` | `RUNNING/FAILED/CANCELED` | `tenant_id + id + status=WAITING + current_node_id + correlation` |
| `FAILED` | `PENDING` | 仅 retryable、未超过次数、人工权限通过；增加 retry count |
| `SUCCESS/CANCELED` | 无 | 终态不可逆 |

### 4.2 Node Run

| 当前状态 | 允许下一状态 | 规则 |
|---|---|---|
| `PENDING` | `RUNNING/CANCELED` | 每个 `attemptNo` 只认领一次 |
| `RUNNING` | `SUCCESS/WAITING/FAILED` | 输出、错误和关联 ID 只通过 CAS 写入 |
| `WAITING` | `SUCCESS/FAILED/CANCELED` | 只由匹配 correlation 的恢复事件消费 |
| `FAILED` | 无 | 重试必须新增 `attemptNo`，禁止复活旧尝试 |
| `SUCCESS/CANCELED` | 无 | 终态不可逆 |

### 4.3 Approval Wait

审批启动成功并同时获得 `processInstanceId + businessKey` 后，节点才可由 `RUNNING` 进入 `WAITING`。回调必须匹配 `tenantId + runId + nodeId + processInstanceId + businessKey`，并将 `APPROVED/REJECTED/CANCELED/FAILED` 映射到同名出口；首次 CAS 消费后节点进入终态并恢复 run，重复或乱序回调不得再次调度后继节点。

## 5. 安全、权限与状态机审查

- 应用拥有流程，业务对象拥有记录；`businessKey` 固定 `<objectCode>:<recordId>`。
- 业务画布为 DAG，Flowable 内部仍可表达会签、驳回、退回和受控循环。
- 同一业务记录只允许一个活动审批子流程，启动前后均以稳定幂等键和 Flowable 侧 businessKey 防重。
- 手动、事件、定时、回调和外部入口分别使用可信身份，不允许 admin 兜底。
- 状态字段只能通过领域状态服务、审批状态映射或显式受控动作更新，结束节点无隐藏副作用。
- 外部调用只引用受治理能力/连接；自由 URL、Secret、任意 SQL/Java/脚本全部失败关闭。
- 版本与运行记录不可变/可审计；旧配置只读兼容和幂等迁移，不物理删除。
- 当前用户已明确授权按上述默认值进入开发；真实数据库、Flowable 和状态迁移验收仍保留人工执行门禁。

## 6. 功能覆盖矩阵

| 功能 | 自动化证据 | 环境证据 |
|---|---|---|
| 1-8 | CRUD/Schema/图校验单测、前端协议与画布测试 | 应用工作台创建、保存、问题定位 |
| 9-18 | 开始、条件、动作、结束、子流程执行器合同测试 | 事件、定时、手动三类真实记录 |
| 19-23 | Approval executor、回调重复/乱序/跨租户测试 | Flowable 通过、驳回、取消、状态修复 |
| 24-30 | run/node CAS、幂等、恢复、身份和日志脱敏测试 | 服务重启恢复、人工重试、权限拒绝 |
| 31-35 | 发布版本、快照、回滚、运行查询与 readiness 测试 | 应用发布/回滚后新旧实例并存 |
| 36-40 | migration preview/apply、旧入口守卫与样例协议测试 | 存量配置沙箱迁移和采购样例 E2E |

## 7. 增量验证命令

低成本检查：

```bash
git diff --check -- code-copilot/changes/application-business-process-orchestrator
rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.83__add_application_business_process.sql forge-server/db/migration/V1.0.84__add_application_business_process_resources.sql
```

后端定向测试与编译：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am \
  -Dtest=BusinessProcessSchemaValidatorTest,BusinessProcessOrchestratorTest,ApprovalProcessNodeExecutorTest,BusinessProcessMigrationServiceTest,GovernedActionStepExecutorTest test

JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

前端定向测试、Lint 与构建：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
cd forge-admin-ui
pnpm exec vitest run src/components/business-process-designer/__tests__/business-process-designer.spec.js
pnpm exec eslint src/components/business-process-designer src/views/app-center src/api/business-process.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

## 8. 真实环境验收清单

- [ ] MySQL 8 新库、存量库、重复执行 Flyway，检查 `forge_schema_history`、四张表、唯一索引和墓碑删除。
- [ ] Admin 与 Flow 服务启动装配，确认无 Mapper 重复、循环依赖和缺失 Bean。
- [ ] 手动提交、记录新增、定时分层提醒分别创建唯一 run。
- [ ] 审批通过、驳回、申请人修改重提、取消、终结和完成后状态修复。
- [ ] 服务在 `RUNNING/WAITING/FAILED` 时重启，扫描恢复不重复副作用。
- [ ] 应用发布、草稿修改、再次发布和回滚，运行实例固定原版本。
- [ ] 旧触发器/FLOW Binding/动作迁移预览、重复 apply、停写和旧实例继续办理。
- [ ] 跨租户、跨应用、无数据权限、伪造 actor、无服务发起人、回调重放和 Secret/URL 注入全部失败关闭。

## 9. 执行记录约定

每轮结果追加到 `execution-log.md`，写明实际命令、Tests run、构建结果、警告、跳过项和本轮服务 PID。未执行的真实 E2E、数据库迁移或浏览器验收不得标记通过。

## 10. Task 7 增量验证

- 控制面服务：创建默认草稿、编码不可变、结构不完整草稿可保存、跨应用主对象拒绝、草稿 hash 409、复制重建图 ID、运行/发布引用删除门禁。
- API 合同：独立加解密命名空间、`pageNum/pageSize`、CRUD/设计/校验/启停/删除权限注解。
- 校验上下文：应用对象和字段、发布动作快照、真实权限资源、已绑定且已部署的 Flowable 模型、表单/消息/同应用已发布子流程；能力桥接继续失败关闭。
- 数据库静态门禁：`V1.0.85` 使用 `tenant_id=1`、显式列、`NOT EXISTS` 和既有运行权限继承；无 Flyway `${...}` 占位符和 `tenant_id=0`。
- 必跑命令：Task 6/7 的 5 个定向测试类、三份 Mapper XML `xmllint`、目标差异 `git diff --check`、`forge-admin-server -am compile -DskipTests`。
- 环境门禁：真实 Flyway、权限继承查询、Flowable 已发布/未发布模型响应和加密 HTTP API 留待 Task 19；未执行前不标记通过。

## 11. Task 14 增量验证

- 协议合同：默认草稿为 `START_MANUAL -> END`；节点、连线、端口和依赖稳定排序；所有 ID 保持字符串，数字 ID 和 BPMN `flowJson` 失败关闭。
- BPMN 隔离：`convertJsonToBpmn` 显式拒绝 `businessProcessJson`，既有 JSON/BPMN roundtrip 和转换测试不得回退。
- 图与编辑合同：覆盖多个开始节点、悬空边、环、八类业务节点注册表、审批四结果出口、条件双分支、节点插入/复制/删除、撤销重做、dirty 基线和深克隆导出。
- 画布合同：`BusinessProcessCanvas` 通过只读布局适配层复用 `FlowCanvas/EdgeLayer/layoutFlow`；持久化节点不写入 `nodeType/bpmnElementId`，现有 `FlowCanvas` 无需修改。
- 必跑命令：业务画布测试与既有 converter/canvas 5 个测试文件、业务画布与转换器定向 ESLint、目标差异 `git diff --check`、前端生产构建。
- 环境门禁：Task 14 尚未接入应用路由和完整节点配置，不启动 Vite/浏览器做伪 E2E；Task 15-16 完成后再执行可视化操作、离开确认和应用工作台验收。

## 12. Task 15 增量验证

- 节点渲染：业务节点卡片与结果出口只读取业务注册表，不写入或依赖 BPMN 元素元数据；审批固定显示 `APPROVED/REJECTED/CANCELED/FAILED`。
- 结构化配置：触发方式、记录事件、日期扫描、条件分支、记录动作、消息、业务动作、受治理能力、审批模型、表单资产和同应用子流程全部使用选择/字段映射；普通路径无高级文本协议、任意表达式、数据库语句或自由目标地址输入。
- 审批边界：审批模型只从已发布/已部署目录选择；点击后异步嵌入真实 `flow/design.vue`，保存、部署或关闭后请求刷新模型摘要，业务画布不保存审批人、会签、驳回和节点字段权限。
- 依赖合同：节点配置变更自动重建对象、Flowable 模型、表单、消息模板、业务动作、能力和子流程依赖数组；删除/改型节点后不保留失效的未使用依赖。
- 草稿交互：覆盖防抖自动保存、显式检查/保存、dirty 事件、服务端 hash 冲突提示、刷新入口、浏览器离开保护、问题定位及撤销重做；保存成功由容器状态回写基线。
- 必跑命令：两份业务流程设计器测试、Task 14 的 converter/canvas 回归、`src/components/business-process-designer` ESLint、目标差异 `git diff --check` 和生产构建。
- 环境门禁：Task 15 仍是独立组件，尚未接入应用工作台路由和真实控制面 API；浏览器页面操作、加密草稿 CAS、目录刷新和路由离开确认留待 Task 16 一并验收。

## 13. Task 16 增量验证

- 控制面 API：分页、详情、创建、复制、更新、设计草稿、Schema CAS、校验、启停和逻辑删除全部走 `/ai/business/process` 加密请求；Schema 保存必须携带服务端 `draftSchemaHash`，前端稳定 hash 只用于判断并发保存期间是否又有本地修改。
- 应用面板：覆盖应用内分页搜索、新建时选择当前应用对象、复制、全屏设计、启停和逻辑删除；流程筛选写入应用路由 query，设计页 `returnTo` 返回后保留原应用分区和筛选状态。
- 全屏设计页：覆盖字符串 ID、业务对象字段/动作、已发布 Flowable 模型、表单资产、消息模板和同应用已发布子流程目录；受治理能力桥接与定时服务账号目录未交付时保持空目录并失败关闭。
- 保存与校验：覆盖防抖/显式保存映射到服务端 CAS、保存期间再次编辑的队列保存、HTTP 409 冲突、脏草稿先保存后服务端校验、关闭真实 Flowable 设计器后刷新模型和表单目录。
- 离开保护：浏览器刷新继续由 `BusinessProcessDesigner` 的 `beforeunload` 保护；应用路由跳转由全屏页 `onBeforeRouteLeave` 二次确认，返回路径只接受本地绝对路径。
- 未交付边界：运行记录与迁移预览入口显示为“待接入”并禁用，不定义 Task 13/17 尚未实现的前端 API 地址。
- 必跑命令：Task 16 API/工作台两份测试、Task 14/15 的业务画布与 BPMN 转换回归、目标 ESLint、目标文件空白检查和生产构建。
- 浏览器验证：用 Playwright 启动临时 Vite，拦截控制面数据但执行真实应用路由和组件；验证工作台、全屏画布、字符串 ID、服务端 CAS、服务端校验、筛选返回和未保存离开确认，浏览器 console/page error 必须为 0。
- 环境门禁：浏览器拦截只验证前端真实装配，不等价于加密 HTTP、真实 Flowable 或权限数据联调；Admin/Flow、MySQL/Flyway 和 Flowable 运行态继续留待 Task 19 目标环境验收。

## 14. Task 12 增量验证

- 不可变版本：相同 `applicationVersion + processId` 重试必须复用同一流程版本；已存在版本与候选草稿 hash 不同必须返回冲突，不允许覆盖或生成第二个版本。
- 候选冻结：应用运行单的 `processes[].draftSchemaHash` 是 `PROCESSES` 恢复边界；候选缺少任一所选流程摘要时必须拒绝发布，不得回退读取当前草稿；流程草稿随后变化时，已生成版本仍按候选 hash 复用并将当前设计投影保持为 `CHANGED`。
- 依赖快照：对象依赖固定 `objectId/designVersionId/versionNo/publishVersion`；审批依赖固定 `modelKey/modelId/modelVersion/processDefinitionId/deploymentId`；表单、业务动作、消息模板、能力和子流程只保存白名单稳定引用。
- 应用快照：候选快照包含 `processes[]`，正式快照包含结构化 `publishedProcessVersions[]`；`runtimeActions[]` 在 Task 12 保持稳定空字段，由 Task 13 从同一不可变版本编译。
- 发布恢复：`PROCESSES` 位于 `SNAPSHOT` 后并属于有副作用可恢复步骤；失败运行标记 `PARTIAL`，恢复使用原应用版本和原候选 hash。
- 回滚：读取来源快照的 `processVersionId` 恢复定义表 `published_version` 投影并清理未选投影；不更新 `ai_business_process_run`，运行中实例继续持有自己的 `processVersionId`。
- 就绪检查：发布前复用完整 Schema 校验，阻断对象未发布、字段失效、Flowable 未发布或缺少版本/流程定义/部署 ID、无结束路径、递归子流程、审批并发策略错误和手动权限缺失。
- 必跑命令：Task 12 九个定向测试类、两份 Mapper XML `xmllint`、目标差异 `git diff --check`、`forge-admin-server -am compile -DskipTests`。
- 全量基线：generator 全量测试若出现非本 Task 失败，必须记录具体类和计数，不得用定向通过掩盖；Task 12 修改类的定向回归必须全部通过。
- 环境门禁：不启动 Admin/Flow，不执行 Flyway 或真实数据库写入；真实 Flowable 部署响应、发布/回滚 HTTP 和新旧实例并存留待 Task 19。

## 15. 业务画布共享后继连线修复增量验证

- 复现场景：默认 `START_MANUAL -> END` 草稿在原连线上插入 `APPROVAL`，审批节点产生 `APPROVED/REJECTED/CANCELED/FAILED` 四条结果边并共享原结束节点。
- 布局合同：业务 DAG 的多入边节点在只读布局适配层中显式标记为汇合点；共享后继必须保持在开始/审批主轴，不得被放置到第一个结果分支的最左侧。
- 连线合同：同源同目标的多条结果边必须保留独立 edge ID 和 sourcePort，并使用与端口顺序一致的独立锚点；四条 SVG 路径和四个插入目标不得重叠。
- 协议边界：不得修改持久化 `businessProcessJson`、审批四结果出口或 BPMN `flowJson`；共享 `layoutFlow` 与 DingFlowDesigner 转换回归必须保持通过。
- 自动化验证：业务画布新增截图场景回归；组合执行两份业务设计器测试、BPMN roundtrip、JSON→BPMN、共享布局算法、FlowCanvas 和 layout-engine 测试；执行目标 ESLint、`git diff --check` 和生产构建。
- 浏览器验证：用受控 API 响应装配真实全屏设计路由，检查三节点中轴、5 条 SVG 边、四个结果插入目标和页面错误；开发态 UnoCSS 首次扫描若不稳定，必须记录环境告警，不能替代组件几何断言和生产构建结果。
- 环境门禁：不启动 Admin/Flow，不访问或修改真实 MySQL、Flyway、Flowable 运行态和用户草稿数据。

## 16. Flowable BPMN XML 解析兼容性增量验证

- 解析门禁：保留 DOCTYPE 和外部实体禁用；JAXP 1.5 外部访问属性作为可选兼容能力，不因旧 XML parser 不识别属性而阻断合法 BPMN 部署。
- 部署日志：BPMN `process id` 已经等于模型 Key 时只记录 debug，避免把正常的无需替换信息误认为部署异常。
- 必跑命令：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-flow -Dtest=BpmnXmlUtilsTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- 覆盖结果：重复连线归一化、条件连线保留和 DOCTYPE 拒绝回归；真实 Flowable 部署、数据库和服务启动仍留待 Task 19。

## 17. 业务流程设计器可用性修复增量验证

- 审批模型目录：Flowable 审批模型是租户级复用资产；目录直接返回当前可信租户下 `status=1`、`version>0`、`processDefinitionId/deploymentId` 完整的审批模型，不要求主对象或应用内其它对象预先存在 `FLOW Binding`。缺少可信租户、未发布、未部署、零版本和已失效模型不返回，Schema 发布校验使用同一判定结果。
- 租户隔离：Flow 服务即使处于 `@IgnoreTenant` 控制器边界，模型目录 SQL 也必须显式使用 `SessionHelper` 的可信租户 ID；缺少租户上下文时返回空目录且不访问 Mapper，不能回退默认租户或全租户查询。
- 条件规则：复用审批设计器规则模型，覆盖 AND/OR、等于/不等于、大小比较、区间、包含、为空、表达式生成和既有表达式反解析；业务流程默认入口只保存结构化条件，不展示高级表达式。
- 条件发布门禁：条件节点必须同时包含至少一个判断分支和一个唯一默认分支；只有默认分支、缺少默认分支、多个默认分支、默认分支带规则或判断分支无完整规则时均拒绝发布，并返回中文可操作提示。
- 分支图操作：新增第三分支自动生成唯一 port 和唯一 edge；重命名 port 同步 edge.sourcePort；删除分支删除对应 edge；设置默认分支同步 `isDefault`；每次操作后节点 ports、branches 和出边保持一一对应。
- 顺序语义：节点 ports 必须保留审批结果和条件 branches 的业务定义顺序，协议规范化不得按技术 port 字母排序；画布标签、锚点、连线和运行判断顺序保持一致。
- 多出口节点删除：条件和审批节点的所有出边指向同一公共后继时允许直接删除，全部入边接回公共后继并去重；无唯一公共后继时拒绝删除并展示明确中文原因。
- 画布布局：条件二至四分支和审批四结果共享后继时保持主轴居中；每条连线、锚点和插入按钮有独立几何位置；在任一分支插入节点后布局和图校验仍稳定。跨层直达边不得穿过中间卡片，绕行边不得与其它分支的汇合边共用同一线段。
- 中文与交互：卡片、抽屉和结果配置不显示 `APPROVED/REJECTED/CANCELED/FAILED/MATCHED/OTHERWISE/BRANCH_*`；可删除节点卡片有中文 tooltip/aria-label 的删除按钮，点击删除不触发节点选择并经过确认。
- 必跑命令：审批模型目录后端定向测试；业务流程设计器与 ConditionConfig 定向 Vitest；目标 ESLint；`git diff --check`；Node `v20.19.0` 生产构建。
- 浏览器验证：先执行 `scripts/with_server.py --help`，再以受控 API 装配真实全屏路由，依次新增条件、添加分支、配置规则、插入下游节点和卡片删除；截图检查连线与按钮，console/page error 必须为 0。
- 环境门禁：不启动真实 Admin/Flow、不写 MySQL/Flyway/Flowable；真实部署与应用发布结果继续作为 Task 19 联调项，未执行不得标记通过。

## 18. 审批终态恢复事务边界增量验证

- 流程回调方法必须建立本地事务，内部审批结果事件只能在该事务 `AFTER_COMMIT` 后恢复应用业务流程。
- `BusinessProcessOrchestrator.resumeApprovalResult` 必须使用 `REQUIRES_NEW`，不能继续加入已经完成业务状态回写的回调事务。
- 动作节点使用独立 `REQUIRES_NEW`；动作异常只回滚本节点数据写入，外层恢复事务仍要把业务流程运行记录和节点记录更新为 `FAILED`，并保存原始错误摘要。
- 监听器恢复异常必须保留完整堆栈、恢复租户上下文且不向已完成的 Flowable 回调反抛。
- 必跑定向类：`BusinessProcessApprovalResultListenerTest`、`BusinessProcessOrchestratorTest`、`BusinessProcessActionExecutorTest`；另执行相关文件 `git diff --check`。
- 遵循用户要求不执行全量 Maven/Vite 构建；真实 MySQL 保存点、Redis 回调和审批后动态字段更新留给运行环境复验。
