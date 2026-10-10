<template>
  <view class="contacts-page contacts-page--stack">
    <!-- 面包屑：层级在页内切换，返回键直接回到通讯录首页 -->
    <scroll-view class="contacts-trail" scroll-x :show-scrollbar="false" :scroll-into-view="trailAnchor">
      <view class="contacts-trail__inner">
        <view
          v-for="(node, index) in trail"
          :id="`trail-${index}`"
          :key="`${node.id}-${index}`"
          class="contacts-trail__item"
        >
          <text
            class="contacts-trail__text"
            :class="{ 'is-current': index === trail.length - 1 }"
            @click="jumpTo(index)"
          >{{ node.name }}</text>
          <AiIcon v-if="index < trail.length - 1" name="chevron-right" color="#c1c3c6" size="xs" />
        </view>
      </view>
    </scroll-view>

    <scroll-view
      class="contacts-scroll contacts-scroll--stack"
      scroll-y
      :show-scrollbar="false"
      refresher-enabled
      :refresher-triggered="pullRefreshing"
      @refresherrefresh="refreshByPull"
      @scrolltolower="memberList.loadMore"
    >
      <AiListSkeleton v-if="orgLoading && !orgs.length" :rows="4" />

      <!-- 下级部门 -->
      <view v-if="orgs.length" class="contacts-section">
        <view class="contacts-org-list">
          <view v-for="org in orgs" :key="org.id" class="contacts-org-row" @click="enter(org)">
            <AiAppIcon icon="folder" :color="MENU_TONES.blue.color" :bg="MENU_TONES.blue.bg" size="sm" />
            <text class="contacts-org-row__name">{{ org.orgName }}</text>
            <text class="contacts-org-row__count">{{ Number(org.memberCount || 0) }}人</text>
            <AiIcon name="chevron-right" color="#c1c3c6" size="sm" />
          </view>
        </view>
      </view>

      <!-- 部门直属成员（根节点只展示部门） -->
      <view v-if="current.id" class="contacts-section">
        <text class="contacts-section__title">部门成员</text>
        <AiListSkeleton v-if="memberList.loading.value && !memberList.members.value.length" :rows="4" />
        <AiEmpty
          v-else-if="!memberList.members.value.length && !orgs.length"
          :title="memberList.failed.value ? '成员加载失败' : '暂无成员'"
          :description="memberList.failed.value ? '下拉刷新重试。' : '该部门还没有直属成员。'"
          icon="users"
        />
        <ContactMemberList
          v-else-if="memberList.members.value.length"
          :members="memberList.members.value"
          :total="memberList.total.value"
          :loading="memberList.loading.value"
          :finished="memberList.finished.value"
          :failed="memberList.failed.value"
          @retry="memberList.loadMore"
        />
        <text v-else class="contacts-section__hint">该部门没有直属成员</text>
      </view>

      <AiEmpty
        v-if="!current.id && !orgLoading && !orgs.length"
        :title="orgFailed ? '组织架构加载失败' : '暂无部门'"
        :description="orgFailed ? '下拉刷新重试。' : '管理员还没有维护组织架构。'"
        icon="layers"
      />
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AiAppIcon from '@/components/AiAppIcon.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import ContactMemberList from '@/components/contacts/ContactMemberList.vue'
import api from '@/api'
import { useContactMembers } from '@/composables/useContactMembers'
import { usePullRefresh } from '@/composables/usePullRefresh'
import { useAppStore, useAuthStore } from '@/store'
import { safeDecode } from '@/utils/contacts'
import { MENU_TONES } from '@/utils/mobile-menu'

const appStore = useAppStore()
const authStore = useAuthStore()
const rootName = authStore.userInfo?.tenantName || appStore.brandName || '组织架构'

// —— 层级路径 ——
const trail = ref([{ id: 0, name: rootName }])
const current = computed(() => trail.value[trail.value.length - 1])
const trailAnchor = computed(() => `trail-${trail.value.length - 1}`)

// —— 下级部门与成员 ——
const orgs = ref([])
const orgLoading = ref(false)
const orgFailed = ref(false)
const memberList = useContactMembers(() => (current.value.id ? { orgId: current.value.id } : null))
let orgSeq = 0

async function loadOrgs() {
  const seq = ++orgSeq
  orgLoading.value = true
  orgFailed.value = false
  try {
    const res = await api.getContactOrgs(current.value.id)
    if (seq === orgSeq) orgs.value = Array.isArray(res?.data) ? res.data : []
  }
  catch (error) {
    if (seq !== orgSeq) return
    console.error('加载组织架构失败:', error)
    orgs.value = []
    orgFailed.value = true
  }
  finally {
    if (seq === orgSeq) orgLoading.value = false
  }
}

function reloadLevel() {
  orgs.value = []
  return Promise.all([loadOrgs(), memberList.reload()])
}

function enter(org) {
  if (!org?.id) return
  trail.value = [...trail.value, { id: org.id, name: org.orgName }]
  reloadLevel()
}

function jumpTo(index) {
  if (index >= trail.value.length - 1) return
  trail.value = trail.value.slice(0, index + 1)
  reloadLevel()
}

onLoad((query = {}) => {
  // “我的部门”直接定位到该部门，面包屑保留根节点便于向上浏览
  if (query.orgId) {
    trail.value = [
      { id: 0, name: rootName },
      { id: query.orgId, name: safeDecode(query.orgName) || '部门' },
    ]
  }
  reloadLevel()
})

const { refreshing: pullRefreshing, onRefresh: refreshByPull } = usePullRefresh(reloadLevel)
</script>

<style lang="scss" scoped src="../styles/contacts.scss"></style>
