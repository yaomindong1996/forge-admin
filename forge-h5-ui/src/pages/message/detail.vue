<template>
  <view class="message-detail-page">
    <AiFeedbackHost />
    <view v-if="loading" class="message-detail-card"><AiListSkeleton :rows="3" /></view>
    <view v-else-if="message" class="message-detail-card">
      <!-- 标题区：标题 + 分类 · 时间 -->
      <text class="message-detail-title">{{ message.title || '消息通知' }}</text>
      <view class="message-detail-head">
        <text class="message-detail-type">{{ category }}</text>
        <text class="message-detail-time">{{ formatFlowDateTime(message.createTime || message.receiveTime) }}</text>
      </view>
      <!-- 正文 -->
      <rich-text v-if="message.content" class="message-detail-body" :nodes="safeContent" />
      <text v-else class="message-detail-body">{{ message.description || '暂无正文内容' }}</text>
    </view>
    <AiEmpty v-else title="消息不存在" description="消息可能已经被删除" icon="inbox" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AiEmpty from '@/components/AiEmpty.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import api from '@/api'
import { formatFlowDateTime } from '@/utils/flow-display'
import { sanitizeMessageHtml } from '@/utils/message-html'
import { toast } from '@/utils/notify'

const message = ref(null)
const loading = ref(true)
const category = computed(() => ({ SYSTEM: '系统消息', SMS: '短信通知', EMAIL: '邮件通知', CUSTOM: '通知' })[message.value?.type] || '消息通知')
const safeContent = computed(() => sanitizeMessageHtml(message.value?.content))

onLoad(async ({ id } = {}) => {
  if (!id) { loading.value = false; return }
  try {
    const response = await api.getMessageDetail(String(id))
    message.value = response?.data || null
    if (Number(message.value?.readFlag ?? message.value?.readStatus) === 0) {
      await api.markMessageRead(String(id))
      message.value.readFlag = 1
    }
  }
  catch (error) {
    console.error('加载消息失败:', error)
    toast('消息加载失败', { type: 'error' })
  }
  finally { loading.value = false }
})
</script>

<style lang="scss" scoped>
.message-detail-page { min-height: 100%; padding: 12px; background: var(--forge-page-bg); box-sizing: border-box; }
.message-detail-card { width: 100%; max-width: 960px; margin: 0 auto; padding: 20px 16px; border-radius: var(--forge-radius-card); background: var(--forge-surface); box-sizing: border-box; }
.message-detail-title { display: block; color: var(--forge-text-primary); font-size: 20px; font-weight: 600; line-height: 1.4; overflow-wrap: anywhere; }
.message-detail-head { display: flex; align-items: center; gap: 8px; margin-top: 8px; padding-bottom: 16px; border-bottom: 1px solid var(--forge-border); color: var(--forge-text-tertiary); font-size: 13px; }
.message-detail-type { padding: 2px 6px; border-radius: 4px; color: var(--forge-color-primary); font-size: 12px; background: var(--forge-color-primary-soft); }
.message-detail-body { display: block; margin-top: 16px; color: var(--forge-text-primary); font-size: 16px; line-height: 1.75; overflow-wrap: anywhere; }
</style>
