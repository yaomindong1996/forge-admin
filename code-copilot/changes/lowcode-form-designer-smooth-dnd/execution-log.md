# 执行日志 — 低代码表单设计器顺滑拖拽改造

## 2026-06-15：原生表单设计器骨架接入

### 变更范围

- 新增 `forge-form-designer` 前端组件目录。
- 新增 Forge 原生表单设计器三栏骨架：字段/布局面板、画布、节点、属性面板。
- 增强 `formDesignerSchema.js`，补充组件树查找、插入、移动、删除、复制、属性更新等纯函数。
- 修改 `BusinessFormDesigner.vue`，默认使用新版画布，保留旧 FormCreate 画布回退按钮。

### 执行命令与结果

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js forge-admin-ui/src/views/app-center/components/designer/forge-form-designer code-copilot/changes/lowcode-form-designer-smooth-dnd code-copilot/memory/preferences.md
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use 20.19.0 && pnpm exec eslint src/views/app-center/components/designer/BusinessFormDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过。命令前置的 `nvm use` 打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use 20.19.0 && pnpm build
```

结果：通过，Vite 构建完成。

### 警告项

- 构建输出存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建输出存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use 20.19.0` 仍打印 `N/A` 未安装提示，但后续 pnpm 命令实际执行成功；后续可单独排查本机 nvm 默认版本配置。
- `pnpm dev -- --host 127.0.0.1` 启动失败，报错 `EMFILE: too many open files, watch`；提升 `ulimit -n 65536` 后仍失败，判断为本机文件监听限制。
- `pnpm preview -- --host 127.0.0.1` 在默认沙箱下绑定端口失败：`listen EPERM`；经用户授权后以提升权限启动成功。

### 跳过项

- 未启动本地 Vite dev server：受本机文件监听限制阻断；已改用 Vite preview 服务提供构建产物预览。
- 未做浏览器截图/点击验证：本轮为骨架接入，已完成静态检查和生产构建；后续拖拽动效细节完成后再做浏览器交互验证。
- 未执行后端验证：本轮不涉及后端代码、数据库脚本或接口协议。

### 服务清理

Vite preview 服务已启动并保留供本地检查：

- URL: `http://localhost:4173/forge`
- 启动命令：`cd forge-admin-ui && source ~/.nvm/nvm.sh && nvm use 20.19.0 && pnpm preview -- --host 127.0.0.1`

## 2026-06-15：栅格布局、落点投影与属性面板增强

### 变更范围

- 表单列数支持扩展到 1-4 列，并在设计态画布真实应用 `rowGap` / `columnGap`。
- `formDesignerSchema` 修正同父级向下移动时的插入位置偏移；调整列数时保留字段跨度，只对超出范围的跨度做合法化。
- 左侧布局面板的“栅格布局”生成显式 `row.props.columns`，支持后续属性面板调整列数。
- 中间画布新增拖动源 ghost 状态、稳定 drop active 状态、空画布/根落点/空容器投影和偏移阴影。
- 栅格 row 只允许列重排，字段必须拖入具体列，避免字段落在 row 与 col 的错误层级。
- 右侧属性面板增加回到表单属性、1-4 列表单布局、栅格 row 列数/间距、col 容器视觉、隐藏开关等高频配置。
- 运行态 `AiFormLayoutNodes` 读取 `row.props.columns`，保证设计态 row 列数发布后不丢失。
- 新增 `forge-form-designer/README.md` 说明模块职责边界。

### 执行命令与结果

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js forge-admin-ui/src/views/app-center/components/designer/forge-form-designer forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/BusinessFormDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js src/components/ai-form/AiFormLayoutNodes.vue
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `2m 59s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器、Schema 工具和运行态前端布局渲染。
- 未做浏览器截图/拖拽实测：本轮完成静态检查与生产构建；当前 4173 端口已有 preview 服务监听，可继续人工打开页面验证交互细节。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：Tabs/Collapse 子项管理、交互规则与源码编辑入口

### 变更范围

- 标签页组件继续使用真实 Naive `NTabs/NTabPane` 渲染，新增页签管理：可新增页签、重命名、设置 `name`、上移/下移、删除，并可一键选中页签内容继续配置。
- 折叠面板组件继续使用真实 Naive `NCollapse/NCollapseItem` 渲染，新增面板管理：可新增面板、重命名、设置 `name`、排序、删除，并可进入面板内容配置。
- 画布预览透传 Tabs 的 `addable`、`justifyContent`、`tabsPadding`、`paneStyle`、`tabStyle`，Collapse 透传 `defaultExpandedNames`、`expandedNames`，让更多属性里的配置能直接反映到预览。
- 新增按钮组件入口，拖入后按真实 Naive `NButton` 渲染，支持按钮文字、类型、尺寸、block、disabled 和更多 Button Props 配置。
- 属性面板新增“交互”页签，事件规则保存到 `props.__events`，支持声明 `click/change/focus/blur/clear/mounted` 触发，以及设置值、清空值、设置选项、显隐、禁用启用、打开弹窗、调用接口等动作。
- 属性面板新增“源码”页签：选中组件时可查看/编辑当前组件 JSON；未选中组件时可查看/编辑整个表单 Schema JSON，应用前进行 JSON 解析和 schema 规范化。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh; nvm use v20.19.0; pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh; nvm use v20.19.0; NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 25s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器配置、预览和 schema 编辑能力。
- 未启动浏览器做交互录屏验证：本轮用目标 eslint、diff 检查和生产构建覆盖模板、脚本和打包链路。

### 服务清理

- 已按前端验证要求启动 Vite 开发服务，当前地址：`http://localhost:3001/`。
- 启动时 3000 端口已被占用，Vite 自动切换到 3001。
- 沙箱内监听端口被拒绝后，已按权限规则在沙箱外启动服务；为便于人工验收，本轮未主动停止该服务。

## 2026-06-16：CRUD 列配置显性化与更多属性中文化

### 变更范围

- “更多属性”抽屉的分组标题和字段标签改为中文说明 + prop 名，例如 `组件尺寸（size）`、`可清空（clearable）`，保留 Naive 文档 prop 名方便对照。
- “更多配置”按钮改为蓝色主按钮样式，不再使用白色弱按钮。
- CRUD 区块画布移除重复的顶部标题块，只保留真实 `AiCrudPage` 预览；移除下方额外字段节点区域，避免和 CRUD 自带查询/表格/编辑区域重复。
- CRUD 区块无字段时保留一个轻量投放提示，拖入字段后由真实 `AiCrudPage` 根据子字段生成查询、列和编辑 schema。
- CRUD 属性页把“字段用途”改为“字段与列配置”，每个字段改为配置卡片，查询/表格列/编辑开关与列标题、列宽、对齐、固定、省略、排序都直接在卡片里可见。
- 新增 `updateCrudFieldConfigById()`，支持在 CRUD 区块面板内直接更新任意子字段的表格列配置，不需要先选中隐藏字段。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 18s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器属性面板和画布渲染。
- 未启动浏览器做交互截图：本轮使用目标 eslint、diff 检查和生产构建覆盖模板/脚本/打包风险。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：组件完整属性抽屉与 CRUD 字段级配置

### 变更范围

- 新增选中组件的“更多属性”抽屉，按当前项目 `naive-ui` 组件类型定义抽象为属性 schema，覆盖输入、文本域、数字/金额、选择、字典选择、单选/按钮单选、多选、开关、滑块、评分、颜色、级联、树选、日期/时间、Card、Tabs、Collapse 的常用可序列化 props。
- 多选组件区分 `CheckboxGroup Props` 和单个选项的 Checkbox 属性：选项列表支持 label/value/disabled，Checkbox 额外支持 indeterminate、focusable 和 option `props JSON`，并在 `AiFormItem` 中透传到真实 `n-checkbox`。
- CRUD 区块内选中某个字段时，基础面板新增“CRUD 字段配置”，支持配置查询标签/占位/跨度、表格列标题/宽度/最小宽/对齐/固定/省略/排序、编辑标签/跨度/只读。
- `AiCrudPage` 设计态预览的 columns/search/edit schema 已消费字段级 CRUD 配置，配置不再只是保存到 schema。
- 继续保留 CRUD 区块级“更多配置”抽屉，字段级配置和全局配置职责分离。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/components/ai-form/AiFormItem.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 10s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。
- 对 `AiFormItem.vue` 单独跑 eslint 会触发该老文件大量既有缩进规则错误；本轮未格式化整文件，使用 `git diff --check` 和生产构建覆盖新增模板透传的有效性。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器属性面板、设计态 CRUD schema 和表单组件透传。
- 未启动浏览器做交互截图：本轮已通过目标 eslint、diff 检查和生产构建覆盖模板/脚本/打包风险。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：属性面板 Naive props 对齐与 CRUD 更多配置收纳

### 变更范围

- 删除选中节点顶部重复的 `selected-summary`，保留右侧面板标题和“表单”返回按钮，减少与顶部信息重复。
- 基础配置折叠面板改为线性分区样式，去掉厚重外框，降低属性多时的拥挤感。
- 根据当前项目安装的 Naive UI 类型定义补充真实组件配置：Card 支持 `size`、`bordered`、`embedded`、`segmented`、`hoverable`；Tabs 支持 `type`、`size`、`placement`、`trigger`、`animated`、`closable`；Collapse 支持 `accordion`、`arrowPlacement`、`displayDirective`、`triggerAreas`。
- 日期/时间字段补充 `format`、`valueFormat`、`placement`、`actions`、`bordered`、`inputReadonly` 等常用配置，并继续通过真实字段 props 下发给 `AiFormItem`。
- CRUD 主面板保留接口基础路径、行主键、字段用途、查询/表格/编辑常用项；API 明细、查询细节、表格滚动、编辑弹窗、分页和开关项收进“更多配置”抽屉，避免基础配置过长。
- 画布中的卡片、标签页、折叠面板渲染同步读取新增 props，不只是在属性面板保存配置。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 14s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。
- 用户给出的 Naive UI 按钮文档 URL 当前浏览返回 404；本轮以本项目 `node_modules/naive-ui` 中的当前版本类型定义为准。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器属性面板、画布渲染和样式。
- 未启动浏览器做交互截图：本轮重点是属性配置结构和构建链路，已通过目标 eslint、diff 检查和生产构建覆盖模板/脚本/打包风险。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：CRUD 字段用途配置、真实布局组件与基础配置折叠

### 变更范围

- CRUD 区块新增“字段用途”矩阵：拖入 CRUD 区块的字段可分别配置是否进入查询条件、表格列、编辑弹窗，配置写入字段组件 `props.__crudRoles`。
- 真实 `AiCrudPage` 设计态预览改为按 `__crudRoles` 过滤生成 `searchSchema`、`columns`、`editSchema`，不再只能默认全部字段或前几个字段。
- 卡片、标签页、折叠面板从普通虚线容器改为真实 Naive `n-card`、`n-tabs`、`n-collapse` 渲染，同时内部仍保留设计器节点用于选中、配置和拖拽。
- 组件基础配置页签改为折叠分组：标识默认展开，字段组件和辅助展示按需展开，减少配置项堆叠导致的混乱感。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `2m 38s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器交互、组件渲染和配置结构。
- 未启动浏览器做人工点击实测：本轮使用目标 eslint、diff 检查和生产构建覆盖模板/脚本/打包链路。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：颜色选择器、真实 AiCrudPage 渲染与样式 spacing 配置

### 变更范围

- 属性面板中涉及颜色的配置从下拉预设改为 `n-color-picker`，包含组件背景色、组件边框色、表单背景色、表单边框色，并保留“默认”快速清空。
- 表单列数从固定单/双/三/四列改为 1-6 滑块 + 数字输入，schema、画布、节点和布局工厂统一放宽到 6 列。
- 样式配置新增 padding/margin 四边编辑；窄面板里的数字输入关闭加减按钮并加宽表单列数数字区，修复用户反馈的“只能看见加号和减号，中间数字看不见”。
- CRUD 区块设计态从假表格预览改为真实 `AiCrudPage` lazy 渲染，基于区块子字段生成 `searchSchema`、`editSchema` 和 `columns`，并传入 `apiConfig`、`rowKey`、`crudOptions`。
- 左侧组件库对齐 `AiFormItem` 已支持能力，补充颜色选择、滑块、评分、按钮单选、级联、树选择、远程选择、日期/时间范围、年月和文本展示模板。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

首次结果：失败。原因是手工 patch 引入 tab 缩进，以及 `ForgeFormCanvasNode.vue` 两处三元表达式不符合项目 multiline-ternary 风格。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint --fix src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，自动修复缩进和三元表达式格式。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 18s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器交互、组件渲染和样式配置。
- 未启动浏览器做人工拖拽实测：本轮使用目标 eslint、diff 检查和生产构建覆盖模板/脚本/打包链路。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：属性面板飞书式结构与 AiForm/AiCrudPage 配置补全

### 变更范围

- 重做 `ForgePropertyPanel` 信息结构：顶部标题与关闭按钮、线型 tabs、纵向配置项，按“基础配置 / 样式配置 / 布局 / 状态 / CRUD”分组。
- 表单属性对齐 `AiForm` 已有能力：表单大小、列数、标签位置、标签对齐、标签宽度、行列间距、校验反馈、隐藏必填星号、行内反馈、表单 class 和自定义 style。
- 组件属性增加通用配置：占位提示、默认值、组件尺寸、可清空、显示反馈、说明文本、角标。
- 样式配置增加背景色、透明度、圆角、边框样式、边框颜色、阴影、宽度模式、高度模式和自定义 CSS style。
- 画布节点实时应用 `props.__designerStyle` 中的 customStyle、width、height、minHeight、backgroundColor、borderColor、borderStyle、borderRadius、boxShadow、opacity。
- 画布表单实时应用 `schema.layout.formStyle`，并在 schema normalize 时保留 `formStyleText`。
- CRUD 区块默认配置补齐更多 `AiCrudPage` props：搜索表单、表格、编辑表单、弹窗、分页、工具栏、导入导出、渲染模式、表格边框/斑马纹/多选等。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 43s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器属性面板、设计态样式和前端 schema。
- 未启动浏览器做人工点击验证：本轮已用目标 eslint、diff 检查和生产构建覆盖模板、脚本和打包链路；仍建议在设计器中人工确认样式配置实时反馈。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：右侧属性栏按需打开与 CRUD 区块配置

### 变更范围

- 右侧属性栏默认收起为窄轨入口；点击节点只显示顶部快捷操作，不再强制占用右侧宽度。
- 节点更多菜单的“配置”通过 `ForgeFormCanvasNode -> ForgeFormCanvas -> ForgeFormDesigner` 事件链打开右侧属性栏；属性栏打开后切换选中节点会继续更新配置内容。
- 右侧属性面板改为紧凑工具面板，支持收起、选中摘要、基础/布局/状态/CRUD 分组。
- 左侧组件库改为更密集的工具箱样式，组件分组带数量，组件按钮和字段列表统一边框、灰阶和 hover 反馈。
- `crudBlock` 从伪装成 `card` 的模板改为独立布局组件，默认带 `AiCrudPage.apiConfig` 风格配置：`list/detail/add/update/delete`、`rowKey`、搜索/分页/导入导出/弹窗等选项。
- `crudBlock` 设计态画布增加系统 CRUD 预览外壳，并继续保留内部查询字段/表格布局子节点投放能力。
- `forgeToFormCreate` 将 `crudBlock` 回退转换为 `elCard`，避免旧 FormCreate 预览链路识别未知组件。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js src/views/app-center/components/designer/form-first/forgeToFormCreate.js
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js forge-admin-ui/src/views/app-center/components/designer/form-first/forgeToFormCreate.js
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 30s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器组件、Schema 工具和 FormCreate 回退转换。
- 未做浏览器点击验证：本轮已用目标 eslint、diff 检查和生产构建覆盖模板/脚本/打包链路，仍建议人工进入表单设计器确认右侧栏打开/切换节点/CRUD 配置交互。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：三列窄节点工具条防重叠

### 变更范围

- 为画布节点启用 `container-type: inline-size`，工具条按节点自身宽度响应，而不是按全屏宽度判断。
- 节点宽度小于 `360px` 时，拖拽手柄从顶部居中移动到左上角，右侧保留必填开关、复制、删除和更多菜单，解决三列布局下工具重叠。
- 节点宽度小于 `280px` 时隐藏“必填”文字，仅保留开关本体，进一步降低窄列拥挤。

### 执行命令与结果

```bash
cd forge-admin-ui
pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 12s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm build 实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器节点工具条样式。
- 未做浏览器截图：本轮完成目标 eslint、diff 检查和生产构建；仍建议在三列表单画布中人工确认 hover 工具条间距。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：节点外露操作改为开关和图标

### 变更范围

- 拖拽手柄的 6 点 `DragOutlined` 图标旋转为横向显示。
- 节点顶部外露“必填”从文字按钮改为开关样式，点击后仍更新 `validation.required` 和 `requiredMessage`。
- 节点顶部外露“复制”“删除”从文字按钮改为图标按钮，分别使用 `CopyOutlined` 和 `DeleteTrashOutlined` SVG。
- 更多菜单继续保留配置、必填、复制、背景色、描边和删除等文字操作。

### 执行命令与结果

```bash
cd forge-admin-ui
pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 23s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm build 实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器节点工具条。
- 未做浏览器拖拽实测：本轮完成目标 eslint、diff 检查和生产构建。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：拖拽 clone 固定定位与落点乱跳修正

### 变更范围

- 修复拖动 clone 不动的关键原因：`cloneNode()` 复制出的节点仍带 `.canvas-node` 和 scoped 属性，原节点样式里的 `position: relative` 优先级可能压过 `.drag-follow-clone` 的 `position: fixed`；本轮改为在创建 clone 时写入内联 `position: fixed/top/left/zIndex/pointerEvents/transition/willChange`，确保 clone 一定按 pointer 坐标移动。
- 拖拽手柄开启会话时调用 `setPointerCapture()`，并把 `pointermove/pointerup/pointercancel` 监听挂到 `window` 捕获阶段，避免事件被表单控件或节点内部结构截断。
- 移除落点半区判断的迟滞逻辑，落点每次只根据当前鼠标在目标节点的上下半区计算，避免沿用上一次位置导致定位块乱跳。
- 鼠标没有命中节点或顶部 drop zone 时不再猜测“末尾落点”，避免定位块跳到和鼠标不一致的位置。

### 执行命令与结果

```bash
cd forge-admin-ui
pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 25s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm build 实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器拖拽交互。
- 未做浏览器拖拽实测：本轮完成目标 eslint、diff 检查和生产构建；真实鼠标手感仍需在页面中人工确认。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：画布内节点改为 Pointer 拖拽

### 变更范围

- 画布内已有节点移动不再走浏览器原生 HTML5 Drag and Drop，改为拖拽手柄 `pointerdown` 开启自管拖拽会话。
- 拖动中的元素 clone 由 `pointermove` 直接用 `translate3d()` 跟随鼠标移动，解决“只有灰色定位块动，被拖动元素不动”的问题。
- 每个节点补充 `data-forge-node-id`、`data-forge-parent-id`、`data-forge-index`，pointer 移动时用 `elementFromPoint()` 直接计算当前落点。
- 松手时根据当前 pointer 落点一次性调用 `moveDesignerComponent()`，减少原生 drop 丢失导致“有时换位置不生效”的情况。
- 拖动期间给 body 增加 `forge-pointer-dragging`，统一 `cursor: grabbing` 并禁用文本选择，提升拖动顺滑度。
- 左侧字段/布局拖入仍保留原生拖拽逻辑，画布节点的 native `draggable` 关闭，避免两套拖拽机制互相干扰。

### 执行命令与结果

```bash
cd forge-admin-ui
pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue
```

结果：通过，无输出。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 10s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm build 实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器拖拽交互。
- 未做浏览器拖拽实测：本轮完成目标 eslint、diff 检查和生产构建；真实鼠标手感仍需在页面中人工确认。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：跟手镜像不动与拖拽错位修复

### 变更范围

- 修复 `node-wrap` 的 `dragover.stop` 截断事件冒泡后，跟手镜像收不到 `document dragover` 的问题：镜像位置监听改为捕获阶段。
- 在节点本体补充 `@drag="handleDrag"`，拖动过程中同时从源节点 drag 事件更新镜像位置，确保被拖动元素和鼠标同步移动。
- 拖拽起点偏移改为使用拖拽手柄 `pointerdown` 时记录的坐标计算，不再只依赖 `dragstart.clientX/clientY`，减少镜像与鼠标错位。
- `drag` 结束阶段浏览器可能给出 `0,0` 坐标，本轮增加保护，避免镜像瞬间跳到页面左上角。

### 执行命令与结果

```bash
cd forge-admin-ui
pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 22s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm build 实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器拖拽事件链。
- 未做浏览器拖拽实测：本轮完成目标 eslint、diff 检查和生产构建；真实鼠标手感仍需在页面中人工确认。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：单一落点块、跟手镜像与拖动源可见性修复

### 变更范围

- 新增 `designerDragState.js`，用共享 `designerDropKey` 保证画布同一时间只显示一个落点块，避免 root 顶部落点和节点前后落点同时出现造成“双灰块”。
- 节点 `dragleave` 不再立即清空落点，落点状态由下一次 `dragover/drop/dragend` 更新，避免表单控件子 DOM 反复触发 leave/enter 导致闪动。
- 拖动源节点不再隐藏到接近透明，改为保持可见的轻量 dragging 态；同时新增固定定位的 `drag-follow-clone` 跟随鼠标，原生 drag image 用透明 1px 元素遮掉，避免“只剩定位块在动”。
- 落点块改为单层、平面、灰色虚线边框，无偏移阴影、无伪元素；高度使用被拖节点真实高度，不再压缩到 260px。
- 左侧字段/布局拖拽结束时同步清理共享落点状态，避免取消拖拽后画布残留灰块。

### 执行命令与结果

```bash
cd forge-admin-ui
pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/designerDragState.js
```

结果：通过，无输出。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerDragState.js
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 54s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm build 实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器拖拽交互与样式。
- 未做浏览器拖拽实测：本轮完成目标 eslint、diff 检查和生产构建；仍需在本地页面人工确认真实鼠标拖拽手感。

### 服务清理

- 本轮未启动新的服务。
- 检查到 `http://localhost:4173/forge` 已有 Vite preview 监听，PID `72372`；该服务不是本轮启动，未停止。

## 2026-06-16：真实表单预览、组件库扩展与侧栏收起

### 变更范围

- 中间画布字段节点改为复用 `AiFormItem` 渲染真实表单控件，不再常驻展示字段编码、组件标题和设计器卡片头。
- 输入框、数字、下拉、日期等控件在设计态可输入/选择，预览值仅保存在设计器本地，不写回业务 Schema。
- 拖拽手柄改为悬浮操作层，只有 hover/选中时显示；节点本体不再整块 draggable，避免误拖。
- 根落点和节点前后落点默认隐藏，不再 hover 满屏出现“放在这里”；拖拽进入目标位置时显示灰色同尺寸占位块和偏移阴影。
- 左侧组件库新增基础组件分组：输入框、多行文本、数字、金额、字典下拉、单选、多选、日期、日期时间、开关、人员、部门、文件上传、图片上传。
- 新增基础组件拖入工厂 `createForgeFieldTemplateComponent()`，拖入后生成可发布的新字段节点。
- 主设计器新增左侧组件库收起/展开按钮，收起后画布区域获得更多宽度。
- 右侧属性面板由长列表调整为“基础 / 布局 / 状态”页签。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 17s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮仍只涉及前端设计器体验和前端构建。
- 未做浏览器拖拽实测：需要在现有 preview 或 dev server 中进入 `/app-center/object/crm_follow_record?suiteCode=IN_OUT` 进行人工交互确认。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：拖拽恢复、灰色落点命中区与组件面板增强

### 变更范围

- 修复画布节点“改完后拖拽不了”：节点本体恢复 `draggable="true"`，但 `dragstart` 只允许从 6 点拖动手柄触发，避免整块误拖。
- `dragenter` / `dragover` 阶段改为只判断 `dataTransfer.types`，payload 只在 `drop` 阶段读取，避免浏览器限制导致落点无法激活。
- 根落点和节点前后落点从 0 高度改为不可见命中区，拖入后展开灰色同尺寸占位块，并保留偏移投影表达“双胞胎落地”。
- 画布拖动手柄和字段资产拖动手柄统一改为 `DragOutlined` 6 点 SVG。
- 复制/删除工具条固定在节点右上角内部，避免被顶出或挤出画布。
- 左侧组件库调整为“输入 / 选择 / 业务 / 布局与业务区块”分组，新增表格布局和 CRUD 区块模板入口。
- 右侧属性面板调整为更紧凑的工具面板样式，选中摘要和属性分区更清晰。
- `table/tableGrid` 的设计器投放规则补齐：表格布局只接收表格单元格，表格单元格可接收字段和布局子项。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `3m 59s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器交互、样式和前端 schema 工具。
- 未做浏览器拖拽实测：当前环境未启动新的 dev/preview 服务；本轮已覆盖目标 eslint、diff 检查和生产构建。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：画布节点顶部手柄、飞书式菜单与同高灰色落点修正

### 变更范围

- 修复中间画布表单元素仍不能拖拽的问题：`dragstart` 可能由外层 `article` 触发，不能只检查 `event.target.closest('.drag-handle')`；本轮改为在拖拽手柄 `pointerdown` 时记录状态，随后外层节点 `dragstart` 放行。
- 节点悬浮操作层调整到元素顶部居中，包含“更多操作”和 6 点 `DragOutlined` 拖拽手柄，贴近飞书组件顶部工具条布局。
- 新增飞书式更多菜单：配置、复制、更换背景色、背景描边、移入（禁用占位）、删除；点击“配置”会选中组件并显示右侧属性栏。
- 拖动源节点本身改成灰色虚线占位块，隐藏内部表单内容，保留原位置指引。
- 拖动时把当前拖拽元素高度写入 `--forge-designer-drag-height`，根落点和节点前后落点使用该高度渲染，形成和被拖动元素同高的灰色虚线落点块。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `2m 21s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器交互与样式。
- 未做浏览器拖拽实测：本轮使用目标 eslint、diff 检查和生产构建覆盖模板/脚本/打包链路。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：落点灰块跟随指针与节点内图标位置修正

### 变更范围

- 落点指示从“进入细 drop-line 才显示”改为节点本体 `dragover` 计算：鼠标在节点上半区显示前置灰块，下半区显示后置灰块，松开后按当前灰块位置执行移动。
- 保留容器中部投放：拖到可接收子项的容器中间区域时仍显示容器内部投放态，拖到容器上下区域则显示前/后落点灰块。
- 删除落点灰块上的“松开后放到这里 / 松开后放到顶部”文案，灰色虚线块本身承担落点指引。
- `node-overlay` 内图标拆开定位：6 点拖拽图标固定在节点框内顶部居中，更多操作图标固定在节点框内右上角，不再两个图标挤在同一组里。
- drop 后统一清理当前节点的 `beforeActive`、`afterActive`、`activeInside` 状态，避免灰块残留。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `3m 13s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器交互与样式。
- 未做浏览器拖拽实测：本轮使用目标 eslint、diff 检查和生产构建覆盖模板/脚本/打包链路。

### 服务清理

- 本轮未启动新的服务。

## 2026-06-16：落点闪动修复与源位置占位移除

### 变更范围

- 修复拖动时灰色落点块闪动、卡顿：drop-line 不再接收拖拽事件，改为整个 `node-wrap` 处理 `dragover/drop`，灰块只负责显示并设置 `pointer-events: none`，避免灰块展开后抢走鼠标事件导致反复 `dragleave/dragenter`。
- 灰色落点块继续作为真实占位参与布局，高度跟随当前拖动元素，展开后会把目标元素挤开，表达松开后的落点。
- 源位置不再显示灰色虚线块：拖动源节点改为透明，不保留一个同尺寸灰块。
- 保留节点上半/下半区域判断：上半区显示前置落点，下半区显示后置落点；可接收子项的容器中部仍显示内部投放态。

### 执行命令与结果

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过。命令前置的 `nvm use` 仍打印 `N/A: version "N/A" is not yet installed.`，但 eslint 实际执行并返回 0。

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue
```

结果：通过，无输出。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：通过，Vite 构建完成，耗时约 `1m 40s`。

### 警告项

- 构建仍存在既有 CSS `//` 注释警告：`Comments in CSS use "/* ... */" instead of "//"`。
- 构建仍存在既有 `src/store/index.js` 动态导入与静态导入混用 chunk 警告。
- `nvm use v20.19.0` 仍打印 `N/A` 未安装提示，但 pnpm 命令实际执行成功。

### 跳过项

- 未执行后端验证：本轮只涉及前端设计器交互与样式。
- 未做浏览器拖拽实测：本轮使用目标 eslint、diff 检查和生产构建覆盖模板/脚本/打包链路。

### 服务清理

- 本轮未启动新的服务。
