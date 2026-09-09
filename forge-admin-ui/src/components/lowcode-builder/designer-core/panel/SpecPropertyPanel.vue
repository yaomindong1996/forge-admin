<script setup>
/**
 * SpecPropertyPanel — schema 驱动的统一属性面板引擎（P2 核心）
 * @description 消费 designer-core 注册表的 propsSchema，自动生成属性编辑 UI。
 *   两个设计器（表单 / 列表）共用同一面板：加属性只改 spec JSON，不再改巨石模板。
 *   支持 group 分组 + priority 分层：常用属性（common）按 group 分节渲染标题后平铺展示，
 *   高级属性（advanced）折叠收纳；同一组件在两个设计器中的属性区块完全一致。
 *   支持关键字搜索过滤（匹配 key / title）。
 *   C3 布局重设计：两列紧凑网格 — 标题统一在控件上方（含布尔开关，用户验收反馈
 *   "标题有的在上面 有的在左面"要求对齐）、number/select/color/短文本占 1 格、
 *   textarea/json/options 占满整行、分组标题占满整行；与两侧手写面板的紧凑风格统一。
 *   type: 'options' 渲染表格化选项编辑器（label/value/disabled 逐行编辑 + 增删），
 *   替代手写 JSON 文本域 — 下拉/单选/多选等高频组件在两侧设计器获得同一份选项编辑体验。
 *   覆盖不到的复杂编辑器（公式/联动/CRUD 分区）后续以 customEditor 挂载。
 */
import { BanOutline, CloseOutline, HelpCircleOutline, SearchOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import IconSelector from '@/components/IconSelector.vue'
import { getComponentSpec } from '../spec/registry'
import PropertyGroupCollapse from './PropertyGroupCollapse.vue'

const props = defineProps({
  /** 组件类型（统一 type 或历史别名，如 grid-layout / elCard / stats-strip） */
  blockType: { type: String, required: true },
  /** 组件当前 props 对象（只读引用，更新走 update:prop 事件） */
  modelProps: { type: Object, default: () => ({}) },
  /** 排除的属性 key（已由接入方手写模板覆盖的属性，避免重复编辑入口） */
  excludeKeys: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
  /** 是否显示搜索框 */
  showSearch: { type: Boolean, default: true },
})

const emit = defineEmits(['update:prop'])

const spec = computed(() => getComponentSpec(props.blockType))

/** 搜索关键字 */
const searchKeyword = ref('')

/** 可编辑属性列表（跳过排除项，保持 spec 声明顺序） */
const editableProperties = computed(() => {
  const properties = spec.value?.propsSchema?.properties
  if (!properties)
    return []
  const exclude = new Set(props.excludeKeys || [])
  return Object.entries(properties)
    .filter(([key]) => !exclude.has(key))
    .map(([key, schema]) => ({ key, ...schema }))
})

/**
 * 按渲染节点组织属性结构。
 * 返回节点列表：
 *   { kind: 'grid', title, properties }        — 常用属性网格（title 非空时先渲染分组标题分隔线）
 *   { kind: 'advanced', title, properties }    — 高级属性折叠分组
 * 常用属性按 group 字段分节渲染标题（如"输入行为 / 外观 / 栅格"），避免全部平铺成一张无结构的网格；
 * 旧版 type: 'section' 分隔线属性被 group 分组标题取代，直接跳过。
 * 搜索模式下所有匹配属性归入单个不分节的 grid 节点。
 */
const groupedSections = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()

  // 搜索模式：扁平返回所有匹配项
  if (keyword) {
    const matched = editableProperties.value.filter(p =>
      p.key.toLowerCase().includes(keyword)
      || (p.title || '').toLowerCase().includes(keyword),
    )
    return matched.length ? [{ kind: 'grid', title: '', properties: matched }] : []
  }

  const nodes = []
  const gridIndex = new Map()
  const advancedMap = new Map()

  for (const prop of editableProperties.value) {
    // 旧版 section 分隔线由 group 分组标题取代，跳过
    if (prop.type === 'section')
      continue
    const priority = prop.priority || 'common'
    if (priority === 'advanced') {
      const group = prop.group || '高级'
      if (!advancedMap.has(group))
        advancedMap.set(group, [])
      advancedMap.get(group).push(prop)
    }
    else {
      const group = prop.group || ''
      if (!gridIndex.has(group)) {
        const gridNode = { kind: 'grid', title: group, properties: [] }
        gridIndex.set(group, gridNode)
        nodes.push(gridNode)
      }
      gridIndex.get(group).properties.push(prop)
    }
  }

  for (const [title, properties] of advancedMap)
    nodes.push({ kind: 'advanced', title, properties })

  return nodes
})

function propValue(key, schema) {
  const raw = props.modelProps?.[key]
  if (raw !== undefined && raw !== null)
    return raw
  return schema?.default
}

function updateProp(key, value) {
  if (props.disabled)
    return
  emit('update:prop', { key, value })
}

/** 长内容属性（多行文本 / JSON）占满整行，不与其它属性并排 */
function isFullRowProperty(property) {
  return property.type === 'json'
    || property.format === 'textarea'
    || property.format === 'html'
}

/** 占位提示：default 存在时以默认值作提示，避免出现 "undefined" 字样 */
function placeholderOf(property, fallback = '') {
  return property.placeholder
    || (property.default !== undefined && property.default !== null ? String(property.default) : fallback)
}

/** select 选项归一化：支持 [{label,value}] 与 ['a','b'] 两种声明 */
function normalizeOptions(options) {
  if (!Array.isArray(options))
    return []
  return options.map(option => (typeof option === 'object'
    ? { label: option.label ?? option.value, value: option.value }
    : { label: option, value: option }))
}

function updateJsonProp(key, schema, text) {
  if (text === '' || text === null || text === undefined) {
    updateProp(key, undefined)
    return
  }
  try {
    updateProp(key, JSON.parse(text))
  }
  catch {
    // JSON 非法时不回写，保留上次合法值
  }
}

// ─── type: 'options' 表格化选项编辑 ───────────────────────

/** 读取 options 属性的数组值（非数组一律归空，避免脏数据渲染崩溃） */
function optionItems(property) {
  const raw = propValue(property.key, property)
  return Array.isArray(raw) ? raw : []
}

/** 是否显示"值"编辑列（宜搭式默认隐藏：值跟随选项名，需要值≠名称时勾选"自定义值"） */
const showOptionValues = ref(false)

// 仅切换组件时检测：存量选项已有"值 ≠ 名称"则自动展开自定义值列；
// 不监听 modelProps，避免编辑属性时把用户手动勾选的状态重置
watch(() => props.blockType, () => {
  const properties = spec.value?.propsSchema?.properties || {}
  showOptionValues.value = Object.entries(properties)
    .filter(([, schema]) => schema?.type === 'options')
    .some(([key]) => {
      const items = Array.isArray(props.modelProps?.[key]) ? props.modelProps[key] : []
      return items.some(item => String(item?.value ?? '') !== '' && String(item.value) !== String(item?.label ?? ''))
    })
}, { immediate: true })

/** 修改单条选项（label / value / disabled），整表浅拷贝后回写 */
function updateOptionItem(property, index, patch) {
  const next = optionItems(property).map((item, i) => {
    if (i !== index)
      return item
    const merged = { ...item, ...patch }
    if (merged.disabled === false)
      delete merged.disabled
    return merged
  })
  updateProp(property.key, next)
}

/** 选项名输入：值未被单独改过时自动跟随名称（窄面板内只维护一个字段） */
function updateOptionItemLabel(property, index, label = '') {
  const item = optionItems(property)[index] || {}
  const valueFollowsLabel = String(item.value ?? '') === '' || String(item.value) === String(item.label ?? '')
  updateOptionItem(property, index, valueFollowsLabel ? { label, value: label } : { label })
}

/** 行尾图标切换选项禁用 */
function toggleOptionItemDisabled(property, index) {
  updateOptionItem(property, index, { disabled: !optionItems(property)[index]?.disabled })
}

/** 追加一条默认选项（值跟随名称：需要不同值时勾选"自定义值"单独修改） */
function addOptionItem(property) {
  const items = optionItems(property)
  const label = `选项${items.length + 1}`
  updateProp(property.key, [...items, { label, value: label }])
}

/** 删除单条选项 */
function removeOptionItem(property, index) {
  updateProp(property.key, optionItems(property).filter((_, i) => i !== index))
}
</script>

<template>
  <div class="spec-property-panel">
    <!-- 搜索栏 -->
    <n-input
      v-if="showSearch && editableProperties.length > 3"
      v-model:value="searchKeyword"
      size="small"
      clearable
      placeholder="搜索属性"
      class="spec-property-search"
    >
      <template #prefix>
        <n-icon :size="14">
          <SearchOutline />
        </n-icon>
      </template>
    </n-input>

    <div v-if="!groupedSections.length" class="spec-property-empty">
      {{ searchKeyword ? '无匹配属性' : '该组件暂无可配置属性' }}
    </div>

    <template v-for="(section, sIdx) in groupedSections" :key="sIdx">
      <!-- 分组标题：占满整行（搜索模式下不渲染） -->
      <div v-if="section.kind === 'grid' && section.title" class="spec-property-section">
        <span>{{ section.title }}</span>
      </div>

      <!-- 常用属性：两列网格 -->
      <div v-if="section.kind === 'grid'" class="spec-property-grid">
        <template v-for="property in section.properties" :key="property.key">
          <!-- 布尔开关：标题在上（与其他属性结构一致），开关占控件行 -->
          <div v-if="property.type === 'boolean'" class="spec-property-field">
            <label class="spec-property-field-label">
              <span>{{ property.title || property.key }}</span>
              <n-tooltip v-if="property.desc" trigger="hover">
                <template #trigger>
                  <n-icon :size="12" class="spec-property-help"><HelpCircleOutline /></n-icon>
                </template>
                {{ property.desc }}
              </n-tooltip>
            </label>
            <div class="spec-property-switch">
              <n-switch
                size="small"
                :value="!!propValue(property.key, property)"
                :disabled="disabled"
                @update:value="updateProp(property.key, $event)"
              />
            </div>
          </div>

          <!-- 选项列表（label/value/disabled）：宜搭式单行编辑 — 名称输入框占满行宽（值默认跟随名称），
               行尾仅保留禁用/删除图标；需要值≠名称时勾选"自定义值"展开值列 -->
          <div v-else-if="property.type === 'options'" class="spec-property-field spec-property-field--full">
            <label class="spec-property-field-label spec-option-list-label">
              <span>{{ property.title || property.key }}</span>
              <n-checkbox
                size="small"
                :checked="showOptionValues"
                @update:checked="showOptionValues = $event"
              >
                自定义值
              </n-checkbox>
              <n-tooltip v-if="property.desc" trigger="hover">
                <template #trigger>
                  <n-icon :size="12" class="spec-property-help"><HelpCircleOutline /></n-icon>
                </template>
                {{ property.desc }}
              </n-tooltip>
            </label>
            <div class="spec-option-list">
              <div
                v-for="(item, index) in optionItems(property)"
                :key="index"
                class="spec-option-row"
                :class="{ 'is-disabled': !!item.disabled }"
              >
                <span class="spec-option-row-index">{{ index + 1 }}</span>
                <n-input
                  class="spec-option-row-label-input"
                  size="small"
                  :value="item.label"
                  placeholder="选项名称"
                  :disabled="disabled"
                  @update:value="updateOptionItemLabel(property, index, $event)"
                />
                <n-input
                  v-if="showOptionValues"
                  class="spec-option-row-value-input"
                  size="small"
                  :value="String(item.value ?? '')"
                  placeholder="值"
                  :disabled="disabled"
                  @update:value="updateOptionItem(property, index, { value: $event })"
                />
                <div class="spec-option-row-actions">
                  <button
                    type="button"
                    class="spec-option-row-action spec-option-row-action--ban"
                    :class="{ active: !!item.disabled }"
                    :title="item.disabled ? '取消禁用' : '禁用选项'"
                    @click="toggleOptionItemDisabled(property, index)"
                  >
                    <n-icon :size="13">
                      <BanOutline />
                    </n-icon>
                  </button>
                  <button
                    type="button"
                    class="spec-option-row-action spec-option-row-action--remove"
                    title="删除选项"
                    @click="removeOptionItem(property, index)"
                  >
                    <n-icon :size="14">
                      <CloseOutline />
                    </n-icon>
                  </button>
                </div>
              </div>
            </div>
            <n-button class="spec-option-add" size="small" secondary block :disabled="disabled" @click="addOptionItem(property)">
              <template #icon>
                <span class="spec-option-add-icon">+</span>
              </template>
              新增选项
            </n-button>
          </div>

          <!-- 长内容属性（多行文本 / JSON）：占满整行 -->
          <div v-else-if="isFullRowProperty(property)" class="spec-property-field spec-property-field--full">
            <label class="spec-property-field-label">
              <span>{{ property.title || property.key }}</span>
              <n-tooltip v-if="property.desc" trigger="hover">
                <template #trigger>
                  <n-icon :size="12" class="spec-property-help"><HelpCircleOutline /></n-icon>
                </template>
                {{ property.desc }}
              </n-tooltip>
            </label>
            <n-input
              :value="property.type === 'json' ? (propValue(property.key, property) == null ? '' : JSON.stringify(propValue(property.key, property), null, 0)) : propValue(property.key, property)"
              type="textarea"
              size="small"
              :rows="property.rows || 3"
              :disabled="disabled"
              :placeholder="placeholderOf(property, property.type === 'json' ? 'JSON 配置' : '请输入')"
              @blur="property.type === 'json' && updateJsonProp(property.key, property, $event.target.value)"
              @change="property.type === 'json' && updateJsonProp(property.key, property, $event)"
              @update:value="property.type !== 'json' && updateProp(property.key, $event)"
            />
          </div>

          <!-- 常规控件（number / select / color / 短文本）：占 1 格 -->
          <div v-else class="spec-property-field">
            <label class="spec-property-field-label">
              <span>{{ property.title || property.key }}</span>
              <n-tooltip v-if="property.desc" trigger="hover">
                <template #trigger>
                  <n-icon :size="12" class="spec-property-help"><HelpCircleOutline /></n-icon>
                </template>
                {{ property.desc }}
              </n-tooltip>
            </label>

            <!-- 数字 -->
            <n-input-number
              v-if="property.type === 'number'"
              size="small"
              :value="propValue(property.key, property)"
              :min="property.min"
              :max="property.max"
              :step="property.step"
              :disabled="disabled"
              :placeholder="placeholderOf(property)"
              class="spec-property-control"
              @update:value="updateProp(property.key, $event)"
            />

            <!-- 下拉选择 -->
            <n-select
              v-else-if="property.type === 'select'"
              size="small"
              clearable
              :value="propValue(property.key, property)"
              :options="normalizeOptions(property.options)"
              :disabled="disabled"
              :placeholder="placeholderOf(property, '请选择')"
              class="spec-property-control"
              @update:value="updateProp(property.key, $event)"
            />

            <!-- 颜色 -->
            <n-color-picker
              v-else-if="property.type === 'color'"
              size="small"
              :value="propValue(property.key, property) || ''"
              :show-alpha="true"
              :modes="['hex', 'rgb']"
              :disabled="disabled"
              class="spec-property-control"
              @update:value="updateProp(property.key, $event)"
            />

            <!-- 图标选择器：抽屉式图标库（ionicons5 + 本地上传），复用项目公共 IconSelector -->
            <IconSelector
              v-else-if="property.type === 'icon'"
              :model-value="propValue(property.key, property) || ''"
              :disabled="disabled"
              @update:model-value="updateProp(property.key, $event)"
            />

            <!-- 单行文本（默认） -->
            <n-input
              v-else
              size="small"
              :value="propValue(property.key, property)"
              :disabled="disabled"
              :maxlength="property.maxLength"
              :placeholder="placeholderOf(property)"
              @update:value="updateProp(property.key, $event)"
            />
          </div>
        </template>
      </div>

      <!-- 高级属性：折叠收纳 -->
      <PropertyGroupCollapse
        v-else-if="section.kind === 'advanced'"
        :title="section.title"
        :count="section.properties.length"
      >
        <div class="spec-property-grid spec-property-grid--nested">
          <template v-for="property in section.properties" :key="property.key">
            <div v-if="property.type === 'boolean'" class="spec-property-field">
              <label class="spec-property-field-label">
                <span>{{ property.title || property.key }}</span>
                <n-tooltip v-if="property.desc" trigger="hover">
                  <template #trigger>
                    <n-icon :size="12" class="spec-property-help"><HelpCircleOutline /></n-icon>
                  </template>
                  {{ property.desc }}
                </n-tooltip>
              </label>
              <div class="spec-property-switch">
                <n-switch
                  size="small"
                  :value="!!propValue(property.key, property)"
                  :disabled="disabled"
                  @update:value="updateProp(property.key, $event)"
                />
              </div>
            </div>

            <div v-else-if="property.type === 'options'" class="spec-property-field spec-property-field--full">
              <label class="spec-property-field-label">
                <span>{{ property.title || property.key }}</span>
                <n-tooltip v-if="property.desc" trigger="hover">
                  <template #trigger>
                    <n-icon :size="12" class="spec-property-help"><HelpCircleOutline /></n-icon>
                  </template>
                  {{ property.desc }}
                </n-tooltip>
              </label>
              <div class="spec-option-list">
                <div
                  v-for="(item, index) in optionItems(property)"
                  :key="index"
                  class="spec-option-row"
                >
                  <n-input
                    size="small"
                    :value="item.label"
                    placeholder="选项名"
                    :disabled="disabled"
                    @update:value="updateOptionItem(property, index, { label: $event })"
                  />
                  <n-input
                    size="small"
                    :value="String(item.value ?? '')"
                    placeholder="值"
                    :disabled="disabled"
                    @update:value="updateOptionItem(property, index, { value: $event })"
                  />
                  <n-switch
                    size="small"
                    :value="!!item.disabled"
                    title="禁用"
                    :disabled="disabled"
                    @update:value="updateOptionItem(property, index, { disabled: $event })"
                  />
                  <n-button size="tiny" quaternary type="error" :disabled="disabled" @click="removeOptionItem(property, index)">
                    删除
                  </n-button>
                </div>
              </div>
              <n-button class="spec-option-add" size="small" secondary block :disabled="disabled" @click="addOptionItem(property)">
                <template #icon>
                  <span class="spec-option-add-icon">+</span>
                </template>
                新增选项
              </n-button>
            </div>

            <div v-else-if="isFullRowProperty(property)" class="spec-property-field spec-property-field--full">
              <label class="spec-property-field-label">
                <span>{{ property.title || property.key }}</span>
                <n-tooltip v-if="property.desc" trigger="hover">
                  <template #trigger>
                    <n-icon :size="12" class="spec-property-help"><HelpCircleOutline /></n-icon>
                  </template>
                  {{ property.desc }}
                </n-tooltip>
              </label>
              <n-input
                :value="property.type === 'json' ? (propValue(property.key, property) == null ? '' : JSON.stringify(propValue(property.key, property), null, 0)) : propValue(property.key, property)"
                type="textarea"
                size="small"
                :rows="property.rows || 3"
                :disabled="disabled"
                :placeholder="placeholderOf(property, property.type === 'json' ? 'JSON 配置' : '请输入')"
                @blur="property.type === 'json' && updateJsonProp(property.key, property, $event.target.value)"
                @change="property.type === 'json' && updateJsonProp(property.key, property, $event)"
                @update:value="property.type !== 'json' && updateProp(property.key, $event)"
              />
            </div>

            <div v-else class="spec-property-field">
              <label class="spec-property-field-label">
                <span>{{ property.title || property.key }}</span>
                <n-tooltip v-if="property.desc" trigger="hover">
                  <template #trigger>
                    <n-icon :size="12" class="spec-property-help"><HelpCircleOutline /></n-icon>
                  </template>
                  {{ property.desc }}
                </n-tooltip>
              </label>
              <n-input-number
                v-if="property.type === 'number'"
                size="small"
                :value="propValue(property.key, property)"
                :min="property.min"
                :max="property.max"
                :step="property.step"
                :disabled="disabled"
                :placeholder="placeholderOf(property)"
                class="spec-property-control"
                @update:value="updateProp(property.key, $event)"
              />
              <n-select
                v-else-if="property.type === 'select'"
                size="small"
                clearable
                :value="propValue(property.key, property)"
                :options="normalizeOptions(property.options)"
                :disabled="disabled"
                :placeholder="placeholderOf(property, '请选择')"
                class="spec-property-control"
                @update:value="updateProp(property.key, $event)"
              />
              <n-color-picker
                v-else-if="property.type === 'color'"
                size="small"
                :value="propValue(property.key, property) || ''"
                :show-alpha="true"
                :modes="['hex', 'rgb']"
                :disabled="disabled"
                class="spec-property-control"
                @update:value="updateProp(property.key, $event)"
              />
              <IconSelector
                v-else-if="property.type === 'icon'"
                :model-value="propValue(property.key, property) || ''"
                :disabled="disabled"
                @update:model-value="updateProp(property.key, $event)"
              />
              <n-input
                v-else
                size="small"
                :value="propValue(property.key, property)"
                :disabled="disabled"
                :maxlength="property.maxLength"
                :placeholder="placeholderOf(property)"
                @update:value="updateProp(property.key, $event)"
              />
            </div>
          </template>
        </div>
      </PropertyGroupCollapse>
    </template>
  </div>
</template>

<style scoped>
.spec-property-panel {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 搜索栏 */
.spec-property-search {
  margin-bottom: 2px;
}

/* C3 两列紧凑网格：与两侧设计器手写面板（style-grid two / grid-config-grid）视觉统一 */
.spec-property-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 5px 8px;
  align-items: start;
}

/* 折叠区内部的网格（display: contents 透传父级 grid-column） */
.spec-property-grid--nested {
  margin-top: 4px;
}

.spec-property-section {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  padding-bottom: 2px;
  border-bottom: 1px solid var(--n-border-color, #ececf1);
  color: var(--n-text-color, #333);
  font-size: 11px;
  font-weight: 500;
  line-height: 16px;
}

/* 布尔开关控件行：与其他控件行同高，开关靠左（标题统一在上方） */
.spec-property-switch {
  display: flex;
  align-items: center;
  min-height: 28px;
}

.spec-property-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.spec-property-field--full {
  grid-column: 1 / -1;
}

.spec-property-field-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--n-text-color-disabled, #666);
  font-size: 11px;
  line-height: 1.3;
}

.spec-property-help {
  color: var(--n-text-color-disabled, #999);
  cursor: help;
}

.spec-property-control {
  width: 100%;
}

/* ─── 选项编辑器（宜搭式单行：名称占满宽度 + 行尾图标操作，与 ForgePropertyPanel 同款）── */
.spec-option-list {
  display: grid;
  gap: 4px;
  margin-bottom: 6px;
}

.spec-option-list-label {
  gap: 8px;
}

.spec-option-list-label :deep(.n-checkbox .n-checkbox__label) {
  padding-left: 4px;
  color: var(--n-text-color-disabled, #71717a);
  font-size: 11px;
  font-weight: 400;
}

.spec-option-row {
  display: flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--n-border-color, #ececf1);
  border-radius: 6px;
  background: var(--n-color, #fff);
  padding: 4px;
  transition: border-color 160ms ease;
}

.spec-option-row:hover {
  border-color: #c7d2fe;
}

.spec-option-row-index {
  flex: 0 0 auto;
  min-width: 14px;
  color: #a1a1aa;
  font-size: 11px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  text-align: center;
}

.spec-option-row .n-input {
  flex: 1 1 0;
  min-width: 0;
}

.spec-option-row .spec-option-row-label-input {
  flex: 1.15 1 0;
}

.spec-option-row-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 2px;
}

.spec-option-row-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #a1a1aa;
  cursor: pointer;
  transition:
    background-color 140ms ease,
    color 140ms ease;
}

.spec-option-row-action--remove:hover {
  background: #fee2e2;
  color: #d03050;
}

.spec-option-row-action--ban:hover,
.spec-option-row-action--ban.active {
  background: #fef3c7;
  color: #b45309;
}

.spec-option-row.is-disabled .spec-option-row-index {
  color: #d4a72c;
}

.spec-option-row.is-disabled .n-input {
  opacity: 0.55;
}

.spec-option-add {
  --n-color: #f0fdf4 !important;
  --n-color-hover: #dcfce7 !important;
  --n-color-pressed: #bbf7d0 !important;
  --n-border: 1px dashed #86efac !important;
  --n-border-hover: 1px dashed #4ade80 !important;
  --n-border-pressed: 1px dashed #22c55e !important;
  --n-text-color: #15803d !important;
  --n-text-color-hover: #166534 !important;
  --n-text-color-pressed: #14532d !important;
  height: 28px;
  font-weight: 700;
}

.spec-option-add-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 999px;
  background: #22c55e;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
}

.spec-property-empty {
  padding: 12px 0;
  color: var(--n-text-color-disabled, #999);
  font-size: 12px;
  text-align: center;
}
</style>
