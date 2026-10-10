<template>
  <view class="approval-start-page">
    <AiFeedbackHost />

    <!-- 搜索：只在已加载目录里按应用名、单据名过滤 -->
    <view class="approval-start-search">
      <AiSearchBar v-model="keyword" placeholder="搜索审批" />
    </view>

    <scroll-view class="approval-start-scroll" scroll-y :show-scrollbar="false">
      <view v-if="loading" class="approval-start-group"><AiListSkeleton :rows="3" /></view>
      <AiEmpty
        v-else-if="!groups.length"
        :title="emptyTitle"
        :description="emptyDescription"
        icon="send"
      />
      <!-- 按业务应用分组，4 列宫格 -->
      <template v-else>
        <view v-for="group in groups" :key="group.key" class="approval-start-group">
          <text class="approval-start-group__title">{{ group.label }}</text>
          <view class="approval-start-grid">
            <button v-for="item in group.items" :key="item.key" class="approval-start-item" @click="openItem(item)">
              <AiAppIcon :icon="item.icon" :color="item.color" :bg="item.toneBg" />
              <text class="approval-start-item__label">{{ item.label }}</text>
            </button>
          </view>
        </view>
        <view class="approval-start-bottom" />
      </template>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AiAppIcon from '@/components/AiAppIcon.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import api from '@/api'
import { useAuthStore } from '@/store'
import { flattenMobileMenus } from '@/utils/mobile-menu'
import { FLOW_PERMISSIONS } from '@/utils/permission'
import { buildStartableGroups, indexMenusByConfigKey } from '@/utils/startable-objects'

const authStore = useAuthStore()
const keyword = ref('')
const objects = ref([])
const loading = ref(true)
const failed = ref(false)

const canStart = computed(() => authStore.hasPermission(FLOW_PERMISSIONS.start))
const menuIndex = computed(() => indexMenusByConfigKey(flattenMobileMenus(authStore.menus)))
const groups = computed(() => buildStartableGroups(objects.value, menuIndex.value, keyword.value))
const emptyTitle = computed(() => {
  if (failed.value) return '加载失败'
  return keyword.value.trim() ? '没有匹配的审批' : '暂无可发起的审批'
})
const emptyDescription = computed(() => {
  if (failed.value) return '请稍后重试。'
  return keyword.value.trim() ? '换个关键字试试。' : '如需开通请联系管理员。'
})

onLoad(async () => {
  // 没有提交审批权限时不请求目录，直接显示空状态。
  if (!canStart.value) { loading.value = false; return }
  try {
    const response = await api.getStartableObjects()
    objects.value = Array.isArray(response?.data) ? response.data : []
  }
  catch (error) {
    console.error('加载可发起审批失败:', error)
    failed.value = true
  }
  finally {
    loading.value = false
  }
})

function openItem(item) {
  uni.navigateTo({ url: item.url })
}
</script>

<style lang="scss" scoped>
.approval-start-page {
  display: flex;
  height: calc(100vh - var(--window-top, 0px));
  flex-direction: column;
  background: var(--forge-page-bg);
}

.approval-start-search {
  padding: 12px var(--forge-space-page) 0;
}

.approval-start-scroll {
  width: 100%;
  min-height: 0;
  flex: 1;
}

.approval-start-group {
  max-width: var(--forge-page-max-width);
  margin: 12px var(--forge-space-page) 0;
  padding: 16px 12px 8px;
  border-radius: var(--forge-radius-card);
  background: var(--forge-surface);
}

.approval-start-bottom {
  height: calc(24px + env(safe-area-inset-bottom));
}

.approval-start-group__title {
  display: block;
  padding: 0 4px 12px;
  color: var(--forge-text-primary);
  font-size: 16px;
  font-weight: 600;
}

.approval-start-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  row-gap: 8px;
}

.approval-start-item {
  display: flex;
  min-height: 88px;
  margin: 0;
  padding: 4px 0;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  border: 0;
  border-radius: var(--forge-radius-control);
  background: transparent;
  line-height: 1.4;
}

.approval-start-item::after {
  display: none;
}

.approval-start-item:active {
  background: var(--forge-surface-subtle);
}

.approval-start-item__label {
  overflow: hidden;
  width: 100%;
  color: var(--forge-text-primary);
  font-size: 13px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
