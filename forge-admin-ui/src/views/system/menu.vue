<template>
  <div class="system-menu-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/resource"
      :api-config="{
        list: 'get@/system/resource/tree',
        detail: 'post@/system/resource/getById',
        add: 'post@/system/resource/add',
        update: 'post@/system/resource/edit',
        delete: 'post@/system/resource/remove'
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-render-list="beforeRenderList"
      :before-submit="beforeSubmit"
      :before-render-form="beforeRenderForm"
      row-key="id"
      :edit-grid-cols="2"
      :modal-width="'900px'"
      :show-pagination="false"
      :lazy="false"
      add-button-text="新增资源"
      :table-props="{
        expandedRowKeys: expandedKeys,
        onUpdateExpandedRowKeys: handleExpandedKeysUpdate
      }"
      @submit-success="handleSubmitSuccess"
    >
      <!-- 自定义工具栏 -->
      <template #toolbar-end>
        <n-button @click="toggleExpandAll">
          <template #icon>
            <i class="i-material-symbols:unfold-more" />
          </template>
          {{ expandAll ? '折叠全部' : '展开全部' }}
        </n-button>
      </template>

      <!-- 自定义图标列 -->
      <template #table-icon="{ row }">
        <div class="inline-edit-cell">
          <IconSelector
            :model-value="row.icon"
            @update:model-value="(value) => handleInlineUpdate(row, 'icon', value)"
          />
        </div>
      </template>

      <!-- 自定义排序列 -->
      <template #table-sort="{ row }">
        <div class="inline-edit-cell">
          <n-input-number
            :value="row.sort"
            @update:value="(value) => handleInlineUpdate(row, 'sort', value)"
            :min="0"
            :show-button="false"
            size="small"
            style="width: 80px"
          />
        </div>
      </template>

      <!-- 自定义操作列 -->
      <template #table-action="{ row }">
        <div class="flex items-center gap-8">
          <a
            class="text-primary cursor-pointer hover:text-primary-hover"
            @click="handleAdd(row)"
          >
            新增
          </a>
          <span class="text-gray-300">|</span>
          <a
            class="text-primary cursor-pointer hover:text-primary-hover"
            @click="handleEdit(row)"
          >
            编辑
          </a>
          <span class="text-gray-300">|</span>
          <a
            class="text-error cursor-pointer hover:text-error-hover"
            @click="handleDelete(row)"
          >
            删除
          </a>
        </div>
      </template>

      <!-- 自定义表单 - 图标选择 -->
      <template #form-icon="{ value, updateValue }">
        <n-space align="center" style="width: 100%">
          <IconSelector :model-value="value" @update:model-value="updateValue" />
          <n-input
            :value="value"
            @update:value="updateValue"
            placeholder="或手动输入图标名称"
            style="flex: 1"
          >
            <template #suffix>
              <IconRenderer v-if="value" :icon="value" />
            </template>
          </n-input>
        </n-space>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NTag, NInputNumber } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import IconRenderer from '@/components/IconRenderer.vue'
import IconSelector from '@/components/IconSelector.vue'
import { request } from '@/utils'
import { usePermissionStore } from '@/store'
import api from '@/api'

defineOptions({ name: 'SystemMenu' })

const permissionStore = usePermissionStore()

// 组件挂载时加载上级资源选项
onMounted(() => {
  loadParentResourceOptions()
})

const crudRef = ref(null)
const expandAll = ref(true)
const expandedKeys = ref([])
const parentResourceOptions = ref([{ label: '顶级资源', value: 0, key: 0 }])
const pendingParentId = ref(null) // 用于存储待设置的父级ID

// 资源类型选项
const resourceTypeOptions = [
  { label: '目录', value: 1 },
  { label: '菜单', value: 2 },
  { label: '按钮', value: 3 },
  { label: 'API接口', value: 4 }
]

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
            : undefined
        }))
      }
      parentResourceOptions.value = [
        { label: '顶级资源', value: 0, key: 0 },
        ...convertToTreeSelect(res.data || [])
      ]
    }
  } catch (error) {
    console.error('加载上级资源选项失败:', error)
  }
}

// 显示状态选项
const visibleOptions = [
  { label: '显示', value: 1 },
  { label: '隐藏', value: 0 }
]

// API请求方法选项
const apiMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' }
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    props: {
      placeholder: '请输入资源名称'
    }
  },
  {
    field: 'resourceType',
    label: '资源类型',
    type: 'select',
    props: {
      placeholder: '请选择资源类型',
      options: resourceTypeOptions
    }
  },
  {
    field: 'visible',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: visibleOptions
    }
  }
]

// 表格列配置
const tableColumns = [
  {
    prop: 'resourceName',
    label: '资源名称',
    width: 220,
    fixed: 'left',
    render: (row) => {
      return h('div', { class: 'flex items-center' }, [
        row.icon ? h(IconRenderer, { icon: row.icon, class: 'mr-8' }) : null,
        h('span', row.resourceName)
      ])
    }
  },
  {
    prop: 'resourceType',
    label: '资源类型',
    width: 100,
    render: (row) => {
      const typeMap = {
        1: { text: '目录', type: 'info' },
        2: { text: '菜单', type: 'success' },
        3: { text: '按钮', type: 'warning' },
        4: { text: 'API', type: 'error' }
      }
      const config = typeMap[row.resourceType] || { text: '未知', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    }
  },
  {
    prop: 'icon',
    label: '图标',
    width: 150,
    _slot: 'icon'
  },
  {
    prop: 'path',
    label: '路由地址',
    width: 200
  },
  {
    prop: 'component',
    label: '组件路径',
    width: 200
  },
  {
    prop: 'perms',
    label: '权限标识',
    width: 180
  },
  {
    prop: 'sort',
    label: '排序',
    width: 100,
    _slot: 'sort'
  },
  {
    prop: 'visible',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag,
        { type: row.visible === 1 ? 'success' : 'error', size: 'small' },
        { default: () => row.visible === 1 ? '显示' : '隐藏' }
      )
    }
  },
  {
    prop: 'action',
    label: '操作',
    width: 180,
    fixed: 'right',
    _slot: 'action'
  }
]

// 编辑表单配置
const editSchema = ref([
  // 基础信息
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left'
    },
    span: 2
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
      defaultExpandAll: false,  // 默认收起
      keyField: 'value',
      labelField: 'label',
      childrenField: 'children'
    },
    options: () => {
      console.log('获取上级资源选项:', parentResourceOptions.value)
      return parentResourceOptions.value
    }
  },
  {
    field: 'resourceType',
    label: '资源类型',
    type: 'radio',
    defaultValue: 1,
    rules: [{ required: true, type: 'number', message: '请选择资源类型', trigger: 'change' }],
    props: {
      options: [
        { label: '目录', value: 1 },
        { label: '菜单', value: 2 },
        { label: '按钮', value: 3 },
        { label: 'API', value: 4 }
      ]
    }
  },
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    rules: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入资源名称'
    }
  },
  {
    field: 'sort',
    label: '排序',
    type: 'input-number',
    defaultValue: 0,
    props: {
      placeholder: '排序值',
      min: 0
    }
  },

  // 目录和菜单配置
  {
    type: 'divider',
    label: '目录/菜单配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2,
    vIf: (formData) => formData.resourceType === 1 || formData.resourceType === 2
  },
  {
    field: 'icon',
    label: '图标',
    type: 'slot',
    slotName: 'icon',
    vIf: (formData) => formData.resourceType === 1 || formData.resourceType === 2
  },
  {
    field: 'path',
    label: '路由地址',
    type: 'input',
    rules: [
      {
        required: true,
        message: '请输入路由地址',
        trigger: 'blur',
        validator: (rule, value) => {
          // 只有在显示该字段时才验证
          const formData = rule.formData || {}
          if (formData.resourceType === 1 || formData.resourceType === 2) {
            return !!value
          }
          return true
        }
      }
    ],
    props: {
      placeholder: '/system/user'
    },
    vIf: (formData) => formData.resourceType === 1 || formData.resourceType === 2
  },
  {
    field: 'component',
    label: '组件路径',
    type: 'input',
    span: 2,
    rules: [
      {
        required: true,
        message: '请输入组件路径',
        trigger: 'blur',
        validator: (rule, value) => {
          // 只有在显示该字段时才验证（菜单类型）
          const formData = rule.formData || {}
          if (formData.resourceType === 2) {
            return !!value
          }
          return true
        }
      }
    ],
    props: {
      placeholder: 'system/user/index'
    },
    vIf: (formData) => formData.resourceType === 2
  },
  {
    field: 'redirect',
    label: '重定向',
    type: 'input',
    props: {
      placeholder: '重定向地址'
    },
    vIf: (formData) => formData.resourceType === 2
  },
  {
    field: 'isExternal',
    label: '是否外链',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: [
        { label: '否', value: 0 },
        { label: '是', value: 1 }
      ]
    },
    vIf: (formData) => formData.resourceType === 2
  },
  {
    field: 'keepAlive',
    label: '是否缓存',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: [
        { label: '否', value: 0 },
        { label: '是', value: 1 }
      ]
    },
    vIf: (formData) => formData.resourceType === 2
  },
  {
    field: 'alwaysShow',
    label: '总是显示',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: [
        { label: '否', value: 0 },
        { label: '是', value: 1 }
      ]
    },
    vIf: (formData) => formData.resourceType === 2
  },

  // 按钮和API配置
  {
    type: 'divider',
    label: '按钮/API配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2,
    vIf: (formData) => formData.resourceType === 3 || formData.resourceType === 4
  },
  {
    field: 'perms',
    label: '权限标识',
    type: 'input',
    span: 2,
    props: {
      placeholder: 'sys:user:add'
    },
    vIf: (formData) => formData.resourceType === 3 || formData.resourceType === 4
  },
  {
    field: 'apiMethod',
    label: '请求方法',
    type: 'select',
    defaultValue: 'GET',
    props: {
      placeholder: '请求方法',
      options: apiMethodOptions
    },
    vIf: (formData) => formData.resourceType === 4
  },
  {
    field: 'apiUrl',
    label: '接口地址',
    type: 'input',
    span: 2,
    props: {
      placeholder: '/system/user/list'
    },
    vIf: (formData) => formData.resourceType === 4
  },

  // 状态配置
  {
    type: 'divider',
    label: '状态配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'visible',
    label: '显示状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: [
        { label: '显示', value: 1 },
        { label: '隐藏', value: 0 }
      ]
    }
  },
  {
    field: 'menuStatus',
    label: '菜单状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: [
        { label: '显示', value: 1 },
        { label: '隐藏', value: 0 }
      ]
    },
    vIf: (formData) => formData.resourceType === 1 || formData.resourceType === 2
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注',
      rows: 3
    }
  }
])

// 获取所有节点的 key（用于展开/收起）
function getAllKeys(list, keys = []) {
  list.forEach(item => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

// 列表数据渲染前处理（后端已返回树形结构）
function beforeRenderList(list) {
  console.log('加载的树形数据:', list)

  // 如果默认展开，收集所有节点的 key
  if (expandAll.value) {
    expandedKeys.value = getAllKeys(list)
  }

  return list
}

// 表单渲染前处理
function beforeRenderForm(data) {
  console.log('beforeRenderForm 被调用:', { data, pendingParentId: pendingParentId.value })
  
  // 如果是新增（data 为 null 或 undefined）
  if (!data) {
    const parentId = pendingParentId.value !== null ? pendingParentId.value : 0
    pendingParentId.value = null // 清空待设置的父级ID
    console.log('返回新增的默认值:', { parentId })
    return {
      parentId: parentId
    }
  }
  
  // 编辑模式
  pendingParentId.value = null // 清空 pendingParentId 以防影响下次操作
  console.log('返回编辑数据:', data)
  return data
}

// 表单提交前处理
function beforeSubmit(formData) {
  console.log('提交的表单数据:', formData)
  return formData
}

// 处理表格展开状态更新（用户手动点击展开/收起时）
function handleExpandedKeysUpdate(keys) {
  expandedKeys.value = keys

  // 根据当前展开的 key 数量判断是否全部展开
  const tableData = crudRef.value?.getTableData() || []
  const allKeys = getAllKeys(tableData)
  expandAll.value = keys.length === allKeys.length
}

// 展开/折叠所有
function toggleExpandAll() {
  expandAll.value = !expandAll.value

  if (expandAll.value) {
    // 展开所有：获取当前表格数据的所有 key
    const tableData = crudRef.value?.getTableData() || []
    expandedKeys.value = getAllKeys(tableData)
  } else {
    // 收起所有：清空展开的 key
    expandedKeys.value = []
  }
}

// 新增子资源
async function handleAdd(row) {
  // 加载上级资源选项
  await loadParentResourceOptions()

  // 如果点击的是某一行的新增按钮，设置待设置的父级ID
  if (row) {
    console.log('点击行新增，设置 pendingParentId:', row.id, '行数据:', row)
    pendingParentId.value = row.id
  } else {
    console.log('点击工具栏新增，清空 pendingParentId')
    pendingParentId.value = null
  }

  crudRef.value?.showAdd()
}

// 编辑
async function handleEdit(row) {
  // 加载上级资源选项
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
          // 刷新系统菜单
          await refreshSystemMenu()
        }
      } catch (error) {
        window.$message.error('删除失败')
      }
    }
  })
}

// 提交成功后的处理（新增或编辑成功）
async function handleSubmitSuccess() {
  // 刷新系统菜单
  await refreshSystemMenu()
}

// 刷新系统菜单
async function refreshSystemMenu() {
  try {
    console.log('开始刷新系统菜单...')
    const res = await api.getMenu()
    if (res.code === 200 && res.data) {
      permissionStore.setMenuData(res.data)
      console.log('系统菜单刷新成功')
      window.$message.success('菜单已更新')
    }
  } catch (error) {
    console.error('刷新系统菜单失败:', error)
  }
}

// 内联编辑处理
async function handleInlineUpdate(row, field, value) {
  try {
    // 更新本地数据
    row[field] = value

    // 调用更新接口
    const res = await request.post('/system/resource/edit', {
      id: row.id,
      [field]: value
    })

    if (res.code === 200) {
      window.$message.success('更新成功')
      // 刷新列表
      crudRef.value?.refresh()
      // 刷新系统菜单
      await refreshSystemMenu()
    } else {
      window.$message.error(res.msg || '更新失败')
    }
  } catch (error) {
    console.error('内联更新失败:', error)
    window.$message.error('更新失败')
  }
}
</script>

<style scoped>
.system-menu-page {
}

.inline-edit-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
