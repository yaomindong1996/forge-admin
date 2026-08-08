# 系统管理页面交互一致性修复
> status: apply
> created: 2026-08-08
> complexity: 🟡中等

## 1. 背景与目标

本变更处理菜单管理和系统管理页面的四项用户反馈：菜单叶节点误显示全部资源、删除子资源后父节点仍被旧数据阻止删除、选择类控件输入框与按钮高度错位，以及组织/用户/岗位页面左侧组织树无法滚动到底部。

完成后应达到以下可验证结果：

- 菜单管理点击无子资源的目录或菜单时，中间资源列表显示空状态，不回退为全部资源。
- 删除子资源并刷新资源树后，当前选中行绑定到新树节点；随后删除原父节点时不再读取已删除子项。
- Naive UI 按钮使用组件自身尺寸变量，中号、大号按钮分别与同尺寸输入控件保持一致。
- 组织管理、用户管理、岗位管理的左侧组织树在节点超过可视高度时出现独立纵向滚动，并可滚动到底部。

## 2. 代码现状（Research Findings）

### 2.1 菜单资源上下文

- `forge-admin-ui/src/views/system/menu.vue#getContextRows` 使用 `currentNode.value?.children || allResources.value`。叶节点的 `children` 为 `null/undefined` 时会错误回退到全部资源。
- `menu.vue#loadResourceTree` 替换 `allResources` 后，`keepSelectionAvailable` 只校验 ID 是否存在，没有把 `selectedRow` 从旧对象重绑到新树对象。
- `menu.vue#handleDelete` 的前置子资源数量检查读取 `row.children`；因此旧的 `selectedRow` 会继续携带已删除子资源。

### 2.2 控件高度

- `forge-admin-ui/src/styles/theme.css` 将默认按钮高度强制设为 `32px`、大号按钮设为 `36px`。
- 当前 Naive UI 的输入与按钮主题尺寸均由 `--n-height` 提供，中号为 `34px`、大号为 `40px`。全局固定值破坏了输入框和相邻按钮的同尺寸对齐。
- `UserSelectPicker.vue` 和 `AiFormItem.vue` 的业务记录选择器均使用 `n-input-group`，会直接暴露该高度差。

### 2.3 组织树滚动

- `org.vue`、`user.vue`、`post.vue` 都以 `MasterDetailWorkspace` 承载左侧树，并在 `.org-tree-content` 上设置纵向滚动。
- `user.vue` 与 `post.vue` 的 `.org-tree-content` 缺少 flex 滚动容器必要的 `min-height: 0`，内容会按最小内容高度撑开并被上层 `overflow: hidden` 裁剪。
- 三个页面的滚动区域没有统一的滚动边界行为；本变更统一补齐 flex 收缩、滚动条占位和滚动链隔离。

## 3. 功能点

- [x] 叶节点资源上下文严格解析为空子集。
- [x] 资源树刷新后重绑当前选中资源对象。
- [x] 选择类输入组的输入框和按钮按同一 Naive UI 尺寸渲染。
- [x] 组织、用户、岗位左侧组织树可以独立纵向滚动到底部。

## 4. 业务规则

- “全部资源”根节点继续展示顶级资源；只有明确选中资源节点时才读取该节点的直接子资源。
- 搜索条件存在时只在当前节点的后代中搜索；叶节点没有后代时结果为空。
- 树刷新后保留仍存在的资源选择，已删除资源清空选择。
- 不改变资源删除接口、逻辑删除规则或权限缓存刷新协议。

## 5. 数据变更

无数据库表、字段、索引或内置数据变更。

## 6. 接口变更

无接口协议变更，继续复用 `/system/resource/tree`、`/system/resource/remove` 和现有系统菜单刷新接口。

## 7. 影响范围

- `forge-admin-ui/src/views/system/menu.vue`
- `forge-admin-ui/src/views/system/org.vue`
- `forge-admin-ui/src/views/system/user.vue`
- `forge-admin-ui/src/views/system/post.vue`
- `forge-admin-ui/src/styles/theme.css`
- 菜单交互纯函数及对应前端测试

## 8. 风险与关注点

- 按钮高度属于共享样式变更，必须使用 Naive UI 已有 `--n-height`，避免为不同尺寸重复写死像素值。
- 资源选择重绑必须同时支持左侧目录/菜单和中间列表中的按钮/API 资源，不能只按导航树节点处理。
- 页面只增加内部滚动约束，不改变系统布局的外层滚动策略。

## 8.5 测试策略

- **测试范围**：菜单上下文和选择重绑纯函数；共享按钮尺寸与三页滚动样式静态契约；前端 ESLint、Vitest、生产构建；可用环境下浏览器检查。
- **覆盖率目标**：覆盖根节点、含子节点、叶节点、搜索后代、选中对象重绑和已删除对象清空分支。
- **独立 Test Spec**：是，见 `test-spec.md`。

## 9. 待澄清

无。用户已明确列出四项问题并要求优化，视为授权直接实施。

## 10. 技术决策

- 将菜单上下文解析与选中对象重绑提取为纯函数，避免继续在页面状态代码中隐藏空值回退语义，并提供聚焦回归测试。
- 按钮高度统一读取 `--n-height`，保留现有边距、圆角、配色和密度设计。
- 组织树滚动继续由页面内 `.org-tree-content` 承担，仅补齐 flex 子项收缩和滚动边界约束。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 | complete | `menu.vue`、菜单交互工具及测试 | 13 项合并定向测试中的菜单逻辑 9 项通过 |
| Task 2 | complete | `theme.css`、`org.vue`、`user.vue`、`post.vue` 及 UI 契约测试 | 共享尺寸和滚动契约 4 项通过 |
| Task 3 | pending | - | 增量验证与文档回填 |

## 12. 审查结论

待实现和验证后回填。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-08-08
- **确认人**：用户
- **确认内容**：用户明确提出菜单、选择控件和组织树的四项修复要求，授权直接实施。
