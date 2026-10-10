<template>
  <view class="home-page">
    <AiFeedbackHost />
    <!-- 顶栏：组织信息 + 应用搜索，浅蓝渐变过渡到页面底色 -->
    <AiTabHeader title="工作台" gradient search-placeholder="搜索应用" @search="openMenuSheet" @brand="goMine" />

    <view class="home-content">
      <HomeWorkspaceSkeleton v-if="workspaceLoading" />
      <view v-else class="home-dashboard">
        <!-- 轮播图：固定两张宣传图 -->
        <HomeBanner class="home-banner-slot" />

        <!-- 概览：问候 + 待办/未读/我发起，点击直达对应页签 -->
        <view class="overview-card">
          <view class="overview-greeting" @click="goMine">
            <view class="avatar-wrap">
              <AiAuthImage v-if="rawAvatarUrl" class="avatar-image" :src="rawAvatarUrl" :fallback="brandLogoUrl || '/static/logo.png'" mode="aspectFill" />
              <image v-else class="avatar-image" :src="brandLogoUrl || '/static/logo.png'" mode="aspectFit" />
            </view>
            <view class="greeting-copy">
              <text class="greeting-title">{{ greeting }}，{{ authStore.displayName }}</text>
              <text class="greeting-subtitle">{{ authStore.roleText || '移动工作台' }}</text>
            </view>
            <button class="overview-start" type="button" @click.stop="openApprovalStart">
              <AiIcon icon="/static/icons/ai-icon/plus.svg" color="#0066ff" size="sm" />
              <text>发起审批</text>
            </button>
          </view>
          <view class="overview-list">
            <button v-for="item in overviewItems" :key="item.key" class="overview-item" :class="`is-${item.tone}`" type="button" @click.stop="item.open">
              <image class="overview-icon" :src="item.icon" mode="aspectFit" />
              <text class="overview-value" :class="{ 'is-alert': item.alert && item.value > 0 }">{{ formatCount(item.value) }}</text>
              <view class="overview-label">
                <text>{{ item.label }}</text>
                <AiIcon icon="/static/icons/ai-icon/chevron-right.svg" color="#a2a3a5" size="xs" />
              </view>
            </button>
          </view>
          <NoticeBanner />
        </view>

        <!-- 常用应用：最多 7 个 + 全部 -->
        <view class="shortcut-section">
          <view class="section-head">
            <text class="section-title">常用应用</text>
            <button v-if="allMenuItems.length" class="section-link" @click="openMenuSheet">
              <text>全部</text>
              <AiIcon icon="/static/icons/ai-icon/chevron-right.svg" color="#a2a3a5" size="sm" />
            </button>
          </view>
          <view class="shortcut-grid">
            <button v-for="item in menuItems" :key="item.key" class="shortcut-item" @click="openBackendMenu(item)">
              <AiAppIcon :icon="item.icon" :color="item.color" :bg="item.toneBg" />
              <text class="shortcut-label">{{ item.label }}</text>
            </button>
            <button v-if="allMenuItems.length" class="shortcut-item shortcut-more" @click="openMenuSheet">
              <AiAppIcon :icon="appIconUrl('more')" />
              <text class="shortcut-label">全部应用</text>
            </button>
          </view>
          <view v-if="!menuItems.length" class="menu-empty-inline">暂无可在移动端打开的授权应用</view>
        </view>

        <!-- 分组应用：按后台菜单目录切换，只有一个分组时不显示 -->
        <view v-if="menuGroups.length > 1" class="group-section">
          <scroll-view class="group-tabs" scroll-x :show-scrollbar="false">
            <button
              v-for="group in menuGroups"
              :key="group.key"
              class="group-tab"
              :class="{ active: activeGroup?.key === group.key }"
              @click="activeGroupKey = group.key"
            >
              {{ group.label }}
            </button>
          </scroll-view>
          <view class="group-grid">
            <button v-for="item in activeGroup?.items || []" :key="item.key" class="shortcut-item" @click="openBackendMenu(item)">
              <AiAppIcon :icon="item.icon" :color="item.color" :bg="item.toneBg" />
              <text class="shortcut-label">{{ item.label }}</text>
            </button>
          </view>
        </view>
      </view>
    </view>

    <!-- 全部应用弹层：分组浏览 + 搜索 -->
    <AiPopupSheet
      v-model="menuSheetVisible"
      :scroll="false"
      :show-handle="false"
      max-height="96vh"
      body-max-height="calc(96vh - 160rpx - env(safe-area-inset-bottom))"
      title="全部应用"
      description="按模块浏览已授权的移动端应用"
    >
      <AiSearchBar
        v-model="menuSearchKeyword"
        class="menu-search-bar"
        placeholder="搜索应用"
        @clear="clearMenuSearch"
      />

      <scroll-view class="menu-browser" scroll-y :show-scrollbar="false" refresher-enabled :refresher-triggered="menuRefreshing" @refresherrefresh="refreshMenus">
        <view v-if="filteredMenuGroups.length" class="menu-module-list">
          <view v-for="group in filteredMenuGroups" :key="group.key" class="menu-module">
            <text class="menu-module-title">{{ group.label }}</text>
            <view class="menu-module-grid">
              <button
                v-for="item in group.items"
                :key="item.key"
                class="shortcut-item"
                @click="openMenuEntry(item)"
              >
                <AiAppIcon :icon="item.icon" :color="item.color" :bg="item.toneBg" />
                <text class="shortcut-label">{{ item.label }}</text>
              </button>
            </view>
          </view>
        </view>
        <AiEmpty
          v-else
          :title="menuSearchKeyword ? '暂无匹配' : '暂无应用'"
          :description="menuSearchKeyword ? '换个关键词再试试' : '当前还没有可用的移动端应用'"
          icon="inbox"
        />
      </scroll-view>
    </AiPopupSheet>

    <AiTabBar active="home" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import AiAppIcon from '@/components/AiAppIcon.vue'
import AiAuthImage from '@/components/AiAuthImage.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import AiTabBar from '@/components/AiTabBar.vue'
import AiTabHeader from '@/components/AiTabHeader.vue'
import HomeBanner from '@/components/home/HomeBanner.vue'
import HomeWorkspaceSkeleton from '@/components/home/HomeWorkspaceSkeleton.vue'
import NoticeBanner from '@/components/notice/NoticeBanner.vue'
import api from '@/api'
import { useAppStore, useAuthStore, useBadgeStore, useNoticeStore } from '@/store'
import { resolveStaticUrl } from '@/utils/assets'
import { toast } from '@/utils/notify'
import { appIconUrl, buildMobileMenuGroups, flattenMobileMenus } from '@/utils/mobile-menu'
import { openTab } from '@/utils/tab-handoff'

const authStore = useAuthStore()
const appStore = useAppStore()
const badgeStore = useBadgeStore()
const noticeStore = useNoticeStore()
const defaultBrandLogo = resolveStaticUrl('/static/logo.png')

const startedCount = ref(0)
const menuSheetVisible = ref(false)
const menuSearchKeyword = ref('')
const menuRefreshing = ref(false)
const workspaceLoading = ref(true)
const activeGroupKey = ref('')

const rawAvatarUrl = computed(() => authStore.avatar)
const brandLogoUrl = computed(() => appStore.brandLogoUrl || defaultBrandLogo)
const allMenuItems = computed(() => flattenMobileMenus(authStore.menus))
const menuItems = computed(() => allMenuItems.value.slice(0, 7))
const menuGroups = computed(() => buildMobileMenuGroups(authStore.menus))
const activeGroup = computed(() => menuGroups.value.find(group => group.key === activeGroupKey.value) || menuGroups.value[0])

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '早上好'
  if (hour < 13) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const overviewIcon = key => resolveStaticUrl(appIconUrl(key))
const overviewItems = computed(() => [
  {
    key: 'todo', label: '待我处理', icon: overviewIcon('approval'), tone: 'blue', value: badgeStore.todoCount, alert: true,
    open: () => openTab('todo', '/pages/todo', { scope: 'todo' }),
  },
  {
    key: 'unread', label: '未读消息', icon: overviewIcon('notice'), tone: 'orange', value: badgeStore.unreadCount, alert: true,
    open: () => openTab('message', '/pages/message/index', { tab: 'unread' }),
  },
  {
    key: 'started', label: '我发起的', icon: overviewIcon('file'), tone: 'green', value: startedCount.value,
    open: () => openTab('todo', '/pages/todo', { scope: 'started' }),
  },
])

const filteredMenuGroups = computed(() => {
  const keyword = normalizeSearchText(menuSearchKeyword.value)
  if (!keyword) {
    return menuGroups.value
  }

  return menuGroups.value
    .map((group) => {
      const groupMatched = normalizeSearchText(group.label).includes(keyword)
      const items = group.items.filter((item) => {
        return groupMatched
          || normalizeSearchText(item.label).includes(keyword)
          || normalizeSearchText(item.path).includes(keyword)
          || normalizeSearchText(item.component).includes(keyword)
      })
      return { ...group, items }
    })
    .filter(group => group.items.length)
})

function normalizeSearchText(value) {
  return String(value || '').trim().toLowerCase()
}

function formatCount(value) {
  const count = Number(value || 0)
  return count > 99 ? '99+' : String(count)
}

onShow(async () => {
  await refreshWorkspace({ silent: true })
})

onPullDownRefresh(async () => {
  try { await refreshWorkspace({ silent: true }) }
  finally { uni.stopPullDownRefresh() }
})

function goMine() {
  uni.switchTab({ url: '/pages/mine/index' })
}

function openApprovalStart() {
  uni.navigateTo({ url: '/pages/approval/start' })
}

function openMenuSheet() {
  menuSearchKeyword.value = ''
  menuSheetVisible.value = true
}

function clearMenuSearch() {
  menuSearchKeyword.value = ''
}

function openMenuEntry(item) {
  menuSheetVisible.value = false
  openBackendMenu(item)
}

function openBackendMenu(item) {
  if (!item.target) return
  const url = item.target.url
  if (item.target.tab) uni.switchTab({ url: url.split('?')[0] })
  else uni.navigateTo({ url })
}

async function refreshMenus() {
  if (menuRefreshing.value) return
  menuRefreshing.value = true
  try { await authStore.fetchAccessSnapshot() }
  finally { menuRefreshing.value = false }
}

async function refreshWorkspace(options = {}) {
  try {
    await Promise.all([
      authStore.fetchUserInfo(),
      authStore.fetchAccessSnapshot(),
    ])
    await Promise.all([badgeStore.refresh(), fetchStartedSummary(), noticeStore.loadLatest()])
    if (!options.silent) {
      toast('已同步', { type: 'success' })
    }
  }
  catch (error) {
    console.error('刷新工作台失败:', error)
  }
  finally {
    // 骨架屏只负责首次进入；后续 onShow/下拉刷新保留现有内容，避免页面闪烁。
    workspaceLoading.value = false
  }
}

async function fetchStartedSummary() {
  const user = authStore.userInfo || {}
  const userId = user.id || user.userId || user.user_id
  if (!userId) {
    startedCount.value = 0
    return
  }
  try {
    const res = await api.getStartedFlowTasks({ pageNum: 1, pageSize: 1, userId })
    startedCount.value = Number(res?.data?.total || 0)
  }
  catch (error) {
    console.error('加载我发起的流程数量失败:', error)
  }
}
</script>

<style lang="scss" scoped src="../styles/home.scss"></style>
