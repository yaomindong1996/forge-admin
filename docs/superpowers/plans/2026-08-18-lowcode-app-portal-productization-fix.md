# Lowcode App Portal Productization Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Make application visibility, page authorization, published portal slugs, and Forge workbench distribution enforceable and consistent with the existing productization Spec, while documenting unsupported external capabilities without unsafe placeholders.

**Architecture:** Centralize application-scope authorization in the generator backend and reuse it from application-center queries and runtime snapshot loading. Resolve runtime portals from published aliases/snapshots so design-time slug edits cannot change the live entry before publish. Persist workbench distribution as a server-side projection that the home page can query, and align the database delete-marker/index contract before adding new behavior.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus XML mappers, Sa-Token session context, MySQL/Flyway, Vue 3, Pinia, Naive UI, Vitest.

---

### Task 1: Map existing identity, role, department, and home-page protocols

**Files:**
- Inspect: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/**`
- Inspect: `forge-admin-ui/src/stores/**`, `forge-admin-ui/src/views/home/index.vue`
- Inspect: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationService.java`

- [x] **Step 1: Identify trusted session and relation query methods**

Run:

```bash
rg -n "SessionHelper\.(get|isAdmin)|roleIds|departmentIds|sys_user_role|sys_user_dept|select.*Role|select.*Dept|我的应用|recent" forge-server/forge-framework forge-admin-ui/src
```

Expected: existing methods and mapper XML are listed before introducing any new authorization query.

- [x] **Step 2: Record the selected protocols in the fix execution log**

Append the exact class/mapper names and the trusted user-id source to `code-copilot/changes/lowcode-app-portal-productization/execution-log.md`; do not overwrite the previous log.

### Task 2: Enforce application visibility and page authorization

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationRuntimeService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessApplicationMapper.xml`
- Modify: related DTO/VO/mapper interfaces and tests under `.../generator/src/test/**`
- Modify: `forge-admin-ui/src/views/app-center/components/settings/AppSettingsPermission.vue` only if the existing JSON shape needs normalization

- [x] **Step 1: Add failing coverage for all visibility modes**

Add tests proving that `all`, `roles`, `departments`, and `users` include only matching applications; an application administrator bypasses page permissions; and `systemMenuVisible=false` does not bypass page permissions.

- [x] **Step 2: Implement one authorization predicate used by list and runtime**

Parse `portal_config.permission`, resolve the current trusted user/role/department identifiers, and make the same predicate available to both `selectApplicationPage/selectApplicationList` and `runtime(...)`. Keep all SQL joins/filters in Mapper XML and retain tenant and logical-delete predicates.

- [x] **Step 3: Apply administrator bypass only to page authorization**

Treat configured application administrators as having all published page permissions, but do not let `systemMenuVisible` make an otherwise unauthorized page visible. Preserve the no-page-access response and first-authorized-page fallback.

- [x] **Step 4: Run focused generator tests**

```bash
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator
mvn -Penable-tests -Dtest='*BusinessApplication*Test' test
```

Expected: all focused tests pass and include the new visibility cases.

### Task 3: Isolate live portal slug resolution to published state

**Files:**
- Modify: `BusinessApplicationService.java`
- Modify: `BusinessApplicationRuntimeService.java`
- Modify: `BusinessApplicationMapper.java`
- Modify: `BusinessApplicationMapper.xml`
- Modify: `BusinessApplicationSnapshotService.java` or version entity/service if a published alias is stored there
- Test: `BusinessApplicationRuntimeServiceTest.java`, `BusinessApplicationServiceTest.java`

- [x] **Step 1: Add failing tests for pre-publish slug edits**

Cover both cases: a newly edited slug is not live until a new version is published, and the prior published slug remains resolvable until its replacement is published.

- [x] **Step 2: Implement published alias lookup**

Resolve `/app/{applicationCodeOrSlug}` by application code or a slug captured in the current published snapshot/version, then load that immutable version. Do not use the mutable design-time `portal_slug` as the sole runtime lookup key.

- [x] **Step 3: Keep settings validation design-time only**

Continue validating uniqueness/reserved words when saving settings, but ensure `savePortalConfig` cannot change the live route without a publish operation.

- [x] **Step 4: Run focused runtime tests**

```bash
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator
mvn -Penable-tests -Dtest='BusinessApplicationRuntimeServiceTest,BusinessApplicationServiceTest' test
```

### Task 4: Make Forge workbench distribution queryable and enforce cancellation

**Files:**
- Modify: `BusinessApplicationService.java`, controller, DTO/VO, mapper interface/XML
- Modify: `forge-admin-ui/src/api/business-application.js`
- Modify: `forge-admin-ui/src/views/home/index.vue`
- Test: generator service/controller tests and a focused home/app-center test where practical

- [x] **Step 1: Add a failing distribution projection/query test**

Verify current-user and role-targeted workbench distributions are returned only when enabled and the application is published, visible, active, and has a reachable first page.

- [x] **Step 2: Implement the read protocol**

Add a read-only endpoint for the current user’s workbench applications. Reuse the centralized visibility predicate and role relation resolution; never trust role/user IDs supplied by the browser for the current-user branch.

- [x] **Step 3: Implement disable/remove semantics**

Persist `enabled=false` and make the query exclude disabled records/configurations. Keep DINGTALK as `PENDING_EXTERNAL_SYNC` without claiming external success because no Connector API exists.

- [x] **Step 4: Replace the static home shortcuts**

Load the endpoint in `home/index.vue`, render authorized distributed application entries, and preserve existing static entries only when they are unrelated to application distribution. Add loading/empty/error states.

- [x] **Step 5: Run backend and frontend focused checks**

```bash
cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator
mvn -Penable-tests -Dtest='*BusinessApplication*Test' test
cd ../../../../../../forge-admin-ui
./node_modules/.bin/eslint src/views/home/index.vue src/api/business-application.js
```

### Task 5: Repair logical-delete and unique-index migration contract

**Files:**
- Modify: `forge-server/db/mysql/migration/V1.0.127__add_business_application_portal_config.sql` only for not-yet-applied corrections
- Create: next monotonic Flyway migration if the existing script is already a recorded baseline
- Modify: `AiBusinessApplication.java` and custom delete/query XML only after the database field type is aligned
- Test: static migration checks and entity/service delete tests

- [x] **Step 1: Verify current schema and migration history assumptions**

Run static checks for `del_flag`, `logic_delete_active`, and the latest migration version. Do not edit a migration that may already be installed; use a new version for corrective changes.

- [x] **Step 2: Align field type, `@TableLogic`, and active-row uniqueness**

Use the project’s numeric primary-key tombstone convention consistently, or explicitly preserve the legacy character convention in entity, SQL, and migration. Ensure the active slug uniqueness semantics match the Spec and do not make deleted rows block recreation.

- [x] **Step 3: Validate migration text**

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/mysql/migration
```

Expected: no whitespace errors and no Flyway property placeholders in the new migration.

### Task 6: Close unsupported-capability gaps without false claims

**Files:**
- Modify: `BusinessApplicationAiAssistantService.java`, `BusinessApplicationAiInitializeService.java`, related VO/tests
- Modify: `AppMarketPanel.vue` and/or add a real private-template persistence protocol only if an existing repository protocol is found
- Modify: `AppSettingsGlobalization.vue`, `AppSettingsAdvanced.vue`, runtime config mapping only where a consumer can be implemented safely
- Modify: `spec.md`, `tasks.md`, and append `execution-log.md`

- [x] **Step 1: Confirm absence/presence of reusable business-action, private-template, and Connector APIs**

```bash
rg -n "business action|DataScope|template.*save|private.*template|Connector|DINGTALK|workbench" forge-server forge-admin-ui/src
```

- [x] **Step 2: Keep unsupported AI/template/connector paths explicit**

If no reusable protocol exists, return an explicit capability-unavailable response/empty state and update the Spec/Tasks status; do not fabricate business data or external synchronization.

- [x] **Step 3: Implement only safe runtime consumers**

Use published snapshot globalization/cache/retention fields only if they have an existing, testable consumer; otherwise document them as persisted-but-not-yet-consumed configuration and avoid implying completion.

### Task 7: Incremental verification and handoff

**Files:**
- Modify: `code-copilot/changes/lowcode-app-portal-productization/execution-log.md`
- Modify: `code-copilot/changes/lowcode-app-portal-productization/tasks.md` and `spec.md` only for corrected acceptance/status

- [x] **Step 1: Run the required backend checks**

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
mvn -pl forge-admin-server -am compile -DskipTests
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Penable-tests -Dtest='*BusinessApplication*Test' test
```

- [x] **Step 2: Run the required frontend checks with the repository Node version**

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
cd forge-admin-ui
./node_modules/.bin/eslint <changed frontend files>
./node_modules/.bin/vitest run <changed or existing focused specs>
node --max_old_space_size=8192 ./node_modules/vite/bin/vite.js build
```

- [x] **Step 3: Append commands, results, warnings, skipped external checks, and cleanup status**

Add a dated “fix review” section to `execution-log.md`; preserve all prior evidence and explicitly distinguish code-level completion from MySQL, service, Playwright, mobile, and external Connector checks that remain unexecuted.
