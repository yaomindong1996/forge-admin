<template>
  <div class="generator-template-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/generator/template/list',
        detail: 'get@/generator/template/{id}',
        add: 'post@/generator/template/add',
        update: 'post@/generator/template/edit',
        delete: 'post@/generator/template/remove/{id}',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="templateId"
      :edit-grid-cols="2"
      modal-width="1200px"
      add-button-text="新增模板"
    >
      <!-- 自定义模板内容编辑 -->
      <template #edit-templateContent>
        <div class="template-editor">
          <div ref="templateEditorRef" class="editor-container" />
        </div>
      </template>
    </AiCrudPage>

    <!-- 模板预览弹窗 -->
    <n-modal
      v-model:show="showPreviewModal"
      title="模板预览"
      preset="card"
      style="width: 1200px"
      :mask-closable="false"
    >
      <div class="preview-modal-content">
        <!-- 选择表 -->
        <div class="table-select">
          <n-form-item label="选择表" label-placement="left">
            <n-select
              v-model:value="previewTableId"
              :options="tableOptions"
              placeholder="请选择要预览的表"
              style="width: 300px"
              @update:value="handlePreviewTable"
            />
          </n-form-item>
        </div>

        <!-- 预览结果 -->
        <div class="preview-result">
          <n-spin :show="previewLoading">
            <div ref="previewEditorRef" class="preview-editor-container" />
          </n-spin>
        </div>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showPreviewModal = false">
            关闭
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { java } from '@codemirror/lang-java'
import { oneDark } from '@codemirror/theme-one-dark'
import { basicSetup, EditorView } from 'codemirror'
import { computed, h, nextTick, onUnmounted, ref, watch } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'
import { toNumberDictOptions } from '@/utils/dict-options'

defineOptions({ name: 'GeneratorTemplate' })

const crudRef = ref(null)
const templateEditorRef = ref(null)
const previewEditorRef = ref(null)
const showPreviewModal = ref(false)
const previewLoading = ref(false)
const previewTableId = ref(null)
const currentTemplateId = ref(null)
const tableOptions = ref([])
const { dict } = useDict('gen_template_type', 'gen_template_engine', 'sys_enable_disable', 'sys_yes_no')

let templateEditorView = null
let previewEditorView = null

const templateTypeOptions = computed(() => dict.value.gen_template_type || [])
const templateEngineOptions = computed(() => dict.value.gen_template_engine || [])
const enableDisableOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))
const yesNoOptions = computed(() => toNumberDictOptions(dict.value.sys_yes_no))

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'templateName',
    label: '模板名称',
    type: 'input',
    props: {
      placeholder: '请输入模板名称',
    },
  },
  {
    field: 'templateType',
    label: '模板类型',
    type: 'select',
    props: {
      placeholder: '请选择模板类型',
      options: templateTypeOptions.value,
      clearable: true,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'templateName',
    label: '模板名称',
    width: 150,
  },
  {
    prop: 'templateCode',
    label: '模板编码',
    width: 130,
  },
  {
    prop: 'templateType',
    label: '模板类型',
    width: 100,
    render: row => h(DictTag, { options: templateTypeOptions.value, value: row.templateType, size: 'small' }),
  },
  {
    prop: 'templateEngine',
    label: '模板引擎',
    width: 100,
    render: row => h(DictTag, { options: templateEngineOptions.value, value: row.templateEngine, size: 'small' }),
  },
  {
    prop: 'fileSuffix',
    label: '文件后缀',
    width: 100,
  },
  {
    prop: 'isSystem',
    label: '系统内置',
    width: 90,
    render: row => h(DictTag, { options: yesNoOptions.value, value: row.isSystem, size: 'small' }),
  },
  {
    prop: 'isEnabled',
    label: '状态',
    width: 80,
    render: row => h(DictTag, { options: enableDisableOptions.value, value: row.isEnabled, size: 'small' }),
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '预览', key: 'preview', type: 'primary', onClick: handlePreview },
      { label: '复制', key: 'copy', type: 'primary', onClick: handleCopy },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete, visible: row => row.isSystem !== 1 },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    field: 'templateName',
    label: '模板名称',
    type: 'input',
    rules: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入模板名称',
    },
  },
  {
    field: 'templateCode',
    label: '模板编码',
    type: 'input',
    rules: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入模板编码',
    },
  },
  {
    field: 'templateType',
    label: '模板类型',
    type: 'select',
    rules: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
    props: {
      placeholder: '请选择模板类型',
      options: templateTypeOptions.value,
    },
  },
  {
    field: 'templateEngine',
    label: '模板引擎',
    type: 'select',
    defaultValue: 'VELOCITY',
    rules: [{ required: true, message: '请选择模板引擎', trigger: 'change' }],
    props: {
      placeholder: '请选择模板引擎',
      options: templateEngineOptions.value,
    },
  },
  {
    field: 'fileSuffix',
    label: '文件后缀',
    type: 'input',
    defaultValue: '.java',
    props: {
      placeholder: '如：.java、.xml、.vue',
    },
  },
  {
    field: 'filePath',
    label: '生成路径',
    type: 'input',
    props: {
      placeholder: `如：src/main/java/\${package}`,
    },
  },
  {
    field: 'isEnabled',
    label: '启用状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: enableDisableOptions.value,
    },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'number',
    defaultValue: 0,
    props: {
      placeholder: '请输入排序号',
      min: 0,
    },
  },
  {
    field: 'templateContent',
    label: '模板内容',
    type: 'slot',
    span: 2,
    _slot: 'templateContent',
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注',
      rows: 2,
    },
  },
])

// 监听编辑弹窗打开
watch(() => crudRef.value?.editModalVisible, async (val) => {
  if (val) {
    await nextTick()
    initTemplateEditor()
  }
  else {
    destroyTemplateEditor()
  }
})

// 初始化模板编辑器
function initTemplateEditor() {
  destroyTemplateEditor()

  if (!templateEditorRef.value)
    return

  const formData = crudRef.value?.formData
  const content = formData?.templateContent || ''

  templateEditorView = new EditorView({
    doc: content,
    extensions: [
      basicSetup,
      java(),
      oneDark,
      EditorView.lineWrapping,
      EditorView.updateListener.of((update) => {
        if (update.docChanged && crudRef.value?.formData) {
          crudRef.value.formData.templateContent = update.state.doc.toString()
        }
      }),
    ],
    parent: templateEditorRef.value,
  })
}

// 销毁模板编辑器
function destroyTemplateEditor() {
  if (templateEditorView) {
    templateEditorView.destroy()
    templateEditorView = null
  }
}

// 初始化预览编辑器
function initPreviewEditor(code) {
  destroyPreviewEditor()

  if (!previewEditorRef.value)
    return

  previewEditorView = new EditorView({
    doc: code || '',
    extensions: [
      basicSetup,
      java(),
      oneDark,
      EditorView.editable.of(false),
      EditorView.lineWrapping,
    ],
    parent: previewEditorRef.value,
  })
}

// 销毁预览编辑器
function destroyPreviewEditor() {
  if (previewEditorView) {
    previewEditorView.destroy()
    previewEditorView = null
  }
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  if (row.isSystem === 1) {
    window.$message.warning('系统内置模板不允许删除')
    return
  }

  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除模板"${row.templateName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post(`/generator/template/remove/${row.templateId}`)
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}

// 预览
async function handlePreview(row) {
  currentTemplateId.value = row.templateId
  previewTableId.value = null
  showPreviewModal.value = true

  // 加载表列表
  await loadTableList()
}

// 加载表列表
async function loadTableList() {
  try {
    const res = await request.get('/generator/list', { params: { pageNum: 1, pageSize: 100 } })
    if (res.code === 200) {
      const records = res.data?.records || res.data?.list || []
      tableOptions.value = records.map(item => ({
        label: `${item.tableName} (${item.tableComment || ''})`,
        value: item.tableId,
      }))
    }
  }
  catch (error) {
    console.error('加载表列表失败:', error)
  }
}

// 预览模板
async function handlePreviewTable(tableId) {
  if (!tableId || !currentTemplateId.value)
    return

  try {
    previewLoading.value = true
    const res = await request.post('/generator/template/preview', {
      templateId: currentTemplateId.value,
      tableId,
    })
    if (res.code === 200) {
      await nextTick()
      initPreviewEditor(res.data)
    }
  }
  catch (error) {
    console.error('预览失败:', error)
    window.$message.error('预览失败')
  }
  finally {
    previewLoading.value = false
  }
}

// 复制模板
function handleCopy(row) {
  const copyData = {
    ...row,
    templateId: null,
    templateName: `${row.templateName}_副本`,
    templateCode: `${row.templateCode}_copy`,
    isSystem: 0,
  }
  crudRef.value?.showAdd(copyData)
}

// 组件卸载
onUnmounted(() => {
  destroyTemplateEditor()
  destroyPreviewEditor()
})
</script>

<style scoped>
.generator-template-page {
  height: 100%;
}

.template-editor {
  border: 1px solid var(--n-border-color);
  border-radius: 4px;
  overflow: hidden;
}

.editor-container {
  height: 300px;
  overflow: auto;
}

.editor-container :deep(.cm-editor) {
  height: 100%;
}

.preview-modal-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.table-select {
  padding-bottom: 8px;
  border-bottom: 1px solid var(--n-border-color);
}

.preview-result {
  min-height: 400px;
}

.preview-editor-container {
  height: 400px;
  border: 1px solid var(--n-border-color);
  border-radius: 4px;
  overflow: hidden;
}

.preview-editor-container :deep(.cm-editor) {
  height: 100%;
}
</style>
