<template>
  <view class="mobile-calendar">
    <view class="mobile-calendar__head">
      <button class="mobile-calendar__nav" @click="changeMonth(-1)">‹</button>
      <text>{{ monthLabel }}</text>
      <button class="mobile-calendar__nav" @click="changeMonth(1)">›</button>
    </view>
    <view class="mobile-calendar__week"><text v-for="day in weekDays" :key="day">{{ day }}</text></view>
    <view class="mobile-calendar__grid">
      <button
        v-for="day in days" :key="day.key" class="mobile-calendar__day"
        :class="{ 'is-outside': !day.currentMonth, 'is-today': day.today, 'is-selected': day.key === selectedKey, 'has-event': day.hasEvent }"
        @click="selectedKey = day.key"
      >{{ day.label }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  value: { type: [String, Number, Date], default: '' },
  items: { type: Array, default: () => [] },
})
const weekDays = ['一', '二', '三', '四', '五', '六', '日']
const selectedKey = ref(dateKey(resolveDate(props.value)))
const viewDate = ref(startOfMonth(resolveDate(props.value)))
const monthLabel = computed(() => `${viewDate.value.getFullYear()} 年 ${viewDate.value.getMonth() + 1} 月`)
const eventKeys = computed(() => new Set(props.items.map(item => dateKey(resolveDate(item?.date || item?.value || item)))))
const days = computed(() => buildMonthDays(viewDate.value, eventKeys.value))

function changeMonth(delta) {
  viewDate.value = new Date(viewDate.value.getFullYear(), viewDate.value.getMonth() + delta, 1)
}
function resolveDate(value) {
  const candidate = value instanceof Date ? value : new Date(value || Date.now())
  return Number.isNaN(candidate.getTime()) ? new Date() : candidate
}
function startOfMonth(value) { return new Date(value.getFullYear(), value.getMonth(), 1) }
function dateKey(value) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
}
function buildMonthDays(month, marked) {
  const first = startOfMonth(month)
  const mondayOffset = (first.getDay() + 6) % 7
  const cursor = new Date(first.getFullYear(), first.getMonth(), 1 - mondayOffset)
  const today = dateKey(new Date())
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(cursor.getFullYear(), cursor.getMonth(), cursor.getDate() + index)
    const key = dateKey(date)
    return { key, label: date.getDate(), currentMonth: date.getMonth() === month.getMonth(), today: key === today, hasEvent: marked.has(key) }
  })
}

watch(() => props.value, (value) => {
  const date = resolveDate(value)
  selectedKey.value = dateKey(date)
  viewDate.value = startOfMonth(date)
})
</script>

<style lang="scss" scoped>
.mobile-calendar { padding: 32rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; }
.mobile-calendar__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; color: #1d2129; font-size: 27rpx; font-weight: 500; }
.mobile-calendar__nav { width: 88rpx; height: 88rpx; margin: 0; padding: 0; border: 0; border-radius: var(--radius-sm); color: #4e5969; font-size: 36rpx; line-height: 88rpx; background: #f7f8fa; }
.mobile-calendar__nav::after, .mobile-calendar__day::after { border: 0; }
.mobile-calendar__week, .mobile-calendar__grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); }
.mobile-calendar__week text { padding: 8rpx 0 12rpx; color: #86909c; font-size: 21rpx; text-align: center; }
.mobile-calendar__day { position: relative; width: 100%; min-height: 88rpx; margin: 0; padding: 0; border: 0; border-radius: var(--radius-sm); color: #1d2129; font-size: 23rpx; line-height: 88rpx; background: transparent; }
.mobile-calendar__day.is-outside { color: #c9cdd4; }
.mobile-calendar__day.is-today { color: #4266f7; font-weight: 500; }
.mobile-calendar__day.is-selected { color: #fff; background: #4266f7; }
.mobile-calendar__day.has-event::before { position: absolute; bottom: 5rpx; left: 50%; width: 6rpx; height: 6rpx; border-radius: 50%; background: #ff7d00; content: ''; transform: translateX(-50%); }
</style>
