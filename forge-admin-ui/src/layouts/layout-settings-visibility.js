export function canConfigureLayout({ routeLayout, authenticated }) {
  // 路由专用的空壳用于登录/错误页，不能与用户主动选择空白布局混淆。
  return Boolean(authenticated) && !['empty', 'app-portal'].includes(routeLayout)
}
