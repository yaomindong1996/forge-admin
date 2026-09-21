<template>
  <n-modal
    v-model:show="testVisible"
    title="连通测试"
    preset="card"
    style="width: 420px"
    :mask-closable="false"
  >
    <n-form label-placement="left" label-width="100px">
      <n-form-item label="测试能力">
        <n-select v-model:value="testCapability" :options="capabilityOptions" />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="testVisible = false">
          取消
        </n-button>
        <n-button type="primary" :loading="testLoading" @click="handleSubmitTest">
          开始测试
        </n-button>
      </n-space>
    </template>
  </n-modal>

  <n-modal
    v-model:show="messageVisible"
    title="消息测试"
    preset="card"
    style="width: 560px"
    :mask-closable="false"
  >
    <n-form
      ref="msgTestFormRef"
      :model="msgTestForm"
      :rules="msgTestRules"
      label-placement="left"
      label-width="80px"
    >
      <n-form-item label="接收人" path="userIds">
        <UserSelectPicker
          v-model:model-value="msgTestForm.userIds"
          v-model:label-value="msgTestForm.userLabels"
          multiple
          placeholder="请选择测试接收人（最多10人）"
          title="选择测试接收人"
        />
      </n-form-item>
      <n-form-item label="标题" path="title">
        <n-input v-model:value="msgTestForm.title" placeholder="消息标题，可空" />
      </n-form-item>
      <n-form-item label="正文" path="content">
        <n-input v-model:value="msgTestForm.content" type="textarea" :rows="3" placeholder="请输入消息正文" />
      </n-form-item>
    </n-form>
    <n-alert type="info" :show-icon="false" size="small" class="message-tip">
      接收人必须已同步或绑定该连接的外部账号，且在平台应用可见范围内，否则会被平台标记无效。
    </n-alert>
    <n-data-table
      v-if="msgTestResult"
      :columns="msgTestResultColumns"
      :data="msgTestResult.deliveries || []"
      :bordered="true"
      size="small"
    />
    <template #footer>
      <n-space justify="end">
        <n-button @click="messageVisible = false">
          关闭
        </n-button>
        <n-button type="primary" :loading="msgTestLoading" @click="handleSubmitMsgTest">
          发送
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, ref, watch } from 'vue'
import { sendTestMessage, testConnection } from '@/api/collaboration'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'

const props = defineProps({
  testVisible: Boolean,
  messageVisible: Boolean,
  testConnectionId: [Number, String],
  messageConnectionId: [Number, String],
  capabilityOptions: {
    type: Array,
    default: () => [],
  },
})
const emit = defineEmits(['update:testVisible', 'update:messageVisible'])

const testVisible = computed({
  get: () => props.testVisible,
  set: value => emit('update:testVisible', value),
})
const messageVisible = computed({
  get: () => props.messageVisible,
  set: value => emit('update:messageVisible', value),
})
const testLoading = ref(false)
const testCapability = ref('MESSAGE')
const msgTestLoading = ref(false)
const msgTestFormRef = ref(null)
const msgTestForm = ref({ connectionId: null, userIds: [], userLabels: [], title: '', content: '' })
const msgTestResult = ref(null)

const msgTestRules = {
  userIds: [{
    validator: () => {
      const ids = msgTestForm.value.userIds || []
      if (!ids.length)
        return new Error('请选择测试接收人')
      if (ids.length > 10)
        return new Error('测试接收人不能超过10人')
      return true
    },
    trigger: 'change',
  }],
  content: [{ required: true, message: '请输入消息正文', trigger: 'blur' }],
}

const msgTestResultColumns = [
  { title: '用户 ID', key: 'userId', width: 90 },
  {
    title: '投递状态',
    key: 'status',
    width: 90,
    render: (row) => {
      const type = row.status === 'SENT' ? 'success' : row.status === 'SKIPPED' ? 'warning' : 'error'
      return h(NTag, { type, size: 'small' }, () => row.status)
    },
  },
  {
    title: '失败原因',
    key: 'errorMessage',
    render: row => row.errorMessage || (row.status === 'SENT' ? '-' : row.errorCode || '-'),
  },
]

watch(() => props.testVisible, (visible) => {
  if (visible)
    testCapability.value = props.capabilityOptions.some(item => item.value === 'MESSAGE') ? 'MESSAGE' : props.capabilityOptions[0]?.value
})

watch(() => props.messageVisible, (visible) => {
  if (visible) {
    msgTestForm.value = {
      connectionId: props.messageConnectionId,
      userIds: [],
      userLabels: [],
      title: '协同消息推送测试',
      content: '',
    }
    msgTestResult.value = null
  }
})

async function handleSubmitTest() {
  if (!props.testConnectionId || !testCapability.value)
    return
  testLoading.value = true
  try {
    const res = await testConnection(props.testConnectionId, testCapability.value)
    if (res.code === 200) {
      window.$message.success(res.data || '连通测试通过')
      testVisible.value = false
    }
  }
  catch {
    window.$message.error('连通测试失败，请检查凭据与能力绑定')
  }
  finally {
    testLoading.value = false
  }
}

async function handleSubmitMsgTest() {
  try {
    await msgTestFormRef.value?.validate()
  }
  catch {
    return
  }
  msgTestLoading.value = true
  msgTestResult.value = null
  try {
    const { connectionId, userIds, title, content } = msgTestForm.value
    const res = await sendTestMessage({ connectionId, userIds, title, content })
    if (res.code === 200) {
      msgTestResult.value = res.data
      const deliveries = res.data?.deliveries || []
      const sent = deliveries.filter(item => item.status === 'SENT').length
      if (sent === deliveries.length && sent > 0)
        window.$message.success('全部发送成功，请在平台客户端查看')
      else
        window.$message.warning(`发送完成：成功 ${sent}/${deliveries.length}，失败原因见下方明细`)
    }
  }
  catch {
    window.$message.error('消息测试发送失败')
  }
  finally {
    msgTestLoading.value = false
  }
}
</script>

<style scoped>
.message-tip {
  margin-bottom: 12px;
}
</style>
