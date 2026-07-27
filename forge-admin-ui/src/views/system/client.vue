<template>
  <div class="client-management-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/client"
      :api-config="{
        list: 'get@/system/client/page',
        detail: 'get@/system/client/:id',
        add: 'post@/system/client',
        update: 'put@/system/client',
        delete: 'delete@/system/client/:id',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      :before-render-detail="beforeRenderDetail"
      row-key="id"
      :edit-grid-cols="2"
      modal-width="900px"
      add-button-text="新增客户端"
    />

    <n-modal
      v-model:show="onlineModalVisible"
      title="在线用户"
      preset="card"
      style="width: 80%"
    >
      <div class="mb-16">
        <n-statistic label="当前在线人数" :value="onlineUsers.length" />
      </div>
      <n-data-table
        :columns="onlineTableColumns"
        :data="onlineUsers"
        :pagination="false"
      />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { NButton, NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'
import { toBooleanDictOptions, toNumberDictOptions } from '@/utils/dict-options'

defineOptions({ name: 'SystemClient' })

const message = window.$message
const crudRef = ref()
const onlineModalVisible = ref(false)
const onlineUsers = ref([])
const { dict, getLabel } = useDict(
  'sys_client_auth_method',
  'sys_enable_disable',
  'sys_yes_no',
  'sys_client_login_auth_type',
  'sys_client_captcha_type',
)
const clientAuthMethodOptions = computed(() => dict.value.sys_client_auth_method || [])
const statusOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))
const booleanOptions = computed(() => toBooleanDictOptions(dict.value.sys_yes_no))
const authTypeOptions = computed(() => dict.value.sys_client_login_auth_type || [])
const captchaTypeOptions = computed(() => dict.value.sys_client_captcha_type || [])

// 搜索配置
const searchSchema = computed(() => [
  {
    field: 'clientCode',
    label: '客户端编码',
    type: 'input',
    props: {
      placeholder: '请输入客户端编码',
    },
  },
  {
    field: 'clientName',
    label: '客户端名称',
    type: 'input',
    props: {
      placeholder: '请输入客户端名称',
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: statusOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'clientName',
    label: '客户端',
    minWidth: 190,
    render: row => h(SystemTableCell, {
      title: row.clientName,
      subtitle: row.clientCode,
      interactive: true,
      avatar: true,
      tooltip: `查看客户端：${row.clientName || row.clientCode || '-'}`,
      onActivate: () => crudRef.value?.showDetail(row),
    }),
  },
  {
    prop: 'appId',
    label: 'AppId',
    width: 180,
  },
  {
    prop: 'tokenTimeout',
    label: 'Token有效期',
    width: 120,
    render: (row) => {
      const timeout = row.tokenTimeout
      if (timeout >= 86400) {
        return `${(timeout / 86400).toFixed(0)} 天`
      }
      else if (timeout >= 3600) {
        return `${(timeout / 3600).toFixed(0)} 小时`
      }
      else {
        return `${timeout} 秒`
      }
    },
  },
  {
    prop: 'concurrentLogin',
    label: '并发登录',
    width: 100,
    render: row => h(DictTag, { options: booleanOptions.value, value: row.concurrentLogin, forceTag: true }),
  },
  {
    prop: 'authTypes',
    label: '支持的认证方式',
    width: 200,
    render: (row) => {
      const types = row.authTypes?.split(',') || []
      return types.map(type =>
        h(NTag, { type: 'info', size: 'small', style: 'margin: 2px' }, { default: () => getLabel('sys_client_login_auth_type', type) }),
      )
    },
  },
  {
    prop: 'captchaType',
    label: '验证码覆盖',
    width: 120,
    render: row => h(DictTag, { options: captchaTypeOptions.value, value: row.captchaType || '', forceTag: true }),
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render: row => h(DictTag, { options: statusOptions.value, value: row.status, forceTag: true }),
  },
  {
    prop: 'description',
    label: '描述',
    minWidth: 80,
  },
  {
    prop: 'action',
    label: '操作',
    width: 160,
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: row => crudRef.value?.showEdit(row) },
      { label: '在线用户', key: 'online', type: 'primary', onClick: handleViewOnline },
      { label: '刷新缓存', key: 'reloadCache', type: 'primary', onClick: handleReloadCache },
      { label: '删除', key: 'delete', type: 'error', onClick: row => crudRef.value?.handleDelete(row), visible: row => row.id > 4 },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'clientCode',
    label: '客户端编码',
    type: 'input',
    rules: [{ required: true, message: '请输入客户端编码', trigger: 'blur' }],
    props: {
      placeholder: '如：pc, app, h5, wechat',
    },
  },
  {
    field: 'clientName',
    label: '客户端名称',
    type: 'input',
    rules: [{ required: true, message: '请输入客户端名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入客户端名称',
    },
  },
  {
    field: 'appId',
    label: 'AppId',
    type: 'input',
    rules: [{ required: true, message: '请输入AppId', trigger: 'blur' }],
    props: {
      placeholder: '应用ID',
    },
  },
  {
    field: 'clientAuthMethod',
    label: '客户端认证方式',
    type: 'radio',
    defaultValue: 'client_secret',
    rules: [{ required: true, message: '请选择客户端认证方式', trigger: 'change' }],
    props: {
      options: clientAuthMethodOptions.value,
    },
  },
  {
    field: 'appSecret',
    label: 'AppSecret',
    type: 'input',
    props: {
      type: 'password',
      placeholder: '新增时必填；编辑留空表示不轮换',
    },
    vIf: formData => formData.clientAuthMethod === 'client_secret',
  },
  {
    type: 'divider',
    label: 'Token配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'tokenTimeout',
    label: 'Token有效期(秒)',
    type: 'number',
    defaultValue: 7200,
    rules: [{ required: true, message: '请输入Token有效期' }],
    props: {
      placeholder: '如：86400(1天), 2592000(30天)',
      min: 60,
      step: 3600,
    },
  },
  {
    field: 'tokenActivityTimeout',
    label: 'Token活跃超时(秒)',
    type: 'number',
    defaultValue: -1,
    props: {
      placeholder: '-1表示不限制',
    },
  },
  {
    field: 'concurrentLogin',
    label: '允许并发登录',
    type: 'radio',
    defaultValue: false,
    props: {
      options: booleanOptions.value,
    },
  },
  {
    field: 'shareToken',
    label: '共享Token',
    type: 'radio',
    defaultValue: false,
    props: {
      options: booleanOptions.value,
    },
  },
  {
    field: 'authTypes',
    label: '支持的认证方式',
    type: 'select',
    props: {
      placeholder: '请选择支持的认证方式',
      options: authTypeOptions.value,
      multiple: true,
    },
  },
  {
    field: 'captchaType',
    label: '验证码覆盖',
    type: 'select',
    defaultValue: '',
    props: {
      placeholder: '为空时继承全局登录配置',
      options: captchaTypeOptions.value,
    },
  },
  {
    type: 'divider',
    label: '状态配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: statusOptions.value,
    },
  },
  {
    field: 'description',
    label: '描述',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入客户端描述',
      rows: 3,
    },
  },
])

// 在线用户表格列
const onlineTableColumns = [
  { title: '用户ID', key: 'userId', width: 80 },
  { title: '用户名', key: 'username', width: 120 },
  { title: '真实姓名', key: 'realName', width: 120 },
  { title: '登录时间', key: 'loginTime', width: 180 },
  { title: 'IP地址', key: 'ipAddress', width: 140 },
  { title: '浏览器', key: 'browser', width: 120 },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: (row) => {
      return h(NButton, {
        type: 'error',
        size: 'small',
        onClick: () => handleKickoutUser(row),
      }, {
        default: () => '踢出',
      })
    },
  },
]

// 详情数据渲染前处理：将字符串转为数组
function beforeRenderDetail(data) {
  // 将authTypes字符串转为数组，供多选select使用
  if (data.authTypes && typeof data.authTypes === 'string') {
    data.authTypes = data.authTypes.split(',').filter(Boolean)
  }

  return data
}

// 提交前处理：将数组转为字符串
function beforeSubmit(formData) {
  // 将authTypes数组转为逗号分隔的字符串
  if (Array.isArray(formData.authTypes)) {
    formData.authTypes = formData.authTypes.join(',')
  }

  return formData
}

// 查看在线用户
async function handleViewOnline(row) {
  try {
    const res = await request.get(`/system/client/online/${row.clientCode}`)
    if (res.data) {
      onlineUsers.value = res.data
      onlineModalVisible.value = true
    }
  }
  catch {
    message.error('获取在线用户失败')
  }
}

// 踢出用户
async function handleKickoutUser(row) {
  try {
    await request.post(`/system/client/kickout/${row.userId}/pc`)
    message.success('已踢出该用户')
    handleViewOnline({ clientCode: 'pc' })
  }
  catch {
    message.error('踢出失败')
  }
}

// 刷新缓存
async function handleReloadCache(row) {
  try {
    await request.post(`/system/client/reload-cache/${row.clientCode}`)
    message.success('缓存已刷新')
  }
  catch {
    message.error('刷新缓存失败')
  }
}
</script>

<style scoped lang="scss">
.client-management-page {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.client-management-page :deep(.ai-crud-page) {
  flex: 1 1 auto;
  min-height: 0;
}
</style>
