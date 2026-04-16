# BPMN 流程设计器

基于 `bpmn-js` 的 BPMN 2.0 流程设计器和查看器，支持 Flowable 扩展属性。

## 组件概览

| 组件 | 说明 |
|------|------|
| `BpmnModeler.vue` | 基础 BPMN 模型设计器 |
| `FlowModeler.vue` | 增强版 Flowable 模型设计器 |
| `ProcessDiagramViewer.vue` | 流程图查看器（只读） |
| `InteractiveProcessDiagram.vue` | 交互式流程图查看器 |
| `NodePropertiesPanel.vue` | 节点属性编辑面板 |
| `UserSelectModal.vue` | 用户选择弹窗 |

## BpmnModeler — 基础模型设计器

```vue
<template>
  <BpmnModeler
    :xml="bpmnXml"
    :read-only="false"
    @save="onSave"
    @change="onChange"
    @ready="onReady"
  />
</template>

<script setup>
import BpmnModeler from '@/components/bpmn/BpmnModeler.vue'

function onSave(xml) {
  console.log('BPMN XML:', xml)
}
</script>
```

### Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `xml` | String | - | BPMN XML 内容 |
| `readOnly` | Boolean | false | 只读模式 |

### Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `save` | `(xml: string)` | 保存时触发 |
| `change` | - | 模型变更 |
| `ready` | - | 模型加载完成 |

### 暴露方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `getXML()` | `Promise<string>` | 获取 BPMN XML |
| `getSVG()` | `Promise<string>` | 获取 SVG 图片 |
| `importXML(xml)` | `Promise<void>` | 导入 BPMN XML |

### 工具栏

支持撤销/重做、缩放（放大/缩小/适应）、导出 BPMN/SVG。

---

## FlowModeler — 增强版模型设计器

在 `BpmnModeler` 基础上增加 Flowable 扩展属性支持。

### 特性

- Flowable moddle 扩展（`flowable-moddle.json`）
- 增强的工具栏（导入/导出、XML 预览）
- 缩放百分比显示
- 节点属性弹窗（委托给 `NodePropertiesPanel`）

### 暴露方法

| 方法 | 说明 |
|------|------|
| `getXML()` | 获取 BPMN XML |
| `setXML(xml)` | 设置 BPMN XML |
| `modeler()` | 获取底层 bpmn-js Modeler 实例 |

---

## ProcessDiagramViewer — 流程图查看器

显示流程实例的运行状态图。

```vue
<template>
  <ProcessDiagramViewer
    process-instance-id="process-123"
    @node-click="onNodeClick"
  />
</template>
```

### Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `processInstanceId` | String | 是 | 流程实例 ID |

### Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `node-click` | `(nodeData)` | 节点点击 |
| `loaded` | - | 加载完成 |

### 功能

- 从 API 获取 BPMN XML 并渲染
- 节点状态标记（已完成/进行中/待处理）
- 处理人徽章
- 悬浮提示（处理人、时间、持续时间、审批意见）

---

## NodePropertiesPanel — 节点属性面板

BPMN 元素的属性编辑器，支持以下节点类型：

| 节点类型 | 可配置属性 |
|----------|-----------|
| User Task | 分配人类型、候选人/组（支持用户/角色选择器）、表单配置、优先级、截止日期、多实例/会签、任务/执行监听器 |
| Service Task | 实现类、表达式 |
| Sequence Flow | 条件表达式 |
| Start/End Event | 事件配置 |

---

## UserSelectModal — 用户选择弹窗

可复用的用户选择器，支持搜索、分页和单选/多选。

```vue
<template>
  <UserSelectModal
    v-model:show="show"
    title="选择处理人"
    :multiple="true"
    :selected-users="selectedUsers"
    @confirm="onConfirm"
  />
</template>
```

### Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `show` | Boolean | false | 控制显示（v-model） |
| `title` | String | - | 弹窗标题 |
| `multiple` | Boolean | false | 是否多选 |
| `selectedUsers` | Array | `[]` | 已选用户列表 |

### Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `update:show` | `(value: boolean)` | 显示状态变更 |
| `confirm` | `(users: Array)` | 确认选择 |

### 功能

- 关键词搜索
- 部门树过滤
- 分页数据表格
- 单选/多选支持

## 依赖

- `bpmn-js`
- `flowable-moddle.json`
- `naive-ui`
- `@/api/flow`
- `@/utils/http`
