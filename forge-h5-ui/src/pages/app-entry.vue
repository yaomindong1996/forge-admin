<template>
  <AiLayoutPage :title="title" subtitle="已从全部应用打开">
    <view class="app-entry">
      <view class="app-entry__icon">
        <AiIcon icon="/static/icons/ai-icon/layout.svg" color="#2563eb" size="lg" />
      </view>
      <text class="app-entry__title">{{ title }}</text>
      <text class="app-entry__desc">该功能已由后台菜单授权。移动端页面完成配置后，将自动从这里进入。</text>
      <AiButton block @click="goHome">返回首页</AiButton>
    </view>
  </AiLayoutPage>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AiButton from '@/components/AiButton.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiLayoutPage from '@/components/AiLayoutPage.vue'

const title = ref('应用功能')

onLoad((query = {}) => {
  title.value = String(query.title || '应用功能')
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
.app-entry { display: flex; min-height: 560rpx; flex-direction: column; align-items: center; justify-content: center; padding: 48rpx 28rpx; text-align: center; box-sizing: border-box; }
.app-entry__icon { display: flex; width: 100rpx; height: 100rpx; align-items: center; justify-content: center; border-radius: 26rpx; background: #eaf3ff; }
.app-entry__title { display: block; margin-top: 26rpx; color: var(--text-strong); font-size: 34rpx; font-weight: 750; }
.app-entry__desc { display: block; max-width: 520rpx; margin: 14rpx 0 32rpx; color: var(--text-muted); font-size: 25rpx; line-height: 1.65; }
</style>
