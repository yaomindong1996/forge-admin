<template>
  <view class="lowcode-unsupported" :class="{ 'is-blocked': blocked }">
    <view class="lowcode-unsupported__head">
      <wd-icon :name="blocked ? 'warning' : 'info-circle'" size="16px" />
      <text>{{ blocked ? '此组件不可在移动端编辑' : '此组件已降级展示' }}</text>
    </view>
    <text class="lowcode-unsupported__reason">{{ reason || fallbackReason }}</text>
    <text v-if="valueText" class="lowcode-unsupported__value">当前值：{{ valueText }}</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  componentType: { type: String, default: '' },
  reason: { type: String, default: '' },
  value: { type: [String, Number, Boolean, Array, Object], default: '' },
  blocked: { type: Boolean, default: true },
})

const fallbackReason = computed(() => props.componentType
  ? `组件 ${props.componentType} 暂无移动端交互实现。`
  : '该组件暂无移动端交互实现。')
const valueText = computed(() => {
  if (props.value === undefined || props.value === null || props.value === '') return ''
  if (typeof props.value === 'object') {
    try { return JSON.stringify(props.value) }
    catch { return '[复杂数据]' }
  }
  return String(props.value)
})
</script>

<style lang="scss" scoped>
.lowcode-unsupported { padding: 18rpx 20rpx; border: 1rpx dashed #bfdbfe; border-radius: 12rpx; color: #475569; background: #f8fbff; }
.lowcode-unsupported.is-blocked { border-color: #fecaca; background: #fff7f7; }
.lowcode-unsupported__head { display: flex; align-items: center; gap: 8rpx; color: #334155; font-size: 24rpx; font-weight: 700; }
.lowcode-unsupported__reason, .lowcode-unsupported__value { display: block; margin-top: 8rpx; color: #64748b; font-size: 22rpx; line-height: 1.5; word-break: break-all; }
</style>
