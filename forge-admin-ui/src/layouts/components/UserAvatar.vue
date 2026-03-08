<template>
  <n-dropdown :options="options" @select="handleSelect">
    <div id="user-dropdown" class="flex cursor-pointer items-center">
      <n-avatar round :size="28" :src="userStore.avatar || defaultAvatar" />
      <div v-if="userStore.userInfo || userStore.staffInfo" class="ml-8 flex-col flex-shrink-0 items-center">
        <span class="text-14">{{ userStore.realName || userStore.staffInfo?.staffName }}</span>
      </div>
    </div>
  </n-dropdown>
</template>

<script setup>
import api from '@/api'
import { useAuthStore, usePermissionStore, useUserStore } from '@/store'
import {defaultThemeConfig} from "@/config/theme.config.js"
import defaultAvatar from '@/assets/images/avatar.png'

const router = useRouter()
const userStore = useUserStore()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()

const options = reactive([
  {
    label: '个人资料',
    key: 'profile',
    icon: () => h('i', { class: 'i-material-symbols:person-outline text-14' }),
  },
  {
    label: '退出登录',
    key: 'logout',
    icon: () => h('i', { class: 'i-mdi:exit-to-app text-14' }),
  },
])

const roleSelectRef = ref(null)
function handleSelect(key) {
  switch (key) {
    case 'profile':
      router.push('/profile')
      break
    case 'toggleRole':
      break
    case 'logout':
      $dialog.confirm({
        title: '提示',
        type: 'info',
        content: '确认退出？',
        'positive-button-props':{
          color:defaultThemeConfig.primaryColor
        },
        async confirm() {
          try {
            await api.logout()
          }
          catch (error) {
            console.error(error)
          }
          authStore.logout()
          $message.success('已退出登录')
        },
      })
      break
  }
}
</script>
