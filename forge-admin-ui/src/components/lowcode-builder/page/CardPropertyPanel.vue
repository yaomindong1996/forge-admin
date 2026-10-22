<script setup>
/**
 * 自由布局「卡片容器」属性：标题竖线、背景/渐变/图片、尺寸与嵌套说明。
 */
import { computed } from 'vue'

const props = defineProps({
  block: { type: Object, required: true },
})

const emit = defineEmits(['patchProps'])

const p = computed(() => props.block?.props || {})

const sizeOptions = [
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
  { label: '超大', value: 'huge' },
]

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

const titleWeightOptions = [
  { label: '常规', value: '400' },
  { label: '中等', value: '500' },
  { label: '加粗', value: '700' },
  { label: '特粗', value: '800' },
]

function patch(partial = {}) {
  emit('patchProps', partial)
}
</script>

<template>
  <div class="card-prop-panel">
    <section class="prop-section">
      <header class="prop-section__head">
        <strong>基础</strong>
        <span>标题与尺寸</span>
      </header>
      <div class="prop-section__body">
        <label class="prop-field">
          <span>卡片标题</span>
          <n-input
            :value="p.title || ''"
            size="small"
            placeholder="卡片标题"
            @update:value="patch({ title: $event || '' })"
          />
        </label>
        <label class="prop-field">
          <span>说明文案</span>
          <n-input
            :value="p.content || ''"
            type="textarea"
            :rows="2"
            size="small"
            placeholder="可选，显示在标题下方"
            @update:value="patch({ content: $event || '' })"
          />
        </label>
        <label class="prop-field">
          <span>尺寸</span>
          <n-select
            :value="p.size || 'medium'"
            :options="sizeOptions"
            size="small"
            @update:value="patch({ size: $event || 'medium' })"
          />
        </label>
        <label class="prop-field">
          <span>内容区内边距</span>
          <n-input-number
            :value="p.contentPadding ?? 12"
            :min="0"
            :max="80"
            size="small"
            :show-button="false"
            @update:value="patch({ contentPadding: $event ?? 12 })"
          />
        </label>
      </div>
    </section>

    <section class="prop-section">
      <header class="prop-section__head">
        <strong>标题样式</strong>
        <span>竖线与字号</span>
      </header>
      <div class="prop-section__body">
        <label class="prop-field prop-field--row">
          <span>标题前竖线</span>
          <n-switch
            :value="p.showTitleAccent !== false"
            size="small"
            @update:value="patch({ showTitleAccent: $event })"
          />
        </label>
        <label class="prop-field">
          <span>竖线颜色</span>
          <n-color-picker
            :value="p.titleAccentColor || '#2563eb'"
            :show-alpha="false"
            size="small"
            @update:value="patch({ titleAccentColor: $event || '#2563eb' })"
          />
        </label>
        <label class="prop-field">
          <span>标题字号</span>
          <n-input-number
            :value="p.titleFontSize ?? 14"
            :min="12"
            :max="28"
            size="small"
            :show-button="false"
            @update:value="patch({ titleFontSize: $event ?? 14 })"
          />
        </label>
        <label class="prop-field">
          <span>标题字重</span>
          <n-select
            :value="String(p.titleFontWeight || '700')"
            :options="titleWeightOptions"
            size="small"
            @update:value="patch({ titleFontWeight: $event || '700' })"
          />
        </label>
        <label class="prop-field">
          <span>标题颜色</span>
          <n-color-picker
            :value="p.titleColor || null"
            :show-alpha="true"
            size="small"
            @update:value="patch({ titleColor: $event || '' })"
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
            :value="p.backgroundColor || null"
            :show-alpha="true"
            size="small"
            @update:value="patch({ backgroundColor: $event || '' })"
          />
        </label>
        <template v-if="(p.backgroundMode || 'solid') === 'gradient'">
          <label class="prop-field">
            <span>渐变起始</span>
            <n-color-picker
              :value="p.gradientFrom || '#eff6ff'"
              :show-alpha="false"
              size="small"
              @update:value="patch({ gradientFrom: $event || '#eff6ff' })"
            />
          </label>
          <label class="prop-field">
            <span>渐变结束</span>
            <n-color-picker
              :value="p.gradientTo || '#ffffff'"
              :show-alpha="false"
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
    </section>

    <section class="prop-section">
      <header class="prop-section__head">
        <strong>外观开关</strong>
      </header>
      <div class="prop-section__body prop-section__body--switches">
        <label class="prop-field prop-field--row">
          <span>边框</span>
          <n-switch :value="p.bordered !== false" size="small" @update:value="patch({ bordered: $event })" />
        </label>
        <label class="prop-field prop-field--row">
          <span>嵌入模式</span>
          <n-switch :value="!!p.embedded" size="small" @update:value="patch({ embedded: $event })" />
        </label>
        <label class="prop-field prop-field--row">
          <span>悬停效果</span>
          <n-switch :value="!!p.hoverable" size="small" @update:value="patch({ hoverable: $event })" />
        </label>
      </div>
      <p class="prop-hint">
        可把组件拖进卡片，或在画布上右键「插入到卡片」。
      </p>
    </section>
  </div>
</template>

<style scoped>
.card-prop-panel {
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
.prop-section__body--switches {
  gap: 6px;
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
