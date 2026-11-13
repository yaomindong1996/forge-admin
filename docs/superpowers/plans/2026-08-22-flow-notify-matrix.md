# Flow Notify Matrix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the event-by-channel notification matrix for Forge flow models, including persisted configuration, channel discovery, multi-channel delivery, and the model editor UI.

**Architecture:** Keep the existing `FlowTaskNotifyEvent`/after-commit listener boundary. Store one guarded JSON column on `sys_flow_model`; resolve channels dynamically from built-in message channels and enabled collaboration connections; keep `NULL` configuration on the legacy todo behavior. Add result/cc template seeds and a Vue matrix editor that serializes only meaningful overrides.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Flowable, Flyway/MySQL, Vue 3, Naive UI, Vite/pnpm.

---

### Task 1: Verify and repair the existing backend notification implementation

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskNotifyListener.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java` only if compile diagnostics identify an event-variable issue.
- Test: existing flow module tests plus narrow Maven compile.

- [x] Run the flow plugin compile with the repository's JDK 17 and capture compiler diagnostics.
- [x] Fix signature/constructor/import/type errors without changing the legacy default path: `TASK_TODO` must still send WEB and connection-gated collaboration, while configured channels use independent idempotency keys.
- [x] Confirm result and cc events load model configuration by tenant, merge process variables into message params, and isolate channel failures.
- [x] Re-run the narrow compile and inspect the diff for unrelated files.

### Task 2: Add the dynamic channel API and model-editor client API

**Files:**
- Modify: `forge-admin-ui/src/api/flow.js`
- Verify: `forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowNotifyChannelController.java`

- [x] Add `getFlowNotifyChannels()` calling `GET /api/flow/notify-channels` beside the model APIs.
- [x] Compile the flow server so the controller's optional social service and response shape are verified.
- [x] Keep the response contract `{ channel, name, type, platforms, alwaysOn, costWarning }` and silent fallback to WEB/EMAIL/SMS when no collaboration connection is available.

### Task 3: Build the notification matrix UI in the flow model modal

**Files:**
- Modify: `forge-admin-ui/src/views/flow/model.vue`

- [x] Load channel metadata once when the model modal opens and show a three-row matrix for todo/result/cc, filtering channels according to the event capability (`cc` excludes SMS).
- [x] Keep todo WEB selected and disabled; result/cc remain optional; show the SMS cost-warning copy when selected; preserve collaboration labels returned by the API.
- [x] Parse `formData.notifyConfig` on edit and serialize only explicitly configured events before create/update. Send `null` for an untouched/empty matrix so legacy behavior remains reachable.
- [x] Reset matrix state on add/edit transitions, handle channel API failures without blocking model editing, and keep all existing event-webhook fields intact.
- [x] Run the frontend lint/build checks using the repository-local Node toolchain.

### Task 4: Seed result and cc collaboration templates

**Files:**
- Create: `forge-server/db/mysql/migration/V1.0.129__flow_result_cc_card_templates.sql`

- [x] Insert `FLOW_RESULT_CARD`, `FLOW_RESULT_CARD_WECOM`, `FLOW_CC_CARD`, and `FLOW_CC_CARD_WECOM` with explicit columns, tenant `1`, `NOT EXISTS` guards, and Flyway-safe placeholder construction.
- [x] Keep generic templates enabled and platform-specific templates disabled by default, matching the existing todo-card seed behavior.
- [x] Run `git diff --check` and static scans for tenant `0`, unescaped `${`, duplicate version numbers, and missing duplicate guards.

### Task 5: Incremental verification and handoff

**Files:**
- Modify: `code-copilot/changes/flow-notify-matrix/execution-log.md` (create if absent)

- [x] Read `code-copilot/rules/automated-testing-standard.md`, reuse this change's existing artifacts, and append commands/results/warnings/skips/service-cleanup notes.
- [x] Run the flow plugin and flow-server compiles, frontend lint/build, migration static checks, and focused unit tests where available.
- [x] Review the final diff/status without staging or resetting unrelated user changes, then report any environment-limited checks honestly.
