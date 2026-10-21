<template>
  <section class="publish-section-card">
    <!-- 标题：投放到 Forge 系统首页「我的应用」 -->
    <header>
      <div>
        <h2>应用分发</h2>
        <p>将已发布应用投放到系统首页「我的应用」。保存后立即生效，无需重新发布；打开应用仍受门户权限控制。</p>
      </div>
      <n-tag :type="workbenchConfigured ? 'success' : 'default'" :bordered="false">
        {{ workbenchConfigured ? '已投放' : '未投放' }}
      </n-tag>
    </header>

    <n-alert
      v-if="!application.lastPublishVersion"
      type="warning"
      :bordered="false"
      class="publish-card-alert"
    >
      应用尚未发布，发布后方可投放到首页。
    </n-alert>

    <n-form label-placement="top">
      <n-form-item label="投放目标">
        <n-radio-group v-model:value="workbench.targetType">
          <n-radio-button value="CURRENT_USER">
            仅我自己
          </n-radio-button>
          <n-radio-button value="ROLES">
            指定角色
          </n-radio-button>
        </n-radio-group>
      </n-form-item>
      <n-form-item v-if="workbench.targetType === 'ROLES'" label="可见角色">
        <n-select
          v-model:value="workbench.roleIds"
          :options="roleOptions"
          :loading="roleLoading"
          multiple
          filterable
          placeholder="选择会出现在首页「我的应用」的角色"
        />
      </n-form-item>
      <n-space justify="end">
        <n-button
          v-if="workbenchConfigured"
          :loading="saving"
          :disabled="!application.lastPublishVersion"
          @click="disableWorkbench"
        >
          <template #icon>
            <n-icon><CloseCircleOutline /></n-icon>
          </template>
          取消投放
        </n-button>
        <n-button
          type="primary"
          :loading="saving"
          :disabled="!application.lastPublishVersion"
          @click="distributeWorkbench"
        >
          <template #icon>
            <n-icon><SaveOutline /></n-icon>
          </template>
          保存投放
        </n-button>
      </n-space>
    </n-form>
  </section>
</template>

<script setup>
import { CloseCircleOutline, SaveOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { distributeBusinessApplicationToWorkbench } from '@/api/business-application'
import { request } from '@/utils/request'
import { normalizePortalConfig } from '../portal/portal-config'

const props = defineProps({ application: { type: Object, required: true } })
const emit = defineEmits(['changed'])
const message = useMessage()
const saving = ref(false)
const roleLoading = ref(false)
const roleOptions = ref([])

const distribution = computed(() => normalizePortalConfig(props.application.portalConfig).distribution || {})
const workbenchConfigured = computed(() => {
  const workbench = distribution.value.workbench
  return workbench === true || workbench?.enabled === true
})

const workbench = reactive({
  targetType: 'CURRENT_USER',
  roleIds: [],
})

onMounted(() => {
  loadRoles()
  syncFromApplication()
})

watch(() => props.application?.portalConfig, () => {
  syncFromApplication()
})

function syncFromApplication() {
  const configured = distribution.value.workbench
  workbench.targetType = configured?.targetType || 'CURRENT_USER'
  workbench.roleIds = normalizeRoleIds(configured?.roleIds || [])
}

async function loadRoles() {
  roleLoading.value = true
  try {
    const response = await request.get('/system/role/page', { params: { pageNum: 1, pageSize: 200 } })
    const records = response.data?.records || response.data?.rows || response.data || []
    roleOptions.value = (Array.isArray(records) ? records : []).map(item => ({
      label: `${item.roleName || item.roleKey || item.id}${item.roleKey ? ` · ${item.roleKey}` : ''}`,
      value: String(item.id || item.roleId),
    })).filter(item => item.value)
  }
  catch {
    roleOptions.value = []
  }
  finally {
    roleLoading.value = false
  }
}

async function distributeWorkbench() {
  if (workbench.targetType === 'ROLES' && normalizeRoleIds(workbench.roleIds).length === 0) {
    message.error('按角色投放时至少选择一个角色')
    return
  }
  await saveDistribution({
    channel: 'WORKBENCH',
    targetType: workbench.targetType,
    roleIds: normalizeRoleIds(workbench.roleIds),
    enabled: true,
  }, '已投放到系统首页「我的应用」')
}

async function disableWorkbench() {
  await saveDistribution({
    channel: 'WORKBENCH',
    targetType: workbench.targetType,
    roleIds: normalizeRoleIds(workbench.roleIds),
    enabled: false,
  }, '已取消首页投放')
}

/** 角色 ID 保持字符串，避免雪花 ID 精度丢失；提交前再转数字 */
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
