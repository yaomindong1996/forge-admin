<script setup>
import { computed } from 'vue'
import {
  NButton,
  NColorPicker,
  NInput,
  NInputNumber,
  NSelect,
  NSwitch,
} from 'naive-ui'
import IconSelector from '@/components/IconSelector.vue'
import {
  FREE_LAYOUT_LINK_TYPE_OPTIONS,
  FREE_LAYOUT_TONE_OPTIONS,
  normalizeQuickEntryItem,
} from './free-layout-widget-kit.js'

const props = defineProps({
  block: { type: Object, required: true },
  /** { label, value }[] 应用内页面候选 */
  pageOptions: { type: Array, default: () => [] },
  listBound: { type: Boolean, default: false },
})

const emit = defineEmits(['patch-props'])

const pageSelectOptions = computed(() =>
  (Array.isArray(props.pageOptions) ? props.pageOptions : [])
    .map(item => ({
      label: item.label || item.value,
      value: String(item.value || ''),
    }))
    .filter(item => item.value),
)

const items = computed(() => {
  const list = Array.isArray(props.block?.props?.items) ? props.block.props.items : []
  return list.map((item, index) => normalizeQuickEntryItem(item, index))
})

function patch(partial) {
  emit('patch-props', partial)
}

function patchItem(index, partial) {
  const next = items.value.map((item, i) => (i === index ? { ...item, ...partial } : { ...item }))
  patch({ items: next })
}

function addItem() {
  const next = [
    ...items.value,
    normalizeQuickEntryItem({
      title: `入口${items.value.length + 1}`,
      description: '',
      icon: 'GridOutline',
      tone: 'primary',
      linkType: 'none',
    }, items.value.length),
  ]
  patch({ items: next })
}

function removeItem(index) {
  patch({ items: items.value.filter((_, i) => i !== index) })
}

function moveItem(index, delta) {
  const target = index + delta
  if (target < 0 || target >= items.value.length)
    return
  const next = [...items.value]
  const [row] = next.splice(index, 1)
  next.splice(target, 0, row)
  patch({ items: next })
}
</script>

<template>
  <div class="qe-prop">
    <!-- 布局 -->
    <div class="qe-section">
      <div class="qe-section-title">布局</div>
      <div class="qe-field">
        <label>列数</label>
        <NInputNumber
          :value="Number(block.props?.columns || 3)"
          :min="1"
          :max="6"
          size="small"
          @update:value="(v) => patch({ columns: Number(v) || 3 })"
        />
      </div>
      <div class="qe-field">
        <label>样式</label>
        <NSelect
          :value="block.props?.variant || 'card'"
          :options="[
            { label: '卡片（横排）', value: 'card' },
            { label: '磁贴（竖排）', value: 'tile' },
            { label: '紧凑', value: 'compact' },
          ]"
          size="small"
          @update:value="(v) => patch({ variant: v || 'card' })"
        />
      </div>
      <div class="qe-field qe-field-row">
        <label>显示描述</label>
        <NSwitch
          :value="block.props?.showDescription !== false"
          size="small"
          @update:value="(v) => patch({ showDescription: v !== false })"
        />
      </div>
    </div>

    <!-- 入口列表 -->
    <div class="qe-section">
      <div class="qe-section-head">
        <div class="qe-section-title">入口项</div>
        <NButton size="tiny" quaternary type="primary" @click="addItem">
          添加
        </NButton>
      </div>
      <div class="qe-help">
        {{ listBound
          ? '当前为「列表循环」：画布按数组条数渲染。这里的入口仅在无数据时占位。'
          : '配置图标、色调与跳转（应用内页面 / 外部链接）。运行态可点击跳转。' }}
      </div>

      <div
        v-for="(item, index) in items"
        :key="item.key"
        class="qe-item"
      >
        <div class="qe-item-head">
          <strong>入口 {{ index + 1 }}</strong>
          <div class="qe-item-actions">
            <NButton size="tiny" quaternary :disabled="index === 0" @click="moveItem(index, -1)">
              上移
            </NButton>
            <NButton size="tiny" quaternary :disabled="index === items.length - 1" @click="moveItem(index, 1)">
              下移
            </NButton>
            <NButton size="tiny" quaternary type="error" @click="removeItem(index)">
              删除
            </NButton>
          </div>
        </div>

        <div class="qe-field">
          <label>标题</label>
          <NInput
            :value="item.title"
            size="small"
            placeholder="入口名称"
            @update:value="(v) => patchItem(index, { title: v })"
          />
        </div>
        <div class="qe-field">
          <label>描述</label>
          <NInput
            :value="item.description"
            size="small"
            placeholder="可选副文案"
            @update:value="(v) => patchItem(index, { description: v })"
          />
        </div>
        <div class="qe-field">
          <label>图标</label>
          <IconSelector
            :model-value="item.icon"
            @update:model-value="(v) => patchItem(index, { icon: v || 'GridOutline' })"
          />
        </div>
        <div class="qe-field">
          <label>色调</label>
          <NSelect
            :value="item.tone || 'primary'"
            :options="FREE_LAYOUT_TONE_OPTIONS"
            size="small"
            @update:value="(v) => patchItem(index, { tone: v || 'primary' })"
          />
        </div>
        <div v-if="item.tone === 'custom'" class="qe-field">
          <label>自定义色</label>
          <NColorPicker
            :value="item.color || '#2563eb'"
            :modes="['hex']"
            size="small"
            @update:value="(v) => patchItem(index, { color: v || '' })"
          />
        </div>
        <div class="qe-field">
          <label>跳转类型</label>
          <NSelect
            :value="item.linkType || 'none'"
            :options="FREE_LAYOUT_LINK_TYPE_OPTIONS"
            size="small"
            @update:value="(v) => patchItem(index, { linkType: v || 'none' })"
          />
        </div>
        <div v-if="item.linkType === 'page'" class="qe-field">
          <label>目标页面</label>
          <NSelect
            :value="item.targetPageId || null"
            :options="pageSelectOptions"
            clearable
            filterable
            size="small"
            placeholder="选择应用内页面"
            @update:value="(v) => patchItem(index, { targetPageId: v || '', linkType: 'page' })"
          />
        </div>
        <div v-if="item.linkType === 'url'" class="qe-field">
          <label>外部链接</label>
          <NInput
            :value="item.href"
            size="small"
            placeholder="https://"
            @update:value="(v) => patchItem(index, { href: v })"
          />
        </div>
      </div>

      <div v-if="!items.length" class="qe-empty">
        暂无入口，点击「添加」配置图标、色调与跳转。
      </div>
    </div>
  </div>
</template>

<style scoped>
.qe-prop {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.qe-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.qe-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.qe-section-title {
  font-size: 12px;
  font-weight: 650;
  color: #334155;
}

.qe-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.qe-field > label {
  font-size: 12px;
  color: #64748b;
}

.qe-field-row {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.qe-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.qe-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.qe-item-head strong {
  font-size: 12px;
  color: #0f172a;
}

.qe-item-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.qe-help {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.45;
}

.qe-empty {
  padding: 12px;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
}
</style>
