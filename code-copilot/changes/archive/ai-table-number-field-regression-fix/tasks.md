# AiTable Selection and Number Field Regression Fix Implementation Plan

> **For agentic workers:** inline execution in the current workspace is required; preserve unrelated user changes and do not commit or push automatically.

**Goal:** 修复 AiTable 选中背景断层，并统一 AiForm 数字字段类型契约。

**Architecture:** AiTable 使用可测试的行类名合并函数表达受控选中态，再由组件级主题 CSS 统一覆盖不同列状态。AiForm 使用集中式类型判断函数兼容历史别名，页面配置机械归一为标准 `number`。

**Tech Stack:** Vue 3、Naive UI、Vitest、ESLint、Vite。

---

### Task 1：建立回归测试工具

**Files:**

- Create: `forge-admin-ui/src/components/ai-form/field-type-utils.js`
- Create: `forge-admin-ui/src/components/ai-form/table-state-utils.js`
- Create: `forge-admin-ui/src/components/ai-form/__tests__/field-type-utils.spec.js`
- Create: `forge-admin-ui/src/components/ai-form/__tests__/table-state-utils.spec.js`

- [x] 新增 `isNumberFieldType(type)`，覆盖 `number`、`inputNumber`、`input-number`。
- [x] 新增 `isInputLikeFieldType(type)`，为占位符和必填文案统一判断文本/数字输入。
- [x] 新增 `resolveTableRowClassName(rowClassName, row, index, checked)`，合并调用方类名与选中态类名。
- [x] 先执行两个定向 Vitest，确认工具契约通过。

### Task 2：修复 AiTable 选中背景

**Files:**

- Modify: `forge-admin-ui/src/components/ai-form/AiTable.vue`
- Test: `forge-admin-ui/src/components/ai-form/__tests__/table-state-utils.spec.js`

- [x] 为 `AiTable` 增加正式的 `rowClassName` prop，并将合并后的 resolver 传给 `n-data-table`。
- [x] 根据 `checked-row-keys` 为行追加 `ai-table-row--checked`。
- [x] 为普通、排序、固定选择和固定操作单元格增加统一选中/选中悬停背景。
- [x] 保持现有勾选协议、行点击行为、排序和筛选事件不变。

### Task 3：统一数字字段共享链路

**Files:**

- Modify: `forge-admin-ui/src/components/ai-form/AiFormItem.vue`
- Modify: `forge-admin-ui/src/components/ai-form/AiForm.vue`
- Modify: `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
- Modify: `forge-admin-ui/src/components/ai-form/AiCustomQuery.vue`
- Test: `forge-admin-ui/src/components/ai-form/__tests__/field-type-utils.spec.js`

- [x] AiFormItem 数字渲染与占位符识别改用统一类型函数。
- [x] AiForm 必填规则、触发器和空值校验改用统一类型函数。
- [x] AiCrudPage 编辑回填和运行时数字字段判断兼容历史别名。
- [x] AiCustomQuery 的数字控件归一化兼容历史别名。

### Task 4：清理页面错误配置

**Files:**

- Modify: `forge-admin-ui/src/views/ai/context-config.vue`
- Modify: `forge-admin-ui/src/views/ai/model.vue`
- Modify: `forge-admin-ui/src/views/data/dataset-category.vue`
- Modify: `forge-admin-ui/src/views/flow/spelTemplate.vue`
- Modify: `forge-admin-ui/src/views/message/biz-type.vue`
- Modify: `forge-admin-ui/src/views/system/client.vue`
- Modify: `forge-admin-ui/src/views/system/config.vue`
- Modify: `forge-admin-ui/src/views/system/dictData.vue`
- Modify: `forge-admin-ui/src/views/system/excel-export-config.vue`
- Modify: `forge-admin-ui/src/views/system/job-config.vue`
- Modify: `forge-admin-ui/src/views/system/notice.vue`
- Modify: `forge-admin-ui/src/views/system/org.vue`
- Modify: `forge-admin-ui/src/views/system/post.vue`
- Modify: `forge-admin-ui/src/views/system/role.vue`
- Modify: `forge-admin-ui/src/views/system/storage-config.vue`
- Modify: `forge-admin-ui/src/views/system/tenant.vue`

- [x] 将所有 `type: 'input-number'` 机械替换为 `type: 'number'`，不改动字段名、默认值、校验或 props。
- [x] 扫描整个 `src/views`，确认错误写法为零。

### Task 5：增量验证与回填

**Files:**

- Modify: `code-copilot/changes/ai-table-number-field-regression-fix/spec.md`
- Modify: `code-copilot/changes/ai-table-number-field-regression-fix/tasks.md`
- Modify: `code-copilot/changes/ai-table-number-field-regression-fix/test-spec.md`
- Modify: `code-copilot/changes/ai-table-number-field-regression-fix/execution-log.md`

- [x] 执行定向 Vitest。
- [x] 执行目标 ESLint。
- [x] 执行 `src/views` 类型零残留扫描。
- [x] 执行前端生产构建。
- [x] 若已有本地前端与后端服务，则使用 Playwright 复验排序后勾选行和数字输入；否则记录为未执行项。
- [x] 执行 `git diff --check` 并回填实际证据。
