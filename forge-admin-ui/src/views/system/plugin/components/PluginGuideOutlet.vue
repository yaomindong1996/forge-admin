<script setup>
import { NAlert, NButton, NSkeleton } from 'naive-ui'
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { getPluginGuideExtension } from '@/utils/plugin-guide'

const props = defineProps({ pluginId: { type: String, required: true } })
const error = ref(false)
const revision = ref(0)
const panel = computed(() => {
  const ticket = `${props.pluginId}:${revision.value}`
  const loader = getPluginGuideExtension(props.pluginId)
  return loader && defineAsyncComponent({
    loader,
    loadingComponent: NSkeleton,
    delay: 0,
    timeout: 15000,
    onError(_failure, _retry, fail) {
      if (`${props.pluginId}:${revision.value}` === ticket)
        error.value = true
      fail()
    },
  })
})
function reload() {
  error.value = false
  revision.value++
}
watch(() => props.pluginId, reload)
</script>

<template>
  <!-- 可选静态扩展失败不影响其它插件或宿主通用说明 -->
  <NAlert v-if="error" type="error" :bordered="false" title="使用说明加载失败">
    <NButton text type="primary" @click="reload">
      重新加载
    </NButton>
  </NAlert>
  <component :is="panel" v-else-if="panel" :key="pluginId + revision" />
</template>
