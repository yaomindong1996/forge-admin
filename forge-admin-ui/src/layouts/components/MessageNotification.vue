<template>
  <div class="message-notification-wrapper">
    <n-popover
      v-model:show="showPopover"
      trigger="click"
      placement="bottom-end"
      :show-arrow="false"
      style="padding: 0"
    >
      <template #trigger>
        <n-badge
          :value="unreadCount"
          :max="99"
          :show="unreadCount > 0"
          :offset="[-5, 5]"
        >
          <div class="flex cursor-pointer items-center justify-center w-36 h-36 hover:bg-gray-100 rounded">
            <i class="i-material-symbols:notifications-outline text-22" />
          </div>
        </n-badge>
      </template>

      <div class="message-notification-popup" style="width: 300px; max-height: 400px; overflow-y: auto">
        <!-- 消息列表 -->
        <div v-if="messages.length > 0" class="message-list">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-item px-16 py-12 hover:bg-gray-50 cursor-pointer flex items-center justify-between"
            @click="handleMessageClick(msg.id)"
          >
            <span class="flex-1 truncate">{{ msg.title }}</span>
            <span v-if="msg.readFlag === 0" class="w-8 h-8 bg-red-500 rounded-full ml-8"></span>
          </div>
        </div>
        <div v-else class="px-16 py-12 text-gray-400 text-center">
          暂无消息
        </div>

        <!-- 分割线 -->
        <n-divider style="margin: 0" />

        <!-- 操作按钮 -->
        <div class="action-list">
          <div
            class="action-item px-16 py-12 hover:bg-gray-50 cursor-pointer flex items-center"
            @click="handleViewAll"
          >
            <i class="i-material-symbols:mail-outline mr-8" />
            <span>查看全部消息</span>
          </div>
          <div
            v-if="unreadCount > 0"
            class="action-item px-16 py-12 hover:bg-gray-50 cursor-pointer flex items-center"
            @click="handleMarkAllRead"
          >
            <i class="i-material-symbols:mark-email-read-outline mr-8" />
            <span>全部标记为已读</span>
          </div>
        </div>
      </div>
    </n-popover>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NBadge, NPopover, NDivider } from 'naive-ui'
import { request } from '@/utils'
import messageApi from '@/api/message'

const router = useRouter()
const unreadCount = ref(0)
const messages = ref([])
const showPopover = ref(false)
const bizTypeOptions = ref([])

// 获取未读消息数量
async function fetchUnreadCount() {
  try {
    const res = await request.get('/api/message/unread/count')
    console.log('[MessageNotification] 未读消息数量响应:', res)
    if (res.code === 200 && res.data) {
      // 后端返回的是 UnreadCountVO 对象，包含 totalCount 字段
      unreadCount.value = res.data.totalCount || 0
      console.log('[MessageNotification] 未读消息数量:', unreadCount.value)
    }
  } catch (error) {
    console.error('获取未读消息数失败:', error)
  }
}

// 获取最新消息列表
async function fetchLatestMessages() {
  try {
    const res = await request.post('/api/message/page?pageNum=1&pageSize=5', {})
    console.log('[MessageNotification] 获取消息列表响应:', res)
    if (res.code === 200 && res.data) {
      messages.value = res.data.list || res.data.records || []
      console.log('[MessageNotification] 消息列表数据:', messages.value)
    }
  } catch (error) {
    console.error('获取最新消息失败:', error)
  }
}

// 加载业务类型配置
async function loadBizTypes() {
  try {
    const res = await messageApi.getBizTypeListEnabled()
    if (res.code === 200 && res.data) {
      bizTypeOptions.value = res.data
    }
  } catch (error) {
    console.error('加载业务类型失败:', error)
  }
}

// 处理消息点击
async function handleMessageClick(messageId) {
  console.log('[MessageNotification] 点击消息:', messageId)
  showPopover.value = false  // 关闭弹窗
  try {
    // 标记为已读
    await request.post(`/api/message/${messageId}/read`)
    
    // 获取消息详情以获取业务类型和业务主键
    const detailRes = await request.get(`/api/message/${messageId}`)
    if (detailRes.code === 200 && detailRes.data) {
      const message = detailRes.data
      
      // 如果有业务关联，跳转到业务页面
      if (message.bizType && message.bizKey) {
        const bizConfig = bizTypeOptions.value.find(opt => opt.bizType === message.bizType)
        if (bizConfig && bizConfig.jumpUrl) {
          let jumpUrl = bizConfig.jumpUrl
          jumpUrl = jumpUrl.replace('${bizKey}', message.bizKey)
          jumpUrl = jumpUrl.replace('${messageId}', message.id)
          
          if (bizConfig.jumpTarget === '_blank') {
            window.open(jumpUrl, '_blank')
          } else {
            router.push(jumpUrl)
          }
          
          // 刷新数据
          fetchUnreadCount()
          fetchLatestMessages()
          return
        }
      }
    }
    
    // 如果没有业务关联，跳转到消息列表
    router.push('/message/message-list')
    // 刷新数据
    fetchUnreadCount()
    fetchLatestMessages()
  } catch (error) {
    console.error('处理消息点击失败:', error)
  }
}

// 查看全部消息
function handleViewAll() {
  console.log('[MessageNotification] 查看全部消息')
  showPopover.value = false  // 关闭弹窗
  router.push('/message/message-list')
}

// 全部标记为已读
async function handleMarkAllRead() {
  console.log('[MessageNotification] 全部标记为已读')
  try {
    await request.post('/api/message/read/all')
    window.$message.success('已全部标记为已读')
    fetchUnreadCount()
    fetchLatestMessages()
  } catch (error) {
    window.$message.error('操作失败')
  }
}

// 初始化数据
function initData() {
  fetchUnreadCount()
  fetchLatestMessages()
  loadBizTypes()
}

onMounted(() => {
  initData()
})

// 暴露刷新方法，供外部调用
defineExpose({
  refresh: () => {
    fetchUnreadCount()
    fetchLatestMessages()
  }
})
</script>

<style scoped>
.message-notification-wrapper {
  margin-right: 8px;
}

:deep(.n-badge-sup) {
  right: 2px;
  top: 2px;
  font-size: 12px;
  font-weight: 600;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background-color: #f5222d;
  color: #fff;
  box-shadow: 0 0 0 2px #fff;
}

.message-notification-popup {
  background: white;
}

.message-item {
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.message-item:last-child {
  border-bottom: none;
}

.action-item {
  transition: background-color 0.2s;
}
</style>
