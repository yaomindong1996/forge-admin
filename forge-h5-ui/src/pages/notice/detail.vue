<template>
  <view class="notice-detail-page">
    <AiFeedbackHost />
    <view v-if="loading" class="notice-detail-card"><AiListSkeleton :rows="3" /></view>

    <view v-else-if="notice" class="notice-detail-card">
      <!-- 标题区：标题 + 类型 · 发布人 · 时间 -->
      <text class="notice-detail-title">{{ notice.noticeTitle || '公告' }}</text>
      <view class="notice-detail-head">
        <text v-if="notice.noticeTypeName" class="notice-detail-type">{{ notice.noticeTypeName }}</text>
        <text v-if="notice.publisherName">{{ notice.publisherName }}</text>
        <text>{{ formatFlowDateTime(notice.publishTime || notice.createTime) }}</text>
      </view>

      <!-- 正文：富文本必须先经过净化 -->
      <rich-text v-if="safeContent" class="notice-detail-body" :nodes="safeContent" />
      <text v-else class="notice-detail-body">暂无正文内容</text>

      <!-- 附件 -->
      <view v-if="attachments.length" class="notice-attachments">
        <text class="notice-attachments__title">附件（{{ attachments.length }}）</text>
        <view
          v-for="file in attachments"
          :key="file.fileId || file.fileUrl"
          class="notice-attachment"
          @click="openNoticeAttachment(file)"
        >
          <AiIcon name="paperclip" color="#747677" size="sm" />
          <text class="notice-attachment__name">{{ file.fileName || '附件' }}</text>
          <text class="notice-attachment__size">{{ formatFileSize(file.fileSize) }}</text>
        </view>
      </view>
    </view>

    <AiEmpty v-else title="公告不存在" description="公告可能已撤回或不在你的可见范围内" icon="volume-2" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AiEmpty from '@/components/AiEmpty.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import api from '@/api'
import { useNoticeStore } from '@/store'
import { formatFlowDateTime } from '@/utils/flow-display'
import { sanitizeMessageHtml } from '@/utils/message-html'
import { formatFileSize, isNoticeUnread } from '@/utils/notice'
import { openNoticeAttachment } from '@/utils/notice-attachment'

const noticeStore = useNoticeStore()
const notice = ref(null)
const loading = ref(true)

const safeContent = computed(() => sanitizeMessageHtml(notice.value?.noticeContent))
const attachments = computed(() => (Array.isArray(notice.value?.attachments) ? notice.value.attachments : []))

onLoad(async ({ noticeId } = {}) => {
  if (!noticeId) { loading.value = false; return }
  try {
    const response = await api.getNoticeDetail(String(noticeId))
    notice.value = response?.data || null
  }
  catch (error) {
    console.error('加载公告失败:', error)
  }
  finally {
    loading.value = false
  }
  // 详情接口只累加阅读次数，不写已读记录；已读的公告不再调用，避免角标被重复扣减。
  if (notice.value && isNoticeUnread(notice.value, noticeStore.readIds)) {
    noticeStore.markRead(notice.value.noticeId).catch(error => console.error('标记公告已读失败:', error))
  }
})
</script>

<style lang="scss" scoped>
@import '../styles/notice.scss';
</style>
