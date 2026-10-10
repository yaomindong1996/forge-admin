<template>
  <view class="lowcode-image-upload">
    <view v-for="(fileId, index) in values" :key="`${fileId}:${index}`" class="lowcode-image-upload__item">
      <AiImageUpload
        :model-value="fileId"
        :business-type="businessType"
        :crop="crop"
        :max-size="maxSize"
        :readonly="readonly"
        @update:model-value="value => replace(index, value)"
      />
      <button v-if="!readonly && showRemoveButton" class="lowcode-image-upload__remove" @click="remove(index)">移除</button>
    </view>
    <AiImageUpload
      v-if="!readonly && values.length < effectiveMaxCount"
      model-value=""
      :business-type="businessType"
      :crop="crop && effectiveMaxCount === 1"
      :max-size="maxSize"
      @update:model-value="append"
    >
      <template #default="{ uploading }">
        <view class="lowcode-image-upload__add">
          <wd-icon name="add" size="24px" color="var(--forge-color-primary, #0066ff)" />
          <text>{{ uploading ? '上传中' : '添加图片' }}</text>
        </view>
      </template>
    </AiImageUpload>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import AiImageUpload from '@/components/AiImageUpload.vue'

const props = defineProps({
  modelValue: { type: [String, Array], default: '' },
  businessType: { type: String, default: 'lowcode_image' },
  crop: { type: Boolean, default: false },
  maxCount: { type: Number, default: 1 },
  maxSize: { type: Number, default: 0 },
  multiple: { type: Boolean, default: false },
  showRemoveButton: { type: Boolean, default: true },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])
const effectiveMaxCount = computed(() => Math.max(1, props.multiple ? props.maxCount : 1))
const values = computed(() => {
  const source = Array.isArray(props.modelValue) ? props.modelValue : String(props.modelValue || '').split(',')
  return source.map(item => typeof item === 'object' ? item.id || item.fileId || item.value : item).map(String).map(item => item.trim()).filter(Boolean)
})

function append(value) {
  if (!value) return
  emitValue([...values.value, String(value)].slice(0, effectiveMaxCount.value))
}

function replace(index, value) {
  const next = [...values.value]
  next[index] = String(value || '')
  emitValue(next.filter(Boolean))
}

function remove(index) {
  emitValue(values.value.filter((_item, itemIndex) => itemIndex !== index))
}

function emitValue(items) {
  if (Array.isArray(props.modelValue)) emit('update:modelValue', items)
  else emit('update:modelValue', items.join(','))
}
</script>

<style lang="scss" scoped>
.lowcode-image-upload { display: flex; flex-wrap: wrap; gap: 16rpx; }
.lowcode-image-upload__item { position: relative; }
.lowcode-image-upload__remove { width: 100%; min-height: 52rpx; margin: 6rpx 0 0; padding: 0; border: 0; color: var(--forge-color-danger, #ff5219); font-size: 22rpx; line-height: 52rpx; background: transparent; }
.lowcode-image-upload__remove::after { border: 0; }
.lowcode-image-upload__add { display: flex; width: 156rpx; height: 156rpx; flex-direction: column; align-items: center; justify-content: center; gap: 8rpx; border: 1rpx dashed var(--forge-color-primary, #0066ff); border-radius: var(--radius-card); color: var(--forge-color-primary, #0066ff); font-size: 22rpx; background: var(--forge-color-primary-soft, #e8f1ff); box-sizing: border-box; }
</style>
