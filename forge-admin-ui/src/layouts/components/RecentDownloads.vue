<template>
  <!-- 最近下载固定在左下角，不进入侧栏菜单 -->
  <div class="recent-downloads">
    <NPopover
      v-model:show="open"
      trigger="click"
      placement="top-start"
      :show-arrow="false"
      raw
      @update:show="onToggle"
    >
      <template #trigger>
        <button class="download-dock" type="button" aria-label="最近下载" title="最近下载" :aria-expanded="open">
          <i class="i-lucide:download" aria-hidden="true" />
        </button>
      </template>
      <section class="download-panel" aria-label="最近下载">
        <header class="panel-head">
          <div>
            <h2>最近下载</h2>
            <p>{{ seeAll ? '本租户全部记录' : '只显示我的下载' }}</p>
          </div>
          <button type="button" class="icon-button" aria-label="关闭最近下载" @click="open = false">
            <i class="i-lucide:x" />
          </button>
        </header>
        <div class="panel-body">
          <p v-if="loading" class="panel-empty">正在加载</p>
          <p v-else-if="errorText" class="panel-empty">{{ errorText }}</p>
          <p v-else-if="!records.length" class="panel-empty">还没有下载记录</p>
          <ul v-else>
            <li v-for="item in records" :key="item.id">
              <span class="file-icon" aria-hidden="true">
                <i :class="fileIcon(item.fileName)" />
              </span>
              <div class="file-copy">
                <span class="file-name" :title="item.fileName">{{ item.fileName }}</span>
                <span class="file-meta">
                  <template v-if="seeAll && item.username">{{ item.username }} · </template>
                  {{ sourceLabel(item.source) }} · {{ formatFileSize(item.fileSize) }} · {{ formatFileDate(item.createTime, true) }}
                </span>
              </div>
              <button
                v-if="item.fileId"
                type="button"
                class="again"
                :aria-label="`再次下载${item.fileName}`"
                title="再次下载"
                @click="downloadAgain(item)"
              >
                <i class="i-lucide:download" />
              </button>
            </li>
          </ul>
        </div>
      </section>
    </NPopover>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { NPopover } from 'naive-ui'
import { useUserStore } from '@/store/modules/user'
import { request } from '@/utils'
import { downloadFile } from '@/utils/file'
import { formatFileDate, formatFileSize } from '@/views/system/file-list/utils'

const userStore = useUserStore()
const open = ref(false)
const loading = ref(false)
const errorText = ref('')
const records = ref([])
const seeAll = computed(() => userStore.isAdmin || Number(userStore.userType) === 1)

function sourceLabel(source) {
  return source === 'export' ? '导出' : '文件'
}

function fileIcon(name = '') {
  const extension = String(name).split('.').pop()?.toLowerCase()
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(extension))
    return 'i-lucide:image'
  if (['pdf', 'doc', 'docx', 'txt', 'md'].includes(extension))
    return 'i-lucide:file-text'
  if (['xls', 'xlsx', 'csv'].includes(extension))
    return 'i-lucide:sheet'
  if (['ppt', 'pptx'].includes(extension))
    return 'i-lucide:presentation'
  if (['zip', 'rar', '7z'].includes(extension))
    return 'i-lucide:file-archive'
  return 'i-lucide:file'
}

async function loadRecords() {
  loading.value = true
  errorText.value = ''
  try {
    const response = await request.get('/system/file/download-log/page', {
      params: { pageNum: 1, pageSize: 20 },
      needTip: false,
    })
    records.value = response?.data?.records || []
  }
  catch (error) {
    records.value = []
    errorText.value = error?.message || '下载记录加载失败'
  }
  finally {
    loading.value = false
  }
}

function onToggle(visible) {
  if (visible)
    loadRecords()
}

function onRecorded() {
  if (open.value)
    loadRecords()
}

function downloadAgain(item) {
  downloadFile(item.fileId, item.fileName)
}

onMounted(() => {
  window.addEventListener('forge-download-recorded', onRecorded)
})

onBeforeUnmount(() => {
  window.removeEventListener('forge-download-recorded', onRecorded)
})
</script>

<style scoped>
.recent-downloads {
  position: fixed;
  left: 16px;
  bottom: 16px;
  z-index: 120;
}

.download-dock {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--border-light);
  border-radius: 999px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 18px;
  box-shadow: 0 4px 14px rgb(15 23 42 / 10%);
  cursor: pointer;
}

.download-dock:hover,
.download-dock[aria-expanded='true'] {
  color: var(--primary-color);
  border-color: color-mix(in srgb, var(--primary-color) 40%, var(--border-light));
}

.download-panel {
  width: 340px;
  max-width: calc(100vw - 32px);
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: var(--bg-primary);
  color: var(--text-primary);
  box-shadow: 0 12px 32px rgb(15 23 42 / 14%);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 12px 10px 14px;
  border-bottom: 1px solid var(--border-light);
}

.panel-head h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
}

.panel-head p,
.panel-empty,
.file-meta {
  margin: 2px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.4;
}

.panel-body {
  max-height: 380px;
  overflow: auto;
}

.panel-empty {
  padding: 36px 16px;
  text-align: center;
}

ul {
  margin: 0;
  padding: 6px;
  list-style: none;
}

li {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 52px;
  padding: 8px;
  border-radius: 8px;
}

li:hover {
  background: var(--bg-secondary);
}

.file-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--primary-color) 10%, var(--bg-secondary));
  color: var(--primary-color);
  font-size: 16px;
}

.file-copy {
  min-width: 0;
  flex: 1;
}

.file-name {
  display: block;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.again,
.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  font-size: 15px;
}

.again:hover,
.icon-button:hover {
  background: color-mix(in srgb, var(--primary-color) 10%, transparent);
  color: var(--primary-color);
}
</style>
