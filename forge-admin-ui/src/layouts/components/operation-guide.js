export function createOperationGuide(layout) {
  const compact = ['simple', 'bento'].includes(layout)
  const drawer = ['immersive', 'bento'].includes(layout)
  const menuContent = navigationDescription(layout, drawer)
  return [
    { key: 'navigation', icon: 'i-lucide:panel-left', title: '找到业务入口', content: menuContent },
    {
      key: 'notification',
      icon: 'i-lucide:bell',
      title: '查看通知与待办',
      content: `点击${compact ? '侧栏底部（窄屏为顶部）' : '顶栏'}的铃铛打开通知中心，筛选个人消息、审批提醒和公告。`,
    },
    {
      key: 'account',
      icon: 'i-lucide:circle-user-round',
      title: '管理账户与工具',
      content: compact
        ? '点击侧栏底部的账户图标，查看个人资料、主题和系统工具。窄屏入口位于顶部。'
        : '点击右上角头像查看个人资料；租户和组织切换只展示当前账号有权使用的选项。',
    },
    {
      key: 'appearance',
      icon: 'i-lucide:panels-top-left',
      title: '调整布局与配色',
      content: '在布局与外观中选择布局和配色方案，文字与图标自动适配。这里仅调整个人会话，不保存到租户。',
    },
  ]
}

function navigationDescription(layout, drawer) {
  if (drawer) {
    return '点击菜单图标打开完整导航，可搜索名称并直接进入已授权的业务页面。'
  }
  if (['business-workbench', 'top-menu', 'top-side-menu'].includes(layout)) {
    return '从顶部导航选择业务模块，再展开子菜单进入页面；窄屏下点击菜单图标打开完整导航。'
  }
  return '从导航选择业务页面；点击菜单图标可收起侧栏，窄屏下会打开菜单面板。'
}
