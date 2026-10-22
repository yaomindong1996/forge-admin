<script setup>
import { computed } from 'vue'

const props = defineProps({
  block: { type: Object, required: true },
})

const emit = defineEmits(['patchProps'])

const p = computed(() => props.block?.props || {})

const modeOptions = [
  { label: '待办 + 已办（可切换）', value: 'both' },
  { label: '仅待办', value: 'todo' },
  { label: '仅已办', value: 'done' },
]

function patch(partial = {}) {
  emit('patchProps', partial)
}
</script>

<template>
  <div class="wtl-prop-panel">
    <section class="prop-section">
      <header class="prop-section__head">
        <strong>基础</strong>
        <span>标题、模式与条数</span>
      </header>
      <div class="prop-section__body">
        <label class="prop-field">
          <span>标题</span>
          <n-input :value="p.title || ''" size="small" placeholder="待办与已办" @update:value="patch({ title: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>副标题</span>
          <n-input :value="p.subtitle || ''" size="small" placeholder="可选说明" @update:value="patch({ subtitle: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>展示模式</span>
          <n-select :value="p.mode || 'both'" :options="modeOptions" size="small" @update:value="patch({ mode: $event || 'both' })" />
        </label>
        <label class="prop-field">
          <span>展示条数</span>
          <n-input-number :value="Number(p.pageSize || 6)" :min="1" :max="30" size="small" style="width: 100%" @update:value="patch({ pageSize: $event || 6 })" />
        </label>
        <label class="prop-field">
          <span>空状态文案</span>
          <n-input :value="p.emptyText || ''" size="small" placeholder="默认按模式提示" @update:value="patch({ emptyText: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>强调色</span>
          <n-color-picker :value="p.accentColor || '#2563eb'" size="small" @update:value="patch({ accentColor: $event || '#2563eb' })" />
        </label>
      </div>
    </section>

    <section class="prop-section">
      <header class="prop-section__head">
        <strong>外观与字段</strong>
        <span>列表信息密度</span>
      </header>
      <div class="prop-section__body">
        <div class="prop-switch"><span>紧凑模式</span><n-switch :value="p.compact === true" size="small" @update:value="patch({ compact: $event })" /></div>
        <div class="prop-switch"><span>显示标题栏</span><n-switch :value="p.showHeader !== false" size="small" @update:value="patch({ showHeader: $event })" /></div>
        <div class="prop-switch"><span>待办/已办切换</span><n-switch :value="p.showTabs !== false" size="small" @update:value="patch({ showTabs: $event })" /></div>
        <div class="prop-switch"><span>搜索框</span><n-switch :value="p.showSearch === true" size="small" @update:value="patch({ showSearch: $event })" /></div>
        <div class="prop-switch"><span>状态标签</span><n-switch :value="p.showStatus !== false" size="small" @update:value="patch({ showStatus: $event })" /></div>
        <div class="prop-switch"><span>节点名称</span><n-switch :value="p.showNode !== false" size="small" @update:value="patch({ showNode: $event })" /></div>
        <div class="prop-switch"><span>申请人</span><n-switch :value="p.showApplicant !== false" size="small" @update:value="patch({ showApplicant: $event })" /></div>
        <div class="prop-switch"><span>时间</span><n-switch :value="p.showTime !== false" size="small" @update:value="patch({ showTime: $event })" /></div>
        <div class="prop-switch"><span>刷新按钮</span><n-switch :value="p.showRefresh !== false" size="small" @update:value="patch({ showRefresh: $event })" /></div>
        <div class="prop-switch"><span>查看全部</span><n-switch :value="p.showMoreLink !== false" size="small" @update:value="patch({ showMoreLink: $event })" /></div>
      </div>
    </section>

    <section class="prop-section">
      <header class="prop-section__head">
        <strong>跳转</strong>
        <span>点击任务打开的页面</span>
      </header>
      <div class="prop-section__body">
        <label class="prop-field">
          <span>待办页路径</span>
          <n-input :value="p.todoPath || '/flow/todo'" size="small" @update:value="patch({ todoPath: $event || '/flow/todo' })" />
        </label>
        <label class="prop-field">
          <span>已办页路径</span>
          <n-input :value="p.donePath || '/flow/done'" size="small" @update:value="patch({ donePath: $event || '/flow/done' })" />
        </label>
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
</style>
