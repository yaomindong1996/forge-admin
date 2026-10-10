<template>
  <AiLayoutPage :title="title" subtitle="已从全部应用打开">
    <AiFeedbackHost />
    <view class="app-entry">
      <AiAppIcon icon="/static/icons/ai-icon/layout.svg" :color="MENU_TONES.blue.color" :bg="MENU_TONES.blue.bg" />
      <text class="app-entry__title">{{ title }}</text>
      <text class="app-entry__desc">该功能已由后台菜单授权。移动端页面完成配置后，将自动从这里进入。</text>
      <AiButton block @click="goHome">返回工作台</AiButton>
    </view>
  </AiLayoutPage>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AiAppIcon from '@/components/AiAppIcon.vue'
import AiButton from '@/components/AiButton.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiLayoutPage from '@/components/AiLayoutPage.vue'
import { MENU_TONES } from '@/utils/mobile-menu'

const title = ref('应用功能')

onLoad((query = {}) => {
  title.value = String(query.title || '应用功能')
  uni.setNavigationBarTitle({ title: title.value })
  const configKey = String(query.configKey || query.runtimeConfigKey || query.pageConfigKey || '').trim()
  const path = String(query.path || '').trim()
  if (configKey || /(?:crud-page|crud)\//.test(path)) {
    const params = Object.entries({
      configKey: configKey || resolveConfigKey(path),
      title: title.value,
      ...(query.mode ? { mode: query.mode } : {}),
      ...(query.recordId ? { recordId: query.recordId } : {}),
      ...(query.applicationId ? { applicationId: query.applicationId } : {}),
      ...(query.appId ? { appId: query.appId } : {}),
      ...(query.pageId ? { pageId: query.pageId } : {}),
      ...(query.pageCode ? { pageCode: query.pageCode } : {}),
    }).map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&')
    uni.redirectTo({ url: `/pages/lowcode-runtime?${params}` })
  }
})

function resolveConfigKey(path) {
  return decodeURIComponent(String(path || '').match(/(?:crud-page|crud|lowcode)\/([^/?]+)/)?.[1] || '')
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.app-entry { display: flex; width: 100%; max-width: 640px; min-height: 260px; flex-direction: column; align-items: center; justify-content: center; margin: 0 auto; padding: 32px 20px 24px; border-radius: var(--forge-radius-card); text-align: center; background: var(--forge-surface); box-sizing: border-box; }
.app-entry__title { display: block; margin-top: 16px; color: var(--forge-text-primary); font-size: 18px; font-weight: 600; }
.app-entry__desc { display: block; max-width: 280px; margin: 8px 0 24px; color: var(--forge-text-secondary); font-size: 14px; line-height: 1.6; }
</style>
