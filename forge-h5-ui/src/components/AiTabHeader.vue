<template>
  <view class="ai-tab-header" :class="{ 'is-gradient': gradient, 'is-embedded': embedded }" :style="{ paddingTop: `${statusBarHeight}px` }">
    <view class="ai-tab-header__bar">
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
const statusBarHeight = resolveStatusBarHeight()
const embedded = isEmbeddedHost()

function resolveStatusBarHeight() {
  try { return Number(uni.getSystemInfoSync()?.statusBarHeight || 0) }
  catch { return 0 }
}

function isEmbeddedHost() {
  const ua = typeof navigator === 'undefined' ? '' : String(navigator.userAgent || '')
  return /wxwork|DingTalk/i.test(ua)
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
</style>
