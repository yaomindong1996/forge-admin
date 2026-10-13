<template>
  <section class="publish-section-card">
    <header>
      <div><h2>应用分发</h2><p>将已发布应用添加到 Forge 工作台，或交给受管连接器同步到外部平台。</p></div>
      <n-tag :type="workbenchConfigured ? 'success' : 'default'" :bordered="false">
        {{ workbenchConfigured ? '已配置' : '未分发' }}
      </n-tag>
    </header>
    <n-tabs v-model:value="channel" type="segment" animated>
      <n-tab-pane name="WORKBENCH" tab="Forge 工作台">
        <n-form label-placement="top">
          <n-form-item label="分发目标">
            <n-radio-group v-model:value="workbench.targetType">
              <n-radio-button value="CURRENT_USER">
                添加到我的应用
              </n-radio-button>
              <n-radio-button value="ROLES">
                指定角色首页
              </n-radio-button>
            </n-radio-group>
          </n-form-item>
          <n-form-item v-if="workbench.targetType === 'ROLES'" label="角色 ID">
            <n-dynamic-tags v-model:value="workbench.roleIds" />
          </n-form-item>
          <n-space justify="end">
            <n-button
              v-if="workbenchConfigured"
              :loading="saving"
              @click="disableWorkbench"
            >
              <template #icon>
                <n-icon><CloseCircleOutline /></n-icon>
              </template>
              取消工作台分发
            </n-button>
            <n-button type="primary" :loading="saving" :disabled="!application.lastPublishVersion" @click="distributeWorkbench">
              <template #icon>
                <n-icon><SaveOutline /></n-icon>
              </template>
              保存工作台分发
            </n-button>
          </n-space>
        </n-form>
      </n-tab-pane>
      <n-tab-pane name="DINGTALK" tab="钉钉工作台">
        <n-alert type="info" :bordered="false" class="publish-card-alert">
          应用发布页不接收 AppKey、AppSecret 等明文凭证；请先在集成中心创建受管连接器。
        </n-alert>
        <n-form label-placement="top">
          <n-form-item label="同步状态">
            <n-tag :bordered="false" :type="dingtalkStatus.type">
              {{ dingtalkStatus.label }}
            </n-tag>
          </n-form-item>
          <n-form-item label="受管连接器标识">
            <n-input v-model:value="dingtalk.managedConnectorKey" placeholder="例如 dingtalk-main" />
          </n-form-item>
          <n-form-item label="分发角色 ID">
            <n-dynamic-tags v-model:value="dingtalk.roleIds" />
          </n-form-item>
          <n-space justify="end">
            <n-button type="primary" :loading="saving" :disabled="!application.lastPublishVersion" @click="prepareDingtalk">
              <template #icon>
                <n-icon><CloudUploadOutline /></n-icon>
              </template>
              保存并等待外部同步
            </n-button>
          </n-space>
        </n-form>
      </n-tab-pane>
    </n-tabs>
  </section>
</template>

<script setup>
import { CloseCircleOutline, CloudUploadOutline, SaveOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, reactive, ref } from 'vue'
import { distributeBusinessApplicationToWorkbench } from '@/api/business-application'
import { normalizePortalConfig } from '../portal/portal-config'

const props = defineProps({ application: { type: Object, required: true } })
const emit = defineEmits(['changed'])
const message = useMessage()
const saving = ref(false)
const channel = ref('WORKBENCH')
const distribution = computed(() => normalizePortalConfig(props.application.portalConfig).distribution || {})
const workbenchConfigured = computed(() => distribution.value.workbench?.enabled === true || distribution.value.workbench === true)
const dingtalkStatus = computed(() => {
  const status = distribution.value.dingtalk?.status
  if (status === 'SYNCED')
    return { label: '已同步', type: 'success' }
  if (status === 'PENDING_EXTERNAL_SYNC')
    return { label: '等待受管连接器同步', type: 'warning' }
  return { label: '未配置', type: 'default' }
})
const workbench = reactive({
  targetType: distribution.value.workbench?.targetType || 'CURRENT_USER',
  roleIds: (distribution.value.workbench?.roleIds || []).map(String),
})
const dingtalk = reactive({
  managedConnectorKey: distribution.value.dingtalk?.managedConnectorKey || '',
  roleIds: (distribution.value.dingtalk?.roleIds || []).map(String),
})

async function distributeWorkbench() {
  await saveDistribution({
    channel: 'WORKBENCH',
    targetType: workbench.targetType,
    roleIds: normalizeRoleIds(workbench.roleIds),
    enabled: true,
  }, '工作台分发配置已保存')
}

async function disableWorkbench() {
  await saveDistribution({
    channel: 'WORKBENCH',
    targetType: workbench.targetType,
    roleIds: normalizeRoleIds(workbench.roleIds),
    enabled: false,
  }, '工作台分发已取消')
}

async function prepareDingtalk() {
  const connectorKey = dingtalk.managedConnectorKey.trim()
  if (!/^[\w.-]{2,64}$/.test(connectorKey)) {
    message.error('请输入 2-64 位受管连接器标识，仅支持字母、数字、下划线、点和中划线')
    return
  }
  await saveDistribution({
    channel: 'DINGTALK',
    targetType: dingtalk.roleIds.length ? 'ROLES' : 'CURRENT_USER',
    roleIds: normalizeRoleIds(dingtalk.roleIds),
    managedConnectorKey: connectorKey,
    enabled: true,
  }, '钉钉分发已进入待同步状态')
}

function normalizeRoleIds(roleIds) {
  return [...new Set((roleIds || [])
    .map(value => String(value).trim())
    .filter(value => /^\d+$/.test(value)))]
}

async function saveDistribution(payload, successMessage) {
  saving.value = true
  try {
    await distributeBusinessApplicationToWorkbench(props.application.id, payload)
    emit('changed')
    message.success(successMessage)
  }
  catch (error) {
    message.error(error?.message || '保存分发配置失败')
  }
  finally {
    saving.value = false
  }
}
</script>
