<template>
  <div class="api-config-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/apiConfig"
      :api-config="{
        list: 'get@/system/apiConfig/page',
        detail: 'post@/system/apiConfig/getById',
        add: 'post@/system/apiConfig/add',
        update: 'post@/system/apiConfig/edit',
        delete: 'post@/system/apiConfig/remove'
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      add-button-text="新增接口配置"
      :load-detail-on-edit="true"
      :edit-grid-cols="2"
      :modal-width="'900px'"
      :before-render-detail="handleBeforeRenderDetail"
      :before-submit="handleBeforeSubmit"
    >
      <!-- 自定义工具栏 -->
      <template #toolbar>
        <n-button
          type="primary"
          @click="handleRegisterApiConfigs"
          :loading="registerLoading"
        >
          <template #icon>
            <i class="i-material-symbols:auto-awesome" />
          </template>
          自动注册接口
        </n-button>
        <n-button
          type="warning"
          @click="handleRefreshCache"
          :loading="refreshLoading"
        >
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          刷新缓存
        </n-button>
      </template>

    </AiCrudPage>

    <!-- 查看详情弹窗 -->
    <n-modal
      v-model:show="detailVisible"
      title="接口配置详情"
      preset="card"
      style="width: 800px"
      :mask-closable="false"
    >
      <div v-if="currentConfig" class="detail-content">
        <n-descriptions bordered :column="2">
          <n-descriptions-item label="ID">
            {{ currentConfig.id }}
          </n-descriptions-item>
          <n-descriptions-item label="接口名称">
            {{ currentConfig.apiName }}
          </n-descriptions-item>
          <n-descriptions-item label="接口编码">
            {{ currentConfig.apiCode }}
          </n-descriptions-item>
          <n-descriptions-item label="请求方式">
            <n-tag :type="getMethodColor(currentConfig.reqMethod)" size="small">
              {{ currentConfig.reqMethod }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="请求路径" :span="2">
            {{ currentConfig.urlPath }}
          </n-descriptions-item>
          <n-descriptions-item label="版本号">
            {{ currentConfig.apiVersion || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="业务模块">
            {{ currentConfig.moduleCode || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="微服务ID" :span="2">
            {{ currentConfig.serviceId || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="需认证/鉴权">
            <n-tag :type="currentConfig.authFlag === 1 ? 'success' : 'default'" size="small">
              {{ currentConfig.authFlag === 1 ? '是' : '否' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="需报文加解密">
            <n-tag :type="currentConfig.encryptFlag === 1 ? 'success' : 'default'" size="small">
              {{ currentConfig.encryptFlag === 1 ? '是' : '否' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="启用租户隔离">
            <n-tag :type="currentConfig.tenantFlag === 1 ? 'success' : 'default'" size="small">
              {{ currentConfig.tenantFlag === 1 ? '启用' : '不启用' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="开启限流">
            <n-tag :type="currentConfig.limitFlag === 1 ? 'warning' : 'default'" size="small">
              {{ currentConfig.limitFlag === 1 ? '开启' : '关闭' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="脱敏字段" :span="2">
            {{ currentConfig.sensitiveFields || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="状态">
            <n-tag :type="currentConfig.status === 1 ? 'success' : 'error'" size="small">
              {{ currentConfig.status === 1 ? '正常' : '停用' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="创建时间">
            {{ currentConfig.createTime }}
          </n-descriptions-item>
          <n-descriptions-item label="备注说明" :span="2">
            {{ currentConfig.remark || '-' }}
          </n-descriptions-item>
        </n-descriptions>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="detailVisible = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, h, computed } from 'vue'
import { NTag } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'ApiConfig' })

const crudRef = ref(null)
const detailVisible = ref(false)
const currentConfig = ref(null)
const refreshLoading = ref(false)
const registerLoading = ref(false)

// 请求方式选项
const reqMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'ALL', value: 'ALL' }
]

// 是否选项
const yesNoOptions = [
  { label: '需要', value: 1 },
  { label: '不需要', value: 0 }
]

// 状态选项
const statusOptions = [
  { label: '正常', value: 1 },
  { label: '停用', value: 0 }
]

// 获取请求方式颜色
function getMethodColor(method) {
  const colorMap = {
    'GET': 'info',
    'POST': 'success',
    'PUT': 'warning',
    'DELETE': 'error',
    'ALL': 'default'
  }
  return colorMap[method] || 'default'
}

// 搜索表单配置
const searchSchema = [
  {
    field: 'apiName',
    label: '接口名称',
    type: 'input',
    props: {
      placeholder: '请输入接口名称'
    }
  },
  {
    field: 'apiCode',
    label: '接口编码',
    type: 'input',
    props: {
      placeholder: '请输入接口编码'
    }
  },
  {
    field: 'reqMethod',
    label: '请求方式',
    type: 'select',
    props: {
      placeholder: '请选择',
      options: reqMethodOptions,
      clearable: true
    }
  },
  {
    field: 'urlPath',
    label: '请求路径',
    type: 'input',
    props: {
      placeholder: '请输入请求路径'
    }
  },
  {
    field: 'moduleCode',
    label: '业务模块',
    type: 'input',
    props: {
      placeholder: '请输入业务模块'
    }
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择',
      options: statusOptions,
      clearable: true
    }
  }
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'id',
    label: 'ID',
    width: 80
  },
  {
    prop: 'apiName',
    label: '接口名称',
    width: 150,
    showOverflowTooltip: true
  },
  {
    prop: 'apiCode',
    label: '接口编码',
    width: 150,
    showOverflowTooltip: true
  },
  {
    prop: 'reqMethod',
    label: '请求方式',
    width: 90,
    render: (row) => {
      return h(NTag,
        { type: getMethodColor(row.reqMethod), size: 'small' },
        { default: () => row.reqMethod }
      )
    }
  },
  {
    prop: 'urlPath',
    label: '请求路径',
    width: 250,
    showOverflowTooltip: true
  },
  {
    prop: 'apiVersion',
    label: '版本号',
    width: 90
  },
  {
    prop: 'moduleCode',
    label: '业务模块',
    width: 100
  },
  {
    prop: 'serviceId',
    label: '微服务ID',
    width: 120,
    showOverflowTooltip: true
  },
  {
    prop: 'authFlag',
    label: '需认证',
    width: 80,
    render: (row) => {
      return h(NTag,
        { type: row.authFlag === 1 ? 'success' : 'default', size: 'small' },
        { default: () => row.authFlag === 1 ? '是' : '否' }
      )
    }
  },
  {
    prop: 'encryptFlag',
    label: '需加解密',
    width: 90,
    render: (row) => {
      return h(NTag,
        { type: row.encryptFlag === 1 ? 'success' : 'default', size: 'small' },
        { default: () => row.encryptFlag === 1 ? '是' : '否' }
      )
    }
  },
  {
    prop: 'tenantFlag',
    label: '租户隔离',
    width: 90,
    render: (row) => {
      return h(NTag,
        { type: row.tenantFlag === 1 ? 'success' : 'default', size: 'small' },
        { default: () => row.tenantFlag === 1 ? '启用' : '不启用' }
      )
    }
  },
  {
    prop: 'limitFlag',
    label: '限流',
    width: 80,
    render: (row) => {
      return h(NTag,
        { type: row.limitFlag === 1 ? 'warning' : 'default', size: 'small' },
        { default: () => row.limitFlag === 1 ? '开启' : '关闭' }
      )
    }
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag,
        { type: row.status === 1 ? 'success' : 'error', size: 'small' },
        { default: () => row.status === 1 ? '正常' : '停用' }
      )
    }
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 160
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '查看', key: 'view', type: 'primary', onClick: handleView },
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete }
    ]
  }
])

// 编辑表单配置
const editSchema = [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'apiName',
    label: '接口名称',
    type: 'input',
    rules: [{ required: true, message: '请输入接口名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入接口名称，如：查询用户信息'
    }
  },
  {
    field: 'apiCode',
    label: '接口编码',
    type: 'input',
    rules: [{ required: true, message: '请输入接口编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入接口编码，如：user:info:query'
    }
  },
  {
    field: 'reqMethod',
    label: '请求方式',
    type: 'select',
    rules: [{ required: true, message: '请选择请求方式', trigger: 'change' }],
    props: {
      options: reqMethodOptions,
      placeholder: '请选择请求方式'
    }
  },
  {
    field: 'urlPath',
    label: '请求路径',
    type: 'input',
    span: 2,
    rules: [{ required: true, message: '请输入请求路径', trigger: 'blur' }],
    props: {
      placeholder: '请输入请求路径，支持Ant风格，如 /api/user/**'
    }
  },
  {
    field: 'apiVersion',
    label: '版本号',
    type: 'input',
    props: {
      placeholder: '请输入版本号，如：v1.0'
    }
  },
  {
    field: 'moduleCode',
    label: '业务模块',
    type: 'input',
    props: {
      placeholder: '请输入业务模块，如：sys, order, pay'
    }
  },
  {
    field: 'serviceId',
    label: '微服务ID',
    type: 'input',
    span: 2,
    props: {
      placeholder: '请输入微服务ID（微服务架构时使用）'
    }
  },
  {
    type: 'divider',
    label: '安全配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'authFlag',
    label: '需认证/鉴权',
    type: 'select',
    defaultValue: 1,
    props: {
      options: yesNoOptions,
      clearable: false
    }
  },
  {
    field: 'encryptFlag',
    label: '需报文加解密',
    type: 'select',
    defaultValue: 0,
    props: {
      options: yesNoOptions,
      clearable: false
    }
  },
  {
    field: 'tenantFlag',
    label: '启用租户隔离',
    type: 'select',
    defaultValue: 0,
    props: {
      options: yesNoOptions,
      clearable: false
    }
  },
  {
    field: 'limitFlag',
    label: '开启限流',
    type: 'select',
    defaultValue: 0,
    props: {
      options: yesNoOptions,
      clearable: false
    }
  },
  {
    field: 'sensitiveFields',
    label: '脱敏字段',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入需脱敏字段，JSON数组格式，如 ["phone", "id_card"]',
      rows: 2
    }
  },
  {
    type: 'divider',
    label: '其他信息',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    defaultValue: 1,
    props: {
      options: statusOptions,
      clearable: false
    }
  },
  {
    field: 'remark',
    label: '备注说明',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注说明',
      rows: 3
    }
  }
]

// 查看详情
async function handleView(row) {
  try {
    const res = await request.get('/system/apiConfig/getById', {
      params: { id: row.id }
    })
    if (res.code === 200) {
      currentConfig.value = res.data
      detailVisible.value = true
    }
  } catch (error) {
    window.$message.error('获取详情失败')
  }
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除该接口配置吗？删除后将无法恢复！',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/apiConfig/remove', null, {
          params: { id: row.id }
        })
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      } catch (error) {
        window.$message.error('删除失败')
      }
    }
  })
}

// 自动注册接口配置
async function handleRegisterApiConfigs() {
  window.$dialog.warning({
    title: '确认自动注册',
    content: '确定要自动注册所有接口配置吗？此操作将扫描系统中的所有接口并自动生成配置。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        registerLoading.value = true
        const res = await request.post('/apiConfig/registerApiConfigs')
        if (res.code === 200) {
          window.$message.success('接口配置自动注册任务已启动，请稍后刷新查看结果')
          crudRef.value?.refresh()
        }
      } catch (error) {
        window.$message.error('自动注册失败')
      } finally {
        registerLoading.value = false
      }
    }
  })
}

// 刷新缓存
async function handleRefreshCache() {
  window.$dialog.warning({
    title: '确认刷新',
    content: '确定要刷新所有API配置缓存吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        refreshLoading.value = true
        const res = await request.post('/apiConfig/refresh')
        if (res.code === 200) {
          window.$message.success('缓存刷新成功')
        }
      } catch (error) {
        window.$message.error('缓存刷新失败')
      } finally {
        refreshLoading.value = false
      }
    }
  })
}

// 详情数据渲染前钩子 - 处理数据类型问题
function handleBeforeRenderDetail(data) {
  if (!data) return data
  
  // 将字符串类型的数字字段转换为数字类型，确保select组件能正确匹配
  const numberFields = ['status', 'authFlag', 'encryptFlag', 'tenantFlag', 'limitFlag']
  numberFields.forEach(field => {
    if (data[field] !== null && data[field] !== undefined) {
      data[field] = Number(data[field])
    }
  })
  
  return data
}

// 表单提交前钩子 - 处理数据
function handleBeforeSubmit(formData) {
  console.log('提交的表单数据:', formData)
  
  // 确保数字字段是数字类型
  const numberFields = ['status', 'authFlag', 'encryptFlag', 'tenantFlag', 'limitFlag']
  numberFields.forEach(field => {
    if (formData[field] !== null && formData[field] !== undefined) {
      formData[field] = Number(formData[field])
    }
  })
  
  return formData
}
</script>

<style scoped>
.api-config-page {
  height: 100%;
}

.detail-content {
  padding: 8px 0;
}
</style>
