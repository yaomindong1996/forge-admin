<template>
  <n-drawer
    :show="show"
    :width="drawerWidth"
    placement="right"
    @update:show="value => emit('update:show', value)"
    @after-leave="handleAfterLeave"
  >
    <n-drawer-content :title="isEdit ? `编辑增强 · ${form.extensionName}` : '新建增强'" closable>
      <n-alert v-if="isEdit" type="info" :show-icon="false" class="lock-alert">
        编辑锁有效至 {{ form.lockExpireTime || '稍后' }}。保存内容会追加新版本；当前运行版本不会被覆盖。
      </n-alert>

      <section v-if="!isEdit" class="extension-type-guide">
        <div class="type-guide-heading">
          <strong>先选择你要实现的效果</strong>
          <span>选择后再配置作用对象、触发时机和具体内容。</span>
        </div>
        <div class="type-guide-grid">
          <button
            v-for="item in extensionTypeChoices"
            :key="item.value"
            type="button"
            class="type-guide-card"
            :class="{ active: form.extensionType === item.value }"
            @click="form.extensionType = item.value"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.description }}</span>
            <small>{{ item.scene }}</small>
          </button>
        </div>
        <n-alert type="info" :show-icon="false" class="sql-boundary-alert">
          当前不开放任意 SQL 文本增强。数据库逻辑请先使用业务规则或 Java 服务增强；如需 SQL 能力，应单独建设参数化只读查询和受控数据动作。
        </n-alert>
      </section>

      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <section class="editor-section">
          <h3>基本信息</h3>
          <div class="form-grid two-columns">
            <n-form-item label="扩展名称" path="extensionName">
              <n-input v-model:value="form.extensionName" placeholder="例如：客户提交校验" />
            </n-form-item>
            <n-form-item label="扩展编码" path="extensionCode">
              <n-input v-model:value="form.extensionCode" :disabled="isEdit" placeholder="validate_customer" />
            </n-form-item>
            <n-form-item label="扩展类型" path="extensionType">
              <div class="selected-extension-type">
                <DictTag dict-type="ai_business_extension_type" :value="form.extensionType" :bordered="false" />
                <span>{{ selectedExtensionType?.description }}</span>
              </div>
            </n-form-item>
            <n-form-item label="业务对象">
              <n-select
                v-model:value="form.objectId"
                clearable
                filterable
                :options="objectOptions"
                placeholder="不选表示应用级"
              />
            </n-form-item>
            <n-form-item label="页面入口">
              <n-select
                v-model:value="form.entryId"
                clearable
                filterable
                :options="entryOptions"
                placeholder="可进一步限定到入口"
              />
            </n-form-item>
            <n-form-item label="失败策略" path="failurePolicy">
              <DictSelect
                v-model:value="form.failurePolicy"
                dict-type="ai_business_extension_failure_policy"
                :clearable="false"
              />
            </n-form-item>
            <n-form-item label="风险级别" path="riskLevel">
              <DictSelect
                v-model:value="form.riskLevel"
                dict-type="ai_business_extension_risk_level"
                :clearable="false"
              />
            </n-form-item>
          </div>
        </section>

        <section class="editor-section hook-editor-section">
          <n-form-item path="hookCode">
            <ExtensionHookMatrix
              v-model="form.hookCode"
              :allowed-hooks="allowedHooksForType"
            />
          </n-form-item>
        </section>

        <section v-if="form.extensionType === 'VISUAL_RULE'" class="editor-section">
          <div class="section-heading">
            <div>
              <h3>可视化条件与动作</h3>
              <p>不写脚本，通过字段条件触发受限动作。</p>
            </div>
            <n-radio-group v-model:value="visualRule.match" size="small">
              <n-radio-button value="ALL">
                满足全部
              </n-radio-button>
              <n-radio-button value="ANY">
                满足任一
              </n-radio-button>
            </n-radio-group>
          </div>

          <div class="rule-block">
            <div class="rule-block-title">
              <strong>条件</strong>
              <n-button size="tiny" secondary @click="addCondition">
                添加条件
              </n-button>
            </div>
            <div v-if="!visualRule.conditions.length" class="inline-empty">
              没有条件时始终执行动作
            </div>
            <div v-for="(condition, index) in visualRule.conditions" :key="`condition-${index}`" class="rule-row condition-row">
              <n-input v-model:value="condition.field" placeholder="字段编码" />
              <DictSelect
                v-model:value="condition.operator"
                dict-type="ai_business_extension_rule_operator"
                :clearable="false"
              />
              <n-input v-model:value="condition.value" :disabled="condition.operator === 'EMPTY'" placeholder="比较值" />
              <n-button quaternary type="error" @click="visualRule.conditions.splice(index, 1)">
                移除
              </n-button>
            </div>
          </div>

          <div class="rule-block">
            <div class="rule-block-title">
              <strong>动作</strong>
              <n-button size="tiny" secondary @click="addAction">
                添加动作
              </n-button>
            </div>
            <div v-if="!visualRule.actions.length" class="inline-empty error">
              至少需要一个动作
            </div>
            <div v-for="(action, index) in visualRule.actions" :key="`action-${index}`" class="rule-row action-row">
              <DictSelect
                v-model:value="action.actionType"
                dict-type="ai_business_extension_rule_action"
                :clearable="false"
              />
              <n-input
                v-if="action.actionType === 'SET_FIELD'"
                v-model:value="action.field"
                placeholder="目标字段编码"
              />
              <n-input
                v-if="action.actionType === 'SET_FIELD'"
                v-model:value="action.value"
                placeholder="设置值"
              />
              <n-input
                v-else-if="action.actionType === 'SHOW_MESSAGE'"
                v-model:value="action.message"
                placeholder="提示内容"
              />
              <n-input
                v-else
                v-model:value="action.actionCode"
                placeholder="白名单动作编码"
              />
              <n-button quaternary type="error" @click="visualRule.actions.splice(index, 1)">
                移除
              </n-button>
            </div>
          </div>
        </section>

        <section v-else-if="form.extensionType === 'CLIENT_JS'" class="editor-section developer-section">
          <h3>客户端脚本</h3>
          <ExtensionCodeWorkbench
            v-model="form.content"
            mode="javascript"
            :hook-code="form.hookCode"
            :application-code="application?.applicationCode"
            :page-code="selectedEntryLabel"
            @example-applied="applyCodeExampleContext"
          />
          <div class="test-context-section">
            <div class="test-context-heading">
              <div>
                <strong>准备一条测试数据</strong>
                <span>字段和页面动作会从脚本自动识别，你只需要确认测试值。</span>
              </div>
              <n-button
                size="tiny"
                secondary
                :loading="clientContextCatalogLoading"
                @click="syncClientContextFromScript(false)"
              >
                重新识别脚本
              </n-button>
            </div>

            <div class="test-scene-summary">
              <div>
                <span class="test-scene-kicker">当前模拟</span>
                <strong>{{ clientContextScene.title }}</strong>
                <p>{{ clientContextScene.description }}</p>
              </div>
              <n-tag size="small" :bordered="false">
                {{ selectedEntryLabel }}
              </n-tag>
            </div>

            <div class="test-data-block">
              <div class="test-data-toolbar">
                <div>
                  <strong>业务字段值</strong>
                  <span>{{ clientDetectedSummary }}</span>
                </div>
                <div class="test-data-actions">
                  <n-button size="tiny" quaternary @click="applyClientValuePreset('SAMPLE')">
                    填充示例值
                  </n-button>
                  <n-button size="tiny" quaternary @click="applyClientValuePreset('EMPTY')">
                    模拟空值
                  </n-button>
                  <n-button size="tiny" secondary @click="addClientTestField()">
                    添加字段
                  </n-button>
                </div>
              </div>

              <div class="record-id-row">
                <span>测试记录 ID</span>
                <n-input
                  v-model:value="clientRecordId"
                  size="small"
                  placeholder="例如：1"
                />
                <small>用于模拟当前表单或列表行，不会查询数据库。</small>
              </div>

              <div v-if="clientTestFields.length" class="test-field-table">
                <div class="test-field-row test-field-head">
                  <span>业务字段</span>
                  <span>值类型</span>
                  <span>本次测试值</span>
                  <span />
                </div>
                <div
                  v-for="(field, index) in clientTestFields"
                  :key="field.key"
                  class="test-field-row"
                >
                  <n-select
                    v-model:value="field.fieldCode"
                    filterable
                    tag
                    :options="clientFieldOptions"
                    placeholder="选择字段或输入字段编码"
                    @update:value="value => handleClientTestFieldChange(field, value)"
                  />
                  <n-select
                    v-model:value="field.valueType"
                    :options="clientValueTypeOptions"
                    :clearable="false"
                    @update:value="value => handleClientValueTypeChange(field, value)"
                  />
                  <n-select
                    v-if="field.valueType === 'BOOLEAN'"
                    v-model:value="field.value"
                    :options="clientBooleanOptions"
                    :clearable="false"
                  />
                  <span v-else-if="field.valueType === 'NULL'" class="null-value-placeholder">
                    空值 null
                  </span>
                  <n-input
                    v-else
                    v-model:value="field.value"
                    :placeholder="clientValuePlaceholder(field.valueType)"
                  />
                  <n-button quaternary type="error" @click="clientTestFields.splice(index, 1)">
                    移除
                  </n-button>
                </div>
              </div>
              <div v-else class="test-fields-empty">
                当前脚本没有读取或修改业务字段，可以直接测试；如需补充数据，请点击“添加字段”。
              </div>

              <n-form-item
                v-if="clientAllowedActions.length"
                label="允许脚本触发的页面动作"
                class="test-actions-field"
              >
                <n-select
                  v-model:value="clientAllowedActions"
                  multiple
                  filterable
                  tag
                  :options="clientActionOptions"
                  placeholder="脚本未调用页面动作时无需选择"
                />
                <template #feedback>
                  已从 triggerAction 自动识别；只有这里列出的动作会在测试中放行。
                </template>
              </n-form-item>
            </div>

            <details class="test-context-advanced">
              <summary>查看沙箱实际接收的内容（高级）</summary>
              <pre>{{ clientContextPreview }}</pre>
            </details>
          </div>
          <ExtensionSandboxHost ref="sandboxRef" />
        </section>

        <section v-else-if="form.extensionType === 'SCOPED_CSS'" class="editor-section developer-section">
          <div class="section-heading">
            <div>
              <h3>作用域样式</h3>
              <p>所有选择器会自动限制在当前应用和页面根节点。</p>
            </div>
            <n-form-item label="页面编码" class="page-code-field">
              <n-input v-model:value="form.scopeKey" placeholder="customer_form" />
            </n-form-item>
          </div>
          <ExtensionCodeWorkbench
            v-model="form.content"
            mode="css"
            :hook-code="form.hookCode"
            :application-code="application?.applicationCode"
            :page-code="form.scopeKey || 'default'"
          />
          <n-alert v-if="cssError" type="error" :show-icon="false" class="code-feedback">
            {{ cssError }}
          </n-alert>
          <ScopedCssPreview
            v-else-if="cssResult"
            class="code-feedback"
            :css="cssResult.css"
            :scope-selector="cssResult.scopeSelector"
            :application-code="application?.applicationCode"
            :page-code="form.scopeKey || 'default'"
          />
        </section>

        <section v-else-if="form.extensionType === 'SERVER_BINDING'" class="editor-section developer-section">
          <h3>Java 服务增强</h3>
          <p class="security-note">
            Java 开发人员实现 <code>LowcodeExtensionHandler</code> 并随服务部署后，会自动注册到这里。设计人员只选择能力，不在线编译 Java，也不填写 Bean 或 Class 名。
          </p>
          <n-alert v-if="!handlerOptions.length" type="warning" :show-icon="false" class="handler-empty-alert">
            暂无已注册的 Java 增强处理器。请先在后端实现并注册 LowcodeExtensionHandler，然后重启服务。
          </n-alert>
          <n-form-item label="注册处理器" path="handlerCode">
            <n-select
              v-model:value="handlerCode"
              filterable
              :options="handlerOptions"
              placeholder="请选择管理员已注册处理器"
            />
          </n-form-item>
          <div v-if="selectedHandler" class="handler-contract">
            <span>允许触发点：{{ selectedHandler.allowedHooks?.length || 0 }} 个，上方矩阵已限制可选范围</span>
            <span>输入字段：{{ Object.keys(selectedHandler.inputSchema || {}).join('、') || '无' }}</span>
            <span>超时：{{ selectedHandler.timeoutMs }}ms</span>
            <span>风险：{{ riskLevelLabel(selectedHandler.riskLevel) }}</span>
            <span>所需权限：{{ selectedHandler.requiredPermission || '无额外权限' }}</span>
          </div>
          <n-form-item label="测试输入 JSON">
            <n-input v-model:value="serverTestInput" type="textarea" :autosize="{ minRows: 6, maxRows: 12 }" />
          </n-form-item>
        </section>

        <section class="editor-section test-console-section">
          <div class="test-console-heading">
            <div>
              <h3>扩展测试</h3>
              <p>测试会依次保存当前草稿、执行安全校验和受限运行，不会直接启用扩展。</p>
            </div>
            <span>{{ testStage === 'IDLE' ? '等待测试' : testSummary }}</span>
          </div>
          <div class="test-step-list">
            <div
              v-for="step in testSteps"
              :key="step.code"
              class="test-step"
              :class="testStepClass(step.code)"
            >
              <i>{{ testStepIcon(step) }}</i>
              <span>{{ step.label }}</span>
            </div>
          </div>
          <n-alert
            v-if="testStage === 'FAILED'"
            type="error"
            :show-icon="false"
            class="test-result-alert"
          >
            {{ testSummary }}
          </n-alert>
          <n-alert
            v-else-if="testStage === 'PASSED'"
            type="success"
            :show-icon="false"
            class="test-result-alert"
          >
            当前草稿已通过校验和受限测试，可以启用或随应用发布。
          </n-alert>
        </section>

        <section class="editor-section">
          <h3>版本说明</h3>
          <n-form-item label="本次变更说明">
            <n-input v-model:value="form.changeSummary" placeholder="说明这次调整的原因和影响" />
          </n-form-item>
          <n-form-item label="备注">
            <n-input v-model:value="form.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
          </n-form-item>
        </section>
      </n-form>

      <template #footer>
        <div class="drawer-footer">
          <span>{{ isEdit ? `草稿 v${extension?.draftVersion || '-'}` : '保存后生成 v1 草稿' }}</span>
          <n-space>
            <n-button @click="emit('update:show', false)">
              取消
            </n-button>
            <n-button :loading="saving" secondary @click="saveCurrent">
              保存草稿
            </n-button>
            <n-button :loading="testing" type="primary" @click="saveAndTest">
              保存并测试
            </n-button>
          </n-space>
        </div>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { businessObjectActions, businessObjectFields } from '@/api/business-app'
import {
  acquireBusinessExtensionLock,
  createBusinessExtension,
  renewBusinessExtensionLock,
  saveBusinessExtensionDraft,
  testBusinessExtension,
  updateBusinessExtension,
  validateBusinessExtension,
} from '@/api/business-extension'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'
import ExtensionCodeWorkbench from '@/components/lowcode-extension/ExtensionCodeWorkbench.vue'
import { processScopedCss } from '@/components/lowcode-extension/css/scoped-css'
import ScopedCssPreview from '@/components/lowcode-extension/css/ScopedCssPreview.vue'
import { validateClientScript } from '@/components/lowcode-extension/js/extension-context-api'
import ExtensionSandboxHost from '@/components/lowcode-extension/js/ExtensionSandboxHost.vue'
import ExtensionHookMatrix from './ExtensionHookMatrix.vue'

const props = defineProps({
  show: Boolean,
  application: {
    type: Object,
    default: null,
  },
  extension: {
    type: Object,
    default: null,
  },
  objects: {
    type: Array,
    default: () => [],
  },
  entries: {
    type: Array,
    default: () => [],
  },
  handlers: {
    type: Array,
    default: () => [],
  },
  startWithTest: Boolean,
})

const emit = defineEmits(['update:show', 'saved', 'closed'])
const message = useMessage()
const formRef = ref(null)
const sandboxRef = ref(null)
const saving = ref(false)
const testing = ref(false)
const handlerCode = ref(null)
const serverTestInput = ref('{}')
const clientRecordId = ref('1')
const clientTestFields = ref([])
const clientAllowedActions = ref([])
const clientFieldCatalog = ref([])
const clientActionCatalog = ref([])
const clientContextCatalogLoading = ref(false)
const testStage = ref('IDLE')
const testFailedStage = ref('')
const testSummary = ref('点击底部“保存并测试”开始')
const completedTestStages = ref([])
const visualRule = reactive({ match: 'ALL', conditions: [], actions: [] })
const form = reactive(defaultForm())
let renewTimer = null
let clientTestFieldSeed = 0
let clientContextCatalogRequestId = 0

const extensionTypeChoices = [
  {
    value: 'VISUAL_RULE',
    title: '业务规则',
    description: '通过条件和动作完成校验、赋值和提示。',
    scene: '适合实施人员，无需写代码',
  },
  {
    value: 'CLIENT_JS',
    title: '页面 JS 增强',
    description: '处理字段联动、页面提示和受控页面动作。',
    scene: '在独立沙箱运行，不访问 DOM 和网络',
  },
  {
    value: 'SCOPED_CSS',
    title: '页面 CSS 增强',
    description: '调整指定应用页面内的局部样式。',
    scene: '选择器自动限制作用范围',
  },
  {
    value: 'SERVER_BINDING',
    title: 'Java 服务增强',
    description: '调用后端已开发、部署并注册的业务处理器。',
    scene: '适合复杂校验、计算和系统集成',
  },
]

const clientValueTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '数字', value: 'NUMBER' },
  { label: '是 / 否', value: 'BOOLEAN' },
  { label: '空值', value: 'NULL' },
  { label: '对象 / 数组', value: 'JSON' },
]
const clientBooleanOptions = [
  { label: '是（true）', value: 'true' },
  { label: '否（false）', value: 'false' },
]
const clientSensitiveKeyPattern = /token|secret|password|cookie|authorization|api[_-]?key|session/i
const clientReservedKeys = new Set(['__proto__', 'prototype', 'constructor'])
const clientHookScenes = {
  PAGE_INIT: {
    title: '页面刚打开',
    description: '用下面这条业务记录模拟页面初始化，适合检查默认值和首次提示。',
  },
  FORM_CHANGE: {
    title: '用户修改了表单字段',
    description: '填写字段变化后的值，检查联动计算、自动赋值和即时提示。',
  },
  BEFORE_SUBMIT: {
    title: '用户点击提交',
    description: '填写即将提交的表单数据，检查业务校验是否按预期通过或阻断。',
  },
  AFTER_SUBMIT: {
    title: '表单已经保存成功',
    description: '填写保存后的记录数据，检查成功提示和后续页面动作。',
  },
  ROW_ACTION: {
    title: '用户操作了一条列表记录',
    description: '用下面这条业务记录模拟当前行，检查行操作后的字段和页面动作。',
  },
}

const isEdit = computed(() => Boolean(form.id))
const objectOptions = computed(() => props.objects.map(item => ({
  label: `${item.objectName || item.objectCode} · ${item.objectCode}`,
  value: item.objectId,
})))
const entryOptions = computed(() => props.entries.map(item => ({
  label: entryDisplayName(item),
  value: item.id,
})))
const handlerOptions = computed(() => props.handlers.map(item => ({
  label: `${item.handlerName} · ${item.handlerCode}`,
  value: item.handlerCode,
})))
const selectedHandler = computed(() => props.handlers.find(item => item.handlerCode === handlerCode.value))
const selectedExtensionType = computed(() => extensionTypeChoices.find(item => item.value === form.extensionType))
const drawerWidth = computed(() => ['CLIENT_JS', 'SCOPED_CSS'].includes(form.extensionType)
  ? 'min(1120px, 96vw)'
  : 'min(760px, 96vw)')
const selectedEntryLabel = computed(() => {
  const entry = props.entries.find(item => item.id === form.entryId)
  return entry ? entryDisplayName(entry) : '当前页面'
})
const clientFieldOptions = computed(() => clientFieldCatalog.value.map(field => ({
  label: `${field.fieldName || field.fieldCode}（${field.fieldCode}）`,
  value: field.fieldCode,
})).filter(item => item.value))
const clientActionOptions = computed(() => clientActionCatalog.value.map(action => ({
  label: `${action.actionName || action.actionCode}（${action.actionCode}）`,
  value: action.actionCode,
})).filter(item => item.value))
const clientContextScene = computed(() => clientHookScenes[form.hookCode] || {
  title: '当前业务触发场景',
  description: '填写脚本本次运行需要读取的业务数据。',
})
const clientDetectedSummary = computed(() => {
  const detected = detectClientScriptBindings(form.content)
  if (!detected.fields.length && !detected.actions.length)
    return '脚本当前不依赖业务字段或页面动作'
  const parts = []
  if (detected.fields.length)
    parts.push(`自动识别 ${detected.fields.length} 个字段`)
  if (detected.actions.length)
    parts.push(`自动识别 ${detected.actions.length} 个页面动作`)
  return parts.join('，')
})
const clientContextPreview = computed(() => {
  try {
    return JSON.stringify(buildClientSandboxContext().context, null, 2)
  }
  catch (error) {
    return `请先完善测试数据：${error instanceof Error ? error.message : '测试数据格式不正确'}`
  }
})
const testSteps = computed(() => {
  const steps = [
    { code: 'SAVE', label: '保存草稿' },
    { code: 'VALIDATE', label: '安全校验' },
  ]
  if (form.extensionType === 'CLIENT_JS')
    steps.push({ code: 'SANDBOX', label: '沙箱执行' })
  steps.push({ code: 'SERVER', label: '后端确认' })
  return steps.map((step, index) => ({ ...step, order: index + 1 }))
})
const allowedHooksForType = computed(() => {
  if (form.extensionType === 'SCOPED_CSS')
    return ['PAGE_INIT']
  if (form.extensionType === 'CLIENT_JS')
    return ['PAGE_INIT', 'FORM_CHANGE', 'BEFORE_SUBMIT', 'AFTER_SUBMIT', 'ROW_ACTION']
  if (form.extensionType === 'SERVER_BINDING' && selectedHandler.value)
    return [...(selectedHandler.value.allowedHooks || [])]
  return null
})
const cssResult = computed(() => {
  if (form.extensionType !== 'SCOPED_CSS' || !form.content.trim())
    return null
  try {
    return processScopedCss(form.content, {
      applicationCode: props.application?.applicationCode || '',
      pageCode: form.scopeKey || 'default',
    })
  }
  catch {
    return null
  }
})

function entryDisplayName(item = {}) {
  const appName = String(item.appName || '').trim()
  const appCode = String(item.appCode || '').trim()
  if (appName && appName !== appCode && !/^[A-Z][A-Z0-9_]*$/.test(appName))
    return appName
  return item.objectName ? `${item.objectName}入口` : '业务访问入口'
}

function riskLevelLabel(value) {
  return {
    LOW: '低风险',
    MEDIUM: '中风险',
    HIGH: '高风险',
  }[value] || '未声明'
}
const cssError = computed(() => {
  if (form.extensionType !== 'SCOPED_CSS' || !form.content.trim())
    return ''
  try {
    processScopedCss(form.content, {
      applicationCode: props.application?.applicationCode || '',
      pageCode: form.scopeKey || 'default',
    })
    return ''
  }
  catch (error) {
    return error.message
  }
})

const rules = {
  extensionName: { required: true, message: '请输入扩展名称', trigger: ['blur', 'input'] },
  extensionCode: {
    required: true,
    pattern: /^[a-z]\w{1,63}$/i,
    message: '字母开头，仅含字母、数字和下划线，2-64字符',
    trigger: ['blur', 'input'],
  },
  extensionType: { required: true, message: '请选择扩展类型', trigger: 'change' },
  hookCode: { required: true, message: '请选择执行钩子', trigger: 'change' },
  failurePolicy: { required: true, message: '请选择失败策略', trigger: 'change' },
  riskLevel: { required: true, message: '请选择风险级别', trigger: 'change' },
}

watch(() => props.show, async (visible) => {
  if (!visible) {
    clearRenewTimer()
    return
  }
  hydrateForm()
  startRenewTimer()
  if (props.startWithTest) {
    await nextTick()
    message.info('请确认自动识别的测试字段值，然后执行“保存并测试”')
  }
})

watch(() => form.objectId, (objectId) => {
  loadClientContextCatalog(objectId || resolveClientContextObjectId())
  if (!objectId)
    return
  const allowedEntryIds = new Set(props.entries
    .filter(item => item.objectCode === props.objects.find(object => object.objectId === objectId)?.objectCode)
    .map(item => item.id))
  if (form.entryId && !allowedEntryIds.has(form.entryId))
    form.entryId = null
})

watch(() => form.entryId, () => {
  if (!form.objectId)
    loadClientContextCatalog(resolveClientContextObjectId())
})

watch(() => form.extensionType, (type) => {
  const allowedHooks = allowedHooksForType.value
  if (type === 'SERVER_BINDING' && !selectedHandler.value)
    return
  if (allowedHooks?.length && !allowedHooks.includes(form.hookCode))
    form.hookCode = allowedHooks[0]
})

watch(handlerCode, () => {
  if (form.extensionType !== 'SERVER_BINDING')
    return
  serverTestInput.value = buildHandlerTestInput(selectedHandler.value)
  const allowedHooks = allowedHooksForType.value
  if (allowedHooks?.length && !allowedHooks.includes(form.hookCode))
    form.hookCode = allowedHooks[0]
})

watch(() => [form.content, form.hookCode, handlerCode.value], () => {
  if (['PASSED', 'FAILED'].includes(testStage.value))
    resetTestStatus()
  if (form.extensionType === 'CLIENT_JS')
    syncClientContextFromScript(true)
})

watch([clientRecordId, clientTestFields, clientAllowedActions], () => {
  if (['PASSED', 'FAILED'].includes(testStage.value))
    resetTestStatus()
}, { deep: true })

onBeforeUnmount(clearRenewTimer)

function defaultForm() {
  return {
    id: null,
    applicationId: null,
    objectId: null,
    entryId: null,
    extensionName: '',
    extensionCode: '',
    extensionType: 'VISUAL_RULE',
    hookCode: 'BEFORE_SUBMIT',
    scopeType: 'APPLICATION',
    scopeKey: '',
    sortOrder: 0,
    failurePolicy: 'BLOCK',
    riskLevel: 'MEDIUM',
    content: '',
    processedContent: '',
    configJson: '{}',
    changeSummary: '',
    lockToken: '',
    lockExpireTime: '',
    remark: '',
  }
}

function hydrateForm() {
  resetTestStatus()
  Object.assign(form, defaultForm(), props.extension || {}, {
    applicationId: props.application?.id,
  })
  handlerCode.value = null
  serverTestInput.value = '{}'
  clientRecordId.value = '1'
  clientTestFields.value = []
  clientAllowedActions.value = []
  clientFieldCatalog.value = []
  clientActionCatalog.value = []
  visualRule.match = 'ALL'
  visualRule.conditions.splice(0)
  visualRule.actions.splice(0)

  if (form.extensionType === 'VISUAL_RULE' && form.content) {
    try {
      const parsed = JSON.parse(form.content)
      visualRule.match = parsed.match === 'ANY' ? 'ANY' : 'ALL'
      visualRule.conditions.push(...(Array.isArray(parsed.conditions) ? parsed.conditions : []))
      visualRule.actions.push(...(Array.isArray(parsed.actions) ? parsed.actions : []))
    }
    catch {
      // 后端校验会保留错误内容；编辑器以空结构让用户修复。
    }
  }
  if (form.extensionType === 'SERVER_BINDING') {
    try {
      handlerCode.value = JSON.parse(form.configJson || '{}').handlerCode || null
    }
    catch {
      handlerCode.value = null
    }
  }
  if (form.extensionType === 'CLIENT_JS')
    syncClientContextFromScript(true)
}

function addCondition() {
  visualRule.conditions.push({ field: '', operator: 'EQ', value: '' })
}

function addAction() {
  visualRule.actions.push({ actionType: 'SHOW_MESSAGE', message: '' })
}

function applyCodeExampleContext(example) {
  if (!example || form.extensionType !== 'CLIENT_JS')
    return
  const context = example.testContext || {}
  const record = context.record && typeof context.record === 'object' ? context.record : {}
  const fieldCodes = [...new Set([
    ...(example.testFields || []),
    ...Object.keys(record).filter(fieldCode => fieldCode !== 'id'),
  ])]
  clientRecordId.value = String(record.id ?? context.recordId ?? 1)
  clientTestFields.value = fieldCodes.map(fieldCode => createClientTestField(
    fieldCode,
    Object.prototype.hasOwnProperty.call(record, fieldCode) ? record[fieldCode] : sampleClientFieldValue(fieldCode),
  ))
  clientAllowedActions.value = [...new Set(context.allowedActions || [])]
}

function resolveClientContextObjectId() {
  const entry = props.entries.find(item => String(item.id) === String(form.entryId))
  if (!entry)
    return null
  if (entry.objectId)
    return entry.objectId
  const object = props.objects.find(item => item.objectCode === entry.objectCode)
  return object?.objectId || null
}

async function loadClientContextCatalog(objectId) {
  const requestId = ++clientContextCatalogRequestId
  if (!objectId) {
    clientFieldCatalog.value = []
    clientActionCatalog.value = []
    clientContextCatalogLoading.value = false
    return
  }

  clientContextCatalogLoading.value = true
  try {
    const [fieldResult, actionResult] = await Promise.allSettled([
      businessObjectFields(objectId),
      businessObjectActions(objectId),
    ])
    if (requestId !== clientContextCatalogRequestId)
      return
    clientFieldCatalog.value = fieldResult.status === 'fulfilled'
      ? (fieldResult.value.data || []).filter(field => field?.fieldCode)
      : []
    clientActionCatalog.value = actionResult.status === 'fulfilled'
      ? (actionResult.value.data || []).filter(action => action?.actionCode && String(action.status ?? '1') !== '0')
      : []
    refreshClientFieldSamples()
  }
  finally {
    if (requestId === clientContextCatalogRequestId)
      clientContextCatalogLoading.value = false
  }
}

function syncClientContextFromScript(silent = true) {
  if (form.extensionType !== 'CLIENT_JS')
    return
  const detected = detectClientScriptBindings(form.content)
  const existingFields = new Set(clientTestFields.value.map(item => item.fieldCode).filter(Boolean))
  detected.fields.forEach((fieldCode) => {
    if (!existingFields.has(fieldCode)) {
      clientTestFields.value.push(createClientTestField(fieldCode, sampleClientFieldValue(fieldCode)))
      existingFields.add(fieldCode)
    }
  })
  clientAllowedActions.value = [...detected.actions]

  if (!silent) {
    if (!detected.fields.length && !detected.actions.length)
      message.info('当前脚本没有读取业务字段或触发页面动作，可以直接测试')
    else
      message.success(`已识别 ${detected.fields.length} 个字段、${detected.actions.length} 个页面动作`)
  }
}

function detectClientScriptBindings(source) {
  const fields = new Set()
  const actions = new Set()
  const script = String(source || '')
  const fieldPattern = /\b(?:readField|setField)\s*\(\s*(['"])([a-z]\w{0,63})\1/g
  const actionPattern = /\btriggerAction\s*\(\s*(['"])([a-z]\w{0,63})\1/g
  let match = fieldPattern.exec(script)
  while (match) {
    fields.add(match[2])
    match = fieldPattern.exec(script)
  }
  match = actionPattern.exec(script)
  while (match) {
    actions.add(match[2])
    match = actionPattern.exec(script)
  }
  return { fields: [...fields], actions: [...actions] }
}

function addClientTestField(fieldCode = '', value = '') {
  clientTestFields.value.push(createClientTestField(fieldCode, value))
}

function handleClientTestFieldChange(field, fieldCode) {
  field.fieldCode = fieldCode
  Object.assign(field, normalizeClientTestValue(sampleClientFieldValue(fieldCode)))
}

function handleClientValueTypeChange(field, valueType) {
  field.valueType = valueType
  if (valueType === 'NULL')
    field.value = ''
  else if (valueType === 'BOOLEAN')
    field.value = 'true'
  else if (valueType === 'NUMBER')
    field.value = Number.isFinite(Number(field.value)) ? String(Number(field.value)) : '1'
  else if (valueType === 'JSON')
    field.value = '{}'
  else if (valueType === 'TEXT' && typeof field.value !== 'string')
    field.value = String(field.value ?? '')
}

function createClientTestField(fieldCode, value) {
  const normalized = normalizeClientTestValue(value)
  return {
    key: `test-field-${++clientTestFieldSeed}`,
    fieldCode,
    valueType: normalized.valueType,
    value: normalized.value,
  }
}

function normalizeClientTestValue(value) {
  if (value === null)
    return { valueType: 'NULL', value: '' }
  if (typeof value === 'number')
    return { valueType: 'NUMBER', value: String(value) }
  if (typeof value === 'boolean')
    return { valueType: 'BOOLEAN', value: String(value) }
  if (value && typeof value === 'object')
    return { valueType: 'JSON', value: JSON.stringify(value) }
  return { valueType: 'TEXT', value: String(value ?? '') }
}

function sampleClientFieldValue(fieldCode) {
  const field = clientFieldCatalog.value.find(item => item.fieldCode === fieldCode)
  const fieldType = String(field?.fieldType || field?.dataType || '').toUpperCase()
  if (/BOOLEAN|BOOL/.test(fieldType))
    return true
  if (/INT|LONG|NUMBER|DECIMAL|MONEY|FLOAT|DOUBLE/.test(fieldType))
    return 1
  if (/ARRAY|LIST|MULTI/.test(fieldType))
    return []
  if (/JSON|OBJECT|MAP/.test(fieldType))
    return {}
  if (/DATETIME|DATE_TIME|TIMESTAMP/.test(fieldType))
    return `${new Date().toISOString().slice(0, 10)} 09:00:00`
  if (/DATE/.test(fieldType))
    return new Date().toISOString().slice(0, 10)
  return '示例值'
}

function refreshClientFieldSamples() {
  clientTestFields.value.forEach((field) => {
    if (field.valueType !== 'TEXT' || field.value !== '示例值')
      return
    Object.assign(field, normalizeClientTestValue(sampleClientFieldValue(field.fieldCode)))
  })
}

function applyClientValuePreset(preset) {
  if (!clientTestFields.value.length) {
    message.info('当前没有需要填写的业务字段')
    return
  }
  clientTestFields.value = clientTestFields.value.map((field) => {
    const value = preset === 'EMPTY' ? null : sampleClientFieldValue(field.fieldCode)
    return createClientTestField(field.fieldCode, value)
  })
}

function clientValuePlaceholder(valueType) {
  return {
    TEXT: '例如：草稿',
    NUMBER: '例如：68.5',
    JSON: '例如：{"name":"示例"} 或 [1,2]',
  }[valueType] || '请输入本次测试值'
}

function buildClientSandboxContext() {
  const record = {}
  const recordId = normalizeClientRecordId(clientRecordId.value)
  if (recordId !== null)
    record.id = recordId

  const allowedFields = []
  const fieldCodeSet = new Set()
  clientTestFields.value.forEach((field, index) => {
    const fieldCode = String(field.fieldCode || '').trim()
    if (!/^[a-z]\w{0,63}$/i.test(fieldCode) || clientReservedKeys.has(fieldCode))
      throw new Error(`第 ${index + 1} 行业务字段编码格式不正确`)
    if (clientSensitiveKeyPattern.test(fieldCode))
      throw new Error(`字段 ${fieldCode} 属于敏感字段，沙箱不会注入该数据`)
    if (fieldCodeSet.has(fieldCode))
      throw new Error(`业务字段 ${fieldCode} 重复，请只保留一行`)
    fieldCodeSet.add(fieldCode)
    allowedFields.push(fieldCode)
    record[fieldCode] = parseClientTestFieldValue(field)
  })

  const allowedActions = [...new Set(clientAllowedActions.value.map((actionCode) => {
    const normalized = String(actionCode || '').trim()
    if (!/^[a-z]\w{0,63}$/i.test(normalized) || clientReservedKeys.has(normalized))
      throw new Error(`页面动作编码 ${normalized || '为空'} 格式不正确`)
    return normalized
  }))]
  return {
    context: { record, allowedActions },
    allowedFields,
  }
}

function normalizeClientRecordId(value) {
  const normalized = String(value ?? '').trim()
  if (!normalized)
    return null
  if (/^-?\d+$/.test(normalized) && normalized.length < 16)
    return Number(normalized)
  return normalized
}

function parseClientTestFieldValue(field) {
  if (field.valueType === 'NULL')
    return null
  if (field.valueType === 'BOOLEAN')
    return field.value === true || String(field.value) === 'true'
  if (field.valueType === 'NUMBER') {
    if (String(field.value ?? '').trim() === '')
      throw new Error(`字段 ${field.fieldCode} 的测试值不能为空；如需测试空值请选择“空值”类型`)
    const value = Number(field.value)
    if (!Number.isFinite(value))
      throw new Error(`字段 ${field.fieldCode} 的测试值必须是数字`)
    return value
  }
  if (field.valueType === 'JSON') {
    try {
      const value = JSON.parse(String(field.value || ''))
      if (!value || typeof value !== 'object')
        throw new Error('JSON 值不是对象或数组')
      return value
    }
    catch {
      throw new Error(`字段 ${field.fieldCode} 的测试值必须是合法对象或数组 JSON`)
    }
  }
  return String(field.value ?? '')
}

async function saveCurrent(showSuccess = true) {
  await formRef.value?.validate()
  const versionPayload = buildVersionPayload()
  saving.value = true
  try {
    if (!form.id) {
      const response = await createBusinessExtension({
        ...metadataPayload(),
        ...versionPayload,
      })
      form.id = response.data
      const lockResponse = await acquireBusinessExtensionLock(form.id)
      form.lockToken = lockResponse.data?.lockToken || ''
      form.lockExpireTime = lockResponse.data?.expireTime || ''
      startRenewTimer()
      if (showSuccess)
        message.success('扩展 v1 草稿已创建')
    }
    else {
      await updateBusinessExtension({
        ...metadataPayload(),
        id: form.id,
        lockToken: form.lockToken,
      })
      await saveBusinessExtensionDraft(form.id, {
        ...versionPayload,
        lockToken: form.lockToken,
      })
      if (showSuccess)
        message.success('已追加新的扩展草稿版本')
    }
    emit('saved')
    return form.id
  }
  finally {
    saving.value = false
  }
}

async function saveAndTest() {
  testing.value = true
  completedTestStages.value = []
  testFailedStage.value = ''
  try {
    beginTestStage('SAVE', '正在保存当前扩展草稿')
    const id = await saveCurrent(false)
    completeTestStage('SAVE')

    beginTestStage('VALIDATE', '正在执行扩展内容与安全边界校验')
    const validationResponse = await validateBusinessExtension(id)
    if (!validationResponse.data?.passed) {
      const summary = validationResponse.data?.summary || '扩展校验未通过'
      failTestStage('VALIDATE', summary)
      message.warning(summary)
      return
    }
    completeTestStage('VALIDATE')

    const testPayload = { input: {} }
    if (form.extensionType === 'CLIENT_JS') {
      beginTestStage('SANDBOX', '正在独立 Worker 中执行客户端脚本')
      const { context, allowedFields } = buildClientSandboxContext()
      if (!sandboxRef.value)
        throw new Error('客户端脚本沙箱尚未初始化，请关闭抽屉后重新进入')
      await sandboxRef.value.execute(form.content, context, allowedFields)
      testPayload.clientSandboxResult = 'PASSED'
      completeTestStage('SANDBOX')
    }
    if (form.extensionType === 'SERVER_BINDING')
      testPayload.input = parseJson(serverTestInput.value, '服务端测试输入')

    beginTestStage('SERVER', '正在由后端确认测试结果和扩展版本状态')
    const testResponse = await testBusinessExtension(id, testPayload)
    if (!testResponse.data?.passed) {
      const summary = testResponse.data?.summary || '扩展测试未通过'
      failTestStage('SERVER', summary)
      message.warning(summary)
      return
    }
    completeTestStage('SERVER')
    testStage.value = 'PASSED'
    testSummary.value = '当前草稿测试通过'
    message.success('扩展校验和受限测试均已通过')
    emit('saved')
  }
  catch (error) {
    const summary = error instanceof Error ? error.message : '扩展测试执行失败'
    failTestStage(testStage.value, summary)
    message.error(summary)
  }
  finally {
    testing.value = false
  }
}

function beginTestStage(stage, summary) {
  testStage.value = stage
  testSummary.value = summary
}

function completeTestStage(stage) {
  if (!completedTestStages.value.includes(stage))
    completedTestStages.value = [...completedTestStages.value, stage]
}

function failTestStage(stage, summary) {
  testFailedStage.value = stage
  testStage.value = 'FAILED'
  testSummary.value = summary
}

function resetTestStatus() {
  testStage.value = 'IDLE'
  testFailedStage.value = ''
  testSummary.value = '点击底部“保存并测试”开始'
  completedTestStages.value = []
}

function testStepState(stage) {
  if (testStage.value === 'PASSED' || completedTestStages.value.includes(stage))
    return 'completed'
  if (testStage.value === 'FAILED' && testFailedStage.value === stage)
    return 'failed'
  if (testStage.value === stage)
    return 'running'
  return 'pending'
}

function testStepClass(stage) {
  return ['is', testStepState(stage)].join('-')
}

function testStepIcon(step) {
  const state = testStepState(step.code)
  if (state === 'completed')
    return '✓'
  if (state === 'failed')
    return '!'
  return step.order
}

function buildHandlerTestInput(handler) {
  if (!handler?.inputSchema)
    return '{}'
  const input = {}
  Object.entries(handler.inputSchema).forEach(([field, definition]) => {
    input[field] = sampleValueForType(definition?.type)
  })
  return JSON.stringify(input, null, 2)
}

function sampleValueForType(type) {
  return {
    STRING: '示例值',
    LONG: 1,
    INTEGER: 1,
    NUMBER: 1,
    DECIMAL: 1,
    BOOLEAN: true,
    OBJECT: {},
    MAP: {},
    ARRAY: [],
    LIST: [],
  }[String(type || '').toUpperCase()] ?? null
}

function metadataPayload() {
  return {
    id: form.id,
    applicationId: props.application?.id,
    objectId: form.objectId,
    entryId: form.entryId,
    extensionName: form.extensionName,
    extensionCode: form.extensionCode,
    extensionType: form.extensionType,
    hookCode: form.hookCode,
    scopeType: resolveScopeType(),
    scopeKey: form.scopeKey || null,
    sortOrder: Number(form.sortOrder || 0),
    failurePolicy: form.failurePolicy,
    riskLevel: form.riskLevel,
    remark: form.remark || null,
  }
}

function buildVersionPayload() {
  let content = form.content || ''
  let processedContent = null
  let configJson = '{}'

  if (form.extensionType === 'VISUAL_RULE') {
    if (!visualRule.actions.length)
      throw new Error('可视化规则至少需要一个动作')
    content = JSON.stringify({
      match: visualRule.match,
      conditions: visualRule.conditions,
      actions: visualRule.actions,
    })
  }
  if (form.extensionType === 'CLIENT_JS')
    validateClientScript(content)
  if (form.extensionType === 'SCOPED_CSS') {
    const scoped = processScopedCss(content, {
      applicationCode: props.application?.applicationCode || '',
      pageCode: form.scopeKey || 'default',
    })
    processedContent = scoped.css
    configJson = JSON.stringify({
      scopeSelector: scoped.scopeSelector,
      selectorCount: scoped.selectorCount,
    })
  }
  if (form.extensionType === 'SERVER_BINDING') {
    if (!handlerCode.value)
      throw new Error('请选择平台注册处理器')
    content = '{}'
    configJson = JSON.stringify({ handlerCode: handlerCode.value })
  }

  return {
    content,
    processedContent,
    configJson,
    changeSummary: form.changeSummary || null,
  }
}

function resolveScopeType() {
  if (form.extensionType === 'SCOPED_CSS')
    return 'PAGE'
  if (form.entryId)
    return 'ENTRY'
  if (form.objectId)
    return 'OBJECT'
  return 'APPLICATION'
}

function parseJson(source, label) {
  try {
    const result = JSON.parse(source || '{}')
    if (!result || Array.isArray(result) || typeof result !== 'object')
      throw new Error('JSON 根节点不是对象')
    return result
  }
  catch {
    throw new Error(`${label}必须是合法 JSON 对象`)
  }
}

function startRenewTimer() {
  clearRenewTimer()
  if (!form.id || !form.lockToken)
    return
  renewTimer = window.setInterval(async () => {
    try {
      const response = await renewBusinessExtensionLock(form.id, form.lockToken)
      form.lockExpireTime = response.data?.expireTime || form.lockExpireTime
    }
    catch {
      clearRenewTimer()
      message.warning('扩展编辑锁已失效，请关闭后重新打开')
    }
  }, 4 * 60 * 1000)
}

function clearRenewTimer() {
  if (renewTimer) {
    window.clearInterval(renewTimer)
    renewTimer = null
  }
}

function handleAfterLeave() {
  clearRenewTimer()
  emit('closed', { id: form.id, lockToken: form.lockToken })
}
</script>

<style scoped>
.lock-alert {
  margin-bottom: 14px;
}

.extension-type-guide {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
  background: var(--bg-secondary, #f7f8fa);
}

.type-guide-heading {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.type-guide-heading strong {
  color: var(--text-primary, #1d2129);
  font-size: 14px;
}

.type-guide-heading span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.type-guide-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.type-guide-card {
  display: flex;
  min-height: 96px;
  flex-direction: column;
  gap: 5px;
  padding: 11px 12px;
  cursor: pointer;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  color: var(--text-secondary, #4e5969);
  background: var(--bg-primary, #fff);
  text-align: left;
}

.type-guide-card:hover,
.type-guide-card.active {
  border-color: var(--primary-color, #165dff);
}

.type-guide-card.active {
  box-shadow: inset 3px 0 0 var(--primary-color, #165dff);
}

.type-guide-card strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
}

.type-guide-card span,
.type-guide-card small {
  font-size: 12px;
  line-height: 1.45;
}

.type-guide-card small {
  color: var(--text-tertiary, #86909c);
}

.sql-boundary-alert,
.handler-empty-alert {
  font-size: 12px;
}

.selected-extension-type {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
}

.selected-extension-type span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.editor-section {
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.editor-section:last-child {
  border-bottom: 0;
}

.editor-section h3 {
  margin: 0 0 12px;
  font-size: 14px;
}

.hook-editor-section :deep(.n-form-item-blank) {
  width: 100%;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.section-heading h3 {
  margin-bottom: 4px;
}

.section-heading p,
.security-note {
  margin: 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.security-note {
  margin-bottom: 12px;
  padding: 8px 10px;
  border-left: 3px solid var(--warning-500, #f59e0b);
  background: var(--bg-secondary, #f7f8fa);
}

.security-note code {
  color: var(--text-secondary, #4e5969);
}

.form-grid {
  display: grid;
  gap: 0 14px;
}

.two-columns {
  grid-template-columns: 1fr 1fr;
}

.rule-block {
  margin-top: 10px;
  padding: 10px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 6px;
}

.rule-block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.rule-row {
  display: grid;
  gap: 8px;
  align-items: center;
  padding: 7px 0;
  border-top: 1px solid var(--border-light, #e5e6eb);
}

.condition-row {
  grid-template-columns: 1fr 140px 1fr auto;
}

.action-row {
  grid-template-columns: 160px minmax(130px, 1fr) minmax(130px, 1fr) auto;
}

.inline-empty {
  padding: 8px 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.inline-empty.error {
  color: var(--error-600, #dc2626);
}

.page-code-field {
  width: 220px;
  margin-bottom: 0;
}

.test-context-section {
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 7px;
  background: var(--bg-primary, #fff);
}

.test-context-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.test-context-heading > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.test-context-heading strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
}

.test-context-heading span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.test-scene-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 11px 14px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  background: color-mix(in srgb, var(--primary-color, #165dff) 4%, var(--bg-primary, #fff));
}

.test-scene-summary > div:first-child {
  display: grid;
  gap: 2px;
}

.test-scene-kicker {
  color: var(--primary-color, #165dff);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.test-scene-summary strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
}

.test-scene-summary p {
  margin: 0;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.test-data-block {
  padding: 12px 14px 4px;
}

.test-data-toolbar,
.test-data-actions,
.record-id-row {
  display: flex;
  align-items: center;
}

.test-data-toolbar {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.test-data-toolbar > div:first-child {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.test-data-toolbar strong {
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
}

.test-data-toolbar span,
.record-id-row small {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.test-data-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px;
}

.record-id-row {
  display: grid;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  grid-template-columns: 92px 180px minmax(180px, 1fr);
}

.record-id-row > span {
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
}

.test-field-table {
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
}

.test-field-row {
  display: grid;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-top: 1px solid var(--border-light, #e5e6eb);
  grid-template-columns: minmax(210px, 1.2fr) 120px minmax(190px, 1fr) auto;
}

.test-field-row:first-child {
  border-top: 0;
}

.test-field-head {
  padding-top: 6px;
  padding-bottom: 6px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 11px;
}

.null-value-placeholder {
  display: flex;
  min-height: 34px;
  align-items: center;
  padding: 0 10px;
  border: 1px dashed var(--border-default, #c9cdd4);
  border-radius: 5px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 12px;
}

.test-fields-empty {
  padding: 16px 12px;
  border: 1px dashed var(--border-default, #c9cdd4);
  border-radius: 6px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  text-align: center;
  font-size: 11px;
}

.test-actions-field {
  margin-top: 12px;
  margin-bottom: 8px;
}

.test-context-advanced {
  border-top: 1px solid var(--border-light, #e5e6eb);
}

.test-context-advanced summary {
  padding: 9px 14px;
  cursor: pointer;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 11px;
  user-select: none;
}

.test-context-advanced pre {
  overflow: auto;
  max-height: 240px;
  margin: 0;
  padding: 12px 14px;
  color: var(--text-secondary, #4e5969);
  background: var(--bg-primary, #fff);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.code-feedback {
  margin-top: 12px;
}

.test-console-section {
  padding: 12px;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 7px;
  background: var(--bg-secondary, #f7f8fa);
}

.test-console-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.test-console-heading h3 {
  margin-bottom: 3px;
}

.test-console-heading p,
.test-console-heading > span {
  margin: 0;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.test-console-heading > span {
  max-width: 360px;
  text-align: right;
}

.test-step-list {
  display: grid;
  margin-top: 12px;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 7px;
}

.test-step {
  display: flex;
  align-items: center;
  min-height: 34px;
  gap: 7px;
  padding: 6px 8px;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 5px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-primary, #fff);
  font-size: 11px;
}

.test-step i {
  display: inline-flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-tertiary, #eef0f3);
  font-size: 10px;
  font-style: normal;
}

.test-step.is-running {
  border-color: var(--primary-color, #165dff);
  color: var(--primary-color, #165dff);
}

.test-step.is-running i {
  color: #fff;
  background: var(--primary-color, #165dff);
}

.test-step.is-completed {
  color: var(--success-700, #16895a);
}

.test-step.is-completed i {
  color: #fff;
  background: var(--success-600, #1eae75);
}

.test-step.is-failed {
  border-color: rgba(239, 82, 82, 0.32);
  color: var(--error-600, #c54747);
}

.test-step.is-failed i {
  color: #fff;
  background: var(--error-500, #ef5252);
}

.test-result-alert {
  margin-top: 10px;
}

.handler-contract {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  row-gap: 6px;
  margin: -4px 0 12px;
  padding: 8px 10px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 12px;
}

.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 16px;
}

.drawer-footer > span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

@media (max-width: 760px) {
  .two-columns,
  .condition-row,
  .action-row,
  .test-step-list,
  .test-field-row,
  .record-id-row {
    grid-template-columns: 1fr;
  }

  .test-field-head {
    display: none;
  }

  .test-context-heading,
  .test-scene-summary,
  .test-data-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .test-data-actions {
    justify-content: flex-start;
  }
}
</style>
