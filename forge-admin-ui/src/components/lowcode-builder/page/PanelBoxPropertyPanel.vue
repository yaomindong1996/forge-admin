<script setup>
/**
 * 自由布局「面板容器」：类 div，默认可设宽高、背景色/渐变/图，内部可嵌多组件。
 */
import { computed } from 'vue'

const props = defineProps({
  block: { type: Object, required: true },
})

const emit = defineEmits(['patchProps'])

const p = computed(() => props.block?.props || {})

const backgroundModeOptions = [
  { label: '纯色', value: 'solid' },
  { label: '渐变', value: 'gradient' },
  { label: '图片', value: 'image' },
]

const backgroundSizeOptions = [
  { label: '覆盖', value: 'cover' },
  { label: '包含', value: 'contain' },
  { label: '拉伸', value: '100% 100%' },
  { label: '原始', value: 'auto' },
]

function patch(partial = {}) {
  emit('patchProps', partial)
}
</script>

<template>
  <div class="panel-prop-panel">
    <section class="prop-section">
      <header class="prop-section__head">
        <strong>尺寸与边框</strong>
        <span>默认一块可放内容的容器</span>
      </header>
      <div class="prop-section__body">
        <label class="prop-field">
          <span>可选标题</span>
          <n-input
            :value="p.title || ''"
            size="small"
            placeholder="留空则不显示标题"
            @update:value="patch({ title: $event || '' })"
          />
        </label>
        <label class="prop-field">
          <span>最小高度</span>
          <n-input-number
            :value="p.minHeight ?? 160"
            :min="40"
            :max="1200"
            size="small"
            :show-button="false"
            @update:value="patch({ minHeight: $event ?? 160 })"
          />
        </label>
        <label class="prop-field">
          <span>最小宽度</span>
          <n-input-number
            :value="p.minWidth ?? 0"
            :min="0"
            :max="1600"
            size="small"
            :show-button="false"
            @update:value="patch({ minWidth: $event ?? 0 })"
          />
        </label>
        <label class="prop-field">
          <span>内边距</span>
          <n-input-number
            :value="p.contentPadding ?? 12"
            :min="0"
            :max="80"
            size="small"
            :show-button="false"
            @update:value="patch({ contentPadding: $event ?? 12 })"
          />
        </label>
        <label class="prop-field">
          <span>圆角</span>
          <n-input-number
            :value="p.borderRadius ?? 8"
            :min="0"
            :max="48"
            size="small"
            :show-button="false"
            @update:value="patch({ borderRadius: $event ?? 8 })"
          />
        </label>
        <label class="prop-field prop-field--row">
          <span>显示边框</span>
          <n-switch :value="p.bordered !== false" size="small" @update:value="patch({ bordered: $event })" />
        </label>
        <label class="prop-field">
          <span>边框色</span>
          <n-color-picker
            :value="p.borderColor || '#e4e4e7'"
            :show-alpha="true"
            size="small"
            @update:value="patch({ borderColor: $event || '#e4e4e7' })"
          />
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
            :value="p.backgroundMode || 'solid'"
            :options="backgroundModeOptions"
            size="small"
            @update:value="patch({ backgroundMode: $event || 'solid' })"
          />
        </label>
        <label class="prop-field">
          <span>背景色</span>
          <n-color-picker
            :value="p.backgroundColor || '#ffffff'"
            :show-alpha="true"
            size="small"
            @update:value="patch({ backgroundColor: $event || '#ffffff' })"
          />
        </label>
        <template v-if="(p.backgroundMode || 'solid') === 'gradient'">
          <label class="prop-field">
            <span>渐变起始</span>
            <n-color-picker
              :value="p.gradientFrom || '#eff6ff'"
              size="small"
              @update:value="patch({ gradientFrom: $event || '#eff6ff' })"
            />
          </label>
          <label class="prop-field">
            <span>渐变结束</span>
            <n-color-picker
              :value="p.gradientTo || '#ffffff'"
              size="small"
              @update:value="patch({ gradientTo: $event || '#ffffff' })"
            />
          </label>
          <label class="prop-field">
            <span>渐变角度</span>
            <n-input-number
              :value="p.gradientAngle ?? 160"
              :min="0"
              :max="360"
              size="small"
              :show-button="false"
              @update:value="patch({ gradientAngle: $event ?? 160 })"
            />
          </label>
        </template>
        <template v-if="p.backgroundMode === 'image'">
          <label class="prop-field">
            <span>背景图 URL</span>
            <n-input
              :value="p.backgroundImage || ''"
              size="small"
              placeholder="https://..."
              @update:value="patch({ backgroundImage: $event || '' })"
            />
          </label>
          <label class="prop-field">
            <span>图片尺寸</span>
            <n-select
              :value="p.backgroundSize || 'cover'"
              :options="backgroundSizeOptions"
              size="small"
              @update:value="patch({ backgroundSize: $event || 'cover' })"
            />
          </label>
        </template>
      </div>
      <p class="prop-hint">
        适合做分区底色、侧栏、头图区；可在内部拖入多个图表或列表。
      </p>
    </section>
  </div>
</template>

<style scoped>
.panel-prop-panel {
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
.prop-field > span {
  color: var(--text-secondary);
  font-size: 12px;
}
.prop-field--row {
  grid-template-columns: 1fr auto;
  align-items: center;
}
.prop-hint {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.5;
}
</style>
