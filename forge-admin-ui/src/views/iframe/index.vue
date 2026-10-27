<template>
  <AppPage>
    <iframe
      v-if="frameSrc"
      :src="frameSrc"
      :title="frameTitle"
      frameborder="0"
      class="wh-full"
      referrerpolicy="no-referrer"
    />
    <div v-else class="iframe-empty">
      未配置外链地址
    </div>
  </AppPage>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useTabStore } from '@/store'

defineOptions({ name: 'IFramePage' })

const route = useRoute()
const tabStore = useTabStore()

const frameSrc = computed(() => {
  const fromMeta = route.meta?.originPath
  const fromQuery = route.query?.page || route.query?.url
  return String(fromMeta || fromQuery || '').trim()
})

const frameTitle = computed(() => {
  return String(route.query?.title || route.meta?.title || '外链页面').trim()
})

watch(frameTitle, (title) => {
  if (title)
    tabStore.updateTabTitle(route.fullPath || route.path, title)
}, { immediate: true })
</script>

<style scoped>
.iframe-empty {
  display: grid;
  place-items: center;
  height: 100%;
  color: var(--text-tertiary);
  font-size: 14px;
}
</style>
