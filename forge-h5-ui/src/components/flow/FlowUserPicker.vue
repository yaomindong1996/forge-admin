<template>
  <view class="flow-user-picker">
    <view class="delegate-search"><AiSearchBar v-model="keyword" placeholder="搜索姓名或用户名" @search="reload" @clear="reload" /></view>
    <view v-if="modelValue" class="delegate-choice">
      <view class="delegate-choice__avatar"><AiAuthImage v-if="modelValue.avatar" :src="modelValue.avatar" mode="aspectFill" /><text v-else>{{ userInitial(modelValue) }}</text></view>
      <view class="delegate-choice__copy"><text>已选择</text><text>{{ displayUserName(modelValue) }}</text></view>
      <AiIcon icon="/static/icons/ai-icon/check-circle.svg" color="#0066ff" size="md" />
    </view>
    <view class="user-list">
      <AiListSkeleton v-if="loading" :rows="3" compact />
      <button v-for="user in users" v-else :key="user.id" class="user-row" :class="{ active: isSelected(user) }" @click.stop="emit('update:modelValue', user)">
        <view class="user-avatar"><AiAuthImage v-if="user.avatar" :src="user.avatar" mode="aspectFill" /><text v-else>{{ userInitial(user) }}</text></view>
        <view class="user-copy">
          <text class="user-name">{{ displayUserName(user) }}</text>
          <text class="user-meta">{{ user.username }}{{ user.deptName ? ` · ${user.deptName}` : '' }}</text>
        </view>
        <view class="user-check" :class="{ active: isSelected(user) }"><AiIcon v-if="isSelected(user)" icon="/static/icons/ai-icon/check.svg" color="#ffffff" size="xs" /></view>
      </button>
      <button v-if="!loading && !exhausted" class="load-more-users" @click="fetchUsers">加载更多成员</button>
      <view v-if="!loading && !users.length" class="page-hint">{{ emptyText }}</view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import AiAuthImage from '@/components/AiAuthImage.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import api from '@/api'
import { resolveApiErrorMessage } from '@/utils/flow-page'
import { toast } from '@/utils/notify'

const PAGE_SIZE = 30

const props = defineProps({
  modelValue: { type: Object, default: null },
  /** 弹层打开时置为 true，每次打开重新加载第一页 */
  active: { type: Boolean, default: false },
  excludeIds: { type: Array, default: () => [] },
  emptyText: { type: String, default: '未找到可选人员' },
})
const emit = defineEmits(['update:modelValue'])

const keyword = ref('')
const users = ref([])
const loading = ref(false)
const pageNum = ref(1)
const exhausted = ref(false)

watch(() => props.active, (active) => {
  if (!active) return
  keyword.value = ''
  reload()
}, { immediate: true })

function displayUserName(user = {}) {
  return user.realName || user.name || user.nickname || user.username || '未命名成员'
}

function userInitial(user = {}) {
  return String(displayUserName(user)).slice(0, 1).toUpperCase()
}

function isSelected(user) {
  return String(props.modelValue?.id || '') === String(user?.id || '')
}

async function reload() {
  pageNum.value = 1
  users.value = []
  exhausted.value = false
  await fetchUsers()
}

async function fetchUsers() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await api.getUserPage({ pageNum: pageNum.value, pageSize: PAGE_SIZE, keyword: keyword.value.trim() || undefined })
    const page = res?.data || {}
    const records = Array.isArray(page.records) ? page.records : []
    const excluded = new Set(props.excludeIds.map(id => String(id || '')).filter(Boolean))
    const candidates = records.filter(user => !excluded.has(String(user.id || '')))
    users.value = pageNum.value === 1 ? candidates : users.value.concat(candidates)
    exhausted.value = records.length < PAGE_SIZE || pageNum.value * PAGE_SIZE >= Number(page.total || 0)
    pageNum.value += 1
  }
  catch (error) {
    users.value = []
    console.error('加载人员失败:', error)
    toast(resolveApiErrorMessage(error, '人员加载失败'), { type: 'error' })
  }
  finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.user-list { display: flex; max-height: 40vh; min-height: 0; flex-direction: column; gap: 12rpx; overflow-y: auto; }
.delegate-search { margin-bottom: 16rpx; }
.delegate-choice { display: flex; align-items: center; gap: 14rpx; margin-bottom: 16rpx; padding: 14rpx 16rpx; border: 1rpx solid var(--primary-color); border-radius: var(--radius-control); background: var(--primary-soft); }
.delegate-choice__avatar, .user-avatar { display: flex; overflow: hidden; align-items: center; justify-content: center; border-radius: 50%; color: var(--primary-color); font-weight: 500; background: var(--primary-soft); }
.delegate-choice__avatar { width: 52rpx; height: 52rpx; flex: 0 0 52rpx; font-size: 24rpx; }
.delegate-choice__copy { min-width: 0; flex: 1; }
.delegate-choice__copy text { display: block; }
.delegate-choice__copy text:first-child { color: #747677; font-size: 20rpx; }
.delegate-choice__copy text:last-child { overflow: hidden; margin-top: 3rpx; color: var(--text-strong); font-size: 26rpx; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.user-row { display: flex; width: 100%; min-height: 72px; height: auto; align-items: center; gap: 14rpx; margin: 0; padding: 10px 12px; border: 1rpx solid #f0f1f2; border-radius: 12rpx; color: var(--text-strong); font-size: 14px; line-height: 1.4; text-align: left; white-space: normal; background: #fff; box-sizing: border-box; }
.user-row::after { border: 0; }
.user-row.active { border-color: var(--primary-color); background: var(--primary-soft); }
.user-avatar { width: 50rpx; height: 50rpx; flex: 0 0 50rpx; font-size: 23rpx; }
.user-copy { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 3px; }
.user-name, .user-meta { display: block; }
.user-check { display: flex; width: 32rpx; height: 32rpx; flex: 0 0 32rpx; align-items: center; justify-content: center; border: 1rpx solid #c1c3c6; border-radius: 50%; box-sizing: border-box; }
.user-check.active { border-color: #0066ff; background: #0066ff; }
.user-name { overflow-wrap: anywhere; color: var(--text-strong); font-size: 14px; line-height: 1.35; }
.user-meta { overflow-wrap: anywhere; color: var(--text-muted); font-size: 12px; line-height: 1.35; }
.load-more-users { min-height: 88rpx; margin: 4rpx 0 0; border: 1rpx solid var(--border-light); border-radius: var(--radius-control); color: var(--primary-color); font-size: 24rpx; background: var(--primary-soft); }
.load-more-users::after { border: 0; }
.page-hint { padding: 80rpx 32rpx; color: var(--text-muted); font-size: 26rpx; text-align: center; }

@media (hover: hover) {
  .user-row:hover { background: var(--surface-muted); }
}
</style>
