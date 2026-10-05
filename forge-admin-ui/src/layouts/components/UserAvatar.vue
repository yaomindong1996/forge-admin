<template>
  <n-dropdown trigger="click" :options="dropdownOptions" @select="handleSelect">
    <button id="user-dropdown" class="user-trigger flex items-center" type="button" aria-label="个人中心">
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
      <span v-if="userStore.userInfo || userStore.staffInfo" class="user-name ml-8 flex-col flex-shrink-0 items-center">
        <span class="text-14">{{ userStore.realName || userStore.staffInfo?.staffName }}</span>
      </span>
    </button>
  </n-dropdown>
</template>

<script setup>
import api from '@/api'
import { useAuthStore, useUserStore } from '@/store'
import { resolveRenderableFileUrl } from '@/utils/file'
import { isSilentAuthError } from '@/utils/http/helpers'

const props = defineProps({
  /**
   * 应用门户提供应用内个人资料入口，避免把用户带回系统布局。
   * 未传入时保持系统布局原有的 /profile 路由。
   */
  profileRoute: {
    type: [String, Object, Function],
    default: null,
  },
})

const router = useRouter()
const userStore = useUserStore()
const authStore = useAuthStore()

const avatarSrc = ref('')
const avatarText = computed(() => {
  const name = userStore.realName || userStore.username
  return name ? name.charAt(0) : 'U'
})

const dropdownOptions = computed(() => {
  const baseOptions = []
  baseOptions.push(
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
  )
  return baseOptions
})

async function loadAvatar(forceRefresh = false) {
  const avatar = userStore.avatar
  if (!avatar) {
    avatarSrc.value = ''
    return
  }
  try {
    avatarSrc.value = await resolveRenderableFileUrl(avatar, undefined, forceRefresh)
  }
  catch {
    avatarSrc.value = ''
  }
}

function handleAvatarError() {
  avatarSrc.value = ''
}

function handleSelect(key) {
  switch (key) {
    case 'profile':
      {
        const target = typeof props.profileRoute === 'function'
          ? props.profileRoute()
          : props.profileRoute
        if (target)
          router.push(target)
        else
          router.push('/profile')
      }
      break
    case 'logout':
      $dialog.confirm({
        'title': '提示',
        'type': 'info',
        'content': '确认退出？',
        'positive-button-props': {
          type: 'primary',
        },
        async confirm() {
          authStore.beginLogout()
          try {
            await api.logout()
          }
          catch (error) {
            if (!isSilentAuthError(error))
              console.error('logout error', error)
          }
          authStore.logout()
          $message.success('已退出登录')
        },
      })
      break
  }
}

watch(() => userStore.avatar, () => {
  loadAvatar(true)
}, { immediate: true })
</script>

<style scoped>
.user-trigger {
  border: 0;
  padding: 0 4px;
  min-height: 32px;
  background: transparent;
  color: var(--chrome-text, var(--text-primary));
  font: inherit;
  cursor: pointer;
}
</style>
