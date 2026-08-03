<template>
  <div class="capability-catalog-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/ai/capability/page',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :hide-add="true"
      :hide-selection="true"
      :hide-batch-delete="true"
    >
      <template #toolbar-start>
        <n-button v-if="registerTypes.length" type="primary" @click="openRegister">
          <template #icon>
            <i class="i-material-symbols:add-circle-outline-rounded" />
          </template>
          注册能力
        </n-button>
      </template>
    </AiCrudPage>

    <CapabilityRegisterModal
      v-model:show="registerVisible"
      :allowed-types="registerTypes"
      :capability="upgradeCapability"
      @success="handleRegisterSuccess"
    />

    <CapabilityCallGuideModal
      v-model:show="callGuideVisible"
      :capability="callGuideCapability"
      :can-update-grant="hasPermission('ai:capability:grant:add')"
    />

    <!-- 能力详情弹窗 -->
    <n-modal
      v-model:show="detailVisible"
      title="能力详情"
      preset="card"
      style="width: 720px"
    >
      <div v-if="currentCapability" class="capability-detail">
        <div class="detail-section">
          <h4 class="section-title">
            基本信息
          </h4>
          <div class="detail-row">
            <span class="label">能力编码：</span>
            <span class="value">{{ currentCapability.capabilityCode }}</span>
          </div>
          <div class="detail-row">
            <span class="label">能力名称：</span>
            <span class="value">{{ currentCapability.capabilityName }}</span>
          </div>
          <div class="detail-row">
            <span class="label">协议工具名：</span>
            <span class="value">{{ currentCapability.protocolToolName || '-' }}</span>
          </div>
          <div class="detail-row full">
            <span class="label">能力描述：</span>
            <span class="value">{{ currentCapability.description || '-' }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">
            来源与版本
          </h4>
          <div class="detail-row">
            <span class="label">来源类型：</span>
            <DictTag :options="sourceTypeOptions" :value="currentCapability.sourceType" size="small" />
          </div>
          <div class="detail-row">
            <span class="label">来源标识：</span>
            <span class="value">{{ currentCapability.sourceKey }}</span>
          </div>
          <div class="detail-row">
            <span class="label">来源版本：</span>
            <span class="value">{{ currentCapability.sourceVersion || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">当前版本：</span>
            <span class="value">{{ currentCapability.currentVersion || '-' }}</span>
          </div>
          <div class="detail-row full">
            <span class="label">Schema 校验和：</span>
            <span class="value checksum">{{ currentCapability.schemaChecksum || '-' }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">
            策略属性
          </h4>
          <div class="detail-row">
            <span class="label">行为类型：</span>
            <DictTag :options="behaviorOptions" :value="currentCapability.behavior || '-'" size="small" />
          </div>
          <div class="detail-row">
            <span class="label">风险等级：</span>
            <DictTag :options="riskLevelOptions" :value="currentCapability.riskLevel" size="small" />
          </div>
          <div class="detail-row">
            <span class="label">调用主体：</span>
            <DictTag :options="actorTypeOptions" :value="currentCapability.requiredActorType" size="small" />
          </div>
          <div class="detail-row">
            <span class="label">可见性：</span>
            <DictTag :options="visibilityOptions" :value="currentCapability.visibility" size="small" />
          </div>
          <div class="detail-row">
            <span class="label">发布状态：</span>
            <DictTag :options="publishStatusOptions" :value="currentCapability.publishStatus" size="small" />
          </div>
        </div>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="detailVisible = false">
            关闭
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { disableCapability, enableCapability, getCapabilityById } from '@/api/ai/capability'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables'
import { useUserStore } from '@/store'
import CapabilityCallGuideModal from './components/CapabilityCallGuideModal.vue'
import CapabilityRegisterModal from './components/CapabilityRegisterModal.vue'

defineOptions({ name: 'CapabilityCatalog' })

const userStore = useUserStore()
const { dict } = useDict(
  'ai_capability_publish_status',
  'ai_capability_risk_level',
  'ai_capability_actor_type',
  'ai_capability_visibility',
  'ai_capability_source_type',
  'ai_capability_behavior',
)

const publishStatusOptions = computed(() => dict.value.ai_capability_publish_status || [])
const riskLevelOptions = computed(() => dict.value.ai_capability_risk_level || [])
const actorTypeOptions = computed(() => dict.value.ai_capability_actor_type || [])
const visibilityOptions = computed(() => dict.value.ai_capability_visibility || [])
const sourceTypeOptions = computed(() => dict.value.ai_capability_source_type || [])
const behaviorOptions = computed(() => dict.value.ai_capability_behavior || [])

const canPublish = computed(() => {
  if (userStore?.isAdmin)
    return true
  const permissions = Array.isArray(userStore?.permissions) ? userStore.permissions : []
  return permissions.includes('ai:capability:publish')
    || permissions.includes('*:*:*')
})

const registerTypes = computed(() => {
  const types = []
  if (hasPermission('ai:capability:business-action:publish'))
    types.push('BUSINESS_ACTION')
  if (hasPermission('ai:capability:flow-action:publish'))
    types.push('FLOW_ACTION')
  if (hasPermission('ai:capability:system-service:publish'))
    types.push('SYSTEM_SERVICE')
  return types
})

function hasPermission(permission) {
  if (userStore?.isAdmin)
    return true
  const permissions = Array.isArray(userStore?.permissions) ? userStore.permissions : []
  return permissions.includes(permission) || permissions.includes('*:*:*')
}

const crudRef = ref(null)
const registerVisible = ref(false)
const upgradeCapability = ref(null)
const callGuideVisible = ref(false)
const callGuideCapability = ref(null)
const detailVisible = ref(false)
const currentCapability = ref(null)

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'keyword',
    label: '关键字',
    type: 'input',
    props: {
      placeholder: '能力编码/名称',
    },
  },
  {
    field: 'publishStatus',
    label: '发布状态',
    type: 'select',
    props: {
      placeholder: '请选择发布状态',
      clearable: true,
      options: publishStatusOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'capabilityCode',
    label: '能力编码',
    width: 200,
    ellipsis: { tooltip: true },
  },
  {
    prop: 'capabilityName',
    label: '能力名称',
    minWidth: 160,
    ellipsis: { tooltip: true },
  },
  {
    prop: 'sourceType',
    label: '来源类型',
    width: 140,
    render: (row) => {
      return h(DictTag, {
        options: sourceTypeOptions.value,
        value: row.sourceType,
        size: 'small',
      })
    },
  },
  {
    prop: 'currentVersion',
    label: '当前版本',
    width: 100,
    render: row => row.currentVersion || '-',
  },
  {
    prop: 'riskLevel',
    label: '风险等级',
    width: 100,
    render: (row) => {
      return h(DictTag, {
        options: riskLevelOptions.value,
        value: row.riskLevel,
        size: 'small',
      })
    },
  },
  {
    prop: 'requiredActorType',
    label: '调用主体',
    width: 100,
    render: (row) => {
      return h(DictTag, {
        options: actorTypeOptions.value,
        value: row.requiredActorType,
        size: 'small',
      })
    },
  },
  {
    prop: 'visibility',
    label: '可见性',
    width: 100,
    render: (row) => {
      return h(DictTag, {
        options: visibilityOptions.value,
        value: row.visibility,
        size: 'small',
      })
    },
  },
  {
    prop: 'publishStatus',
    label: '发布状态',
    width: 100,
    render: (row) => {
      return h(DictTag, {
        options: publishStatusOptions.value,
        value: row.publishStatus,
        size: 'small',
      })
    },
  },
  {
    prop: 'action',
    label: '操作',
    width: 340,
    fixed: 'right',
    actions: [
      { label: '调用与测试', key: 'call-guide', type: 'primary', onClick: handleCallGuide },
      {
        label: '发布新版本',
        key: 'publish-version',
        type: 'success',
        onClick: handlePublishVersion,
        visible: row => !!row.currentVersion && registerTypes.value.includes(row.sourceType),
      },
      { label: '详情', key: 'detail', onClick: handleViewDetail },
      {
        label: '停用',
        key: 'disable',
        type: 'error',
        onClick: handleDisable,
        visible: row => canPublish.value && row.publishStatus === 'PUBLISHED',
      },
      {
        label: '启用',
        key: 'enable',
        type: 'success',
        onClick: handleEnable,
        visible: row => canPublish.value && row.publishStatus === 'DISABLED',
      },
    ],
  },
])

function openRegister() {
  upgradeCapability.value = null
  registerVisible.value = true
}

function handlePublishVersion(row) {
  upgradeCapability.value = { ...row }
  registerVisible.value = true
}

function handleRegisterSuccess() {
  crudRef.value?.refresh()
}

function handleCallGuide(row) {
  callGuideCapability.value = { ...row }
  callGuideVisible.value = true
}

// 查看详情
async function handleViewDetail(row) {
  try {
    const res = await getCapabilityById(row.id)
    if (res.code === 200) {
      currentCapability.value = res.data
      detailVisible.value = true
    }
  }
  catch (error) {
    console.error('获取能力详情失败:', error)
  }
}

// 停用能力
function handleDisable(row) {
  window.$dialog.warning({
    title: '停用确认',
    content: `确定停用能力「${row.capabilityName}」吗？停用后开放网关将拒绝该能力的调用。`,
    positiveText: '确定停用',
    negativeText: '取消',
    onPositiveClick: async () => {
      const res = await disableCapability(row.id)
      if (res.code === 200) {
        window.$message.success('能力已停用')
        crudRef.value?.refresh()
      }
    },
  })
}

function handleEnable(row) {
  window.$dialog.success({
    title: '启用确认',
    content: `确定重新启用能力「${row.capabilityName}」吗？启用后，已有的有效授权可以继续通过开放网关调用。`,
    positiveText: '确定启用',
    negativeText: '取消',
    onPositiveClick: async () => {
      const res = await enableCapability(row.id)
      if (res.code === 200) {
        window.$message.success('能力已重新启用')
        crudRef.value?.refresh()
      }
    },
  })
}
</script>

<style scoped>
.capability-catalog-page {
  height: 100%;
}

.capability-detail {
  padding: 8px 0;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid #f0f0f0;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  line-height: 1.6;
}

.detail-row.full {
  flex-direction: column;
}

.detail-row .label {
  font-weight: 500;
  color: #666;
  min-width: 110px;
  flex-shrink: 0;
}

.detail-row .value {
  color: #262626;
  word-break: break-all;
}

.detail-row .checksum {
  font-family: 'Courier New', 'Consolas', monospace;
  font-size: 12px;
}
</style>
