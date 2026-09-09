---
kind: external_dependency
name: 表单设计器 FormCreate 3.x
slug: form-create-designer
category: external_dependency
category_hints:
    - sdk_real_api
    - framework_behavior
scope:
    - '**'
source_files:
    - forge-admin-ui/package.json
    - forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue
---

### 身份与角色
- 通过 @form-create/designer ^3 与 @form-create/element-ui ^3 提供可视化表单设计能力，作为低代码表单画布的核心引擎。
- 由 FormCreateDesignerAdapter 桥接到 Forge 的低代码页面体系。

### 关键约束
- 依赖 Element Plus 2.14（表单设计器的底层 UI），升级时需同时关注 Element Plus 兼容。
- 表单预览链路中的 Slider 等字段若通过 FormCreate 渲染，仍需保证 field.marks/value 等属性非 null，否则会触发上游 Slider 崩溃。

### 集成点
- `forge-admin-ui/src/components/lowcode-builder/page/FormCreateDesignerAdapter.vue`：将 Forge 的表单 schema 适配为 FormCreate 可消费的配置。

### 方向
- 新增表单字段类型时，需在 FormCreate 侧注册对应的 rule/组件映射，并保证 spec 的 propsSchema 与渲染器消费 key 一致。