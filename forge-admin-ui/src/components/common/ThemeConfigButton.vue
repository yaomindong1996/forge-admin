<template>
  <div>
    <n-tooltip placement="bottom">
      <template #trigger>
        <i
          class="ai-icon:awesome cursor-pointer text-20 color-inherit"
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
