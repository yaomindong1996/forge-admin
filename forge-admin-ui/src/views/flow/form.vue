<template>
  <div class="h-full flex flex-col p-16">
    <div class="flex flex-col flex-1 overflow-hidden rounded bg-white p-16">
      <div class="mb-16 flex items-center justify-between">
        <h2 class="text-18 font-bold">
          表单管理
        </h2>
        <NSpace>
          <NButton type="primary" @click="handleAdd">
            <template #icon>
              <i class="i-material-symbols:add" />
            </template>
            新增表单
          </NButton>
        </NSpace>
      </div>
      <n-alert type="info" :bordered="false" class="mb-16">
        该列表仅用于未绑定业务应用的独立流程，业务应用请到应用中心配置表单与流程。
      </n-alert>

      <!-- 搜索栏 -->
      <NSpace class="mb-16" :vertical="false">
        <n-input
          v-model:value="queryParams.formName"
          placeholder="搜索表单名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-select
          v-model:value="queryParams.status"
          placeholder="状态"
          clearable
          style="width: 120px"
          :options="statusOptions"
        />
        <NButton type="primary" @click="handleSearch">
          <template #icon>
            <i class="i-material-symbols:search" />
          </template>
          搜索
        </NButton>
        <NButton @click="handleReset">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          重置
        </NButton>
      </NSpace>

      <!-- 数据表格 -->
      <n-data-table
        :columns="columns"
        :data="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
        class="flex-1"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showModal"
        preset="card"
        :title="modalTitle"
        style="width: 600px"
        :mask-closable="false"
      >
        <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="100">
          <n-form-item label="表单名称" path="formName">
            <n-input v-model:value="formData.formName" placeholder="请输入表单名称" />
          </n-form-item>
          <n-form-item label="表单Key" path="formKey">
            <n-input
              v-model:value="formData.formKey"
              placeholder="请输入表单Key（唯一标识）"
              :disabled="!!formData.id"
            />
          </n-form-item>
          <n-form-item label="表单类型" path="formType">
            <n-select
              v-model:value="formData.formType"
              placeholder="请选择表单类型"
              :options="formTypeOptions"
            />
          </n-form-item>
          <n-form-item label="表单分类" path="formCategory">
            <n-input v-model:value="formData.formCategory" placeholder="如人事、财务、运营" />
          </n-form-item>
          <n-form-item label="数据模式" path="defaultDataMode">
            <n-select
              v-model:value="formData.defaultDataMode"
              placeholder="请选择默认数据模式"
              :options="dataModeOptions"
            />
          </n-form-item>
          <n-form-item label="描述" path="description">
            <n-input
              v-model:value="formData.description"
              type="textarea"
              placeholder="请输入描述"
              :rows="3"
            />
          </n-form-item>
        </n-form>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="showModal = false">
              取消
            </NButton>
            <NButton type="primary" :loading="submitLoading" @click="handleSubmit">
              确定
            </NButton>
          </NSpace>
        </template>
      </n-modal>
    </Teleport>

    <!-- 表单设计器弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showDesigner"
        preset="card"
        title="表单设计器"
        style="width: 95vw; height: 90vh"
        :mask-closable="false"
      >
        <div class="h-full -m-4">
          <FlowFormCreateDesigner
            ref="designerRef"
            v-model="currentFormSchema"
            @save="handleSaveSchema"
          />
        </div>
      </n-modal>
    </Teleport>

    <!-- 表单预览弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showPreview"
        preset="card"
        title="表单预览"
        style="width: 800px"
      >
        <FlowFormCreateRenderer
          v-if="currentFormSchema"
          :schema="currentFormSchema"
          read-only
        />
      </n-modal>
    </Teleport>
  </div>
</template>

<script setup>
import { NButton, NPopconfirm, NSpace, NSwitch, NTag } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import api from '@/api/flow'
import DictTag from '@/components/DictTag.vue'
import FlowFormCreateDesigner from '@/components/form-create/FlowFormCreateDesigner.vue'
import FlowFormCreateRenderer from '@/components/form-create/FlowFormCreateRenderer.vue'
import { useDict } from '@/composables/useDict'

// 使用全局 message 实例
const message = window.$message

const { dict } = useDict('sys_enable_disable', 'flow_form_type', 'sys_flow_data_mode')

const statusOptions = computed(() => toNumberOptions(dict.value.sys_enable_disable))
const formTypeOptions = computed(() => dict.value.flow_form_type || [])
const dataModeOptions = computed(() => dict.value.sys_flow_data_mode || [])

// 查询参数
const queryParams = reactive({
  formName: '',
  status: null,
})

// 表格数据
const dataSource = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page) => {
    pagination.page = page
    loadData()
  },
  onUpdatePageSize: (pageSize) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    loadData()
  },
})

// 弹窗相关
const showModal = ref(false)
const modalTitle = ref('新增表单')
const formRef = ref(null)
const submitLoading = ref(false)
const mutationLocks = reactive(new Set())
const formData = reactive({
  id: null,
  formName: '',
  formKey: '',
  formType: 'dynamic',
  formCategory: '',
  defaultDataMode: 'PROCESS_ONLY',
  description: '',
})

// 表单验证规则
const rules = {
  formName: [
    { required: true, message: '请输入表单名称', trigger: 'blur' },
  ],
  formKey: [
    { required: true, message: '请输入表单Key', trigger: 'blur' },
    { pattern: /^[a-z]\w*$/i, message: '表单Key必须以字母开头，只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  formType: [
    { required: true, message: '请选择表单类型', trigger: 'change' },
  ],
}

// 表单设计器相关
const showDesigner = ref(false)
const designerRef = ref(null)
const currentFormId = ref(null)
const currentFormSchema = ref([])

// 表单预览相关
const showPreview = ref(false)

// 表格列定义
const columns = [
  {
    title: '表单名称',
    key: 'formName',
    width: 180,
    ellipsis: {
      tooltip: true,
    },
  },
  {
    title: '表单Key',
    key: 'formKey',
    width: 150,
  },
  {
    title: '表单类型',
    key: 'formType',
    width: 100,
    render: row => h(DictTag, { dictType: 'flow_form_type', value: row.formType }),
  },
  {
    title: '默认数据模式',
    key: 'defaultDataMode',
    width: 140,
    render: row => h(DictTag, { dictType: 'sys_flow_data_mode', value: row.defaultDataMode || 'PROCESS_ONLY' }),
  },
  {
    title: '发布',
    key: 'publishStatus',
    width: 90,
    render: row => h(NTag, {
      type: row.publishStatus === 1 ? 'success' : 'default',
      size: 'small',
      bordered: false,
    }, { default: () => row.publishStatus === 1 ? '已发布' : '草稿' }),
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => {
      return h(NSwitch, {
        value: row.status === 1,
        disabled: isMutationLocked('status', row),
        onUpdateValue: value => handleStatusChange(row, value),
      })
    },
  },
  {
    title: '版本',
    key: 'version',
    width: 80,
  },
  {
    title: '描述',
    key: 'description',
    width: 200,
    ellipsis: {
      tooltip: true,
    },
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 160,
  },
  {
    title: '操作',
    key: 'actions',
    width: 340,
    fixed: 'right',
    render: (row) => {
      return h(NSpace, { size: 'small' }, {
        default: () => [
          h(NButton, {
            size: 'small',
            type: 'primary',
            text: true,
            onClick: () => handleDesign(row),
          }, { default: () => '设计' }),
          h(NButton, {
            size: 'small',
            type: 'info',
            text: true,
            onClick: () => handlePreview(row),
          }, { default: () => '预览' }),
          h(NButton, {
            size: 'small',
            type: 'success',
            text: true,
            disabled: !row.formSchema || isMutationLocked('publish', row),
            loading: isMutationLocked('publish', row),
            onClick: () => handlePublish(row),
          }, { default: () => '发布' }),
          h(NButton, {
            size: 'small',
            type: 'warning',
            text: true,
            onClick: () => handleEdit(row),
          }, { default: () => '编辑' }),
          h(NButton, {
            size: 'small',
            type: 'info',
            text: true,
            disabled: isMutationLocked('copy', row),
            loading: isMutationLocked('copy', row),
            onClick: () => handleCopy(row),
          }, { default: () => '复制' }),
          h(NPopconfirm, {
            onPositiveClick: () => handleDelete(row),
          }, {
            trigger: () => h(NButton, {
              size: 'small',
              type: 'error',
              text: true,
              disabled: isMutationLocked('delete', row),
              loading: isMutationLocked('delete', row),
            }, { default: () => '删除' }),
            default: () => '确定要删除此表单吗？',
          }),
        ],
      })
    },
  },
]

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

function mutationKey(action, row) {
  return `${action}:${row?.id || 'new'}`
}

function tryLockMutation(action, row) {
  const key = mutationKey(action, row)
  if (mutationLocks.has(key))
    return false
  mutationLocks.add(key)
  return true
}

function unlockMutation(action, row) {
  mutationLocks.delete(mutationKey(action, row))
}

function isMutationLocked(action, row) {
  return mutationLocks.has(mutationKey(action, row))
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await api.getFormPage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      ...queryParams,
    })
    if (res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
    }
  }
  catch (error) {
    message.error('加载数据失败')
    console.error(error)
  }
  finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  queryParams.formName = ''
  queryParams.status = null
  pagination.page = 1
  loadData()
}

// 新增
function handleAdd() {
  modalTitle.value = '新增表单'
  Object.assign(formData, {
    id: null,
    formName: '',
    formKey: '',
    formType: 'dynamic',
    formCategory: '',
    defaultDataMode: 'PROCESS_ONLY',
    description: '',
  })
  showModal.value = true
}

// 编辑
function handleEdit(row) {
  modalTitle.value = '编辑表单'
  Object.assign(formData, {
    id: row.id,
    formName: row.formName,
    formKey: row.formKey,
    formType: row.formType,
    formCategory: row.formCategory,
    defaultDataMode: row.defaultDataMode || 'PROCESS_ONLY',
    description: row.description,
  })
  showModal.value = true
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
  }
  catch {
    return
  }

  submitLoading.value = true
  try {
    if (formData.id) {
      await api.updateForm(formData)
      message.success('更新成功')
    }
    else {
      // 检查Key是否存在
      const checkRes = await api.checkFormKeyExists(formData.formKey)
      if (checkRes.data === true) {
        message.error('表单Key已存在，请更换')
        submitLoading.value = false
        return
      }
      await api.createForm(formData)
      message.success('创建成功')
    }
    showModal.value = false
    loadData()
  }
  catch (error) {
    message.error(formData.id ? '更新失败' : '创建失败')
    console.error(error)
  }
  finally {
    submitLoading.value = false
  }
}

// 设计表单
async function handleDesign(row) {
  currentFormId.value = row.id
  try {
    const res = await api.getFormById(row.id)
    if (res.data) {
      currentFormSchema.value = res.data.formSchema ? JSON.parse(res.data.formSchema) : []
      showDesigner.value = true
    }
  }
  catch (error) {
    message.error('加载表单失败')
    console.error(error)
  }
}

// 保存表单Schema
async function handleSaveSchema(schema) {
  const lockRow = { id: currentFormId.value }
  if (!tryLockMutation('schema', lockRow))
    return
  try {
    await api.updateForm({
      id: currentFormId.value,
      formSchema: JSON.stringify(schema),
    })
    message.success('保存成功')
    showDesigner.value = false
    loadData()
  }
  catch (error) {
    message.error('保存失败')
    console.error(error)
  }
  finally {
    unlockMutation('schema', lockRow)
  }
}

// 预览表单
async function handlePreview(row) {
  try {
    const res = await api.getFormById(row.id)
    if (res.data && res.data.formSchema) {
      currentFormSchema.value = JSON.parse(res.data.formSchema)
      showPreview.value = true
    }
    else {
      message.warning('表单尚未设计')
    }
  }
  catch (error) {
    message.error('加载表单失败')
    console.error(error)
  }
}

// 发布表单版本
async function handlePublish(row) {
  if (!tryLockMutation('publish', row))
    return
  try {
    await api.publishForm(row.id)
    message.success('表单版本已发布')
    loadData()
  }
  catch (error) {
    message.error('发布失败，请确认表单已完成设计')
    console.error(error)
  }
  finally {
    unlockMutation('publish', row)
  }
}

// 复制表单
async function handleCopy(row) {
  if (!tryLockMutation('copy', row))
    return
  try {
    await api.copyForm(row.id, `${row.formName}_副本`)
    message.success('复制成功')
    loadData()
  }
  catch (error) {
    message.error('复制失败')
    console.error(error)
  }
  finally {
    unlockMutation('copy', row)
  }
}

// 删除表单
async function handleDelete(row) {
  if (!tryLockMutation('delete', row))
    return
  try {
    await api.deleteForm(row.id)
    message.success('删除成功')
    loadData()
  }
  catch (error) {
    message.error('删除失败')
    console.error(error)
  }
  finally {
    unlockMutation('delete', row)
  }
}

// 状态切换
async function handleStatusChange(row, value) {
  if (!tryLockMutation('status', row))
    return
  try {
    if (value) {
      await api.enableForm(row.id)
    }
    else {
      await api.disableForm(row.id)
    }
    message.success('状态更新成功')
    loadData()
  }
  catch (error) {
    message.error('状态更新失败')
    console.error(error)
  }
  finally {
    unlockMutation('status', row)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.h-full {
  height: 100%;
}

.flex-1 {
  flex: 1;
}

.overflow-hidden {
  overflow: hidden;
}
</style>
