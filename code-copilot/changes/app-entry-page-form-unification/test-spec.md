# 测试规格：app-entry-page-form-unification

## 范围

本变更覆盖应用入口、列表设计器、表单设计器、运行态渲染、发布检查和低代码发布版本读取链路。

## P0 验证

- 前端定向 ESLint：覆盖列表设计器、GridBlockRenderer、page-schema、AiCrudPage、运行页和表单属性面板。
- 前端生产构建：确认 Vue 模板、路由运行页和低代码设计器能完成 Vite 构建。
- 后端生成器模块编译：确认发布版本快照读取、发布检查和 Mapper XML 能编译通过。
- `git diff --check`：确认无空白字符错误。

## P1 验证

- 有本地后端和测试数据时，手动验证：
  - 保存草稿后不发布，已发布入口仍显示旧版本表单/列表。
  - 列表设计器真实预览在 mock、真实列表、新增、编辑、详情模式下状态提示正确。
  - 真实预览失败后发布检查出现 `PAGE_PREVIEW_API_ERROR`。
  - 按钮权限码和显示条件在运行态隐藏不满足条件的操作。
  - 表单事件 request 的 `resultMapping` 能把接口响应回填到提交 payload。

## 本轮跳过项

- 未启动本地后端、数据库和浏览器做端到端点击验证；本轮以静态检查、前端构建和后端编译为验收证据。

## 2026-06-20 UI 修复增量验证

- 覆盖范围：表单设计右侧属性栏样式、表单预览弹窗、列表设计预览弹窗、列表只读画布白底。
- 必跑验证：
  - 定向 ESLint 覆盖本轮 4 个 Vue 文件。
  - 本轮相关文件 `git diff --check`。
  - `pnpm --dir forge-admin-ui build`。
- 跳过项：
  - 未启动浏览器做截图验证；当前环境只做构建、Lint 和空白检查闭环。

## 2026-06-20 发布解密与预览滚动修复验证

- 覆盖范围：
  - 前端密钥交换恢复逻辑，避免使用本地旧会话密钥导致 `/ai/business/object/{id}/designer` 解密失败。
  - 后端解密结果为空时的明确错误保护，避免 `NullPointerException`。
  - 列表预览弹窗横向滚动条。
- 必跑验证：
  - 定向 ESLint：`key-exchange.js`、`BusinessListDesigner.vue`。
  - 后端 `forge-starter-crypto` Java 17 编译。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动本地后端和浏览器做实际发布/保存点击验证。

## 2026-06-20 发布接口加密与日志脱敏增量验证

- 覆盖范围：
  - 显式 `encrypt: true` 的业务对象设计器保存、字段、布局、动作和发布接口。
  - 前端请求拦截器在无会话密钥时先触发密钥协商，协商失败则阻止明文请求。
  - SM4 大报文 Base64 转换改为分块处理，避免设计器大 JSON 加密时栈溢出。
  - 操作日志对 `@ApiDecrypt` 接口的 `@RequestBody` 解密对象做省略记录，避免 `sys_operation_log.request_params` 落完整明文设计器 JSON。
- 必跑验证：
  - SM4 30 万字符级别大报文加密/解密往返脚本。
  - 定向 ESLint：`sm4.js`、`crypto-interceptor.js`、`interceptors.js`、`business-app.js`。
  - 后端 `forge-starter-log` Java 17 编译。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未重新启动后端服务复测数据库 `sys_operation_log` 新记录；本轮通过切面编译和前端构建完成静态闭环。

## 2026-06-20 表单保真与发布阻断收敛增量验证

- 覆盖范围：
  - 新版表单画布 `flushDesigner()` 保留多表单元数据和默认表单 key，避免保存/发布后运行态新增、编辑弹窗取到旧表单或错误表单。
  - 发布检查仅保留会直接影响页面渲染、表单绑定、运行配置和表结构的阻断项；其他检查降级为提醒，不阻断发布。
  - 运行页存在 `formDesignerSchema` 时，新增/编辑弹窗不再把默认 `editSchema` 中未出现在画布上的字段追加回表单。
  - 运行页从 `formDesignerSchema` 生成布局时保留标题、按钮等无字段绑定的独立组件，确保应用页新增/编辑弹窗与表单设计器结构一致。
- 必跑验证：
  - 定向 ESLint：`ForgeFormDesigner.vue`。
  - 定向 ESLint：`crud-page.vue`。
  - 后端 `forge-plugin-generator` Java 17 编译。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动本地后端和浏览器做新增/编辑弹窗端到端比对；本轮通过构建、编译和静态检查验证。

## 2026-06-20 左树右表画布宽度联动增量验证

- 覆盖范围：
  - 左树右表布局同步不再用固定 3 栅格判定主区块风险，避免自定义左树宽度被自动还原。
  - 左树宽度调整时，右侧 `100%` 主区块自动贴到左树右侧并按画布剩余宽度填充。
- 必跑验证：
  - 定向 ESLint：`ListPageGridDesigner.vue`、`page-schema.js`。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动浏览器做实际拖拽截图验证；本轮通过静态检查和构建验证。

## 2026-06-20 CRUD 预览 100% 宽度与 resize 请求风暴增量验证

- 覆盖范围：
  - 低代码画布中 `AiCrudPage` 外层保持 `width: 100%`，不再用内部内容撑大组件宽度。
  - CRUD 预览外层不裁剪内容，表格横向滚动由 `AiCrudPage` 根据列 `width/minWidth` 自己计算。
  - `publicParams/publicQuery` 监听改为稳定内容签名，避免设计器拖拽宽高导致对象引用变化时反复调用列表接口。
- 必跑验证：
  - 定向 ESLint：`GridBlockRenderer.vue`、`AiCrudPage.vue`。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动浏览器做实际拖拽网络面板验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。

## 2026-06-20 树折叠与栅格布局组件增量验证

- 覆盖范围：
  - 筛选树折叠状态不再只收内部内容，设计态和只读态外层区块都会按折叠 rail 宽度显示，并联动右侧主区块视觉宽度。
  - 页面组件面板新增 `grid-layout` 栅格布局容器。
  - 栅格布局支持列数、行数、列间距、行间距、格子最小高、水平/垂直对齐、格子边框、格子背景和格子内容管理。
  - 拖拽组件到栅格具体 cell 后，cell 内子组件按 `width: 100%` 自适应。
- 必跑验证：
  - 定向 ESLint：`ListPageGridDesigner.vue`、`GridBlockRenderer.vue`、`page-schema.js`。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动浏览器做实际拖入栅格 cell 的可视化验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。

## 2026-06-20 栅格子组件设计态控制增量验证

- 覆盖范围：
  - 栅格 cell 内子组件选中后显示拖拽手柄、更多操作按钮和 resize 锚点。
  - 栅格内子组件更多菜单支持复制和删除，复制时递归重置子节点 id。
  - 已放入栅格的子组件可拖拽移动到其他栅格 cell，目标 cell 高亮提示落点。
  - 栅格内子组件 resize 支持递归更新样式，默认宽度保持 100%，高度按组件默认高度排布。
- 必跑验证：
  - 定向 ESLint：`ListPageGridDesigner.vue`、`GridBlockRenderer.vue`、`page-schema.js`、`ForgePropertyPanel.vue`。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动浏览器做真实拖拽和锚点拖动验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。

## 2026-06-20 栅格配置可理解性与子组件宽度增量验证

- 覆盖范围：
  - 栅格结构、格子样式、格子内容配置提前到属性面板顶部。
  - 栅格样式控件改为带明确标签的配置项：总列数、列间距、组件行距、最小高度、垂直位置、水平位置、显示格子边框。
  - 拖拽到栅格 cell 时恢复蓝色背景落点提示。
  - 栅格内子组件拖动宽度锚点后，外壳宽度按固定宽度显示，不再被 `max-width: 100%` 卡住。
- 必跑验证：
  - 定向 ESLint：`ListPageGridDesigner.vue`、`GridBlockRenderer.vue`、`page-schema.js`、`ForgePropertyPanel.vue`。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动浏览器做真实属性面板和拖拽锚点视觉验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。

## 2026-06-20 页面/表单栅格拖拽一致性增量验证

- 覆盖范围：
  - 页面栅格内已有子组件拖拽时，外层画布能识别已有块并显示普通画布落点；拖出栅格后转为顶层块。
  - 页面栅格内已有子组件仍可拖到其他栅格 cell，目标格子继续显示高亮落点。
  - 表单设计器栅格 row 改为 24 栅格语义，默认 4 个格子、每格 span=6。
  - 表单栅格属性面板拆分为总列数、格子数量、列间距、每格 span，避免列数和 span 混用。
  - 表单栅格 row 接收普通字段/组件时自动投放到目标格子，已有组件 pointer 拖动也会从 row 映射到具体 col。
- 必跑验证：
  - 定向 ESLint：`ListPageGridDesigner.vue`、`GridBlockRenderer.vue`、`ForgeFormCanvas.vue`、`ForgeFormCanvasNode.vue`、`ForgePropertyPanel.vue`、`designerLayoutFactory.js`、`formDesignerSchema.js`。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动浏览器做真实拖拽录屏/截图验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。

## 2026-06-21 栅格拖拽丝滑度与配置入口增量验证

- 覆盖范围：
  - 列表页栅格内子组件手柄接入 pointer 拖动，不再依赖浏览器原生 drag/drop；拖动过程中持续显示栅格 cell 或外层画布落点。
  - 列表页栅格内子组件松手时按当前鼠标位置移动到目标 cell 或转为画布顶层块，拖动过程不实时写 layout，降低卡顿和副作用。
  - 表单设计器 pointer 拖动普通组件经过 row 区域时强制映射到具体 col，避免组件变成 row 的直接子节点。
  - 表单设计器选中 row/col 时在基础配置中前置“栅格快捷配置”，减少查找成本。
- 必跑验证：
  - 定向 ESLint：`ListPageGridDesigner.vue`、`GridBlockRenderer.vue`、`ForgeFormCanvasNode.vue`、`ForgePropertyPanel.vue`。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动浏览器做真实拖拽录屏/截图验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。

## 2026-06-21 表单/列表栅格嵌套容器与预览一致性增量验证

- 覆盖范围：
  - 列表页栅格拖拽落点优先命中最内层可接收容器，卡片/标签页放在栅格里后，中间区域可以继续拖入组件。
  - 列表页栅格内已有组件拖拽到卡片/标签页时按容器子节点移动，不再错误掉回画布或被外层 cell 截走。
  - 列表页栅格内子组件手柄去掉原生 HTML5 drag，仅保留 pointer 拖动，减少拖拽抖动和落点丢失。
  - 表单设计器 row/col 视觉降噪，row 只承担栅格结构，col 作为明确投放区；设计态补齐 rowGap，运行预览继续使用 24 栅格 span/gutter 语义。
- 必跑验证：
  - 定向 ESLint：`ListPageGridDesigner.vue`、`GridBlockRenderer.vue`、`AiFormLayoutNodes.vue`、`ForgeFormCanvasNode.vue`、`ForgePropertyPanel.vue`。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动浏览器做真实拖拽录屏/截图验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。

## 2026-06-21 列表栅格内部拖拽落点降噪增量验证

- 覆盖范围：
  - 列表栅格内部拖动时，渲染器接收当前正在拖动的子块 id，拖动源原位块进入低透明安静状态。
  - 栅格内部拖动时普通 cell 边框、子组件 hover/selected 边框和内部块 selected 样式不再叠加显示。
  - 目标 cell 只显示一个高对比浅蓝落点层，减少多层边框和斜纹背景造成的视觉混乱。
- 必跑验证：
  - 定向 ESLint：`ListPageGridDesigner.vue`、`GridBlockRenderer.vue`。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动浏览器做真实拖拽截图验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。

## 2026-06-21 列表栅格放置后提示层遮挡修复验证

- 覆盖范围：
  - 栅格 cell 已有内容时不再渲染整块“释放到此格/拖入组件”覆盖层。
  - 空状态只在 cell 真正没有 children 且不是当前 active drop cell 时显示。
  - cell 内容层级高于提示层，避免拖拽状态残留一帧时盖住刚放入的组件。
- 必跑验证：
  - 定向 ESLint：`GridBlockRenderer.vue`。
  - 前端生产构建。
  - 本轮相关文件 `git diff --check`。
- 跳过项：
  - 未启动浏览器做真实拖拽截图验证；本轮通过代码路径、定向 ESLint、构建和空白检查验证。
