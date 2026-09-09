<script setup>
/**
 * ApproverAssigneeForm — 审批人选择表单（assignee Tab）
 *
 * 字段：
 *   - taskType: assignee / candidateUsers / candidateGroups
 *   - assignee（taskType=assignee 下）：发起人自选 / 4 种静态变量 / custom / spel
 *   - candidateUsers / candidateGroups + 对应名称列表
 *   - 指定人员保存固定字符串用户 ID；SPEL 模板单独维护 assigneeExpr
 */
import { computed, ref, watch } from 'vue'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import { request } from '@/utils/http'

const props = defineProps({
  config: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])

const TASK_TYPE_OPTIONS = [
  { label: '人工审批', value: 'assignee', icon: 'i-lucide:user' },
  { label: '候选人员', value: 'candidateUsers', icon: 'i-lucide:users' },
  { label: '候选角色', value: 'candidateGroups', icon: 'i-lucide:shield' },
]

const DOLLAR = '$'

const ASSIGNEE_OPTIONS = [
  { label: '发起人自选', value: 'initiatorSelect', icon: 'i-lucide:user-cog' },
  { label: '发起人', value: `${DOLLAR}{initiator}`, icon: 'i-lucide:user-circle' },
  { label: '上级领导', value: `${DOLLAR}{initiatorLeader}`, icon: 'i-lucide:building-2' },
  { label: '部门主管', value: `${DOLLAR}{deptManager}`, icon: 'i-lucide:building-2' },
  { label: 'HR', value: `${DOLLAR}{hr}`, icon: 'i-lucide:shield' },
  { label: '指定人员', value: 'custom', icon: 'i-lucide:user-plus' },
  { label: 'SPEL 模板', value: 'spel', icon: 'i-lucide:braces' },
]

const taskType = useField('taskType', 'assignee')
const assignee = useField('assignee', '')
const assigneeUserId = useField('assigneeUserId', '')
const assigneeExpr = useField('assigneeExpr', '')
const assigneeUserName = useField('assigneeUserName', '')
const spelTemplate = useField('spelTemplate', '')
const roleOptions = ref([])
const roleLoading = ref(false)
const roleLoaded = ref(false)
const flowGroupOptions = ref([])
const flowGroupLoading = ref(false)
const flowGroupLoaded = ref(false)
const spelTemplateOptions = ref([])
const spelLoading = ref(false)
const spelLoaded = ref(false)

const selectedAssigneeUserId = computed({
  get: () => normalizeUserId(assigneeUserId.value) || extractLegacyUserId(assigneeExpr.value),
  set: (value) => {
    const userId = normalizeUserId(value)
    emit('update:config', {
      assigneeUserId: userId,
      assigneeExpr: '',
      assigneeUserName: userId ? assigneeUserName.value : '',
    })
  },
})
const candidateUsers = computed({
  get: () => normalizeValueList(props.config.candidateUsers),
  set: v => emit('update:config', { candidateUsers: normalizeValueList(v) }),
})
const candidateUserNames = computed({
  get: () => normalizeValueList(props.config.candidateUserNames),
  set: v => emit('update:config', { candidateUserNames: normalizeValueList(v) }),
})
const candidateGroups = computed({
  get: () => normalizeValueList(props.config.candidateGroups),
  set: v => emit('update:config', { candidateGroups: normalizeValueList(v) }),
})
const candidateGroupNames = computed({
  get: () => normalizeValueList(props.config.candidateGroupNames),
  set: v => emit('update:config', { candidateGroupNames: normalizeValueList(v) }),
})
const mergedRoleOptions = computed(() => {
  const currentOptions = candidateGroups.value.map((value, index) => ({
    label: candidateGroupNames.value[index] || value,
    value,
    roleName: candidateGroupNames.value[index] || value,
    roleKey: value,
    kind: 'current',
  }))
  return mergeOptions([...flowGroupOptions.value, ...roleOptions.value], currentOptions)
})
const mergedSpelTemplateOptions = computed(() => {
  const currentCode = spelTemplate.value
  const currentExpression = assigneeExpr.value
  const currentOption = currentCode
    ? [{
        label: findSpelOption(currentCode)?.label || currentCode,
        value: currentCode,
        expression: currentExpression,
      }]
    : []
  return mergeOptions(spelTemplateOptions.value, currentOption)
})

function useField(name, fallback = '') {
  return computed({
    get: () => props.config?.[name] ?? fallback,
    set: v => emit('update:config', { [name]: v }),
  })
}

const isCustomAssignee = computed(() => assignee.value === 'custom')
const isSpelAssignee = computed(() => assignee.value === 'spel')
const isInitiatorSelect = computed(() => assignee.value === 'initiatorSelect')

watch(taskType, (value) => {
  if (value === 'candidateGroups')
    ensureRoleOptionsLoaded()
}, { immediate: true })

watch(assignee, (value) => {
  if (value === 'spel')
    ensureSpelTemplatesLoaded()
}, { immediate: true })

function handleAssigneeChange(value) {
  const patch = { assignee: value }
  if (value === 'initiatorSelect') {
    emit('update:config', {
      assignee: 'initiatorSelect',
      assigneeUserId: '',
      assigneeExpr: '',
      assigneeUserName: '',
      multiInstanceCollection: '',
      multiInstanceElementVariable: 'assignee',
    })
    return
  }
  else if (value === 'custom') {
    patch.spelTemplate = ''
    const legacyUserId = extractLegacyUserId(assigneeExpr.value)
    if (legacyUserId) {
      patch.assigneeUserId = legacyUserId
    }
    else if (!normalizeUserId(assigneeUserId.value)) {
      patch.assigneeUserId = ''
      patch.assigneeExpr = ''
      patch.assigneeUserName = ''
    }
    patch.assigneeExpr = ''
  }
  else if (value === 'spel') {
    patch.assigneeUserId = ''
    patch.assigneeUserName = ''
    if (extractLegacyUserId(assigneeExpr.value))
      patch.assigneeExpr = ''
    ensureSpelTemplatesLoaded()
  }
  else {
    patch.assigneeUserId = ''
    patch.assigneeExpr = ''
    patch.assigneeUserName = ''
    patch.spelTemplate = ''
  }
  emit('update:config', patch)
}

function handleAssigneeUserSelect(user) {
  if (!user || !isFilledValue(user.id)) {
    emit('update:config', { assigneeUserId: '', assigneeExpr: '', assigneeUserName: '' })
    return
  }
  const userId = String(user.id)
  emit('update:config', {
    assignee: 'custom',
    assigneeUserId: userId,
    assigneeExpr: '',
    assigneeUserName: resolveUserLabel(user),
  })
}

function handleCandidateUsersSelect(users) {
  const list = (Array.isArray(users) ? users : users ? [users] : []).filter(user => isFilledValue(user?.id))
  emit('update:config', {
    candidateUsers: list.map(user => String(user.id)),
    candidateUserNames: list.map(resolveUserLabel).filter(Boolean),
  })
}

function handleCandidateGroupsChange(values, selectedOptions = []) {
  const nextValues = normalizeValueList(values)
  const selectedOptionList = Array.isArray(selectedOptions) ? selectedOptions : selectedOptions ? [selectedOptions] : []
  const optionMap = new Map(mergedRoleOptions.value.map(option => [String(option.value), option]))
  const selectedMap = new Map(selectedOptionList.map(option => [String(option.value), option]))
  emit('update:config', {
    candidateGroups: nextValues,
    candidateGroupNames: nextValues.map((value) => {
      const option = selectedMap.get(String(value)) || optionMap.get(String(value))
      return option?.roleName || option?.label || value
    }),
  })
}

function handleSpelTemplateChange(value, selectedOption) {
  const option = selectedOption || findSpelOption(value)
  emit('update:config', {
    spelTemplate: value || '',
    assigneeExpr: option?.expression || '',
  })
}

async function ensureRoleOptionsLoaded() {
  if ((roleLoaded.value && flowGroupLoaded.value) || roleLoading.value || flowGroupLoading.value)
    return
  await Promise.all([loadRoleOptions(), loadFlowGroupOptions()])
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
    const records = resolveRecords(res.data)
    roleOptions.value = records.map(normalizeRoleOption).filter(Boolean)
    roleLoaded.value = true
  }
  catch (error) {
    console.error('加载角色列表失败', error)
  }
  finally {
    roleLoading.value = false
  }
}

async function loadFlowGroupOptions(keyword = '') {
  flowGroupLoading.value = true
  try {
    const res = await request.get('/api/flow/org/groups/page', {
      params: {
        pageNum: 1,
        pageSize: 50,
        status: 1,
        keyword: keyword || undefined,
      },
    })
    const records = resolveRecords(res.data)
    flowGroupOptions.value = records.map(normalizeFlowGroupOption).filter(Boolean)
    flowGroupLoaded.value = true
  }
  catch (error) {
    // 用户组属于可选治理能力；接口未部署或无权限时仍保留角色候选配置。
    console.warn('加载流程用户组失败，继续使用角色候选组', error)
  }
  finally {
    flowGroupLoading.value = false
  }
}

async function handleCandidateGroupSearch(keyword) {
  await Promise.all([
    loadRoleOptions(keyword),
    loadFlowGroupOptions(keyword),
  ])
}

async function ensureSpelTemplatesLoaded() {
  if (spelLoaded.value || spelLoading.value)
    return
  await loadSpelTemplates()
}

async function loadSpelTemplates() {
  spelLoading.value = true
  try {
    const res = await request.get('/api/flow/spelTemplate/list')
    const records = resolveRecords(res.data)
    spelTemplateOptions.value = records.map(normalizeSpelOption).filter(Boolean)
    spelLoaded.value = true
  }
  catch (error) {
    console.error('加载SPEL模板失败', error)
  }
  finally {
    spelLoading.value = false
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
    kind: 'role',
  }
}

function normalizeFlowGroupOption(group) {
  const value = group?.groupCode
  if (!isFilledValue(value))
    return null
  const groupName = String(group.groupName || value)
  return {
    label: `用户组 · ${groupName}`,
    value: String(value),
    roleName: groupName,
    roleKey: String(value),
    kind: 'flow-group',
  }
}

function normalizeSpelOption(template) {
  const value = isFilledValue(template?.templateCode) ? template.templateCode : template?.expression
  if (!isFilledValue(value))
    return null
  return {
    label: String(template.templateName || template.templateCode || value),
    value: String(value),
    expression: template.expression || '',
    templateCode: template.templateCode || '',
  }
}

function findSpelOption(value) {
  if (!isFilledValue(value))
    return null
  return spelTemplateOptions.value.find(option => String(option.value) === String(value)) || null
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

function resolveRecords(data) {
  if (Array.isArray(data))
    return data
  return data?.records || data?.list || []
}

function normalizeValueList(value) {
  if (Array.isArray(value))
    return value.map(item => String(item ?? '').trim()).filter(Boolean)
  if (!isFilledValue(value))
    return []
  return String(value).split(/[,，\s]+/).map(item => item.trim()).filter(Boolean)
}

function extractLegacyUserId(expression) {
  const match = String(expression || '').match(/^\$\{user_(\d+)\}$/)
  return match?.[1] || ''
}

function normalizeUserId(value) {
  const userId = String(value ?? '').trim()
  return /^\d+$/.test(userId) ? userId : ''
}

function resolveUserLabel(user) {
  return String(user?.realName || user?.name || user?.nickname || user?.username || '').trim()
}

function isFilledValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== ''
}
</script>

<template>
  <div class="approver-assignee-form">
    <n-form-item label="审批类型" label-placement="top" required :show-feedback="false">
      <div class="option-card-grid is-three">
        <button
          v-for="option in TASK_TYPE_OPTIONS"
          :key="option.value"
          type="button"
          class="option-card"
          :class="{ active: taskType === option.value }"
          :disabled="readonly"
          @click="taskType = option.value"
        >
          <i :class="option.icon" />
          <span>{{ option.label }}</span>
        </button>
      </div>
    </n-form-item>

    <template v-if="taskType === 'assignee'">
      <n-form-item label="审批人" label-placement="top" required :show-feedback="false">
        <div class="option-card-grid">
          <button
            v-for="option in ASSIGNEE_OPTIONS"
            :key="option.value"
            type="button"
            class="option-card"
            :class="{ active: assignee === option.value }"
            :disabled="readonly"
            @click="handleAssigneeChange(option.value)"
          >
            <i :class="option.icon" />
            <span>{{ option.label }}</span>
          </button>
        </div>
      </n-form-item>

      <n-form-item v-if="isCustomAssignee" label="指定人员" label-placement="top" required :show-feedback="false">
        <UserSelectPicker
          :model-value="selectedAssigneeUserId"
          :label-value="assigneeUserName"
          placeholder="请选择人员"
          title="选择审批人员"
          :disabled="readonly"
          @update:model-value="selectedAssigneeUserId = $event"
          @update:label-value="assigneeUserName = $event"
          @select="handleAssigneeUserSelect"
        />
      </n-form-item>

      <n-form-item v-if="isSpelAssignee" label="SPEL 模板" label-placement="top" required :show-feedback="false">
        <n-select
          :value="spelTemplate"
          :options="mergedSpelTemplateOptions"
          :loading="spelLoading"
          :disabled="readonly"
          placeholder="请选择SPEL模板"
          clearable
          filterable
          @focus="ensureSpelTemplatesLoaded"
          @update:value="handleSpelTemplateChange"
        />
      </n-form-item>
      <n-alert v-if="isInitiatorSelect" type="info" :show-icon="false">
        与「发起人」不同：本节点审批人在发起时由申请人选择，不是发起人自己审批。多人审批方式为「不会签」时只选一人；选择会签后可同时选择多人。
      </n-alert>
    </template>

    <template v-else-if="taskType === 'candidateUsers'">
      <n-form-item label="候选人员" label-placement="top" required :show-feedback="false">
        <UserSelectPicker
          :model-value="candidateUsers"
          :label-value="candidateUserNames"
          placeholder="请选择候选人员"
          title="选择候选人员"
          multiple
          :disabled="readonly"
          @update:model-value="candidateUsers = $event"
          @update:label-value="candidateUserNames = $event"
          @select="handleCandidateUsersSelect"
        />
      </n-form-item>
    </template>

    <template v-else-if="taskType === 'candidateGroups'">
      <n-form-item label="候选角色或用户组" label-placement="top" required :show-feedback="false">
        <n-select
          :value="candidateGroups"
          :options="mergedRoleOptions"
          :loading="roleLoading || flowGroupLoading"
          :disabled="readonly"
          placeholder="请选择角色或流程用户组"
          multiple
          clearable
          filterable
          remote
          @focus="ensureRoleOptionsLoaded"
          @search="handleCandidateGroupSearch"
          @update:value="handleCandidateGroupsChange"
        />
        <n-text depth="3" class="candidate-group-hint">
          角色使用系统角色编码；流程用户组使用“流程用户组”页面维护的用户组编码。
        </n-text>
      </n-form-item>
    </template>
  </div>
</template>

<style scoped>
.approver-assignee-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.approver-assignee-form :deep(.n-form-item) {
  margin-bottom: 0;
}

.option-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.option-card-grid.is-three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.option-card {
  min-width: 0;
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 9px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    color 160ms ease;
}

.option-card:hover:not(:disabled) {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #1e293b;
}

.option-card.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.option-card:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.option-card i {
  width: 14px;
  height: 14px;
  flex: none;
  font-size: 14px;
}

.candidate-group-hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
}
</style>
