---
kind: external_dependency
name: 流程建模 BPMN.js 17 + properties-panel 5
slug: bpmn-js
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
- README 明确“Flowable 工作流”“流程设计”“流程模型”等能力，前端通过 BPMN.js 渲染流程图、编辑属性。

### 关键约束
- 版本锁定在 17.x / 5.x，升级需验证 properties-panel 与 BPMN modeler 的兼容性。
- 流程设计器属于独立模块（forge-flow），前后端通过 Flowable API 交互。

### 集成点
- `forge-admin-ui/package.json` 声明 bpmn-js 与 bpmn-js-properties-panel。
- README 技术栈章节列出 BPMN.js / CodeMirror 用于流程设计与代码编辑。

### 方向
- 新增流程节点或属性时，需同步扩展 BPMN 模型定义与 properties-panel 的自定义编辑器。