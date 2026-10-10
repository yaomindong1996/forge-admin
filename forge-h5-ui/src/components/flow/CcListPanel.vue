<template>
  <view class="cc-panel">
    <!-- 已读筛选 + 全部已读 -->
    <view class="cc-toolbar">
      <view class="cc-filter">
        <button
          v-for="item in CC_READ_FILTERS"
          :key="item.value"
          class="cc-filter__item"
          :class="{ active: readFilter === item.value }"
          @click="setReadFilter(item.value)"
        >
          {{ item.label }}
        </button>
      </view>
      <button v-if="unreadCount" class="cc-read-all" :disabled="markingAll" @click="markAllRead">全部已读</button>
    </view>

    <!-- 抄送列表：整卡进入抄送详情 -->
    <scroll-view
      class="cc-list"
      scroll-y
      :show-scrollbar="false"
      refresher-enabled
      :refresher-triggered="pullRefreshing"
      @refresherrefresh="refreshByPull"
      @scrolltolower="loadMore"
    >
      <AiListSkeleton v-if="loading && !records.length" :rows="6" />
      <template v-else-if="records.length">
        <view v-for="cc in records" :key="cc.id" class="cc-card" @click="openCc(cc)">
          <view class="cc-card__head">
            <text v-if="isCcUnread(cc, readIds)" class="cc-card__dot" />
            <text class="cc-card__title">{{ ccTitle(cc) }}</text>
            <text class="cc-card__time">{{ formatFlowDateTime(cc.ccTime) }}</text>
          </view>
          <text v-if="ccSummary(cc)" class="cc-card__summary">{{ ccSummary(cc) }}</text>
          <view class="cc-card__meta">
            <text class="cc-card__sender">{{ ccSender(cc) }} 抄送给你</text>
            <text v-if="cc.processName" class="cc-card__process">{{ cc.processName }}</text>
          </view>
        </view>
        <AiListSkeleton v-if="loading" :rows="2" compact />
        <view v-else class="cc-list-foot">{{ hasMore ? '上拉加载更多' : '没有更多抄送' }}</view>
      </template>
      <view v-else class="cc-state">
        <AiIcon icon="/static/icons/ai-icon/inbox.svg" color="#0066ff" size="lg" />
        <text class="cc-state__title">{{ loadFailed ? '抄送加载失败' : readFilter ? '没有未读抄送' : '暂无抄送' }}</text>
        <text class="cc-state__copy">{{ loadFailed ? '下拉刷新重试' : '流程抄送给你后会显示在这里' }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import AiIcon from '@/components/AiIcon.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import api from '@/api'
import { useCcStore } from '@/store'
import { showConfirmDialog } from '@/utils/dialog'
import { formatFlowDateTime } from '@/utils/flow-display'
import {
  buildCcDetailUrl, CC_PAGE_SIZE, CC_READ_FILTERS, ccSender, ccSummary, ccTitle, isCcUnread, normalizeCcPage,
} from '@/utils/flow-cc'
import { resolveApiErrorMessage } from '@/utils/flow-page'
import { toast } from '@/utils/notify'

const props = defineProps({
  /** 顶部搜索框关键字，对应接口 title 参数 */
  keyword: { type: String, default: '' },
})

const ccStore = useCcStore()
const { unreadCount, readIds } = storeToRefs(ccStore)
const records = ref([])
const pageNum = ref(1)
const total = ref(0)
const readFilter = ref('')
const loading = ref(false)
const loadFailed = ref(false)
const pullRefreshing = ref(false)
const markingAll = ref(false)
const hasMore = computed(() => records.value.length < total.value)

// —— 列表查询 ——
async function load({ reset = false } = {}) {
  if (loading.value || (!reset && !hasMore.value)) return
  if (reset) {
    pageNum.value = 1
    total.value = 0
    records.value = []
  }
  loading.value = true
  loadFailed.value = false
  try {
    const response = await api.getMyCcPage({
      pageNum: pageNum.value,
      pageSize: CC_PAGE_SIZE,
      isRead: readFilter.value === '' ? undefined : Number(readFilter.value),
      title: props.keyword.trim() || undefined,
    })
    const page = normalizeCcPage(response?.data)
    records.value = reset ? page.records : records.value.concat(page.records)
    total.value = page.total
    pageNum.value += 1
  }
  catch (error) {
    loadFailed.value = true
    console.error('加载抄送失败:', error)
  }
  finally {
    loading.value = false
  }
}

function reload() {
  ccStore.loadUnreadCount(api.getCcUnreadCount)
  return load({ reset: true })
}

function loadMore() { load() }

async function refreshByPull() {
  if (pullRefreshing.value) return
  pullRefreshing.value = true
  try { await reload() }
  finally { pullRefreshing.value = false }
}

function setReadFilter(value) {
  if (readFilter.value === value) return
  readFilter.value = value
  load({ reset: true })
}

// —— 已读处理 ——
async function markAllRead() {
  const confirmed = await showConfirmDialog({
    title: '全部标记为已读',
    description: `共 ${unreadCount.value} 条未读抄送，标记后不再显示未读提醒。`,
    confirmText: '全部已读',
  })
  if (!confirmed) return
  markingAll.value = true
  try {
    await ccStore.markAllRead(api.markAllCcRead)
    toast('已全部标记为已读', { type: 'success' })
    await load({ reset: true })
  }
  catch (error) {
    toast(resolveApiErrorMessage(error, '标记已读失败'), { type: 'error' })
  }
  finally {
    markingAll.value = false
  }
}

function openCc(cc) {
  ccStore.setCurrent(cc)
  uni.navigateTo({ url: buildCcDetailUrl(cc) })
}

onMounted(reload)
defineExpose({ reload })
</script>

<style lang="scss" scoped>
.cc-panel { display: flex; min-height: 0; flex: 1; flex-direction: column; }

.cc-toolbar {
  display: flex;
  min-height: 40px;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--forge-space-page) 4px;
}

.cc-filter { display: flex; align-items: center; gap: 8px; }

.cc-filter__item,
.cc-read-all {
  min-height: 28px;
  margin: 0;
  padding: 0 12px;
  border: 0;
  border-radius: 14px;
  font-size: 13px;
  line-height: 28px;
}

.cc-filter__item { color: var(--forge-text-secondary); background: var(--forge-surface); }
.cc-filter__item.active { color: var(--forge-color-primary); background: var(--forge-tone-blue-bg); font-weight: 600; }
.cc-read-all { padding-right: 0; color: var(--forge-color-primary); background: transparent; }
.cc-read-all[disabled] { opacity: .5; }
.cc-filter__item::after,
.cc-read-all::after { border: 0; }

.cc-list {
  min-height: 0;
  height: 0;
  flex: 1;
  padding: 4px var(--forge-space-page) 0;
  box-sizing: border-box;
}

.cc-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
  padding: 14px 16px;
  border-radius: var(--forge-radius-card);
  background: var(--forge-surface);
  box-sizing: border-box;
}

.cc-card:active { background: var(--forge-surface-subtle); }
.cc-card__head { display: flex; min-width: 0; align-items: center; gap: 8px; }
.cc-card__dot { width: 8px; height: 8px; flex: 0 0 8px; border-radius: 50%; background: var(--forge-color-danger); }

.cc-card__title {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: var(--forge-text-primary);
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cc-card__time { flex: 0 0 auto; color: var(--forge-text-tertiary); font-size: 12px; }

.cc-card__summary {
  display: -webkit-box;
  overflow: hidden;
  color: var(--forge-text-secondary);
  font-size: 14px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.cc-card__meta { display: flex; min-width: 0; align-items: center; justify-content: space-between; gap: 8px; font-size: 12px; }
.cc-card__sender { color: var(--forge-text-tertiary); }

.cc-card__process {
  max-width: 50%;
  overflow: hidden;
  padding: 2px 8px;
  border-radius: 6px;
  color: var(--forge-text-secondary);
  background: var(--forge-surface-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cc-list-foot { padding: 12px 0 var(--forge-tabbar-space); color: var(--forge-text-tertiary); font-size: 12px; text-align: center; }

.cc-state {
  display: flex;
  min-height: 400rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--forge-text-tertiary);
  font-size: 13px;
  text-align: center;
}

.cc-state__title { color: var(--forge-text-primary); font-size: 15px; font-weight: 600; }

@media (hover: hover) {
  .cc-card:hover { background: var(--forge-surface-subtle); }
}

@media (min-width: 1024px) {
  .cc-toolbar,
  .cc-list { padding-right: 24px; padding-left: 24px; }
  .cc-card { padding: 16px 20px; }
}
</style>
