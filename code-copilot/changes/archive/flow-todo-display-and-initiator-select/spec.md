# 待办业务字段展示与发起人自选卡片文案

> status: apply
> created: 2026-08-30
> complexity: 🟢简单

## 1. 背景与目标

待办只显示对象名/摘要，`displayExtensions` 没有上屏；设计器把「发起人自选」渲染成原始码，容易和「发起人」混淆。

完成后：待办/已办/我发起的/抄送能展示业务扩展字段；审批节点卡片明确显示「发起人自选」。

## 2. 功能点

- [x] 前端按 `displayExtensions.fields` 渲染待办扩展字段，启动变量 `businessParams.displayFields` 可回退。
- [x] 默认适配器保留发起时的 displayFields，不再被业务记录覆盖。
- [x] 设计器卡片摘要显示「发起人自选」，配置区说明与「发起人」的区别。
