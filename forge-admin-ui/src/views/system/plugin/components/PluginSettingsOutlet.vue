<script setup>
import { NAlert, NButton, NSkeleton } from 'naive-ui'
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { getPluginSettingsExtension } from '@/utils/plugin-settings'

const props = defineProps({ pluginId: { type: String, required: true } })
const revision = ref(0)
const error = ref(false)
const panel = computed(() => {
  void revision.value
  const extension = getPluginSettingsExtension(props.pluginId)
  if (!extension)
    return null
  return defineAsyncComponent({
    loader: extension.loader,
    delay: 0,
    timeout: 15000,
    loadingComponent: NSkeleton,
    onError(_failure, _retry, fail) {
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
  <!-- 插件资源加载失败只影响设置区，不把配置内容回退到其它插件 -->
  <NAlert v-if="error" type="error" title="插件设置页面加载失败" :bordered="false">
    <NButton text type="primary" @click="reload">
      重新加载
    </NButton>
  </NAlert>
  <component :is="panel" v-else-if="panel" :key="pluginId + revision" />
  <NAlert v-else type="info" :bordered="false">
    当前插件没有可用的设置扩展。
  </NAlert>
</template>
