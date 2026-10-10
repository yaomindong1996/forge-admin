<template>
  <!-- 工作台最新公告条：接口已按置顶优先排序，取第一条；没有公告时不显示 -->
  <view v-if="noticeStore.latest" class="notice-banner" @click.stop="openLatest">
    <text class="notice-banner__label">公告</text>
    <text class="notice-banner__title">{{ noticeStore.latest.noticeTitle || '查看公告' }}</text>
    <AiIcon name="chevron-right" color="#c1c3c6" size="sm" />
  </view>
</template>

<script setup>
import AiIcon from '@/components/AiIcon.vue'
import { useNoticeStore } from '@/store'
import { buildNoticeDetailUrl } from '@/utils/notice'

const noticeStore = useNoticeStore()

function openLatest() {
  uni.navigateTo({ url: buildNoticeDetailUrl(noticeStore.latest.noticeId) })
}
</script>

<style lang="scss" scoped>
/* 嵌在概览卡片底部，用分隔线和上方数字区隔开 */
.notice-banner {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--forge-border);
  box-sizing: border-box;
}

.notice-banner:active {
  opacity: 0.7;
}

.notice-banner__label {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 4px;
  color: var(--forge-tone-orange);
  font-size: 12px;
  font-weight: 500;
  background: var(--forge-tone-orange-bg);
}

.notice-banner__title {
  overflow: hidden;
  min-width: 0;
  flex: 1;
  color: var(--forge-text-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
