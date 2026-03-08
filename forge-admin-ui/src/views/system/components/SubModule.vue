<template>
  <div class="sub-module-management">
    <n-space vertical>
      <n-space>
        <n-button type="primary" @click="handleAdd" class="add-module-btn">新增模块</n-button>
        <n-button
            type="error"
            :disabled="checkedRowKeys.length === 0"
            @click="handleBatchDelete"
        >
          批量删除  {{checkedRowKeys.length ? `(${checkedRowKeys.length})`:''}}
        </n-button>
      </n-space>

      <n-data-table
        size="small"
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :remote="true"
        @update:page="handlePageChange"
         :row-key="(rowData) => rowData.id"
      />
    </n-space>

    <!-- 编辑抽屉 -->
    <n-drawer v-model:show="showDrawer" :width="500" placement="right">
      <n-drawer-content :title="drawerTitle" closable>
        <n-form
          ref="formRef"
          :model="formValue"
          :rules="formRules"
          label-placement="left"
          label-width="100"
        >
          <n-form-item label="模块名称" path="name">
            <n-input v-model:value="formValue.name" placeholder="请输入模块名称" />
          </n-form-item>

          <n-form-item label="模块ID" path="id">
            <n-input v-model:value="formValue.id" placeholder="请输入模块ID" :disabled="status==='edit'"/>
          </n-form-item>

          <n-form-item label="图标" path="icon">
            <IconSelector v-model="formValue.icon" />
          </n-form-item>

          <n-form-item label="排序" path="sort">
            <n-input-number v-model:value="formValue.sort" :min="0" />
          </n-form-item>

          <n-form-item label="类型" path="type">
            <n-select
              v-model:value="formValue.type"
              :options="typeOptions"
              placeholder="请选择类型"
            />
          </n-form-item>

          <n-form-item label="描述" path="remark">
            <n-input v-model:value="formValue.remark" type="textarea" placeholder="请输入描述" />
          </n-form-item>
        </n-form>

        <template #footer>
          <n-space justify="end">
            <n-button @click="showDrawer = false">取消</n-button>
            <n-button type="primary" @click="handleSubmit" :loading="submitLoading">保存</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, h } from 'vue'
import { NButton, NSpace, NInputNumber } from 'naive-ui'
import IconSelector from '@/components/IconSelector.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import { request } from '@/utils'

const props = defineProps({
  subAppId: {
    type: [String, Number],
    default: ''
  },
  currentItem: {
    type: Object,
    default: () => ({})
  }
})

const message = window.$message

// 数据相关
const loading = ref(false)
const submitLoading = ref(false)
const showDrawer = ref(false)
const drawerTitle = ref('')
const tableData = ref([])
const formRef = ref(null)
const status = ref('')

// 表单数据
const formValue = ref({
  _id: '', // 使用 _id 字段判断是编辑还是新增
  id: '',
  name: '',
  icon: '',
  sort: 0,
  type: '1',
  remark: ''
})

// 类型选项
const typeOptions = [
  { label: 'PC', value: '1' },
  { label: 'APP', value: '2' },
  { label: '小程序', value: '3' }
]

const checkedRowKeysRef = ref([])
const checkedRowKeys = checkedRowKeysRef
// 表单验证规则
const formRules = {
  name: {
    required: true,
    message: '请输入模块名称',
    trigger: 'blur'
  },
  id: {
    required: true,
    message: '请输入模块ID',
    trigger: 'blur'
  }
}

// 分页配置
const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: handlePageChange
})

// 表格列配置
const columns = [
  {
    type: "selection",
    width: 80,
  },
  {
    title: '模块名称',
    key: 'name',
    width: 150,
    align: 'center'
  },
  {
    title: '系统名称',
    key: 'subAppName',
    width: 150,
    align: 'center'
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
        onUpdateValue: (value) => handleOrderChange(row, value)
      })
    }
  },
  {
    title: '模块ID',
    key: 'id',
    width: 215,
    align: 'center'
  },
  {
    title: 'icon',
    key: 'icon',
    width: 150,
    align: 'center',
    render(row) {
      return h('div', { style: 'display: flex; align-items: center; justify-content: center;' }, [
        h(IconSelector, {
          modelValue: row.icon,
          'onUpdate:modelValue': (value) => handleIconChange(row, value)
        }),
      ])
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    align: 'center',
    fixed: 'right',
    render(row) {
      return h(NSpace, { justify: 'center' }, {
        default: () => [
          h(NButton, {
            type: 'info',
            size: 'small',
            quaternary:true,
            onClick: () => handleEdit(row)
          }, { default: () => '编辑' }),
          h(NButton, {
            type: 'error',
            size: 'small',
            quaternary:true,
            onClick: () => handleDelete(row)
          }, { default: () => '删除' })
        ]
      })
    }
  }
]

// 批量删除
function handleBatchDelete() {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的数据')
    return
  }

  window.$dialog?.warning({
    title: '批量删除',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 条模块吗?`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete(`/module`,{
          data:checkedRowKeys.value
        })
        message.success('批量删除成功')
        checkedRowKeys.value = []
        loadMenuData()
        emit('refresh')
      } catch (error) {
      }
    }
  })
}
// 处理图标变化
let iconChangeTimer = null
function handleIconChange(row, value) {
  row.icon = value
  if (iconChangeTimer) {
    clearTimeout(iconChangeTimer)
  }
  iconChangeTimer = setTimeout(async () => {
    try {
      await request.put('/module', row)
      loadData()
    } catch (error) {
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
      await request.put('/module', row)
      loadData()
    } catch (error) {
    }
  }, 300)
}

// 获取数据
async function loadData() {
  // 确保必要的参数都存在
  if (!props.subAppId) {
    return
  }

  try {
    loading.value = true
    // 调用实际的API接口，保持与旧版本一致
    const res = await request.get('/module', {
      params: {
        subAppId: props.subAppId,
        page: pagination.value.page,
        pageSize: pagination.value.pageSize
      }
    })
    // 兼容新旧数据结构
    const list = res.data?.list || res.data || []
    const total = res.data?.total || res.total || 0

    tableData.value = list
    // 确保 total是数字类型
    pagination.value.itemCount = Number(total)
  } catch (error) {
  } finally {
    loading.value = false
  }
}

// 处理分页变化
function handlePageChange(page) {
  pagination.value.page = page
  loadData()
}

// 新增
function handleAdd() {
  drawerTitle.value = '新增模块'
  status.value = 'add'
  formValue.value = {
    _id: '', // 使用 _id 字段
    id: '',
    name: '',
    icon: '',
    sort: 0,
    type: '1',
    remark: ''
  }
  showDrawer.value = true
}

// 编辑
function handleEdit(row) {
  drawerTitle.value = '编辑模块'
  status.value = 'edit'
  formValue.value = { ...row,type:row.type.val }
  showDrawer.value = true
}

// 删除
function handleDelete(row) {
  window.$dialog?.warning({
    title: '提示',
    content: `确定要删除模块 "${row.name}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 调用实际的API接口，使用 _id 或 id 字段
        await request.delete(`/module`,{
          data:[row.id]
        })
        loadData()
        // 通知父组件刷新
        emit('refresh')
      } catch (error) {
      }
    }
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
      // 添加子应用ID
      const formData = {
        ...formValue.value,
        subAppId: props.subAppId
      }
      // 根据是否有 _id 判断是编辑还是新增
      if (status.value ==='edit') {
        // 编辑 - 使用 PUT
        await request.put('/module', formData)
        message.success('更新成功')
      } else {
        // 新增 - 使用 POST
        await request.post('/module', formData)
        message.success('新增成功')
      }
      showDrawer.value = false
      loadData()
      // 通知父组件刷新树
      emit('refresh')
    } catch (error) {
    } finally {
      submitLoading.value = false
    }
  })
}

// 刷新数据
function refresh() {
  loadData()
}

// 定义事件
const emit = defineEmits(['refresh'])

// 暴露方法给父组件
defineExpose({
  refresh
})

// 监听子应用ID变化
watch(() => props.subAppId, () => {
  if (props.subAppId) {
    loadData()
  }
}, { immediate: true })

// 初始化
onMounted(() => {
  if (props.subAppId) {
    loadData()
  }
})
</script>

<style scoped>
.sub-module-management {
  padding: 16px 0;
}
</style>
