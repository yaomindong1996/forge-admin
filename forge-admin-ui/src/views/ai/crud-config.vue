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
        <n-button type="warning" size="small" @click="goToGenerator()">
          <template #icon>
            <n-icon><SparklesOutline /></n-icon>
          </template>
          AI 生成配置
        </n-button>
      </template>

      <!-- 搜索配置可视化编辑器 -->
      <template #form-searchSchemaSlot="{ formData, updateValue }">
        <n-form-item label="搜索配置" :span="2" style="grid-column: span 2">
          <SchemaFieldEditor
            mode="search"
            :value="formData.searchSchema || ''"
            @update:value="updateValue"
          />
        </n-form-item>
      </template>

      <!-- 表格列可视化编辑器 -->
      <template #form-columnsSchemaSlot="{ formData, updateValue }">
        <n-form-item label="表格列配置" :span="2" style="grid-column: span 2">
          <SchemaFieldEditor
            mode="columns"
            :value="formData.columnsSchema || ''"
            @update:value="updateValue"
          />
        </n-form-item>
      </template>

      <!-- 编辑表单可视化编辑器 -->
      <template #form-editSchemaSlot="{ formData, updateValue }">
        <n-form-item label="编辑表单配置" :span="2" style="grid-column: span 2">
          <SchemaFieldEditor
            mode="edit"
            :value="formData.editSchema || ''"
            @update:value="updateValue"
          />
        </n-form-item>
      </template>

      <!-- API配置可视化编辑器 -->
      <template #form-apiConfigSlot="{ formData, updateValue }">
        <n-form-item label="API配置" :span="2" style="grid-column: span 2">
          <ApiConfigEditor
            :value="formData.apiConfig || ''"
            @update:value="updateValue"
          />
        </n-form-item>
      </template>

      <!-- 字典配置 -->
      <template #form-dictConfigSlot="{ formData, updateValue }">
        <n-form-item label="字典配置" :span="2" style="grid-column: span 2">
          <DictConfigPanel
            :search-schema="formData.searchSchema || ''"
            :columns-schema="formData.columnsSchema || ''"
            :edit-schema="formData.editSchema || ''"
            :dict-config="formData.dictConfig || ''"
            @update:value="updateValue"
          />
        </n-form-item>
      </template>

      <!-- 脱敏配置 -->
      <template #form-desensitizeConfigSlot="{ formData, updateValue }">
        <n-form-item label="脱敏配置" :span="2" style="grid-column: span 2">
          <DesensitizeConfigPanel
            :value="formData.desensitizeConfig || ''"
            :columns-schema="formData.columnsSchema || ''"
            @update:value="updateValue"
          />
        </n-form-item>
      </template>

      <!-- 加解密配置 -->
      <template #form-encryptConfigSlot="{ formData, updateValue }">
        <n-form-item label="加解密配置" :span="2" style="grid-column: span 2">
          <EncryptConfigPanel
            :value="formData.encryptConfig || ''"
            @update:value="updateValue"
          />
        </n-form-item>
      </template>

      <!-- 翻译配置 -->
      <template #form-transConfigSlot="{ formData, updateValue }">
        <n-form-item label="翻译配置" :span="2" style="grid-column: span 2">
          <TransConfigPanel
            :value="formData.transConfig || ''"
            :columns-schema="formData.columnsSchema || ''"
            :edit-schema="formData.editSchema || ''"
            @update:value="updateValue"
          />
        </n-form-item>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NTag } from 'naive-ui'
import { SparklesOutline } from '@vicons/ionicons5'
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
const { error } = useDiscreteMessage()

const apiConfig = {
  list: 'get@/ai/crud-config/page',
  detail: 'get@/ai/crud-config/{id}',
  create: 'post@/ai/crud-config',
  update: 'put@/ai/crud-config',
  delete: 'delete@/ai/crud-config/{id}',
}

const searchSchema = [
  { field: 'configKey', label: '配置键', type: 'input' },
  { field: 'tableName', label: '表名', type: 'input' },
]

function goToGenerator(configKey) {
  const query = configKey ? { configKey } : {}
  router.push({ path: '/ai/crud-generator', query })
}

const tableColumns = [
  { prop: 'configKey', title: '配置键', width: 160 },
  { prop: 'tableName', title: '数据表', width: 160 },
  { prop: 'tableComment', title: '表描述', width: 160 },
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
  { prop: 'menuName', title: '菜单名称', width: 140 },
  { prop: 'createTime', title: '创建时间', width: 180 },
  {
    // 使用 key: 'action' （框架检测单数）防止自动安插默认操作列
    key: 'action',
    prop: 'action',
    title: '操作',
    width: 220,
    fixed: 'right',
    render: row => h('div', { class: 'table-action-column' }, [
      h('a', {
        class: 'table-action-link',
        style: { color: '#f0a020' },
        onClick: () => goToGenerator(row.configKey),
      }, 'AI 生成'),
      h('span', { class: 'table-action-divider' }, ' | '),
      h('a', {
        class: 'table-action-link',
        onClick: () => crudRef.value?.handleEdit(row),
      }, '编辑'),
      h('span', { class: 'table-action-divider' }, ' | '),
      h('a', {
        class: 'table-action-link danger',
        onClick: () => handleDelete(row),
      }, '删除'),
    ]),
  },
]

const editSchema = [
  { field: 'configKey', label: '配置键', type: 'input', required: true },
  { field: 'tableName', label: '数据表', type: 'input', required: true },
  { field: 'tableComment', label: '表描述', type: 'input' },
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
  { field: 'menuName', label: '菜单名称', type: 'input' },
  { field: 'menuSort', label: '菜单排序', type: 'number' },
  // 以下字段用 slot 类型，在 template 中用可视化编辑器渲染
  { field: 'searchSchema', type: 'slot', slotName: 'searchSchemaSlot', span: 2 },
  { field: 'columnsSchema', type: 'slot', slotName: 'columnsSchemaSlot', span: 2 },
  { field: 'editSchema', type: 'slot', slotName: 'editSchemaSlot', span: 2 },
  { field: 'apiConfig', type: 'slot', slotName: 'apiConfigSlot', span: 2 },
  { field: 'dictConfig', type: 'slot', slotName: 'dictConfigSlot', span: 2 },
  { field: 'desensitizeConfig', type: 'slot', slotName: 'desensitizeConfigSlot', span: 2 },
  { field: 'encryptConfig', type: 'slot', slotName: 'encryptConfigSlot', span: 2 },
  { field: 'transConfig', type: 'slot', slotName: 'transConfigSlot', span: 2 },
  { field: 'options', label: '扩展配置JSON', type: 'textarea', props: { rows: 3 } },
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
