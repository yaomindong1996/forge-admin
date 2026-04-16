# AiModal AI 模态框

可配置、可拖拽的模态对话框，基于 Naive UI `n-modal` + `n-card` 构建。

## 使用方式

```vue
<template>
  <ai-modal ref="modalRef">
    <div>模态框内容</div>
  </ai-modal>

  <n-button @click="openModal">打开</n-button>
</template>

<script setup>
import { ref } from 'vue'

const modalRef = ref()

function openModal() {
  modalRef.value.open({
    title: '编辑',
    width: '600px',
    okText: '保存',
    onOk: async () => {
      await saveData()
      return true  // 返回 true 关闭模态框
    }
  })
}
</script>
```

## Props（默认值可通过 `open()` 覆盖）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `width` | String | `'520px'` | 模态框宽度 |
| `title` | String | - | 标题 |
| `closable` | Boolean | `true` | 显示关闭按钮 |
| `maskClosable` | Boolean | `false` | 点击遮罩关闭 |
| `draggable` | Boolean | `true` | 是否可拖拽 |
| `cancelText` | String | `'取消'` | 取消按钮文字 |
| `okText` | String | `'确定'` | 确认按钮文字 |
| `showFooter` | Boolean | `true` | 显示底部按钮区 |
| `showCancel` | Boolean | `true` | 显示取消按钮 |
| `showOk` | Boolean | `true` | 显示确认按钮 |
| `cancelType` | String | `'default'` | 取消按钮类型 |
| `okType` | String | `'primary'` | 确认按钮类型 |
| `cancelGhost` | Boolean | `false` | 取消按钮幽灵模式 |
| `okGhost` | Boolean | `false` | 确认按钮幽灵模式 |
| `modalStyle` | Object | - | 模态框自定义样式 |
| `contentStyle` | Object | - | 内容区自定义样式 |
| `segmented` | Boolean/Object | - | 分段样式 |
| `showHeader` | Boolean | `true` | 显示头部 |
| `showHeaderExtra` | Boolean | `true` | 显示头部额外操作区 |
| `onOk` | Function | - | 确认回调（可返回 Promise / boolean） |
| `onCancel` | Function | - | 取消回调 |
| `onClose` | Function | - | 关闭回调 |

## 暴露方法

| 方法 | 说明 |
|------|------|
| `open(options)` | 打开模态框，传入配置选项 |
| `close()` | 关闭模态框 |
| `handleOk()` | 触发确认逻辑 |
| `handleCancel()` | 触发取消逻辑 |

## 暴露属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `show` | Ref\<boolean\> | 当前显示状态 |
| `okLoading` | Ref\<boolean\> | 确认按钮 loading 状态 |
| `options` | Object | 当前配置选项 |

## 依赖

- `naive-ui`
- `./utils.js`（拖拽初始化）
