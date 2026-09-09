---
kind: external_dependency
name: 图形编辑 Leafer Editor 2.1
slug: leafer-editor
category: external_dependency
category_hints:
    - vendor_identity
    - framework_behavior
scope:
    - '**'
source_files:
    - forge-admin-ui/package.json
    - README.md
---

### 身份与角色
- README 将其列为前端界面组件之一。

### 关键约束
- 作为图形编辑引擎，其生命周期、渲染上下文与 Vue 3 组件树需要适配器桥接。

### 集成点
- README 技术栈章节列出 Leafer Editor 2.1。

### 方向
- 新增图形类组件时，需确保与 Vue 3 响应式数据流兼容，并在测试环境中正确 mock Leafer 实例。