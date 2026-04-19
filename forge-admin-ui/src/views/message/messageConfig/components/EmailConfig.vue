<template>
  <div class="email-config">
    <n-spin :show="loading">
      <n-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="left"
        label-width="120px"
        class="config-form"
      >
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>
              启用
            </template>
            <template #unchecked>
              禁用
            </template>
          </n-switch>
        </n-form-item>

        <n-form-item label="配置标识" path="configId">
          <n-input v-model:value="formData.configId" placeholder="请输入配置标识（如：company-email）" />
        </n-form-item>

        <n-form-item label="SMTP服务器" path="smtpServer">
          <n-input v-model:value="formData.smtpServer" placeholder="如：smtp.qq.com" />
        </n-form-item>

        <n-form-item label="端口" path="port">
          <n-input-number
            v-model:value="formData.port"
            :min="1"
            :max="65535"
            placeholder="端口"
            style="width: 200px"
          />
          <span class="ml-8 text-gray-500">常用：25(明文)、465(SSL)、587(TLS)</span>
        </n-form-item>

        <n-form-item label="用户名" path="username">
          <n-input v-model:value="formData.username" placeholder="邮箱账号" />
        </n-form-item>

        <n-form-item label="密码/授权码" path="password">
          <n-input
            v-model:value="formData.password"
            type="password"
            show-password-on="click"
            placeholder="QQ/网易等邮箱请使用授权码"
          />
        </n-form-item>

        <n-form-item label="发件人地址" path="fromAddress">
          <n-input v-model:value="formData.fromAddress" placeholder="发件人邮箱地址" />
        </n-form-item>

        <n-form-item label="发件人名称" path="fromName">
          <n-input v-model:value="formData.fromName" placeholder="发件人显示名称（可选）" />
        </n-form-item>

        <n-divider>安全设置</n-divider>

        <n-form-item label="开启SSL" path="isSsl">
          <n-switch v-model:value="formData.isSsl">
            <template #checked>
              是
            </template>
            <template #unchecked>
              否
            </template>
          </n-switch>
          <span class="ml-8 text-gray-500">QQ、网易等邮箱通常需要开启SSL</span>
        </n-form-item>

        <n-form-item label="开启验证" path="isAuth">
          <n-switch v-model:value="formData.isAuth">
            <template #checked>
              是
            </template>
            <template #unchecked>
              否
            </template>
          </n-switch>
        </n-form-item>

        <n-divider>重试配置</n-divider>

        <n-form-item label="重试间隔" path="retryInterval">
          <n-input-number
            v-model:value="formData.retryInterval"
            :min="1"
            placeholder="重试间隔"
            style="width: 200px"
          />
          <span class="ml-8 text-gray-500">秒</span>
        </n-form-item>

        <n-form-item label="最大重试次数" path="maxRetries">
          <n-input-number
            v-model:value="formData.maxRetries"
            :min="0"
            placeholder="最大重试次数"
            style="width: 200px"
          />
          <span class="ml-8 text-gray-500">次（0表示不重试）</span>
        </n-form-item>

        <n-form-item label="备注" path="remark">
          <n-input
            v-model:value="formData.remark"
            type="textarea"
            placeholder="请输入备注"
            :rows="3"
          />
        </n-form-item>

        <n-form-item>
          <n-space>
            <n-button type="primary" :loading="saving" @click="handleSave">
              保存配置
            </n-button>
            <n-button @click="handleReset">
              重置
            </n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-spin>
  </div>
</template>

<script setup>
import api from '../api.js'

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)

const formData = ref({
  id: null,
  configId: null,
  smtpServer: null,
  port: 465,
  username: null,
  password: null,
  fromAddress: null,
  fromName: null,
  isSsl: true,
  isAuth: true,
  retryInterval: 5,
  maxRetries: 1,
  status: 0,
  remark: null,
})

const rules = {
  smtpServer: { required: true, message: '请输入 SMTP 服务器地址', trigger: 'blur' },
  port: { required: true, type: 'number', message: '请输入端口', trigger: 'blur' },
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码/授权码', trigger: 'blur' },
  fromAddress: { required: true, message: '请输入发件人地址', trigger: 'blur' },
}

async function loadConfig() {
  loading.value = true
  try {
    const res = await api.getEmailConfig()
    if (res.code === 200 && res.data) {
      Object.assign(formData.value, res.data)
    }
  }
  catch (error) {
    console.error('获取邮件配置失败:', error)
  }
  finally {
    loading.value = false
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
    saving.value = true
    const res = await api.saveEmailConfig(formData.value)
    if (res.code === 200) {
      window.$message.success('保存成功')
      loadConfig()
    }
    else {
      window.$message.error(res.msg || '保存失败')
    }
  }
  catch (error) {
    console.error('保存邮件配置失败:', error)
  }
  finally {
    saving.value = false
  }
}

function handleReset() {
  loadConfig()
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.email-config {
  max-width: 800px;
  padding: 24px 0;
}

.config-form {
  padding: 16px 0;
}
</style>
