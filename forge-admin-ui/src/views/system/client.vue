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
      :edit-grid-cols="24"
      edit-label-placement="top"
      edit-label-align="left"
      :edit-x-gap="16"
      :edit-y-gap="8"
      edit-form-class="system-edit-form"
      edit-size="small"
      modal-width="960px"
      add-button-text="新增客户端"
    />

    <n-modal
      v-model:show="onlineModalVisible"
      title="在线用户"
      preset="card"
      class="client-online-modal"
      :style="{ width: 'min(960px, calc(100vw - 24px))' }"
    >
      <div class="mb-16">
        <n-statistic label="当前在线人数" :value="onlineUsers.length" />
      </div>
      <n-data-table
        :columns="onlineTableColumns"
        :data="onlineUsers"
        :scroll-x="ONLINE_TABLE_SCROLL_X"
        :pagination="false"
      />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { NButton } from 'naive-ui'
import { computed, h, ref } from 'vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'
import { toBooleanDictOptions, toNumberDictOptions } from '@/utils/dict-options'
import {
  buildSystemClientEditSchema,
  CLIENT_AUTH_METHOD_DICT,
  CLIENT_CAPTCHA_TYPE_DICT,
  CLIENT_ENABLE_DISABLE_DICT,
  CLIENT_LOGIN_AUTH_TYPE_DICT,
  CLIENT_YES_NO_DICT,
  createClientCodeAutoCode,
  resolveClientEditSchemaOptions,
} from '@/views/system/schemas/systemClientEditSchema'

defineOptions({ name: 'SystemClient' })

const message = window.$message
const crudRef = ref()
const clientCodeAuto = createClientCodeAutoCode()
const onlineModalVisible = ref(false)
const onlineUsers = ref([])
/** 当前在线用户弹窗所属客户端，踢出后按此编码刷新 */
const currentOnlineClientCode = ref('')
const { dict } = useDict(
  CLIENT_AUTH_METHOD_DICT,
  CLIENT_ENABLE_DISABLE_DICT,
  CLIENT_YES_NO_DICT,
  CLIENT_LOGIN_AUTH_TYPE_DICT,
  CLIENT_CAPTCHA_TYPE_DICT,
)
const statusOptions = computed(() => toNumberDictOptions(dict.value[CLIENT_ENABLE_DISABLE_DICT]))
const booleanOptions = computed(() => toBooleanDictOptions(dict.value[CLIENT_YES_NO_DICT]))
const authTypeOptions = computed(() => dict.value[CLIENT_LOGIN_AUTH_TYPE_DICT] || [])
const captchaTypeOptions = computed(() => dict.value[CLIENT_CAPTCHA_TYPE_DICT] || [])

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
    render: row => h(SystemTableCell, {
      values: splitTableCellValues(row.authTypes).map(resolveAuthTypeLabel),
    }),
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
      {
        label: '删除',
        key: 'delete',
        type: 'error',
        onClick: row => crudRef.value?.handleDelete(row),
        visible: row => !row.builtIn,
      },
    ],
  },
])

// 编辑表单配置（与快捷新增 systemClient 预设共用 schema 工厂）
const editSchema = computed(() => buildSystemClientEditSchema({
  clientCodeAuto,
  ...resolveClientEditSchemaOptions(dict.value),
  withFieldQuickCreate: true,
}))

/** 在线用户列宽合计，避免无 scroll-x 时撑破弹窗 */
const ONLINE_TABLE_SCROLL_X = 860

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

function splitTableCellValues(value) {
  const rawValues = Array.isArray(value) ? value : [value]
  return rawValues
    .flatMap(item => String(item || '').split(/[、,，]/))
    .map(item => item.trim())
    .filter(Boolean)
}

function resolveAuthTypeLabel(value) {
  return authTypeOptions.value.find(option => String(option.value) === String(value))?.label || value
}

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
    const clientCode = row?.clientCode
    if (!clientCode) {
      message.error('客户端编码无效')
      return
    }
    currentOnlineClientCode.value = clientCode
    const res = await request.get(`/system/client/online/${clientCode}`)
    if (res.data) {
      onlineUsers.value = res.data
      onlineModalVisible.value = true
    }
  }
  catch {
    message.error('获取在线用户失败')
  }
}

// 踢出用户（必须使用当前弹窗客户端，禁止写死 pc）
async function handleKickoutUser(row) {
  const clientCode = currentOnlineClientCode.value
  if (!clientCode || !row?.userId) {
    message.error('踢出失败：缺少客户端或用户信息')
    return
  }
  try {
    await request.post(`/system/client/kickout/${row.userId}/${clientCode}`)
    message.success('已踢出该用户')
    handleViewOnline({ clientCode })
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

<style>
.client-online-modal.n-card,
.client-online-modal .n-card__content {
  max-width: 100%;
  overflow: hidden;
}
</style>
