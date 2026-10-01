<template>
  <n-modal
    :show="show"
    preset="card"
    title="导出应用配置"
    :style="{ width: 'min(560px, 94vw)' }"
    :mask-closable="!exporting"
    :closable="!exporting"
    @update:show="emit('update:show', $event)"
  >
    <n-alert type="info" :bordered="false" class="export-alert">
      勾选要带走的页面；系统会自动带上页面依赖的对象及关系闭包，并打包流程、打印、扩展、触发器、入口等完整设计态配置。
      全选或不勾选均等价导出整应用。
    </n-alert>

    <div class="export-toolbar">
      <n-checkbox
        :checked="allChecked"
        :indeterminate="indeterminate"
        :disabled="loading || !pages.length"
        @update:checked="toggleAll"
      >
        全选页面
      </n-checkbox>
      <n-button text type="primary" :disabled="loading" @click="loadPages">
        刷新
      </n-button>
    </div>

    <n-spin :show="loading">
      <n-empty v-if="!loading && !pages.length" description="当前应用还没有可导出的页面" />
      <n-checkbox-group v-else v-model:value="selectedPageIds" class="page-list">
        <label
          v-for="page in pages"
          :key="page.pageId"
          class="page-item"
        >
          <n-checkbox :value="page.pageId" :label="page.title || page.pageId" />
          <div class="page-meta">
            <span>{{ pageTypeLabel(page.pageType) }}</span>
            <span v-if="page.objectCode">对象 {{ page.objectName || page.objectCode }}</span>
            <span v-else-if="page.dataBound">含数据依赖</span>
          </div>
        </label>
      </n-checkbox-group>
    </n-spin>

    <template #footer>
      <n-space justify="end">
        <n-button :disabled="exporting" @click="emit('update:show', false)">
          取消
        </n-button>
        <n-button type="primary" :loading="exporting" :disabled="loading" @click="runExport">
          {{ selectedPageIds.length ? `导出选中（${selectedPageIds.length}）` : '导出整应用' }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import {
  exportBusinessApplicationDebugBundle,
  listBusinessApplicationDebugBundlePages,
} from '@/api/business-application'

const props = defineProps({
  show: { type: Boolean, default: false },
  applicationId: { type: [Number, String], default: null },
  applicationCode: { type: String, default: '' },
})
const emit = defineEmits(['update:show', 'exported'])

const message = useMessage()
const loading = ref(false)
const exporting = ref(false)
const pages = ref([])
const selectedPageIds = ref([])

const allChecked = computed(() => pages.value.length > 0
  && selectedPageIds.value.length === pages.value.length)
const indeterminate = computed(() => selectedPageIds.value.length > 0
  && selectedPageIds.value.length < pages.value.length)

watch(() => props.show, (visible) => {
  if (!visible)
    return
  selectedPageIds.value = []
  pages.value = []
  void loadPages()
})

async function loadPages() {
  if (!props.applicationId)
    return
  loading.value = true
  try {
    const response = await listBusinessApplicationDebugBundlePages(props.applicationId)
    pages.value = response.data || []
    selectedPageIds.value = pages.value.map(page => page.pageId).filter(Boolean)
  }
  catch (error) {
    pages.value = []
    selectedPageIds.value = []
    message.error(error?.message || '加载页面列表失败')
  }
  finally {
    loading.value = false
  }
}

function toggleAll(checked) {
  selectedPageIds.value = checked
    ? pages.value.map(page => page.pageId).filter(Boolean)
    : []
}

function pageTypeLabel(pageType) {
  const map = {
    home: '首页',
    intro: '介绍页',
    object: '业务数据页',
    content: '内容页',
    entry: '入口页',
  }
  return map[pageType] || pageType || '页面'
}

function downloadBlobResponse(response, fallbackName) {
  let blob = response?.data instanceof Blob ? response.data : response
  // 兜底：若拦截器已把 JSON 附件解析成对象，重新包成文件流
  if (!(blob instanceof Blob) && blob && typeof blob === 'object') {
    blob = new Blob([JSON.stringify(blob, null, 2)], { type: 'application/json;charset=UTF-8' })
  }
  if (!(blob instanceof Blob))
    throw new TypeError('下载响应不是文件流')
  const disposition = response?.headers?.['content-disposition']
    || response?.headers?.get?.('content-disposition')
    || ''
  const utf8Match = disposition.match(/filename\*=utf-8''([^;]+)/i)
  const normalMatch = disposition.match(/filename="?([^";]+)"?/i)
  const fileName = utf8Match?.[1]
    ? decodeURIComponent(utf8Match[1])
    : (normalMatch?.[1] ? decodeURIComponent(normalMatch[1]) : fallbackName)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

async function runExport() {
  if (!props.applicationId || exporting.value)
    return
  exporting.value = true
  try {
    // 全选时不传 pageIds，走 FULL 模式，避免与整应用导出语义漂移
    const pageIds = allChecked.value ? [] : selectedPageIds.value
    const response = await exportBusinessApplicationDebugBundle(props.applicationId, pageIds)
    downloadBlobResponse(response, `${props.applicationCode || 'application'}.forge-app.json`)
    message.success(pageIds.length ? `已导出 ${pageIds.length} 个页面的应用配置` : '已导出整应用配置')
    emit('exported')
    emit('update:show', false)
  }
  catch (error) {
    message.error(error?.message || '导出应用配置失败')
  }
  finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.export-alert {
  margin-bottom: 12px;
}

.export-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.page-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: min(420px, 56vh);
  overflow: auto;
}

.page-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--n-color-embedded);
  cursor: pointer;
}

.page-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  padding-left: 28px;
  color: var(--n-text-color-3);
  font-size: 12px;
}
</style>
