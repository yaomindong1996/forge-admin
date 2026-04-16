# DemoBanner 演示模式横幅

演示模式横幅组件，当应用处于演示模式时在页面顶部显示警告横幅。

## 使用方式

```vue
<template>
  <n-layout>
    <DemoBanner />
    <!-- 页面内容 -->
  </n-layout>
</template>

<script setup>
import DemoBanner from '@/components/DemoBanner.vue'
</script>
```

## 功能说明

- 从 Pinia `demo` store 读取演示模式状态
- 当 `demoStore.isDemo` 为 `true` 时显示警告横幅
- 显示 `demoStore.demoMessage` 内容
- 支持关闭按钮，关闭状态持久化到 `localStorage`（`demo-banner-closed`）
- 使用 Naive UI `NAlert` 组件，带 Warning 图标

## 依赖

- `naive-ui`（NAlert, NIcon）
- `@vicons/ionicons5`（Warning 图标）
- `@/stores/demo`（演示模式 Store）
