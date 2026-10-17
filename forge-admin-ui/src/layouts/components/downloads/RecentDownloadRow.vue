<template>
  <article class="downloads-file-row">
    <div class="downloads-file-icon" :class="`file-tone-${fileType.tone}`" aria-hidden="true">
      <span>{{ fileType.extension.toUpperCase() }}</span>
    </div>
    <div class="downloads-file-copy">
      <div class="downloads-file-name" :title="row.fileName">
        {{ row.fileName || '未命名文件' }}
      </div>
      <div class="downloads-file-meta" :title="metadata">
        {{ metadata }}
      </div>
      <template v-if="receiving">
        <div class="downloads-file-state is-receiving" role="status">
          <span>{{ progressText }}</span><span v-if="percentage !== null">{{ percentage }}%</span>
        </div>
        <NProgress
          v-if="percentage !== null" type="line" :percentage="percentage" :height="3"
          :show-indicator="false" :processing="false" :border-radius="2"
          color="var(--downloads-accent)"
          :aria-label="`${row.fileName}接收进度`"
        />
      </template>
      <div v-else-if="row.phase === 'failed'" class="downloads-file-state is-failed" role="status">
        <i class="i-lucide:circle-alert" aria-hidden="true" /><span>下载失败，请重试</span>
      </div>
      <div v-else class="downloads-file-state">
        <i class="i-lucide:check" aria-hidden="true" />
        <span>{{ row.fileId ? '已发起保存' : '请回原页面重新导出' }}</span>
      </div>
    </div>
    <!-- 不重放导出写接口；有 fileId 才复用受控下载接口。 -->
    <NButton
      v-if="row.fileId" quaternary size="small" class="downloads-icon-button downloads-file-action"
      :disabled="busy" :aria-label="`${row.phase === 'failed' ? '重试' : '再次下载'}${row.fileName}`"
      :title="receiving ? '正在接收文件' : row.phase === 'failed' ? '重试' : '再次下载'"
      @click="store.download(row)"
    >
      <i :class="row.phase === 'failed' ? 'i-lucide:rotate-cw' : 'i-lucide:download'" aria-hidden="true" />
    </NButton>
  </article>
</template>

<script setup>
import { NButton, NProgress } from 'naive-ui'
import { computed } from 'vue'
import { useDict } from '@/composables/useDict'
import { useRecentDownloadsStore } from '@/stores/system/recentDownloadsStore'
import { downloadFileType, downloadTime, progressPercentage } from '@/stores/system/recentDownloadsUtils'
import { formatFileSize } from '@/views/system/file-list/utils'

const props = defineProps({ row: { type: Object, required: true } })
const store = useRecentDownloadsStore()
const { dict } = useDict('sys_file_download_source')
const fileType = computed(() => downloadFileType(props.row.fileName))
const receiving = computed(() => props.row.phase === 'receiving')
const percentage = computed(() => progressPercentage(props.row))
const busy = computed(() => store.tasks.some(task => task.fileId === props.row.fileId && task.phase === 'receiving'))
const source = computed(() => props.row.runtime
  ? '本次下载'
  : dict.value.sys_file_download_source?.find(item => item.value === props.row.source)?.label || '来源未标注')
const metadata = computed(() => {
  const row = props.row
  const size = row.fileSize ?? row.total
  const parts = [source.value, size == null ? '大小未知' : formatFileSize(size), downloadTime(row.createTime)]
  if (store.seeAll && row.username)
    parts.unshift(row.username)
  return parts.join(' · ')
})
const progressText = computed(() => {
  const row = props.row
  if (!(row.loaded > 0))
    return '正在准备下载'
  return row.total > 0
    ? `接收中 · ${formatFileSize(row.loaded)} / ${formatFileSize(row.total)}`
    : `接收中 · ${formatFileSize(row.loaded)}`
})
</script>
