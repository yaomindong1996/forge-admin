# execution-log — application-debug-bundle

## 2026-09-30 P0 验证

### 后端生产编译
```bash
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```
- 结果：通过

### 单测 BusinessApplicationBundleSupportTest
```bash
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator test -Penable-tests -Dtest=BusinessApplicationBundleSupportTest -DfailIfNoTests=false
```
- 结果：通过（Tests run: 3, Failures: 0）
  - export sanitize strips nested sensitive keys
  - import validate rejects unknown format
  - import remaps object codes inside nested JSON trees

### 前端 lint
```bash
cd forge-admin-ui && pnpm exec eslint --no-error-on-unmatched-pattern \
  src/views/app-center/components/settings/AppSettingsAdvanced.vue \
  src/views/app-center/components/debug-bundle/ApplicationDebugBundleImportDialog.vue \
  src/api/business-application.js
```
- 结果：通过

## 2026-09-30 P1 验证

### 后端编译 + 单测
```bash
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator test -Penable-tests \
  -Dtest=BusinessApplicationBundlePageSupportTest,BusinessApplicationBundleSupportTest -DfailIfNoTests=false
```
- 结果：编译通过；Tests run: 6, Failures: 0

### 前端 lint
```bash
cd forge-admin-ui && pnpm exec eslint --no-error-on-unmatched-pattern \
  src/views/app-center/components/settings/AppSettingsAdvanced.vue \
  src/views/app-center/components/debug-bundle/ApplicationDebugBundleExportDialog.vue \
  src/api/business-application.js
```
- 结果：通过

## 2026-09-30 P2 验证

### 后端编译 + 单测
```bash
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator test -Penable-tests \
  -Dtest=BusinessApplicationBundlePageSupportTest,BusinessApplicationBundleSupportTest -DfailIfNoTests=false
```
- 结果：编译通过；Tests run: 6, Failures: 0

### 前端 lint
```bash
cd forge-admin-ui && pnpm exec eslint --no-error-on-unmatched-pattern \
  src/views/app-center/components/debug-bundle/*.vue \
  src/api/business-application.js
```
- 结果：通过

### P2 覆盖
- schemaVersion=2：processes / bindings / extensions / entries / triggers / printing / documentConfig / aiAssistantConfig
- 导入步骤 `companions` + 可选 `autoPublish`
- 单项伴随资产失败记入 warnings，不阻断主导入
