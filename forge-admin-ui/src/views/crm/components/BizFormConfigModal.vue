<template>
  <!-- 老 CRM 风格：预定义勾选表 + 自定义字段维护（含排序 / AiForm 类型） -->
  <n-modal
    v-model:show="visible"
    preset="card"
    :title="`配置表单 · ${detail.templateName || objectType}`"
    style="width:min(1080px,96vw)"
    :mask-closable="false"
    :close-on-esc="false"
  >
    <n-spin :show="loading">
      <n-tabs v-model:value="activeTab" type="line" size="small">
        <n-tab-pane name="system" tab="预定义字段">
          <div class="cfg-table-wrap">
            <table class="cfg-table">
              <thead>
                <tr>
                  <th style="width:88px">
                    排序
                  </th>
                  <th>原始名称</th>
                  <th>显示文字</th>
                  <th>启用</th>
                  <th>列表显示</th>
                  <th>必填</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, idx) in systemRows" :key="row._key">
                  <td>
                    <div class="sort-actions">
                      <a class="cursor-pointer text-primary" :class="{ disabled: idx === 0 }" @click="moveRow(row, -1)">
                        <i class="i-lucide:arrow-up" />
                        上移
                      </a>
                      <a class="cursor-pointer text-primary" :class="{ disabled: idx >= systemRows.length - 1 }" @click="moveRow(row, 1)">
                        <i class="i-lucide:arrow-down" />
                        下移
                      </a>
                    </div>
                  </td>
                  <td>{{ row.fieldLabel }}<span class="code">{{ row.fieldCode }}</span></td>
                  <td>
                    <n-input v-model:value="row.displayLabelDraft" size="small" maxlength="100" placeholder="可改显示名" />
                  </td>
                  <td>
                    <n-checkbox v-model:checked="row.enabledBool" :disabled="row.locked === 1" />
                  </td>
                  <td>
                    <n-checkbox v-model:checked="row.listVisibleBool" />
                  </td>
                  <td>
                    <n-checkbox v-model:checked="row.requiredBool" :disabled="row.locked === 1" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </n-tab-pane>
        <n-tab-pane name="custom" tab="自定义字段">
          <div class="custom-toolbar">
            <NButton size="small" type="primary" secondary @click="addCustom">
              <template #icon>
                <i class="i-lucide:plus" />
              </template>
              新增扩展字段
            </NButton>
            <span class="hint">自定义字段写入扩展存储（CRM 单据 ext_json / 平台侧表）；类型对齐 AiForm</span>
          </div>
          <div class="cfg-table-wrap">
            <table class="cfg-table">
              <thead>
                <tr>
                  <th style="width:88px">
                    排序
                  </th>
                  <th>显示文字</th>
                  <th>编码</th>
                  <th style="min-width:160px">
                    类型
                  </th>
                  <th>启用</th>
                  <th>必填</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <template v-for="(row, idx) in customRows" :key="row._key">
                  <tr>
                    <td>
                      <div class="sort-actions">
                        <a class="cursor-pointer text-primary" :class="{ disabled: idx === 0 }" @click="moveRow(row, -1)">
                          <i class="i-lucide:arrow-up" />
                          上移
                        </a>
                        <a class="cursor-pointer text-primary" :class="{ disabled: idx >= customRows.length - 1 }" @click="moveRow(row, 1)">
                          <i class="i-lucide:arrow-down" />
                          下移
                        </a>
                      </div>
                    </td>
                    <td>
                      <n-input
                        v-model:value="row.fieldLabel"
                        size="small"
                        maxlength="100"
                        @update:value="onCustomLabel(row)"
                      />
                    </td>
                    <td>
                      <n-input v-model:value="row.fieldCode" size="small" maxlength="64" :disabled="Boolean(row.fieldId)" />
                    </td>
                    <td>
                      <n-select
                        v-model:value="row.componentType"
                        size="small"
                        filterable
                        :options="typeOptions"
                        @update:value="onTypeChange(row)"
                      />
                    </td>
                    <td>
                      <n-checkbox v-model:checked="row.enabledBool" />
                    </td>
                    <td>
                      <n-checkbox v-model:checked="row.requiredBool" />
                    </td>
                    <td>
                      <a class="cursor-pointer text-error" @click="removeCustom(idx)">删除</a>
                    </td>
                  </tr>
                  <tr v-if="needsDict(row) || needsOptions(row)" class="cfg-extra-row">
                    <td colspan="7">
                      <div v-if="needsDict(row)" class="cfg-extra-block">
                        <span class="cfg-extra-label">字典类型</span>
                        <n-input v-model:value="row.dictType" size="small" maxlength="64" placeholder="如 sys_yes_no" style="max-width:280px" />
                      </div>
                      <div v-if="needsOptions(row)" class="cfg-extra-block">
                        <div class="cfg-extra-head">
                          <span class="cfg-extra-label">选项</span>
                          <NButton size="tiny" secondary @click="addOptionRow(row)">
                            添加选项
                          </NButton>
                        </div>
                        <table class="opt-table">
                          <thead>
                            <tr>
                              <th>显示文字</th>
                              <th>值</th>
                              <th v-if="isTreeType(row)">
                                父级值
                              </th>
                              <th style="width:56px" />
                            </tr>
                          </thead>
                          <tbody>
                            <tr v-for="(opt, optIdx) in row.optionRows" :key="opt.key">
                              <td>
                                <n-input v-model:value="opt.label" size="tiny" maxlength="100" />
                              </td>
                              <td>
                                <n-input v-model:value="opt.value" size="tiny" maxlength="100" />
                              </td>
                              <td v-if="isTreeType(row)">
                                <n-input v-model:value="opt.parentValue" size="tiny" maxlength="100" placeholder="可空=根" />
                              </td>
                              <td>
                                <a class="cursor-pointer text-error" @click="row.optionRows.splice(optIdx, 1)">删</a>
                              </td>
                            </tr>
                            <tr v-if="!row.optionRows?.length">
                              <td :colspan="isTreeType(row) ? 4 : 3" class="empty">
                                请添加至少一个选项
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </td>
                  </tr>
                </template>
                <tr v-if="!customRows.length">
                  <td colspan="7" class="empty">
                    暂无扩展字段，可点击上方新增
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </n-tab-pane>
      </n-tabs>
    </n-spin>
    <template #footer>
      <CrmActionBar variant="footer" label="" aria-label="表单配置操作">
        <NButton secondary @click="visible = false">
          取消
        </NButton>
        <NButton type="primary" :loading="saving" @click="save">
          保存配置
        </NButton>
      </CrmActionBar>
    </template>
  </n-modal>
</template>

<script setup>
import { NButton } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { bizFormTemplateDefault, saveBizFormTemplateDefault } from '@/api/crm/bizForm'
import { nameToCode } from '@/utils/name-to-code'
import {
  bizFormTypeOptions,
  buildOptionsTree,
  flattenOptionsTree,
  parseOptionsJson,
  serializeOptions,
  TYPES_NEED_DICT,
  TYPES_NEED_OPTIONS,
  TYPES_TREE_OPTIONS,
  validateCustomFieldType,
} from './bizFormFieldTypes'
import CrmActionBar from './CrmActionBar.vue'

const props = defineProps({
  objectType: { type: String, required: true },
})
const emit = defineEmits(['saved'])
const visible = defineModel('show', { type: Boolean, default: false })

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('system')
const detail = ref({})
const rows = ref([])
const removed = ref([])
let keySeq = 1
let optKeySeq = 1

const typeOptions = bizFormTypeOptions

const systemRows = computed(() => rows.value.filter(r => r.fieldKind === 'SYSTEM' && !r._removed))
const customRows = computed(() => rows.value.filter(r => r.fieldKind === 'CUSTOM' && !r._removed))

watch(visible, async (open) => {
  if (open)
    await load()
})

async function load() {
  loading.value = true
  removed.value = []
  try {
    const res = await bizFormTemplateDefault(props.objectType)
    detail.value = res.data || {}
    rows.value = (detail.value.fields || []).map(decorate)
  }
  catch (e) {
    window.$message?.error?.(e?.message || '加载表单配置失败')
    visible.value = false
  }
  finally {
    loading.value = false
  }
}

function decorate(item) {
  const optionRows = flattenOptionsTree(parseOptionsJson(item.optionsJson)).map(row => ({
    ...row,
    key: row.key || `opt-${optKeySeq++}`,
  }))
  return {
    ...item,
    _key: `f-${item.fieldId || keySeq++}`,
    _removed: false,
    displayLabelDraft: item.displayLabel || item.fieldLabel || '',
    enabledBool: item.enabled === 1,
    requiredBool: item.required === 1,
    listVisibleBool: item.listVisible === 1,
    locked: item.locked === 1 ? 1 : 0,
    dictType: item.dictType || '',
    optionRows,
  }
}

function needsDict(row) {
  return TYPES_NEED_DICT.has(String(row.componentType || ''))
}

function needsOptions(row) {
  return TYPES_NEED_OPTIONS.has(String(row.componentType || ''))
}

function isTreeType(row) {
  return TYPES_TREE_OPTIONS.has(String(row.componentType || ''))
}

function onTypeChange(row) {
  if (needsOptions(row) && (!row.optionRows || !row.optionRows.length)) {
    row.optionRows = [
      { key: `opt-${optKeySeq++}`, label: '选项1', value: '1', parentValue: '' },
      { key: `opt-${optKeySeq++}`, label: '选项2', value: '2', parentValue: '' },
    ]
  }
  if (!needsDict(row))
    row.dictType = row.dictType || ''
}

function addOptionRow(row) {
  if (!row.optionRows)
    row.optionRows = []
  row.optionRows.push({ key: `opt-${optKeySeq++}`, label: '', value: '', parentValue: '' })
}

function addCustom() {
  rows.value.push(decorate({
    fieldId: null,
    fieldCode: '',
    fieldKind: 'CUSTOM',
    fieldLabel: '',
    componentType: 'input',
    sectionCode: 'extra',
    enabled: 1,
    required: 0,
    listVisible: 0,
    listSortable: 0,
    commonlyUsed: 0,
    sortOrder: 900 + rows.value.length,
    optionsJson: null,
    dictType: '',
  }))
  activeTab.value = 'custom'
}

function onCustomLabel(row) {
  if (!row.fieldId && row.fieldLabel)
    row.fieldCode = nameToCode(row.fieldLabel)
}

function removeCustom(visibleIndex) {
  const row = customRows.value[visibleIndex]
  if (!row)
    return
  row._removed = true
  if (row.fieldId)
    removed.value.push(row)
}

/** 在同 kind 可见行中上下移，改写底层 rows 顺序 */
function moveRow(row, delta) {
  const kind = row.fieldKind
  const visible = rows.value.filter(r => r.fieldKind === kind && !r._removed)
  const from = visible.indexOf(row)
  const to = from + delta
  if (from < 0 || to < 0 || to >= visible.length)
    return
  const fromIdx = rows.value.indexOf(row)
  const toIdx = rows.value.indexOf(visible[to])
  if (fromIdx < 0 || toIdx < 0)
    return
  const next = rows.value.slice()
  next.splice(fromIdx, 1)
  next.splice(toIdx, 0, row)
  rows.value = next
}

function reindexSortOrders() {
  let systemOrder = 10
  rows.value.filter(r => r.fieldKind === 'SYSTEM' && !r._removed).forEach((row) => {
    row.sortOrder = systemOrder
    systemOrder += 10
  })
  let customOrder = 900
  rows.value.filter(r => r.fieldKind === 'CUSTOM' && !r._removed).forEach((row) => {
    row.sortOrder = customOrder
    customOrder += 10
  })
}

function syncOptionsJson(row) {
  if (!needsOptions(row)) {
    row.optionsJson = null
    return
  }
  const tree = isTreeType(row)
    ? buildOptionsTree(row.optionRows || [])
    : (row.optionRows || []).map(opt => ({ label: opt.label, value: opt.value }))
  row.optionsJson = serializeOptions(tree)
}

async function save() {
  reindexSortOrders()
  const payloadFields = []
  for (const row of rows.value) {
    if (row._removed)
      continue
    if (row.fieldKind === 'CUSTOM') {
      if (!String(row.fieldLabel || '').trim()) {
        window.$message?.warning?.('请填写自定义字段显示名')
        activeTab.value = 'custom'
        return
      }
      if (!String(row.fieldCode || '').trim()) {
        window.$message?.warning?.('请填写自定义字段编码')
        activeTab.value = 'custom'
        return
      }
      syncOptionsJson(row)
      const typeError = validateCustomFieldType(row)
      if (typeError) {
        window.$message?.warning?.(typeError)
        activeTab.value = 'custom'
        return
      }
    }
    payloadFields.push({
      fieldId: row.fieldId,
      fieldCode: row.fieldCode,
      fieldKind: row.fieldKind,
      fieldLabel: row.fieldKind === 'SYSTEM'
        ? (row.displayLabelDraft || row.fieldLabel)
        : row.fieldLabel,
      componentType: row.componentType || 'input',
      dictType: row.dictType || null,
      optionsJson: row.optionsJson,
      sectionCode: row.sectionCode || 'extra',
      enabled: row.enabledBool ? 1 : 0,
      required: row.requiredBool ? 1 : 0,
      listVisible: row.listVisibleBool ? 1 : 0,
      listSortable: row.listSortable === 1 ? 1 : 0,
      commonlyUsed: row.commonlyUsed === 1 ? 1 : 0,
      displayLabel: row.fieldKind === 'SYSTEM' ? (row.displayLabelDraft || null) : null,
      sortOrder: row.sortOrder,
    })
  }
  for (const row of removed.value) {
    payloadFields.push({
      fieldId: row.fieldId,
      fieldCode: row.fieldCode,
      fieldKind: 'CUSTOM',
      fieldLabel: row.fieldLabel || row.fieldCode,
      componentType: row.componentType || 'input',
      enabled: 0,
      required: 0,
      remove: true,
    })
  }
  // 按 sortOrder 提交，保证后端回退序号时也正确
  payloadFields.sort((a, b) => {
    if (a.remove)
      return 1
    if (b.remove)
      return -1
    return (a.sortOrder || 0) - (b.sortOrder || 0)
  })
  saving.value = true
  try {
    const res = await saveBizFormTemplateDefault({
      objectType: props.objectType,
      version: detail.value.version,
      templateName: detail.value.templateName,
      formEnabled: detail.value.formEnabled,
      fields: payloadFields,
    })
    if (res.code === 200) {
      window.$message?.success?.('表单配置已保存，当前录入页将立即生效')
      visible.value = false
      emit('saved')
    }
  }
  finally {
    saving.value = false
  }
}
</script>

<style scoped>
.cfg-table-wrap {
  max-height: min(56vh, 520px);
  overflow: auto;
  border: 1px solid var(--border-light, #f0f0f0);
  border-radius: 6px;
}
.cfg-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.cfg-table th,
.cfg-table td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--border-light, #f0f0f0);
  text-align: left;
  vertical-align: middle;
}
.cfg-table th {
  position: sticky;
  top: 0;
  background: #fafafa;
  font-weight: 600;
  z-index: 1;
}
.cfg-extra-row td {
  background: #fafafa;
  padding-top: 4px;
  padding-bottom: 12px;
}
.cfg-extra-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cfg-extra-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cfg-extra-label {
  font-size: 12px;
  color: var(--text-secondary, #595959);
  font-weight: 600;
}
.opt-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.opt-table th,
.opt-table td {
  padding: 4px 6px;
  border-bottom: 1px dashed var(--border-light, #f0f0f0);
}
.sort-actions {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
}
.sort-actions a {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}
.sort-actions a i {
  font-size: 13px;
}
.sort-actions .disabled {
  opacity: 0.35;
  pointer-events: none;
}
.code {
  display: block;
  color: var(--text-tertiary, #8c8c8c);
  font-size: 11px;
}
.custom-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.hint {
  font-size: 12px;
  color: var(--text-tertiary, #8c8c8c);
}
.empty {
  text-align: center;
  color: var(--text-tertiary, #8c8c8c);
  padding: 24px 8px !important;
}
</style>
