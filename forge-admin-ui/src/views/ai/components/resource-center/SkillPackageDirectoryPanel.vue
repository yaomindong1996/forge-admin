<script setup>
import {
  NAlert,
  NButton,
  NDataTable,
  NDrawer,
  NDrawerContent,
  NInput,
  NModal,
  NSelect,
  NSpace,
  NTag,
} from 'naive-ui'
import { h, onMounted, reactive, ref } from 'vue'
import {
  skillPackageActivate,
  skillPackageImport,
  skillPackagePage,
  skillPackagePreview,
  skillPackageRetire,
  skillPackageVersionDetail,
} from '@/api/ai'
import './resource-center.css'

const loading = ref(false)
const keyword = ref('')
const status = ref(null)
const rows = ref([])
const pagination = reactive({ page: 1, pageSize: 15, itemCount: 0 })

const importModal = reactive({
  open: false,
  file: null,
  previewing: false,
  importing: false,
  preview: null,
})

const detailDrawer = reactive({
  open: false,
  loading: false,
  data: null,
})

const statusOptions = [
  { label: '已导入', value: 'IMPORTED' },
  { label: '已启用', value: 'READY' },
  { label: '已停用', value: 'RETIRED' },
]

function statusTag(code) {
  if (code === 'READY')
    return { type: 'success', label: '已启用' }
  if (code === 'RETIRED')
    return { type: 'default', label: '已停用' }
  return { type: 'warning', label: '待启用' }
}

async function loadPage(page = pagination.page) {
  loading.value = true
  pagination.page = page
  try {
    const res = await skillPackagePage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      keyword: keyword.value.trim() || undefined,
      status: status.value || undefined,
    })
    if (res.code === 200 && res.data) {
      rows.value = res.data.records || []
      pagination.itemCount = Number(res.data.total || 0)
    }
  }
  catch (e) {
    window.$message.error(e.message || '加载技能资料包失败')
  }
  finally {
    loading.value = false
  }
}

function openImport() {
  importModal.file = null
  importModal.preview = null
  importModal.open = true
}

async function handleFileChange(event) {
  const file = event.target?.files?.[0]
  if (!file)
    return
  importModal.file = file
  importModal.preview = null
  importModal.previewing = true
  try {
    const res = await skillPackagePreview(file)
    if (res.code === 200)
      importModal.preview = res.data
    else
      window.$message.error(res.msg || 'ZIP 预览校验失败')
  }
  catch (e) {
    window.$message.error(e.message || 'ZIP 预览校验失败')
  }
  finally {
    importModal.previewing = false
  }
}

async function handleConfirmImport() {
  if (!importModal.file || !importModal.preview?.archiveHash)
    return
  importModal.importing = true
  try {
    const res = await skillPackageImport(importModal.file, importModal.preview.archiveHash)
    if (res.code === 200) {
      window.$message.success('技能资料包已导入，启用后即可绑定至智能体')
      importModal.open = false
      await loadPage(1)
    }
    else {
      window.$message.error(res.msg || '导入失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '导入失败')
  }
  finally {
    importModal.importing = false
  }
}

async function handleActivate(row) {
  try {
    const res = await skillPackageActivate(row.versionId, {
      expectedStatus: row.status,
      expectedPackageHash: row.packageHash,
    })
    if (res.code === 200) {
      window.$message.success('技能版本已启用')
      await loadPage()
    }
    else {
      window.$message.error(res.msg || '启用失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '启用失败')
  }
}

async function handleRetire(row) {
  try {
    const res = await skillPackageRetire(row.versionId, {
      expectedStatus: row.status,
      expectedPackageHash: row.packageHash,
    })
    if (res.code === 200) {
      window.$message.success('技能版本已停用')
      await loadPage()
    }
    else {
      window.$message.error(res.msg || '停用失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '停用失败')
  }
}

async function openDetail(row) {
  detailDrawer.open = true
  detailDrawer.loading = true
  detailDrawer.data = null
  try {
    const res = await skillPackageVersionDetail(row.versionId)
    if (res.code === 200)
      detailDrawer.data = res.data
  }
  finally {
    detailDrawer.loading = false
  }
}

const columns = [
  {
    title: '技能资料包',
    key: 'packageName',
    minWidth: 200,
    render(row) {
      return h('div', [
        h('strong', { style: 'font-size: 13px;' }, row.packageName),
        h('div', { style: 'font-size: 11px; color: var(--text-tertiary); font-family: monospace;' }, `${row.packageCode} · v${row.versionName}`),
      ])
    },
  },
  {
    title: '说明',
    key: 'description',
    minWidth: 220,
    ellipsis: { tooltip: true },
    render: row => row.description || '-',
  },
  {
    title: '文件数',
    key: 'fileCount',
    width: 90,
    render: row => `${row.fileCount || 0} 个`,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      const meta = statusTag(row.status)
      return h(NTag, { size: 'small', type: meta.type, bordered: false }, () => meta.label)
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 190,
    render(row) {
      return h(NSpace, { size: 6 }, () => [
        h(NButton, { size: 'tiny', quaternary: true, onClick: () => openDetail(row) }, () => '查看文件'),
        row.status !== 'READY'
          ? h(NButton, { size: 'tiny', type: 'primary', secondary: true, onClick: () => handleActivate(row) }, () => '启用')
          : h(NButton, { size: 'tiny', secondary: true, onClick: () => handleRetire(row) }, () => '停用'),
      ])
    },
  },
]

onMounted(() => {
  void loadPage(1)
})
</script>

<template>
  <div class="rc-panel">
    <div class="rc-toolbar">
      <div class="rc-toolbar__filters">
        <NInput v-model:value="keyword" clearable placeholder="搜索技能名称或编码" style="width: 240px" @keyup.enter="loadPage(1)" />
        <NSelect v-model:value="status" clearable placeholder="全部状态" :options="statusOptions" style="width: 140px" @update:value="loadPage(1)" />
        <NButton secondary @click="loadPage(1)">
          查询
        </NButton>
      </div>
      <NButton type="primary" @click="openImport">
        导入 ZIP 技能包
      </NButton>
    </div>

    <NDataTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      :row-key="row => row.versionId"
      :pagination="pagination"
      remote
      :bordered="false"
      size="medium"
      @update:page="loadPage"
    />

    <NModal v-model:show="importModal.open" preset="card" title="导入技能资料包（ZIP）" :style="{ maxWidth: '580px', width: 'calc(100vw - 32px)' }">
      <div class="skill-import-body">
        <NAlert type="info" :bordered="false">
          支持包含 <code>SKILL.md</code> 或 <code>skill.json</code> 的只读文本 ZIP 包；运行期仅由智能体按需读取文档，不会执行任何脚本。
        </NAlert>
        <input type="file" accept=".zip" @change="handleFileChange">
        <div v-if="importModal.preview" class="skill-preview-box">
          <div><strong>{{ importModal.preview.packageName }}</strong>（{{ importModal.preview.packageCode }} v{{ importModal.preview.versionName }}）</div>
          <div class="skill-preview-meta">
            共 {{ importModal.preview.fileCount }} 个文本文件 · {{ importModal.preview.totalBytes }} 字节
          </div>
          <ul class="skill-file-list">
            <li v-for="file in importModal.preview.files" :key="file.filePath">
              {{ file.filePath }}
            </li>
          </ul>
        </div>
      </div>
      <template #action>
        <NSpace justify="end">
          <NButton @click="importModal.open = false">
            取消
          </NButton>
          <NButton
            type="primary"
            :disabled="!importModal.preview"
            :loading="importModal.importing || importModal.previewing"
            @click="handleConfirmImport"
          >
            确认导入
          </NButton>
        </NSpace>
      </template>
    </NModal>

    <NDrawer v-model:show="detailDrawer.open" :width="620">
      <NDrawerContent :title="detailDrawer.data ? `${detailDrawer.data.packageName} v${detailDrawer.data.versionName}` : '技能资料详情'" closable>
        <div v-if="detailDrawer.data" class="skill-files-detail">
          <div v-for="file in detailDrawer.data.files" :key="file.filePath" class="skill-file-card">
            <div class="skill-file-card__head">
              {{ file.filePath }}
            </div>
            <pre class="skill-file-card__content">{{ file.content }}</pre>
          </div>
        </div>
      </NDrawerContent>
    </NDrawer>
  </div>
</template>

<style scoped>
.skill-import-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skill-preview-box {
  padding: 12px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  font-size: 13px;
}

.skill-preview-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-tertiary, #6b7280);
}

.skill-file-list {
  margin: 8px 0 0;
  padding-left: 18px;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.skill-files-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skill-file-card {
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  overflow: hidden;
}

.skill-file-card__head {
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 600;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  background: var(--bg-secondary, #f6f8fb);
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

.skill-file-card__content {
  margin: 0;
  padding: 10px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 360px;
  overflow: auto;
}
</style>
