<template>
  <div class="notice-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/system/notice/user/page',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="noticeId"
      :show-add-button="false"
      :show-toolbar="false"
    >
      <!-- 自定义顶部 -->
      <template #toolbar-end>
        <NBadge :value="noticeStore.unreadCount" :max="99" show-zero>
          <n-button @click="refreshList">
            <template #icon>
              <i class="i-material-symbols:refresh" />
            </template>
            刷新
          </n-button>
        </NBadge>
      </template>
    </AiCrudPage>

    <!-- 详情弹窗 -->
    <n-modal
      v-model:show="showDetailModal"
      :title="currentNotice?.noticeTitle"
      preset="card"
      style="width: 900px"
      :segmented="{ content: 'soft' }"
    >
      <div v-if="currentNotice" class="notice-detail">
        <div class="detail-header">
          <n-space>
            <DictTag dict-type="sys_notice_type" :value="currentNotice.noticeType" />
            <span class="text-gray-500">发布人：{{ currentNotice.publisherName }}</span>
            <span class="text-gray-500">发布时间：{{ currentNotice.publishTime }}</span>
          </n-space>
        </div>

        <n-divider />

        <div class="detail-content" v-html="sanitizedNoticeContent" />

        <div v-if="currentNotice.attachments && currentNotice.attachments.length > 0" class="detail-attachments">
          <n-divider />
          <div class="attachment-title">
            附件
          </div>
          <n-space vertical>
            <div
              v-for="file in currentNotice.attachments"
              :key="file.fileId"
              class="attachment-item"
              @click="handleDownloadAttachment(file)"
            >
              <i class="i-material-symbols:attach-file" />
              <span>{{ file.fileName }}</span>
              <span class="text-gray-400">{{ formatFileSize(file.fileSize) }}</span>
              <i class="i-material-symbols:download" style="margin-left: auto" />
            </div>
          </n-space>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { NBadge, NTag } from 'naive-ui'
import { computed, h, onMounted, ref, watch } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useNoticeStore } from '@/stores/system/noticeStore'
import { request } from '@/utils'
import { downloadFile } from '@/utils/file'
import { sanitizeHtml } from '@/utils/sanitize-html'

defineOptions({ name: 'NoticeList' })

const NOTICE_TYPE_DICT = 'sys_notice_type'

const crudRef = ref(null)
const showDetailModal = ref(false)
const currentNotice = ref(null)

const noticeStore = useNoticeStore()
const { dict } = useDict(NOTICE_TYPE_DICT)

const noticeTypeOptions = computed(() => dict.value[NOTICE_TYPE_DICT] || [])
const sanitizedNoticeContent = computed(() => sanitizeHtml(currentNotice.value?.noticeContent))

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'noticeTitle',
    label: '标题',
    type: 'input',
    props: {
      placeholder: '请输入标题',
    },
  },
  {
    field: 'noticeType',
    label: '类型',
    type: 'select',
    props: {
      placeholder: '请选择类型',
      options: noticeTypeOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'noticeTitle',
    label: '标题',
    minWidth: 200,
    render: (row) => {
      return h('div', { class: 'flex items-center gap-8' }, [
        row.isRead === 0 ? h('span', { class: 'unread-dot' }) : null,
        h('span', {
          class: row.isRead === 0 ? 'font-semibold' : '',
          style: row.isRead === 0 ? 'color: #18a058' : '',
        }, row.noticeTitle),
        row.isTop === 1 ? h(NTag, { type: 'error', size: 'small' }, { default: () => '置顶' }) : null,
      ])
    },
  },
  {
    prop: 'noticeType',
    label: '类型',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: NOTICE_TYPE_DICT, value: row.noticeType, size: 'small' })
    },
  },
  {
    prop: 'publisherName',
    label: '发布人',
    width: 120,
  },
  {
    prop: 'publishTime',
    label: '发布时间',
    width: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 80,
    fixed: 'right',
    actions: [
      { label: '查看详情', key: 'view', onClick: handleView },
    ],
  },
])

// 查看详情
async function handleView(row) {
  try {
    const res = await request.get(`/system/notice/user/${encodeURIComponent(row.noticeId)}`)
    if (res.code === 200 && res.data) {
      currentNotice.value = res.data
      showDetailModal.value = true

      // 标记为已读并同步共享状态
      if (row.isRead === 0) {
        await markAsRead(row.noticeId)
      }
    }
  }
  catch {
    window.$message.error('获取详情失败')
  }
}

// 标记为已读并同步共享状态
async function markAsRead(noticeId) {
  try {
    await request.post('/system/notice/markAsRead', null, {
      params: { noticeId },
    })
    // 刷新共享 store 的未读计数
    await noticeStore.refreshCount()
    // 刷新列表
    crudRef.value?.refresh()
  }
  catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 刷新列表并同步共享状态
function refreshList() {
  crudRef.value?.refresh()
  noticeStore.refreshCount()
}

// 监听共享 store 的 contextVersion，当登录状态变化时刷新
watch(() => noticeStore.contextVersion, () => {
  crudRef.value?.refresh()
})

// 格式化文件大小
function formatFileSize(bytes) {
  if (!bytes)
    return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / k ** i).toFixed(2)} ${sizes[i]}`
}

// 下载附件
async function handleDownloadAttachment(file) {
  try {
    await downloadFile(file.fileId, file.fileName)
    window.$message.success('开始下载')
  }
  catch (error) {
    console.error('下载失败:', error)
    window.$message.error('下载失败')
  }
}

onMounted(() => {
  noticeStore.refreshCount()
})
</script>

<style scoped>
.notice-page {
  height: 100%;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #18a058;
  display: inline-block;
}

.notice-detail {
  padding: 16px 0;
}

.detail-header {
  margin-bottom: 16px;
}

.detail-content {
  line-height: 1.8;
  color: var(--text-primary);
  min-height: 200px;
}

.detail-content :deep(img) {
  max-width: 100%;
  height: auto;
}

.detail-attachments {
  margin-top: 24px;
}

.attachment-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--n-border-color);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.attachment-item:hover {
  background-color: var(--n-color-hover);
}
</style>
