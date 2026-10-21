<template>
  <section class="settings-section-card">
    <header>
      <h2>基础属性</h2>
      <p>这些信息用于应用中心、门户顶栏和发布快照。</p>
    </header>

    <n-form class="settings-basic-form" label-placement="top" :model="modelValue">
      <!-- 基本信息 -->
      <div class="settings-block">
        <h3 class="settings-block__title">基本信息</h3>
        <n-grid :cols="2" :x-gap="20" :y-gap="8" responsive="screen">
          <n-form-item-gi label="应用名称" required>
            <n-input
              :value="modelValue.applicationName"
              placeholder="请输入应用名称"
              @update:value="patch({ applicationName: $event })"
            />
          </n-form-item-gi>
          <n-form-item-gi label="应用编码">
            <n-input :value="modelValue.applicationCode" disabled />
          </n-form-item-gi>
          <n-form-item-gi label="应用图标">
            <div class="settings-icon-field">
              <IconSelector :model-value="modelValue.icon" @update:model-value="patch({ icon: $event })" />
            </div>
          </n-form-item-gi>
          <n-form-item-gi label="启用状态">
            <div class="settings-switch-row">
              <n-switch
                :value="modelValue.status"
                :checked-value="1"
                :unchecked-value="0"
                @update:value="patch({ status: $event })"
              />
              <span>{{ modelValue.status === 1 ? '应用已启用' : '应用已停用' }}</span>
            </div>
          </n-form-item-gi>
          <n-form-item-gi :span="2" label="应用说明">
            <n-input
              :value="modelValue.description"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 4 }"
              maxlength="500"
              show-count
              placeholder="简要说明应用用途，便于同事在应用中心识别"
              @update:value="patch({ description: $event })"
            />
          </n-form-item-gi>
        </n-grid>
      </div>

      <!-- 主题 -->
      <div class="settings-block">
        <h3 class="settings-block__title">应用主题</h3>
        <n-form-item label="主题色" :show-feedback="false">
          <div class="theme-color-row">
            <button
              v-for="color in presetColors"
              :key="color"
              type="button"
              class="theme-color-swatch"
              :class="{ active: modelValue.themeColor === color }"
              :style="{ backgroundColor: color }"
              :aria-label="`选择主题色 ${color}`"
              @click="patch({ themeColor: color })"
            />
            <n-color-picker
              :value="modelValue.themeColor"
              :modes="['hex']"
              :show-alpha="false"
              class="theme-color-picker"
              @update:value="patch({ themeColor: $event })"
            />
          </div>
        </n-form-item>
      </div>

      <!-- 水印 -->
      <div class="settings-block">
        <div class="settings-block__head">
          <h3 class="settings-block__title">应用水印</h3>
          <div class="settings-switch-row">
            <n-switch
              :value="modelValue.watermark?.enabled"
              @update:value="patchWatermark({ enabled: $event })"
            />
            <span>{{ modelValue.watermark?.enabled ? '已启用' : '未启用' }}</span>
          </div>
        </div>
        <n-grid
          v-if="modelValue.watermark?.enabled"
          :cols="2"
          :x-gap="20"
          :y-gap="8"
          responsive="screen"
        >
          <n-form-item-gi label="水印范围">
            <n-radio-group
              :value="modelValue.watermark?.scope"
              @update:value="patchWatermark({ scope: $event })"
            >
              <n-radio-button value="content">
                内容区
              </n-radio-button>
              <n-radio-button value="full">
                全屏
              </n-radio-button>
            </n-radio-group>
          </n-form-item-gi>
          <n-form-item-gi label="动态信息">
            <n-space>
              <n-checkbox
                :checked="modelValue.watermark?.showUsername"
                @update:checked="patchWatermark({ showUsername: $event })"
              >
                用户名
              </n-checkbox>
              <n-checkbox
                :checked="modelValue.watermark?.showTime"
                @update:checked="patchWatermark({ showTime: $event })"
              >
                当前时间
              </n-checkbox>
            </n-space>
          </n-form-item-gi>
          <n-form-item-gi :span="2" label="自定义文字">
            <n-input
              :value="modelValue.watermark?.text"
              maxlength="50"
              placeholder="例如：内部资料"
              @update:value="patchWatermark({ text: $event })"
            />
          </n-form-item-gi>
        </n-grid>
        <p v-else class="settings-block__hint">
          启用水印后，可配置显示范围、自定义文字和动态信息。
        </p>
      </div>
    </n-form>
  </section>
</template>

<script setup>
import IconSelector from '@/components/IconSelector.vue'

const props = defineProps({ modelValue: { type: Object, required: true } })
const emit = defineEmits(['update:modelValue'])
const presetColors = ['#3370ff', '#165dff', '#0f766e', '#7c3aed', '#c2410c', '#be123c']

function patch(value) {
  emit('update:modelValue', { ...props.modelValue, ...value })
}

function patchWatermark(value) {
  patch({ watermark: { ...(props.modelValue.watermark || {}), ...value } })
}
</script>

<style scoped>
.settings-basic-form {
  width: 100%;
}

.settings-basic-form :deep(.n-form-item) {
  margin-bottom: 18px;
}

.settings-basic-form :deep(.n-form-item-blank) {
  min-height: 34px;
}

.settings-block {
  padding: 18px 18px 10px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-secondary, #f8fafc) 70%, #fff);
}

.settings-block + .settings-block {
  margin-top: 20px;
}

.settings-block__title {
  margin: 0 0 16px;
  color: var(--text-primary, #0f172a);
  font-size: 13px;
  font-weight: 650;
}

.settings-block__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.settings-block__head .settings-block__title {
  margin-bottom: 0;
}

.settings-block__hint {
  margin: 0 0 8px;
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
  line-height: 1.6;
}

.settings-icon-field {
  max-width: 280px;
}

.settings-switch-row,
.theme-color-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 34px;
}

.settings-switch-row span {
  color: var(--text-secondary, #646a73);
  font-size: 13px;
}

.theme-color-swatch {
  width: 28px;
  height: 28px;
  border: 3px solid #fff;
  border-radius: 7px;
  box-shadow: 0 0 0 1px #d9dce1;
  cursor: pointer;
}

.theme-color-swatch.active {
  box-shadow: 0 0 0 2px var(--primary-color, #3370ff);
}

.theme-color-picker {
  width: 116px;
}
</style>
