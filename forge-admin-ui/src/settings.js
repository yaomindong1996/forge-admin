// 默认权限
export const basePermissions = [
  {
    path: '/403',
    component: '/src/views/error-page/403.vue',
    meta: {
      title: '无权限',
      layout: 'empty',
    },
  },
  {
    path: '/404',
    component: '/src/views/error-page/404.vue',
    meta: {
      title: '页面找不到',
      layout: 'empty',
    },
  },
]

// 默认设置
export const defaultLayout = 'business-workbench'
export const defaultPrimaryColor = '#4242F7'
export const naiveThemeOverrides = {
  common: {
    primaryColor: '#4242F7',
    primaryColorHover: '#5388ff',
    primaryColorSuppl: '#5388ff',
    primaryColorPressed: '#2058f5',
    borderRadius: '3px',
  },
  Button: {
    borderRadiusTiny: '3px',
    borderRadiusSmall: '3px',
    borderRadiusMedium: '3px',
    borderRadiusLarge: '4px',
    fontWeight: '500',
  },
}

// 控制是否显示布局设置按钮
export const layoutSettingVisible = true

// 布局配置
export const layoutSettings = {
  // 默认布局
  defaultLayout,

  // 可用布局列表
  layouts: [
    {
      name: 'business-workbench',
      title: '业务工作台',
      description: '顶部导航与分组菜单，适合多模块业务后台',
    },
    {
      name: 'normal',
      title: '默认布局',
      description: '侧边栏菜单布局',
    },
    {
      name: 'top-menu',
      title: '顶部菜单布局',
      description: '菜单集中在顶部，通过下拉面板展开子菜单',
    },
    {
      name: 'top-side-menu',
      title: '顶部加侧面菜单布局',
      description: '一级菜单在顶部，二级及以下菜单在左侧侧边栏',
    },
    {
      name: 'dropdown-menu',
      title: '下拉菜单布局',
      description: '顶部菜单可下拉选择子级菜单',
    },
    {
      name: 'full',
      title: '全屏布局',
      description: '无边框全屏布局',
    },
    {
      name: 'simple',
      title: '简洁布局',
      description: '简单布局，仅包含基本元素',
    },
    {
      name: 'immersive',
      title: '沉浸式布局',
      description: '无侧边栏，抽屉式菜单，最大化内容展示区域',
    },
    {
      name: 'bento',
      title: '便当盒布局',
      description: '超窄图标导航栏，右侧抽屉菜单，极致内容空间',
    },
    {
      name: 'nexus',
      title: 'Nexus 浮岛布局',
      description: '浮岛卡片式侧边栏与内容区，现代商务风格',
    },
    {
      name: 'empty',
      title: '空布局',
      description: '空布局，不包含任何框架元素',
    },
    {
      name: 'app-portal',
      title: '应用门户布局',
      description: '已发布低代码应用的独立运行布局',
    },
  ],
}

export const validLayoutNames = layoutSettings.layouts.map(item => item.name)

export function normalizeLayout(layout) {
  const name = layout && layout !== 'default' ? layout : defaultLayout
  return validLayoutNames.includes(name) ? name : defaultLayout
}
