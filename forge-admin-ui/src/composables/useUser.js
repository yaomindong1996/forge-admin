/**
 * User-related composable for layout components
 * Encapsulates user info, avatar, dropdown options, and logout logic
 */
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore, useAuthStore } from '@/store'
import { defaultThemeConfig } from '@/config/theme.config.js'
import api from '@/api'

/**
 * Default user dropdown options
 */
const DEFAULT_DROPDOWN_OPTIONS = [
  {
    label: '个人资料',
    key: 'profile',
    icon: () => h('i', { class: 'i-material-symbols:person-outline text-14' }),
  },
  {
    type: 'divider',
    key: 'd1',
  },
  {
    label: '退出登录',
    key: 'logout',
    icon: () => h('i', { class: 'i-mdi:exit-to-app text-14' }),
  },
]

/**
 * Composable for user data and authentication
 * @returns {object} User composable API
 */
export function useUser() {
  const router = useRouter()
  const userStore = useUserStore()
  const authStore = useAuthStore()

  /**
   * User's display name
   */
  const userName = computed(() => {
    return userStore.userInfo?.realName || userStore.userInfo?.username || userStore.staffInfo?.staffName || 'User'
  })

  /**
   * Avatar text (first letter of name) for avatar fallback
   */
  const userAvatarText = computed(() => {
    const name = userName.value
    return name.charAt(0).toUpperCase()
  })

  /**
   * User's avatar URL or null if using default
   */
  const userAvatar = computed(() => {
    return userStore.avatar || null
  })

  /**
   * User dropdown menu options
   */
  const userDropdownOptions = computed(() => DEFAULT_DROPDOWN_OPTIONS)

  /**
   * Dropdown visible state (for manual control)
   */
  const dropdownVisible = ref(false)

  /**
   * Navigate to user profile page
   */
  function goToProfile() {
    router.push('/profile')
    dropdownVisible.value = false
  }

  /**
   * Navigate to password change page
   */
  function goToPassword() {
    router.push('/system/password')
    dropdownVisible.value = false
  }

  /**
   * Handle logout with confirmation dialog
   */
  function handleLogout() {
    $dialog.confirm({
      title: '提示',
      type: 'info',
      content: '确认退出？',
      'positive-button-props': {
        color: defaultThemeConfig.primaryColor,
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
    dropdownVisible.value = false
  }

  /**
   * Unified handler for dropdown menu selection
   * @param {string} key - Selected dropdown option key
   */
  function handleDropdownSelect(key) {
    dropdownVisible.value = false
    switch (key) {
      case 'profile':
        goToProfile()
        break
      case 'password':
        goToPassword()
        break
      case 'logout':
        handleLogout()
        break
    }
  }

  return {
    userName,
    userAvatarText,
    userAvatar,
    userDropdownOptions,
    dropdownVisible,
    goToProfile,
    goToPassword,
    handleLogout,
    handleDropdownSelect,
  }
}
