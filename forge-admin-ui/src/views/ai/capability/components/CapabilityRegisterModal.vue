<template>
  <n-modal
    :show="show"
    preset="card"
    :title="modalTitle"
    class="capability-register-modal"
    :mask-closable="false"
    @update:show="emit('update:show', $event)"
  >
    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      label-width="112px"
    >
      <n-alert v-if="isUpgrade" type="info" class="form-alert">
        当前版本为 {{ capability.currentVersion }}。新版本会重新读取当前绑定生成快照，旧版本保持不变。
      </n-alert>

      <n-form-item label="能力类型">
        <n-radio-group v-model:value="form.sourceType" :disabled="isUpgrade" @update:value="handleSourceTypeChange">
          <n-radio-button v-if="allowedTypes.includes('BUSINESS_ACTION')" value="BUSINESS_ACTION">
            业务动作
          </n-radio-button>
          <n-radio-button v-if="allowedTypes.includes('FLOW_ACTION')" value="FLOW_ACTION">
            流程动作
          </n-radio-button>
          <n-radio-button v-if="allowedTypes.includes('SYSTEM_SERVICE')" value="SYSTEM_SERVICE">
            系统服务
          </n-radio-button>
        </n-radio-group>
      </n-form-item>

      <n-alert v-if="sourceError" type="error" class="form-alert">
        {{ sourceError }}
      </n-alert>

      <n-form-item v-if="form.sourceType !== 'SYSTEM_SERVICE'" label="业务对象" path="objectId">
        <n-select
          v-model:value="form.objectId"
          :options="objectOptions"
          :loading="objectLoading"
          :disabled="isUpgrade"
          placeholder="请选择已发布业务对象"
          filterable
          @update:value="handleObjectChange"
        >
          <template #empty>
            <n-empty size="small" description="暂无已发布业务对象" />
          </template>
        </n-select>
      </n-form-item>

      <n-alert v-if="form.sourceType === 'FLOW_ACTION' && flowSourceError" type="error" class="form-alert">
        {{ flowSourceError }}
      </n-alert>
      <n-alert
        v-else-if="form.sourceType === 'FLOW_ACTION' && flowSource"
        type="success"
        class="form-alert"
      >
        已匹配主流程 {{ flowSource.flowModelKey }}，发布对象版本 v{{ flowSource.publishedObjectVersion }}。
      </n-alert>
      <n-alert
        v-if="form.sourceType === 'FLOW_ACTION' && flowSource && !flowSource.submissionSupported"
        type="warning"
        class="form-alert"
      >
        “提交业务申请”不可用：{{ flowSource.submissionUnavailableReason || '当前对象暂不支持由平台自动创建申请记录' }}
      </n-alert>
      <n-alert
        v-if="form.sourceType === 'FLOW_ACTION' && flowSubmitOptionMissing"
        type="warning"
        class="form-alert"
      >
        <div class="dict-refresh-notice">
          <span>
            当前没有加载到“提交业务申请”流程动作。{{ flowOperationDictError || '可能仍在使用页面打开时缓存的旧字典。' }}
          </span>
          <n-button
            text
            type="warning"
            size="small"
            :loading="dictLoading"
            @click="reloadFlowOperationOptions(true)"
          >
            重新加载流程动作
          </n-button>
        </div>
      </n-alert>

      <template v-if="form.sourceType === 'BUSINESS_ACTION'">
        <n-form-item label="业务动作" path="actionCode">
          <n-select
            v-model:value="form.actionCode"
            :options="actionOptions"
            :loading="detailLoading"
            :disabled="isUpgrade || !form.objectId"
            placeholder="请选择可开放的业务动作"
            filterable
            @update:value="handleActionChange"
          >
            <template #empty>
              <n-empty size="small" description="该对象暂无可发布动作" />
            </template>
          </n-select>
        </n-form-item>
        <n-alert
          v-if="businessActionNotice"
          :type="businessActionNotice.type"
          class="form-alert"
        >
          <div class="action-diagnostic">
            <strong>{{ businessActionNotice.title }}</strong>
            <span>{{ businessActionNotice.summary }}</span>
            <ul v-if="businessActionNotice.items.length">
              <li v-for="item in businessActionNotice.items" :key="item.actionCode">
                {{ item.unavailableReason }}
              </li>
            </ul>
            <span v-if="businessActionNotice.remaining > 0">
              还有 {{ businessActionNotice.remaining }} 个不可发布动作，可在业务对象设计器中查看并修正。
            </span>
            <div class="action-diagnostic-actions">
              <n-button
                v-if="recommendFlowSubmission"
                text
                type="primary"
                size="small"
                @click="switchToFlowSubmission"
              >
                改为“提交业务申请”
              </n-button>
              <n-button text type="warning" size="small" @click="openBusinessActionDesigner">
                打开业务对象设计器
              </n-button>
            </div>
          </div>
        </n-alert>
        <n-alert
          v-else-if="businessActionSource"
          type="success"
          class="form-alert"
        >
          已按业务对象发布版本 v{{ businessActionSource.publishedObjectVersion }} 校验执行步骤，当前动作均可发布。
        </n-alert>
        <n-form-item label="允许字段" path="allowedFields">
          <n-select
            v-model:value="form.allowedFields"
            :options="fieldOptions"
            :loading="detailLoading"
            :disabled="!form.objectId"
            placeholder="选择外部调用可以写入的字段"
            multiple
            filterable
            clearable
          >
            <template #empty>
              <n-empty size="small" description="该对象暂无可写业务字段" />
            </template>
          </n-select>
        </n-form-item>
        <n-form-item label="必填字段">
          <n-select
            v-model:value="form.requiredFields"
            :options="requiredFieldOptions"
            :disabled="form.allowedFields.length === 0"
            placeholder="可选，必须属于允许字段"
            multiple
            filterable
            clearable
          />
        </n-form-item>
      </template>

      <template v-else-if="form.sourceType === 'FLOW_ACTION'">
        <n-form-item label="流程动作" path="operation">
          <n-select
            v-model:value="form.operation"
            :options="flowOperationOptions"
            :loading="dictLoading"
            :disabled="isUpgrade || !form.objectId || detailLoading || !flowSource"
            placeholder="请选择流程动作"
            @update:value="handleOperationChange"
          >
            <template #empty>
              <n-empty size="small" description="流程动作字典尚未初始化" />
            </template>
          </n-select>
        </n-form-item>
        <template v-if="form.operation === 'SUBMIT'">
          <n-alert type="info" class="form-alert">
            外围系统只提交申请数据，Forge 会使用 Token 对应的真实用户创建业务记录并立即发起主流程，不需要先准备 recordId。
          </n-alert>
          <n-form-item label="允许输入字段" path="allowedFields">
            <n-select
              v-model:value="form.allowedFields"
              :options="flowSubmissionFieldOptions"
              :disabled="!flowSource?.submissionSupported"
              placeholder="选择外围系统可以填写的申请字段"
              multiple
              filterable
              clearable
            >
              <template #empty>
                <n-empty size="small" description="当前发布模型没有可开放的申请字段" />
              </template>
            </n-select>
            <template #feedback>
              字段类型、长度、字典和业务说明会自动写入接口文档；系统字段、用户、租户、单据状态和流程字段不会开放。
            </template>
          </n-form-item>
          <n-form-item label="必填字段">
            <n-select
              v-model:value="form.requiredFields"
              :options="flowRequiredFieldOptions"
              :disabled="form.allowedFields.length === 0"
              placeholder="业务模型必填项已自动锁定，可增加接口级必填项"
              multiple
              filterable
              clearable
            />
          </n-form-item>
        </template>
        <n-alert v-else type="info" class="form-alert">
          {{ form.operation === 'START'
            ? 'START 只适用于 Forge 中已经保存的业务记录，调用时必须传真实 recordId。'
            : '流程办理只能通过用户委托 Token 调用，办理人和组织从可信登录身份解析。' }}
        </n-alert>
      </template>

      <template v-else>
        <n-form-item label="系统服务" path="systemServiceCode">
          <n-select
            v-model:value="form.systemServiceCode"
            :options="systemServiceOptions"
            :loading="systemSourceLoading"
            :disabled="isUpgrade"
            placeholder="请选择平台已注册的系统服务"
            filterable
            @update:value="handleSystemServiceChange"
          >
            <template #empty>
              <n-empty size="small" description="暂无代码注册的系统服务" />
            </template>
          </n-select>
        </n-form-item>
        <n-alert v-if="selectedSystemService" type="info" class="form-alert">
          <div class="service-summary">
            <strong>{{ selectedSystemService.serviceName }}</strong>
            <span>{{ selectedSystemService.description }}</span>
            <span>
              调用主体：{{ selectedSystemServiceActorLabel }}；风险等级：{{ selectedSystemServiceRiskLabel }}
            </span>
          </div>
        </n-alert>
        <n-form-item label="流程模型" path="systemModelId">
          <n-select
            v-model:value="form.systemModelId"
            :options="systemModelOptions"
            :disabled="isUpgrade || !selectedSystemService"
            placeholder="请选择已发布且启用的流程模型"
            filterable
            @update:value="updateGeneratedCode"
          >
            <template #empty>
              <n-empty size="small" description="暂无可开放的已发布流程模型" />
            </template>
          </n-select>
        </n-form-item>

        <n-form-item label="开放流程变量">
          <div class="variable-editor">
            <n-alert type="warning" :show-icon="true">
              流程变量会影响审批人和分支路由，默认不开放。只有外围系统确实需要传入的变量才应逐项添加。
            </n-alert>
            <div v-if="form.systemVariables.length" class="variable-list">
              <div
                v-for="(variable, index) in form.systemVariables"
                :key="variable.key"
                class="variable-row"
              >
                <n-input
                  v-model:value="variable.name"
                  placeholder="变量名"
                  maxlength="64"
                />
                <n-select
                  v-model:value="variable.type"
                  :options="systemVariableTypeOptions"
                  placeholder="类型"
                />
                <n-input
                  v-model:value="variable.description"
                  placeholder="业务含义和取值说明"
                  maxlength="200"
                />
                <n-checkbox v-model:checked="variable.required">
                  必填
                </n-checkbox>
                <n-button quaternary circle type="error" aria-label="删除变量" @click="removeSystemVariable(index)">
                  <template #icon>
                    <i class="i-material-symbols:delete-outline-rounded" />
                  </template>
                </n-button>
              </div>
            </div>
            <n-button dashed block :disabled="form.systemVariables.length >= 50" @click="addSystemVariable">
              <template #icon>
                <i class="i-material-symbols:add-rounded" />
              </template>
              添加允许外围传入的变量
            </n-button>
          </div>
        </n-form-item>
        <n-alert type="info" class="form-alert">
          外围请求不能传入模型、租户、用户、组织或发起人；这些信息由发布快照和用户委托身份固定。
        </n-alert>
      </template>

      <n-form-item label="能力编码" path="capabilityCode">
        <n-input
          v-model:value="form.capabilityCode"
          placeholder="如 business.order.create"
          maxlength="128"
          show-count
          :disabled="isUpgrade"
        />
      </n-form-item>
      <n-form-item label="能力版本" path="version">
        <n-input v-model:value="form.version" placeholder="如 1.0.0" />
        <template v-if="isUpgrade" #feedback>
          必须高于当前版本 {{ capability.currentVersion }}，已为你建议下一补丁版本。
        </template>
      </n-form-item>
      <n-form-item label="能力描述">
        <n-input
          v-model:value="form.description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-count
          placeholder="可选"
        />
      </n-form-item>
    </n-form>

    <template #footer>
      <n-space justify="end">
        <n-button @click="emit('update:show', false)">
          取消
        </n-button>
        <n-button
          type="primary"
          :loading="submitting"
          :disabled="submitDisabled"
          @click="handleSubmit"
        >
          {{ submitText }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  getBusinessActionRegistrationSource,
  getCapabilityVersionDraft,
  getFlowActionRegistrationSource,
  getSystemServiceRegistrationSources,
  publishBusinessActionCapability,
  publishFlowActionCapability,
  publishSystemServiceCapability,
} from '@/api/ai/capability'
import {
  businessObjectList,
} from '@/api/business-app'
import { useDict } from '@/composables'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  allowedTypes: {
    type: Array,
    default: () => [],
  },
  capability: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:show', 'success'])
const router = useRouter()

const {
  dict,
  loading: dictLoading,
  errors: dictErrors,
  reload: reloadCapabilityDicts,
} = useDict(
  'ai_capability_flow_operation',
  'ai_capability_actor_type',
  'ai_capability_risk_level',
)
const formRef = ref(null)
const objectLoading = ref(false)
const detailLoading = ref(false)
const systemSourceLoading = ref(false)
const submitting = ref(false)
const draftLoading = ref(false)
const sourceError = ref('')
const flowSourceError = ref('')
const flowSource = ref(null)
const businessActionSource = ref(null)
const objects = ref([])
const actions = ref([])
const fields = ref([])
const systemServices = ref([])
const lastGeneratedCode = ref('')
let variableKeySequence = 0

const form = reactive({
  sourceType: 'BUSINESS_ACTION',
  objectId: null,
  suiteCode: '',
  objectCode: '',
  actionCode: null,
  operation: null,
  capabilityCode: '',
  version: '1.0.0',
  description: '',
  allowedFields: [],
  requiredFields: [],
  systemServiceCode: null,
  systemModelId: null,
  systemVariables: [],
})

const flowOperationOptions = computed(() => (dict.value.ai_capability_flow_operation || [])
  .map(option => ({
    ...option,
    disabled: (option.value === 'START' && flowSource.value && !flowSource.value.startSupported)
      || (option.value === 'SUBMIT' && flowSource.value && !flowSource.value.submissionSupported),
  })))

const flowOperationDictLoaded = computed(() => Object.prototype.hasOwnProperty.call(
  dict.value,
  'ai_capability_flow_operation',
))

const flowOperationDictError = computed(() => dictErrors.value.ai_capability_flow_operation || '')

const flowSubmitOptionMissing = computed(() => !isUpgrade.value
  && !dictLoading.value
  && (flowOperationDictLoaded.value || !!flowOperationDictError.value)
  && !flowOperationOptions.value.some(option => option.value === 'SUBMIT'))

const flowSubmissionFieldOptions = computed(() => (flowSource.value?.submissionFields || [])
  .map(field => ({
    label: `${field.label || field.field}（${field.field} · ${field.dataType || 'string'}${field.required ? ' · 必填' : ''}）`,
    value: field.field,
    disabled: field.required,
  })))

const flowRequiredSourceFields = computed(() => (flowSource.value?.submissionFields || [])
  .filter(field => field.required)
  .map(field => field.field))

const flowRequiredFieldOptions = computed(() => flowSubmissionFieldOptions.value
  .filter(item => form.allowedFields.includes(item.value)))

const selectedSystemService = computed(() => systemServices.value
  .find(item => item.serviceCode === form.systemServiceCode))

const selectedSystemServiceActorLabel = computed(() => resolveDictLabel(
  'ai_capability_actor_type',
  selectedSystemService.value?.requiredActorType,
))

const selectedSystemServiceRiskLabel = computed(() => resolveDictLabel(
  'ai_capability_risk_level',
  selectedSystemService.value?.riskLevel,
))

const systemServiceOptions = computed(() => systemServices.value.map(item => ({
  label: `${item.serviceName}（${item.serviceCode}）`,
  value: item.serviceCode,
})))

const systemModelOptions = computed(() => (selectedSystemService.value?.options?.models || [])
  .map(model => ({
    label: `${model.modelName}（${model.modelKey} · v${model.modelVersion}）`,
    value: model.modelId,
  })))

const systemVariableTypeOptions = computed(() => (selectedSystemService.value?.options?.variableTypes || [])
  .map(type => ({
    label: variableTypeLabel(type),
    value: type,
  })))

const isUpgrade = computed(() => !!props.capability?.id)
const modalTitle = computed(() => isUpgrade.value ? '发布能力新版本' : '注册开放能力')
const submitText = computed(() => isUpgrade.value ? '发布新版本' : '注册并发布')

const submitDisabled = computed(() => {
  if (draftLoading.value || !!sourceError.value)
    return true
  if (form.sourceType === 'FLOW_ACTION')
    return !flowSource.value || detailLoading.value
      || (form.operation === 'SUBMIT' && !flowSource.value.submissionSupported)
  if (form.sourceType === 'SYSTEM_SERVICE')
    return systemSourceLoading.value || !selectedSystemService.value || !form.systemModelId
  return detailLoading.value || !selectedBusinessAction.value?.publishable
})

const rules = {
  objectId: {
    trigger: 'change',
    validator: (_rule, value) => form.sourceType === 'SYSTEM_SERVICE' || isPositiveId(value)
      ? true
      : new Error('请选择已发布业务对象'),
  },
  actionCode: {
    trigger: 'change',
    validator: () => {
      if (form.sourceType !== 'BUSINESS_ACTION')
        return true
      if (!form.actionCode)
        return new Error('请选择业务动作')
      if (!selectedBusinessAction.value?.publishable) {
        return new Error(selectedBusinessAction.value?.unavailableReason
          || '该业务动作的执行步骤不符合开放平台安全规则')
      }
      return true
    },
  },
  operation: {
    trigger: 'change',
    validator: () => {
      if (form.sourceType !== 'FLOW_ACTION')
        return true
      if (!flowSource.value)
        return new Error('所选对象未匹配到可发布的主流程')
      if (!form.operation)
        return new Error('请选择流程动作')
      if (form.operation === 'START' && !flowSource.value.startSupported)
        return new Error('该对象不是平台托管运行对象，不能注册发起流程能力')
      if (form.operation === 'SUBMIT' && !flowSource.value.submissionSupported)
        return new Error(flowSource.value.submissionUnavailableReason
          || '该对象暂不能注册提交业务申请能力')
      return true
    },
  },
  allowedFields: {
    trigger: 'change',
    validator: () => (form.sourceType !== 'BUSINESS_ACTION'
      && !(form.sourceType === 'FLOW_ACTION' && form.operation === 'SUBMIT'))
      || form.allowedFields.length > 0
      ? true
      : new Error('请至少选择一个允许字段'),
  },
  systemServiceCode: {
    trigger: 'change',
    validator: () => form.sourceType !== 'SYSTEM_SERVICE' || form.systemServiceCode
      ? true
      : new Error('请选择系统服务'),
  },
  systemModelId: {
    trigger: 'change',
    validator: () => form.sourceType !== 'SYSTEM_SERVICE' || form.systemModelId
      ? true
      : new Error('请选择已发布流程模型'),
  },
  capabilityCode: [
    { required: true, message: '请输入能力编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$/,
      message: '使用小写点分编码，每段以字母开头',
      trigger: 'blur',
    },
  ],
  version: [
    { required: true, message: '请输入能力版本', trigger: 'blur' },
    {
      pattern: /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/,
      message: '版本必须使用三段语义版本，如 1.0.0',
      trigger: 'blur',
    },
  ],
}

function isPositiveId(value) {
  if (typeof value === 'number')
    return Number.isInteger(value) && value > 0
  return typeof value === 'string' && /^[1-9]\d*$/.test(value)
}

function actionAvailabilitySuffix(action) {
  if (action.publishable)
    return ''
  if (action.status === 0)
    return ' · 已停用'
  if (String(action.actionType || '').toUpperCase() === 'OPEN_PAGE')
    return ' · 页面操作，不能直接开放'
  if (['START_FLOW', 'START_APPROVAL'].includes(String(action.actionType || '').toUpperCase()))
    return ' · 请使用流程动作'
  if (!Array.isArray(action.stepTypes) || action.stepTypes.length === 0)
    return ' · 未配置执行步骤'
  return ` · 步骤暂不支持（${action.stepTypes.join(' / ')}）`
}

const objectOptions = computed(() => objects.value.map(item => ({
  label: `${item.objectName || item.objectCode}（${item.objectCode}）`,
  value: item.id,
})))

const selectedBusinessAction = computed(() => actions.value
  .find(item => item.actionCode === form.actionCode))

const actionOptions = computed(() => actions.value.map(item => ({
  label: `${item.actionName || item.actionCode}（${item.actionCode}）${actionAvailabilitySuffix(item)}`,
  value: item.actionCode,
  disabled: !item.publishable,
})))

const recommendFlowSubmission = computed(() => !isUpgrade.value
  && props.allowedTypes.includes('FLOW_ACTION')
  && actions.value.some(item => isCreatePageAction(item)))

const businessActionNotice = computed(() => {
  if (!businessActionSource.value)
    return null
  const unavailable = actions.value.filter(item => !item.publishable)
  const availableCount = actions.value.length - unavailable.length
  if (actions.value.length === 0) {
    return {
      type: 'error',
      title: '当前发布版本没有业务动作',
      summary: '请先在业务对象设计器中新增自动化动作、配置执行步骤，然后重新发布业务对象。',
      items: [],
      remaining: 0,
    }
  }
  if (unavailable.length === 0)
    return null
  const containsCreatePageAction = unavailable.some(item => isCreatePageAction(item))
  return {
    type: availableCount > 0 ? 'warning' : 'error',
    title: availableCount > 0
      ? `${unavailable.length} 个动作已从可发布候选中禁用`
      : '当前发布版本没有可开放的业务动作',
    summary: availableCount > 0
      ? `仍有 ${availableCount} 个动作可选；禁用项会保留在下拉列表中并标明原因。`
      : containsCreatePageAction
        ? '你看到的“新增”只是打开新增表单的页面按钮，不会在服务端创建记录。申请类对象请改用“提交业务申请”，一次完成创建记录和发起流程。'
        : '业务动作必须包含开放平台支持的执行步骤，启用状态不代表它已经可执行。',
    items: unavailable.slice(0, 3),
    remaining: Math.max(unavailable.length - 3, 0),
  }
})

function isCreatePageAction(action = {}) {
  if (String(action.actionType || '').toUpperCase() !== 'OPEN_PAGE')
    return false
  const code = String(action.actionCode || '').trim().toLowerCase()
  const name = String(action.actionName || '').trim()
  return ['add', 'create', 'new', 'insert'].includes(code)
    || ['新增', '创建', '新建'].some(keyword => name.includes(keyword))
}

const fieldOptions = computed(() => fields.value
  .filter(item => String(item.fieldStatus || '').toUpperCase() !== 'DISABLED')
  .map(item => ({
    label: `${item.fieldName || item.fieldCode}（${item.fieldCode}）`,
    value: item.fieldCode,
  })))

const requiredFieldOptions = computed(() => fieldOptions.value
  .filter(item => form.allowedFields.includes(item.value)))

function resolveDictLabel(dictType, value) {
  if (!value)
    return '-'
  const option = (dict.value[dictType] || [])
    .find(item => String(item.value) === String(value))
  return option?.label || value
}

watch(() => props.show, async (visible) => {
  if (!visible)
    return
  await reloadFlowOperationOptions(false)
  resetForm()
  if (isUpgrade.value) {
    await initializeUpgrade()
  }
  else if (form.sourceType === 'SYSTEM_SERVICE')
    await loadSystemServices()
  else
    await loadObjects()
}, { immediate: true })

watch(flowOperationOptions, (options) => {
  if (form.sourceType !== 'FLOW_ACTION' || form.operation || options.length === 0)
    return
  const defaultOption = preferredFlowOperation(options)
  form.operation = defaultOption.value
  updateGeneratedCode()
}, { immediate: true })

watch(() => form.allowedFields, (allowedFields) => {
  if (form.sourceType === 'FLOW_ACTION' && form.operation === 'SUBMIT') {
    const required = flowRequiredSourceFields.value
    const nextAllowed = [...new Set([...allowedFields, ...required])]
    const nextRequired = [...new Set([
      ...form.requiredFields.filter(field => form.allowedFields.includes(field)),
      ...required,
    ])]
    if (!sameStringArray(form.allowedFields, nextAllowed))
      form.allowedFields = nextAllowed
    if (!sameStringArray(form.requiredFields, nextRequired))
      form.requiredFields = nextRequired
    return
  }
  const nextRequired = form.requiredFields.filter(field => allowedFields.includes(field))
  if (!sameStringArray(form.requiredFields, nextRequired))
    form.requiredFields = nextRequired
}, { deep: true })

function sameStringArray(left, right) {
  return left.length === right.length && left.every((item, index) => item === right[index])
}

function resetForm() {
  const sourceType = props.allowedTypes.includes('BUSINESS_ACTION')
    ? 'BUSINESS_ACTION'
    : props.allowedTypes[0] || 'BUSINESS_ACTION'
  Object.assign(form, {
    sourceType,
    objectId: null,
    suiteCode: '',
    objectCode: '',
    actionCode: null,
    operation: resolveDefaultOperation(),
    capabilityCode: '',
    version: '1.0.0',
    description: '',
    allowedFields: [],
    requiredFields: [],
    systemServiceCode: null,
    systemModelId: null,
    systemVariables: [],
  })
  actions.value = []
  fields.value = []
  sourceError.value = ''
  flowSourceError.value = ''
  flowSource.value = null
  businessActionSource.value = null
  systemServices.value = []
  lastGeneratedCode.value = ''
}

async function initializeUpgrade() {
  draftLoading.value = true
  sourceError.value = ''
  try {
    const res = await getCapabilityVersionDraft(props.capability.id)
    const draft = res.data
    if (!draft || !props.allowedTypes.includes(draft.sourceType)) {
      throw new Error('当前账号没有发布该类型能力新版本的权限')
    }
    Object.assign(form, {
      sourceType: draft.sourceType,
      capabilityCode: draft.capabilityCode,
      version: draft.suggestedVersion,
      description: draft.description || '',
    })
    if (draft.sourceType === 'SYSTEM_SERVICE') {
      await initializeSystemServiceUpgrade(draft)
    }
    else {
      await initializeObjectCapabilityUpgrade(draft)
    }
  }
  catch (error) {
    sourceError.value = error?.message || '能力新版本草稿加载失败'
  }
  finally {
    draftLoading.value = false
  }
}

async function initializeObjectCapabilityUpgrade(draft) {
  await loadObjects()
  const [suiteCode, objectCode, sourceAction] = String(draft.sourceKey || '').split('/')
  if (!suiteCode || !objectCode || !sourceAction)
    throw new Error('当前能力来源标识不完整，无法自动创建新版本')
  const object = objects.value.find((item) => {
    const itemSuiteCode = item.suiteCode || 'default'
    return itemSuiteCode === suiteCode && item.objectCode === objectCode
  })
  if (!object)
    throw new Error(`原业务对象 ${suiteCode}/${objectCode} 已不存在或尚未发布`)

  form.objectId = object.id
  if (draft.sourceType === 'FLOW_ACTION') {
    const operation = draft.policySnapshot?.operation || sourceAction
    if (operation !== sourceAction)
      throw new Error('当前能力流程动作快照不一致，无法自动创建新版本')
    form.operation = operation
  }
  await handleObjectChange(object.id)
  if (draft.sourceType === 'BUSINESS_ACTION') {
    const sourceOption = actionOptions.value.find(item => item.value === sourceAction)
    if (!sourceOption)
      throw new Error(`原业务动作 ${sourceAction} 已停用或不存在，无法创建新版本`)
    if (sourceOption.disabled) {
      const sourceActionDefinition = actions.value.find(item => item.actionCode === sourceAction)
      throw new Error(sourceActionDefinition?.unavailableReason
        || `原业务动作 ${sourceAction} 的执行步骤已不符合开放平台安全规则`)
    }
    form.actionCode = sourceAction
    const allowedFields = Array.isArray(draft.policySnapshot?.allowedFields)
      ? draft.policySnapshot.allowedFields
      : []
    const requiredFields = Array.isArray(draft.policySnapshot?.requiredFields)
      ? draft.policySnapshot.requiredFields
      : []
    const availableFields = new Set(fieldOptions.value.map(item => item.value))
    form.allowedFields = allowedFields.filter(field => availableFields.has(field))
    form.requiredFields = requiredFields.filter(field => form.allowedFields.includes(field))
  }
  else {
    if (!flowSource.value)
      throw new Error(flowSourceError.value || '当前业务对象未匹配到可发布的主流程')
    if (form.operation !== sourceAction)
      throw new Error(`原流程动作 ${sourceAction} 当前不可用，无法创建新版本`)
    if (form.operation === 'SUBMIT') {
      const availableFields = new Set(flowSubmissionFieldOptions.value.map(item => item.value))
      const allowedFields = Array.isArray(draft.policySnapshot?.allowedFields)
        ? draft.policySnapshot.allowedFields.filter(field => availableFields.has(field))
        : []
      const requiredFields = Array.isArray(draft.policySnapshot?.requiredFields)
        ? draft.policySnapshot.requiredFields.filter(field => availableFields.has(field))
        : []
      form.allowedFields = [...new Set([...allowedFields, ...flowRequiredSourceFields.value])]
      form.requiredFields = [...new Set([...requiredFields, ...flowRequiredSourceFields.value])]
    }
  }
}

async function initializeSystemServiceUpgrade(draft) {
  await loadSystemServices()
  const service = systemServices.value.find(item => item.serviceCode === draft.sourceKey)
  if (!service)
    throw new Error(`原系统服务 ${draft.sourceKey} 当前未注册，无法创建新版本`)
  form.systemServiceCode = service.serviceCode
  const modelId = draft.policySnapshot?.modelId || null
  const modelOption = systemModelOptions.value.find(item => String(item.value) === String(modelId))
  if (!modelOption)
    throw new Error('原流程模型已停用或未发布，无法创建新版本')
  form.systemModelId = modelOption.value

  const variableSchemas = draft.inputSchema?.properties?.variables?.properties || {}
  const allowedVariables = Array.isArray(draft.policySnapshot?.allowedVariables)
    ? draft.policySnapshot.allowedVariables
    : Object.keys(variableSchemas)
  const requiredVariables = new Set(Array.isArray(draft.policySnapshot?.requiredVariables)
    ? draft.policySnapshot.requiredVariables
    : [])
  form.systemVariables = allowedVariables.map((name) => {
    variableKeySequence += 1
    const schema = variableSchemas[name] || {}
    return {
      key: `variable-${variableKeySequence}`,
      name,
      type: schema.type || 'string',
      description: schema.description || '',
      required: requiredVariables.has(name),
    }
  })
}

function resolveDefaultOperation() {
  const options = flowOperationOptions.value
  return preferredFlowOperation(options)?.value || null
}

function preferredFlowOperation(options) {
  return options.find(item => item.value === 'SUBMIT' && !item.disabled)
    || options.find(item => item.isDefault === 'Y' && !item.disabled)
    || options.find(item => !item.disabled)
    || options[0]
}

async function loadObjects() {
  objectLoading.value = true
  sourceError.value = ''
  try {
    const res = await businessObjectList({})
    objects.value = (res.data || []).filter(item => item.status === 1
      && item.designStatus === 'PUBLISHED'
      && Number(item.lastPublishVersion || 0) > 0)
  }
  catch (error) {
    objects.value = []
    sourceError.value = error?.message || '已发布业务对象加载失败'
  }
  finally {
    objectLoading.value = false
  }
}

async function loadSystemServices() {
  systemSourceLoading.value = true
  sourceError.value = ''
  systemServices.value = []
  try {
    const res = await getSystemServiceRegistrationSources()
    systemServices.value = res.data || []
    if (systemServices.value.length === 1) {
      form.systemServiceCode = systemServices.value[0].serviceCode
      handleSystemServiceChange(form.systemServiceCode)
    }
  }
  catch (error) {
    sourceError.value = error?.message || '系统服务注册来源加载失败'
  }
  finally {
    systemSourceLoading.value = false
  }
}

async function handleSourceTypeChange() {
  form.objectId = null
  form.suiteCode = ''
  form.objectCode = ''
  form.actionCode = null
  form.operation = resolveDefaultOperation()
  form.allowedFields = []
  form.requiredFields = []
  form.systemServiceCode = null
  form.systemModelId = null
  form.systemVariables = []
  actions.value = []
  fields.value = []
  businessActionSource.value = null
  flowSourceError.value = ''
  flowSource.value = null
  updateGeneratedCode(true)
  if (form.sourceType === 'SYSTEM_SERVICE')
    await loadSystemServices()
  else if (objects.value.length === 0)
    await loadObjects()
}

async function handleObjectChange(objectId) {
  const selected = objects.value.find(item => item.id === objectId)
  form.suiteCode = selected?.suiteCode || ''
  form.objectCode = selected?.objectCode || ''
  form.actionCode = null
  form.allowedFields = []
  form.requiredFields = []
  actions.value = []
  fields.value = []
  businessActionSource.value = null
  flowSourceError.value = ''
  flowSource.value = null
  updateGeneratedCode()
  if (!selected)
    return

  detailLoading.value = true
  sourceError.value = ''
  try {
    if (form.sourceType === 'BUSINESS_ACTION') {
      const res = await getBusinessActionRegistrationSource({
        suiteCode: selected.suiteCode,
        objectCode: selected.objectCode,
      })
      businessActionSource.value = res.data || null
      actions.value = businessActionSource.value?.actions || []
      fields.value = businessActionSource.value?.writableFields || []
      const publishableActions = actions.value.filter(item => item.publishable)
      if (publishableActions.length === 1) {
        form.actionCode = publishableActions[0].actionCode
        updateGeneratedCode()
      }
    }
    else {
      const res = await getFlowActionRegistrationSource({
        suiteCode: selected.suiteCode,
        objectCode: selected.objectCode,
      })
      flowSource.value = res.data
      const selectedOperation = flowOperationOptions.value
        .find(option => option.value === form.operation && !option.disabled)
      if (!selectedOperation)
        form.operation = preferredFlowOperation(flowOperationOptions.value)?.value || null
      applyFlowSubmissionDefaults()
      updateGeneratedCode()
    }
  }
  catch (error) {
    if (form.sourceType === 'FLOW_ACTION') {
      flowSourceError.value = Number(error?.code) === 404
        ? '流程能力注册接口未装配，请更新并重启 Admin 服务'
        : error?.message || '该对象未配置已启用的主流程，暂不能注册流程能力'
    }
    else {
      sourceError.value = error?.message || '业务动作和字段加载失败'
    }
  }
  finally {
    detailLoading.value = false
  }
}

function handleActionChange() {
  updateGeneratedCode()
}

function openBusinessActionDesigner() {
  if (!form.objectCode)
    return
  const target = router.resolve({
    name: 'BusinessObjectDesigner',
    params: { objectCode: form.objectCode },
    query: {
      panel: 'actions',
      ...(form.suiteCode ? { suiteCode: form.suiteCode } : {}),
    },
  })
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

async function switchToFlowSubmission() {
  const objectId = form.objectId
  await reloadFlowOperationOptions(false)
  form.sourceType = 'FLOW_ACTION'
  await handleSourceTypeChange()
  form.operation = 'SUBMIT'
  form.objectId = objectId
  await handleObjectChange(objectId)
}

async function reloadFlowOperationOptions(selectSubmit = true) {
  await reloadCapabilityDicts('ai_capability_flow_operation')
  const submitOption = flowOperationOptions.value
    .find(option => option.value === 'SUBMIT' && !option.disabled)
  if (!selectSubmit || isUpgrade.value || form.sourceType !== 'FLOW_ACTION' || !submitOption)
    return
  form.operation = submitOption.value
  applyFlowSubmissionDefaults()
  updateGeneratedCode()
}

function handleOperationChange() {
  applyFlowSubmissionDefaults()
  updateGeneratedCode()
}

function applyFlowSubmissionDefaults() {
  if (form.operation !== 'SUBMIT') {
    if (form.sourceType === 'FLOW_ACTION') {
      form.allowedFields = []
      form.requiredFields = []
    }
    return
  }
  const available = (flowSource.value?.submissionFields || []).map(field => field.field)
  form.allowedFields = [...available]
  form.requiredFields = [...flowRequiredSourceFields.value]
}

function handleSystemServiceChange() {
  form.systemModelId = null
  form.systemVariables = []
  updateGeneratedCode()
}

function addSystemVariable() {
  if (form.systemVariables.length >= 50)
    return
  const defaultType = selectedSystemService.value?.options?.variableTypes?.includes('string')
    ? 'string'
    : selectedSystemService.value?.options?.variableTypes?.[0] || 'string'
  variableKeySequence += 1
  form.systemVariables.push({
    key: `variable-${variableKeySequence}`,
    name: '',
    type: defaultType,
    description: '',
    required: false,
  })
}

function removeSystemVariable(index) {
  form.systemVariables.splice(index, 1)
}

function variableTypeLabel(type) {
  return {
    string: '文本（string）',
    integer: '整数（integer）',
    number: '数值（number）',
    boolean: '布尔（boolean）',
    object: '对象（object）',
    array: '数组（array）',
  }[type] || type
}

function updateGeneratedCode(force = false) {
  if (isUpgrade.value)
    return
  if (form.sourceType === 'SYSTEM_SERVICE') {
    const model = selectedSystemService.value?.options?.models
      ?.find(item => item.modelId === form.systemModelId)
    const parts = [
      'system',
      ...String(form.systemServiceCode || '').split('.'),
      model?.modelKey,
    ].map(normalizeCodeSegment).filter(Boolean)
    const nextCode = form.systemServiceCode && model ? parts.join('.') : ''
    if (force || !form.capabilityCode || form.capabilityCode === lastGeneratedCode.value)
      form.capabilityCode = nextCode
    lastGeneratedCode.value = nextCode
    return
  }
  const actionSegment = form.sourceType === 'BUSINESS_ACTION' ? form.actionCode : form.operation
  const parts = [
    form.sourceType === 'BUSINESS_ACTION' || form.operation === 'SUBMIT' ? 'business' : 'flow',
    form.suiteCode,
    form.objectCode,
    actionSegment,
  ].map(normalizeCodeSegment).filter(Boolean)
  const nextCode = parts.length === 4 ? parts.join('.') : ''
  if (force || !form.capabilityCode || form.capabilityCode === lastGeneratedCode.value)
    form.capabilityCode = nextCode
  lastGeneratedCode.value = nextCode
}

function normalizeCodeSegment(value) {
  let segment = String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, '_')
    .replace(/^_+|_+$/g, '')
  if (segment && !/^[a-z]/.test(segment))
    segment = `x_${segment}`
  return segment
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  }
  catch {
    return
  }
  if (form.sourceType === 'SYSTEM_SERVICE' && !validateSystemVariables())
    return

  submitting.value = true
  try {
    const common = {
      capabilityCode: form.capabilityCode,
      version: form.version,
      suiteCode: form.suiteCode,
      objectCode: form.objectCode,
      description: form.description || null,
    }
    let res
    if (form.sourceType === 'BUSINESS_ACTION') {
      res = await publishBusinessActionCapability({
        ...common,
        actionCode: form.actionCode,
        allowedFields: form.allowedFields,
        requiredFields: form.requiredFields,
      })
    }
    else if (form.sourceType === 'FLOW_ACTION') {
      res = await publishFlowActionCapability({
        ...common,
        operation: form.operation,
        allowedFields: form.operation === 'SUBMIT' ? form.allowedFields : [],
        requiredFields: form.operation === 'SUBMIT' ? form.requiredFields : [],
      })
    }
    else {
      res = await publishSystemServiceCapability({
        serviceCode: form.systemServiceCode,
        capabilityCode: form.capabilityCode,
        version: form.version,
        description: form.description || null,
        parameters: {
          modelId: form.systemModelId,
          variables: form.systemVariables.map(variable => ({
            name: variable.name.trim(),
            type: variable.type,
            description: variable.description.trim(),
            required: variable.required,
          })),
        },
      })
    }
    if (res.code === 200) {
      window.$message.success(isUpgrade.value
        ? `能力新版本 ${form.version} 已发布`
        : '能力已注册并发布')
      if (isUpgrade.value) {
        window.$message.warning('固定版本授权不会自动切换，请到授权管理修改版本，或改用“跟随主版本”策略')
      }
      emit('update:show', false)
      emit('success', { id: res.data, version: form.version, upgrade: isUpgrade.value })
    }
  }
  finally {
    submitting.value = false
  }
}

function validateSystemVariables() {
  const names = new Set()
  for (const [index, variable] of form.systemVariables.entries()) {
    const name = variable.name.trim()
    if (!/^[a-z]\w{0,63}$/i.test(name)) {
      window.$message.error(`第 ${index + 1} 个流程变量名称无效，只能以字母开头并包含字母、数字、下划线`)
      return false
    }
    if (names.has(name)) {
      window.$message.error(`流程变量名称重复：${name}`)
      return false
    }
    names.add(name)
    if (!variable.description.trim()) {
      window.$message.error(`请填写流程变量 ${name} 的业务含义和取值说明`)
      return false
    }
  }
  return true
}
</script>

<style scoped>
.form-alert {
  margin-bottom: 18px;
}

.service-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1.55;
}

.action-diagnostic {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  line-height: 1.55;
}

.action-diagnostic ul {
  display: grid;
  gap: 4px;
  margin: 0;
  padding-left: 18px;
}

.action-diagnostic-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.dict-refresh-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.variable-editor {
  display: flex;
  width: 100%;
  min-width: 0;
  flex-direction: column;
  gap: 12px;
}

.variable-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.variable-row {
  display: grid;
  grid-template-columns: minmax(120px, 0.9fr) 150px minmax(180px, 1.4fr) auto 34px;
  align-items: center;
  gap: 8px;
}

@media (max-width: 760px) {
  .variable-row {
    grid-template-columns: 1fr;
    padding: 12px;
    border: 1px solid var(--border-light);
  }
}

:global(.capability-register-modal) {
  width: min(920px, calc(100vw - 32px));
}
</style>
