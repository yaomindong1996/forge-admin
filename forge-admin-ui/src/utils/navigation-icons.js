import { resolveForgeSymbol } from '@/components/common/forgeSymbols'

// 仅匹配已知导航语义，不由菜单顺序/数据库 ID 决定图标，也不覆盖真实路由。
const navigationSymbols = [
  { name: 'plugins', paths: ['/system/plugin'], labels: ['插件中心', '插件市场'] },
  { name: 'print', paths: ['/print'], labels: ['打印中心'] },
  { name: 'home', paths: ['/home', '/workbench'], labels: ['首页', '工作台'] },
  { name: 'apps', paths: ['/app-center'], labels: ['应用中心', '应用总览'] },
  { name: 'builder', paths: ['/generator', '/lowcode'], labels: ['业务搭建', '低代码开发', '开发中心'] },
  { name: 'workflow', paths: ['/flow'], labels: ['流程中心', '审批中心'] },
  { name: 'analytics', paths: ['/data-report', '/data', '/report'], labels: ['数据与报表', '数据中心', '报表中心'] },
  { name: 'ai', paths: ['/ai'], labels: ['AI能力', 'AI中心', '智能中心'] },
  { name: 'collaboration', paths: ['/system/collaboration', '/collaboration'], labels: ['企业协同'] },
  { name: 'open', paths: ['/open-platform', '/external'], labels: ['开放平台'] },
  { name: 'platform', paths: ['/platform', '/system'], labels: ['平台管理', '系统管理'] },
  { name: 'monitor', paths: ['/monitor'], labels: ['运维监控', '系统监控'] },
  { name: 'message', paths: ['/message'], labels: ['消息中心'] },
  { name: 'schedule', paths: ['/job'], labels: ['任务调度', '定时任务'] },
]

export function resolveNavigationIcon(menu) {
  const configured = String(menu?.icon || '')
  if (configured.startsWith('forge:'))
    return `forge:${resolveForgeSymbol(configured.slice(6))}`
  const label = String(menu?.label || menu?.name || menu?.resourceName || '').replace(/\s/g, '')
  const path = `/${String(menu?.path || '').replace(/^\/+/, '').split(/[?#]/)[0]}`
  const match = navigationSymbols.find(item => item.labels.includes(label))
    || navigationSymbols.find(item => item.paths.some(base => (
      path === base || path.startsWith(`${base}/`)
    )))
  return `forge:${match?.name || 'module'}`
}

/** 只适配显示层的一级菜单；保留原 API 图标与所有子级，避免改动菜单编辑/权限/导航含义。 */
export function withUnifiedNavigationIcons(menus) {
  return (menus || []).map(menu => ({
    ...menu,
    ...(menu.type === 'subapp'
      ? { children: withUnifiedNavigationIcons(menu.children) }
      : { icon: resolveNavigationIcon(menu) }),
  }))
}
