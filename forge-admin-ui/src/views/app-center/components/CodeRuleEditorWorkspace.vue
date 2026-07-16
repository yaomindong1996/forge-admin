<template>
  <section class="code-rule-workspace">
    <header class="workspace-header">
      <div class="workspace-header__identity">
        <n-button quaternary circle title="返回编码规则列表" @click="closeWorkspace">
          <template #icon>
            <i class="i-material-symbols:arrow-back-rounded" />
          </template>
        </n-button>
        <div>
          <div class="workspace-header__eyebrow">
            编码规则配置
          </div>
          <h2>{{ workspaceTitle }}</h2>
          <p>按顺序设计编码分段，并在右侧确认最终生成结果。</p>
        </div>
      </div>
      <div class="workspace-header__actions">
        <n-tag v-if="isBuiltin" type="info" :bordered="false">
          内置规则
        </n-tag>
        <n-button @click="closeWorkspace">
          返回列表
        </n-button>
        <n-button
          type="primary"
          :loading="saving"
          :disabled="loading || !validation.valid"
          @click="saveRule"
        >
          保存规则
        </n-button>
      </div>
    </header>

    <div class="workspace-scroll">
      <n-spin :show="loading">
        <div class="code-rule-editor">
          <main class="code-rule-editor__main">
            <section class="editor-section editor-section--basic">
              <header class="editor-section__header">
                <div>
                  <h3>基础信息</h3>
                  <p>规则编码用于业务字段绑定；创建后不可修改。</p>
                </div>
                <n-tag v-if="Number(draft.builtin) === 1" type="info" :bordered="false">
                  内置规则
                </n-tag>
              </header>

              <n-form label-placement="top" :show-feedback="false">
                <div class="basic-form-grid">
                  <n-form-item label="规则编码" required>
                    <n-input
                      v-model:value="draft.ruleCode"
                      placeholder="如 purchase_order_no"
                      :disabled="Boolean(draft.id)"
                    />
                  </n-form-item>
                  <n-form-item label="规则名称" required>
                    <n-input v-model:value="draft.ruleName" placeholder="如 采购单号" />
                  </n-form-item>
                  <n-form-item label="编码分类" required>
                    <n-select
                      v-model:value="draft.category"
                      :options="categoryOptions"
                      :disabled="isBuiltin"
                    />
                  </n-form-item>
                  <n-form-item label="适用场景">
                    <div class="form-field-with-help">
                      <n-select
                        v-model:value="draft.scene"
                        :options="sceneOptions"
                        placeholder="请选择适用场景"
                        :disabled="isBuiltin"
                      />
                      <span>用于兼容旧调用方的场景筛选；低代码字段的精确范围由业务对象绑定控制。</span>
                    </div>
                  </n-form-item>
                  <n-form-item label="字段来源业务对象" class="basic-form-grid__wide">
                    <div class="source-object-field">
                      <n-select
                        :value="draft.sourceObjectId"
                        :options="businessObjectOptions"
                        :loading="businessFieldLoading"
                        :disabled="isBuiltin || !variableSegments.length"
                        filterable
                        clearable
                        placeholder="先添加业务变量段，再选择低代码业务对象"
                        @update:value="handleSourceObjectChange"
                      />
                      <span>
                        仅业务变量段需要绑定。绑定后只能选择该对象中已启用的非系统字段，运行时也会校验当前对象。
                      </span>
                    </div>
                  </n-form-item>
                  <n-form-item label="启用状态">
                    <div class="switch-field">
                      <n-switch
                        :value="Number(draft.status) === 1"
                        @update:value="value => draft.status = value ? 1 : 0"
                      />
                      <span>{{ Number(draft.status) === 1 ? '启用' : '停用' }}</span>
                    </div>
                  </n-form-item>
                  <n-form-item label="业务字段可选择">
                    <div class="switch-field">
                      <n-switch
                        :value="Number(draft.inCodeList) === 1"
                        :disabled="isBuiltin"
                        @update:value="value => draft.inCodeList = value ? 1 : 0"
                      />
                      <span>{{ Number(draft.inCodeList) === 1 ? '可选择' : '仅内部使用' }}</span>
                    </div>
                  </n-form-item>
                  <n-form-item label="规则说明" class="basic-form-grid__wide">
                    <n-input
                      v-model:value="draft.remark"
                      type="textarea"
                      :autosize="{ minRows: 2, maxRows: 4 }"
                      maxlength="500"
                      show-count
                      placeholder="说明编码用途、唯一范围和业务注意事项"
                    />
                  </n-form-item>
                </div>
              </n-form>
            </section>

            <CodeRuleSegmentEditor
              v-model="draft.segments"
              :capabilities="effectiveCapabilities"
              :disabled="isBuiltin"
              :source-object-selected="Boolean(draft.sourceObjectId)"
              :business-fields-loading="businessFieldLoading"
            />

            <n-alert
              v-if="validation.errors.length"
              type="error"
              title="配置尚未完成"
              :bordered="false"
            >
              {{ validation.errors[0] }}
            </n-alert>
            <n-alert
              v-else-if="validation.warnings.length"
              type="warning"
              title="配置提示"
              :bordered="false"
            >
              {{ validation.warnings[0] }}
            </n-alert>
          </main>

          <aside class="code-rule-editor__preview">
            <section class="preview-card">
              <header class="preview-card__header">
                <div>
                  <span>实时预览</span>
                  <small>不会消耗真实流水号</small>
                </div>
                <n-button
                  quaternary
                  circle
                  size="small"
                  :loading="previewing"
                  title="刷新预览"
                  @click="refreshPreview"
                >
                  <template #icon>
                    <i class="i-material-symbols:refresh-rounded" />
                  </template>
                </n-button>
              </header>

              <div class="preview-card__sample">
                <span>编码结果</span>
                <strong :class="{ 'is-invalid': preview?.valid === false }">
                  {{ preview?.previewCode || '等待有效配置' }}
                </strong>
                <code>{{ preview?.formatExpression || '—' }}</code>
              </div>

              <div class="preview-card__metrics">
                <div>
                  <span>实际长度</span>
                  <strong>{{ preview?.totalLength ?? validation.totalLength }}</strong>
                </div>
                <div>
                  <span>示例流水</span>
                  <strong>{{ preview?.sequence ?? draft.sampleSequence }}</strong>
                </div>
                <div>
                  <span>重置周期</span>
                  <strong>{{ preview?.period || 'all' }}</strong>
                </div>
              </div>

              <div class="preview-card__sequence-input">
                <label>预览流水值</label>
                <n-input-number
                  v-model:value="draft.sampleSequence"
                  :min="0"
                  :show-button="false"
                />
              </div>

              <div v-if="variableSegments.length" class="preview-card__variables">
                <div class="preview-card__subhead">
                  业务变量样例
                </div>
                <label v-for="segment in variableSegments" :key="segment.segmentKey">
                  <span>{{ segment.segmentValue }}</span>
                  <n-input
                    v-model:value="sampleFields[segment.segmentValue]"
                    size="small"
                    :placeholder="`输入 ${segment.segmentValue} 示例值`"
                  />
                </label>
              </div>

              <div v-if="preview?.segmentPreviews?.length" class="preview-card__segments">
                <div class="preview-card__subhead">
                  分段解析
                </div>
                <div
                  v-for="segment in preview.segmentPreviews"
                  :key="segment.segmentKey"
                  class="preview-segment"
                >
                  <span>{{ segment.segmentOrder }} · {{ segment.segmentType }}</span>
                  <code>{{ segment.value || '∅' }}</code>
                </div>
              </div>

              <n-alert
                v-if="preview?.errors?.length"
                type="error"
                :bordered="false"
                class="preview-card__alert"
              >
                {{ preview.errors[0].message }}
              </n-alert>
              <n-alert
                v-else-if="preview?.warnings?.length"
                type="warning"
                :bordered="false"
                class="preview-card__alert"
              >
                {{ preview.warnings[0].message }}
              </n-alert>
            </section>
          </aside>
        </div>
      </n-spin>
    </div>
  </section>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import {
  addSystemCodeRule,
  editSystemCodeRule,
  previewSystemCodeRule,
  systemCodeRuleCapabilities,
  systemCodeRuleDetail,
} from '@/api/business-app'
import {
  buildCodeRulePreviewPayload,
  createEmptyCodeRuleDraft,
  createLatestRequestGuard,
  normalizeCodeRuleSegments,
  validateCodeRuleDraft,
} from '../code-rule-utils'
import CodeRuleSegmentEditor from './CodeRuleSegmentEditor.vue'

const props = defineProps({
  ruleId: {
    type: [Number, String],
    default: null,
  },
  capabilities: {
    type: Object,
    default: () => ({}),
  },
  categoryOptions: {
    type: Array,
    default: () => [],
  },
  sceneOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['close', 'saved'])
const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const previewing = ref(false)
const preview = ref(null)
const objectCapabilities = ref(null)
const businessFieldLoading = ref(false)
const draft = reactive(createEmptyCodeRuleDraft())
const sampleFields = reactive({})
let previewTimer = null
const detailRequestGuard = createLatestRequestGuard()
const previewRequestGuard = createLatestRequestGuard()
const capabilityRequestGuard = createLatestRequestGuard()

const isBuiltin = computed(() => Number(draft.builtin) === 1)
const workspaceTitle = computed(() => props.ruleId
  ? `编辑 · ${draft.ruleName || draft.ruleCode || '加载中'}`
  : '新增编码规则')
const validation = computed(() => validateCodeRuleDraft(draft))
const variableSegments = computed(() => draft.segments.filter(segment => segment.segmentType === 'VARIABLE'))
const effectiveCapabilities = computed(() => ({
  ...props.capabilities,
  ...(objectCapabilities.value || {}),
}))
const businessObjectOptions = computed(() => effectiveCapabilities.value.businessObjects || [])

watch(() => props.ruleId, async () => {
  await initializeDraft()
}, { immediate: true })

watch(variableSegments, (segments) => {
  const activeFields = new Set(segments.map(segment => segment.segmentValue).filter(Boolean))
  Object.keys(sampleFields).forEach((key) => {
    if (!activeFields.has(key))
      delete sampleFields[key]
  })
  activeFields.forEach((key) => {
    if (sampleFields[key] === undefined)
      sampleFields[key] = key.toUpperCase().slice(0, 8)
  })
  if (!segments.length) {
    draft.sourceObjectId = null
    draft.sourceObjectCode = null
    objectCapabilities.value = null
    capabilityRequestGuard.invalidate()
    businessFieldLoading.value = false
  }
}, { immediate: true, deep: true })

watch([draft, sampleFields], () => schedulePreview(), { deep: true })

onBeforeUnmount(() => {
  clearTimeout(previewTimer)
  detailRequestGuard.invalidate()
  previewRequestGuard.invalidate()
  capabilityRequestGuard.invalidate()
})

function resetDraft(value = createEmptyCodeRuleDraft()) {
  Object.keys(draft).forEach(key => delete draft[key])
  Object.assign(draft, JSON.parse(JSON.stringify(value)))
  draft.sampleSequence = Number(draft.sampleSequence ?? 1)
  draft.segments = normalizeCodeRuleSegments(draft.segments || [])
  draft.sourceObjectId = draft.sourceObjectId ? String(draft.sourceObjectId) : null
  preview.value = null
}

async function initializeDraft() {
  const requestVersion = detailRequestGuard.begin()
  const requestedRuleId = props.ruleId
  loading.value = true
  try {
    if (!requestedRuleId) {
      resetDraft()
      return
    }
    const res = await systemCodeRuleDetail(requestedRuleId)
    if (!detailRequestGuard.isLatest(requestVersion)
      || String(props.ruleId) !== String(requestedRuleId)) {
      return
    }
    resetDraft({
      ...res.data,
      sampleSequence: 1,
      segments: res.data?.segments || [],
    })
    await loadBusinessFields(draft.sourceObjectId)
  }
  finally {
    if (detailRequestGuard.isLatest(requestVersion)) {
      loading.value = false
      schedulePreview(0)
    }
  }
}

function schedulePreview(delay = 450) {
  clearTimeout(previewTimer)
  previewRequestGuard.invalidate()
  if (loading.value || !validation.value.valid) {
    previewing.value = false
    preview.value = null
    return
  }
  previewTimer = setTimeout(refreshPreview, delay)
}

async function refreshPreview() {
  clearTimeout(previewTimer)
  if (!validation.value.valid)
    return
  const requestVersion = previewRequestGuard.begin()
  const payload = buildCodeRulePreviewPayload(draft, sampleFields)
  previewing.value = true
  try {
    const res = await previewSystemCodeRule(payload)
    if (previewRequestGuard.isLatest(requestVersion))
      preview.value = res.data || null
  }
  finally {
    if (previewRequestGuard.isLatest(requestVersion))
      previewing.value = false
  }
}

function savePayload() {
  return {
    id: draft.id || null,
    versionNo: draft.versionNo ?? null,
    ruleCode: draft.ruleCode,
    ruleName: draft.ruleName,
    scene: draft.scene || 'COMMON',
    category: draft.category,
    sourceObjectId: draft.sourceObjectId || null,
    status: Number(draft.status) === 0 ? 0 : 1,
    inCodeList: Number(draft.inCodeList) === 0 ? 0 : 1,
    remark: draft.remark || null,
    segments: validation.value.segments,
  }
}

async function handleSourceObjectChange(value) {
  const nextObjectId = value ? String(value) : null
  if (String(draft.sourceObjectId || '') === String(nextObjectId || ''))
    return
  draft.sourceObjectId = nextObjectId
  draft.sourceObjectCode = null
  draft.segments = draft.segments.map(segment => segment.segmentType === 'VARIABLE'
    ? { ...segment, segmentValue: '' }
    : segment)
  await loadBusinessFields(nextObjectId)
}

async function loadBusinessFields(sourceObjectId) {
  capabilityRequestGuard.invalidate()
  objectCapabilities.value = null
  if (!sourceObjectId) {
    businessFieldLoading.value = false
    return
  }
  const requestVersion = capabilityRequestGuard.begin()
  businessFieldLoading.value = true
  try {
    const res = await systemCodeRuleCapabilities({ sourceObjectId: String(sourceObjectId) })
    if (capabilityRequestGuard.isLatest(requestVersion)
      && String(draft.sourceObjectId || '') === String(sourceObjectId)) {
      objectCapabilities.value = res.data || null
    }
  }
  finally {
    if (capabilityRequestGuard.isLatest(requestVersion))
      businessFieldLoading.value = false
  }
}

async function saveRule() {
  if (!validation.value.valid) {
    message.warning(validation.value.errors[0])
    return
  }
  saving.value = true
  try {
    if (draft.id)
      await editSystemCodeRule(savePayload())
    else
      await addSystemCodeRule(savePayload())
    message.success(draft.id ? '编码规则已更新' : '编码规则已创建')
    emit('saved')
  }
  finally {
    saving.value = false
  }
}

function closeWorkspace() {
  detailRequestGuard.invalidate()
  previewRequestGuard.invalidate()
  capabilityRequestGuard.invalidate()
  emit('close')
}
</script>

<style scoped>
.code-rule-workspace {
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  color: var(--text-primary, #1d2129);
  background: var(--bg-secondary, #f7f8fa);
}

.workspace-header {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 72px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-primary, #fff);
}

.workspace-header__identity,
.workspace-header__actions {
  display: flex;
  align-items: center;
}

.workspace-header__identity {
  gap: 10px;
  min-width: 0;
}

.workspace-header__identity > div {
  min-width: 0;
}

.workspace-header__eyebrow {
  margin-bottom: 2px;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.workspace-header h2 {
  overflow: hidden;
  margin: 0;
  color: var(--text-primary, #1d2129);
  font-size: 17px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-header p {
  margin: 3px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.workspace-header__actions {
  flex: none;
  gap: 10px;
}

.workspace-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px;
}

.code-rule-editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  width: 100%;
  max-width: 1600px;
  min-height: 100%;
  margin: 0 auto;
  color: var(--text-primary, #1d2129);
}

.code-rule-editor__main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.editor-section {
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 10px;
  background: var(--bg-primary, #fff);
  padding: 18px;
}

.editor-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.editor-section__header h3 {
  margin: 0;
  color: var(--text-primary, #1d2129);
  font-size: 15px;
}

.editor-section__header p {
  margin: 5px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.basic-form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 16px;
}

.basic-form-grid__wide {
  grid-column: 1 / -1;
}

.form-field-with-help,
.source-object-field {
  width: 100%;
}

.form-field-with-help > span,
.source-object-field > span {
  display: block;
  margin-top: 6px;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1.5;
}

.switch-field {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 34px;
  color: var(--text-secondary, #4e5969);
  font-size: 13px;
}

.code-rule-editor__preview {
  position: relative;
}

.preview-card {
  position: sticky;
  top: 0;
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 10px;
  background: var(--bg-primary, #fff);
}

.preview-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.preview-card__header div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.preview-card__header span {
  color: var(--text-primary, #1d2129);
  font-weight: 650;
}

.preview-card__header small {
  color: var(--text-tertiary, #86909c);
}

.preview-card__sample {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 20px 16px;
  text-align: center;
  background: var(--bg-secondary, #f7f8fa);
}

.preview-card__sample > span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.preview-card__sample strong {
  overflow-wrap: anywhere;
  color: var(--primary-color, #4242f7);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 22px;
  letter-spacing: 1px;
}

.preview-card__sample strong.is-invalid {
  color: var(--error-500, #ef4444);
}

.preview-card__sample code {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.preview-card__metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-top: 1px solid var(--border-light, #e5e6eb);
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.preview-card__metrics div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 11px 5px;
  border-right: 1px solid var(--border-light, #e5e6eb);
}

.preview-card__metrics div:last-child {
  border-right: 0;
}

.preview-card__metrics span {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.preview-card__metrics strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
}

.preview-card__sequence-input,
.preview-card__variables,
.preview-card__segments {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.preview-card__sequence-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preview-card__sequence-input label,
.preview-card__subhead {
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
  font-weight: 650;
}

.preview-card__variables label {
  display: block;
  margin-top: 10px;
}

.preview-card__variables label span {
  display: block;
  margin-bottom: 5px;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.preview-segment {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 9px;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.preview-segment code {
  overflow: hidden;
  color: var(--text-primary, #1d2129);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-card__alert {
  margin: 12px;
}

@media (max-width: 1180px) {
  .code-rule-editor {
    grid-template-columns: 1fr;
  }

  .basic-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .preview-card {
    position: static;
  }
}

@media (max-width: 720px) {
  .workspace-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .workspace-header__actions {
    justify-content: flex-end;
    width: 100%;
  }

  .basic-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
