<template>
  <view class="contact-member-list">
    <view
      v-for="member in members"
      :key="member.userId"
      class="contact-member-row"
      @click="openMember(member)"
    >
      <ContactAvatar :src="member.avatar || ''" :name="member.realName" />
      <view class="contact-member-row__main">
        <text class="contact-member-row__name">{{ member.realName || '未命名成员' }}</text>
        <text v-if="contactSubtitle(member)" class="contact-member-row__desc">{{ contactSubtitle(member) }}</text>
      </view>
    </view>
    <!-- 加载状态：首屏骨架由页面负责，这里只处理翻页 -->
    <view v-if="members.length" class="contact-member-list__footer">
      <text v-if="loading">加载中…</text>
      <text v-else-if="failed" class="is-link" @click="emit('retry')">加载失败，点击重试</text>
      <text v-else-if="finished">共 {{ total }} 人</text>
    </view>
  </view>
</template>

<script setup>
import ContactAvatar from './ContactAvatar.vue'
import { buildContactMemberUrl, contactSubtitle } from '@/utils/contacts'

defineProps({
  members: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  loading: { type: Boolean, default: false },
  finished: { type: Boolean, default: false },
  failed: { type: Boolean, default: false },
})
const emit = defineEmits(['retry'])

function openMember(member) {
  if (!member?.userId) return
  uni.navigateTo({ url: buildContactMemberUrl(member.userId) })
}
</script>

<style lang="scss" scoped>
.contact-member-list {
  overflow: hidden;
  border-radius: var(--forge-radius-card);
  background: var(--forge-surface);
}

.contact-member-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 64px;
  padding: 0 16px;
}

.contact-member-row:active {
  background: var(--forge-surface-muted);
}

.contact-member-row + .contact-member-row::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 68px;
  height: 1px;
  background: var(--forge-border);
  content: '';
  transform: scaleY(0.5);
}

.contact-member-row__main {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.contact-member-row__name {
  color: var(--forge-text-primary);
  font-size: 16px;
  line-height: 1.4;
}

.contact-member-row__desc {
  overflow: hidden;
  color: var(--forge-text-secondary);
  font-size: 13px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-member-list__footer {
  padding: 12px 0 14px;
  color: var(--forge-text-tertiary);
  font-size: 12px;
  text-align: center;
}

.contact-member-list__footer .is-link {
  color: var(--forge-color-primary);
}
</style>
