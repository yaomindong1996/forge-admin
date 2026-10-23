<template>
  <div class="icon-selector">
    <n-button v-if="trigger" ghost :disabled="disabled" class="icon-selector-trigger" @click="open">
      <template v-if="modelValue">
        <IconRenderer :icon="modelValue" custom-class="icon-preview" />
        <span class="icon-text">{{ formatDisplayName(modelValue) }}</span>
      </template>
      <span v-else class="icon-selector-empty">
        <i class="i-material-symbols:imagesmode-outline" />
        <span>选择图标</span>
      </span>
    </n-button>

    <n-drawer v-model:show="showDrawer" width="min(720px, calc(100vw - 24px))" placement="right">
      <n-drawer-content closable>
        <template #header>
          <div class="drawer-header">
            <div>
              <span class="drawer-title">选择图标</span>
              <span class="drawer-subtitle">字体图标、内置图片和上传图片统一维护</span>
            </div>
          </div>
        </template>

        <div class="icon-selector-popover">
          <div class="icon-selector-header">
            <div class="icon-search-row">
              <n-input
                v-model:value="searchText"
                placeholder="搜索图标名称 / 类名"
                clearable
                size="medium"
              >
                <template #prefix>
                  <i class="i-material-symbols:search" />
                </template>
              </n-input>
            </div>

            <div v-if="modelValue" class="selected-icon-summary">
              <span class="selected-icon-preview">
                <IconRenderer :icon="modelValue" :size="30" />
              </span>
              <div class="selected-icon-copy">
                <span>当前已选</span>
                <strong>{{ formatDisplayName(modelValue) }}</strong>
              </div>
              <n-button type="error" secondary size="small" @click="clearSelection">
                <template #icon>
                  <i class="i-material-symbols:delete-outline" />
                </template>
                清除
              </n-button>
            </div>

            <n-tabs v-model:value="activeTab" type="segment" size="small">
              <n-tab v-if="currentImagePreview" name="currentImage">
                当前图片
              </n-tab>
              <n-tab name="ionicons">
                常用图标
              </n-tab>
              <n-tab name="local">
                本地图标
              </n-tab>
              <n-tab name="localImages">
                图片图标
              </n-tab>
              <n-tab name="uploadImage">
                上传图片
              </n-tab>
            </n-tabs>
          </div>

          <div ref="contentRef" class="icon-selector-content">
            <div v-if="activeTab === 'currentImage'" class="current-image-panel">
              <div class="current-image-preview selected">
                <IconRenderer :icon="modelValue" :size="44" />
                <div class="current-image-meta">
                  <div class="current-image-title">
                    当前图片图标
                  </div>
                  <div class="current-image-name">
                    {{ formatDisplayName(modelValue) }}
                  </div>
                </div>
              </div>
            </div>

            <div v-if="activeTab === 'ionicons'" class="icon-grid">
              <div
                v-for="icon in filteredIonicons"
                :key="icon"
                class="icon-item"
                :class="{ selected: isIconSelected(icon, 'ionicons') }"
                @click="selectIcon(icon)"
              >
                <component :is="getIoniconComponent(icon)" class="icon" />
                <div class="icon-name">
                  {{ formatIconName(icon) }}
                </div>
              </div>
              <div v-if="filteredIonicons.length === 0" class="no-icons">
                未找到匹配的图标
              </div>
            </div>

            <div v-if="activeTab === 'local'" class="icon-grid">
              <div
                v-for="icon in filteredLocalIcons"
                :key="icon"
                class="icon-item"
                :class="{ selected: isIconSelected(icon, 'local') }"
                @click="selectIcon(icon)"
              >
                <i :class="icon" class="text-20" />
                <div class="icon-name">
                  {{ icon }}
                </div>
              </div>
              <div v-if="filteredLocalIcons.length === 0" class="no-icons">
                未找到匹配的图标
              </div>
            </div>

            <div v-if="activeTab === 'localImages'" class="icon-grid">
              <div
                v-for="icon in filteredLocalImageIcons"
                :key="icon.value"
                class="icon-item"
                :class="{ selected: isIconSelected(icon.value, 'localImages') }"
                @click="selectLocalImageIcon(icon.value)"
              >
                <IconRenderer :icon="icon.value" :size="32" />
                <div class="icon-name">
                  {{ icon.displayName }}
                </div>
              </div>
              <div v-if="filteredLocalImageIcons.length === 0" class="no-icons">
                未找到匹配的图片图标
              </div>
            </div>

            <div v-if="activeTab === 'uploadImage'" class="upload-image-panel">
              <ImageUpload
                :model-value="uploadImageValue"
                :limit="1"
                :file-size="2"
                :file-type="['png', 'jpg', 'jpeg', 'webp', 'gif', 'svg']"
                business-type="menu-icon"
                :show-tip="true"
                value-type="string"
                @success="handleUploadSuccess"
                @update:model-value="handleUploadValueChange"
              />
            </div>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import * as ionicons from '@vicons/ionicons5'
import icons from 'isme:icons'
import { computed, nextTick, ref, watch } from 'vue'
import ImageUpload from '@/components/image-upload/index.vue'
import { getLocalImageIconName, isLocalImageIcon, localImageIcons } from '@/utils/local-image-icons'
import IconRenderer from './IconRenderer.vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  trigger: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const showDrawer = ref(false)
const searchText = ref('')
const activeTab = ref('ionicons')
const contentRef = ref(null)

// 获取所有 Ionicons 图标名称
const ioniconNames = Object.keys(ionicons)

// 过滤 Ionicons 图标
const filteredIonicons = computed(() => {
  if (!searchText.value)
    return ioniconNames
  return ioniconNames.filter(name =>
    name.toLowerCase().includes(searchText.value.toLowerCase()),
  )
})

// 过滤本地图标
const filteredLocalIcons = computed(() => {
  if (!searchText.value)
    return icons
  return icons.filter((name) => {
    // 对于带目录前缀的图标名称（如 feather:activity），只搜索文件名部分
    const displayName = name.includes(':') ? name.split(':')[1] : name
    return displayName.toLowerCase().includes(searchText.value.toLowerCase())
  })
})

const filteredLocalImageIcons = computed(() => {
  if (!searchText.value)
    return localImageIcons
  return localImageIcons.filter((icon) => {
    const keyword = searchText.value.toLowerCase()
    return icon.name.toLowerCase().includes(keyword)
      || icon.displayName.toLowerCase().includes(keyword)
  })
})

const currentImagePreview = computed(() => {
  const value = props.modelValue?.trim?.() || ''
  if (!value || isLocalImageIcon(value))
    return false
  return isImageIconValue(value)
})

const uploadImageValue = computed(() => currentImagePreview.value ? props.modelValue : '')

// 获取 Ionicons 图标组件
function getIoniconComponent(name) {
  return ionicons[name]
}

// 格式化图标名称显示
function formatIconName(name) {
  // 对于带目录前缀的图标名称（如 feather:activity），只显示文件名部分
  if (name.includes(':')) {
    return name.split(':')[1]
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/-/g, ' ')
      .replace(/^./, str => str.toUpperCase())
  }

  return name
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/-/g, ' ')
    .replace(/^./, str => str.toUpperCase())
}

// 选择图标
function selectIcon(iconName) {
  // 根据当前标签页添加对应的前缀
  let iconWithPrefix = ''
  if (activeTab.value === 'ionicons') {
    iconWithPrefix = `ionicons5:${iconName}`
  }
  else if (activeTab.value === 'local') {
    iconWithPrefix = `local:${iconName}`
  }
  emit('update:modelValue', iconWithPrefix)
  showDrawer.value = false // 选择后关闭抽屉
}

function selectLocalImageIcon(iconValue) {
  emit('update:modelValue', iconValue)
  showDrawer.value = false
}

function normalizeUploadedIconValue(value) {
  if (!value)
    return ''
  if (typeof value === 'string')
    return value
  return value.fileId || value.filePath || value.accessUrl || ''
}

function handleUploadSuccess(fileData) {
  const value = normalizeUploadedIconValue(fileData)
  if (!value)
    return
  emit('update:modelValue', value)
  showDrawer.value = false
}

function handleUploadValueChange(value) {
  const nextValue = normalizeUploadedIconValue(value)
  emit('update:modelValue', nextValue)
}

function isImageIconValue(value) {
  if (!value || typeof value !== 'string')
    return false

  const iconValue = value.trim()
  if (!iconValue || isLocalImageIcon(iconValue))
    return false

  return iconValue.startsWith('http://')
    || iconValue.startsWith('https://')
    || iconValue.startsWith('data:')
    || iconValue.startsWith('blob:')
    || iconValue.startsWith('/api/file/')
    || iconValue.startsWith('forge-file://')
    || /^[a-f0-9]{32}$/i.test(iconValue)
    || /\.(?:png|jpe?g|webp|gif|svg|avif)(?:\?.*)?$/i.test(iconValue)
}

function getInitialTab() {
  const value = props.modelValue?.trim?.() || ''
  if (!value)
    return 'ionicons'
  if (currentImagePreview.value)
    return 'currentImage'
  if (isLocalImageIcon(value))
    return 'localImages'
  if (value.startsWith('ionicons5:'))
    return 'ionicons'
  return 'local'
}

function scrollSelectedIntoView() {
  const selectedEl = contentRef.value?.querySelector?.('.icon-item.selected')
  selectedEl?.scrollIntoView?.({ block: 'center' })
}

function open() {
  if (props.disabled)
    return
  searchText.value = ''
  activeTab.value = getInitialTab()
  showDrawer.value = true
  nextTick(scrollSelectedIntoView)
}

// 清除选择
function clearSelection() {
  emit('update:modelValue', '')
  searchText.value = ''
  showDrawer.value = false
}

// 判断图标是否被选中
function isIconSelected(iconName, type) {
  if (!props.modelValue)
    return false

  if (type === 'localImages')
    return props.modelValue === iconName

  // 构建完整的图标名称（带前缀）
  const fullIconName = type === 'ionicons'
    ? `ionicons5:${iconName}`
    : `local:${iconName}`

  return props.modelValue === fullIconName
}

// 格式化显示名称（用于按钮显示）
function formatDisplayName(iconValue) {
  if (!iconValue)
    return ''

  if (isLocalImageIcon(iconValue))
    return getLocalImageIconName(iconValue)

  if (isImageIconValue(iconValue)) {
    const filename = iconValue.split(/[\\/]/).pop() || iconValue
    if (/^[a-f0-9]{32}$/i.test(iconValue))
      return '上传图片图标'
    return filename.length > 28 ? `${filename.slice(0, 12)}...${filename.slice(-10)}` : filename
  }

  // 移除前缀，只显示图标名称
  if (iconValue.includes(':')) {
    return iconValue.split(':')[1]
  }

  return iconValue
}

defineExpose({
  open,
})

watch(activeTab, () => {
  if (showDrawer.value)
    nextTick(scrollSelectedIntoView)
})
</script>

<style scoped>
.icon-selector {
  width: 100%;
}

.icon-selector-trigger {
  width: 100%;
}

.icon-selector-empty {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary, #4e5969);
}

.icon-selector-empty > i {
  font-size: 16px;
  line-height: 1;
  color: var(--text-tertiary, #8b95a8);
}

.icon-selector-input {
  display: flex;
  align-items: center;
}

.icon-selector-popover {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
  background: var(--bg-primary, #fff);
}

.icon-selector-header {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  background: var(--bg-primary, #fff);
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

.icon-search-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-selector-content {
  flex: 1;
  min-height: 0;
  max-height: calc(100vh - 190px);
  overflow-y: auto;
  padding: 12px 14px 16px;
  background: var(--bg-secondary, #f7f8fa);
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(86px, 1fr));
  gap: 10px;
}

.icon-grid::before {
  display: none;
}

.icon-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px 8px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  cursor: pointer;
  transition:
    background 0.16s ease,
    border-color 0.16s ease,
    transform 0.16s ease,
    box-shadow 0.16s ease;
  height: 86px;
  min-width: 0;
}

.icon-item:hover {
  background: color-mix(in srgb, var(--primary-color, #165dff) 5%, var(--bg-primary, #fff));
  border-color: color-mix(in srgb, var(--primary-color, #165dff) 28%, var(--border-light, #e5e7eb));
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgb(15 23 42 / 7%);
}

.icon-item.selected {
  color: var(--primary-color, #165dff);
  background: color-mix(in srgb, var(--primary-color, #165dff) 8%, var(--bg-primary, #fff));
  border-color: color-mix(in srgb, var(--primary-color, #165dff) 46%, var(--border-light, #e5e7eb));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--primary-color, #165dff) 36%, transparent);
}

.current-image-panel {
  padding: 4px 0;
}

.current-image-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--primary-color, #165dff) 36%, var(--border-light, #e5e7eb));
  border-radius: 6px;
  background: color-mix(in srgb, var(--primary-color, #165dff) 8%, var(--bg-primary, #fff));
}

.current-image-title {
  font-size: var(--font-size-base);
  font-weight: 500;
}

.current-image-name {
  margin-top: 4px;
  color: var(--text-secondary, #4e5969);
  font-size: var(--font-size-sm);
  word-break: break-all;
}

.upload-image-panel {
  padding: 12px;
  border: 1px dashed var(--border-default, #c9cdd4);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.icon {
  width: 28px;
  height: 28px;
  margin-bottom: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-name {
  font-size: var(--font-size-sm);
  color: var(--text-secondary, #4e5969);
  text-align: center;
  word-break: break-word;
  line-height: 1.2;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.icon-preview {
  width: 20px;
  height: 20px;
  margin-right: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-text {
  font-size: var(--font-size-sm);
  color: var(--text-secondary, #4e5969);
}

.no-icons {
  grid-column: 1 / -1;
  text-align: center;
  padding: 32px 20px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-primary, #fff);
  border: 1px dashed var(--border-light, #e5e7eb);
  border-radius: 6px;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.drawer-title {
  display: block;
  color: var(--text-primary, #1d2129);
  font-size: 16px;
  font-weight: 650;
  line-height: 1.3;
}

.drawer-subtitle {
  display: block;
  margin-top: 2px;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1.3;
}

.selected-icon-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 8px 10px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-secondary, #f7f8fa);
}

.selected-icon-preview {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 6px;
  color: var(--primary-color, #165dff);
  background: color-mix(in srgb, var(--primary-color, #165dff) 10%, var(--bg-primary, #fff));
}

.selected-icon-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.selected-icon-copy span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1.2;
}

.selected-icon-copy strong {
  min-width: 0;
  color: var(--text-primary, #1d2129);
  font-size: 13px;
  line-height: 1.25;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .icon-grid {
    grid-template-columns: repeat(auto-fill, minmax(76px, 1fr));
  }

  .icon-item {
    height: 80px;
  }
}
</style>
