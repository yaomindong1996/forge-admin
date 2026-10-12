<script setup>
/**
 * CarbonCopyConfig - 抄送节点配置。
 *
 * 业务入口支持指定人员、指定角色和表达式；高级实现仅作为开发者兜底。
 */
import { computed, ref, watch } from 'vue'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import { request } from '@/utils/http'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:config'])

const DOLLAR = '$'

const RECEIVER_TYPE_OPTIONS = [
  { label: '指定人员', value: 'users' },
  { label: '指定角色', value: 'roles' },
  { label: '表达式', value: 'expression' },
]

const EXPRESSION_TARGET_OPTIONS = [
  { label: '表达式返回人员', value: 'users' },
  { label: '表达式返回角色', value: 'roles' },
]

const IMPLEMENTATION_TYPE_OPTIONS = [
  { label: '平台默认抄送', value: '' },
  { label: '表达式', value: 'expression' },
  { label: '委托表达式', value: 'delegateExpression' },
  { label: 'Java 类', value: 'class' },
]

const roleOptions = ref([])
const roleLoading = ref(false)
const roleLoaded = ref(false)

const receiverType = computed({
  get: () => props.node.config?.ccReceiverType || inferReceiverType(props.node.config || {}),
  set: value => handleReceiverTypeChange(value || 'users'),
})

const candidateUsers = computed({
  get: () => normalizeValueList(props.node.config?.candidateUsers),
  set: v => emit('update:config', { candidateUsers: normalizeValueList(v) }),
})

const candidateUserNames = computed({
  get: () => normalizeValueList(props.node.config?.candidateUserNames),
  set: v => emit('update:config', { candidateUserNames: normalizeValueList(v) }),
})

const candidateGroups = computed({
  get: () => normalizeValueList(props.node.config?.candidateGroups),
  set: v => emit('update:config', { candidateGroups: normalizeValueList(v) }),
})

const candidateGroupNames = computed({
  get: () => normalizeValueList(props.node.config?.candidateGroupNames),
  set: v => emit('update:config', { candidateGroupNames: normalizeValueList(v) }),
})

const ccExpressionTarget = computed({
  get: () => props.node.config?.ccExpressionTarget || inferExpressionTarget(props.node.config || {}),
  set: v => emit('update:config', { ccExpressionTarget: v || 'users' }),
})

const ccExpression = computed({
  get: () => props.node.config?.ccExpression || inferExpression(props.node.config || {}),
  set: value => updateExpression(value),
})

const mergedRoleOptions = computed(() => {
  const currentOptions = candidateGroups.value.map((value, index) => ({
    label: candidateGroupNames.value[index] || value,
    value,
    roleName: candidateGroupNames.value[index] || value,
    roleKey: value,
  }))
  return mergeOptions(roleOptions.value, currentOptions)
})

const implementationType = computed({
  get: () => normalizeImplementationType(props.node.config || {}),
  set: v => emit('update:config', { implementationType: v || 'delegateExpression' }),
})

const implementation = computed({
  get: () => props.node.config?.implementation || '',
  set: v => emit('update:config', {
    implementation: v,
    implementationType: v ? (props.node.config?.implementationType || 'expression') : 'delegateExpression',
  }),
})

watch(receiverType, (value) => {
  if (value === 'roles')
    ensureRoleOptionsLoaded()
}, { immediate: true })

function handleReceiverTypeChange(value) {
  const patch = { ccReceiverType: value }
  if (value === 'users') {
    Object.assign(patch, {
      candidateGroups: [],
      candidateGroupNames: [],
      ccExpression: '',
      ccExpressionTarget: 'users',
    })
  }
  else if (value === 'roles') {
    Object.assign(patch, {
      candidateUsers: [],
      candidateUserNames: [],
      candidateGroups: candidateGroups.value,
      candidateGroupNames: candidateGroupNames.value,
      ccExpression: '',
      ccExpressionTarget: 'roles',
    })
    ensureRoleOptionsLoaded()
  }
  else {
    Object.assign(patch, buildExpressionPatch(ccExpression.value, ccExpressionTarget.value))
  }
  emit('update:config', patch)
}

function handleCandidateUsersSelect(users) {
  const list = (Array.isArray(users) ? users : users ? [users] : []).filter(user => isFilledValue(user?.id))
  emit('update:config', {
    ccReceiverType: 'users',
    candidateUsers: list.map(user => String(user.id)),
    candidateUserNames: list.map(resolveUserLabel).filter(Boolean),
    candidateGroups: [],
    candidateGroupNames: [],
    ccExpression: '',
    ccExpressionTarget: 'users',
  })
}

function handleCandidateGroupsChange(values, selectedOptions = []) {
  const nextValues = normalizeValueList(values)
  const selectedOptionList = Array.isArray(selectedOptions) ? selectedOptions : selectedOptions ? [selectedOptions] : []
  const optionMap = new Map(mergedRoleOptions.value.map(option => [String(option.value), option]))
  const selectedMap = new Map(selectedOptionList.map(option => [String(option.value), option]))
  emit('update:config', {
    ccReceiverType: 'roles',
    candidateGroups: nextValues,
    candidateGroupNames: nextValues.map((value) => {
      const option = selectedMap.get(String(value)) || optionMap.get(String(value))
      return option?.roleName || option?.label || value
    }),
    candidateUsers: [],
    candidateUserNames: [],
    ccExpression: '',
    ccExpressionTarget: 'roles',
  })
}

function updateExpression(value) {
  emit('update:config', buildExpressionPatch(value, ccExpressionTarget.value))
}

function handleExpressionTargetChange(value) {
  emit('update:config', {
    ccExpressionTarget: value || 'users',
    ...buildExpressionPatch(ccExpression.value, value || 'users'),
  })
}

function buildExpressionPatch(value, target) {
  const expression = normalizeExpression(value)
  const label = expression ? ['表达式配置'] : []
  return {
    ccReceiverType: 'expression',
    ccExpression: expression,
    ccExpressionTarget: target || 'users',
    candidateUsers: target === 'roles' || !expression ? [] : [expression],
    candidateUserNames: target === 'roles' || !expression ? [] : label,
    candidateGroups: target === 'roles' && expression ? [expression] : [],
    candidateGroupNames: target === 'roles' && expression ? label : [],
  }
}

async function ensureRoleOptionsLoaded() {
  if (roleLoaded.value || roleLoading.value)
    return
  await loadRoleOptions()
}

async function loadRoleOptions(keyword = '') {
  roleLoading.value = true
  try {
    const res = await request.get('/system/role/page', {
      params: {
        pageNum: 1,
        pageSize: 50,
        roleName: keyword || undefined,
      },
    })
    roleOptions.value = resolveRecords(res.data).map(normalizeRoleOption).filter(Boolean)
    roleLoaded.value = true
  }
  catch (error) {
    console.error('加载角色列表失败', error)
  }
  finally {
    roleLoading.value = false
  }
}

function normalizeRoleOption(role) {
  const value = isFilledValue(role?.roleKey) ? role.roleKey : role?.id
  if (!isFilledValue(value))
    return null
  const label = String(role.roleName || role.roleKey || value)
  return {
    label,
    value: String(value),
    roleName: label,
    roleKey: String(value),
  }
}

function resolveRecords(data) {
  if (Array.isArray(data))
    return data
  return data?.records || data?.list || []
}

function inferReceiverType(config = {}) {
  if (config.ccExpression || hasExpressionValue(config.candidateUsers) || hasExpressionValue(config.candidateGroups))
    return 'expression'
  if (normalizeValueList(config.candidateGroups).length)
    return 'roles'
  return 'users'
}

function inferExpressionTarget(config = {}) {
  if (hasExpressionValue(config.candidateGroups))
    return 'roles'
  return config.ccExpressionTarget || 'users'
}

function inferExpression(config = {}) {
  if (isFilledValue(config.ccExpression))
    return normalizeExpression(config.ccExpression)
  return findExpressionValue(config.candidateUsers) || findExpressionValue(config.candidateGroups) || ''
}

function hasExpressionValue(value) {
  return Boolean(findExpressionValue(value))
}

function findExpressionValue(value) {
  return normalizeValueList(value).find(item => item.startsWith(`${DOLLAR}{`) && item.endsWith('}')) || ''
}

function normalizeExpression(value) {
  const text = String(value || '').trim()
  if (!text)
    return ''
  if (text.startsWith(`${DOLLAR}{`) && text.endsWith('}'))
    return text
  return `${DOLLAR}{${text}}`
}

function normalizeValueList(value) {
  if (Array.isArray(value))
    return value.map(item => String(item ?? '').trim()).filter(Boolean)
  if (!isFilledValue(value))
    return []
  return String(value).split(/[,，\s]+/).map(item => item.trim()).filter(Boolean)
}

function mergeOptions(primary = [], append = []) {
  const map = new Map()
  for (const option of [...append, ...primary]) {
    if (!option || !isFilledValue(option.value))
      continue
    map.set(String(option.value), option)
  }
  return Array.from(map.values())
}

function normalizeImplementationType(config = {}) {
  const type = config.implementationType || ''
  if (!config.implementation && type === 'delegateExpression')
    return ''
  return type
}

function resolveUserLabel(user) {
  return String(user?.realName || user?.name || user?.nickname || user?.username || '').trim()
}

function isFilledValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== ''
}
</script>

<template>
  <div class="space-y-3">
    <n-form-item label="抄送来源" label-placement="top" required :show-feedback="false">
      <div class="carbon-copy-source-switch">
        <button
          v-for="item in RECEIVER_TYPE_OPTIONS"
          :key="item.value"
          type="button"
          class="carbon-copy-source-button"
          :class="{ active: receiverType === item.value }"
          :disabled="readonly"
          @click="handleReceiverTypeChange(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </n-form-item>

    <n-form-item v-if="receiverType === 'users'" label="抄送人" label-placement="top" required :show-feedback="false">
      <UserSelectPicker
        :model-value="candidateUsers"
        :label-value="candidateUserNames"
        placeholder="请选择抄送人"
        title="选择抄送人"
        multiple
        :disabled="readonly"
        @update:model-value="candidateUsers = $event"
        @update:label-value="candidateUserNames = $event"
        @select="handleCandidateUsersSelect"
      />
    </n-form-item>

    <n-form-item v-else-if="receiverType === 'roles'" label="抄送角色" label-placement="top" required :show-feedback="false">
      <n-select
        :value="candidateGroups"
        :options="mergedRoleOptions"
        :loading="roleLoading"
        :disabled="readonly"
        placeholder="请选择角色"
        multiple
        clearable
        filterable
        remote
        @focus="ensureRoleOptionsLoaded"
        @search="loadRoleOptions"
        @update:value="handleCandidateGroupsChange"
      />
    </n-form-item>

    <template v-else>
      <n-form-item label="表达式返回内容" label-placement="top" required :show-feedback="false">
        <n-select
          :value="ccExpressionTarget"
          :options="EXPRESSION_TARGET_OPTIONS"
          :disabled="readonly"
          @update:value="handleExpressionTargetChange"
        />
      </n-form-item>
      <n-form-item label="抄送表达式" label-placement="top" required :show-feedback="false">
        <n-input
          v-model:value="ccExpression"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 5 }"
          :disabled="readonly"
          placeholder="${ccUserIds} 或 ${flowSpelService.findUsersByRole('general_manager')}"
        />
      </n-form-item>
      <div class="text-xs rounded bg-blue-50 px-3 py-2 text-blue-600">
        表达式可返回单个 ID、逗号分隔字符串或数组；选择“返回角色”时系统会按角色编码解析抄送人。
      </div>
    </template>

    <n-collapse class="carbon-copy-advanced-collapse">
      <n-collapse-item title="开发者高级配置（可选）" name="advanced">
        <n-space vertical size="small">
          <div class="carbon-copy-advanced-row">
            <n-select
              v-model:value="implementationType"
              :options="IMPLEMENTATION_TYPE_OPTIONS"
              :disabled="readonly"
              placeholder="平台默认抄送"
            />
            <n-input
              v-if="implementationType"
              v-model:value="implementation"
              :disabled="readonly"
              placeholder="请输入表达式、委托表达式或 Java 类名"
            />
          </div>
          <div v-if="!implementationType" class="carbon-copy-advanced-tip">
            默认使用平台内置抄送服务，通常不需要修改。
          </div>
        </n-space>
      </n-collapse-item>
    </n-collapse>

    <div class="text-xs rounded bg-gray-50 px-3 py-2 text-gray-500">
      流程到达该节点时会发送抄送消息，抄送只通知，不需要审批，流程会立即流转到下一节点。
    </div>
  </div>
</template>

<style scoped>
.carbon-copy-source-switch {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border: 1px solid #d8dee8;
  border-radius: 6px;
  overflow: hidden;
  background: var(--bg-primary);
}

.carbon-copy-source-button {
  min-width: 0;
  height: 34px;
  border: 0;
  border-right: 1px solid #d8dee8;
  background: transparent;
  color: #344054;
  font-size: 13px;
  line-height: 34px;
  text-align: center;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background-color 0.16s ease,
    box-shadow 0.16s ease;
}

.carbon-copy-source-button:last-child {
  border-right: 0;
}

.carbon-copy-source-button.active {
  background: #eef4ff;
  color: #2563eb;
  box-shadow: inset 0 -2px 0 #2563eb;
  font-weight: 600;
}

.carbon-copy-source-button:disabled {
  cursor: not-allowed;
  color: #98a2b3;
}

.carbon-copy-advanced-collapse {
  margin-top: 2px;
}

.carbon-copy-advanced-collapse :deep(.n-collapse-item__header) {
  font-size: 13px;
  color: var(--text-secondary);
}

.carbon-copy-advanced-row {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.carbon-copy-advanced-tip {
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}
</style>
