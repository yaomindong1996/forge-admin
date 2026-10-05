import { generate } from '@arco-design/color'
import { useDark } from '@vueuse/core'
import { defineStore } from 'pinia'
import { applyThemeConfig, defaultThemeConfig } from '@/config/theme.config'
import { defaultLayout, defaultPrimaryColor, naiveThemeOverrides, normalizeLayout } from '@/settings'
import { solidColor } from '@/utils/navigation-theme'
import { getDefaultPageTitle } from '@/utils/page-title'
import { readTenantAppearance } from '@/utils/tenant-appearance'

function cloneConfig(config) {
  return JSON.parse(JSON.stringify(config))
}

export const useAppStore = defineStore('app', {
  state: () => ({
    collapsed: false,
    appearanceOpen: false, // 跨布局切换时保留设置面板，不写入会话缓存。
    isDark: useDark(),
    layout: normalizeLayout(import.meta.env.VITE_DEFAULT_LAYOUT || defaultLayout),
    primaryColor: defaultPrimaryColor,
    naiveThemeOverrides: cloneConfig(naiveThemeOverrides),
    selectedTopMenuId: null, // 当前选中的顶部菜单ID
    themeConfig: cloneConfig(defaultThemeConfig), // 主题配置
    routeGuardCompleted: null,
  }),
  actions: {
    switchCollapsed() {
      this.collapsed = !this.collapsed
    },
    setCollapsed(b) {
      this.collapsed = b
    },
    toggleDark() {
      this.isDark = !this.isDark
    },
    setLayout(v) {
      this.layout = normalizeLayout(v)
    },
    setPrimaryColor(color) {
      this.primaryColor = color
    },
    setThemeColor(color = this.primaryColor, isDark = this.isDark) {
      document.body.style.setProperty('--primary-color', color)
      const colors = generate(color, {
        list: true,
        dark: isDark,
      })
      this.naiveThemeOverrides.common = {
        ...this.naiveThemeOverrides.common,
        primaryColor: colors[5],
        primaryColorHover: colors[4],
        primaryColorSuppl: colors[4],
        primaryColorPressed: colors[6],
      }
    },
    setSelectedTopMenuId(id) {
      this.selectedTopMenuId = id
    },
    restoreTenantAppearance(config) {
      // 仅恢复本会话外观，不调用租户切换，不触碰权限或保存配置。
      this.setLayout(config?.systemLayout || defaultLayout)
      this.themeConfig = readTenantAppearance(config)
      this.setThemeConfig(this.themeConfig)
    },
    restoreSystemTheme() {
      this.themeConfig = cloneConfig(defaultThemeConfig)
      this.setThemeConfig(this.themeConfig)
    },
    resetAccountState() {
      this.appearanceOpen = false
      this.layout = normalizeLayout(import.meta.env.VITE_DEFAULT_LAYOUT || defaultLayout)
      this.primaryColor = defaultPrimaryColor
      this.naiveThemeOverrides = cloneConfig(naiveThemeOverrides)
      this.themeConfig = cloneConfig(defaultThemeConfig)
      this.selectedTopMenuId = null
      this.setThemeColor(defaultPrimaryColor)
      applyThemeConfig(this.themeConfig, this.isDark)
      document.title = getDefaultPageTitle()
    },
    setThemeConfig(config) {
      this.themeConfig = { ...this.themeConfig, ...config }
      // 同步外层的 primaryColor 和 themeConfig.primaryColor
      if (config.primaryColor) {
        const primary = solidColor(config.primaryColor, defaultPrimaryColor)
        this.themeConfig.primaryColor = primary
        this.primaryColor = primary
        this.setThemeColor(primary)
      }
      applyThemeConfig(this.themeConfig, this.isDark)
    },
    updateHeaderConfig(headerConfig) {
      this.themeConfig.header = { ...this.themeConfig.header, ...headerConfig }
      applyThemeConfig(this.themeConfig, this.isDark)
    },
    updateTopMenuConfig(topMenuConfig) {
      this.themeConfig.topMenu = { ...this.themeConfig.topMenu, ...topMenuConfig }
      applyThemeConfig(this.themeConfig, this.isDark)
    },
    updateSideMenuConfig(sideMenuConfig) {
      this.themeConfig.sideMenu = { ...this.themeConfig.sideMenu, ...sideMenuConfig }
      applyThemeConfig(this.themeConfig, this.isDark)
    },
    applyCurrentTheme() {
      applyThemeConfig(this.themeConfig, this.isDark)
    },
    // 添加设置路由守卫完成状态的方法
    setRouteGuardCompleted(completed) {
      this.routeGuardCompleted = completed
    },
    updateNaiveThemeOverrides(common) {
      this.naiveThemeOverrides.common = {
        ...this.naiveThemeOverrides.common,
        ...common,
      }
    },
  },
  persist: {
    key: `${import.meta.env.VITE_TENANT || 'default'}_app`,
    pick: ['collapsed', 'layout', 'primaryColor', 'naiveThemeOverrides', 'themeConfig'],
    storage: sessionStorage,
  },
})
