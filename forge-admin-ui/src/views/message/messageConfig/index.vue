<template>
  <div class="message-config-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <div class="title-icon message-icon">
            <i class="i-material-symbols:notifications-active-rounded" />
          </div>
          <h2 class="page-title">
            消息配置
          </h2>
        </div>
        <div class="header-desc">
          短信与邮件消息服务配置，支持多厂商接入与发送策略管理
        </div>
      </div>
    </div>

    <!-- 配置面板 -->
    <div class="config-panel">
      <n-tabs v-model:value="activeTab" type="line" animated>
        <n-tab-pane name="sms">
          <template #tab>
            <div class="tab-header">
              <i class="i-material-symbols:sms tab-icon sms-icon" />
              <span>短信配置</span>
            </div>
          </template>
          <SmsConfig />
        </n-tab-pane>
        <n-tab-pane name="email">
          <template #tab>
            <div class="tab-header">
              <i class="i-material-symbols:mail-rounded tab-icon email-icon" />
              <span>邮件配置</span>
            </div>
          </template>
          <EmailConfig />
        </n-tab-pane>
      </n-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import EmailConfig from './components/EmailConfig.vue'
import SmsConfig from './components/SmsConfig.vue'

const activeTab = ref('sms')
</script>

<style scoped>
.message-config-page {
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 页面头部 */
.page-header {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}

.title-icon.message-icon {
  background: linear-gradient(135deg, #ec4899 0%, #be185d 100%);
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.header-desc {
  font-size: 12px;
  color: #64748b;
}

/* 配置面板 */
.config-panel {
  flex: 1;
  min-height: 0;
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
  overflow-y: auto;
}

/* Tab 样式 */
.tab-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-icon {
  font-size: 16px;
}

.tab-icon.sms-icon {
  color: #10b981;
}

.tab-icon.email-icon {
  color: #3b82f6;
}

.config-panel :deep(.n-tabs-nav) {
  padding: 0;
}

.config-panel :deep(.n-tabs-tab) {
  font-weight: 500;
  padding: 12px 20px;
}

.config-panel :deep(.n-tabs-tab.n-tabs-tab--active) {
  color: #ec4899;
}

.config-panel :deep(.n-tabs-pane-wrapper) {
  padding-top: 16px;
}

/* 深色模式 */
.dark .page-header {
  background: #0f172a !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.dark .page-title {
  color: #f1f5f9;
}

.dark .header-desc {
  color: #94a3b8;
}

.dark .config-panel {
  background: #0f172a !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.dark .config-panel :deep(.n-tabs-tab.n-tabs-tab--active) {
  color: #f472b6;
}
</style>
