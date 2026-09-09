# Execution log

## 2026-08-06 implementation and incremental verification

Scope: authenticated image enlargement, safe application process-publish diagnostics, draft runtime load deduplication, stable CRUD runtime mounting, and reactive table alignment/row spacing.

### Commands and results

1. Focused frontend tests:
   `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec vitest run src/components/common/__tests__/AuthImage.spec.js src/components/lowcode-builder/shared/__tests__/runtime-crud-props.spec.js src/views/app-center/__tests__/application-runtime-load.spec.js src/components/ai-form/__tests__/AiTable.spec.js`
   Result: 4 files passed, 21 tests passed.
2. Target frontend lint:
   `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint <affected frontend files>`
   Result: passed with no errors or warnings.
3. Generator tests with JDK 17:
   `JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH=$(/usr/libexec/java_home -v 17)/bin:$PATH mvn -Penable-tests -Dtest=BusinessProcessPublishServiceTest,BusinessApplicationDraftPreviewContractTest test`
   Result: build success; 12 tests passed, 0 failures/errors/skips.
4. Frontend production build:
   `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
   Result: passed; 8868 modules transformed, built in 17m 41s.
5. Scoped `git diff --check`:
   Result: passed before and after documentation closeout; new untracked files also passed a trailing-whitespace scan.

### Warnings

- Vite reported existing dynamic/static import chunking warnings, an existing `UserSelectModal` auto-registration name conflict, and an existing CSS `//` comment warning. None blocked the build and none originated from this change's functional paths.
- The Maven module defaults to skipping test compilation/execution. The final test command used the repository's `enable-tests` profile; the earlier compile-only invocation was not counted as test evidence.

### Skipped environment checks

- Did not start Admin, Flow, Vite dev server, MySQL, or Redis.
- Did not execute Flyway or mutate application/process runtime data.
- Did not run browser E2E. The user will validate the real application and publish flow in the existing environment.
- Services started by this verification: none.

## 2026-08-07 incremental implementation and verification

Scope: exact table header/body centering, optional access-entry publication, and business-process edge insertion with palette drag/drop.

### Implementation notes

- `AiTable` now wraps every normal body renderer in a full-width flex cell whose justification follows the final column alignment. Centered columns receive a dedicated header class; the title is centered against the full header cell while sort/filter controls are positioned outside the title flow.
- Application publish selection now distinguishes `null` (default/all publishable entries) from `[]` (publish no entries). Explicit disabled or incomplete runtime entries are removed with dependency warnings; ownership validation runs before any filtering or side effect and still rejects foreign IDs.
- The business-process designer now inserts through `insertNodeOnEdge`, renders one `BusinessProcessAddNodeButton` per graph edge, accepts the governed node MIME type from the left palette, highlights the nearest edge during canvas dragover, and opens the existing node configuration drawer after insertion.

### Commands and results

1. Red/green frontend regression loop:
   `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec vitest run src/components/business-process-designer/__tests__/business-process-designer.spec.js src/components/business-process-designer/__tests__/business-process-designer-workbench.spec.js`
   Result: initial 3 expected failures for the missing insertion API/controls; after implementation 20 tests passed.
2. Focused frontend regression suite:
   `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec vitest run src/components/common/__tests__/AuthImage.spec.js src/components/lowcode-builder/shared/__tests__/runtime-crud-props.spec.js src/views/app-center/__tests__/application-runtime-load.spec.js src/components/ai-form/__tests__/AiTable.spec.js src/components/business-process-designer/__tests__/business-process-designer.spec.js src/components/business-process-designer/__tests__/business-process-designer-workbench.spec.js`
   Result: 6 files passed, 42 tests passed.
3. Target frontend lint:
   `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/components/ai-form/AiTable.vue src/components/ai-form/__tests__/AiTable.spec.js src/components/business-process-designer/BusinessProcessAddNodeButton.vue src/components/business-process-designer/BusinessProcessCanvas.vue src/components/business-process-designer/BusinessProcessDesigner.vue src/components/business-process-designer/business-process-node-types.js src/components/business-process-designer/useBusinessProcessDesigner.js src/components/business-process-designer/__tests__/business-process-designer.spec.js src/components/business-process-designer/__tests__/business-process-designer-workbench.spec.js`
   Result: passed with no errors or warnings.
4. Generator tests with JDK 17:
   `JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH=$(/usr/libexec/java_home -v 17)/bin:$PATH mvn -Penable-tests -Dtest=BusinessApplicationAssetSelectionServiceTest,BusinessAppServiceCompatibilityTest,BusinessProcessPublishServiceTest,BusinessApplicationDraftPreviewContractTest test`
   Result: 4 test classes passed, 23 tests passed, 0 failures/errors/skips.
5. Frontend production build:
   `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
   Result: passed; 8874 modules transformed, built in 4m 23s.
6. Scoped whitespace check:
   `git diff --check`
   Result: passed. The new untracked `BusinessProcessAddNodeButton.vue` was also checked separately for trailing whitespace.

### Warnings and skipped environment checks

- Vite reported the same existing dynamic/static import chunking warnings, `UserSelectModal` component-name conflict, and CSS `//` comment warning as the previous baseline; none originated from the changed behavior or blocked the build.
- Maven emitted existing deprecation/unchecked compilation notes; tests passed.
- Did not start Admin, Flow, Vite dev server, MySQL, or Redis; did not execute Flyway or browser E2E. Services started by this verification: none.
