<template>
  <div>
    <n-tooltip placement="bottom">
      <template #trigger>
        <i
          class="ai-icon:awesome text-20 cursor-pointer color-inherit"
          @click="handleOpen"
        />
      </template>
      主题配置
    </n-tooltip>

    <ThemeConfig ref="themeConfigRef" />
  </div>
</template>

<script setup>
import ThemeConfig from './ThemeConfig.vue'

const themeConfigRef = ref(null)

function handleOpen() {
  themeConfigRef.value?.open()
}
</script>
