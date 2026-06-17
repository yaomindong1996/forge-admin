# 任务清单：forge-form-designer-productivity-upgrades
> status: apply
> spec: `code-copilot/changes/forge-form-designer-productivity-upgrades/spec.md`

## 本轮任务

- [x] 记录变更计划和后续优化方向。
- [x] 拖拽非法位置提示。
- [x] 会话级撤销/重做历史栈。
- [x] 右侧属性面板搜索定位。
- [x] 预览新增/编辑/详情三种模式。
- [x] 前端验证并记录结果。

## 验证记录

- 2026-06-17：执行 `pnpm --dir forge-admin-ui build`，构建通过。
- 2026-06-17：执行定向 `pnpm --dir forge-admin-ui exec eslint` 检查本次相关设计器文件，检查通过。
- 2026-06-17：追加撤销/重做图标、清空画布确认范围、`designer-nav` 默认收起后，再次执行定向 eslint 和 `pnpm --dir forge-admin-ui build`，均通过。
- 2026-06-17：调整 Forge 表单设计器工具栏和左右收起入口后，再次执行定向 eslint 和 `pnpm --dir forge-admin-ui build`，均通过。
- 2026-06-17：修正更多按钮白底白图标、左侧收起按钮遮挡标题、折叠态按钮居中问题后，再次执行定向 eslint 和 `pnpm --dir forge-admin-ui build`，均通过。
- 构建存在既有警告：CSS 中有 `//` 注释、`src/store/index.js` 同时动态和静态导入导致 chunk 提示；本次未改这些文件。

## 追加优化

- [x] 撤销/重做按钮增加图标。
- [x] 新增清空画布确认弹窗，默认清空当前画布，可选清空全部画布。
- [x] 外层 `designer-nav` 默认收起，并保留图标导航和展开按钮。
- [x] 左右面板收起控制移入各自模块，顶部移除左右箭头。
- [x] “按字段生成 / 清理失效字段”移入更多菜单，更多菜单放在工具栏最右侧。
- [x] “清空画布”按钮文案改为“清空”。
- [x] 左右折叠箭头按钮尺寸、位置和 padding 统一。
- [x] 更多按钮改为高对比实心图标按钮。
- [x] 左侧展开态收起按钮移入组件库标题栏 action 区，不再覆盖标题。
- [x] 折叠态展开按钮改为水平居中，避免左右 padding 视觉不一致。
- [x] 工具栏更多菜单右侧按钮改为控制右侧属性栏，右侧收起时不再保留模块内展开按钮和空白窄栏。
- [x] 主表单画布标题区压缩为单行展示，并将“补齐未使用字段 / 旧版画布”透传到新版画布“按字段生成”同一个更多操作里。
- [x] 选中新版画布中的表单组件时自动展开右侧配置面板。
- [x] 顶部保存按钮 loading 前置，避免保存前同步转换导致用户误以为卡顿。

## 实施顺序

1. 在 `designerDragState.js` 增加非法提示状态。
2. 在 `ForgeFormCanvas.vue` 和 `ForgeFormCanvasNode.vue` 接入非法 drop 提示。
3. 在 `ForgeFormDesigner.vue` 维护历史栈，并在工具栏提供撤销/重做按钮。
4. 在 `ForgePropertyPanel.vue` 增加属性搜索框、匹配表和折叠定位。
5. 在 `ForgeFormDesigner.vue` 预览弹窗增加模式切换，并按模式生成 schema/value。
6. 执行 `pnpm --dir forge-admin-ui build` 验证。
