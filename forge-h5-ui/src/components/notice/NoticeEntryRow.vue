<template>
  <!-- 消息页顶部公告入口：没有任何公告时整行隐藏 -->
  <view v-if="noticeStore.latest" class="notice-entry" @click="openList">
    <view class="notice-entry__icon">
      <AiIcon name="volume-2" color="currentColor" size="md" />
    </view>
    <view class="notice-entry__main">
      <text class="notice-entry__title">公告</text>
      <text class="notice-entry__desc">{{ noticeStore.latest.noticeTitle || '查看全部公告' }}</text>
    </view>
    <text v-if="unreadText" class="notice-entry__badge">{{ unreadText }}</text>
    <AiIcon name="chevron-right" color="#c1c3c6" size="sm" />
  </view>
</template>

<script setup>
import { computed } from 'vue'
import AiIcon from '@/components/AiIcon.vue'
import { useBadgeStore, useNoticeStore } from '@/store'
import { formatBadgeCount } from '@/store/modules/badge'

const noticeStore = useNoticeStore()
const badgeStore = useBadgeStore()
const unreadText = computed(() => formatBadgeCount(badgeStore.noticeUnreadCount))

function openList() {
  uni.navigateTo({ url: '/pages/notice/index' })
}
</script>

<style lang="scss" scoped>
.notice-entry {
  display: flex;
  min-height: 72px;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 12px 16px;
  border-radius: var(--forge-radius-card);
  background: var(--forge-surface);
  box-sizing: border-box;
}

.notice-entry:active {
  background: var(--forge-surface-subtle);
}

.notice-entry__icon {
  display: flex;
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  align-items: center;
  justify-content: center;
  border-radius: var(--forge-radius-icon);
  color: var(--forge-tone-orange);
  background: var(--forge-tone-orange-bg);
}

.notice-entry__main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.notice-entry__title {
  color: var(--forge-text-primary);
  font-size: 16px;
  font-weight: 500;
}

.notice-entry__desc {
  overflow: hidden;
  color: var(--forge-text-tertiary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-entry__badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  background: var(--forge-color-danger);
  box-sizing: border-box;
}
</style>
