# Lowcode Flow Status Column Consistency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Execute this plan task-by-task with validation checkpoints.

**Goal:** Ensure a low-code business-flow `flowStatus` field remains present in the published CRUD columns and visible runtime list after binding, saving, and application publishing.

**Architecture:** Reuse the existing managed-field conventions and add a single consistency gate around the existing flow-status ensure path. Persist/refresh the published runtime configuration when a managed field is found in draft but missing from the published snapshot, while keeping the frontend field-ref fallback for legacy page blocks. Expose failures instead of silently claiming a successful binding.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Jackson, Vue 3, Vitest, Maven, pnpm.

---

### Task 1: Establish failing regression coverage

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigServiceDesignPreviewTest.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowStatusFieldServiceTest.java`
- Modify: `forge-admin-ui/src/components/lowcode-builder/shared/__tests__/runtime-crud-props.spec.js`

- [ ] Add assertions for a published snapshot missing `flowStatus`, a legacy field without `managedBy`, and an old page `fieldRefs` list.
- [ ] Run the narrow tests and capture the current failure or missing coverage.

### Task 2: Fix backend managed-field recognition and publish drift

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowStatusFieldService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessFlowService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessService.java`

- [ ] Recognize legacy `flowStatus/flow_status/business_flow_status` fields and normalize managed metadata.
- [ ] Ensure binding/save paths surface synchronization failures and invoke the existing idempotent field/list repair.
- [ ] Make published runtime column assembly include a managed status column whenever draft metadata proves it exists, without overriding explicit visibility settings.
- [ ] Preserve tenant, permission, transaction, and typed DTO constraints.

### Task 3: Fix runtime list filtering and user feedback

**Files:**
- Modify: `forge-admin-ui/src/components/lowcode-builder/shared/runtime-crud-props.js`
- Modify: `forge-admin-ui/src/components/business-process-designer/ActionAndApprovalNodeConfig.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/runtime-crud-block-props.js`

- [ ] Keep managed-field detection compatible with legacy field catalog records.
- [ ] Ensure stale page `fieldRefs` cannot remove an active managed flow-status column.
- [ ] Replace silent automatic ensure failure with visible warning and a save/publish-blocking error state.

### Task 4: Validate and document

**Files:**
- Modify: `code-copilot/changes/lowcode-flow-status-column-consistency/tasks.md`
- Modify: `code-copilot/changes/lowcode-flow-status-column-consistency/test-spec.md`
- Create: `code-copilot/changes/lowcode-flow-status-column-consistency/execution-log.md`

- [ ] Run focused backend tests/compile and frontend Vitest/build with the required Node/JDK environment.
- [ ] Run `git diff --check` and record commands, results, and skipped real-environment checks.
- [ ] Review the final diff for unrelated files and verify no user changes were overwritten.
