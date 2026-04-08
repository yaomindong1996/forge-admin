<template>
  <div class="client-management-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/client"
      :api-config="{
        list: 'get@/system/client/page',
        detail: 'get@/system/client/{id}',
        add: 'post@/system/client',
        update: 'put@/system/client',
        delete: 'delete@/system/client/{id}'
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      :before-render-detail="beforeRenderDetail"
      row-key="id"
      :edit-grid-cols="2"
      :modal-width="'900px'"
      add-button-text="新增客户端"
    >
      <template #table-action="{ row }">
        <div class="flex items-center gap-8">
          <a
            class="text-primary cursor-pointer hover:text-primary-hover"
            @click="handleEdit(row)"
          >
            编辑
          </a>
          <span class="text-gray-300">|</span>
          <a
            class="text-info cursor-pointer hover:text-info-hover"
            @click="handleViewOnline(row)"
          >
            在线用户
          </a>
          <span class="text-gray-300">|</span>
          <a
            class="text-warning cursor-pointer hover:text-warning-hover"
            @click="handleReloadCache(row)"
          >
            刷新缓存
          </a>
          <span class="text-gray-300" v-if="row.id > 4">|</span>
          <a
            v-if="row.id > 4"
            class="text-error cursor-pointer hover:text-error-hover"
            @click="handleDelete(row)"
          >
            删除
          </a>
        </div>
      </template>
    </AiCrudPage>

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
import { ref, h } from 'vue'
import { NTag, NButton } from 'naive-ui'
import { request } from '@/utils'

defineOptions({ name: 'SystemClient' })

const message = window.$message
const crudRef = ref()
const onlineModalVisible = ref(false)
const onlineUsers = ref([])

// 状态选项
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

// 是否选项（并发登录、共享Token）
const booleanOptions = [
  { label: '是', value: true },
  { label: '否', value: false }
]

// 认证方式选项
const authTypeOptions = [
  { label: '用户名密码', value: 'password' },
  { label: '用户名密码+验证码', value: 'password_captcha' },
  { label: '手机验证码', value: 'phone_captcha' },
  { label: '微信登录', value: 'wechat' },
  { label: '邮箱验证码', value: 'email_captcha' }
]

// 搜索配置
const searchSchema = [
  {
    field: 'clientCode',
    label: '客户端编码',
    type: 'input',
    props: {
      placeholder: '请输入客户端编码'
    }
  },
  {
    field: 'clientName',
    label: '客户端名称',
    type: 'input',
    props: {
      placeholder: '请输入客户端名称'
    }
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: [
        { label: '全部', value: null },
        ...statusOptions
      ]
    }
  }
]

// 表格列配置
const tableColumns = [
  {
    prop: 'clientCode',
    label: '客户端编码',
    width: 120
  },
  {
    prop: 'clientName',
    label: '客户端名称',
    width: 150
  },
  {
    prop: 'appId',
    label: 'AppId',
    width: 180
  },
  {
    prop: 'tokenTimeout',
    label: 'Token有效期',
    width: 120,
    render: (row) => {
      const timeout = row.tokenTimeout
      if (timeout >= 86400) {
        return `${(timeout / 86400).toFixed(0)} 天`
      } else if (timeout >= 3600) {
        return `${(timeout / 3600).toFixed(0)} 小时`
      } else {
        return `${timeout} 秒`
      }
    }
  },
  {
    prop: 'concurrentLogin',
    label: '并发登录',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: row.concurrentLogin ? 'success' : 'warning',
        size: 'small'
      }, {
        default: () => row.concurrentLogin ? '允许' : '不允许'
      })
    }
  },
  {
    prop: 'authTypes',
    label: '支持的认证方式',
    width: 200,
    render: (row) => {
      const types = row.authTypes?.split(',') || []
      return types.map(type => 
        h(NTag, { type: 'info', size: 'small', style: 'margin: 2px' }, { default: () => type })
      )
    }
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: row.status === 1 ? 'success' : 'error',
        size: 'small'
      }, {
        default: () => row.status === 1 ? '启用' : '禁用'
      })
    }
  },
  {
    prop: 'description',
    label: '描述',
    minWidth: 150
  }
]

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
    field: 'clientCode',
    label: '客户端编码',
    type: 'input',
    rules: [{ required: true, message: '请输入客户端编码', trigger: 'blur' }],
    props: {
      placeholder: '如：pc, app, h5, wechat'
    }
  },
  {
    field: 'clientName',
    label: '客户端名称',
    type: 'input',
    rules: [{ required: true, message: '请输入客户端名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入客户端名称'
    }
  },
  {
    field: 'appId',
    label: 'AppId',
    type: 'input',
    rules: [{ required: true, message: '请输入AppId', trigger: 'blur' }],
    props: {
      placeholder: '应用ID'
    }
  },
  {
    field: 'appSecret',
    label: 'AppSecret',
    type: 'input',
    props: {
      type: 'password',
      placeholder: '应用密钥'
    }
  },
  {
    type: 'divider',
    label: 'Token配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'tokenTimeout',
    label: 'Token有效期(秒)',
    type: 'input-number',
    defaultValue: 7200,
    rules: [{ required: true, message: '请输入Token有效期' }],
    props: {
      placeholder: '如：86400(1天), 2592000(30天)',
      min: 60,
      step: 3600
    }
  },
  {
    field: 'tokenActivityTimeout',
    label: 'Token活跃超时(秒)',
    type: 'input-number',
    defaultValue: -1,
    props: {
      placeholder: '-1表示不限制'
    }
  },
  {
    field: 'concurrentLogin',
    label: '允许并发登录',
    type: 'radio',
    defaultValue: false,
    props: {
      options: booleanOptions
    }
  },
  {
    field: 'shareToken',
    label: '共享Token',
    type: 'radio',
    defaultValue: false,
    props: {
      options: booleanOptions
    }
  },
  {
    field: 'authTypes',
    label: '支持的认证方式',
    type: 'select',
    props: {
      placeholder: '请选择支持的认证方式',
      options: authTypeOptions,
      multiple: true
    }
  },
  {
    type: 'divider',
    label: '状态配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: statusOptions
    }
  },
  {
    field: 'description',
    label: '描述',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入客户端描述',
      rows: 3
    }
  }
]

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
        onClick: () => handleKickoutUser(row)
      }, {
        default: () => '踢出'
      })
    }
  }
]

// 详情数据渲染前处理：将字符串转为数组
const beforeRenderDetail = (data: any) => {
  console.log('编辑前的数据:', data)
  
  // 将authTypes字符串转为数组，供多选select使用
  if (data.authTypes && typeof data.authTypes === 'string') {
    data.authTypes = data.authTypes.split(',').filter(Boolean)
  }
  
  return data
}

// 提交前处理：将数组转为字符串
const beforeSubmit = (formData: any) => {
  console.log('提交前的数据:', formData)
  
  // 将authTypes数组转为逗号分隔的字符串
  if (Array.isArray(formData.authTypes)) {
    formData.authTypes = formData.authTypes.join(',')
  }
  
  return formData
}

// 查看在线用户
const handleViewOnline = async (row: any) => {
  try {
    const res = await request.get(`/system/client/online/${row.clientCode}`)
    if (res.data) {
      onlineUsers.value = res.data
      onlineModalVisible.value = true
    }
  } catch (error) {
    message.error('获取在线用户失败')
  }
}

// 踢出用户
const handleKickoutUser = async (row: any) => {
  try {
    await request.post(`/system/client/kickout/${row.userId}/pc`)
    message.success('已踢出该用户')
    handleViewOnline({ clientCode: 'pc' })
  } catch (error) {
    message.error('踢出失败')
  }
}

// 刷新缓存
const handleReloadCache = async (row: any) => {
  try {
    await request.post(`/system/client/reload-cache/${row.clientCode}`)
    message.success('缓存已刷新')
  } catch (error) {
    message.error('刷新缓存失败')
  }
}
</script>

<style scoped lang="scss">
.client-management-page {
  padding: 16px;
}
</style>