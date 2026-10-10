<template>
  <view class="contacts-page">
    <AiTabHeader title="通讯录" :searchable="false" />

    <!-- 搜索 -->
    <view class="contacts-search">
      <AiSearchBar v-model="keyword" placeholder="搜索姓名或账号" @search="search.reload" @clear="search.reload" />
    </view>

    <scroll-view class="contacts-scroll" scroll-y :show-scrollbar="false" @scrolltolower="handleScrollToLower">
      <!-- 搜索结果：有关键字时替换首页内容 -->
      <view v-if="searching" class="contacts-section">
        <AiListSkeleton v-if="search.loading.value && !search.members.value.length" :rows="5" />
        <AiEmpty
          v-else-if="!search.members.value.length"
          :title="search.failed.value ? '搜索失败' : '没有找到成员'"
          :description="search.failed.value ? '请稍后重试。' : '换个关键字试试。'"
          icon="users"
        />
        <ContactMemberList
          v-else
          :members="search.members.value"
          :total="search.total.value"
          :loading="search.loading.value"
          :finished="search.finished.value"
          :failed="search.failed.value"
          @retry="search.loadMore"
        />
      </view>

      <template v-else>
        <!-- 组织卡片 -->
        <view class="contacts-org-card">
          <view class="contacts-org-card__head">
            <image v-if="brandLogoUrl" class="contacts-org-card__logo" :src="brandLogoUrl" mode="aspectFill" />
            <text v-else class="contacts-org-card__logo contacts-org-card__logo--text">{{ tenantInitials }}</text>
            <view class="contacts-org-card__copy">
              <text class="contacts-org-card__name">{{ tenantName }}</text>
              <text class="contacts-org-card__meta">{{ summaryLoading ? '正在加载…' : `共 ${memberCount} 人` }}</text>
            </view>
          </view>
          <view class="contacts-entry" @click="openOrg()">
            <AiAppIcon icon="layers" :color="MENU_TONES.blue.color" :bg="MENU_TONES.blue.bg" size="sm" />
            <text class="contacts-entry__label">组织架构</text>
            <AiIcon name="chevron-right" color="#c1c3c6" size="sm" />
          </view>
          <!-- 未分配部门的成员不显示“我的部门” -->
          <view v-if="summary.myOrgId" class="contacts-entry" @click="openOrg(summary.myOrgId, summary.myOrgName)">
            <AiAppIcon icon="users" :color="MENU_TONES.green.color" :bg="MENU_TONES.green.bg" size="sm" />
            <text class="contacts-entry__label">我的部门</text>
            <text class="contacts-entry__extra">{{ summary.myOrgName }}</text>
            <AiIcon name="chevron-right" color="#c1c3c6" size="sm" />
          </view>
        </view>

        <!-- 全部成员 -->
        <view class="contacts-section">
          <text class="contacts-section__title">全部成员</text>
          <AiListSkeleton v-if="all.loading.value && !all.members.value.length" :rows="5" />
          <AiEmpty
            v-else-if="!all.members.value.length"
            :title="all.failed.value ? '成员加载失败' : '暂无成员'"
            :description="all.failed.value ? '下拉刷新重试。' : '当前组织还没有可见成员。'"
            icon="users"
          />
          <ContactMemberList
            v-else
            :members="all.members.value"
            :total="all.total.value"
            :loading="all.loading.value"
            :finished="all.finished.value"
            :failed="all.failed.value"
            @retry="all.loadMore"
          />
        </view>
      </template>
    </scroll-view>

    <AiTabBar active="contacts" />
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import AiAppIcon from '@/components/AiAppIcon.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import AiTabBar from '@/components/AiTabBar.vue'
import AiTabHeader from '@/components/AiTabHeader.vue'
import ContactMemberList from '@/components/contacts/ContactMemberList.vue'
import api from '@/api'
import { useContactMembers } from '@/composables/useContactMembers'
import { useAppStore, useAuthStore } from '@/store'
import { buildContactOrgUrl } from '@/utils/contacts'
import { MENU_TONES } from '@/utils/mobile-menu'

const appStore = useAppStore()
const authStore = useAuthStore()

// —— 组织概要 ——
const summary = ref({})
const summaryLoading = ref(false)
const tenantName = computed(() => summary.value.tenantName || authStore.userInfo?.tenantName || appStore.brandName || '我的组织')
const tenantInitials = computed(() => tenantName.value.slice(0, 2))
const brandLogoUrl = computed(() => appStore.brandLogoUrl || '')
const memberCount = computed(() => Number(summary.value.memberCount || 0))

async function loadSummary() {
  summaryLoading.value = true
  try {
    const res = await api.getContactsSummary()
    summary.value = res?.data || {}
  }
  catch (error) {
    console.error('加载通讯录概要失败:', error)
  }
  finally {
    summaryLoading.value = false
  }
}

// —— 成员列表与搜索 ——
const keyword = ref('')
const searching = computed(() => Boolean(keyword.value.trim()))
const all = useContactMembers(() => ({}))
const search = useContactMembers(() => (searching.value ? { keyword: keyword.value.trim() } : null))

let searchTimer = null
watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => search.reload(), 300)
})

function handleScrollToLower() {
  if (searching.value) search.loadMore()
  else all.loadMore()
}

function openOrg(orgId, orgName) {
  uni.navigateTo({ url: buildContactOrgUrl({ orgId, orgName }) })
}

async function refreshAll() {
  await Promise.all([loadSummary(), all.reload()])
}

onShow(() => {
  // 从成员详情返回时不重复拉取，只有首次进入或列表为空才加载
  if (!all.members.value.length) refreshAll()
})

onPullDownRefresh(async () => {
  try { await refreshAll() }
  finally { uni.stopPullDownRefresh() }
})
</script>

<style lang="scss" scoped src="../styles/contacts.scss"></style>
