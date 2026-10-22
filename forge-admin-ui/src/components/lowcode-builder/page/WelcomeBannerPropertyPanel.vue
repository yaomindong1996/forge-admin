<script setup>
import { computed } from 'vue'

const props = defineProps({
  block: { type: Object, required: true },
})

const emit = defineEmits(['patchProps'])

const p = computed(() => props.block?.props || {})

const toneOptions = [
  { label: '主题蓝', value: 'primary' },
  { label: '成功绿', value: 'success' },
  { label: '暖色', value: 'warm' },
]

const backgroundModeOptions = [
  { label: '纯色', value: 'solid' },
  { label: '渐变', value: 'gradient' },
  { label: '图片', value: 'image' },
]

function patch(partial = {}) {
  emit('patchProps', partial)
}
</script>

<template>
  <div class="wb-prop-panel">
    <section class="prop-section">
      <header class="prop-section__head">
        <strong>文案</strong>
        <span>问候与说明</span>
      </header>
      <div class="prop-section__body">
        <label class="prop-field">
          <span>自定义标题</span>
          <n-input
            :value="p.title || ''"
            size="small"
            placeholder="留空则按时段 + 昵称自动问候"
            @update:value="patch({ title: $event || '' })"
          />
        </label>
        <label class="prop-field">
          <span>问候前缀</span>
          <n-input
            :value="p.greetingPrefix || ''"
            size="small"
            placeholder="如：早上好（可选）"
            @update:value="patch({ greetingPrefix: $event || '' })"
          />
        </label>
        <label class="prop-field">
          <span>副标题</span>
          <n-input
            :value="p.subtitle || ''"
            size="small"
            placeholder="今天也要高效推进工作"
            @update:value="patch({ subtitle: $event || '' })"
          />
        </label>
        <label class="prop-field">
          <span>色调</span>
          <n-select
            :value="p.tone || 'primary'"
            :options="toneOptions"
            size="small"
            @update:value="patch({ tone: $event || 'primary' })"
          />
        </label>
      </div>
    </section>

    <section class="prop-section">
      <header class="prop-section__head">
        <strong>展示</strong>
      </header>
      <div class="prop-section__body">
        <div class="prop-switch">
          <span>紧凑模式</span>
          <n-switch :value="p.compact === true" size="small" @update:value="patch({ compact: $event })" />
        </div>
        <div class="prop-switch">
          <span>头像块</span>
          <n-switch :value="p.showAvatar !== false" size="small" @update:value="patch({ showAvatar: $event })" />
        </div>
        <div class="prop-switch">
          <span>显示日期</span>
          <n-switch :value="p.showDate !== false" size="small" @update:value="patch({ showDate: $event })" />
        </div>
        <div class="prop-switch">
          <span>操作按钮</span>
          <n-switch :value="p.showCta !== false" size="small" @update:value="patch({ showCta: $event })" />
        </div>
        <label class="prop-field">
          <span>按钮文案</span>
          <n-input :value="p.ctaText || ''" size="small" placeholder="去办理" @update:value="patch({ ctaText: $event || '' })" />
        </label>
        <label class="prop-field">
          <span>按钮跳转</span>
          <n-input :value="p.ctaPath || ''" size="small" placeholder="/flow/todo" @update:value="patch({ ctaPath: $event || '' })" />
        </label>
      </div>
    </section>

    <section class="prop-section">
      <header class="prop-section__head">
        <strong>背景</strong>
        <span>纯色 / 渐变 / 图片</span>
      </header>
      <div class="prop-section__body">
        <label class="prop-field">
          <span>背景模式</span>
          <n-select
            :value="p.backgroundMode || 'gradient'"
            :options="backgroundModeOptions"
            size="small"
            @update:value="patch({ backgroundMode: $event || 'gradient' })"
          />
        </label>
        <label class="prop-field">
          <span>背景色</span>
          <n-color-picker
            :value="p.backgroundColor || '#eff6ff'"
            size="small"
            @update:value="patch({ backgroundColor: $event || '#eff6ff' })"
          />
        </label>
        <template v-if="(p.backgroundMode || 'gradient') === 'gradient'">
          <label class="prop-field">
            <span>渐变起始</span>
            <n-color-picker :value="p.gradientFrom || '#eff6ff'" size="small" @update:value="patch({ gradientFrom: $event || '#eff6ff' })" />
          </label>
          <label class="prop-field">
            <span>渐变结束</span>
            <n-color-picker :value="p.gradientTo || '#ffffff'" size="small" @update:value="patch({ gradientTo: $event || '#ffffff' })" />
          </label>
        </template>
        <template v-if="p.backgroundMode === 'image'">
          <label class="prop-field">
            <span>背景图 URL</span>
            <n-input :value="p.backgroundImage || ''" size="small" placeholder="https://..." @update:value="patch({ backgroundImage: $event || '' })" />
          </label>
        </template>
      </div>
    </section>
  </div>
</template>

<style scoped>
.wb-prop-panel {
  display: grid;
  gap: 12px;
}
.prop-section {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary, #f6f8fb);
}
.prop-section__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.prop-section__head strong {
  color: var(--text-primary);
  font-size: 13px;
}
.prop-section__head span {
  color: var(--text-tertiary);
  font-size: 11px;
}
.prop-section__body {
  display: grid;
  gap: 8px;
}
.prop-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}
.prop-field > span,
.prop-switch > span {
  color: var(--text-secondary);
  font-size: 12px;
}
.prop-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
</style>
