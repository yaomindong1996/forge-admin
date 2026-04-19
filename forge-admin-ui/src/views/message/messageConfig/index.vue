<template>
  <div class="message-config-page">
    <n-card title="消息配置">
      <n-tabs v-model:value="activeTab" type="line" animated>
        <n-tab-pane name="sms" tab="短信配置">
          <SmsConfig />
        </n-tab-pane>
        <n-tab-pane name="email" tab="邮件配置">
          <EmailConfig />
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup>
import EmailConfig from './components/EmailConfig.vue'
import SmsConfig from './components/SmsConfig.vue'

const activeTab = ref('sms')
</script>

<style scoped>
.message-config-page {
  padding: 16px;
}
</style>
