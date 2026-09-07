# Excel 导出配置易用性优化 Implementation Plan

> **For agentic workers:** 按任务逐项执行并在每个检查点验证。

**Goal:** 将 Excel 技术配置页改造成业务人员可理解、可快速维护的导出方案工作台。

**Architecture:** 保留 `AiCrudPage` 与既有 API，重排列表信息层级和编辑 Schema；列配置继续使用独立组件，但改成业务预览式列表，并用局部状态提示未保存变更。

**Tech Stack:** Vue 3、Naive UI、UnoCSS、AiCrudPage、Vitest、Vite。

---

### Task 1: 建立用户路径与文案基线

**Files:**
- Modify: `forge-admin-ui/src/views/system/excel-export-config.vue`
- Modify: `forge-admin-ui/src/views/system/excel-column-config.vue`

- [x] 梳理列表、方案编辑、列配置三个路径，建立业务文案和技术字段层级。
- [x] 明确状态切换、复制、删除、导出测试的反馈和失败提示。

### Task 2: 优化导出方案列表与编辑表单

**Files:**
- Modify: `forge-admin-ui/src/views/system/excel-export-config.vue`

- [x] 列表首屏改为方案名称、用途、文件名、行数限制、状态和更新时间；技术字段移入辅助信息/高级设置。
- [x] 增加页面说明和业务化搜索占位符；状态列改为可确认的列表开关。
- [x] 将表单分组、默认值、动态用途说明和技术设置折叠化，优化复制弹窗和删除/测试反馈。
- [x] 增加响应式样式和可访问标签。

### Task 3: 优化列配置管理

**Files:**
- Modify: `forge-admin-ui/src/views/system/excel-column-config.vue`

- [x] 增加方案上下文、列数和未保存状态提示。
- [x] 用“Excel 表头 / 对应字段 / 显示顺序”等业务文案替代技术感标题，低频格式与校验项折叠。
- [x] 优化排序操作、空态、删除确认、保存 loading 和移动端布局。

### Task 4: 验证与交付

**Files:**
- Create: `code-copilot/changes/excel-export-usability/test-spec.md`
- Create: `code-copilot/changes/excel-export-usability/execution-log.md`

- [x] 执行 ESLint、关键 Vitest 和 `pnpm --ignore-workspace build`。
- [x] 用 Playwright 预览验证亮暗色、窄屏、状态切换确认、复制/列配置弹窗和错误提示。
- [x] 仅提交本次页面与变更文档，保留无关 `.DS_Store` 改动，不推送。
