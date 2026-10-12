<script setup>
import { computed, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { request } from '@/utils/request'
import WidgetFieldPathPicker from './WidgetFieldPathPicker.vue'
import {
  isBindingSlotVisible,
  listAssignedBindingSlots,
  resolveWidgetBindingProfile,
  resolveWidgetRenderMode,
} from './widget-binding-slots'
import {
  buildRemoteFieldSelectOptions,
  extractJsonFieldPaths,
  normalizeRemoteProbeParams,
  resolveProbeSlotSource,
} from './remote-binding-probe'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) },
  fields: { type: Array, default: () => [] },
  formDesignerSchema: { type: Object, default: null },
  /** 当前组件类型，用于只展示本组件可赋值槽位 */
  blockType: { type: String, default: '' },
  showMappingFields: { type: Boolean, default: true },
})

const emit = defineEmits(['update:modelValue'])
const message = useMessage()

const sourceTypeOptions = [
  { label: '静态配置', value: 'static' },
  { label: '当前详情/表单数据', value: 'context' },
  { label: '远程接口', value: 'remote' },
]
const requestMethodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' },
  { label: 'PUT', value: 'put' },
  { label: 'DELETE', value: 'delete' },
]
const displayModeOptions = [
  { label: '单个字段', value: 'single' },
  { label: '组合显示', value: 'compose' },
]

const showAdvancedNestPath = ref(false)
const remoteProbeLoading = ref(false)
const remoteProbeError = ref('')
const remoteProbeRaw = ref(null)
const remoteProbeExpanded = ref(false)

const binding = computed(() => props.modelValue || {})
const sourceType = computed(() => binding.value.sourceType || 'static')
const profile = computed(() => resolveWidgetBindingProfile(props.blockType))
const slots = computed(() => profile.value.slots || [])
const supportsCompose = computed(() => profile.value.supportsCompose === true)
const supportsRenderMode = computed(() => profile.value.supportsRenderMode === true)
const renderMode = computed(() => resolveWidgetRenderMode(binding.value, props.blockType))
const renderModeOptions = computed(() => profile.value.renderModeOptions || [])
const needsContextPath = computed(() => {
  if (supportsRenderMode.value)
    return renderMode.value === 'list'
  return profile.value.needsContextPath === true || profile.value.kind === 'list'
})
const showListSlotMapping = computed(() => (
  showSlotMapping.value && (!supportsRenderMode.value || renderMode.value === 'list')
))
const showManualHint = computed(() => supportsRenderMode.value && renderMode.value === 'manual' && sourceType.value !== 'static')
const contextPathLabel = computed(() => profile.value.contextPathLabel || '列表字段')
const contextPathHelp = computed(() => profile.value.contextPathHelp || '')
const primarySlot = computed(() => (
  slots.value.find(item => item.primary)
  || slots.value.find(item => item.key === profile.value.primarySlot)
  || slots.value[0]
  || null
))
const displayMode = computed(() => {
  if (!supportsCompose.value)
    return 'single'
  if (binding.value.displayMode === 'compose')
    return 'compose'
  if (Array.isArray(binding.value.displayFields) && binding.value.displayFields.length)
    return 'compose'
  if (String(binding.value.displayTemplate || '').trim())
    return 'compose'
  return 'single'
})
const displayFields = computed(() => (
  Array.isArray(binding.value.displayFields)
    ? binding.value.displayFields.map(item => String(item || '').trim()).filter(Boolean)
    : []
))
const hasFields = computed(() => Array.isArray(props.fields) && props.fields.length > 0)
const assignedSlots = computed(() => listAssignedBindingSlots(binding.value, props.blockType, props.fields))
const canPickField = computed(() => hasFields.value && (sourceType.value === 'context' || sourceType.value === 'remote'))
const showSlotMapping = computed(() => (
  props.showMappingFields
  && sourceType.value !== 'static'
  && displayMode.value === 'single'
))
const hasNestPath = computed(() => Boolean(String(binding.value.contextPath || '').trim()))

const remoteDataPathOptions = computed(() => {
  if (!remoteProbeRaw.value)
    return []
  const paths = extractJsonFieldPaths(remoteProbeRaw.value, { includeContainers: true })
  return [
    { label: '整份响应（不截取）', value: '' },
    ...buildRemoteFieldSelectOptions(paths.filter(item => ['array', 'object'].includes(item.type) || !item.path.includes('.'))),
    ...buildRemoteFieldSelectOptions(paths),
  ].filter((item, index, list) => list.findIndex(row => row.value === item.value) === index)
})

const remoteSlotFieldOptions = computed(() => {
  if (!remoteProbeRaw.value)
    return []
  const slotSource = resolveProbeSlotSource(remoteProbeRaw.value, binding.value.dataPath || '')
  if (!slotSource)
    return []
  const paths = extractJsonFieldPaths(slotSource, { includeContainers: false })
  return buildRemoteFieldSelectOptions(paths, { onlyLeaves: true })
})

const remoteProbePreviewText = computed(() => {
  if (!remoteProbeRaw.value)
    return ''
  try {
    return JSON.stringify(remoteProbeRaw.value, null, 2)
  }
  catch {
    return String(remoteProbeRaw.value)
  }
})

watch(sourceType, (value) => {
  if (value !== 'remote') {
    remoteProbeError.value = ''
    remoteProbeRaw.value = null
  }
})

function patch(partial = {}) {
  const next = {
    ...binding.value,
    ...partial,
  }
  if (Object.prototype.hasOwnProperty.call(partial, 'sourceType')) {
    next.enabled = partial.sourceType !== 'static'
    next.sourceType = partial.sourceType || 'static'
    if (supportsRenderMode.value && !next.renderMode)
      next.renderMode = 'manual'
  }
  emit('update:modelValue', next)
}

function setDisplayMode(mode) {
  if (mode === 'compose') {
    patch({
      displayMode: 'compose',
      displayFields: displayFields.value.length ? displayFields.value : [],
    })
    return
  }
  patch({
    displayMode: 'single',
    displayFields: [],
    displayTemplate: '',
  })
}

function patchSlot(slotKey, value) {
  patch({ [slotKey]: value || '' })
}

function setSlotVisible(slotKey, visible) {
  const nextHidden = {
    ...(binding.value.slotHidden || {}),
  }
  if (visible)
    delete nextHidden[slotKey]
  else
    nextHidden[slotKey] = true
  patch({ slotHidden: nextHidden })
}

function isSlotVisible(slotKey) {
  return isBindingSlotVisible(binding.value, slotKey)
}

function toggleDisplayField(path, checked) {
  const next = new Set(displayFields.value)
  if (checked)
    next.add(path)
  else
    next.delete(path)
  patch({
    displayMode: 'compose',
    displayFields: [...next],
  })
}

function moveDisplayField(path, offset) {
  const list = [...displayFields.value]
  const index = list.indexOf(path)
  if (index < 0)
    return
  const target = index + offset
  if (target < 0 || target >= list.length)
    return
  const [item] = list.splice(index, 1)
  list.splice(target, 0, item)
  patch({ displayFields: list })
}

function slotValue(slotKey) {
  return binding.value[slotKey] || ''
}

function parseProbeApi(api = '', method = 'get') {
  const text = String(api || '').trim()
  if (!text)
    return { method: String(method || 'get').toLowerCase(), url: '' }
  if (text.includes('@')) {
    const [rawMethod, ...urlParts] = text.split('@')
    return {
      method: String(rawMethod || method || 'get').toLowerCase(),
      url: urlParts.join('@'),
    }
  }
  return {
    method: String(method || 'get').toLowerCase(),
    url: text,
  }
}

async function testRemoteBinding() {
  const api = String(binding.value.api || '').trim()
  if (!api) {
    message.warning('请先填写接口地址')
    return
  }
  const { method, url } = parseProbeApi(api, binding.value.method || 'get')
  if (!url) {
    message.warning('接口地址无效')
    return
  }
  remoteProbeLoading.value = true
  remoteProbeError.value = ''
  try {
    const params = normalizeRemoteProbeParams(binding.value.paramsText || '{}')
    const response = await request({
      url,
      method,
      ...(method === 'get' ? { params } : { data: params }),
      needTip: false,
    })
    // 与运行时一致：优先取 RespInfo.data，否则整包
    const payload = response?.data ?? response
    remoteProbeRaw.value = payload
    remoteProbeExpanded.value = true
    message.success('接口测试成功，可在下方点选字段')
  }
  catch (error) {
    remoteProbeRaw.value = null
    remoteProbeError.value = error?.message || '接口请求失败'
    message.error(remoteProbeError.value)
  }
  finally {
    remoteProbeLoading.value = false
  }
}

function applySuggestedDataPath(path) {
  patch({ dataPath: path || '', enabled: true, sourceType: 'remote' })
}

function suggestArrayDataPath() {
  if (!remoteProbeRaw.value)
    return
  const arrays = extractJsonFieldPaths(remoteProbeRaw.value)
    .filter(item => item.type === 'array')
  if (!arrays.length) {
    message.info('响应里没有检测到数组字段')
    return
  }
  applySuggestedDataPath(arrays[0].path)
}
</script>

<template>
  <div class="widget-data-binding-editor">
    <div class="field-help">
      静态配置用下方「组件内容」。选「当前详情/表单数据」后给本组件各项点选字段；页面暂无详情时预览会用字段示例值。
    </div>

    <div class="data-source-row">
      <span>来源类型</span>
      <n-select
        :value="sourceType"
        :options="sourceTypeOptions"
        @update:value="patch({ sourceType: $event || 'static' })"
      />
    </div>

    <template v-if="supportsRenderMode && sourceType !== 'static'">
      <div class="data-source-row">
        <span>渲染方式</span>
        <n-select
          :value="renderMode"
          :options="renderModeOptions.map(item => ({ label: item.label, value: item.value }))"
          @update:value="patch({
            renderMode: $event || 'manual',
            enabled: true,
            sourceType: sourceType === 'static' ? 'context' : sourceType,
          })"
        />
      </div>
      <div class="field-help">
        {{ renderModeOptions.find(item => item.value === renderMode)?.help || '' }}
      </div>
      <div v-if="showManualHint" class="field-help">
        手动多项：下方「指标项」里增删卡片；每项可写静态值，也可在该项上点选字段。不会被列表循环覆盖。
      </div>
    </template>

    <template v-if="sourceType === 'context'">
      <div v-if="supportsCompose" class="data-source-row">
        <span>显示方式</span>
        <n-select
          :value="displayMode"
          :options="displayModeOptions"
          @update:value="setDisplayMode"
        />
      </div>

      <template v-if="displayMode === 'compose'">
        <div class="compose-panel">
          <div class="field-help">
            组合结果写入「{{ primarySlot?.label || '主值' }}」。勾选字段可调顺序，也可用模板如
            <code>{customerName}（{orderNo}）</code>。
          </div>
          <div v-if="hasFields" class="compose-field-list">
            <label
              v-for="field in fields.filter(item => item.nodeType !== 'widget')"
              :key="field.fieldCode || field.field"
              class="compose-field-item"
            >
              <n-checkbox
                :checked="displayFields.includes(field.fieldCode || field.field)"
                @update:checked="checked => toggleDisplayField(field.fieldCode || field.field, checked)"
              />
              <span class="compose-field-label">{{ field.label || field.fieldName || field.fieldCode || field.field }}</span>
              <code>{{ field.fieldCode || field.field }}</code>
              <span v-if="displayFields.includes(field.fieldCode || field.field)" class="compose-field-actions">
                <n-button size="tiny" quaternary @click.stop="moveDisplayField(field.fieldCode || field.field, -1)">
                  上
                </n-button>
                <n-button size="tiny" quaternary @click.stop="moveDisplayField(field.fieldCode || field.field, 1)">
                  下
                </n-button>
              </span>
            </label>
          </div>
          <n-empty v-else size="small" description="还没有可勾选的字段，请先在「数据」页关联表单或业务对象" />

          <div class="data-source-row">
            <span>分隔符</span>
            <n-input
              :value="binding.displaySeparator ?? ' '"
              placeholder="例如空格、/、-"
              @update:value="patch({ displaySeparator: $event ?? ' ' })"
            />
          </div>
          <div class="data-source-row">
            <span>显示模板</span>
            <n-input
              :value="binding.displayTemplate || ''"
              clearable
              placeholder="可选。填写后优先按模板渲染，如 {name} - {code}"
              @update:value="patch({ displayTemplate: $event || '' })"
            />
          </div>
          <div v-if="displayFields.length" class="compose-preview">
            已选（{{ primarySlot?.label || '主值' }}）：{{ displayFields.join(' + ') }}
          </div>
        </div>

        <div v-if="slots.filter(s => !s.primary).length" class="slot-panel">
          <div class="slot-panel-title">其它可赋值项</div>
          <div
            v-for="slot in slots.filter(s => !s.primary)"
            :key="slot.key"
            class="slot-row"
          >
            <div class="slot-row-head">
              <span>{{ slot.label }}</span>
              <label class="slot-visible">
                <n-switch
                  size="small"
                  :value="isSlotVisible(slot.key)"
                  @update:value="setSlotVisible(slot.key, $event)"
                />
                <em>显示</em>
              </label>
            </div>
            <WidgetFieldPathPicker
              v-if="canPickField && isSlotVisible(slot.key)"
              :value="slotValue(slot.key)"
              :fields="fields"
              :form-designer-schema="formDesignerSchema"
              :placeholder="slot.placeholder || slot.label"
              @update:value="patchSlot(slot.key, $event)"
            />
            <n-input
              v-else-if="isSlotVisible(slot.key)"
              :value="slotValue(slot.key)"
              clearable
              :placeholder="slot.placeholder || slot.label"
              @update:value="patchSlot(slot.key, $event)"
            />
          </div>
        </div>
      </template>

      <template v-else>
        <!-- 列表类组件：必须选数组字段 -->
        <template v-if="needsContextPath">
          <div class="data-source-row">
            <span>{{ contextPathLabel }}</span>
            <WidgetFieldPathPicker
              v-if="hasFields"
              :value="binding.contextPath || ''"
              :fields="fields"
              :form-designer-schema="formDesignerSchema"
              include-collections
              placeholder="选择子表/数组字段"
              @update:value="patch({ contextPath: $event || '' })"
            />
            <n-input
              v-else
              :value="binding.contextPath || ''"
              clearable
              placeholder="如 metrics / tags"
              @update:value="patch({ contextPath: $event || '' })"
            />
          </div>
          <div v-if="contextPathHelp" class="field-help">
            {{ contextPathHelp }}
          </div>
        </template>

        <!-- 单值组件：默认不用；高级才展开「数据在对象里」 -->
        <template v-else-if="profile.kind === 'object'">
          <button
            type="button"
            class="advanced-toggle"
            @click="showAdvancedNestPath = !showAdvancedNestPath"
          >
            {{ showAdvancedNestPath || hasNestPath ? '收起' : '展开' }}：数据在嵌套对象里（一般不用）
          </button>
          <template v-if="showAdvancedNestPath || hasNestPath">
            <div class="data-source-row">
              <span>对象路径</span>
              <WidgetFieldPathPicker
                v-if="hasFields"
                :value="binding.contextPath || ''"
                :fields="fields"
                :form-designer-schema="formDesignerSchema"
                include-collections
                placeholder="例如 summary，表示从 record.summary 取值"
                @update:value="patch({ contextPath: $event || '' })"
              />
              <n-input
                v-else
                :value="binding.contextPath || ''"
                clearable
                placeholder="例如 summary"
                @update:value="patch({ contextPath: $event || '' })"
              />
            </div>
            <div class="field-help">
              仅当标题/数值等字段都挂在同一个嵌套对象下时才填，例如数据是
              <code>{ summary: { title, amount } }</code>。绝大多数情况留空即可，直接从整条记录点选字段。
            </div>
          </template>
        </template>
      </template>
    </template>

    <template v-if="sourceType === 'remote'">
      <div class="data-source-row">
        <span>接口地址</span>
        <n-input
          :value="binding.api || ''"
          clearable
          placeholder="例如 /api/order/detail/{{ id }}"
          @update:value="patch({ api: $event || '' })"
        />
      </div>
      <div class="data-source-row">
        <span>请求方式</span>
        <n-select
          :value="binding.method || 'get'"
          :options="requestMethodOptions"
          @update:value="patch({ method: $event || 'get' })"
        />
      </div>
      <div class="data-source-row">
        <span>响应路径</span>
        <n-select
          v-if="remoteDataPathOptions.length > 1"
          :value="binding.dataPath || ''"
          :options="remoteDataPathOptions"
          filterable
          tag
          clearable
          placeholder="测试后可点选，如 data / data.list"
          @update:value="applySuggestedDataPath"
        />
        <n-input
          v-else
          :value="binding.dataPath || 'data'"
          clearable
          placeholder="如 data、data.records"
          @update:value="patch({ dataPath: $event || 'data' })"
        />
      </div>
      <div class="data-source-row">
        <span>请求参数</span>
        <n-input
          :value="binding.paramsText || '{}'"
          type="textarea"
          :rows="3"
          placeholder='{ "id": "{{ id }}" }，可引用当前详情数据'
          @update:value="patch({ paramsText: $event || '{}' })"
        />
      </div>
      <div class="remote-probe-toolbar">
        <n-button size="small" type="primary" secondary :loading="remoteProbeLoading" @click="testRemoteBinding">
          测试接口
        </n-button>
        <n-button
          v-if="remoteProbeRaw && needsContextPath"
          size="small"
          quaternary
          @click="suggestArrayDataPath"
        >
          选用第一个数组路径
        </n-button>
        <n-button
          v-if="remoteProbeRaw"
          size="small"
          quaternary
          @click="remoteProbeExpanded = !remoteProbeExpanded"
        >
          {{ remoteProbeExpanded ? '收起结果' : '查看结果' }}
        </n-button>
      </div>
      <n-alert
        v-if="remoteProbeError"
        type="error"
        :bordered="false"
        class="field-empty-alert"
        :title="remoteProbeError"
      />
      <div v-if="remoteProbeRaw && remoteProbeExpanded" class="remote-probe-result">
        <div class="slot-panel-title">
          接口返回（可据此选字段）
        </div>
        <pre>{{ remoteProbePreviewText }}</pre>
      </div>
      <div v-if="remoteProbeRaw && remoteSlotFieldOptions.length" class="field-help">
        已解析 {{ remoteSlotFieldOptions.length }} 个可映射字段；下方槽位可直接下拉选择。
      </div>
    </template>

    <template v-if="showListSlotMapping">
      <div class="slot-panel">
        <div class="slot-panel-title">
          {{ needsContextPath ? '每条记录字段映射' : '本组件可赋值项' }}
          <span class="slot-panel-hint">
            {{ sourceType === 'remote'
              ? (remoteSlotFieldOptions.length ? '点选接口返回字段' : '可手填字段名，或先点「测试接口」自动提取')
              : needsContextPath
                ? (hasNestPath ? '字段相对上方数组；点选即可' : '请先选择上方数组字段')
                : '点选字段；不需要的项关掉「显示」' }}
          </span>
        </div>
        <div
          v-for="slot in slots"
          :key="slot.key"
          class="slot-row"
        >
          <div class="slot-row-head">
            <span>
              {{ slot.label }}
              <em v-if="slot.primary" class="slot-primary-tag">主值</em>
            </span>
            <label class="slot-visible">
              <n-switch
                size="small"
                :value="isSlotVisible(slot.key)"
                @update:value="setSlotVisible(slot.key, $event)"
              />
              <em>显示</em>
            </label>
          </div>
          <n-select
            v-if="sourceType === 'remote' && isSlotVisible(slot.key) && remoteSlotFieldOptions.length"
            :value="slotValue(slot.key)"
            :options="remoteSlotFieldOptions"
            filterable
            tag
            clearable
            :placeholder="slot.placeholder || slot.label"
            @update:value="patchSlot(slot.key, $event)"
          />
          <WidgetFieldPathPicker
            v-else-if="canPickField && sourceType === 'context' && isSlotVisible(slot.key) && (!needsContextPath || hasNestPath)"
            :value="slotValue(slot.key)"
            :fields="fields"
            :form-designer-schema="formDesignerSchema"
            :only-under="needsContextPath ? (binding.contextPath || '') : ''"
            :placeholder="slot.placeholder || slot.label"
            @update:value="patchSlot(slot.key, $event)"
          />
          <n-input
            v-else-if="isSlotVisible(slot.key) && (sourceType === 'remote' || !needsContextPath || hasNestPath || sourceType !== 'context')"
            :value="slotValue(slot.key)"
            clearable
            :placeholder="slot.placeholder || slot.label"
            @update:value="patchSlot(slot.key, $event)"
          />
          <div v-else-if="needsContextPath && sourceType === 'context' && !hasNestPath" class="slot-hidden-tip">
            先选「{{ contextPathLabel }}」后再映射本项
          </div>
          <div v-else class="slot-hidden-tip">
            已隐藏，预览中不展示此项
          </div>
        </div>
        <div v-if="assignedSlots.length" class="compose-preview">
          已选：{{ assignedSlots.map(item => `${item.label}=${item.path}`).join('；') }}
        </div>
        <div v-else class="compose-preview muted">
          尚未绑定字段时，下方「组件内容」静态值作占位；绑定后预览用字段示例/真实详情。
        </div>
      </div>
    </template>

    <n-alert
      v-if="sourceType === 'context' && !hasFields"
      type="warning"
      :bordered="false"
      class="field-empty-alert"
      title="还没有字段可选"
    >
      请先到右侧「数据」页关联表单或业务对象；关联后这里会列出可渲染字段（含主表/子表）。
    </n-alert>
  </div>
</template>

<style scoped>
.widget-data-binding-editor {
  display: grid;
  gap: 10px;
  width: 100%;
}
.field-help {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}
.field-help code {
  padding: 0 4px;
  border-radius: 4px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}
.data-source-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}
.data-source-row > span {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.3;
}
.slot-panel {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #fafbfc;
}
.slot-panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}
.slot-panel-hint {
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 400;
}
.slot-row {
  display: grid;
  gap: 6px;
}
.slot-row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
}
.slot-visible {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.slot-visible em {
  font-style: normal;
  color: var(--text-tertiary);
  font-size: 12px;
}
.slot-hidden-tip {
  color: #c9cdd4;
  font-size: 12px;
}
.slot-primary-tag {
  margin-left: 4px;
  padding: 0 4px;
  border-radius: 4px;
  background: #e8f3ff;
  color: #165dff;
  font-style: normal;
  font-size: 11px;
}
.advanced-toggle {
  justify-self: start;
  padding: 0;
  border: 0;
  background: transparent;
  color: #165dff;
  font-size: 12px;
  cursor: pointer;
}
.compose-panel {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #fafbfc;
}
.compose-field-list {
  display: grid;
  gap: 6px;
  max-height: 220px;
  overflow: auto;
}
.compose-field-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--bg-primary);
  border: 1px solid #eef0f3;
  cursor: pointer;
}
.compose-field-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
  font-size: 13px;
}
.compose-field-item code {
  color: var(--text-tertiary);
  font-size: 11px;
}
.compose-field-actions {
  display: inline-flex;
  gap: 2px;
}
.compose-preview {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}
.compose-preview.muted {
  color: var(--text-tertiary);
}
.field-empty-alert {
  margin-top: 2px;
}
.remote-probe-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.remote-probe-result {
  display: grid;
  gap: 6px;
  padding: 8px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #0f172a;
}
.remote-probe-result .slot-panel-title {
  color: #e2e8f0;
}
.remote-probe-result pre {
  margin: 0;
  max-height: 220px;
  overflow: auto;
  color: #cbd5e1;
  font-size: 11px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
