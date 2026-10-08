<template>
  <span v-if="!scope">—</span>
  <div v-else class="license-scope">
    <span>{{ scope.pluginIds.join('、') }}</span>
    <NPopover trigger="click" placement="bottom-start">
      <template #trigger>
        <NButton text type="primary" size="small" aria-label="查看精确授权功能">
          {{ scope.featureCodes.length }} 项功能
        </NButton>
      </template>
      <ul class="license-scope__features">
        <li v-for="code in scope.featureCodes" :key="code">
          {{ code }}
        </li>
      </ul>
    </NPopover>
  </div>
</template>

<script setup>
import { NButton, NPopover } from 'naive-ui'

defineProps({ scope: { type: Object, default: null } })
</script>

<style scoped>
.license-scope {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 4px;
  overflow-wrap: anywhere;
}
.license-scope__features {
  max-width: min(360px, 72vw);
  max-height: 240px;
  overflow: auto;
  overflow-wrap: anywhere;
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
}
</style>
