<template>
  <section class="form-init-panel">
    <!-- 区块一：初始化默认值（Session / URL 参数 → 表单字段） -->
    <section class="form-automation-block">
      <div class="form-automation-block__head">
        <div>
          <strong>初始化默认值</strong>
          <p>打开表单时，把登录用户或地址参数自动填到字段。</p>
        </div>
        <n-button size="tiny" type="primary" secondary @click="addContextDefault">
          添加默认值
        </n-button>
      </div>

      <div v-if="contextDefaults.length" class="form-init-list">
        <div v-for="item in contextDefaults" :key="item.id || item.field" class="form-init-card">
          <div class="form-init-card__main">
            <div class="form-init-card__row">
              <label>填到字段</label>
              <n-select
                :value="item.field"
                :options="fieldOptions"
                filterable
                size="small"
                placeholder="选择表单字段"
                @update:value="updateContextDefault(item.id, { field: $event || '' })"
              />
            </div>
            <div class="form-init-card__row">
              <label>取值来源</label>
              <n-select
                :value="item.source || 'CONTEXT_PATH'"
                :options="contextSourceOptions"
                size="small"
                @update:value="handleSourceChange(item, $event)"
              />
            </div>
            <div class="form-init-card__row">
              <label>{{ item.source === 'ROUTE_QUERY' ? '地址参数名' : '上下文路径' }}</label>
              <template v-if="item.source === 'CONTEXT_PATH'">
                <n-select
                  v-if="!isCustomPath(item)"
                  :value="item.path"
                  :options="contextPathOptions"
                  size="small"
                  placeholder="选择上下文取值"
                  @update:value="handleContextPathSelect(item, $event)"
                />
                <n-input
                  v-else
                  :value="item.path"
                  size="small"
                  placeholder="currentUser.userId"
                  :status="item.path && !isSafePath(item.path) ? 'error' : undefined"
                  @update:value="updateContextDefault(item.id, { path: $event })"
                />
              </template>
              <n-input
                v-else
                :value="item.path"
                size="small"
                placeholder="如 orderNo"
                :status="item.path && !isSafePath(item.path) ? 'error' : undefined"
                @update:value="updateContextDefault(item.id, { path: $event })"
              />
              <small v-if="item.path && !isSafePath(item.path)" class="form-init-field-error">
                路径只能包含字母、数字、点号和下划线
              </small>
            </div>
          </div>
          <div class="form-init-card__actions">
            <n-switch
              size="small"
              :value="item.enabled !== false"
              @update:value="updateContextDefault(item.id, { enabled: $event })"
            />
            <button type="button" class="form-init-delete" title="删除" @click="removeContextDefault(item.id)">
              ×
            </button>
          </div>
        </div>
      </div>
      <div v-else class="form-automation-empty">
        还没有初始化默认值，点击右上角添加
      </div>
    </section>

    <!-- 区块二：打开表单时加载存量数据 -->
    <section class="form-automation-block">
      <div class="form-automation-block__head form-automation-block__head--switch">
        <div>
          <strong>打开时加载存量数据</strong>
          <p>按地址参数或上下文定位记录，调用详情接口回填整表。</p>
        </div>
        <n-switch
          size="small"
          :value="recordLoad.enabled"
          @update:value="handleRecordLoadToggle"
        />
      </div>
      <div v-if="recordLoad.enabled" class="form-init-record-keys">
        <label>定位参数名</label>
        <n-dynamic-tags
          :value="recordLoad.keyNames"
          size="small"
          @update:value="updateRecordLoad({ keyNames: $event })"
        />
        <p class="form-init-help">
          依次尝试地址参数与登录上下文，命中第一个即加载；支持
          currentUser.staffId 这类路径。
        </p>
      </div>
    </section>

    <p class="form-init-tip">
      打开后要调数据集或外部接口回填？在下方「字段自动查询」中配置，时机选「表单打开后」。
    </p>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { isSafePath } from '@/components/ai-form/data-source-binding-runtime'
import {
  CUSTOM_PATH_OPTION,
  RUNTIME_CONTEXT_PATH_OPTIONS,
  withCustomPathOption,
} from '@/components/ai-form/runtime-context-options'
import { useFormDesignerStore } from '@/store'

/**
 * 表单初始化配置面板（数据源统一架构 P0）
 *
 * 配置存储在 schema.settings.governance.formInit：
 * - contextDefaults：Session / URL 参数 → 表单字段默认值（零请求，打开即生效）
 * - recordLoad：按定位参数解析记录 ID，走既有详情接口加载存量数据
 * - 打开后的自动查询不在此重复建设，复用 fieldEvents 中 trigger=FORM_LOAD 的规则
 *
 * 面板直接读写 Pinia store（AGENTS.md 5.14），不再经过 props / emit 透传。
 */
const designerStore = useFormDesignerStore()

const contextSourceOptions = [
  { label: '登录用户等上下文', value: 'CONTEXT_PATH' },
  { label: '页面地址参数', value: 'ROUTE_QUERY' },
]
// 常用上下文取值项与运行时 buildFormRuntimeContext 产出严格对齐（见 runtime-context-options.js），
// 禁止写顶层 tenantId / activeOrgId：真实结构在 currentUser 内部，顶层路径取不到值
const contextPathOptions = computed(() => withCustomPathOption(RUNTIME_CONTEXT_PATH_OPTIONS))

const rawFormInitConfig = computed(() => {
  const config = designerStore.formGovernanceSettings?.formInit
  return config && typeof config === 'object' ? config : {}
})
const contextDefaults = computed(() => (
  Array.isArray(rawFormInitConfig.value.contextDefaults) ? rawFormInitConfig.value.contextDefaults : []
))
const recordLoad = computed(() => {
  const source = rawFormInitConfig.value.recordLoad
  return {
    enabled: source?.enabled === true,
    keyNames: Array.isArray(source?.keyNames) ? source.keyNames : [],
  }
})
const fieldOptions = computed(() => collectBoundFieldOptions(designerStore.schema?.components || []))

// 与 ForgePropertyPanel.collectBoundFieldOptions 同语义：收集画布上全部已绑定字段
function collectBoundFieldOptions(components = [], result = []) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    const field = component?.fieldBinding?.fieldCode || component?.field || component?.props?.field
    if (field && !result.some(item => item.value === field)) {
      result.push({
        label: `${component.label || field}（${field}）`,
        value: field,
      })
    }
    collectBoundFieldOptions(component?.children || component?.props?.children || [], result)
  })
  return result
}

function updateFormInit(patch = {}) {
  designerStore.updateGovernance({
    formInit: {
      ...rawFormInitConfig.value,
      ...patch,
    },
  })
}

function addContextDefault() {
  updateFormInit({
    contextDefaults: [
      ...contextDefaults.value,
      { id: `ctx_default_${Date.now()}`, field: '', source: 'CONTEXT_PATH', path: '', enabled: true },
    ],
  })
}

function updateContextDefault(id, patch = {}) {
  const list = contextDefaults.value.map(item => (item.id === id ? { ...item, ...patch } : item))
  updateFormInit({ contextDefaults: list })
}

function removeContextDefault(id) {
  updateFormInit({ contextDefaults: contextDefaults.value.filter(item => item.id !== id) })
}

function handleSourceChange(item, source) {
  const nextSource = source || 'CONTEXT_PATH'
  // 切换来源时原路径大概率失效，清空让用户重新选，避免跨形态残留
  updateContextDefault(item.id, { source: nextSource, path: '' })
}

function isCustomPath(item) {
  return item.path !== '' && !contextPathOptions.value.some(option => option.value === item.path)
}

function handleContextPathSelect(item, value) {
  if (value === CUSTOM_PATH_OPTION)
    updateContextDefault(item.id, { path: '' })
  else
    updateContextDefault(item.id, { path: value })
}

function updateRecordLoad(patch = {}) {
  updateFormInit({
    recordLoad: {
      ...recordLoad.value,
      ...patch,
    },
  })
}

function handleRecordLoadToggle(enabled) {
  updateRecordLoad({
    enabled,
    // 首次开启时预置常用定位参数，减少一次手动输入
    keyNames: enabled && !recordLoad.value.keyNames.length ? ['recordId', 'id'] : recordLoad.value.keyNames,
  })
}
</script>

<style scoped>
.form-init-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-init-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-init-card {
  display: flex;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e8eaed;
  border-radius: 8px;
  background: #fafbfc;
}

.form-init-card__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.form-init-card__row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-init-card__row label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
}

.form-init-card__actions {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding-top: 2px;
}

.form-init-delete {
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  padding: 2px 4px;
}

.form-init-delete:hover {
  color: var(--n-error-color, #d03050);
}

.form-init-field-error {
  color: var(--n-error-color, #d03050);
  font-size: 11px;
}

.form-init-record-keys {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 2px;
}

.form-init-record-keys label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
}

.form-init-help {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.5;
}

.form-init-tip {
  margin: 0;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f4f6fa;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.55;
}
</style>
