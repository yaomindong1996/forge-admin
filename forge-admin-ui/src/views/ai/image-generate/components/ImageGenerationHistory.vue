<template>
  <NCard size="small" title="生成历史">
    <!-- 只读历史，与当前任务结果查询互不覆盖 -->
    <NAlert v-if="error" type="warning" class="history-error" :show-icon="false">
      {{ error }}
    </NAlert>
    <NDataTable
      :scroll-x="650"
      :columns="columns" :data="records" :pagination="pagination" :loading="loading" :row-key="row => row.id"
      remote @update:page="load"
    />
  </NCard>
</template>

<script setup>
import { NAlert, NButton, NCard, NDataTable } from 'naive-ui'
import { h, onActivated, onBeforeUnmount, onDeactivated, ref, watch } from 'vue'
import { imageGeneratePage } from '@/api/ai/media'
import DictTag from '@/components/DictTag.vue'
import { useAiImagePermission } from '@/composables/ai/useAiImagePermission'
import { useUserStore } from '@/store/modules/user'
import { imageRecordId } from '@/stores/ai/image-job-utils'
import { useAiImageJobsStore } from '@/stores/ai/imageJobsStore'
import { formatDateTime } from '@/utils/common'

const props = defineProps({ refreshKey: { type: Number, default: 0 } })
const emit = defineEmits(['select'])
const user = useUserStore()
const jobs = useAiImageJobsStore()
const { canQuery, canHistory } = useAiImagePermission()
const records = ref([])
const loading = ref(false)
const error = ref('')
const active = ref(true)
const pagination = ref({ page: 1, pageSize: 10, itemCount: 0 })
let controller = null
let requestVersion = 0
const columns = [
  { title: '提示词', key: 'prompt', ellipsis: { tooltip: true } },
  { title: '尺寸', key: 'size', width: 120 },
  { title: '状态', key: 'status', width: 110, render: renderStatus },
  { title: '创建时间', key: 'createTime', width: 180, render: renderTime },
  { title: '操作', key: 'actions', width: 90, render: renderAction },
]

function renderStatus(row) {
  return h(DictTag, { dictType: 'ai_image_generate_status', value: row.status, bordered: false })
}
function renderTime(row) {
  return row.createTime ? formatDateTime(row.createTime) : '—'
}
function renderAction(row) {
  if (!canQuery.value || String(row.userId) !== String(user.userId) || !imageRecordId(row.id)) {
    return '—'
  }
  const select = () => emit('select', imageRecordId(row.id))
  return h(NButton, { text: true, type: 'primary', onClick: select }, () => '查看结果')
}

function cancel() {
  requestVersion += 1
  controller?.abort()
  controller = null
  loading.value = false
}

async function load(page = pagination.value.page) {
  cancel()
  if (!active.value || !canHistory.value) {
    return
  }
  const version = requestVersion
  controller = new AbortController()
  loading.value = true
  error.value = ''
  try {
    const params = { pageNum: page, pageSize: pagination.value.pageSize }
    const config = { signal: controller.signal, needTip: false }
    const response = await imageGeneratePage(params, config)
    if (version !== requestVersion) {
      return
    }
    if (response?.code !== 200 || !Array.isArray(response.data?.records)) {
      throw new Error('无效历史响应')
    }
    records.value = response.data.records
    pagination.value = { ...pagination.value, page, itemCount: response.data.total || 0 }
  }
  catch {
    if (version === requestVersion) {
      error.value = '加载生成历史失败，请刷新页面后重试'
    }
  }
  finally {
    if (version === requestVersion) {
      loading.value = false
      controller = null
    }
  }
}

let loadedContextVersion = jobs.contextVersion
watch([active, canHistory, () => props.refreshKey, () => jobs.contextVersion], () => {
  records.value = []
  if (loadedContextVersion !== jobs.contextVersion) {
    loadedContextVersion = jobs.contextVersion
    pagination.value = { ...pagination.value, page: 1, itemCount: 0 }
  }
  load()
}, { immediate: true })
onActivated(() => {
  active.value = true
})
onDeactivated(() => {
  active.value = false
  cancel()
})
onBeforeUnmount(cancel)
</script>

<style scoped>
.history-error {
  margin-bottom: 12px;
}
</style>
