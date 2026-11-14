# Lowcode Platform Capabilities Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐应用管理低代码平台能力，使采购仓储模块可以用 0 代码完成可上线业务闭环，而不是只完成页面演示。

**Architecture:** 以现有应用中心、动态 CRUD、Flowable 绑定、触发器、公式和主子表为基础，新增“动作编排引擎”和“库存台账引擎”两个平台能力。按钮、触发器、流程回调统一调用动作引擎；采购入库、出库、调拨统一调用库存台账引擎，保证幂等、锁库存和库存流水一致。

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, MySQL 8, Vue 3.5, Naive UI, Flowable, existing `forge-plugin-generator` dynamic CRUD runtime.

---

## Scope

本计划覆盖采购仓储完整落地所需的平台级能力：

1. 业务动作编排：支持按钮动作、弹窗表单、字段映射、多步骤事务、动作日志。
2. 主子表和选择器增强：支持“选择物料/供应商/项目”弹窗、批量写入子表、字段自动带入。
3. 流程回调动作：支持流程通过/驳回后执行配置动作。
4. 库存台账引擎：支持入库、出库、调拨、锁定、释放、幂等、防负库存。
5. 提醒和价格历史：支持金额分层提醒、逾期提醒、供应商报价版本。

本计划不把采购、仓库、供应商、物料写成固定业务代码；采购仓储业务对象仍由应用管理建模，平台只补通用能力。

## Existing Capabilities To Reuse

- Dynamic CRUD runtime: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java`
- Business object actions: `BusinessObjectActionService.java`
- Business triggers: `BusinessTriggerExecutor.java`, `BusinessTriggerSchedulerService.java`
- Flow binding/runtime: `BusinessFlowService.java`
- Runtime pages: `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
- Action designer: `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue`
- Relation/form designer: `BusinessRelationDesigner.vue`, `BusinessFormDesigner.vue`
- Child table runtime: `forge-admin-ui/src/components/page-templates/ChildTableEditor.vue`

## Target Procurement/Warehouse Modeling

采购仓储模块建议使用以下低代码对象：

- `material`: 物料
- `supplier`: 供应商
- `supplier_material_price`: 供应商物料报价
- `purchase_order`: 采购单
- `purchase_order_item`: 采购明细
- `purchase_quote`: 采购供应商报价/比价单
- `warehouse`: 仓库
- `stock_out_order`: 出库单
- `stock_out_item`: 出库明细
- `stock_transfer_order`: 调拨单
- `stock_transfer_item`: 调拨明细

库存余额、库存流水、库存锁定建议由平台库存引擎维护，不放到普通低代码对象里手工维护。

---

## Phase 1: Business Action Orchestration

### Task 1: Add Action Execution API

**Files:**
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessActionExecuteDTO.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessActionExecuteResultVO.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessActionExecutionController.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionExecutionService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectActionService.java`
- Create SQL: `forge-server/db/mysql/migration/V1.0.xx__add_business_action_execution_log.sql`

**Behavior:**
- Add `POST /ai/business/action/execute`.
- Request contains `objectCode`, `recordId`, `actionCode`, `formData`, `context`.
- Action config supports these step types:
  - `UPDATE_FIELD`: update current or target record fields.
  - `CREATE_RECORD`: create related record.
  - `START_FLOW`: start main or specified flow.
  - `SEND_MESSAGE`: send internal message.
  - `INVENTORY_OPERATION`: call stock engine after Phase 4.
- All steps run in one transaction except message sending, which records message intent after commit or logs failure without rolling back data.
- Write execution log for every run.

**Acceptance:**
- A row button can open a confirmation or action form and call the action API.
- A configured action can update current record status and create one related record in one transaction.
- Failed action writes a failed log with error message.

**Verification:**
- Backend compile: `cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
- Backend test: add `BusinessActionExecutionServiceTest` covering successful multi-step action and rollback.

### Task 2: Extend Frontend Action Runtime

**Files:**
- Modify: `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/BusinessActionDesigner.vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`
- Modify: `forge-admin-ui/src/api/business-app.js`

**Behavior:**
- Add action type `COMMAND`.
- For `COMMAND`, runtime opens action form when `actionConfig.formSchema` exists.
- Submit payload to `/ai/business/action/execute`.
- Support `successBehavior`: `refreshList`, `refreshDetail`, `closeModal`, `goBack`.
- Reuse existing loading key logic so duplicate clicks are blocked.

**Acceptance:**
- Can configure “确认交付”“付款登记”“出库申请提交” as action buttons.
- Button displays global or row-level loading while executing.
- Success refreshes list/detail without manual reload.

**Verification:**
- Frontend build: `cd forge-admin-ui && pnpm build`
- Manual UI: configure a `COMMAND` action on one object and execute it from row/detail.

---

## Phase 2: Selector And Master-Detail Runtime

### Task 3: Add Generic Record Selector Component

**Files:**
- Create: `forge-admin-ui/src/components/ai-form/AiRecordSelectorModal.vue`
- Create: `forge-admin-ui/src/components/ai-form/record-selector-utils.js`
- Modify: `forge-admin-ui/src/components/page-templates/ChildTableEditor.vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

**Behavior:**
- Selector config contains `targetConfigKey`, `searchFields`, `displayColumns`, `valueField`, `mapping`.
- Supports single select and multi select.
- Multi select can append rows into child table.
- Mapping example:
  - `materialId <- id`
  - `materialCode <- materialCode`
  - `materialName <- materialName`
  - `specification <- specification`
  - `unit <- unit`
  - `price <- referencePrice`

**Acceptance:**
- Purchase detail can click “添加物料”, select multiple materials, and append child rows.
- Supplier material maintenance can select multiple materials and append price rows.
- Outbound order can select current warehouse stock records and carry available quantity.

**Verification:**
- Frontend build: `cd forge-admin-ui && pnpm build`
- Manual UI: configure selector in form designer and verify child rows are created with mapped fields.

### Task 4: Harden Child Table Persistence

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java`
- Modify: `forge-admin-ui/src/components/page-templates/ChildTableEditor.vue`

**Behavior:**
- Add child row stable key support.
- On update, support `replace` mode and `merge` mode.
- `replace` mode keeps current behavior: delete by parent key and reinsert.
- `merge` mode updates rows with `id`, inserts rows without `id`, deletes rows marked `_deleted=true`.
- Add child row validation for required fields and numeric min/max.

**Acceptance:**
- Purchase order can preserve child item IDs across edit.
- Outbound quantity cannot exceed available quantity in the UI.
- Backend still validates submitted child rows so direct API calls cannot bypass rules.

**Verification:**
- Backend test: create master-detail record, update one child, delete one child, insert one child.
- Frontend build: `cd forge-admin-ui && pnpm build`

---

## Phase 3: Flow Callback Action Binding

### Task 5: Let Flow Result Execute Configured Actions

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessTriggerExecutor.java`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowBindingPanel.vue`
- Modify: `forge-admin-ui/src/views/app-center/components/designer/BusinessFlowAppConfigPanel.vue`

**Behavior:**
- Flow binding adds callback action slots:
  - `onApprovedActionCode`
  - `onRejectedActionCode`
  - `onCanceledActionCode`
- After process result is saved, `BusinessFlowService` calls `BusinessActionExecutionService` with the business record context.
- If callback action fails, flow state remains completed but action log records failure and readiness panel shows callback error.

**Acceptance:**
- Purchase approval approved: update purchase status and call inventory inbound action.
- Stock out approval rejected: release stock lock.
- Stock out approval approved: commit stock outbound.

**Verification:**
- Backend test: simulate flow approved event and assert action execution log exists.
- Manual UI: bind callback action in app center and verify flow completion triggers it.

---

## Phase 4: Inventory Ledger Engine

### Task 6: Add Stock Tables And Domain Service

**Files:**
- Create SQL: `forge-server/db/mysql/migration/V1.0.xx__add_business_stock_engine.sql`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessStockBalance.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessStockLedger.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessStockLock.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessStockService.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessStockOperationDTO.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessStockOperationResultVO.java`

**Tables:**
- `ai_business_stock_balance`: `tenant_id`, `warehouse_id`, `material_id`, `batch_no`, `quantity`, `locked_quantity`, `available_quantity`, unique key on tenant/warehouse/material/batch.
- `ai_business_stock_ledger`: immutable ledger with operation type `IN/OUT/TRANSFER_IN/TRANSFER_OUT/ADJUST/LOCK/RELEASE/COMMIT_LOCK`.
- `ai_business_stock_lock`: lock records for approval-in-progress outbound/transfer.

**Behavior:**
- `inbound`: increase balance and write `IN` ledger.
- `lockOutbound`: increase locked quantity, prevent available quantity below zero.
- `releaseLock`: reduce locked quantity and mark lock released.
- `commitOutbound`: reduce quantity and locked quantity, write `OUT` ledger.
- `transfer`: source outbound + target inbound in one transaction.
- Every operation requires idempotency key: `sourceObjectCode + sourceRecordId + operationType`.

**Acceptance:**
- Purchase approved creates inbound ledger once even if callback retries.
- Outbound submitted locks stock.
- Outbound rejected releases stock.
- Outbound approved deducts stock.
- Transfer approved deducts source warehouse and adds target warehouse atomically.
- Negative stock is impossible.

**Verification:**
- Backend tests:
  - inbound idempotency
  - lock prevents over-issue
  - release lock
  - commit lock
  - transfer atomic rollback

### Task 7: Connect Stock Engine To Lowcode Actions

**Files:**
- Modify: `BusinessActionExecutionService.java`
- Modify: `BusinessActionDesigner.vue`
- Modify: `TriggerActionConfigPanel.vue`

**Behavior:**
- Add `INVENTORY_OPERATION` action step.
- Config supports:
  - `operationType`: `INBOUND`, `LOCK_OUTBOUND`, `RELEASE_LOCK`, `COMMIT_OUTBOUND`, `TRANSFER`
  - `warehouseField`
  - `targetWarehouseField`
  - `itemsPath`
  - `materialField`
  - `quantityField`
  - `batchField`
- Runtime reads current record and child rows from dynamic CRUD detail, then calls `BusinessStockService`.

**Acceptance:**
- Purchase order callback can map purchase items into stock inbound.
- Stock out order submit action can lock quantities from item rows.
- Stock transfer approved action can transfer all item rows.

**Verification:**
- Backend integration test using dynamic CRUD record with child rows.

---

## Phase 5: Reminder Rules And Price History

### Task 8: Add Rule-Based Reminder Profiles

**Files:**
- Create SQL: `forge-server/db/mysql/migration/V1.0.xx__add_business_reminder_profile.sql`
- Create: `BusinessReminderProfileService.java`
- Modify: `BusinessTriggerSchedulerService.java`
- Create frontend component: `forge-admin-ui/src/views/app-center/components/designer/BusinessReminderRulePanel.vue`

**Behavior:**
- Reminder profile supports amount tiers:
  - `amountGte`
  - `daysBefore`
  - `receiverRule`
  - `templateCode`
- Overdue rules support:
  - `overdueDays`
  - `rowClass`
  - `receiverRule`
  - `repeatIntervalDays`
- Scheduler resolves due records, applies tier rule, dedupes per day.

**Acceptance:**
- 回款/付款可配置“金额 >= 100 万提前 60 天提醒”。
- 逾期 3 天列表标黄，逾期 7 天列表标红并提醒指定角色。

**Verification:**
- Backend test: amount tier selection and duplicate reminder prevention.
- Frontend build: `cd forge-admin-ui && pnpm build`

### Task 9: Add Supplier Price Version Support

**Files:**
- Create SQL: `forge-server/db/mysql/migration/V1.0.xx__add_supplier_price_version_template.sql`
- Add app-center template metadata in existing lowcode seed script for supplier price object.
- Modify selector mapping docs/examples in `BusinessFormDesigner.vue`.

**Behavior:**
- Supplier material price object uses:
  - `supplierId`
  - `materialId`
  - `price`
  - `effectiveStart`
  - `effectiveEnd`
  - `previousPrice`
  - `changeReason`
  - `status`
- “编辑物料报价” action creates a new price version and closes old active version.
- Material detail can show active supplier quotations and recent price changes via relation expand panels.

**Acceptance:**
- Editing price does not overwrite history.
- Material detail can display current quarter quote and previous quote.

---

## Delivery Order

1. Phase 1 first: no action engine, procurement/warehouse cannot be truly 0-code.
2. Phase 2 second: purchase detail and supplier material maintenance rely on selector + child table.
3. Phase 3 third: approval result must trigger real business effects.
4. Phase 4 fourth: inventory correctness is the core production risk.
5. Phase 5 fifth: reminders and price history improve completeness after core flow is stable.

## Suggested Change Split

- `lowcode-action-orchestration`: Phase 1.
- `lowcode-selector-master-detail`: Phase 2.
- `lowcode-flow-callback-actions`: Phase 3.
- `lowcode-stock-ledger-engine`: Phase 4.
- `lowcode-reminder-price-history`: Phase 5.

## End-To-End Acceptance Scenario

1. In app center create material, supplier, warehouse, purchase order, stock out order, transfer order objects.
2. Purchase order form uses selector to add material rows.
3. Purchase order submit starts approval.
4. Approval approved runs inbound action.
5. Warehouse detail shows updated stock balance and inbound ledger.
6. Stock out order submit locks stock.
7. Stock out approval rejected releases lock.
8. Stock out approval approved deducts stock and writes outbound ledger.
9. Transfer approval moves stock from source warehouse to target warehouse.
10. Supplier price edit creates a new price version and preserves old price history.
11. Due reminder job sends only one reminder per rule per record per day.

## Risk Controls

- Inventory operations must be idempotent.
- Inventory operations must use row-level lock or conditional update to prevent concurrent negative stock.
- Flow callback failure must be visible in action logs and readiness panel.
- Action executor must validate action permission before execution.
- Dynamic CRUD child rows must be reloaded from backend before inventory operation to avoid trusting stale frontend payload.
- All SQL migrations must use duplicate protection and `tenant_id = 1` for built-in data.

## Validation Commands

Backend:

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test
```

Frontend:

```bash
cd forge-admin-ui
pnpm build
```

Full build:

```bash
cd forge-server
mvn clean install -DskipTests
```

