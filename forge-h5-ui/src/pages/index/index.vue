<template>
  <view class="home-page">
    <AiFeedbackHost />
    <view class="home-content">
      <view class="home-header">
        <view class="user-block" @click="goMine">
          <view class="avatar-wrap">
            <AiAuthImage class="avatar-image" :src="rawAvatarUrl" :fallback="DEFAULT_AVATAR_URL" mode="aspectFill" />
          </view>
          <view class="user-copy">
            <text class="hello-title">{{ authStore.displayName }}</text>
            <text class="hello-subtitle">{{ authStore.roleText || '移动工作台' }}</text>
          </view>
        </view>
        <button class="bell-button" @click="goMessages">
          <view class="icon-mask bell-icon" :style="iconMask('/static/icons/ai-icon/bell.svg', '#4e5969')" />
          <view v-if="unreadCount > 0" class="bell-badge">
            <text>{{ unreadCount > 99 ? '99+' : unreadCount }}</text>
          </view>
        </button>
      </view>

      <view class="home-dashboard">
        <view class="workbench-panel">
          <view class="section-head">
            <view class="section-heading">
              <text class="section-title">工作概览</text>
              <text class="section-subtitle">待处理事项与未读消息</text>
            </view>
            <button class="section-action" aria-label="刷新工作概览" @click="refreshWorkspace">
              <AiIcon icon="/static/icons/ai-icon/refresh-cw.svg" color="#4266f7" size="sm" />
            </button>
          </view>
          <view class="overview-list">
            <button class="overview-item" @click="goTodo">
              <view class="overview-icon"><AiIcon icon="/static/icons/ai-icon/check-square.svg" color="#4266f7" size="sm" /></view>
              <view class="overview-copy">
                <text class="overview-title">待办任务</text>
                <text class="overview-desc">{{ todoCount > 0 ? '有审批任务等待处理' : '当前没有待处理任务' }}</text>
              </view>
              <view class="overview-metric"><text>{{ todoCount > 99 ? '99+' : todoCount }}</text><text>项</text></view>
              <AiIcon icon="/static/icons/ai-icon/chevron-right.svg" color="#86909c" size="sm" />
            </button>
            <button class="overview-item" @click="goMessages">
              <view class="overview-icon"><AiIcon icon="/static/icons/ai-icon/bell.svg" color="#4266f7" size="sm" /></view>
              <view class="overview-copy">
                <text class="overview-title">未读消息</text>
                <text class="overview-desc">{{ unreadCount > 0 ? '有新的业务通知' : '消息已全部查看' }}</text>
              </view>
              <view class="overview-metric"><text>{{ unreadCount > 99 ? '99+' : unreadCount }}</text><text>条</text></view>
              <AiIcon icon="/static/icons/ai-icon/chevron-right.svg" color="#86909c" size="sm" />
            </button>
          </view>
        </view>

        <view class="shortcut-section">
          <view class="section-head">
            <view class="section-heading">
              <text class="section-title">常用应用</text>
              <text class="section-subtitle">快速进入已授权功能</text>
            </view>
          </view>
          <view class="shortcut-grid">
            <button
              v-for="item in menuItems"
              :key="item.key"
              class="shortcut-item"
              @click="handleShortcut(item)"
            >
              <view class="shortcut-icon"><AiIcon :icon="item.icon" :color="item.color" size="md" /></view>
              <text class="shortcut-label">{{ item.label }}</text>
            </button>
          </view>
        </view>

        <view class="feed-section">
          <view class="section-head">
            <view class="section-heading">
              <text class="section-title">最新提醒</text>
              <text class="section-subtitle">最近收到的业务消息</text>
            </view>
            <button class="section-link" @click="goMessages">
              <text>全部消息</text>
              <view class="icon-mask arrow-icon" :style="iconMask('/static/icons/ai-icon/arrow-right.svg', '#4266f7')" />
            </button>
          </view>

          <view class="message-list">
            <button
              v-for="message in messages"
              :key="message.id"
              class="message-card"
              @click="openMessage(message)"
            >
              <view class="message-icon">
                <view class="icon-mask" :style="iconMask(message.icon, message.color)" />
                <view v-if="message.unread" class="message-dot" />
              </view>
              <view class="message-main">
                <view class="message-title-row">
                  <text class="message-title">{{ message.title }}</text>
                  <text class="message-time">{{ message.time }}</text>
                </view>
                <text class="message-desc">{{ message.desc }}</text>
              </view>
              <AiIcon icon="/static/icons/ai-icon/chevron-right.svg" color="#86909c" size="sm" />
            </button>
            <view v-if="!messages.length" class="message-empty-card">
              <AiIcon icon="/static/icons/ai-icon/check-circle.svg" color="#16815d" size="md" />
              <text>暂无新提醒</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <AiPopupSheet
      v-model="menuSheetVisible"
      :scroll="false"
      :show-handle="false"
      max-height="96vh"
      body-max-height="calc(96vh - 160rpx - env(safe-area-inset-bottom))"
      title="全部应用"
      description="按模块浏览已授权的移动端菜单"
    >
      <AiSearchBar
        v-model="menuSearchKeyword"
        class="menu-search-bar"
        placeholder="搜索菜单"
        @clear="clearMenuSearch"
      />

      <scroll-view class="menu-browser" scroll-y :show-scrollbar="false">
        <view v-if="filteredMenuGroups.length" class="menu-module-list">
          <view v-for="group in filteredMenuGroups" :key="group.key" class="menu-module">
            <view class="menu-module-head">
              <text class="menu-module-title">{{ group.label }}</text>
            </view>
            <view class="menu-list-grid">
              <button
                v-for="item in group.items"
                :key="item.key"
                class="menu-list-card"
                @click="openMenuEntry(item)"
              >
                <view class="menu-list-icon">
                  <AiIcon :icon="item.icon" :color="item.color" size="md" />
                </view>
                <view class="menu-list-copy">
                  <text class="menu-list-name">{{ item.label }}</text>
                  <text class="menu-list-desc">打开功能</text>
                </view>
                <AiIcon icon="/static/icons/ai-icon/chevron-right.svg" color="#86909c" size="sm" />
              </button>
            </view>
          </view>
        </view>
        <view v-else class="menu-empty">
          <view class="menu-empty-icon">
            <AiIcon icon="/static/icons/ai-icon/inbox.svg" color="#86909c" size="lg" />
          </view>
          <text class="menu-empty-title">{{ menuSearchKeyword ? '暂无匹配' : '暂无菜单' }}</text>
          <text class="menu-empty-desc">{{ menuSearchKeyword ? '换个关键词再试试' : '当前还没有可用的应用菜单' }}</text>
        </view>
      </scroll-view>
    </AiPopupSheet>

    <AiTabBar active="home" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AiAuthImage from '@/components/AiAuthImage.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import AiTabBar from '@/components/AiTabBar.vue'
import api from '@/api'
import { useAuthStore } from '@/store'
import { resolveStaticUrl } from '@/utils/assets'
import { DEFAULT_AVATAR_URL } from '@/utils/file'
import { toast } from '@/utils/notify'

const authStore = useAuthStore()

const unreadCount = ref(0)
const todoCount = ref(0)
const latestMessages = ref([])
const menuSheetVisible = ref(false)
const menuSearchKeyword = ref('')

const rawAvatarUrl = computed(() => authStore.userInfo?.avatar || '')

const fallbackMenuItems = [
  {
    key: 'account',
    label: '账户',
    icon: '/static/icons/ai-icon/pocket.svg',
    color: '#4266f7',
    bgClass: 'bg-blue',
  },
  {
    key: 'cards',
    label: '卡包',
    icon: '/static/icons/ai-icon/credit-card.svg',
    color: '#4266f7',
    bgClass: 'bg-indigo',
  },
  {
    key: 'analytics',
    label: '数据',
    icon: '/static/icons/ai-icon/pie-chart.svg',
    color: '#4266f7',
    bgClass: 'bg-purple',
  },
  {
    key: 'service',
    label: '服务',
    icon: '/static/icons/ai-icon/zap.svg',
    color: '#4266f7',
    bgClass: 'bg-amber',
  },
]

const moreMenuItem = {
  key: 'more',
  label: '更多',
  icon: '/static/icons/ai-icon/grid.svg',
  color: '#4266f7',
  bgClass: 'bg-teal',
  isMore: true,
}

const menuToneList = [
  { icon: '/static/icons/ai-icon/pocket.svg', color: '#4266f7', bgClass: 'bg-blue' },
  { icon: '/static/icons/ai-icon/credit-card.svg', color: '#4266f7', bgClass: 'bg-blue' },
  { icon: '/static/icons/ai-icon/pie-chart.svg', color: '#4266f7', bgClass: 'bg-blue' },
  { icon: '/static/icons/ai-icon/zap.svg', color: '#4266f7', bgClass: 'bg-blue' },
  { icon: '/static/icons/ai-icon/message-square.svg', color: '#4266f7', bgClass: 'bg-blue' },
  { icon: '/static/icons/ai-icon/briefcase.svg', color: '#4266f7', bgClass: 'bg-blue' },
]

const menuItems = computed(() => {
  const backendItems = flattenMenus(authStore.menus)
  const sourceItems = backendItems.length ? backendItems : fallbackMenuItems
  return sourceItems.slice(0, 7).concat(moreMenuItem)
})

const menuGroups = computed(() => buildMenuGroups(authStore.menus))
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
const messages = computed(() => latestMessages.value.slice(0, 2))

function flattenMenus(menus = []) {
  const result = []

  function walk(list = []) {
    sortMenus(list)
      .filter(isVisibleMenu)
      .forEach((menu) => {
        const children = Array.isArray(menu.children) ? menu.children : []
        if (children.length) {
          walk(children)
          return
        }
        if (isNavigableMenu(menu)) {
          result.push(normalizeMenuEntry(menu, result.length))
        }
      })
  }

  walk(menus)
  return result
}

function buildMenuGroups(menus = []) {
  const groups = []
  const topLevelItems = []

  sortMenus(menus)
    .filter(isVisibleMenu)
    .forEach((menu) => {
      const children = Array.isArray(menu.children) ? menu.children : []
      const childItems = collectMenuEntries(children, groups.length)
      if (childItems.length) {
        groups.push({
          key: `group-${menu.id || menu.resourceName || groups.length}`,
          label: menu.resourceName || menu.title || menu.name || '未命名模块',
          items: childItems,
        })
        return
      }

      if (isNavigableMenu(menu)) {
        topLevelItems.push(normalizeMenuEntry(menu, topLevelItems.length))
      }
    })

  if (topLevelItems.length) {
    groups.unshift({
      key: 'quick',
      label: '常用',
      items: topLevelItems,
    })
  }

  if (!groups.length) {
    groups.push({
      key: 'template',
      label: '模板',
      items: fallbackMenuItems,
    })
  }

  return groups
}

function collectMenuEntries(list = [], offset = 0) {
  const result = []

  function walk(children = []) {
    sortMenus(children)
      .filter(isVisibleMenu)
      .forEach((menu) => {
        const nextChildren = Array.isArray(menu.children) ? menu.children : []
        if (nextChildren.length) {
          walk(nextChildren)
          return
        }
        if (isNavigableMenu(menu)) {
          result.push(normalizeMenuEntry(menu, offset + result.length))
        }
      })
  }

  walk(list)
  return result
}

function sortMenus(list = []) {
  return [...list].sort((a, b) => Number(a?.sort || 0) - Number(b?.sort || 0))
}

function isVisibleMenu(menu) {
  return menu && menu.visible !== 0 && menu.menuStatus !== 0
}

function isNavigableMenu(menu) {
  return Boolean(String(menu?.component || menu?.path || '').trim())
}

function isRegisteredH5Route(path) {
  const normalized = String(path || '').split('?')[0].replace(/^\//, '')
  return [
    'pages/index/index',
    'pages/message/index',
    'pages/todo',
    'pages/mine/index',
    'pages/demo/loading/index',
    'pages/app-entry',
    'pages/lowcode-runtime',
  ].includes(normalized)
}

function normalizeMenuEntry(menu, index = 0) {
  const tone = menuToneList[index % menuToneList.length]
  return {
    ...tone,
    key: menu.id || menu.path || menu.resourceName,
    label: menu.resourceName || menu.title || menu.name || '未命名',
    path: menu.path,
    component: menu.component,
    icon: normalizeMenuIcon(menu.icon, tone.icon),
    external: menu.isExternal === 1,
    fromBackend: true,
  }
}

function normalizeMenuIcon(icon, fallbackIcon) {
  const iconValue = String(icon || '').trim()
  return iconValue || fallbackIcon
}

function normalizeSearchText(value) {
  return String(value || '').trim().toLowerCase()
}

onShow(async () => {
  hideNativeTabBar()
  await refreshWorkspace({ silent: true })
})

function hideNativeTabBar() {
  if (typeof uni === 'undefined' || typeof uni.hideTabBar !== 'function') {
    return
  }
  uni.hideTabBar({
    animation: false,
    fail: () => {},
  })
}

function iconMask(icon, color) {
  const url = resolveStaticUrl(icon)
  return {
    backgroundColor: color,
    WebkitMask: `url(${url}) center / contain no-repeat`,
    mask: `url(${url}) center / contain no-repeat`,
  }
}

function goMine() {
  uni.switchTab({ url: '/pages/mine/index' })
}

function goMessages() {
  uni.navigateTo({ url: '/pages/message/index' })
}

function goTodo() {
  uni.switchTab({ url: '/pages/todo' })
}

function handleShortcut(item) {
  if (item.isMore) {
    openMenuSheet()
    return
  }
  if (item.key === 'account') {
    goMine()
    return
  }
  if (item.key === 'component-demo') {
    uni.navigateTo({ url: '/pages/demo/loading/index' })
    return
  }
  if (item.fromBackend) {
    openBackendMenu(item)
    return
  }
  toast(`${item.label}待接入`, { type: 'info' })
}

function openMenuSheet() {
  menuSearchKeyword.value = ''
  menuSheetVisible.value = true
}

function clearMenuSearch() {
  menuSearchKeyword.value = ''
}

function openMenuEntry(item) {
  if (item.isMore) {
    return
  }
  menuSheetVisible.value = false
  handleShortcut(item)
}

function openBackendMenu(item) {
  const path = item.component || item.path
  if (String(path || '').split('?')[0] === '/pages/todo') {
    uni.switchTab({ url: '/pages/todo' })
    return
  }
  if (path && path.startsWith('/pages/')) {
    if (isRegisteredH5Route(path)) {
      uni.navigateTo({ url: path })
      return
    }
    uni.navigateTo({ url: `/pages/app-entry?title=${encodeURIComponent(item.label)}&path=${encodeURIComponent(path)}` })
    return
  }
  const lowcodeConfigKey = resolveLowcodeConfigKey(path)
  if (lowcodeConfigKey) {
    uni.navigateTo({ url: `/pages/lowcode-runtime?configKey=${encodeURIComponent(lowcodeConfigKey)}&title=${encodeURIComponent(item.label)}` })
    return
  }
  toast(`${item.label}页面待接入`, { type: 'info' })
}

function resolveLowcodeConfigKey(path) {
  return String(path || '').match(/(?:crud-page|crud)\/([^/?]+)/)?.[1] || ''
}

function openMessage(message) {
  if (message.fromBackend) {
    uni.navigateTo({ url: `/pages/message/index?id=${message.id}` })
    return
  }
  goMessages()
}

async function refreshWorkspace(options = {}) {
  try {
    await Promise.all([
      authStore.fetchUserInfo(),
      authStore.fetchAccessSnapshot(),
      fetchMessageSummary(),
      fetchTodoSummary(),
    ])
    if (!options.silent) {
      toast('已同步', { type: 'success' })
    }
  }
  catch (error) {
    console.error('刷新首页信息失败:', error)
  }
}

async function fetchTodoSummary() {
  const user = authStore.userInfo || {}
  const userId = user.id || user.userId || user.user_id
  if (!userId) {
    todoCount.value = 0
    return
  }
  try {
    const res = await api.getTodoTasks({ pageNum: 1, pageSize: 1, userId })
    todoCount.value = Number(res?.data?.total || 0)
  }
  catch (error) {
    todoCount.value = 0
    console.error('加载待办摘要失败:', error)
  }
}

async function fetchMessageSummary() {
  try {
    const [countResult, pageResult] = await Promise.allSettled([
      api.getUnreadMessageCount(),
      api.getMessagePage({ pageNum: 1, pageSize: 5 }),
    ])

    if (countResult.status === 'fulfilled') {
      unreadCount.value = normalizeUnreadCount(countResult.value?.data)
    }

    if (pageResult.status === 'fulfilled') {
      const records = normalizeMessageRecords(pageResult.value?.data)
      latestMessages.value = records.map(normalizeHomeMessage)
    }
  }
  catch (error) {
    console.error('加载消息摘要失败:', error)
  }
}

function normalizeUnreadCount(data) {
  if (typeof data === 'number') {
    return data
  }
  return Number(data?.totalCount || data?.unreadCount || data?.count || 0)
}

function normalizeMessageRecords(data) {
  if (Array.isArray(data)) {
    return data
  }
  return data?.records || data?.list || data?.rows || []
}

function normalizeHomeMessage(message) {
  const isApproval = message?.bizType === 'FLOW_TODO'
  const unread = Number(message?.readFlag) === 0
  return {
    id: message.id,
    title: message.title || '消息通知',
    desc: stripHtml(message.content || message.description || '-'),
    time: formatMessageTime(message.createTime || message.receiveTime),
    icon: isApproval ? '/static/icons/ai-icon/check-square.svg' : '/static/icons/ai-icon/message-square.svg',
    color: unread ? '#4266f7' : '#4e5969',
    bgClass: isApproval ? 'bg-emerald' : unread ? 'bg-blue' : 'bg-slate',
    unread,
    fromBackend: true,
  }
}

function stripHtml(value) {
  return String(value || '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function formatMessageTime(value) {
  const date = parseMessageDate(value)
  if (!date) {
    return ''
  }
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff >= 0 && diff < 60 * 1000) {
    return '刚刚'
  }
  if (diff >= 0 && diff < 60 * 60 * 1000) {
    return `${Math.max(1, Math.floor(diff / 60000))}分钟前`
  }
  if (diff >= 0 && diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${month}-${day}`
}

function parseMessageDate(value) {
  if (!value) {
    return null
  }
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    return new Date(year, month - 1, day, hour, minute, second)
  }
  const date = new Date(typeof value === 'string' ? value.replace(' ', 'T') : value)
  return Number.isNaN(date.getTime()) ? null : date
}

</script>

<style lang="scss" scoped src="../styles/home.scss"></style>
