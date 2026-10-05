<template>
  <section class="theme-editor">
    <!-- 一键方案 -->
    <div class="theme-presets" aria-label="配色方案">
      <button
        v-for="preset in navigationPresets" :key="preset.key" type="button"
        :class="{ active: matchesPreset(preset) }" @click="applyPreset(preset)"
      >
        <span class="preset-colors" aria-hidden="true">
          <span :style="{ background: preset.header }" /><span :style="{ background: preset.primary }" />
        </span>
        {{ preset.name }}
      </button>
    </div>
    <!-- 基础色；文字和图标不要求用户逐个配置 -->
    <div class="theme-color-fields">
      <label v-for="field in baseFields" :key="field.key">
        <span>{{ field.label }}</span>
        <n-color-picker
          :value="baseValue(field.key)" :show-alpha="false" :modes="['hex']"
          @update:value="value => updateBase(field.key, value)"
        />
      </label>
    </div>
    <p class="theme-guidance">
      {{ scopeNote }} 文字、图标与选中态自动适配，深色模式使用独立深色表面。
    </p>
    <div v-if="layout !== 'empty'" class="theme-preview-tools">
      <span>效果预览</span>
      <div class="preview-modes" role="group" aria-label="预览模式">
        <button type="button" :aria-pressed="!previewDark" @click="previewDark = false">
          浅色
        </button>
        <button type="button" :aria-pressed="previewDark" @click="previewDark = true">
          深色
        </button>
      </div>
    </div>
    <!-- 与当前草稿绑定的小预览，不污染系统正在使用的主题 -->
    <div v-if="layout !== 'empty'" class="theme-sample" :style="sampleStyle" aria-label="导航配色预览">
      <div v-if="hasHeader" class="sample-header">
        <i class="i-lucide:panel-left" /><span>系统导航</span><i class="i-lucide:bell" />
      </div>
      <div class="sample-body">
        <div class="sample-side">
          <span>业务管理</span><span class="selected">当前菜单</span>
        </div><span>内容工作区</span>
      </div>
    </div>
    <n-collapse v-if="layout !== 'empty'" class="advanced-theme" :default-expanded-names="[]">
      <n-collapse-item name="advanced" title="高级自定义">
        <!-- 深色独立配置不占首屏，选择模式只调整本地预览 -->
        <div class="theme-preview-tools custom-theme-mode">
          <span>自定义配色</span>
          <div class="preview-modes" role="group" aria-label="自定义配色模式">
            <button type="button" :aria-pressed="!advancedDark" @click="selectAdvancedMode(false)">
              浅色
            </button>
            <button type="button" :aria-pressed="advancedDark" @click="selectAdvancedMode(true)">
              深色
            </button>
          </div>
        </div>
        <div v-if="advancedDark" class="theme-color-fields dark-base-fields">
          <label v-for="field in darkBaseFields" :key="field.key">
            <span>{{ field.label }}</span>
            <n-color-picker
              :value="pathValue(field.path)" :show-alpha="false" :modes="['hex']"
              @update:value="value => updateDarkBase(field.key, value)"
            />
          </label>
        </div>
        <div class="manual-mode">
          <span>手动配置文字和状态色</span>
          <n-switch :value="model.navigationMode === 'custom'" @update:value="setManualMode" />
        </div>
        <p class="theme-guidance">
          历史自定义保持不变。低对比颜色在显示时自动保护；修改基础色会重新启用自动配色。
        </p>
        <div v-if="model.navigationMode === 'custom'" class="theme-color-fields">
          <label v-for="field in advancedFields" :key="field.path">
            <span>{{ field.label }}</span>
            <n-color-picker
              :value="pathValue(field.path)" :show-alpha="false" :modes="['hex']"
              @update:value="value => updateCustom(field.path, value)"
            />
          </label>
        </div>
      </n-collapse-item>
    </n-collapse>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { defaultThemeConfig } from '@/config/theme.config'
import {
  createNavigationTheme,
  navigationPresets,
  resolveNavigationTheme,
  solidColor,
  updateNavigationBases,
} from '@/utils/navigation-theme'

const props = defineProps({ layout: { type: String, default: 'normal' } })
const model = defineModel({ type: Object, required: true })
const previewDark = ref(false)
const advancedDark = ref(false)
const hasHeader = computed(() => !['simple', 'bento', 'empty'].includes(props.layout))
const baseFields = computed(() => [
  { key: 'primary', label: '品牌主色' },
  ...(['simple', 'bento', 'empty'].includes(props.layout) ? [] : [{ key: 'header', label: '顶栏背景' }]),
  ...(props.layout === 'empty' ? [] : [{ key: 'side', label: '导航背景' }]),
])
const advancedFields = computed(() => {
  const fields = [
    { path: 'header.textColor', label: '顶栏文字 / 工具' },
    { path: 'topMenu.textColorActive', label: '顶栏选中态' },
    { path: 'sideMenu.textColor', label: '导航文字 / 图标' },
    { path: 'sideMenu.textColorActive', label: '导航选中文字' },
    { path: 'sideMenu.backgroundColorActive', label: '导航选中背景' },
    { path: 'sideMenu.backgroundColorHover', label: '导航悬停背景' },
  ]
  const suffix = advancedDark.value ? 'Dark.' : '.'
  return fields.filter(field => hasHeader.value || field.path.startsWith('sideMenu.'))
    .map(field => ({ ...field, path: field.path.replace('.', suffix) }))
})
const darkBaseFields = computed(() => [
  ...(hasHeader.value ? [{ key: 'header', path: 'headerDark.backgroundColor', label: '深色顶栏背景' }] : []),
  { key: 'side', path: 'sideMenuDark.backgroundColor', label: '深色导航背景' },
])
const scopeNote = computed(() => {
  if (props.layout === 'empty') {
    return '空白布局没有导航区域，配色用于页面控件。'
  }
  if (props.layout === 'immersive') {
    return '导航背景用于展开的菜单面板。'
  }
  if (props.layout === 'business-workbench') {
    return '顶栏与展开的业务菜单共用这套配色。'
  }
  if (['simple', 'bento'].includes(props.layout)) {
    return '当前布局只有侧边导航，不需要配置顶栏。'
  }
  return '顶栏和侧边导航在所有后台布局中保持一致。'
})
const rendered = computed(() => resolveNavigationTheme(model.value, defaultThemeConfig, previewDark.value))
const sampleStyle = computed(() => ({
  '--sample-header': rendered.value.header.backgroundColor,
  '--sample-header-text': rendered.value.header.textColor,
  '--sample-side': rendered.value.sideMenu.backgroundColor,
  '--sample-side-text': rendered.value.sideMenu.textColor,
  '--sample-active': rendered.value.sideMenu.backgroundColorActive,
  '--sample-active-text': rendered.value.sideMenu.textColorActive,
  '--sample-content-bg': previewDark.value ? '#101014' : '#F5F7FA',
  '--sample-content-text': previewDark.value ? '#E5E7EB' : '#4E5969',
}))
function baseValue(key) {
  if (key === 'primary')
    return model.value.primaryColor
  return model.value[key === 'side' ? 'sideMenu' : 'header']?.backgroundColor
}
function pathValue(path) {
  const [group, field] = path.split('.')
  return solidColor(model.value[group]?.[field], defaultThemeConfig[group]?.[field])
}
function updateBase(key, value) {
  model.value = updateNavigationBases(model.value, { [key]: value })
}
function applyPreset(preset) {
  model.value = updateNavigationBases(model.value, preset)
}
function matchesPreset(preset) {
  return baseValue('primary')?.toLowerCase() === preset.primary.toLowerCase()
    && baseValue('header')?.toLowerCase() === preset.header.toLowerCase()
    && baseValue('side')?.toLowerCase() === preset.side.toLowerCase()
}
function selectAdvancedMode(isDark) {
  advancedDark.value = isDark
  previewDark.value = isDark
}
function updateDarkBase(key, value) {
  model.value = updateNavigationBases(model.value, { [key]: value }, true)
}
function setManualMode(enabled) {
  if (!enabled) {
    model.value = updateNavigationBases(model.value, {}, advancedDark.value)
    return
  }
  const suffix = advancedDark.value ? 'Dark' : ''
  const generated = createNavigationTheme({
    primary: baseValue('primary'),
    header: model.value[`header${suffix}`]?.backgroundColor,
    side: model.value[`sideMenu${suffix}`]?.backgroundColor,
  })
  const next = { ...model.value, navigationMode: 'custom' }
  for (const group of ['header', 'topMenu', 'sideMenu']) {
    next[group + suffix] = { ...generated[group], ...model.value[group + suffix] }
  }
  model.value = next
}
function updateCustom(path, value) {
  const [group, field] = path.split('.')
  const kind = group.replace(/Dark$/, '')
  const patch = { [field]: value }
  if (field === 'textColor') {
    patch[kind === 'header' ? 'brandTitleTextColor' : 'iconColor'] = value
  }
  if (field === 'textColorActive') {
    patch[kind === 'topMenu' ? 'iconActiveColor' : 'iconColorActive'] = value
    if (kind === 'topMenu') {
      patch.textColorActiveHorizontal = value
      patch.textColorActiveHover = value
    }
  }
  model.value = { ...model.value, navigationMode: 'custom', [group]: { ...model.value[group], ...patch } }
}
</script>

<style scoped>
.theme-editor {
  display: grid;
  gap: 12px;
  width: 100%;
}
.theme-presets {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.theme-presets button {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 6px 10px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
}
.theme-presets button:hover,
.theme-presets button.active {
  border-color: var(--primary-color);
}
.theme-presets button.active {
  color: var(--primary-color);
  background: color-mix(in srgb, var(--primary-color) 5%, var(--bg-primary));
}
.preset-colors {
  display: flex;
  flex-shrink: 0;
  border: 1px solid var(--border-light);
  border-radius: 3px;
  overflow: hidden;
}
.preset-colors span {
  width: 12px;
  height: 18px;
}
.theme-color-fields {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.theme-color-fields label {
  display: grid;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
  color: var(--text-secondary);
}
.theme-guidance {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}
.theme-sample {
  color: var(--sample-side-text);
  background: var(--sample-side);
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  font-size: 12px;
}
.theme-preview-tools,
.preview-modes {
  display: flex;
  align-items: center;
  gap: 8px;
}
.theme-preview-tools {
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
}
.preview-modes button {
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  color: var(--text-secondary);
  background: var(--bg-primary);
  font: inherit;
  cursor: pointer;
}
.preview-modes button[aria-pressed='true'] {
  border-color: var(--primary-color);
  color: var(--primary-color);
}
.theme-editor button:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}
.sample-header {
  height: 36px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  background: var(--sample-header);
  color: var(--sample-header-text);
}
.sample-header span {
  flex: 1;
}
.sample-body {
  display: flex;
  min-height: 80px;
  color: var(--sample-content-text);
  background: var(--sample-content-bg);
}
.sample-body > span {
  padding: 16px;
}
.sample-side {
  display: grid;
  gap: 4px;
  align-content: center;
  width: 120px;
  padding: 8px;
  background: var(--sample-side);
  color: var(--sample-side-text);
}
.sample-side span {
  padding: 6px 8px;
  border-radius: 4px;
}
.sample-side .selected {
  background: var(--sample-active);
  color: var(--sample-active-text);
}
.manual-mode {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 12px;
}
.custom-theme-mode,
.dark-base-fields {
  margin-bottom: 12px;
}
.advanced-theme .theme-color-fields {
  margin-top: 12px;
}
@media (max-width: 600px) {
  .theme-color-fields {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
