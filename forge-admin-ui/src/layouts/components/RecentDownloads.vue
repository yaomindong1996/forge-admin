<template>
  <!-- 侧栏内使用布局流；没有侧栏时才使用左下角兜底入口。 -->
  <div v-if="store.available" class="recent-downloads-entry" :class="{ 'is-docked': docked, 'is-compact': compact }">
    <NPopover
      v-model:show="store.open" trigger="click" :placement="docked ? 'right-end' : 'top-start'"
      :show-arrow="false" :keep-alive-on-hover="false" raw
    >
      <template #trigger>
        <!-- 与 MenuCollapse 同级原生按钮，避免 Naive 白底卡片感。 -->
        <button
          ref="trigger"
          type="button"
          class="download-entry"
          :class="{ 'is-open': store.open }"
          aria-label="最近下载"
          title="最近下载"
          :aria-expanded="store.open"
          aria-haspopup="dialog"
        >
          <i class="i-lucide:download" aria-hidden="true" />
          <span v-if="!compact" class="download-entry-label">最近下载</span>
          <span v-if="store.activeCount" class="download-entry-count">{{ store.activeCount }}</span>
        </button>
      </template>
      <RecentDownloadPanel @close="close" />
    </NPopover>
  </div>
</template>

<script setup>
import { NPopover } from 'naive-ui'
import { ref } from 'vue'
import { useRecentDownloadsStore } from '@/stores/system/recentDownloadsStore'
import RecentDownloadPanel from './downloads/RecentDownloadPanel.vue'
import './downloads/recent-downloads.css'

defineProps({ docked: Boolean, compact: Boolean })
const store = useRecentDownloadsStore()
const trigger = ref(null)

function close() {
  store.open = false
  trigger.value?.focus()
}
</script>
