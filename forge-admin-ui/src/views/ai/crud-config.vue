<template>
  <div class="ai-crud-config-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="apiConfig"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      :edit-grid-cols="2"
      modal-width="1000px"
      :hide-add="true"
    >
      <template #toolbar-start>
        <n-button type="primary" size="small" @click="goToGenerator()">
          <template #icon>
            <n-icon><SparklesOutline /></n-icon>
          </template>
          AI 生成配置
        </n-button>
      </template>

      <!-- 搜索配置可视化编辑器 -->
      <template #form-searchSchemaSlot="{ formData, updateValue }">
        <n-form-item label="搜索配置" :span="2" style="grid-column: span 2">
          <div class="schema-editor-wrapper">
            <SchemaFieldEditor
              mode="search"
              :value="formData.searchSchema || ''"
              @update:value="updateValue"
            />
          </div>
        </n-form-item>
      </template>

      <!-- 表格列可视化编辑器 -->
      <template #form-columnsSchemaSlot="{ formData, updateValue }">
        <n-form-item label="表格列配置" :span="2" style="grid-column: span 2">
          <div class="schema-editor-wrapper">
            <SchemaFieldEditor
              mode="columns"
              :value="formData.columnsSchema || ''"
              @update:value="updateValue"
            />
          </div>
        </n-form-item>
      </template>

      <!-- 编辑表单可视化编辑器 -->
      <template #form-editSchemaSlot="{ formData, updateValue }">
        <n-form-item label="编辑表单配置" :span="2" style="grid-column: span 2">
          <div class="schema-editor-wrapper">
            <SchemaFieldEditor
              mode="edit"
              :value="formData.editSchema || ''"
              @update:value="updateValue"
            />
          </div>
        </n-form-item>
      </template>

      <!-- API配置可视化编辑器 -->
      <template #form-apiConfigSlot="{ formData, updateValue }">
        <n-form-item label="API配置" :span="2" style="grid-column: span 2">
          <div class="schema-editor-wrapper">
            <ApiConfigEditor
              :value="formData.apiConfig || ''"
              @update:value="updateValue"
            />
          </div>
        </n-form-item>
      </template>

      <!-- 字典配置 -->
      <template #form-dictConfigSlot="{ formData, updateValue }">
        <n-form-item label="字典配置" :span="2" style="grid-column: span 2">
          <div class="schema-editor-wrapper">
            <DictConfigPanel
              :search-schema="formData.searchSchema || ''"
              :columns-schema="formData.columnsSchema || ''"
              :edit-schema="formData.editSchema || ''"
              :dict-config="formData.dictConfig || ''"
              @update:value="updateValue"
            />
          </div>
        </n-form-item>
      </template>

      <!-- 脱敏配置 -->
      <template #form-desensitizeConfigSlot="{ formData, updateValue }">
        <n-form-item label="脱敏配置" :span="2" style="grid-column: span 2">
          <div class="schema-editor-wrapper">
            <DesensitizeConfigPanel
              :value="formData.desensitizeConfig || ''"
              :columns-schema="formData.columnsSchema || ''"
              @update:value="updateValue"
            />
          </div>
        </n-form-item>
      </template>

      <!-- 加解密配置 -->
      <template #form-encryptConfigSlot="{ formData, updateValue }">
        <n-form-item label="加解密配置" :span="2" style="grid-column: span 2">
          <div class="schema-editor-wrapper">
            <EncryptConfigPanel
              :value="formData.encryptConfig || ''"
              @update:value="updateValue"
            />
          </div>
        </n-form-item>
      </template>

      <!-- 翻译配置 -->
      <template #form-transConfigSlot="{ formData, updateValue }">
        <n-form-item label="翻译配置" :span="2" style="grid-column: span 2">
          <div class="schema-editor-wrapper">
            <TransConfigPanel
              :value="formData.transConfig || ''"
              :columns-schema="formData.columnsSchema || ''"
              :edit-schema="formData.editSchema || ''"
              @update:value="updateValue"
            />
          </div>
        </n-form-item>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NTag, NTooltip, NIcon } from 'naive-ui'
import { SparklesOutline, EyeOutline, CloudDownloadOutline } from '@vicons/ionicons5'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import { useDiscreteMessage } from '@/composables/useDiscreteMessage'
import SchemaFieldEditor from './components/SchemaFieldEditor.vue'
import ApiConfigEditor from './components/ApiConfigEditor.vue'
import DictConfigPanel from './components/DictConfigPanel.vue'
import DesensitizeConfigPanel from './components/DesensitizeConfigPanel.vue'
import EncryptConfigPanel from './components/EncryptConfigPanel.vue'
import TransConfigPanel from './components/TransConfigPanel.vue'

defineOptions({ name: 'AiCrudConfig' })

const crudRef = ref(null)
const router = useRouter()
const { error, success } = useDiscreteMessage()

const apiConfig = {
  list: 'get@/ai/crud-config/page',
  detail: 'get@/ai/crud-config/{id}',
  create: 'post@/ai/crud-config',
  update: 'put@/ai/crud-config',
  delete: 'delete@/ai/crud-config/{id}',
}

const searchSchema = [
  { field: 'configKey', label: '配置键', type: 'input', placeholder: '请输入配置键' },
  { field: 'tableName', label: '表名', type: 'input', placeholder: '请输入表名' },
]

function goToGenerator(configKey) {
  const query = configKey ? { configKey } : {}
  router.push({ path: '/ai/crud-generator', query })
}

/**
 * 预览动态页面
 */
function handlePreview(configKey) {
  const resolved = router.resolve({ path: `/ai/crud-page/${configKey}` })
  window.open(resolved.href, '_blank')
}

/**
 * 下载代码包（携带防重放 header）
 */
async function handleDownloadCode(configKey) {
  const { useAuthStore } = await import('@/store')
  const authStore = useAuthStore()
  const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''
  const url = `${BASE_URL}/ai/crud-config/codegen/download/${configKey}`
  const uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
  })
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: {
        Authorization: authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
        'X-Timestamp': Date.now().toString(),
        'X-Nonce': uuid,
      },
    })
    if (!resp.ok) {
      const text = await resp.text()
      window.$message?.error('下载失败: ' + (text || resp.statusText))
      return
    }
    const blob = await resp.blob()
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = `${configKey}-code.zip`
    a.click()
    URL.revokeObjectURL(blobUrl)
    window.$message?.success('代码包下载成功')
  } catch (e) {
    window.$message?.error('下载失败: ' + e.message)
  }
}

const tableColumns = [
  { prop: 'configKey', title: '配置键', width: 160, ellipsis: true },
  { prop: 'tableName', title: '数据表', width: 160, ellipsis: true },
  { prop: 'tableComment', title: '表描述', width: 160, ellipsis: true },
  {
    prop: 'mode',
    title: '模式',
    width: 100,
    render: row => h(NTag, { type: row.mode === 'CONFIG' ? 'success' : 'info', size: 'small' },
      () => row.mode === 'CONFIG' ? '配置驱动' : '代码生成'),
  },
  {
    prop: 'status',
    title: '状态',
    width: 80,
    render: row => h(NTag, { type: row.status === '0' ? 'success' : 'error', size: 'small' },
      () => row.status === '0' ? '启用' : '停用'),
  },
  { prop: 'menuName', title: '菜单名称', width: 140, ellipsis: true },
  { prop: 'createTime', title: '创建时间', width: 180 },
  {
    // 使用 key: 'action' （框架检测单数）防止自动安插默认操作列
    key: 'action',
    prop: 'action',
    title: '操作',
    width: 280,
    fixed: 'right',
    render: row => h('div', { class: 'table-action-column' }, [
      // AI 生成
      h('a', {
        class: 'table-action-link',
        style: { color: '#6366F1' },
        onClick: () => goToGenerator(row.configKey),
      }, 'AI 生成'),
      h('span', { class: 'table-action-divider' }, ' | '),
      // 编辑
      h('a', {
        class: 'table-action-link',
        onClick: () => crudRef.value?.handleEdit(row),
      }, '编辑'),
      h('span', { class: 'table-action-divider' }, ' | '),
      // 预览效果
      h(NTooltip, { placement: 'top' }, {
        trigger: () => h('a', {
          class: 'table-action-link icon-action',
          onClick: () => handlePreview(row.configKey),
        }, [
          h(NIcon, { size: 16, style: { verticalAlign: 'middle' } }, { default: () => h(EyeOutline) }),
        ]),
        default: () => '预览效果',
      }),
      h('span', { class: 'table-action-divider' }, ' | '),
      // 下载代码
      h(NTooltip, { placement: 'top' }, {
        trigger: () => h('a', {
          class: 'table-action-link icon-action',
          onClick: () => handleDownloadCode(row.configKey),
        }, [
          h(NIcon, { size: 16, style: { verticalAlign: 'middle' } }, { default: () => h(CloudDownloadOutline) }),
        ]),
        default: () => '下载代码',
      }),
      h('span', { class: 'table-action-divider' }, ' | '),
      // 删除
      h('a', {
        class: 'table-action-link danger',
        onClick: () => handleDelete(row),
      }, '删除'),
    ]),
  },
]

const editSchema = [
  { field: 'configKey', label: '配置键', type: 'input', required: true, placeholder: '请输入配置键' },
  { field: 'tableName', label: '数据表', type: 'input', required: true, placeholder: '请输入表名' },
  { field: 'tableComment', label: '表描述', type: 'input', placeholder: '请输入表描述' },
  {
    field: 'mode',
    label: '模式',
    type: 'select',
    required: true,
    options: [
      { label: '配置驱动', value: 'CONFIG' },
      { label: '代码生成', value: 'CODEGEN' },
    ],
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '启用', value: '0' },
      { label: '停用', value: '1' },
    ],
  },
  { field: 'menuName', label: '菜单名称', type: 'input', placeholder: '请输入菜单名称' },
  { field: 'menuSort', label: '菜单排序', type: 'number', placeholder: '请输入菜单排序' },
  // 以下字段用 slot 类型，在 template 中用可视化编辑器渲染
  { field: 'searchSchema', type: 'slot', slotName: 'searchSchemaSlot', span: 2 },
  { field: 'columnsSchema', type: 'slot', slotName: 'columnsSchemaSlot', span: 2 },
  { field: 'editSchema', type: 'slot', slotName: 'editSchemaSlot', span: 2 },
  { field: 'apiConfig', type: 'slot', slotName: 'apiConfigSlot', span: 2 },
  { field: 'dictConfig', type: 'slot', slotName: 'dictConfigSlot', span: 2 },
  { field: 'desensitizeConfig', type: 'slot', slotName: 'desensitizeConfigSlot', span: 2 },
  { field: 'encryptConfig', type: 'slot', slotName: 'encryptConfigSlot', span: 2 },
  { field: 'transConfig', type: 'slot', slotName: 'transConfigSlot', span: 2 },
  { field: 'options', label: '扩展配置JSON', type: 'textarea', props: { rows: 3, placeholder: '请输入扩展配置JSON' } },
]

/**
 * 自定义删除：调用建有删除查验的后端接口
 */
async function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除配置「${row.configKey}」吗？如果该配置已注册菜单，将同时删除对应菜单。`,
    positiveText: '删除',
    negativeText: '取消',
    type: 'warning',
    onPositiveClick: async () => {
      try {
        const { request } = await import('@/utils')
        await request.delete(`/ai/crud-config/${row.id}`)
        window.$message.success('删除成功')
        crudRef.value?.refresh()
      }
      catch (e) {
        const msg = e?.response?.data?.msg || e?.message || '删除失败'
        window.$message.error(msg)
      }
    },
  })
}
</script>

<style scoped>
.ai-crud-config-page {
  width: 100%;
  height: 100%;
  background: #F5F3FF;
}

.schema-editor-wrapper {
  border: 1px solid #E0E7FF;
  border-radius: 6px;
  overflow: hidden;
  background: #FFFFFF;
}

.schema-editor-wrapper :deep(.schema-editor) {
  padding: 8px;
}

.table-action-column {
  display: flex;
  align-items: center;
  gap: 4px;
}

.table-action-link {
  color: #6366F1;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
  font-size: 13px;
}

.table-action-link:hover {
  background: #E0E7FF;
  text-decoration: none;
}

.table-action-link.danger {
  color: #EF4444;
}

.table-action-link.danger:hover {
  background: #FEE2E2;
}

.table-action-link.icon-action {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
}

.table-action-divider {
  color: #D1D5DB;
  font-size: 12px;
}
</style>
