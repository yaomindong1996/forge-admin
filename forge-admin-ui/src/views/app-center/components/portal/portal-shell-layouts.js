/**
 * 已发布应用门户壳布局：复用系统布局选项语义，渲染独立于后台 Layout。
 */

export const PORTAL_SHELL_LAYOUTS = Object.freeze([
  {
    value: 'business-workbench',
    label: '业务工作台',
    description: '顶部栏 + 左侧分组导航，适合多模块业务应用',
    recommended: true,
    group: 'common',
    preview: 'data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' width=\'104\' height=\'60\' viewBox=\'0 0 104 60\'%3E%3Crect width=\'104\' height=\'10\' fill=\'%234242F7\'/%3E%3Crect y=\'12\' width=\'22\' height=\'48\' fill=\'%23EEF2FF\'/%3E%3Crect x=\'24\' y=\'12\' width=\'80\' height=\'48\' fill=\'%23F8FAFC\'/%3E%3C/svg%3E',
  },
  {
    value: 'side-flyout',
    label: '侧栏弹出',
    description: '左侧一级菜单，点击后侧出分组面板',
    group: 'common',
    preview: 'data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' width=\'104\' height=\'60\' viewBox=\'0 0 104 60\'%3E%3Crect width=\'12\' height=\'60\' fill=\'%234242F7\'/%3E%3Crect x=\'14\' width=\'22\' height=\'60\' fill=\'%23EEF2FF\'/%3E%3Crect x=\'38\' width=\'66\' height=\'10\' fill=\'%234242F7\'/%3E%3Crect x=\'38\' y=\'12\' width=\'66\' height=\'48\' fill=\'%23F8FAFC\'/%3E%3C/svg%3E',
  },
  {
    value: 'normal',
    label: '通用',
    description: '经典左侧导航 + 顶栏，结构稳定',
    group: 'common',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIyMCIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjI0IiB3aWR0aD0iODAiIGhlaWdodD0iMTAiIGZpbGw9IiNGNUY1RjUiLz48cmVjdCB4PSIyNCIgeT0iMTQiIHdpZHRoPSI4MCIgaGVpZ2h0PSI0NiIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg==',
  },
  {
    value: 'simple',
    label: '简约',
    description: '轻量侧栏，内容区更紧凑',
    group: 'common',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIyMCIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjI0IiB3aWR0aD0iODAiIGhlaWdodD0iNjAiIGZpbGw9IiNGNUY1RjUiLz48L3N2Zz4=',
  },
  {
    value: 'top-side-menu',
    label: '顶部加侧面',
    description: '顶栏品牌区 + 左侧页面树',
    group: 'common',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMDQiIGhlaWdodD0iMTAiIGZpbGw9IiNFMEUwRTAiLz48cmVjdCB5PSIxMiIgd2lkdGg9IjIwIiBoZWlnaHQ9IjQ4IiBmaWxsPSIjRTBFMEUwIi8+PHJlY3QgeD0iMjIiIHk9IjEyIiB3aWR0aD0iODIiIGhlaWdodD0iNDgiIGZpbGw9IiNGNUY1RjUiLz48L3N2Zz4=',
  },
  {
    value: 'top-menu',
    label: '顶部菜单',
    description: '一级导航放顶部，最大化内容宽度',
    group: 'common',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMDQiIGhlaWdodD0iMTAiIGZpbGw9IiNFMEUwRTAiLz48cmVjdCB5PSIxMiIgd2lkdGg9IjEwNCIgaGVpZ2h0PSI0OCIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg==',
  },
  {
    value: 'full',
    label: '全屏',
    description: '弱化外框，内容区铺满',
    group: 'common',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIyMCIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjI0IiB3aWR0aD0iODAiIGhlaWdodD0iNiIgZmlsbD0iI0Y1RjVGNSIvPjxyZWN0IHg9IjI0IiB5PSIxMCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjQiIGZpbGw9IiNGNUY1RjUiLz48cmVjdCB4PSIyNCIgeT0iMTgiIHdpZHRoPSI4MCIgaGVpZ2h0PSI0MiIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg==',
  },
  {
    value: 'immersive',
    label: '沉浸式',
    description: '无常驻侧栏，抽屉菜单打开导航',
    group: 'advanced',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMDQiIGhlaWdodD0iOCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHk9IjEyIiB3aWR0aD0iMTA0IiBoZWlnaHQ9IjQiIGZpbGw9IiNGNUY1RjUiLz48cmVjdCB5PSIyMCIgd2lkdGg9IjEwNCIgaGVpZ2h0PSI0MCIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg==',
  },
  {
    value: 'bento',
    label: '便当盒',
    description: '超窄图标轨，内容区更大',
    group: 'advanced',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMiIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjE2IiB3aWR0aD0iODgiIGhlaWdodD0iOCIgZmlsbD0iI0Y1RjVGNSIvPjxyZWN0IHg9IjE2IiB5PSIxMiIgd2lkdGg9Ijg4IiBoZWlnaHQ9IjQ4IiBmaWxsPSIjRjVGNUY1Ii8+PC9zdmc+',
  },
  {
    value: 'nexus',
    label: 'Nexus 浮岛',
    description: '浮岛卡片式侧栏与内容区',
    group: 'advanced',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHg9IjMiIHk9IjMiIHdpZHRoPSIxNiIgaGVpZ2h0PSI1NCIgZmlsbD0iI0YwRjBGMCIgc3Ryb2tlPSIjRDVENTVFIiByeD0iMiIvPjxyZWN0IHg9IjIyIiB5PSIzIiB3aWR0aD0iNzkiIGhlaWdodD0iOCIgZmlsbD0iI0YwRjBGMCIgc3Ryb2tlPSIjRDVENTVFIiByeD0iMiIvPjxyZWN0IHg9IjIyIiB5PSIxNCIgd2lkdGg9Ijc5IiBoZWlnaHQ9IjQzIiBmaWxsPSIjRjBGMEYwIiBzdHJva2U9IiNENUQ1RTUiIHJ4PSIyIi8+PC9zdmc+',
  },
])

export const PORTAL_SHELL_LAYOUT_VALUES = Object.freeze(PORTAL_SHELL_LAYOUTS.map(item => item.value))

const LEGACY_STYLE_TO_SHELL = Object.freeze({
  side: 'normal',
  top: 'top-menu',
  collapsed: 'bento',
})

/** 从 portalConfig 解析壳布局（兼容旧 navigation.style） */
export function resolvePortalShellLayout(portalConfig = {}) {
  const raw = String(portalConfig?.shellLayout || '').trim()
  if (PORTAL_SHELL_LAYOUT_VALUES.includes(raw))
    return raw
  const legacy = String(portalConfig?.navigation?.style || 'side').trim()
  return LEGACY_STYLE_TO_SHELL[legacy] || 'normal'
}

/**
 * 壳布局 → 门户渲染参数
 * @returns {{
 *   shellLayout: string,
 *   navigationStyle: 'side'|'top'|'collapsed',
 *   showTopNav: boolean,
 *   showPersistentSidebar: boolean,
 *   showDrawerToggle: boolean,
 *   defaultCollapsed: boolean,
 *   shellClass: string,
 * }}
 */
export function resolvePortalShellChrome(portalConfig = {}) {
  const shellLayout = resolvePortalShellLayout(portalConfig)
  const navigation = portalConfig?.navigation || {}

  if (shellLayout === 'top-menu') {
    return {
      shellLayout,
      navigationStyle: 'top',
      showTopNav: true,
      showPersistentSidebar: false,
      showDrawerToggle: false,
      defaultCollapsed: false,
      shellClass: 'shell-top-menu',
    }
  }

  if (shellLayout === 'immersive') {
    return {
      shellLayout,
      navigationStyle: 'side',
      showTopNav: false,
      showPersistentSidebar: false,
      showDrawerToggle: true,
      defaultCollapsed: false,
      shellClass: 'shell-immersive',
    }
  }

  if (shellLayout === 'bento') {
    return {
      shellLayout,
      navigationStyle: 'collapsed',
      showTopNav: false,
      showPersistentSidebar: true,
      showDrawerToggle: Boolean(navigation.collapsible),
      defaultCollapsed: true,
      shellClass: 'shell-bento',
    }
  }

  if (shellLayout === 'business-workbench' || shellLayout === 'top-side-menu' || shellLayout === 'side-flyout') {
    return {
      shellLayout,
      navigationStyle: 'side',
      showTopNav: false,
      showPersistentSidebar: true,
      showDrawerToggle: Boolean(navigation.collapsible),
      defaultCollapsed: navigation.collapsed === true,
      shellClass: shellLayout === 'business-workbench'
        ? 'shell-workbench'
        : shellLayout === 'side-flyout' ? 'shell-side-flyout' : 'shell-top-side',
    }
  }

  if (shellLayout === 'nexus') {
    return {
      shellLayout,
      navigationStyle: 'side',
      showTopNav: false,
      showPersistentSidebar: true,
      showDrawerToggle: Boolean(navigation.collapsible),
      defaultCollapsed: navigation.collapsed === true,
      shellClass: 'shell-nexus',
    }
  }

  // normal / simple / full
  return {
    shellLayout,
    navigationStyle: navigation.collapsed === true ? 'collapsed' : 'side',
    showTopNav: false,
    showPersistentSidebar: true,
    showDrawerToggle: Boolean(navigation.collapsible !== false),
    defaultCollapsed: navigation.collapsed === true,
    shellClass: `shell-${shellLayout}`,
  }
}

export function buildPortalShellLayoutGroups() {
  const common = PORTAL_SHELL_LAYOUTS.filter(item => item.group === 'common')
  const advanced = PORTAL_SHELL_LAYOUTS.filter(item => item.group === 'advanced')
  return [
    {
      key: 'common',
      title: '常用布局',
      description: '与系统后台布局语义一致，适合大多数业务应用。',
      options: common,
    },
    {
      key: 'advanced',
      title: '扩展布局',
      description: '更大内容区或更现代的导航形态。',
      options: advanced,
    },
  ].filter(group => group.options.length)
}

/** 写入配置时同步 shellLayout 与旧 navigation.style，避免发布端读错 */
export function syncPortalShellNavigationFields(portal = {}) {
  const shellLayout = resolvePortalShellLayout(portal)
  const chrome = resolvePortalShellChrome({ ...portal, shellLayout })
  const style = chrome.navigationStyle === 'top'
    ? 'top'
    : (chrome.defaultCollapsed || chrome.navigationStyle === 'collapsed' ? 'collapsed' : 'side')
  return {
    ...portal,
    shellLayout,
    navigation: {
      ...(portal.navigation || {}),
      style,
    },
  }
}
