export const basicRoutes = [
  {
    name: 'Login',
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: {
      title: '登录页',
      layout: 'empty',
    },
  },

  {
    name: 'Home',
    path: '/',
    component: () => import('@/views/home/index.vue'),
    meta: {
      title: '首页',
    },
  },

  {
    name: '404',
    path: '/404',
    component: () => import('@/views/error-page/404.vue'),
    meta: {
      title: '页面飞走了',
      layout: 'empty',
    },
  },

  {
    name: '403',
    path: '/403',
    component: () => import('@/views/error-page/403.vue'),
    meta: {
      title: '没有权限',
      layout: 'empty',
    },
  },

  {
    name: 'iframe',
    path: '/iframe',
    component: () => import('@/views/iframe/index.vue'),
    meta: {
      title: 'iframe',
      // layout: 'full',  // 注释掉固定布局
    },
  },

  {
    name: 'NoticeList',
    path: '/system/notice-list',
    component: () => import('@/views/system/notice-list.vue'),
    meta: {
      title: '通知公告',
    },
  },

  // {
  //   name: 'MessageList',
  //   path: '/message/message-list',
  //   component: () => import('@/views/message/message-list.vue'),
  //   meta: {
  //     title: '我的消息',
  //   },
  // },

  {
    name: 'MessageTemplate',
    path: '/message/template',
    component: () => import('@/views/message/template-list.vue'),
    meta: {
      title: '消息模板管理',
    },
  },
  {
    name: 'UserProfile',
    path: '/profile',
    component: () => import('@/views/system/profile.vue'),
    meta: {
      title: '个人中心',
    },
  },
]
