# Low-code preview, publish diagnostics, and table layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make low-code image cells previewable, make workflow publish failures actionable, prevent duplicate draft-preview loads, and keep table alignment/row spacing live in both designer and runtime previews.

**Architecture:** Extend the existing authenticated `AuthImage` component instead of introducing an unauthenticated image URL path. Publish failures keep the current safe response contract but add a stable error code and diagnostic reference, while server logs retain the exception context. A route-load key and in-flight promise make application preview loading idempotent. Table layout is normalized through `buildRuntimeCrudProps` and `AiTable`, with the designer renderer and static preview consuming the same `globalAlign`, `fieldSettings`, and `tableRowGap` values.

**Tech Stack:** Vue 3, Naive UI, Vitest, Spring Boot 3, Java 17, MyBatis-Plus.

---

### Task 1: Authenticated image preview

**Files:**
- Modify: `forge-admin-ui/src/components/common/AuthImage.vue`
- Modify: `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue`
- Test: `forge-admin-ui/src/components/common/__tests__/AuthImage.spec.js`

- [x] Add an opt-in preview modal to `AuthImage` using the already resolved authenticated/blob URL.
- [x] Pass `preview` from image/file columns in both CRUD renderers.
- [x] Cover the click-to-open and close behavior without bypassing authenticated URL resolution.

### Task 2: Actionable business-application publish diagnostics

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPublishService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessApplicationPublishResultVO.java`
- Modify: `forge-admin-ui/src/views/app-center/application-workspace/ApplicationPublishPanel.vue`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessPublishServiceTest.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationDraftPreviewContractTest.java`

- [x] Log unexpected failures with application/run/step context and a generated diagnostic reference.
- [x] Return a safe step-specific message and `errorCode` while keeping stack traces and sensitive values out of the API.
- [x] Display the process-publish step label and error code in the publish step dialog.

### Task 3: Idempotent draft application preview loading

**Files:**
- Modify: `forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`
- Test: `forge-admin-ui/src/views/app-center/__tests__/application-runtime-load.spec.js`

- [x] Guard same-route concurrent loads with an in-flight promise.
- [x] Skip a second load for an already loaded identical route key while allowing draft/runtime/page changes to reload.
- [x] Keep current page selection and CRUD preloading behavior unchanged after the single load.

### Task 4: Live table alignment and row spacing

**Files:**
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/shared/runtime-crud-props.js`
- Modify: `forge-admin-ui/src/components/ai-form/AiCrudPageProps.js`
- Modify: `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
- Modify: `forge-admin-ui/src/components/ai-form/AiTable.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue`
- Test: `forge-admin-ui/src/components/lowcode-builder/shared/__tests__/runtime-crud-props.spec.js`

- [x] Apply global alignment to every configured table field, independent of the currently open field drawer.
- [x] Apply field/global alignment to runtime columns even when compiled columns already exist.
- [x] Carry `tableRowGap` through runtime props and render it in both preview and `AiTable` table rows.
- [x] Verify changes are reactive and persisted through the existing block props/schema update path.

### Task 5: Incremental verification

- [x] Run focused Vitest suites for image preview, runtime props, and preview load guards.
- [x] Run `git diff --check` and the frontend build with Node 20.19.0.
- [x] Run the affected generator Maven test/compile with JDK 17 when the local environment permits; otherwise record the exact blocker.
- [x] Append commands, results, warnings, and skipped service-level checks to `execution-log.md`.
