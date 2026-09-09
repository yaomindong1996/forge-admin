# 节点配置面板、不会签持久化与审批职责要点
> status: apply
> created: 2026-08-30
> complexity: 🟡中等

## 1. 背景与目标
节点属性抽屉过窄，右侧 Tab 被裁切，处理时限天数在小屏显示不全。多人审批应默认「不会签」，保存后不能回退成会签。从 shenrong-projman 迁入审批职责、审批要点，并让外置表单能自动读取字段目录用于节点表单权限。

## 2. 代码现状
- `NodeConfigDrawer` 默认宽度 400。
- `user-task-writer` 在 `initiatorSelect` 时强制写出 multiInstance，parser 再读回 parallel。
- `ApproverAssigneeForm` 选择发起人自选时覆盖 `multiInstanceType=parallel`。
- 外置表单 `formType !== dynamic` 时 `formFieldCatalog` 被清空。

## 3. 功能点
- [x] 加宽抽屉、Tab 横向滚动、处理时限完整展示
- [x] 默认不会签，保存后 roundtrip 仍为 none
- [x] 审批职责/要点：设计器配置、待办勾选、必审拦截、历史回显
- [x] 外置表单按 formUrl 读取 flowFieldCatalog 用于节点字段权限
