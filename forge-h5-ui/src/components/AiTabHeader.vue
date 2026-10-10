<template>
  <view class="ai-tab-header" :class="{ 'is-gradient': gradient, 'is-embedded': embedded, 'is-capsule': navMetrics.barHeight > 0 }" :style="headerStyle">
    <view class="ai-tab-header__bar" :style="barStyle">
      <!-- 企业微信等第三方 App 内嵌浏览器自带标题栏，只保留搜索和功能区，避免出现双标题 -->
      <view v-if="!embedded" class="ai-tab-header__brand" @click="emit('brand')">
        <!-- showOrg=false：页面正文已有组织信息（如通讯录组织卡片），顶栏只留标题 -->
        <template v-if="showOrg">
          <image v-if="logoUrl" class="ai-tab-header__logo" :src="logoUrl" mode="aspectFill" />
          <text v-else class="ai-tab-header__logo ai-tab-header__logo--text">{{ initials }}</text>
        </template>
        <view class="ai-tab-header__copy">
          <text class="ai-tab-header__title">{{ title }}</text>
          <text v-if="showOrg && orgName" class="ai-tab-header__subtitle">{{ orgName }}</text>
        </view>
      </view>
      <view class="ai-tab-header__tools" :class="{ 'is-full': embedded }">
        <button v-if="searchable" class="ai-tab-header__search" @click="emit('search')">
          <AiIcon icon="/static/icons/ai-icon/search.svg" color="#a2a3a5" size="xs" />
          <text>{{ searchPlaceholder }}</text>
        </button>
        <slot name="actions" />
      </view>
    </view>
    <slot />
  </view>
</template>

<script setup>
import { computed } from 'vue'
import AiIcon from '@/components/AiIcon.vue'
import { useAppStore, useAuthStore } from '@/store'
import { isEmbeddedHost } from '@/utils/embedded-host'

const props = defineProps({
  title: { type: String, required: true },
  searchable: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索' },
  gradient: { type: Boolean, default: false },
  showOrg: { type: Boolean, default: true },
})
const emit = defineEmits(['search', 'brand'])

const appStore = useAppStore()
const authStore = useAuthStore()

const orgName = computed(() => authStore.userInfo?.tenantName || appStore.brandName || '')
const logoUrl = computed(() => appStore.brandLogoUrl || '')
const initials = computed(() => (orgName.value || props.title).slice(0, 2))
const navMetrics = resolveNavMetrics()
const embedded = isEmbeddedHost()
const headerStyle = {
  paddingTop: `${navMetrics.statusBarHeight}px`,
  ...(navMetrics.capsuleSpace ? { paddingRight: `${navMetrics.capsuleSpace}px` } : {}),
}
const barStyle = navMetrics.barHeight ? { minHeight: `${navMetrics.barHeight}px`, height: `${navMetrics.barHeight}px` } : {}

/**
 * 小程序右上角胶囊按钮与自绘顶栏同一行：行高对齐胶囊，右侧让出胶囊宽度，否则搜索框和右侧按钮会被盖住。
 * H5 / App 没有胶囊，barHeight、capsuleSpace 为 0 时沿用样式里的 56px 行高和页面边距。
 */
function resolveNavMetrics() {
  const metrics = { statusBarHeight: 0, barHeight: 0, capsuleSpace: 0 }
  try {
    const system = uni.getSystemInfoSync() || {}
    metrics.statusBarHeight = Number(system.statusBarHeight || 0)
    // #ifdef MP
    const capsule = uni.getMenuButtonBoundingClientRect?.()
    if (capsule?.height && capsule.left) {
      metrics.barHeight = (capsule.top - metrics.statusBarHeight) * 2 + capsule.height
      metrics.capsuleSpace = Number(system.windowWidth || 0) - capsule.left + 8
    }
    // #endif
  }
  catch {}
  return metrics
}
</script>

<style lang="scss" scoped>
.ai-tab-header {
  position: relative;
  padding-right: var(--forge-space-page);
  padding-left: var(--forge-space-page);
}

.ai-tab-header.is-gradient {
  background: linear-gradient(180deg, #dce9ff 0%, #ebf2ff 55%, var(--forge-page-bg) 100%);
}

.ai-tab-header__bar {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 56px;
}

.ai-tab-header.is-embedded .ai-tab-header__bar {
  min-height: 52px;
}

.ai-tab-header__brand {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ai-tab-header__logo {
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--forge-surface);
}

.ai-tab-header__logo--text {
  background: var(--forge-color-primary-soft);
  color: var(--forge-color-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 36px;
  text-align: center;
}

.ai-tab-header__copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.ai-tab-header__title {
  color: var(--forge-text-primary);
  font-size: 20px;
  font-weight: 600;
  line-height: 1.25;
}

.ai-tab-header__subtitle {
  overflow: hidden;
  color: var(--forge-text-secondary);
  font-size: 11px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-tab-header__tools {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.ai-tab-header__tools.is-full {
  flex: 1;
}

.ai-tab-header__search {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 34px;
  margin: 0;
  padding: 0 14px;
  border: 0;
  border-radius: 17px;
  background: rgba(23, 26, 29, 0.06);
  color: var(--forge-text-tertiary);
  font-size: 14px;
  line-height: 34px;
}

.ai-tab-header__tools.is-full .ai-tab-header__search {
  flex: 1;
}

.ai-tab-header__search::after {
  display: none;
}

/* 胶囊行高约 40px，标题区和搜索框随之收紧 */
.ai-tab-header.is-capsule .ai-tab-header__logo {
  flex-basis: 30px;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  line-height: 30px;
}

.ai-tab-header.is-capsule .ai-tab-header__title {
  font-size: 17px;
}

.ai-tab-header.is-capsule .ai-tab-header__subtitle {
  font-size: 10px;
}

.ai-tab-header.is-capsule .ai-tab-header__search {
  height: 30px;
  padding: 0 12px;
  border-radius: 15px;
  font-size: 13px;
  line-height: 30px;
}
</style>
