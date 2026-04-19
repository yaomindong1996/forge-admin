<template>
  <div class="menu-management">
    <NSpace vertical>
      <NSpace>
        <NButton type="primary" class="add-menu-btn" @click="handleAdd">
          新增菜单
        </NButton>
        <NButton
          type="error"
          :disabled="checkedRowKeys.length === 0"
          @click="handleBatchDelete"
        >
          批量删除  {{ checkedRowKeys.length ? `(${checkedRowKeys.length})` : '' }}
        </NButton>
      </NSpace>

      <n-data-table
        v-model:checked-row-keys="checkedRowKeys"
        size="small"
        :columns="columns"
        :data="menuData"
        :loading="loading"
        :pagination="pagination"
        :remote="true"
        :row-key="(rowData) => rowData.id"
        @update:page="handlePageChange"
      />
    </NSpace>

    <!-- 菜单编辑抽屉 -->
    <n-drawer v-model:show="showDrawer" :width="500" placement="right">
      <n-drawer-content :title="drawerTitle" closable>
        <n-form
          ref="formRef"
          :model="formValue"
          :rules="formRules"
          label-placement="left"
          label-width="100"
        >
          <n-form-item label="菜单名称" path="title">
            <n-input v-model:value="formValue.title" placeholder="请输入菜单名称" />
          </n-form-item>

          <n-form-item label="菜单路径" path="url">
            <n-input v-model:value="formValue.url" placeholder="请输入菜单路径" />
          </n-form-item>

          <n-form-item label="菜单图标" path="icon">
            <IconSelector v-model="formValue.icon" />
          </n-form-item>

          <n-form-item label="排序" path="showOrder">
            <NInputNumber v-model:value="formValue.showOrder" :min="0" />
          </n-form-item>

          <n-form-item label="是否显示" path="visible">
            <n-switch v-model:value="formValue.visible" />
          </n-form-item>

          <n-form-item label="打开方式" path="openMode">
            <n-radio-group v-model:value="formValue.openMode">
              <n-radio value="router">
                路由
              </n-radio>
              <n-radio value="iframe">
                Iframe
              </n-radio>
            </n-radio-group>
          </n-form-item>

          <n-form-item label="备注" path="remark">
            <n-input v-model:value="formValue.remark" type="textarea" placeholder="请输入备注" />
          </n-form-item>
        </n-form>

        <template #footer>
          <NSpace justify="end">
            <NButton @click="showDrawer = false">
              取消
            </NButton>
            <NButton type="primary" :loading="submitLoading" @click="handleSubmit">
              保存
            </NButton>
          </NSpace>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { NButton, NInputNumber, NSpace, NTreeSelect } from 'naive-ui'
import { h, onMounted, ref, watch } from 'vue'
import IconSelector from '@/components/IconSelector.vue'
import { request } from '@/utils/index.js'

const props = defineProps({
  subAppId: {
    type: [String, Number],
    default: '',
  },
  moduleId: {
    type: [String, Number],
    default: '',
  },
})

// 定义事件
const emit = defineEmits(['refresh'])

const message = window.$message

const loading = ref(false)
const submitLoading = ref(false)
const showDrawer = ref(false)
const drawerTitle = ref('')
const formRef = ref(null)

// 表单数据
const formValue = ref({
  title: '',
  url: '',
  icon: '',
  showOrder: 0,
  visible: true,
  remark: '',
})

// 表单验证规则
const formRules = {
  title: {
    required: true,
    message: '请输入菜单名称',
    trigger: 'blur',
  },
  url: {
    required: true,
    message: '请输入菜单路径',
    trigger: 'blur',
  },
}

// 分页配置
const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: handlePageChange,
})

// 菜单数据
const menuData = ref([])

// 子系统和模块树形数据
const subSysTreeData = ref([])
const subSysTreeLoaded = ref(false)
const status = ref('')
const checkedRowKeysRef = ref([])
const checkedRowKeys = checkedRowKeysRef

// 批量删除
function handleBatchDelete() {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的数据')
    return
  }

  window.$dialog?.warning({
    title: '批量删除',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 条菜单吗?`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete(`/menu`, {
          data: checkedRowKeys.value,
        })
        message.success('批量删除成功')
        checkedRowKeys.value = []
        loadMenuData()
        emit('refresh')
      }
      catch (error) {
        message.error(`批量删除失败: ${error.message}`)
      }
    },
  })
}

// 表格列配置
const columns = [
  {
    type: 'selection',
    width: 80,
  },
  {
    title: '菜单名称',
    key: 'title',
    width: 180,
    align: 'center',
  },
  {
    title: '路径',
    key: 'url',
    width: 200,
    align: 'center',
  },
  {
    title: '图标',
    key: 'icon',
    width: 150,
    align: 'center',
    render(row) {
      return h('div', { style: 'display: flex; align-items: center; justify-content: center;' }, [
        h(IconSelector, {
          'modelValue': row.icon,
          'onUpdate:modelValue': value => handleIconChange(row, value),
        }),
      ])
    },
  },
  {
    title: '所属模块',
    key: 'moduleId',
    width: 200,
    align: 'center',
    render(row) {
      return h(NTreeSelect, {
        value: `${row.moduleId}#${row.subAppId}`,
        options: subSysTreeData.value,
        labelField: 'title',
        keyField: '_value',
        childrenField: 'children',
        placeholder: '请选择所属模块',
        onUpdateValue: value => handleModuleChange(row, value),
      })
    },
  },
  {
    title: '排序',
    key: 'showOrder',
    width: 110,
    align: 'center',
    render(row) {
      return h(NInputNumber, {
        value: row.showOrder,
        min: 0,
        onUpdateValue: value => handleOrderChange(row, value),
      })
    },
  },
  {
    title: 'iframe',
    key: 'openMode',
    width: 150,
    align: 'center',
    render(row) {
      return row.openMode === 'iframe' ? 'iframe' : '否'
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    align: 'center',
    fixed: 'right',
    render(row) {
      return h(NSpace, { justify: 'center' }, {
        default: () => [
          h(NButton, {
            type: 'info',
            size: 'small',
            quaternary: true,
            onClick: () => handleEdit(row),
          }, { default: () => '编辑' }),
          h(NButton, {
            type: 'error',
            size: 'small',
            quaternary: true,
            onClick: () => handleDelete(row),
          }, { default: () => '删除' }),
        ],
      })
    },
  },
]

// 处理图标变化
let iconChangeTimer = null
function handleIconChange(row, value) {
  row.icon = value
  if (iconChangeTimer) {
    clearTimeout(iconChangeTimer)
  }
  iconChangeTimer = setTimeout(async () => {
    try {
      await request.put('/menu', row)
      message.success('图标更新成功')
      loadMenuData()
    }
    catch (error) {
      message.error(`图标更新失败: ${error.message || error}`)
    }
  }, 300)
}

// 处理排序变化
let orderChangeTimer = null
function handleOrderChange(row, value) {
  row.showOrder = value
  if (orderChangeTimer) {
    clearTimeout(orderChangeTimer)
  }
  orderChangeTimer = setTimeout(async () => {
    try {
      await request.put('/menu', row)
      message.success('排序更新成功')
      loadMenuData()
    }
    catch (error) {
      message.error(`排序更新失败: ${error.message || error}`)
    }
  }, 300)
}

// 处理模块变化
let moduleChangeTimer = null
function handleModuleChange(row, value) {
  if (!value)
    return

  const [moduleId, subAppId] = value.split('#')
  row.moduleId = moduleId
  row.subAppId = subAppId

  if (moduleChangeTimer) {
    clearTimeout(moduleChangeTimer)
  }
  moduleChangeTimer = setTimeout(async () => {
    try {
      await request.put('/menu', row)
      message.success('所属模块更新成功')
      loadMenuData()
    }
    catch (error) {
      message.error(`所属模块更新失败: ${error.message || error}`)
    }
  }, 300)
}

// 获取子系统树数据
async function loadSubSysTree() {
  if (subSysTreeLoaded.value)
    return

  try {
    const res = await request.get('/subApp/tree')
    const data = res.data || res || []

    // 转换树形数据格式,支持 n-tree-select
    subSysTreeData.value = mapSubSysTreeData(data)
    subSysTreeLoaded.value = true
  }
  catch (error) {
    console.error('获取子系统树失败:', error)
  }
}

// 映射子系统树数据为 n-tree-select 需要的格式
function mapSubSysTreeData(data) {
  if (!Array.isArray(data))
    return []

  return data.map((item) => {
    const node = {
      title: item.name || item.title,
      key: item.id,
      _value: `${item.id}#${item.id}`, // 用于保存 moduleId#subAppId
      selectable: false, // 子系统节点不可选
      children: [],
    }

    // 处理模块子节点
    if (item.children && Array.isArray(item.children)) {
      node.children = item.children.map((module) => {
        const moduleNode = {
          title: module.name || module.title,
          key: module.id,
          _value: `${module.id}#${item.id}`, // moduleId#subAppId
          selectable: true, // 模块节点可选
          children: [],
        }

        // 处理子模块
        if (module.children && Array.isArray(module.children)) {
          moduleNode.children = module.children.map(subModule => ({
            title: subModule.name || subModule.title,
            key: subModule.id,
            _value: `${subModule.id}#${item.id}`, // moduleId#subAppId
            selectable: true,
          }))
        }

        return moduleNode
      })
    }

    return node
  })
}

// 获取菜单数据
async function loadMenuData() {
  // 确保必要的参数都存在
  if (!props.subAppId || !props.moduleId) {
    return
  }

  try {
    loading.value = true
    // 调用实际的API接口，保持与旧版本一致
    const res = await request.get('/menu/plist', {
      params: {
        subAppId: props.subAppId,
        moduleId: props.moduleId,
        page: pagination.value.page,
        pageSize: pagination.value.pageSize,
      },
    })
    // 兼容新旧数据结构
    const list = res.data?.list || res.data || []
    const total = res.data?.total || res.total || 0

    menuData.value = list
    // 确保 total是数字类型
    pagination.value.itemCount = Number(total)
  }
  catch (error) {
    message.error(`获取菜单数据失败: ${error.message || error}`)
  }
  finally {
    loading.value = false
  }
}

// 处理分页变化
function handlePageChange(page) {
  pagination.value.page = page
  loadMenuData()
}

// 新增菜单
function handleAdd() {
  drawerTitle.value = '新增菜单'
  status.value = 'add'
  formValue.value = {
    title: '',
    url: '',
    icon: '',
    showOrder: 0,
    visible: true,
    remark: '',
  }
  showDrawer.value = true
}

// 编辑菜单
function handleEdit(row) {
  drawerTitle.value = '编辑菜单'
  status.value = 'edit'
  formValue.value = { ...row }
  showDrawer.value = true
}

// 删除菜单
function handleDelete(row) {
  window.$dialog?.warning({
    title: '提示',
    content: `确定要删除菜单 "${row.title}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 调用实际的API接口，保持与旧版本一致，使用 _id 字段
        await request.delete(`/menu`, {
          data: [row.id],
        })
        loadMenuData()
        // 通知父组件刷新
        emit('refresh')
      }
      catch (error) {
      }
    },
  })
}

// 提交表单
function handleSubmit() {
  formRef.value?.validate(async (errors) => {
    if (errors) {
      message.error('请检查表单填写是否正确')
      return
    }

    try {
      submitLoading.value = true
      // 添加模块ID和子应用ID，自动使用当前选中的模块
      const formData = {
        ...formValue.value,
        moduleId: props.moduleId, // 自动设置为当前模块
        subAppId: props.subAppId, // 自动设置为当前子系统
      }

      // 如果 openMode 为 '0'，删除该字段（与旧版本保持一致）
      if (formData.openMode === '0') {
        delete formData.openMode
      }

      // 调用实际的API接口,保持与旧版本一致
      if (status.value === 'edit') {
        await request.put('/menu', formData)
        message.success('更新成功')
      }
      else {
        await request.post('/menu', formData)
        message.success('新增成功')
      }
      showDrawer.value = false
      loadMenuData()
      // 通知父组件刷新
      emit('refresh')
    }
    catch (error) {
    }
    finally {
      submitLoading.value = false
    }
  })
}

// 刷新数据
function refresh() {
  loadMenuData()
}

// 暴露方法给父组件
defineExpose({
  refresh,
})

// 监听模块ID变化
watch([() => props.subAppId, () => props.moduleId], ([newSubAppId, newModuleId]) => {
  if (newSubAppId && newModuleId) {
    loadMenuData()
    // 加载子系统树数据用于模块选择
    loadSubSysTree()
  }
}, { immediate: true })

// 初始化
onMounted(() => {
  if (props.subAppId && props.moduleId) {
    loadMenuData()
    // 加载子系统树数据
    loadSubSysTree()
  }
})
</script>

<style scoped>
.menu-management {
  padding: 16px 0;
}
</style>
