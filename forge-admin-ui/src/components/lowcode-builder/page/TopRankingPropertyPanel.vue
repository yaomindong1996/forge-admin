<script setup>
import { computed } from 'vue'

const props = defineProps({
  block: { type: Object, required: true },
})

const emit = defineEmits(['patchProps'])

const p = computed(() => props.block?.props || {})

const items = computed(() => Array.isArray(p.value.items) ? p.value.items : [])

function patch(partial = {}) {
  emit('patchProps', partial)
}

function updateItem(index, key, value) {
  const next = items.value.map((item, i) => (i === index ? { ...item, [key]: value } : item))
  patch({ items: next })
}

function addItem() {
  patch({
    items: [
      ...items.value,
      {
        key: `rank_${Date.now()}`,
        name: `项目 ${items.value.length + 1}`,
        value: Math.max(10, 100 - items.value.length * 8),
        unit: p.value.valueUnit || '',
      },
    ],
  })
}

function removeItem(index) {
  patch({ items: items.value.filter((_, i) => i !== index) })
}
</script>

<template>
  <div class="tr-prop-panel">
    <section class="prop-section">
      <header class="prop-section__head">
        <strong>基础</strong>
        <span>标题与展示条数</span>
      </header>
      <div class="prop-section__body">
        <label class="prop-field">
          <span>标题</span>
          <n-input :value="p.title || ''" size="small" placeholder="排行榜" @update:value="patch({ title: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>副标题</span>
          <n-input :value="p.subtitle || ''" size="small" placeholder="可选说明" @update:value="patch({ subtitle: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>展示前 N 名</span>
          <n-input-number :value="Number(p.limit || 10)" :min="1" :max="50" size="small" style="width: 100%" @update:value="patch({ limit: $event || 10 })" />
        </label>
        <label class="prop-field">
          <span>数值单位</span>
          <n-input :value="p.valueUnit || ''" size="small" placeholder="如 次 / 单" @update:value="patch({ valueUnit: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>空状态文案</span>
          <n-input :value="p.emptyText || ''" size="small" placeholder="暂无排行数据" @update:value="patch({ emptyText: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>强调色</span>
          <n-color-picker :value="p.accentColor || '#2563eb'" size="small" @update:value="patch({ accentColor: $event || '#2563eb' })" />
        </label>
      </div>
    </section>

    <section class="prop-section">
      <header class="prop-section__head">
        <strong>外观</strong>
      </header>
      <div class="prop-section__body">
        <div class="prop-switch"><span>显示标题栏</span><n-switch :value="p.showHeader !== false" size="small" @update:value="patch({ showHeader: $event })" /></div>
        <div class="prop-switch"><span>显示 TOP 角标</span><n-switch :value="p.showTopHint !== false" size="small" @update:value="patch({ showTopHint: $event })" /></div>
        <div class="prop-switch"><span>显示进度条</span><n-switch :value="p.showBar !== false" size="small" @update:value="patch({ showBar: $event })" /></div>
        <div class="prop-switch"><span>紧凑模式</span><n-switch :value="p.compact === true" size="small" @update:value="patch({ compact: $event })" /></div>
      </div>
    </section>

    <section class="prop-section">
      <header class="prop-section__head">
        <strong>排行数据</strong>
        <span>手动维护；也可在「数据来源」绑列表</span>
      </header>
      <div class="prop-section__body">
        <div v-for="(item, index) in items" :key="item.key || index" class="tr-item-editor">
          <div class="tr-item-editor__head">
            <span>#{{ index + 1 }}</span>
            <n-button text type="error" size="tiny" @click="removeItem(index)">
              删除
            </n-button>
          </div>
          <label class="prop-field">
            <span>名称</span>
            <n-input :value="item.name || ''" size="small" @update:value="updateItem(index, 'name', $event || '')" />
          </label>
          <div class="tr-item-editor__row">
            <label class="prop-field">
              <span>数值</span>
              <n-input-number :value="Number(item.value) || 0" size="small" style="width: 100%" @update:value="updateItem(index, 'value', $event ?? 0)" />
            </label>
            <label class="prop-field">
              <span>单位</span>
              <n-input :value="item.unit || ''" size="small" @update:value="updateItem(index, 'unit', $event || '')" />
            </label>
          </div>
        </div>
        <n-button size="small" dashed block @click="addItem">
          添加一项
        </n-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.prop-section { margin-bottom: 14px; }
.prop-section__head { display: flex; flex-direction: column; gap: 2px; margin-bottom: 8px; }
.prop-section__head strong { font-size: 13px; }
.prop-section__head span { font-size: 11px; color: var(--text-tertiary, #94a3b8); }
.prop-section__body { display: flex; flex-direction: column; gap: 8px; }
.prop-field { display: flex; flex-direction: column; gap: 4px; font-size: 12px; }
.prop-field > span { color: var(--text-secondary, #64748b); }
.prop-switch { display: flex; align-items: center; justify-content: space-between; gap: 8px; font-size: 12px; }
.tr-item-editor {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 6px;
  background: var(--bg-secondary, #f8fafc);
}
.tr-item-editor__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary, #64748b);
}
.tr-item-editor__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
</style>
