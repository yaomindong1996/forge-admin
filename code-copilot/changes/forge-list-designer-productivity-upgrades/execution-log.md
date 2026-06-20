# 执行日志：forge-list-designer-productivity-upgrades

## 2026-06-19 21:29:05 CST

### 变更范围

- 修复左树右表运行态边界：右侧 `AiCrudPage` 运行 props 清空 `treeConfig` 与 `options.treeConfig`，避免右表误进入树表模式。
- `tree-panel` 运行态继续使用独立 `apiConfig.tree` 加载树数据；树节点选择只通过 `publicParams` 给右侧列表追加过滤字段。
- 简化筛选树属性面板，移除“高级联动 / 生成联动事件”入口，改为说明树接口和右表过滤字段的关系。
- 保留预览弹窗 `.n-card__content` / `.n-card-content` 的全高 flex 与滚动约束。

### 命令与结果

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint ...`：未执行到 eslint，`nvm use v20.19.0` 返回 `N/A: version "N/A" is not yet installed`。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 运行态和设计器配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 09:05:00 CST

### 变更范围

- 修复树模块折叠后内部 `grid-block` 仍按保存的固定宽度渲染的问题：折叠状态强制组件根节点宽度跟随父级 44px。
- 将整块树折叠按钮从模块外沿移回树模块内部右上角，避免被外层 `overflow:hidden` 裁切或被右侧 CRUD 覆盖。
- 调整树标题区布局，给折叠按钮预留空间，并把树节点展开/收起按钮做成更紧凑的横向按钮组。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器树模块样式和折叠宽度。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-20 08:57:20 CST

### 变更范围

- 将左侧筛选树的“收起面板”改为区块级折叠：折叠后树区块宽度压缩到 44px，并保留竖向侧栏提示。
- 父级画布接收 `treePanelCollapseChange`，在只读预览/运行态下重算同一行右侧区块位置和宽度，让 CRUD 列表真正补满树释放的空间。
- 树节点展开/收起按钮继续保留在树标题内；整块树折叠改为模块边缘箭头按钮，避免与节点展开收起混淆。
- 修复 CRUD 快速配置搜索区域：配置面板改为“头部 + 搜索 + 滚动内容”三行布局，搜索区域有稳定白色背景和分隔阴影。
- 自由画布配置组件搜索区域补齐固定高度、白底和层级，避免遮挡配置内容。
- 预览弹窗内容区、画布容器、滚动区和内部网格统一白底并撑满高度，避免弹窗下半截透明/无背景。
- 回调参数处理编辑器改为单列规则输入，降低窄面板内输入框横向溢出的概率。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/CrudHookRulesEditor.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 设计器和预览样式。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 22:42:11 CST

### 变更范围

- 抽出共享 `CrudHookRulesEditor.vue` 和 `crud-hook-rules.js`，`CRUD 快速配置` 与自由画布 `AiCrudPage` 属性面板统一复用同一套回调参数处理配置。
- 回调配置从单一 `beforeSubmitRules` 扩展为统一 `crudHookRules`，覆盖加载列表前、搜索前、打开表单前、打开详情前、提交前、构建提交数据后等对象参数处理场景。
- 运行页 `crud-page.vue` 编译 `crudHookRules` 为真实 `AiCrudPage` hook，并从发布画布的 `AiCrudPage.props` 兜底读取；旧 `beforeSubmitRules` 自动兼容迁移。
- 统一 `CRUD 快速配置` 与自由画布 `配置组件` 搜索样式。
- 简化 `CRUD 快速配置` 面板布局：窄面板表单默认一列、开关两列、复杂行纵向排列，避免输入框横向溢出。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/CrudHookRulesEditor.vue src/components/lowcode-builder/page/crud-hook-rules.js src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/CrudHookRulesEditor.vue forge-admin-ui/src/components/lowcode-builder/page/crud-hook-rules.js forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 运行态和设计器配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 22:13:19 CST

### 变更范围

- 筛选树区块增加设计态和运行态的展开/收起控制，运行态 `n-tree` 改为受控 `expandedKeys`。
- 修复 CRUD 快速配置中左树右表预览容器：去掉右侧 CRUD 硬最小宽度，补齐 `min-width: 0` 与内部滚动约束，避免右侧内容显示不全。
- `CRUD 快速配置` 增加配置搜索；自由画布右侧 `配置组件` 增加配置搜索。
- 增加可视化 `beforeSubmitRules` 配置，支持提交前设置固定值、复制字段、尾部拼接和清空字段；运行页编译为 `AiCrudPage.beforeSubmit`。
- 设计态 AiCrudPage 新增/编辑预览优先使用当前对象完整字段集合，减少和应用页表单内容不一致的问题。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/components/lowcode-builder/page/page-schema.js src/views/ai/crud-page.vue`：通过。
- `git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/StructuredListPageDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/page-schema.js forge-admin-ui/src/views/ai/crud-page.vue`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 跳过项

- 未进行浏览器手工验证或 Playwright 截图：本轮未启动 Vite/后端服务。
- 未执行后端编译：本轮只改前端 Vue 运行态和设计器配置面板。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。

## 2026-06-19 21:38:58 CST

### 变更范围

- 针对已发布 `listGridLayout.layoutType = tree-crud` 但运行配置顶层 `layoutType` 仍为 `simple-crud` 的场景，运行页改为优先使用当前画布布局类型。
- `crudProps` 的树表判断改为读取有效布局类型，避免左树右表右侧列表继续把 `apiConfig.list` 替换成 `apiConfig.tree`。
- 删除 `BusinessListDesigner.vue` 中残留的 `.n-card-content { background: red; }` 调试样式。

### 命令与结果

- `pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/BusinessListDesigner.vue`：通过。
- `git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue code-copilot/changes/forge-list-designer-productivity-upgrades/tasks.md code-copilot/changes/forge-list-designer-productivity-upgrades/test-spec.md code-copilot/changes/forge-list-designer-productivity-upgrades/execution-log.md`：通过。
- `pnpm --dir forge-admin-ui build`：通过。

### 警告

- 构建保留项目既有 warning：CSS 中存在 `//` 注释。
- 构建保留项目既有 warning：`src/store/index.js` 同时被动态和静态导入，Rollup 不会将其移动到单独 chunk。

### 服务清理

- 本轮未启动常驻服务，无需清理 PID。
