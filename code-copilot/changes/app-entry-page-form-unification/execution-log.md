# 执行日志：app-entry-page-form-unification

## 2026-06-20

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/AppEditorDrawer.vue 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/designer/BusinessFormDesigner.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/views/app-center/components/designer/BusinessActionDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/ai/crud-page.vue src/components/ai-form/AiCrudPage.vue
```

结果：通过。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 后端编译

命令：

```bash
JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH="/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:${PATH}" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

执行目录：`forge-server`

结果：通过。

备注：首次使用未引用的 `PATH` 内联赋值时，本机 IntelliJ Maven 路径包含空格导致 shell 拆分失败；已用带引号的 `PATH` 重跑并通过。

### 空白字符检查

命令：

```bash
git diff --check
```

结果：通过。

## 2026-06-21 表单/列表栅格嵌套容器与预览一致性修复验证

### 变更范围

- 列表页栅格落点解析改为从 DOM 当前点向上寻找最近的可接收容器，优先命中栅格内的卡片/标签页，再回退到外层 grid cell。
- 列表页已有组件支持移动到卡片/标签页容器；栅格子组件手柄改为纯 pointer 拖动，避免浏览器原生 drag 与自定义拖动互相干扰。
- 列表页卡片/标签页递归渲染子节点时透传选中和子节点事件，保证嵌套设计链路能继续工作。
- 表单设计器 row/col 设计态补齐 rowGap，并降低 row/col 多层边框噪声；运行预览继续通过 `AiFormLayoutNodes` 使用 24 栅格 span/gutter。

### 前端定向 ESLint

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/ai-form/AiFormLayoutNodes.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue
```

结果：通过。

### 前端构建

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui build
```

结果：通过，最终复跑输出 `✓ built in 1m 5s`。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue
```

结果：通过。

### 未覆盖项

- 未启动浏览器做真实拖拽录屏/截图验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。
- 本轮未启动任何服务，无需清理服务进程。

## 2026-06-21 列表栅格内部拖拽落点降噪验证

### 变更范围

- `ListPageGridDesigner.vue` 向 `GridBlockRenderer.vue` 透传 `nestedMovingBlockId`，使栅格内部能准确识别正在拖动的子组件。
- `GridBlockRenderer.vue` 在栅格拖动状态下隐藏普通 cell 边框、子组件 hover/selected 边框和拖动源操作层，避免多层边框叠加。
- 栅格目标 cell 落点改为单一浅蓝投放层，去掉高噪声斜纹背景，增强拖动时的落点辨识度。

### 前端定向 ESLint

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue
```

结果：通过。

### 前端构建

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 2m 43s`。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue
```

结果：通过。

### 未覆盖项

- 未启动浏览器做真实拖拽截图验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。
- 本轮未启动任何服务，无需清理服务进程。

## 2026-06-21 列表栅格放置后提示层遮挡修复验证

### 变更范围

- `GridBlockRenderer.vue` 新增栅格 cell 显示判断，已有 children 的 cell 不再渲染整块空状态/落点覆盖层。
- 空状态 `拖入组件` 仅在 cell 真实为空且不是当前 active drop cell 时显示。
- 栅格内容层级提升到提示层之上，避免拖拽状态清理延迟一帧时覆盖刚放入的组件。

### 前端定向 ESLint

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue
```

结果：通过。

### 前端构建

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 1m 6s`。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue
```

结果：通过。

### 未覆盖项

- 未启动浏览器做真实拖拽截图验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。
- 本轮未启动任何服务，无需清理服务进程。

## 2026-06-21 栅格拖拽丝滑度与配置入口修复验证

### 变更范围

- 列表页栅格内子组件手柄新增 pointer 拖动链路，拖动时通过 `elementFromPoint` 解析当前 cell/画布落点，松手后一次性移动。
- 列表页栅格内子组件拖动过程复用外层画布 drop preview 和 cell 高亮，减少原生 HTML5 drag/drop 带来的断续感。
- 表单设计器 pointer 拖动普通组件经过 row 区域时优先映射到具体 col，避免普通组件被插成 row 的直接子节点。
- 表单设计器选中 row/col 时在基础配置前置“栅格快捷配置”，常用总列数、格子数量、列间距、span 不再只藏在布局 tab。

### 前端定向 ESLint

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue
```

结果：通过。

### 前端构建

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 1m 1s`。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue code-copilot/changes/app-entry-page-form-unification/test-spec.md code-copilot/changes/app-entry-page-form-unification/execution-log.md
```

结果：通过。

### 未覆盖项

- 未启动浏览器做真实拖拽录屏/截图验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。
- 未启动本地后端；本轮不涉及后端接口和数据库变更。

## 2026-06-20 页面/表单栅格拖拽一致性修复验证

### 变更范围

- 页面设计器栅格子组件拖拽开始事件向外层画布透传，外层可识别已有块拖拽状态。
- 页面栅格子组件拖出栅格时可落回普通画布，并保留原组件配置、按落点生成顶层块。
- 表单设计器栅格 row/col 调整为 24 栅格语义，默认 4 格、每格 span=6。
- 表单属性面板拆分 row 总列数、格子数量、列间距和每列 span。
- 表单 row 接收普通字段/组件时自动投放到具体 col，pointer 拖拽已有组件时同样映射到目标 col。

### 前端定向 ESLint

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过。

备注：首次执行发现 `handleDropToTarget` 重构后仍引用旧 `index` 变量、模板缩进和 CSS 换行格式问题；已修复后重跑通过。

### 前端构建

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 58.76s`。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check -- forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvas.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormCanvasNode.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js
```

结果：通过。

### 未覆盖项

- 未启动浏览器做真实拖拽录屏/截图验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。
- 未启动本地后端；本轮不涉及后端接口和数据库变更。

## 2026-06-20 栅格子组件设计态控制补齐

### 变更范围

- 页面设计器栅格 cell 内子组件补齐设计态外壳：选中态、拖拽手柄、更多操作按钮和 resize 锚点。
- 栅格内子组件支持复制、删除；复制会递归生成新 id，避免与原组件选中和更新串联。
- 已放入栅格的子组件可通过手柄拖拽移动到其他栅格 cell，拖拽经过目标 cell 时显示高亮落点。
- 栅格内子组件默认宽度保持 100%，高度改为按组件默认高度排布；resize 锚点可调整子组件尺寸。

### 前端定向 ESLint

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue
```

结果：通过。

### 前端构建

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check
```

结果：通过。

### 未覆盖项

- 未启动浏览器做真实拖拽、移动到其他 cell、更多菜单和 resize 锚点交互验证。

## 2026-06-20 栅格配置可理解性与落点反馈补齐

### 变更范围

- 页面设计器属性面板中，栅格结构、格子样式、格子内容提前到区块标题后，避免用户在通用外观配置后面查找核心布局配置。
- 栅格配置控件改为显式标签：总列数、列间距、组件行距、最小高度、垂直位置、水平位置、显示格子边框。
- “拉伸/起始/结束”改为“垂直填满/靠上/靠下”和“水平填满/靠左/靠右”，开关文案明确为“显示格子边框”。
- 栅格 cell 拖拽命中时恢复蓝色背景落点提示“释放到此格”，与外层画布落点反馈保持一致。
- 栅格内子组件宽度外壳跟随固定宽度样式，调整右/左侧锚点后可见宽度会变化；默认仍保持 `width: 100%`。

### 前端定向 ESLint

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue
```

结果：通过。

### 前端构建

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check
```

结果：通过。

### 未覆盖项

- 未启动浏览器做真实配置面板排序、拖拽落点和宽度锚点视觉验证。

## 2026-06-20 22:34 CST CRUD 预览宽度与 resize 请求风暴修复

### 变更范围

- `GridBlockRenderer.vue`：修正低代码画布中 `AiCrudPage` 预览容器样式，外层块保持 `width: 100%`，不再通过 `width: max-content` 按表格内容撑开，避免出现 `n-scrollbar-current-width: 500000px` 和搜索输入框异常拉长。
- `AiCrudPage.vue`：表格默认 `scroll-x` 计算同时统计列 `width` 和 `minWidth`，让表格在自身内部横向滚动，不把整个 CRUD 页面撑宽。
- `AiCrudPage.vue`：`publicParams/publicQuery` 监听改为稳定内容签名，避免设计器拖拽宽高时仅对象引用变化导致重复 `loadList()`。

### 前端定向 ESLint

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/ai-form/AiCrudPage.vue
```

结果：通过。

备注：用户偏好中的 `v20.19.0` 本机未安装，本轮使用同为 Node 20 的 `v20.19.5`；`COREPACK_HOME` 指向 `/private/tmp/corepack`，避免 pnpm 尝试写入用户目录时报 `EPERM`。

### 前端构建

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 1m 5s`。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check
```

结果：通过。

### 未覆盖项

- 未启动浏览器做实际拖拽宽度和网络面板验证；本轮通过代码路径、定向 ESLint、生产构建和空白检查验证。

## 2026-06-20 22:54 CST 树折叠与栅格布局组件

### 变更范围

- `ListPageGridDesigner.vue`：树面板折叠状态从只读态扩展到设计态，外层 `grid-item` 按 `TREE_PANEL_COLLAPSED_WIDTH` 收窄，并继续联动右侧主区块视觉宽度。
- `page-schema.js`：新增 `grid-layout` 页面组件，默认 4 列 1 行；保存/同步时清洗栅格 cell children。
- `GridBlockRenderer.vue`：新增栅格布局渲染，每个 cell 可作为拖拽落点，cell 内子组件按 100% 宽度渲染。
- `ListPageGridDesigner.vue`：属性面板新增栅格布局配置，支持列数、行数、间距、最小高、水平/垂直对齐、边框、背景、格子增删和格子内子组件管理。

### 前端定向 ESLint

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js
```

结果：通过。

### 前端构建

命令：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.5 && COREPACK_HOME=/private/tmp/corepack pnpm --dir forge-admin-ui build
```

结果：通过，`✓ built in 1m 17s`。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check
```

结果：通过。

### 未覆盖项

- 未启动浏览器做实际树折叠和栅格 cell 拖拽验证；本轮通过代码路径、定向 ESLint、生产构建和空白检查验证。

## 2026-06-20 运行页新增/编辑表单虚拟组件保真修复

### 问题定位

- 本地调用 `GET /ai/crud-config/render/in_out_crm_follow_record` 并附带 `X-Inner-Call: true` 验证，后端 `options.formDesignerSchema` 已返回当前设计器 schema，包含 `分组标题333` 和按钮组件。
- 运行页 `crud-page.vue` 从 `formDesignerSchema` 生成 `editFormLayout` 时，只保留绑定字段或有子节点的组件，导致无字段绑定的标题、按钮等独立组件被丢弃。

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue
```

结果：通过。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 未覆盖项

- 未启动浏览器做新增/编辑弹窗截图比对；本轮通过后端 render 接口实测、定向 ESLint 和前端构建验证。

## 2026-06-20 左树右表画布宽度联动修复

### 变更范围

- 列表画布左树右表布局同步不再用固定 3 栅格判定主区块风险，避免右表调整时把左树宽度还原为默认宽度。
- 左树宽度调整后，右侧 `AiCrudPage`、`AiTable`、`data-table`、查询区和工具栏等 `100%` 主区块会自动贴到左树右侧，并按画布剩余宽度填充。
- `100%` 宽度区块的外层绝对定位改为 `left + right: 0`，不再输出固定 `width: xxxpx`，确保画布宽度变化时真实自适应。

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/page-schema.js
```

结果：通过。

备注：首次失败，原因是 `page-schema.js` 中宽度对齐函数误用了外层 `source.designWidth`，已改为向 `normalizeGridItemsForLayout` 显式传入 `designWidth` 后重跑通过。

补充命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue
```

结果：通过。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check
```

结果：通过。

### 未覆盖项

- 未启动浏览器做左树右表拖拽截图验证；本轮通过定向 ESLint、生产构建和空白检查验证。

## 2026-06-20 表单保真与发布阻断收敛修复

### 变更范围

- 新版表单画布 `ForgeFormDesigner.flushDesigner()` 改为使用 `normalizeFormDesignerSchemaForSave()` 输出，保留 `defaultFormKey` 和 `settings.formAssets`，避免保存/发布后运行态新增、编辑弹窗与设计器当前表单不一致。
- 发布检查新增阻断项白名单：字段基础合法性、页面协议、页面/表单目标引用、表单字段绑定、视图字段、运行配置、入口目标、表结构仍会阻断；动作参数、预览接口、表单治理、关系、联动、单据、公式、入口敏感参数等非页面必要项降级为提醒。

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue
```

结果：通过。

### 后端编译

命令：

```bash
JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

执行目录：`forge-server`

结果：通过，`BUILD SUCCESS`。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectPublishService.java
```

结果：通过。

### 未覆盖项

- 未启动本地后端、数据库和浏览器做端到端新增/编辑弹窗比对。
- 当前工作树存在其他变更，本轮只验证表单保真与发布阻断收敛相关文件。

## 2026-06-20 运行页新增/编辑表单严格按设计器画布渲染

### 变更范围

- 修复 `crud-page.vue` 中存在 `formDesignerSchema` 时仍把 `baseEditSchema` 未使用字段追加回 `editSchema` 的问题。
- 修复后，运行态新增/编辑弹窗以设计器画布组件为准；只有没有设计器 schema 或设计器字段为空时才回落到默认 `editSchema`。

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue
```

结果：通过。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue
```

结果：通过。

## 2026-06-20 发布接口加密与日志脱敏修复

### 变更范围

- 前端业务对象设计器保存、字段、布局、动作和发布接口显式传入 `{ encrypt: true }`。
- 请求拦截器对显式加密接口在缺少会话密钥时自动触发密钥协商，协商失败则阻止明文请求。
- `encryptRequest()` 对显式加密接口不再吞掉缺密钥或加密异常，避免失败后继续发送明文。
- SM4 `hexToBase64/base64ToHex` 改为分块转换，修复大 JSON 加密时 `Maximum call stack size exceeded`。
- 操作日志切面对 `@ApiDecrypt` 接口的 `@RequestBody` 解密对象记录为 `[DECRYPTED_REQUEST_BODY_OMITTED]`，保留路径参数，避免 `sys_operation_log.request_params` 写入完整明文发布 DTO。

### SM4 大报文验证

命令：

```bash
node -e "const { sm4 } = require('./forge-admin-ui/node_modules/sm-crypto'); const CHUNK=0x8000; function hexToBase64(hexString){const chunks=[];let binary='';for(let i=0;i<hexString.length;i+=2){binary+=String.fromCharCode(Number.parseInt(hexString.slice(i,i+2),16));if(binary.length>=CHUNK){chunks.push(binary);binary='';}}if(binary)chunks.push(binary);return btoa(chunks.join(''));} function base64ToHex(base64String){const raw=atob(base64String);const chunks=[];let result='';for(let i=0;i<raw.length;i++){const hex=raw.charCodeAt(i).toString(16);result+=(hex.length===2?hex:'0'+hex);if(result.length>=CHUNK){chunks.push(result);result='';}}if(result)chunks.push(result);return chunks.join('');} const key='0123456789abcdeffedcba9876543210'; const text=JSON.stringify({x:'a'.repeat(300000)}); const enc=hexToBase64(sm4.encrypt(text,key)); const dec=sm4.decrypt(base64ToHex(enc),key); console.log(JSON.stringify({plain:text.length, encrypted:enc.length, roundTrip:dec===text}));"
```

结果：通过，输出 `{"plain":300008,"encrypted":400024,"roundTrip":true}`。

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/utils/crypto/sm4.js src/utils/crypto/crypto-interceptor.js src/utils/http/interceptors.js src/api/business-app.js
```

结果：通过。

### 后端日志模块编译

命令：

```bash
JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-starter-parent/forge-starter-log -am compile -DskipTests
```

执行目录：`forge-server`

结果：通过，`BUILD SUCCESS`。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过，`built in 1m 32s` 和复跑 `built in 1m 46s`。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check -- forge-admin-ui/src/utils/crypto/sm4.js forge-admin-ui/src/utils/crypto/crypto-interceptor.js forge-admin-ui/src/utils/http/interceptors.js forge-admin-ui/src/api/business-app.js forge-server/forge-framework/forge-starter-parent/forge-starter-log/src/main/java/com/mdframe/forge/starter/log/aspect/OperationLogAspect.java
```

结果：通过。

### 未覆盖项

- 未重启后端服务重新点击发布验证数据库中新写入的 `sys_operation_log.request_params`，本轮以切面编译和代码路径验证为准。
- 用户贴出的 `/publish` 业务失败原因是发布检查阻断项，不是加解密失败；需在发布检查页修复阻断项后才能发布成功。

## 2026-06-20 UI 修复：属性栏与预览弹窗

### 变更范围

- 表单设计右侧属性栏内容区对齐列表设计右侧属性栏：浅灰蓝背景、卡片式表单项、蓝色标签标识和统一阴影。
- 表单预览弹窗从窄弹窗改为大工作区弹窗，内容区可滚动，顶部预览模式工具条在滚动时保持可见。
- 列表预览弹窗补齐 Naive Modal 全局样式覆盖，卡片内容区负责滚动，避免预览右侧被隐藏。
- 列表只读画布补白底，避免列表页过长时下方露出透明背景。

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/BusinessListDesigner.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue
```

结果：通过。

### 空白字符检查

命令：

```bash
git diff --check -- forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue
```

结果：通过。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 未覆盖项

- 未启动浏览器或 Playwright 做截图验证；本轮以构建、Lint 和空白检查验证。

## 2026-06-20 发布解密失败与预览横向滚动修复

### 问题定位

- `/ai/business/object/{objectId}/designer` 所在 Controller 类有 `@ApiDecrypt`，保存设计器时后端会解密请求体。
- 前端本地恢复了 `crypto_session_key` 和 `crypto_exchanged=true` 后，可能在后端重启、Redis 清空或会话密钥过期后继续用旧密钥加密请求。
- 后端拿不到动态会话密钥时会回落默认密钥，解密旧动态密钥请求得到 `null`，随后 `decryptedData.getBytes(...)` 触发 `NullPointerException`。
- 列表预览弹窗内容区使用 flex column 默认横向拉伸子元素，导致只读画布的宽内容被压回容器宽度，横向滚动条不稳定。

### 变更范围

- `key-exchange.js`：页面加载时不再恢复本地旧会话密钥，发现旧 `crypto_session_key/crypto_exchanged` 后清理，等待路由守卫重新协商。
- `DecryptRequestBodyAdvice`：解密结果为空时抛出明确的 `IOException`，避免 NPE，并提示密钥协商或加密格式问题。
- `BusinessListDesigner.vue`：列表预览弹窗内容区改为 `align-items: flex-start`，只读设计器改为不收缩的 `max-content` 宽度，横向滚动由弹窗内容区接管。

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/utils/crypto/key-exchange.js src/views/app-center/components/designer/BusinessListDesigner.vue
```

结果：通过。

备注：首次扫描发现 `key-exchange.js` 既有 `console.log` 和 JSDoc 警告，已一并修正后重跑通过。

### 后端编译

命令：

```bash
JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-starter-parent/forge-starter-crypto -am compile -DskipTests
```

执行目录：`forge-server`

结果：通过，`BUILD SUCCESS`。

备注：未指定 Java 17 时首次编译失败，错误为当前 shell Java 版本不匹配；使用项目 Java 17 路径后通过。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 空白字符检查

命令：

```bash
git diff --check -- forge-admin-ui/src/utils/crypto/key-exchange.js forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/advice/DecryptRequestBodyAdvice.java
```

结果：通过。

### 未覆盖项

- 未启动本地后端和浏览器做实际保存/发布点击验证。

## 2026-06-20 继续补齐

### 变更范围

- 应用入口抽屉补齐入口类型、入口权限码和结构化默认参数编辑。
- 表单属性面板补齐表单治理配置：权限、字段覆盖规则、表单事件。
- 运行态补齐入口默认参数承接：URL 公共参数进入列表查询，`formDefaultValues` / `submitDefaultParams` 进入表单默认值和提交固定参数。
- 运行态应用表单字段规则：隐藏、必填、只读、默认值。
- 表单事件运行态先支持请求型事件，覆盖打开表单前、提交前、提交成功后。
- 列表按钮补齐主点击动作配置；发布检查补齐无动作按钮、动作目标、请求地址、入口类型/权限、敏感参数和表单治理检查。

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/AppEditorDrawer.vue src/components/lowcode-builder/page/CrudDefaultParamsEditor.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue
```

结果：通过。

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js
```

结果：通过。

备注：按用户偏好尝试执行 `source ~/.nvm/nvm.sh && nvm use v20.19.0 ...` 时，本机 nvm 返回 `N/A: version "N/A" is not yet installed`，未进入 eslint；当前 shell 的 `node -v` 为 `v20.20.0`，用当前 Node + pnpm 重跑通过。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 后端编译

命令：

```bash
JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH="/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:${PATH}" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

执行目录：`forge-server`

结果：通过，`BUILD SUCCESS`。

### 空白字符检查

命令：

```bash
git diff --check
```

结果：通过。

### 未覆盖项

- 未启动本地后端和浏览器做真实交互验证；本轮以构建、编译和静态检查为准。
- 表单事件只执行请求型事件；字段联动、按钮动作、自定义脚本白名单执行和事件结果回填仍是后续待办。
- 表单草稿/发布版本隔离、真实预览仍未实现；响应式预览和按钮/行操作参数映射已在后续补齐，入口参数和表单事件结果回填仍待统一。

## 2026-06-20 继续补按钮与参数映射

### 变更范围

- 列表设计器动作参数映射从简单 `name/value` 扩展为 `sourceType/sourceField/value`：
  - 固定值
  - 当前行字段
  - 路由参数
  - 系统变量
- 自定义按钮/行操作补齐权限码、二次确认和成功后行为配置。
- 普通画布按钮运行态接入 click 事件，支持跳转、请求、确认提示和成功后行为。
- `AiCrudPage` 自定义动作运行态解析新参数映射协议，并兼容旧 `value` 占位符。
- 后端 `LowcodeRuntimeConfigBuilder` 保留动作参数的 `sourceType/sourceField`，保留 `permissionCode/successBehavior`。
- 发布检查补齐动作参数映射校验：空参数名、来源字段缺失、字段不存在、来源类型无效、固定值为空警告。
- 列表设计器补齐响应式预览预设：桌面、窄屏、弹窗、抽屉、移动。

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/ai-form/AiCrudPage.vue src/views/ai/crud-page.vue
```

结果：首次失败，原因是 `${route.xxx}` 和 `${system.xxx}` 字符串字面量触发 `no-template-curly-in-string`，改为通过 `resolveTemplatePlaceholder()` 生成占位符后重跑通过。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 后端编译

命令：

```bash
JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH="/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:${PATH}" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

执行目录：`forge-server`

结果：通过，`BUILD SUCCESS`。

### 空白字符检查

命令：

```bash
git diff --check
```

结果：通过。

### 未覆盖项

- 未启动浏览器验证点击链路；本轮通过构建、编译和静态检查验证。
- 普通画布按钮的 `refreshBlock/filterBlock` 跨区块调度还未实现；当前支持跳转、请求、确认和成功后返回/刷新页。
- 入口参数、表单事件结果回填尚未完全统一到同一套参数映射编辑器。

## 2026-06-20 继续补版本隔离、真实预览和事件闭环

### 变更范围

- 后端运行态渲染改为读取 `publishedVersion` 对应的 `ai_crud_config_version` 快照，避免草稿保存覆盖线上入口。
- 列表设计器真实预览从单开关升级为模拟数据、真实列表、新增表单、编辑表单、详情状态，支持记录 ID、请求状态和错误展示。
- 真实接口预览错误接入发布检查：最后一次预览失败阻断发布，开启真实预览但未成功验证给警告。
- 自定义按钮和普通画布按钮补齐显示条件；运行态按当前用户权限集合过滤 `permissionCode`。
- 表单事件补齐请求结果回填和自定义脚本白名单，发布检查阻断未登记脚本。
- 新增 `test-spec.md`，记录本变更增量验证范围和跳过项。

### 前端定向 ESLint

命令：

```bash
pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js src/components/ai-form/AiCrudPage.vue src/views/ai/crud-page.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue
```

结果：通过。

备注：首次失败，原因是显示条件解析使用正则触发 `regexp/no-super-linear-backtracking` 和 `regexp/no-dupe-characters-character-class`，以及事件结果映射链式调用格式触发 `antfu/consistent-chaining`；已改为手写解析并重跑通过。

### 后端编译

命令：

```bash
JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home PATH="/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.15-1/Contents/Home/bin:${PATH}" mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

执行目录：`forge-server`

结果：通过，`BUILD SUCCESS`。

### 前端构建

命令：

```bash
pnpm --dir forge-admin-ui build
```

结果：通过。

警告：
- CSS 中存在既有 `//` 注释，Vite/esbuild 提示 CSS 注释应使用 `/* ... */`。
- `src/store/index.js` 同时被动态和静态导入，Vite 提示不会拆到独立 chunk。

### 未覆盖项

- 未启动本地后端、数据库和浏览器做端到端点击验证。
- 入口参数和表单事件结果回填尚未抽成同一个 UI 映射编辑器；本轮已打通运行协议和发布校验。

### 空白字符检查

命令：

```bash
git diff --check
```

结果：通过。
