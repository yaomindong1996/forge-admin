import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { setupRouterGuards } from './guards'

// 手动定义的路由（登录页、SSO、带参数的路由等）
export const manualRoutes = [
  // 白名单页面
  {
    name: 'Login',
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录页', layout: 'empty' },
  },
  {
    name: 'SocialCallback',
    path: '/login/callback',
    component: () => import('@/views/login/callback.vue'),
    meta: { title: '社交登录回调', layout: 'empty' },
  },
  {
    name: '404',
    path: '/404',
    component: () => import('@/views/error-page/404.vue'),
    meta: { title: '页面飞走了', layout: 'empty' },
  },
  {
    name: '403',
    path: '/403',
    component: () => import('@/views/error-page/403.vue'),
    meta: { title: '没有权限', layout: 'empty' },
  },
  // 首页重定向
  {
    path: '/',
    redirect: '/home',
    meta: { title: '首页' },
  },
  // iframe 页面
  {
    name: 'iframe',
    path: '/iframe',
    component: () => import('@/views/iframe/index.vue'),
    meta: { title: 'iframe' },
  },
  // 通知公告
  {
    name: 'NoticeList',
    path: '/system/notice-list',
    component: () => import('@/views/system/notice-list.vue'),
    meta: { title: '通知公告' },
  },
  // 消息模板管理
  {
    name: 'MessageTemplate',
    path: '/message/template',
    component: () => import('@/views/message/template-list.vue'),
    meta: { title: '消息模板管理' },
  },
  // 个人中心
  {
    name: 'UserProfile',
    path: '/profile',
    component: () => import('@/views/system/profile.vue'),
    meta: { title: '个人中心' },
  },
]

// 从 unplugin-vue-router 自动生成的路由
// eslint-disable-next-line
import { routes as autoRoutes } from 'vue-router/auto-routes'

// 开发环境打印所有路由
if (import.meta.env.DEV) {
  console.log('📋 所有注册的路由:', autoRoutes)
}

// 合并路由：手动路由 + 自动生成路由 + 兜底路由
const routes = [
  ...manualRoutes,
  ...autoRoutes,
  // 兜底路由
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

export const router = createRouter({
  history:
      import.meta.env.VITE_USE_HASH === 'true'
        ? createWebHashHistory(import.meta.env.VITE_PUBLIC_PATH || '/')
        : createWebHistory(import.meta.env.VITE_PUBLIC_PATH || '/'),
  routes,
  scrollBehavior: () => ({ left: 0, top: 0 }),
})

export async function setupRouter(app) {
  app.use(router)
  setupRouterGuards(router)
}
