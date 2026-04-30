<template>
  <div class="system-menu-page">
    <!-- 客户端切换 Tab -->
    <div class="client-tabs-container">
      <n-tabs type="line" size="small" :value="currentClientCode" @update:value="handleClientTabChange">
        <n-tab-pane name="" tab="全部">
          <template #tab>
            <span class="tab-label">全部</span>
          </template>
        </n-tab-pane>
        <n-tab-pane v-for="client in clientList" :key="client.clientCode" :name="client.clientCode">
          <template #tab>
            <span class="tab-label">{{ client.clientName }}</span>
          </template>
        </n-tab-pane>
      </n-tabs>
    </div>

    <AiCrudPage
      ref="crudRef"
      api="/system/resource"
      :api-config="{
        list: 'get@/system/resource/tree',
        detail: 'post@/system/resource/getById',
        add: 'post@/system/resource/add',
        update: 'post@/system/resource/edit',
        delete: 'post@/system/resource/remove',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-render-list="beforeRenderList"
      :before-submit="beforeSubmit"
      :before-render-form="beforeRenderForm"
      :public-params="publicParams"
      row-key="id"
      :edit-grid-cols="1"
      modal-width="800px"
      :show-pagination="false"
      :lazy="false"
      add-button-text="新增资源"
      :table-props="{
        indent: 24,
        expandOnClick: true,
        expandedRowKeys: expandedKeys,
        onUpdateExpandedRowKeys: handleExpandedKeysUpdate,
        rowProps,
      }"
      @submit-success="handleSubmitSuccess"
      @add="handleToolbarAdd"
    >
      <!-- 自定义工具栏 -->
      <template #toolbar-end>
        <n-button size="small" @click="toggleExpandAll">
          <template #icon>
            <i :class="expandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
          </template>
          {{ expandAll ? '折叠全部' : '展开全部' }}
        </n-button>
      </template>

      <!-- 自定义图标列 -->
      <template #table-icon="{ row }">
        <div class="inline-edit-cell" @click.stop>
          <div class="inline-edit-preview" @click="row._editingIcon = true">
            <IconRenderer v-if="row.icon" :icon="row.icon" :font-size="16" />
            <span v-else class="text-xs text-gray-400">选择</span>
          </div>
          <div v-if="row._editingIcon" class="inline-edit-editor" @click.stop>
            <IconSelector
              :model-value="row.icon"
              @update:model-value="(value) => { handleInlineUpdate(row, 'icon', value); row._editingIcon = false }"
            />
          </div>
        </div>
      </template>

      <!-- 自定义排序列 -->
      <template #table-sort="{ row }">
        <div class="inline-edit-cell" @click.stop>
          <div class="inline-edit-preview" @click="row._editingSort = true">
            <span class="sort-value">{{ row.sort }}</span>
          </div>
          <NInputNumber
            v-if="row._editingSort"
            :value="row.sort"
            :min="0"
            :show-button="false"
            size="small"
            style="width: 80px"
            @update:value="(value) => { handleInlineUpdate(row, 'sort', value); row._editingSort = false }"
            @blur="row._editingSort = false"
          />
        </div>
      </template>

      <!-- 自定义表单 - 图标选择 -->
      <template #form-icon="{ value, updateValue }">
        <n-tabs type="line" size="small" animated>
          <n-tab-pane name="font" tab="字体图标">
            <div class="icon-selector-container">
              <IconSelector :model-value="value" @update:model-value="updateValue" />
              <n-input
                :value="value"
                placeholder="或手动输入图标名称（如: i-mdi-home）"
                clearable
                @update:value="updateValue"
              >
                <template #prefix>
                  <IconRenderer v-if="value" :icon="value" />
                </template>
              </n-input>
            </div>
          </n-tab-pane>
          <n-tab-pane name="image" tab="图片图标">
            <div class="icon-upload-container">
              <ImageUpload
                :model-value="value"
                :limit="1"
                :file-size="2"
                :file-type="['png', 'jpg', 'jpeg', 'webp', 'gif', 'svg']"
                business-type="menu-icon"
                :show-tip="true"
                value-type="string"
                @update:model-value="updateValue"
              />
            </div>
          </n-tab-pane>
        </n-tabs>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { NInputNumber } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import api from '@/api'
import { AiCrudPage } from '@/components/ai-form'
import IconRenderer from '@/components/IconRenderer.vue'
import IconSelector from '@/components/IconSelector.vue'
import ImageUpload from '@/components/image-upload/index.vue'
import { usePermissionStore } from '@/store'
import { request } from '@/utils'

defineOptions({ name: 'SystemMenu' })

const permissionStore = usePermissionStore()

// 客户端列表（从后端动态加载）
const clientList = ref([])
const currentClientCode = ref('')

// 公共搜索参数（用于 Tab 切换筛选）
const publicParams = computed(() => {
  if (currentClientCode.value) {
    return { clientCode: currentClientCode.value }
  }
  return {}
})

// 组件挂载时加载上级资源选项和客户端列表
onMounted(() => {
  loadParentResourceOptions()
  loadClientList()
})

const crudRef = ref(null)
const expandAll = ref(true)
const expandedKeys = ref([])
const parentResourceOptions = ref([{ label: '顶级资源', value: 0, key: 0 }])
const pendingParentId = ref(null)
const pendingClientCode = ref(null)

// 资源类型选项
const resourceTypeOptions = [
  { label: '目录', value: 1 },
  { label: '菜单', value: 2 },
  { label: '按钮', value: 3 },
  { label: 'API接口', value: 4 },
]

// 客户端选项（动态从后端加载）
const clientCodeOptions = computed(() => {
  return clientList.value.map(client => ({
    label: client.clientName,
    value: client.clientCode,
  }))
})

// 加载上级资源选项
async function loadParentResourceOptions() {
  try {
    const res = await request.get('/system/resource/tree')
    if (res.code === 200) {
      const convertToTreeSelect = (list) => {
        return list.map(item => ({
          label: item.resourceName,
          value: item.id,
          key: item.id,
          children: item.children && item.children.length > 0
            ? convertToTreeSelect(item.children)
            : undefined,
        }))
      }
      parentResourceOptions.value = [
        { label: '顶级资源', value: 0, key: 0 },
        ...convertToTreeSelect(res.data || []),
      ]
    }
  }
  catch (error) {
    console.error('加载上级资源选项失败:', error)
  }
}

// 加载客户端列表
async function loadClientList() {
  try {
    const res = await request.get('/system/client/list')
    if (res.code === 200) {
      clientList.value = res.data || []
    }
  }
  catch (error) {
    console.error('加载客户端列表失败:', error)
  }
}

// 客户端 Tab 切换
function handleClientTabChange(clientCode) {
  currentClientCode.value = clientCode
  // Tab 切换时清空 pendingClientCode，避免残留值影响新增表单
  pendingClientCode.value = null
}

// 显示状态选项
const visibleOptions = [
  { label: '显示', value: 1 },
  { label: '隐藏', value: 0 },
]

// API请求方法选项
const apiMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    props: {
      placeholder: '请输入资源名称',
    },
  },
  {
    field: 'resourceType',
    label: '资源类型',
    type: 'select',
    props: {
      placeholder: '请选择资源类型',
      options: resourceTypeOptions,
    },
  },
  {
    field: 'visible',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: visibleOptions,
    },
  },
]

// 资源类型样式配置
const typeStyleMap = {
  1: { text: '目录', icon: 'i-material-symbols:folder-outline', color: '#4C6EF5', bg: '#EDF2FF', fontWeight: '600' },
  2: { text: '菜单', icon: 'i-material-symbols:menu', color: '#40C057', bg: '#EBFBEE', fontWeight: '500' },
  3: { text: '按钮', icon: 'i-material-symbols:smart-button-outline', color: '#FD7E14', bg: '#FFF4E6', fontWeight: '400' },
  4: { text: 'API', icon: 'i-material-symbols:api', color: '#FA5252', bg: '#FFF5F5', fontWeight: '400' },
}

// 客户端样式配置
const clientStyleMap = {
  pc: { text: 'PC端', type: 'info' },
  app: { text: 'APP', type: 'success' },
  h5: { text: 'H5', type: 'warning' },
}

// 行样式 — 根据资源类型添加 class
function rowProps(row) {
  return {
    class: `resource-type-${row.resourceType}`,
  }
}

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'resourceName',
    label: '资源名称',
    width: 180,
    fixed: 'left',
    render: (row) => {
      const style = typeStyleMap[row.resourceType] || { fontWeight: '400' }
      return h('div', { class: 'flex items-center', style: { fontWeight: style.fontWeight } }, [
        row.icon
          ? h(IconRenderer, {
              icon: row.icon,
              fontSize: '16',
              customStyle: row.resourceType <= 2 ? { color: 'var(--primary-color, #4C6EF5)' } : { color: '#999' },
            })
          : null,
        h('span', { style: { marginLeft: row.icon ? '8px' : '0' } }, row.resourceName),
      ])
    },
  },
  {
    prop: 'clientCode',
    label: '客户端',
    width: 100,
    render: (row) => {
      const config = clientStyleMap[row.clientCode] || { text: row.clientCode, type: 'default' }
      return h(
        'n-tag',
        { type: config.type, size: 'small', bordered: false },
        { default: () => config.text },
      )
    },
  },
  {
    prop: 'resourceType',
    label: '类型',
    width: 90,
    render: (row) => {
      const config = typeStyleMap[row.resourceType]
      if (!config)
        return h('span', '未知')
      return h('span', {
        class: 'resource-type-badge',
        style: {
          color: config.color,
          backgroundColor: config.bg,
          borderColor: config.color,
        },
      }, [
        h('i', { class: config.icon, style: { fontSize: '12px', marginRight: '4px' } }),
        config.text,
      ])
    },
  },
  {
    prop: 'icon',
    label: '图标',
    width: 120,
    _slot: 'icon',
  },
  {
    prop: 'path',
    label: '路由地址',
    width: 180,
  },
  {
    prop: 'sort',
    label: '排序',
    width: 90,
    _slot: 'sort',
  },
  {
    prop: 'visible',
    label: '状态',
    width: 80,
    render: (row) => {
      const isVisible = row.visible === 1
      return h('span', {
        class: ['visibility-toggle', isVisible ? 'is-visible' : 'is-hidden'],
        onClick: (e) => {
          e.stopPropagation()
          handleInlineUpdate(row, 'visible', isVisible ? 0 : 1)
        },
      }, [
        h('i', {
          class: isVisible ? 'i-material-symbols:visibility' : 'i-material-symbols:visibility-off',
          style: { fontSize: '14px', marginRight: '2px' },
        }),
        isVisible ? '显示' : '隐藏',
      ])
    },
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '新增子项', key: 'add', type: 'primary', onClick: handleAdd },
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置 - 优化布局和验证规则
const editSchema = computed(() => [
  // 基础信息分组
  {
    type: 'divider',
    label: '基础信息',
    props: { titlePlacement: 'left' },
    span: 1,
  },
  {
    field: 'parentId',
    label: '上级资源',
    type: 'treeSelect',
    defaultValue: 0,
    props: {
      placeholder: '请选择上级资源',
      clearable: true,
      filterable: true,
      defaultExpandAll: false,
      keyField: 'value',
      labelField: 'label',
      childrenField: 'children',
    },
    options: () => parentResourceOptions.value,
  },
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    rules: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
    props: { placeholder: '请输入资源名称' },
  },
  {
    field: 'resourceType',
    label: '资源类型',
    type: 'radio',
    defaultValue: 1,
    rules: [
      {
        required: true,
        message: '请选择资源类型',
        trigger: 'change',
        validator: (rule, value) => {
          if (value === null || value === undefined || value === '') {
            return new Error('请选择资源类型')
          }
          return true
        },
      },
    ],
    props: {
      options: [
        { label: '目录', value: 1 },
        { label: '菜单', value: 2 },
        { label: '按钮', value: 3 },
        { label: 'API', value: 4 },
      ],
    },
  },
  {
    field: 'clientCode',
    label: '客户端',
    type: 'radio',
    defaultValue: 'pc',
    rules: [
      {
        required: true,
        message: '请选择客户端',
        trigger: 'change',
        validator: (rule, value) => {
          if (!value) {
            return new Error('请选择客户端')
          }
          return true
        },
      },
    ],
    props: { options: clientCodeOptions.value },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'input-number',
    defaultValue: 0,
    props: { placeholder: '排序值', min: 0 },
  },

  // 目录和菜单配置
  {
    type: 'divider',
    label: '目录/菜单配置',
    props: { titlePlacement: 'left' },
    span: 1,
    vIf: formData => formData.resourceType == 1 || formData.resourceType == 2,
  },
  {
    field: 'icon',
    label: '图标',
    type: 'slot',
    slotName: 'icon',
    vIf: formData => formData.resourceType == 1 || formData.resourceType == 2,
  },
  {
    field: 'path',
    label: '路由地址',
    type: 'input',
    props: { placeholder: '/system/user' },
    vIf: formData => formData.resourceType == 1 || formData.resourceType == 2,
  },
  {
    field: 'component',
    label: '组件路径',
    type: 'input',
    props: { placeholder: 'system/user/index' },
    vIf: formData => formData.resourceType == 2,
  },
  {
    field: 'redirect',
    label: '重定向地址',
    type: 'input',
    props: { placeholder: '重定向地址' },
    vIf: formData => formData.resourceType == 1 || formData.resourceType == 2,
  },
  {
    field: 'isExternal',
    label: '是否外链',
    type: 'radio',
    defaultValue: 0,
    props: { options: [{ label: '否', value: 0 }, { label: '是', value: 1 }] },
    vIf: formData => formData.resourceType == 2,
  },
  {
    field: 'keepAlive',
    label: '是否缓存',
    type: 'radio',
    defaultValue: 0,
    props: { options: [{ label: '否', value: 0 }, { label: '是', value: 1 }] },
    vIf: formData => formData.resourceType == 2,
  },
  {
    field: 'alwaysShow',
    label: '总是显示',
    type: 'radio',
    defaultValue: 0,
    props: { options: [{ label: '否', value: 0 }, { label: '是', value: 1 }] },
    vIf: formData => formData.resourceType == 1 || formData.resourceType == 2,
  },

  // 按钮和API配置
  {
    type: 'divider',
    label: '按钮/API配置',
    props: { titlePlacement: 'left' },
    span: 1,
    vIf: formData => formData.resourceType == 3 || formData.resourceType == 4,
  },
  {
    field: 'perms',
    label: '权限标识',
    type: 'input',
    props: { placeholder: 'sys:user:add' },
    vIf: formData => formData.resourceType == 3 || formData.resourceType == 4,
  },
  {
    field: 'apiMethod',
    label: '请求方法',
    type: 'select',
    defaultValue: 'GET',
    props: { placeholder: '请求方法', options: apiMethodOptions },
    vIf: formData => formData.resourceType == 4,
  },
  {
    field: 'apiUrl',
    label: '接口地址',
    type: 'input',
    props: { placeholder: '/system/user/list' },
    vIf: formData => formData.resourceType == 4,
  },

  // 状态配置
  {
    type: 'divider',
    label: '状态配置',
    props: { titlePlacement: 'left' },
    span: 1,
  },
  {
    field: 'visible',
    label: '显示状态',
    type: 'radio',
    defaultValue: 1,
    props: { options: [{ label: '显示', value: 1 }, { label: '隐藏', value: 0 }] },
  },
  {
    field: 'menuStatus',
    label: '菜单状态',
    type: 'radio',
    defaultValue: 1,
    props: { options: [{ label: '显示', value: 1 }, { label: '隐藏', value: 0 }] },
    vIf: formData => formData.resourceType == 1 || formData.resourceType == 2,
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    props: { placeholder: '请输入备注', rows: 3 },
  },
])

// 获取所有节点的 key（用于展开/收起）
function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

// 列表数据渲染前处理
function beforeRenderList(list) {
  if (expandAll.value) {
    expandedKeys.value = getAllKeys(list)
  }
  return list
}

// 表单渲染前处理
function beforeRenderForm(data) {
  if (!data) {
    // 新增时设置默认值
    const parentId = pendingParentId.value !== null ? pendingParentId.value : 0
    // 优先使用 pendingClientCode（新增子项时带入父级），其次使用当前 Tab 的 clientCode
    const clientCode = pendingClientCode.value || currentClientCode.value || 'pc'
    // 清空临时值
    pendingParentId.value = null
    pendingClientCode.value = null
    return { parentId, clientCode }
  }
  // 编辑时清空临时值
  pendingParentId.value = null
  pendingClientCode.value = null
  return data
}

// 表单提交前处理
function beforeSubmit(formData) {
  return {
    ...formData,
    resourceType: formData.resourceType !== undefined ? Number(formData.resourceType) : formData.resourceType,
  }
}

// 提交成功后
async function handleSubmitSuccess() {
  await refreshSystemMenu()
  loadParentResourceOptions()
}

// 刷新系统菜单
async function refreshSystemMenu() {
  try {
    const res = await api.getMenu()
    if (res.code === 200 && res.data) {
      permissionStore.setMenuData(res.data)
    }
  }
  catch (error) {
    console.error('刷新系统菜单失败:', error)
  }
}

// 内联更新
async function handleInlineUpdate(row, field, value) {
  try {
    row[field] = value
    const res = await request.post('/system/resource/edit', {
      id: row.id,
      [field]: value,
    })
    if (res.code === 200) {
      window.$message.success('更新成功')
      await refreshSystemMenu()
      loadParentResourceOptions()
    }
    else {
      window.$message.error(res.msg || '更新失败')
    }
  }
  catch (error) {
    console.error('内联更新失败:', error)
    window.$message.error('更新失败')
  }
}

// 展开/折叠所有
function toggleExpandAll() {
  expandAll.value = !expandAll.value
  if (expandAll.value) {
    const tableData = crudRef.value?.getTableData() || []
    expandedKeys.value = getAllKeys(tableData)
  }
  else {
    expandedKeys.value = []
  }
}

// 处理表格展开状态更新
function handleExpandedKeysUpdate(keys) {
  expandedKeys.value = keys
  const tableData = crudRef.value?.getTableData() || []
  const allKeys = getAllKeys(tableData)
  expandAll.value = keys.length === allKeys.length
}

// 工具栏新增按钮点击（AiCrudPage 内部按钮触发）
function handleToolbarAdd() {
  // 设置当前 Tab 的 clientCode，beforeRenderForm 会使用
  pendingClientCode.value = currentClientCode.value || null
}

// 新增子资源（操作列按钮触发）
async function handleAdd(row) {
  await loadParentResourceOptions()
  if (row) {
    // 新增子项：带入父级的 clientCode
    pendingParentId.value = row.id
    pendingClientCode.value = row.clientCode
  }
  else {
    // 顶级新增：带入当前 Tab 的 clientCode
    pendingParentId.value = null
    pendingClientCode.value = currentClientCode.value || null
  }
  crudRef.value?.showAdd()
}

// 编辑
async function handleEdit(row) {
  await loadParentResourceOptions()
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除该资源吗？删除后将无法恢复！',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/resource/remove', null, { params: { id: row.id } })
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
          await refreshSystemMenu()
          loadParentResourceOptions()
        }
      }
      catch (error) {
        window.$message.error('删除失败')
      }
    },
  })
}
</script>

<style scoped>
.system-menu-page {
  height: 100%;
}

/* 行内编辑 */
.inline-edit-cell {
  display: flex;
  align-items: center;
}

.inline-edit-preview {
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background 0.2s;
  min-height: 24px;
  display: flex;
  align-items: center;
}

.inline-edit-preview:hover {
  background: var(--n-color-hover, #f5f5f5);
}

.sort-value {
  font-size: 13px;
  min-width: 24px;
  text-align: center;
}

/* 资源类型标签 */
.resource-type-badge {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  line-height: 1;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid;
  white-space: nowrap;
}

/* 可见状态切换 */
.visibility-toggle {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
  user-select: none;
}

.visibility-toggle.is-visible {
  color: #40c057;
}

.visibility-toggle.is-visible:hover {
  background: #ebfbee;
}

.visibility-toggle.is-hidden {
  color: #868e96;
}

.visibility-toggle.is-hidden:hover {
  background: #f8f9fa;
}

/* 行背景区分类型 */
:deep(.resource-type-1) {
  background-color: #f8f9ff !important;
}

:deep(.resource-type-1:hover) {
  background-color: #edf2ff !important;
}

:deep(.resource-type-2) {
  background-color: #f8fff8 !important;
}

:deep(.resource-type-2:hover) {
  background-color: #ebfbee !important;
}

.icon-selector-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.icon-upload-container {
  padding: 8px 0;
}

.client-tabs-container {
  background: #fff;
  padding: 8px 16px 0;
  border-radius: 4px;
  margin-bottom: 8px;
}

.tab-label {
  font-size: 14px;
}
</style>
