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
              :source-object-id="draft.sourceObjectId"
              :business-object-options="businessObjectOptions"
              @request-low-code-mapping="openLowCodeMapping"
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

    <n-modal
      :show="mappingModalVisible"
      preset="card"
      title="低代码字段映射"
      style="width: min(620px, calc(100vw - 32px))"
      :bordered="false"
      :mask-closable="true"
      @update:show="handleMappingModalVisibility"
    >
      <div class="mapping-dialog">
        <div class="mapping-dialog__context">
          <i class="i-material-symbols:account-tree-outline" />
          <div>
            <strong>配置第 {{ mappingTargetSegment?.segmentOrder || '—' }} 段</strong>
            <span>选择后，该分段将从低代码业务记录的对应字段取值。</span>
          </div>
        </div>

        <n-alert
          v-if="mappingObjectChanged && mappingOtherLowCodeCount"
          type="warning"
          :bordered="false"
          title="来源对象将变更"
        >
          一条规则的低代码变量共用同一对象。确认后将清空其它 {{ mappingOtherLowCodeCount }} 个分段的旧字段映射。
        </n-alert>

        <n-form label-placement="top" :show-feedback="false" class="mapping-dialog__form">
          <n-form-item label="来源业务对象" required>
            <n-select
              :value="mappingDraft.sourceObjectId"
              :options="businessObjectOptions"
              filterable
              clearable
              placeholder="选择低代码业务对象"
              @update:value="handleMappingObjectChange"
            />
          </n-form-item>
          <n-form-item label="映射字段" required>
            <n-select
              v-model:value="mappingDraft.fieldCode"
              :options="mappingFieldOptions"
              :loading="mappingLoading"
              :disabled="!mappingDraft.sourceObjectId || mappingLoading"
              filterable
              clearable
              placeholder="选择已启用的非系统字段"
            />
          </n-form-item>
        </n-form>

        <div v-if="mappingSelectedField?.description" class="mapping-dialog__field-note">
          <i class="i-material-symbols:info-outline-rounded" />
          <span>{{ mappingSelectedField.description }}</span>
        </div>
      </div>

      <template #footer>
        <div class="mapping-dialog__footer">
          <n-button @click="closeMappingModal">
            取消
          </n-button>
          <n-button
            type="primary"
            :disabled="!canConfirmMapping"
            :loading="mappingLoading"
            @click="confirmLowCodeMapping"
          >
            确认映射
          </n-button>
        </div>
      </template>
    </n-modal>
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
  applyLowCodeVariableMapping,
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
const mappingModalVisible = ref(false)
const mappingLoading = ref(false)
const mappingFieldOptions = ref([])
const mappingObjectCapabilities = ref(null)
const mappingTargetSegmentKey = ref(null)
const mappingDraft = reactive({
  sourceObjectId: null,
  fieldCode: null,
})
const draft = reactive(createEmptyCodeRuleDraft())
const sampleFields = reactive({})
let previewTimer = null
const detailRequestGuard = createLatestRequestGuard()
const previewRequestGuard = createLatestRequestGuard()
const capabilityRequestGuard = createLatestRequestGuard()
const mappingRequestGuard = createLatestRequestGuard()

const isBuiltin = computed(() => Number(draft.builtin) === 1)
const workspaceTitle = computed(() => props.ruleId
  ? `编辑 · ${draft.ruleName || draft.ruleCode || '加载中'}`
  : '新增编码规则')
const validation = computed(() => validateCodeRuleDraft(draft))
const variableSegments = computed(() => draft.segments.filter(segment => segment.segmentType === 'VARIABLE'))
const lowCodeVariableSegments = computed(() => variableSegments.value
  .filter(segment => segment.variableSource === 'LOWCODE'))
const effectiveCapabilities = computed(() => ({
  ...props.capabilities,
  ...(objectCapabilities.value || {}),
}))
const businessObjectOptions = computed(() => effectiveCapabilities.value.businessObjects || [])
const mappingTargetSegment = computed(() => draft.segments
  .find(segment => segment.segmentKey === mappingTargetSegmentKey.value))
const mappingOtherLowCodeCount = computed(() => draft.segments.filter(segment => (
  segment.segmentKey !== mappingTargetSegmentKey.value
  && segment.segmentType === 'VARIABLE'
  && segment.variableSource === 'LOWCODE'
  && segment.segmentValue
)).length)
const mappingObjectChanged = computed(() => Boolean(mappingDraft.sourceObjectId)
  && String(mappingDraft.sourceObjectId) !== String(draft.sourceObjectId || ''))
const mappingSelectedField = computed(() => mappingFieldOptions.value
  .find(option => option.value === mappingDraft.fieldCode))
const canConfirmMapping = computed(() => Boolean(
  mappingDraft.sourceObjectId
  && mappingDraft.fieldCode
  && !mappingLoading.value,
))

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
  if (!lowCodeVariableSegments.value.length) {
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
  mappingRequestGuard.invalidate()
})

function resetDraft(value = createEmptyCodeRuleDraft()) {
  Object.keys(draft).forEach(key => delete draft[key])
  Object.assign(draft, JSON.parse(JSON.stringify(value)))
  draft.sampleSequence = Number(draft.sampleSequence ?? 1)
  const hasLegacyObjectBinding = Boolean(draft.sourceObjectId)
  draft.segments = normalizeCodeRuleSegments((draft.segments || []).map(segment => (
    hasLegacyObjectBinding && segment.segmentType === 'VARIABLE' && !segment.variableSource
      ? { ...segment, variableSource: 'LOWCODE' }
      : segment
  )))
  draft.sourceObjectId = draft.sourceObjectId ? String(draft.sourceObjectId) : null
  preview.value = null
}

async function initializeDraft() {
  closeMappingModal()
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
    await loadBusinessFields(lowCodeVariableSegments.value.length ? draft.sourceObjectId : null)
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
    sourceObjectId: lowCodeVariableSegments.value.length ? (draft.sourceObjectId || null) : null,
    status: Number(draft.status) === 0 ? 0 : 1,
    inCodeList: Number(draft.inCodeList) === 0 ? 0 : 1,
    remark: draft.remark || null,
    segments: validation.value.segments,
  }
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

async function openLowCodeMapping(segmentKey) {
  const segment = draft.segments.find(item => item.segmentKey === segmentKey)
  if (!segment || segment.segmentType !== 'VARIABLE')
    return

  mappingRequestGuard.invalidate()
  mappingTargetSegmentKey.value = segmentKey
  mappingDraft.sourceObjectId = draft.sourceObjectId ? String(draft.sourceObjectId) : null
  mappingDraft.fieldCode = segment.variableSource === 'LOWCODE' ? segment.segmentValue : null
  mappingFieldOptions.value = mappingDraft.sourceObjectId
    ? [...(effectiveCapabilities.value.businessFields || [])]
    : []
  mappingObjectCapabilities.value = mappingDraft.sourceObjectId ? objectCapabilities.value : null
  mappingModalVisible.value = true
  if (mappingDraft.sourceObjectId)
    await loadMappingFields(mappingDraft.sourceObjectId, mappingDraft.fieldCode)
}

async function handleMappingObjectChange(value) {
  const sourceObjectId = value ? String(value) : null
  mappingDraft.sourceObjectId = sourceObjectId
  mappingDraft.fieldCode = null
  await loadMappingFields(sourceObjectId, null)
}

async function loadMappingFields(sourceObjectId, preferredFieldCode) {
  mappingRequestGuard.invalidate()
  mappingObjectCapabilities.value = null
  if (!sourceObjectId) {
    mappingFieldOptions.value = []
    mappingLoading.value = false
    return
  }

  const requestVersion = mappingRequestGuard.begin()
  const keepCurrentOptions = String(sourceObjectId) === String(draft.sourceObjectId || '')
  if (!keepCurrentOptions)
    mappingFieldOptions.value = []
  mappingLoading.value = true
  try {
    const res = await systemCodeRuleCapabilities({ sourceObjectId: String(sourceObjectId) })
    if (!mappingRequestGuard.isLatest(requestVersion)
      || !mappingModalVisible.value
      || String(mappingDraft.sourceObjectId || '') !== String(sourceObjectId)) {
      return
    }
    mappingObjectCapabilities.value = res.data || null
    mappingFieldOptions.value = res.data?.businessFields || []
    const resolvedFieldCode = preferredFieldCode || mappingDraft.fieldCode
    mappingDraft.fieldCode = mappingFieldOptions.value.some(option => option.value === resolvedFieldCode)
      ? resolvedFieldCode
      : null
  }
  finally {
    if (mappingRequestGuard.isLatest(requestVersion))
      mappingLoading.value = false
  }
}

function confirmLowCodeMapping() {
  if (!canConfirmMapping.value) {
    message.warning('请选择来源业务对象和映射字段')
    return
  }
  try {
    const result = applyLowCodeVariableMapping(
      draft.segments,
      mappingTargetSegmentKey.value,
      {
        sourceObjectId: mappingDraft.sourceObjectId,
        fieldCode: mappingDraft.fieldCode,
      },
      draft.sourceObjectId,
    )
    draft.sourceObjectId = result.sourceObjectId
    draft.sourceObjectCode = null
    draft.segments = result.segments
    if (mappingObjectCapabilities.value)
      objectCapabilities.value = mappingObjectCapabilities.value
    if (result.clearedSegmentKeys.length)
      message.warning(`已清空其它 ${result.clearedSegmentKeys.length} 个低代码字段映射，请重新配置`)
    closeMappingModal()
  }
  catch (error) {
    message.warning(error?.message || '低代码字段映射失败')
  }
}

function handleMappingModalVisibility(show) {
  if (!show)
    closeMappingModal()
}

function closeMappingModal() {
  mappingRequestGuard.invalidate()
  mappingModalVisible.value = false
  mappingLoading.value = false
  mappingTargetSegmentKey.value = null
  mappingDraft.sourceObjectId = null
  mappingDraft.fieldCode = null
  mappingFieldOptions.value = []
  mappingObjectCapabilities.value = null
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
  closeMappingModal()
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

.form-field-with-help {
  width: 100%;
}

.form-field-with-help > span {
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

.mapping-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mapping-dialog__context {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 11px;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 8px;
  background: var(--bg-secondary, #f7f8fa);
}

.mapping-dialog__context > i {
  color: var(--primary-color, #4242f7);
  font-size: 24px;
}

.mapping-dialog__context > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.mapping-dialog__context strong {
  color: var(--text-primary, #1d2129);
  font-size: 14px;
}

.mapping-dialog__context span,
.mapping-dialog__field-note {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.mapping-dialog__form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.mapping-dialog__field-note {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: -8px;
}

.mapping-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 640px) {
  .mapping-dialog__form {
    grid-template-columns: 1fr;
  }
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
