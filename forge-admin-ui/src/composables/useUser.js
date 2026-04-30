/**
 * 布局组件用户相关组合式函数
 * 封装用户信息、头像、下拉菜单和退出登录逻辑
 */
import { computed, h, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { defaultThemeConfig } from '@/config/theme.config.js'
import { useAuthStore, useUserStore } from '@/store'
import { getFileUrl } from '@/utils/file'

/**
 * 默认用户下拉菜单选项
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
 * 用户数据和认证组合式函数
 * @returns {object} 用户组合式API
 */
export function useUser() {
  const router = useRouter()
  const userStore = useUserStore()
  const authStore = useAuthStore()

  /**
   * 用户显示名称
   */
  const userName = computed(() => {
    return userStore.userInfo?.realName || userStore.userInfo?.username || userStore.staffInfo?.staffName || 'User'
  })

  /**
   * 头像文字（姓名首字母）用于头像回退
   */
  const userAvatarText = computed(() => {
    const name = userName.value
    return name.charAt(0).toUpperCase()
  })

  /**
   * 用户头像URL（通过fetch下载，返回blob URL）
   */
  const userAvatar = ref('')

  async function loadAvatar() {
    const avatar = userStore.avatar
    if (!avatar) {
      userAvatar.value = ''
      return
    }
    try {
      const url = getFileUrl(avatar)
      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${authStore.accessToken}` },
      })
      if (response.ok) {
        const blob = await response.blob()
        userAvatar.value = URL.createObjectURL(blob)
      }
      else {
        userAvatar.value = ''
      }
    }
    catch {
      userAvatar.value = ''
    }
  }

  watch(() => userStore.avatar, () => {
    loadAvatar()
  }, { immediate: true })

  /**
   * 用户下拉菜单选项
   */
  const userDropdownOptions = computed(() => DEFAULT_DROPDOWN_OPTIONS)

  /**
   * 下拉菜单可见性状态
   */
  const dropdownVisible = ref(false)

  /**
   * 跳转到个人资料页
   */
  function goToProfile() {
    router.push('/profile')
    dropdownVisible.value = false
  }

  /**
   * 跳转到修改密码页
   */
  function goToPassword() {
    router.push('/system/password')
    dropdownVisible.value = false
  }

  /**
   * 退出登录，带确认弹窗
   */
  function handleLogout() {
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
    dropdownVisible.value = false
  }

  /**
   * 下拉菜单选择统一处理函数
   * @param {string} key - 选中的下拉选项key
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
