<template>
  <view class="notice-page">
    <AiFeedbackHost />

    <!-- 全部 / 未读：未读页签只在已加载数据里按阅读状态过滤 -->
    <view class="notice-tabs">
      <AiTabs v-model="activeIndex" :tabs="tabs" />
    </view>

    <!-- 公告列表 -->
    <scroll-view class="notice-scroll" scroll-y :show-scrollbar="false" @scrolltolower="loadMore">
      <AiListSkeleton v-if="loading && !records.length" :rows="5" />
      <AiEmpty
        v-else-if="!visibleRecords.length"
        :title="failed ? '公告加载失败' : activeTab === 'unread' ? '暂无未读公告' : '暂无公告'"
        :description="failed ? '下拉刷新重试。' : '有新公告时会在这里显示。'"
        icon="volume-2"
      />
      <view v-else class="notice-list">
        <view
          v-for="item in visibleRecords"
          :key="item.noticeId"
          class="notice-row"
          :class="{ unread: isNoticeUnread(item, noticeStore.readIds) }"
          @click="openNotice(item)"
        >
          <view class="notice-row__title-line">
            <text v-if="isNoticeTop(item)" class="notice-row__top">置顶</text>
            <text class="notice-row__title">{{ item.noticeTitle || '公告' }}</text>
            <view v-if="isNoticeUnread(item, noticeStore.readIds)" class="notice-row__dot" />
          </view>
          <view class="notice-row__meta">
            <text v-if="item.noticeTypeName">{{ item.noticeTypeName }}</text>
            <text v-if="item.publisherName">{{ item.publisherName }}</text>
            <text>{{ formatFlowDateTime(item.publishTime || item.createTime) }}</text>
          </view>
        </view>
        <text class="notice-footer">{{ footerText }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import AiEmpty from '@/components/AiEmpty.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiTabs from '@/components/AiTabs.vue'
import api from '@/api'
import { useNoticeStore } from '@/store'
import { formatFlowDateTime } from '@/utils/flow-display'
import {
  NOTICE_PAGE_SIZE,
  buildNoticeDetailUrl,
  filterNotices,
  isNoticeTop,
  isNoticeUnread,
  normalizeNoticePage,
} from '@/utils/notice'

const noticeStore = useNoticeStore()
const tabs = [{ key: 'all', label: '全部' }, { key: 'unread', label: '未读' }]
const activeIndex = ref(0)
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const failed = ref(false)

const activeTab = computed(() => tabs[activeIndex.value]?.key || 'all')
const visibleRecords = computed(() => filterNotices(records.value, activeTab.value, noticeStore.readIds))
const finished = computed(() => records.value.length >= total.value)
const footerText = computed(() => {
  if (loading.value) return '加载中…'
  return finished.value ? `共 ${total.value} 条` : '上拉加载更多'
})

onLoad(() => reload())

onPullDownRefresh(async () => {
  try { await reload() }
  finally { uni.stopPullDownRefresh() }
})

async function reload() {
  pageNum.value = 1
  records.value = []
  total.value = 0
  await fetchPage()
}

async function loadMore() {
  if (loading.value || finished.value || failed.value) return
  pageNum.value += 1
  await fetchPage()
}

async function fetchPage() {
  loading.value = true
  failed.value = false
  try {
    const response = await api.getNoticePage({ pageNum: pageNum.value, pageSize: NOTICE_PAGE_SIZE })
    const page = normalizeNoticePage(response?.data)
    records.value = pageNum.value === 1 ? page.records : records.value.concat(page.records)
    total.value = page.total
  }
  catch (error) {
    console.error('加载公告失败:', error)
    failed.value = true
  }
  finally {
    loading.value = false
  }
}

function openNotice(item) {
  uni.navigateTo({ url: buildNoticeDetailUrl(item.noticeId) })
}
</script>

<style lang="scss" scoped>
@import '../styles/notice.scss';
</style>
