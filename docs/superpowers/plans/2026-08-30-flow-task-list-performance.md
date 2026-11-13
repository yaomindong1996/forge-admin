# Flow Task List Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Execute this plan task-by-task with checkpoints.

**Goal:** Remove per-row database/Flowable lookups from todo/done list requests while preserving user, business summary, and tenant behavior.

**Architecture:** Keep the list result contract unchanged. Enrich task rows from SQL joins/batch queries only, read business parameters from the persisted business-flow snapshot when available, and use a single batch user-name lookup for rows that still need a display fallback. Correct candidate-group filtering and add indexes through Flyway.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL 8, Flowable 7, Java 17.

---

### Task 1: Define the change contract

**Files:**
- Create: `code-copilot/changes/flow-task-list-performance/spec.md`
- Create: `code-copilot/changes/flow-task-list-performance/tasks.md`
- Create: `code-copilot/changes/flow-task-list-performance/execution-log.md`

- [ ] Record the N+1 sources, candidate-group correctness fix, explicit tenant filtering, and migration/index requirements.
- [ ] Record verification commands and the fact that no service is started during this change.

### Task 2: Rewrite flow task list enrichment

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/resources/mapper/FlowTaskMapper.xml`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/mapper/FlowTaskMapper.java`

- [ ] Select only list columns and join `sys_user` for start/assignee names.
- [ ] Replace per-row `runtimeService.getVariable` with a single batch lookup against persisted business-flow snapshots, with no runtime lookup for done rows.
- [ ] Add a batch fallback for unresolved user IDs and remove organization/post lookups from list rendering.
- [ ] Require the current user’s candidate groups in todo filtering and include tenant criteria in todo/done SQL.

### Task 3: Add Flyway indexes

**Files:**
- Create: `forge-server/db/mysql/migration/V1.0.135__optimize_flow_task_list_queries.sql`

- [ ] Add guarded composite indexes for tenant/assignee/status/create_time and tenant/assignee/status/complete_time.
- [ ] Add guarded indexes for task start user and candidate group lookup where supported by the revised query.

### Task 4: Verify

**Files:**
- Modify: `code-copilot/changes/flow-task-list-performance/execution-log.md`

- [ ] Run `git diff --check`.
- [ ] Run the narrow flow-module Maven compile and SQL contract checks.
- [ ] Report skipped runtime verification because services/database are not started.
