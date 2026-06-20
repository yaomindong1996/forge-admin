# 测试计划：forge-list-designer-productivity-upgrades

## 本轮增量验证（2026-06-19）

### 范围

- 左树右表运行态：左侧 `tree-panel` 独立加载树接口，右侧 `AiCrudPage` 保持普通分页接口。
- 左树右表设计态：筛选树属性面板简化为必要字段，去掉容易误导的手动联动事件入口。
- 列表预览弹窗：继续验证内容容器 flex 高度和滚动约束不破坏构建。

### P0 命令

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/views/ai/crud-page.vue`
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue`
- `pnpm --dir forge-admin-ui build`

### 跳过项

- 未启动 Vite 和浏览器截图验证：本轮没有启动前后端服务，先以静态检查和生产构建覆盖；运行时请求链路需在本地服务可用时用真实发布页面复验。
