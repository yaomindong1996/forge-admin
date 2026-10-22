<script setup>
import { computed } from 'vue'

const props = defineProps({
  block: { type: Object, required: true },
})

const emit = defineEmits(['patchProps'])

const p = computed(() => props.block?.props || {})

const variantOptions = [
  { label: '列表', value: 'list' },
  { label: '卡片感', value: 'card' },
]

const densityOptions = [
  { label: '舒适', value: 'comfortable' },
  { label: '紧凑', value: 'compact' },
]

function patch(partial = {}) {
  emit('patchProps', partial)
}
</script>

<template>
  <div class="nb-prop-panel">
    <section class="prop-section">
      <header class="prop-section__head">
        <strong>基础</strong>
        <span>标题与条数</span>
      </header>
      <div class="prop-section__body">
        <label class="prop-field">
          <span>标题</span>
          <n-input :value="p.title || ''" size="small" placeholder="通知公告" @update:value="patch({ title: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>副标题</span>
          <n-input :value="p.subtitle || ''" size="small" placeholder="可选说明" @update:value="patch({ subtitle: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>展示条数</span>
          <n-input-number :value="Number(p.pageSize || 6)" :min="1" :max="30" size="small" style="width: 100%" @update:value="patch({ pageSize: $event || 6 })" />
        </label>
        <label class="prop-field">
          <span>样式</span>
          <n-select :value="p.variant || 'list'" :options="variantOptions" size="small" @update:value="patch({ variant: $event || 'list' })" />
        </label>
        <label class="prop-field">
          <span>密度</span>
          <n-select :value="p.density || 'comfortable'" :options="densityOptions" size="small" @update:value="patch({ density: $event || 'comfortable' })" />
        </label>
        <label class="prop-field">
          <span>空状态文案</span>
          <n-input :value="p.emptyText || ''" size="small" placeholder="暂无通知公告" @update:value="patch({ emptyText: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>强调色</span>
          <n-color-picker :value="p.accentColor || '#2563eb'" size="small" @update:value="patch({ accentColor: $event || '#2563eb' })" />
        </label>
      </div>
    </section>

    <section class="prop-section">
      <header class="prop-section__head">
        <strong>内容显示</strong>
        <span>字段与筛选</span>
      </header>
      <div class="prop-section__body">
        <div class="prop-switch"><span>紧凑模式</span><n-switch :value="p.compact === true" size="small" @update:value="patch({ compact: $event })" /></div>
        <div class="prop-switch"><span>标题栏</span><n-switch :value="p.showHeader !== false" size="small" @update:value="patch({ showHeader: $event })" /></div>
        <div class="prop-switch"><span>全部/未读切换</span><n-switch :value="p.showFilterTabs !== false" size="small" @update:value="patch({ showFilterTabs: $event })" /></div>
        <div class="prop-switch"><span>仅未读</span><n-switch :value="p.unreadOnly === true" size="small" @update:value="patch({ unreadOnly: $event })" /></div>
        <div class="prop-switch"><span>未读角标</span><n-switch :value="p.showUnreadBadge !== false" size="small" @update:value="patch({ showUnreadBadge: $event })" /></div>
        <div class="prop-switch"><span>未读圆点</span><n-switch :value="p.showUnreadDot !== false" size="small" @update:value="patch({ showUnreadDot: $event })" /></div>
        <div class="prop-switch"><span>类型标签</span><n-switch :value="p.showTypeTag !== false" size="small" @update:value="patch({ showTypeTag: $event })" /></div>
        <div class="prop-switch"><span>摘要</span><n-switch :value="p.showSummary !== false" size="small" @update:value="patch({ showSummary: $event })" /></div>
        <div class="prop-switch"><span>发送人</span><n-switch :value="p.showSender === true" size="small" @update:value="patch({ showSender: $event })" /></div>
        <div class="prop-switch"><span>时间</span><n-switch :value="p.showTime !== false" size="small" @update:value="patch({ showTime: $event })" /></div>
        <div class="prop-switch"><span>刷新</span><n-switch :value="p.showRefresh !== false" size="small" @update:value="patch({ showRefresh: $event })" /></div>
        <div class="prop-switch"><span>全部已读</span><n-switch :value="p.showMarkAll !== false" size="small" @update:value="patch({ showMarkAll: $event })" /></div>
        <div class="prop-switch"><span>更多入口</span><n-switch :value="p.showMoreLink !== false" size="small" @update:value="patch({ showMoreLink: $event })" /></div>
        <div class="prop-switch"><span>打开时标已读</span><n-switch :value="p.markReadOnOpen !== false" size="small" @update:value="patch({ markReadOnOpen: $event })" /></div>
      </div>
    </section>

    <section class="prop-section">
      <header class="prop-section__head">
        <strong>跳转</strong>
      </header>
      <div class="prop-section__body">
        <label class="prop-field">
          <span>消息中心路径</span>
          <n-input :value="p.morePath || '/message'" size="small" @update:value="patch({ morePath: $event || '/message' })" />
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
