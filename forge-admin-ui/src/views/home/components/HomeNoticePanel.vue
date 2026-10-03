<template>
  <section class="dashboard-pane home-notice-panel">
    <header>
      <div>
        <h2>通知公告 <NBadge :value="noticeStore.unreadCount" :max="99" :show="noticeStore.unreadCount > 0" /></h2>
        <p>{{ noticeStore.unreadCount ? `${noticeStore.unreadCount} 条未读公告` : '公告已全部阅读' }}</p>
      </div>
      <NButton text type="primary" size="small" @click="router.push('/system/notice-list')">
        查看全部
      </NButton>
    </header>
    <NSpin :show="noticeStore.loading">
      <div v-if="noticeStore.error" class="notice-empty">
        {{ noticeStore.error }}
        <NButton text type="primary" @click="noticeStore.refresh">
          重试
        </NButton>
      </div>
      <div v-else-if="!noticeStore.notices.length" class="notice-empty">
        <IllustratedEmpty artwork="welcome" description="暂无公告" compact />
      </div>
      <div v-else class="home-notice-list">
        <button
          v-for="notice in noticeStore.notices.slice(0, 5)"
          :key="notice.noticeId"
          type="button"
          class="home-notice-item"
          :class="{ unread: noticeStore.isUnread(notice) }"
          @click="noticeStore.openNotice(notice)"
        >
          <span class="notice-status" />
          <span class="notice-copy">
            <strong>{{ notice.noticeTitle }}</strong>
            <span class="notice-meta">
              <DictTag dict-type="sys_notice_type" :value="notice.noticeType" size="small" />
              <time>{{ String(notice.publishTime || '').slice(0, 10) }}</time>
            </span>
          </span>
        </button>
      </div>
    </NSpin>
  </section>
</template>

<script setup>
import { NBadge, NButton, NSpin } from 'naive-ui'
import { watch } from 'vue'
import { useRouter } from 'vue-router'
import IllustratedEmpty from '@/components/common/IllustratedEmpty.vue'
import DictTag from '@/components/DictTag.vue'
import { useNoticeStore } from '@/stores/system/noticeStore'

const router = useRouter()
const noticeStore = useNoticeStore()
watch(() => noticeStore.contextVersion, () => noticeStore.refresh(), { immediate: true })
</script>

<style scoped>
.home-notice-panel {
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
}

.home-notice-panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 44px;
  margin: 0;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #1f2329);
}

p,
.notice-empty {
  margin: 2px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.notice-empty {
  padding: 28px 14px;
  text-align: center;
}

.home-notice-list {
  padding: 4px 14px 12px;
}

.home-notice-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  padding: 10px 0;
  border: 0;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: var(--text-primary, #1f2329);
}

.home-notice-item:last-child {
  border-bottom: 0;
}

.home-notice-item:hover strong {
  color: var(--primary-color, #0e42d2);
}

.notice-status {
  width: 6px;
  height: 6px;
  flex: 0 0 6px;
  margin-top: 8px;
  border-radius: 50%;
  background: var(--border-light, #e5e7eb);
}

.unread .notice-status {
  background: var(--primary-color, #0e42d2);
}

.notice-copy {
  min-width: 0;
}

.notice-copy strong {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.5;
  overflow-wrap: anywhere;
  transition: color 0.15s ease;
}

.unread strong {
  font-weight: 600;
}

.notice-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}
</style>
