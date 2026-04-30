<template>
  <n-dropdown :options="options" @select="handleSelect">
    <div id="user-dropdown" class="flex cursor-pointer items-center">
      <n-avatar
        v-if="avatarSrc"
        round
        :size="28"
        :src="avatarSrc"
        @error="handleAvatarError"
      />
      <n-avatar
        v-else
        round
        :size="28"
        :style="{ backgroundColor: 'var(--primary-500)', fontSize: '12px' }"
      >
        {{ avatarText }}
      </n-avatar>
      <div v-if="userStore.userInfo || userStore.staffInfo" class="ml-8 flex-col flex-shrink-0 items-center">
        <span class="text-14">{{ userStore.realName || userStore.staffInfo?.staffName }}</span>
      </div>
    </div>
  </n-dropdown>
</template>

<script setup>
import api from '@/api'
import { defaultThemeConfig } from '@/config/theme.config.js'
import { useAuthStore, useUserStore } from '@/store'
import { getFileUrl } from '@/utils/file'

const router = useRouter()
const userStore = useUserStore()
const authStore = useAuthStore()

const avatarSrc = ref('')
const avatarText = computed(() => {
  const name = userStore.realName || userStore.username
  return name ? name.charAt(0) : 'U'
})

async function loadAvatar() {
  const avatar = userStore.avatar
  if (!avatar) {
    avatarSrc.value = ''
    return
  }
  try {
    const url = getFileUrl(avatar)
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${authStore.accessToken}` },
    })
    if (response.ok) {
      const blob = await response.blob()
      avatarSrc.value = URL.createObjectURL(blob)
    }
    else {
      avatarSrc.value = ''
    }
  }
  catch {
    avatarSrc.value = ''
  }
}

function handleAvatarError() {
  avatarSrc.value = ''
}

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

function handleSelect(key) {
  switch (key) {
    case 'profile':
      router.push('/profile')
      break
    case 'toggleRole':
      break
    case 'logout':
      $dialog.confirm({
        'title': '提示',
        'type': 'info',
        'content': '确认退出？',
        'positive-button-props': {
          color: defaultThemeConfig.primaryColor,
        },
        async confirm() {
          try {
            await api.logout()
          }
          catch {
            console.error('logout error')
          }
          authStore.logout()
          $message.success('已退出登录')
        },
      })
      break
  }
}

watch(() => userStore.avatar, () => {
  loadAvatar()
}, { immediate: true })
</script>
