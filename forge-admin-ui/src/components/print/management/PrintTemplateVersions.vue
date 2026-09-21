<script setup>
import { NButton, NEmpty, NTable } from 'naive-ui'
import { usePrintTemplateStore } from '@/stores/print/printTemplateStore'

defineProps({ canRestore: Boolean })
defineEmits(['restore'])
const store = usePrintTemplateStore()
</script>

<template>
  <p>历史版本保持不变。载入后需保存草稿并重新发布。</p>
  <NEmpty v-if="!store.versions.length" description="尚无发布版本" />
  <NTable v-else :single-line="false" size="small">
    <thead><tr><th>版本</th><th>发布时间</th><th>操作</th></tr></thead>
    <tbody>
      <tr v-for="version in store.versions" :key="version.id">
        <td>{{ version.versionNo }}</td><td>{{ version.publishTime }}</td><td>
          <NButton text type="primary" :disabled="!canRestore" @click="$emit('restore', version.id)">
            载入到草稿
          </NButton>
        </td>
      </tr>
    </tbody>
  </NTable>
</template>
