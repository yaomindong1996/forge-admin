<template>
  <n-drawer
    v-model:show="visible"
    placement="right"
    width="400"
    :on-after-leave="handleClose"
  >
    <n-drawer-content title="主题定制" closable>
      <div class="theme-customizer">
        <!-- 颜色主题 -->
        <div class="customizer-section">
          <div class="section-header">
            <i class="i-material-symbols:palette" />
            <span class="section-title">颜色主题</span>
          </div>

          <div class="color-presets">
            <div
              v-for="preset in colorPresets"
              :key="preset.name"
              class="color-preset"
              :style="{ background: preset.color }"
              :class="{ 'preset-active': activeColorPreset === preset.name }"
              :title="preset.name"
              @click="applyColorPreset(preset)"
            />
          </div>

          <div class="color-picker-wrapper">
            <div class="color-picker-label">
              主色调
            </div>
            <n-color-picker
              v-model:value="customTheme.primaryColor"
              :show-alpha="false"
              :modes="['hex']"
              size="small"
            />
          </div>
        </div>

        <!-- 圆角设置 -->
        <div class="customizer-section">
          <div class="section-header">
            <i class="i-material-symbols:rounded-corner" />
            <span class="section-title">圆角设置</span>
          </div>

          <div class="radius-control">
            <div class="radius-labels">
              <span class="label-item">直角</span>
              <span class="label-item">圆润</span>
            </div>
            <n-slider
              v-model:value="customTheme.borderRadius"
              :min="0"
              :max="24"
              :step="2"
              :marks="radiusMarks"
            />
            <div class="radius-value">
              {{ customTheme.borderRadius }}px
            </div>
          </div>

          <div class="radius-preview">
            <div
              class="preview-box"
              :style="{ borderRadius: `${customTheme.borderRadius}px` }"
            >
              预览
            </div>
          </div>
        </div>

        <!-- 间距设置 -->
        <div class="customizer-section">
          <div class="section-header">
            <i class="i-material-symbols:space-bar" />
            <span class="section-title">间距密度</span>
          </div>

          <div class="spacing-options">
            <div
              v-for="option in spacingOptions"
              :key="option.value"
              class="spacing-option"
              :class="{ 'option-active': customTheme.spacingScale === option.value }"
              @click="customTheme.spacingScale = option.value"
            >
              <div class="option-preview">
                <div
                  v-for="i in 3"
                  :key="i"
                  class="option-bar"
                  :style="{
                    height: `${20 * i * option.value}px`,
                    gap: `${8 * option.value}px`,
                  }"
                />
              </div>
              <span class="option-label">{{ option.label }}</span>
            </div>
          </div>
        </div>

        <!-- 字体大小 -->
        <div class="customizer-section">
          <div class="section-header">
            <i class="i-material-symbols:text-fields" />
            <span class="section-title">字体大小</span>
          </div>

          <n-select
            v-model:value="customTheme.fontSize"
            :options="fontSizeOptions"
            size="small"
          />

          <div class="font-preview">
            <div class="preview-text" :style="{ fontSize: `${customTheme.fontSize}px` }">
              这是预览文本
            </div>
            <div class="preview-subtitle">
              Aa
            </div>
          </div>
        </div>

        <!-- 布局设置 -->
        <div class="customizer-section">
          <div class="section-header">
            <i class="i-material-symbols:view-quilt" />
            <span class="section-title">布局设置</span>
          </div>

          <div class="layout-options">
            <div class="layout-option">
              <span class="option-label">紧凑模式</span>
              <n-switch v-model:value="customTheme.compact" />
            </div>

            <div class="layout-option">
              <span class="option-label">显示玻璃态</span>
              <n-switch v-model:value="customTheme.glassmorphism" />
            </div>

            <div class="layout-option">
              <span class="option-label">显示边框</span>
              <n-switch v-model:value="customTheme.showBorders" />
            </div>
          </div>
        </div>

        <!-- 动画设置 -->
        <div class="customizer-section">
          <div class="section-header">
            <i class="i-material-symbols:animation" />
            <span class="section-title">动画设置</span>
          </div>

          <div class="animation-control">
            <div class="animation-label">
              <span>动画速度</span>
              <span class="animation-value">{{ animationLabels[customTheme.animationSpeed] }}</span>
            </div>
            <n-slider
              v-model:value="customTheme.animationSpeed"
              :min="0"
              :max="2"
              :step="1"
              :marks="{ 0: '慢', 1: '正常', 2: '快' }"
            />
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="customizer-actions">
          <n-button
            type="primary"
            size="large"
            block
            @click="applyTheme"
          >
            <i class="i-material-symbols:check-circle" />
            应用主题
          </n-button>

          <div class="action-row">
            <n-button
              secondary
              size="large"
              block
              @click="resetTheme"
            >
              重置默认
            </n-button>

            <n-button
              secondary
              size="large"
              block
              @click="exportTheme"
            >
              导出配置
            </n-button>
          </div>
        </div>
      </div>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { message } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useAppStore } from '@/store'

const props = defineProps({
  show: Boolean,
})

const emit = defineEmits(['update:show'])

const appStore = useAppStore()

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value),
})

const activeColorPreset = ref('default')

// 颜色预设
const colorPresets = ref([
  { name: '科技蓝', color: '#3b82f6', value: '#3b82f6' },
  { name: '紫色', color: '#8b5cf6', value: '#8b5cf6' },
  { name: '绿色', color: '#10b981', value: '#10b981' },
  { name: '橙色', color: '#f59e0b', value: '#f59e0b' },
  { name: '红色', color: '#ef4444', value: '#ef4444' },
  { name: '深灰', color: '#6b7280', value: '#6b7280' },
])

// 圆角标记
const radiusMarks = computed(() => ({
  0: '0',
  8: '8',
  16: '16',
  24: '24',
}))

// 间距选项
const spacingOptions = [
  { label: '紧凑', value: 0.75 },
  { label: '标准', value: 1 },
  { label: '舒适', value: 1.25 },
  { label: '宽松', value: 1.5 },
]

// 字体大小选项
const fontSizeOptions = [
  { label: '小 (13px)', value: 13 },
  { label: '默认 (14px)', value: 14 },
  { label: '中等 (15px)', value: 15 },
  { label: '大 (16px)', value: 16 },
  { label: '特大 (17px)', value: 17 },
]

// 动画速度标签
const animationLabels = {
  0: '慢',
  1: '正常',
  2: '快',
}

// 自定义主题
const customTheme = ref({
  primaryColor: appStore.themeConfig?.primaryColor || '#3b82f6',
  borderRadius: appStore.themeConfig?.borderRadius || 8,
  spacingScale: appStore.themeConfig?.spacingScale || 1,
  fontSize: appStore.themeConfig?.fontSize || 14,
  compact: appStore.themeConfig?.compact || false,
  glassmorphism: appStore.themeConfig?.glassmorphism || true,
  showBorders: appStore.themeConfig?.showBorders || true,
  animationSpeed: appStore.themeConfig?.animationSpeed || 1,
})

// 应用颜色预设
function applyColorPreset(preset) {
  activeColorPreset.value = preset.name
  customTheme.value.primaryColor = preset.value
}

// 应用主题
function applyTheme() {
  const theme = {
    primaryColor: customTheme.value.primaryColor,
    borderRadius: customTheme.value.borderRadius,
    spacingScale: customTheme.value.spacingScale,
    fontSize: customTheme.value.fontSize,
    compact: customTheme.value.compact,
    glassmorphism: customTheme.value.glassmorphism,
    showBorders: customTheme.value.showBorders,
    animationSpeed: customTheme.value.animationSpeed,
  }

  appStore.updateThemeConfig(theme)
  applyThemeToDOM(theme)
  message.success('主题已应用')
}

// 应用主题到DOM
function applyThemeToDOM(theme) {
  const root = document.documentElement

  // 主色调
  root.style.setProperty('--base-primary-color', theme.primaryColor)

  // 圆角
  root.style.setProperty('--radius-button', `${theme.borderRadius}px`)
  root.style.setProperty('--radius-input', `${theme.borderRadius}px`)
  root.style.setProperty('--radius-card', `${theme.borderRadius * 1.5}px`)

  // 间距
  const baseSpacing = 8 * theme.spacingScale
  root.style.setProperty('--space-2', `${baseSpacing}px`)
  root.style.setProperty('--space-3', `${baseSpacing * 1.5}px`)
  root.style.setProperty('--space-4', `${baseSpacing * 2}px`)
  root.style.setProperty('--space-6', `${baseSpacing * 3}px`)

  // 字体大小
  root.style.setProperty('--font-size-base', `${theme.fontSize}px`)

  // 动画速度
  const speedMap = { 0: 400, 1: 200, 2: 150 }
  root.style.setProperty('--transition-base', `${speedMap[theme.animationSpeed]}ms`)
}

// 重置主题
function resetTheme() {
  const defaultTheme = {
    primaryColor: '#3b82f6',
    borderRadius: 8,
    spacingScale: 1,
    fontSize: 14,
    compact: false,
    glassmorphism: true,
    showBorders: true,
    animationSpeed: 1,
  }

  customTheme.value = { ...defaultTheme }
  activeColorPreset.value = '科技蓝'
  applyTheme()
}

// 导出主题
function exportTheme() {
  const theme = JSON.stringify(customTheme.value, null, 2)
  const blob = new Blob([theme], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'theme-config.json'
  a.click()
  URL.revokeObjectURL(url)
  message.success('主题配置已导出')
}

// 处理关闭
function handleClose() {
  // 可以在这里添加保存逻辑
}

// 监听主色调变化
watch(() => customTheme.value.primaryColor, (newColor) => {
  const preset = colorPresets.value.find(p => p.value === newColor)
  if (preset) {
    activeColorPreset.value = preset.name
  }
})
</script>

<style scoped>
.theme-customizer {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  padding: var(--space-2);
}

.customizer-section {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  border: 1px solid var(--border-light);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.section-header i {
  font-size: 20px;
  color: var(--primary-500);
}

.section-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

/* 颜色预设 */
.color-presets {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
}

.color-preset {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition);
  border: 2px solid transparent;
  box-shadow: var(--shadow-sm);
}

.color-preset:hover {
  transform: scale(1.1);
  box-shadow: var(--shadow-md);
}

.preset-active {
  border-color: var(--text-primary);
  transform: scale(1.15);
}

.color-picker-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.color-picker-label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

/* 圆角设置 */
.radius-control {
  margin-bottom: var(--space-4);
}

.radius-labels {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.label-item {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.radius-value {
  text-align: center;
  margin-top: var(--space-2);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.radius-preview {
  display: flex;
  justify: center;
  padding: var(--space-4);
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px dashed var(--border-default);
}

.preview-box {
  width: 80px;
  height: 80px;
  background: var(--primary-500);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  transition: all var(--transition);
}

/* 间距设置 */
.spacing-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-2);
}

.spacing-option {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-3);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition);
  background: var(--bg-primary);
  border: 1px solid transparent;
}

.spacing-option:hover {
  background: var(--bg-tertiary);
  transform: translateY(-2px);
}

.option-active {
  border-color: var(--primary-500);
  background: var(--primary-50);
}

.option-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
}

.option-bar {
  width: 8px;
  background: var(--primary-300);
  border-radius: var(--radius-sm);
}

.option-label {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  text-align: center;
}

/* 字体大小 */
.font-preview {
  margin-top: var(--space-4);
  padding: var(--space-4);
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.preview-text {
  color: var(--text-primary);
  font-weight: var(--font-weight-medium);
}

.preview-subtitle {
  font-size: 24px;
  color: var(--text-tertiary);
  opacity: 0.5;
}

/* 布局设置 */
.layout-options {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.layout-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.layout-option .option-label {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

/* 动画设置 */
.animation-control {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.animation-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.animation-label span:first-child {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

.animation-value {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

/* 操作按钮 */
.customizer-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.action-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-2);
}

/* 暗色模式适配 */
.dark .spacing-option.option-active {
  background: var(--primary-900);
}

.dark .preview-box {
  color: white;
}

.dark .option-bar {
  background: var(--primary-700);
}
</style>
