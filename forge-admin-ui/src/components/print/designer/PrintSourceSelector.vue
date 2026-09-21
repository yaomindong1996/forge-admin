<script setup>
import { NSelect } from 'naive-ui'
import { usePrintWorkspaceStore } from '@/stores/print/printWorkspaceStore'

const store = usePrintWorkspaceStore()
</script>

<template>
  <div class="print-source-selector">
    <label id="print-source-label">新模板所属表单</label>
    <NSelect v-model:value="store.selectedKey" :options="store.sources" filterable clearable :disabled="!store.sources.length" placeholder="选择页面中的表单" aria-labelledby="print-source-label" />
    <p v-if="!store.sources.length">
      请先在应用页面中配置业务表单，再创建打印模板。
    </p>
  </div>
</template>

<style scoped>
.print-source-selector {
  display: grid;
  gap: 6px;
  max-width: 560px;
}
label {
  font-weight: 500;
  font-size: 13px;
}
p {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
}
</style>
