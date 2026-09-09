---
kind: external_dependency
name: 前端组件库 Naive UI 2.42
slug: naive-ui
category: external_dependency
category_hints:
    - framework_behavior
    - client_constraint
scope:
    - '**'
source_files:
    - forge-admin-ui/package.json
    - forge-admin-ui/src/components/ai-form/AiFormItem.vue
    - forge-admin-ui/src/components/lowcode-builder/designer-core/panel/SpecPropertyPanel.vue
---

### 身份与角色
- 项目前端管理端（forge-admin-ui）的 UI 组件库，版本 ^2.42，用于表单设计器、属性面板、物料面板等低代码编辑界面。
- 在 AiFormItem 中通过 n-slider 渲染滑块字段；在 SpecPropertyPanel 中通过 n-input/n-switch/n-select 等构建两列紧凑的属性面板。

### 关键约束
- SpecPropertyPanel 重写后依赖 Naive UI 的栅格/布局能力，测试环境需通过 global.stubs 提供 Naive UI 组件 stub，否则测试无法解析。

### 集成点
- `forge-admin-ui/src/components/ai-form/AiFormItem.vue`：表单预览时通过 n-slider 渲染 marks/value。
- `forge-admin-ui/src/components/lowcode-builder/designer-core/panel/SpecPropertyPanel.vue`：属性面板使用 Naive UI 控件实现两列网格布局。

### 方向
- 新增/修改表单字段或属性面板控件时，需确认对应 Naive UI 组件在预览和测试环境的边界行为（如 null value、disabled 透传等）。