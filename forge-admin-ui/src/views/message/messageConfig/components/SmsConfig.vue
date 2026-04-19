<template>
  <div class="sms-config">
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

        <n-form-item label="短信厂商" path="supplier">
          <n-select
            v-model:value="formData.supplier"
            :options="supplierOptions"
            placeholder="请选择短信厂商"
            @update:value="handleSupplierChange"
          />
        </n-form-item>

        <n-form-item label="配置标识" path="configId">
          <n-input v-model:value="formData.configId" placeholder="请输入配置标识（如：aliyun-sms）" />
        </n-form-item>

        <n-form-item label="AccessKey ID" path="accessKeyId">
          <n-input v-model:value="formData.accessKeyId" placeholder="请输入 AccessKey ID" />
        </n-form-item>

        <n-form-item label="AccessKey Secret" path="accessKeySecret">
          <n-input
            v-model:value="formData.accessKeySecret"
            type="password"
            show-password-on="click"
            placeholder="请输入 AccessKey Secret"
          />
        </n-form-item>

        <n-form-item label="短信签名" path="signature">
          <n-input v-model:value="formData.signature" placeholder="请输入短信签名" />
        </n-form-item>

        <n-form-item label="默认模板ID" path="templateId">
          <n-input v-model:value="formData.templateId" placeholder="请输入默认模板ID" />
        </n-form-item>

        <!-- 阿里云自定义配置 -->
        <template v-if="formData.supplier === 'alibaba'">
          <n-divider>阿里云自定义配置</n-divider>

          <n-form-item label="模板变量名称">
            <n-input v-model:value="extraConfig.alibaba.templateName" placeholder="模板变量名称（可选）" />
          </n-form-item>

          <n-form-item label="请求地址">
            <n-input v-model:value="extraConfig.alibaba.requestUrl" placeholder="默认: dysmsapi.aliyuncs.com" />
          </n-form-item>

          <n-form-item label="接口名称">
            <n-input v-model:value="extraConfig.alibaba.action" placeholder="默认: SendSms" />
          </n-form-item>

          <n-form-item label="接口版本号">
            <n-input v-model:value="extraConfig.alibaba.version" placeholder="默认: 2017-05-25" />
          </n-form-item>

          <n-form-item label="地域信息">
            <n-input v-model:value="extraConfig.alibaba.regionId" placeholder="默认: cn-hangzhou" />
          </n-form-item>
        </template>

        <!-- 腾讯云自定义配置 -->
        <template v-if="formData.supplier === 'tencent'">
          <n-divider>腾讯云自定义配置</n-divider>

          <n-form-item label="SDK AppId">
            <n-input v-model:value="extraConfig.tencent.sdkAppId" placeholder="请输入 SDK AppId" />
          </n-form-item>

          <n-form-item label="地域信息">
            <n-input v-model:value="extraConfig.tencent.territory" placeholder="默认: ap-guangzhou" />
          </n-form-item>

          <n-form-item label="验证码短信请求地址">
            <n-input v-model:value="extraConfig.tencent.codeUrl" placeholder="默认: https://api.netease.im/sms/sendcode.action" />
          </n-form-item>

          <n-form-item label="请求超时时间">
            <n-input-number v-model:value="extraConfig.tencent.connTimeout" :min="1" placeholder="默认: 60" style="width: 200px" />
            <span class="ml-8 text-gray-500">秒</span>
          </n-form-item>

          <n-form-item label="请求地址">
            <n-input v-model:value="extraConfig.tencent.requestUrl" placeholder="默认: sms.tencentcloudapi.com" />
          </n-form-item>

          <n-form-item label="接口名称">
            <n-input v-model:value="extraConfig.tencent.action" placeholder="默认: SendSms" />
          </n-form-item>

          <n-form-item label="接口版本">
            <n-input v-model:value="extraConfig.tencent.version" placeholder="默认: 2021-01-11" />
          </n-form-item>

          <n-form-item label="服务名">
            <n-input v-model:value="extraConfig.tencent.service" placeholder="默认: sms" />
          </n-form-item>
        </template>

        <!-- 中国移动云MAS自定义配置 -->
        <template v-if="formData.supplier === 'mas'">
          <n-divider>中国移动云MAS自定义配置</n-divider>

          <n-alert type="info" class="mb-16">
            <template #header>
              字段说明
            </template>
            <ul class="text-12">
              <li>SDK AppId = 接口账号用户名</li>
              <li>AccessKey Secret = 用户密码</li>
              <li>企业名称 = 企业名称</li>
              <li>签名编码 = 签名编码</li>
            </ul>
          </n-alert>

          <n-form-item label="企业名称">
            <n-input v-model:value="extraConfig.mas.ecName" placeholder="请输入企业名称" />
          </n-form-item>

          <n-form-item label="签名编码">
            <n-input v-model:value="extraConfig.mas.signature" placeholder="请输入签名编码" />
          </n-form-item>

          <n-form-item label="模板ID">
            <n-input v-model:value="extraConfig.mas.templateId" placeholder="有模板接口时需要配置" />
          </n-form-item>

          <n-form-item label="扩展码">
            <n-input v-model:value="extraConfig.mas.addSerial" placeholder="可为空，总长度不能超过20位" />
          </n-form-item>

          <n-form-item label="请求方法">
            <n-select
              v-model:value="extraConfig.mas.action"
              :options="masActionOptions"
              placeholder="选择请求方法"
            />
          </n-form-item>

          <n-form-item label="请求地址">
            <n-input v-model:value="extraConfig.mas.requestUrl" placeholder="HTTP模式默认: http://112.35.1.155:1992/sms/" />
          </n-form-item>
        </template>

        <n-divider>发送限制</n-divider>

        <n-form-item label="每日上限" path="dailyLimit">
          <n-input-number
            v-model:value="formData.dailyLimit"
            :min="0"
            placeholder="每日发送上限"
            style="width: 200px"
          />
          <span class="ml-8 text-gray-500">条/天（0表示不限制）</span>
        </n-form-item>

        <n-form-item label="每分钟上限" path="minuteLimit">
          <n-input-number
            v-model:value="formData.minuteLimit"
            :min="0"
            placeholder="每分钟发送上限"
            style="width: 200px"
          />
          <span class="ml-8 text-gray-500">条/分钟（0表示不限制）</span>
        </n-form-item>

        <n-form-item label="发送上限" path="maximum">
          <n-input-number
            v-model:value="formData.maximum"
            :min="0"
            placeholder="厂商发送上限"
            style="width: 200px"
          />
          <span class="ml-8 text-gray-500">条（0表示不限制）</span>
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

        <n-form-item label="权重" path="weight">
          <n-input-number
            v-model:value="formData.weight"
            :min="1"
            placeholder="权重"
            style="width: 200px"
          />
          <span class="ml-8 text-gray-500">负载均衡权重值</span>
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
  supplier: null,
  accessKeyId: null,
  accessKeySecret: null,
  signature: null,
  templateId: null,
  weight: 1,
  retryInterval: 5,
  maxRetries: 0,
  maximum: null,
  dailyLimit: null,
  minuteLimit: null,
  status: 0,
  remark: null,
  extraConfig: null,
})

const extraConfig = ref({
  alibaba: {
    templateName: null,
    requestUrl: 'dysmsapi.aliyuncs.com',
    action: 'SendSms',
    version: '2017-05-25',
    regionId: 'cn-hangzhou',
  },
  tencent: {
    sdkAppId: null,
    territory: 'ap-guangzhou',
    codeUrl: 'https://api.netease.im/sms/sendcode.action',
    connTimeout: 60,
    requestUrl: 'sms.tencentcloudapi.com',
    action: 'SendSms',
    version: '2021-01-11',
    service: 'sms',
  },
  mas: {
    ecName: null,
    signature: null,
    templateId: null,
    addSerial: null,
    action: 'tmpsubmit',
    requestUrl: 'http://112.35.1.155:1992/sms/',
  },
})

const rules = {
  supplier: { required: true, message: '请选择短信厂商', trigger: 'change' },
  configId: { required: true, message: '请输入配置标识', trigger: 'blur' },
  accessKeyId: { required: true, message: '请输入 AccessKey ID', trigger: 'blur' },
  accessKeySecret: { required: true, message: '请输入 AccessKey Secret', trigger: 'blur' },
}

const supplierOptions = ref([])

const masActionOptions = [
  { label: 'HTTP-无模板接口(norsubmit)', value: 'norsubmit' },
  { label: 'HTTP-有模板接口(tmpsubmit)', value: 'tmpsubmit' },
  { label: 'HTTPS-有模板接口(submit)', value: 'submit' },
]

async function loadSuppliers() {
  try {
    const res = await api.getSmsSuppliers()
    if (res.code === 200 && res.data) {
      supplierOptions.value = Object.entries(res.data).map(([value, label]) => ({
        label,
        value,
      }))
    }
  }
  catch (error) {
    console.error('获取短信厂商列表失败:', error)
  }
}

async function loadConfig() {
  loading.value = true
  try {
    const res = await api.getSmsConfig()
    if (res.code === 200 && res.data) {
      Object.assign(formData.value, res.data)

      if (res.data.extraConfig) {
        try {
          const extra = JSON.parse(res.data.extraConfig)
          if (extra.alibaba) {
            Object.assign(extraConfig.value.alibaba, extra.alibaba)
          }
          if (extra.tencent) {
            Object.assign(extraConfig.value.tencent, extra.tencent)
          }
          if (extra.mas) {
            Object.assign(extraConfig.value.mas, extra.mas)
          }
        }
        catch (e) {
          console.error('解析额外配置失败:', e)
        }
      }
    }
  }
  catch (error) {
    console.error('获取短信配置失败:', error)
  }
  finally {
    loading.value = false
  }
}

function handleSupplierChange() {
}

async function handleSave() {
  try {
    await formRef.value?.validate()
    saving.value = true

    const submitData = { ...formData.value }

    const currentSupplier = formData.value.supplier
    if (currentSupplier && extraConfig.value[currentSupplier]) {
      const extra = {}
      extra[currentSupplier] = extraConfig.value[currentSupplier]
      submitData.extraConfig = JSON.stringify(extra)
    }

    const res = await api.saveSmsConfig(submitData)
    if (res.code === 200) {
      window.$message.success('保存成功')
      loadConfig()
    }
    else {
      window.$message.error(res.msg || '保存失败')
    }
  }
  catch (error) {
    console.error('保存短信配置失败:', error)
  }
  finally {
    saving.value = false
  }
}

function handleReset() {
  loadConfig()
}

onMounted(() => {
  loadSuppliers()
  loadConfig()
})
</script>

<style scoped>
.sms-config {
  max-width: 800px;
  padding: 24px 0;
}

.config-form {
  padding: 16px 0;
}
</style>
