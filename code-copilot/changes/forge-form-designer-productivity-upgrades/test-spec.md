# 测试计划：forge-form-designer-productivity-upgrades

## 2026-06-21 增量验证

### 变更范围

- `ForgeFormCanvas.vue` 新增画布缩放浮层和 `Ctrl/⌘ + 滚轮` 缩放交互。
- 不改表单 schema 保存协议、后端接口或数据库脚本。

### P0 验证

- `git diff --check` 覆盖本轮表单与列表画布文件。
- 定向 eslint 覆盖 `ForgeFormCanvas.vue` 和 `ListPageGridDesigner.vue`。
- `pnpm --dir forge-admin-ui build` 覆盖 Vue 模板编译和生产构建。

### 跳过项

- 未启动 Vite/后端服务做浏览器截图验证；本轮为设计器前端布局和本地交互优化，已用构建覆盖模板编译。

## 2026-06-21 表单布局组件增量验证

### 变更范围

- `ForgeFormCanvas.vue` 将缩放控件固定在画布视口右下角，画布内容改为内部滚动。
- `ForgeFormCanvasNode.vue` 调整布局组件结构槽位、内部 drop 事件和设计态边框样式。
- 不改表单 schema 保存协议、后端接口或数据库脚本。

### P0 验证

- `git diff --check` 覆盖本轮表单画布文件。
- 定向 eslint 覆盖 `ForgeFormCanvas.vue` 和 `ForgeFormCanvasNode.vue`。
- `pnpm --dir forge-admin-ui build` 覆盖 Vue 模板编译和生产构建。

### 跳过项

- 未启动 Vite/后端服务做浏览器截图验证；本轮为设计器前端布局和拖拽命中优化，已用构建覆盖模板编译。
