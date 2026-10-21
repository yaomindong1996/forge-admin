<script setup>
import { NAlert, NButton, NSkeleton } from 'naive-ui'
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { getPluginExamplesExtension } from '@/utils/plugin-examples'

const props = defineProps({ pluginId: { type: String, required: true } })
const failed = ref(false)
const revision = ref(0)
const panel = computed(() => {
  const ticket = `${props.pluginId}:${revision.value}`
  const extension = getPluginExamplesExtension(props.pluginId)
  return extension && defineAsyncComponent({
    loader: extension.loader,
    loadingComponent: NSkeleton,
    delay: 0,
    timeout: 15000,
    onError(_error, _retry, fail) {
      if (`${props.pluginId}:${revision.value}` === ticket)
        failed.value = true
      fail()
    },
  })
})
function retry() {
  failed.value = false
  revision.value++
}
watch(() => props.pluginId, retry)
</script>

<template>
  <NAlert v-if="failed" type="error" :bordered="false" title="使用示例加载失败">
    <NButton text type="primary" @click="retry">
      重新加载
    </NButton>
  </NAlert>
  <component :is="panel" v-else-if="panel" :key="pluginId + revision" />
</template>
