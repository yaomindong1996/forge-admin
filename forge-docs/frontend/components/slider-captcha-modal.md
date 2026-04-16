# SliderCaptchaModal 滑块验证码

滑块拼图验证码模态框，基于 `vue3-slide-verify` 实现拖拽验证。

## 使用方式

```vue
<template>
  <n-button @click="showCaptcha = true">验证</n-button>
  <SliderCaptchaModal
    v-model:show="showCaptcha"
    :images="captchaImages"
    @success="onVerified"
    @fail="onFailed"
  />
</template>

<script setup>
import { ref } from 'vue'
import SliderCaptchaModal from '@/components/SliderCaptchaModal.vue'

const showCaptcha = ref(false)
const captchaImages = ref(['/captcha-bg-1.jpg', '/captcha-bg-2.jpg'])

function onVerified() {
  console.log('验证通过')
}

function onFailed() {
  console.log('验证失败')
}
</script>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `show` | Boolean | false | 控制显示/隐藏（v-model） |
| `images` | Array | `[]` | 拼图背景图片列表 |

## Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `update:show` | `(value: boolean)` | 显示状态变更 |
| `success` | - | 验证通过 |
| `fail` | - | 验证失败 |
| `refresh` | - | 刷新验证码 |

## 暴露方法

| 方法 | 说明 |
|------|------|
| `reset()` | 重置验证码 |
| `handleClose()` | 关闭弹窗 |

## 行为

- 验证成功后 800ms 自动关闭
- 验证失败时显示错误提示，然后重置

## 依赖

- `vue3-slide-verify`
- `naive-ui`（NModal）
