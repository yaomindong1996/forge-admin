// 模拟菜单数据
export const mockMenuData = {
  code: 200,
  message: '成功',
  time: '20251023111433289',
  data: [
    {
      id: 19,
      parentId: 0,
      resourceName: '案件信息管理',
      resourceType: 2,
      sort: 1,
      path: '/system/legalCase',
      component: '/system/legalCase/index',
      isExternal: 0,
      menuStatus: 1,
      visible: 1,
      perms: 'system:legalCase:view',
      keepAlive: 0,
      alwaysShow: 0,
    },
    {
      id: 1,
      parentId: 0,
      resourceName: '系统管理',
      resourceType: 1,
      sort: 1,
      path: '/system',
      isExternal: 0,
      menuStatus: 1,
      visible: 1,
      icon: 'ionicons5:AddCircleSharp',
      keepAlive: 0,
      alwaysShow: 1,
      children: [
        {
          id: 2,
          parentId: 1,
          resourceName: '用户管理',
          resourceType: 2,
          sort: 1,
          path: '/system/user',
          component: 'system/user/index',
          isExternal: 0,
          menuStatus: 1,
          visible: 1,
          perms: 'system:user:list',
          icon: 'ionicons5:AddCircleSharp',
          keepAlive: 1,
          alwaysShow: 0,
        },
        {
          id: 9,
          parentId: 1,
          resourceName: '角色管理',
          resourceType: 2,
          sort: 2,
          path: '/system/role',
          component: 'system/role/index',
          isExternal: 0,
          menuStatus: 1,
          visible: 1,
          perms: 'system:role:list',
          icon: 'ionicons5:AddCircleSharp',
          keepAlive: 1,
          alwaysShow: 0,
        },
        {
          id: 14,
          parentId: 1,
          resourceName: '组织管理',
          resourceType: 2,
          sort: 3,
          path: '/system/org',
          component: 'system/org/index',
          isExternal: 0,
          menuStatus: 1,
          visible: 1,
          perms: 'system:org:list',
          icon: 'ionicons5:AddCircleSharp',
          keepAlive: 1,
          alwaysShow: 0,
        },
        {
          id: 17,
          parentId: 1,
          resourceName: '岗位管理',
          resourceType: 2,
          sort: 4,
          path: '/system/post',
          component: 'system/post/index',
          isExternal: 0,
          menuStatus: 1,
          visible: 1,
          perms: 'system:post:list',
          icon: 'ionicons5:AddCircleSharp',
          keepAlive: 1,
          alwaysShow: 0,
        },
      ],
    },
  ],
}

// 模拟API调用
export const mockMenuApi = {
  getMenu: async (type = 1) => {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 500))
    return mockMenuData
  },

  // 添加获取子应用树的模拟接口 - 兼容新数据结构
  getSubAppTree: async () => {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 500))
    return {
      data: [
        {
          id: '1',
          url: '/project-user',
          name: '系统管理',
          directoryId: null,
          parentId: null,
          subAppIcon: 'i-material-symbols:settings',
          moduleIcon: null,
          children: [
            {
              id: '1-1',
              url: null,
              name: '用户中心',
              directoryId: null,
              parentId: '1',
              subAppIcon: null,
              moduleIcon: 'i-material-symbols:person',
              children: [
                {
                  id: '1-1-1',
                  url: '/system/user',
                  name: '用户管理',
                  directoryId: null,
                  parentId: '1-1',
                  subAppIcon: null,
                  moduleIcon: null,
                  children: null,
                },
                {
                  id: '1-1-2',
                  url: '/system/role',
                  name: '角色管理',
                  directoryId: null,
                  parentId: '1-1',
                  subAppIcon: null,
                  moduleIcon: null,
                  children: null,
                },
                {
                  id: '1-1-3',
                  url: '/system/menu',
                  name: '菜单管理',
                  directoryId: null,
                  parentId: '1-1',
                  subAppIcon: null,
                  moduleIcon: null,
                  children: null,
                },
              ],
            },
          ],
        },
        {
          id: '2',
          url: '/cbc-web',
          name: '二级调度中心',
          directoryId: null,
          parentId: null,
          subAppIcon: 'i-material-symbols:dashboard',
          moduleIcon: null,
          children: [
            {
              id: '2-1',
              url: null,
              name: '一键打印管理',
              directoryId: '',
              parentId: '2',
              subAppIcon: null,
              moduleIcon: 'i-material-symbols:print',
              children: [
                {
                  id: '2-1-1',
                  url: '/approval/config',
                  name: '审批人员配置',
                  directoryId: '',
                  parentId: '2-1',
                  subAppIcon: null,
                  moduleIcon: null,
                  children: null,
                },
              ],
            },
          ],
        },
      ],
    }
  },

  // 添加获取菜单列表的模拟接口
  getMenuList: async (params) => {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 500))
    return {
      data: [
        {
          id: 1,
          title: '用户管理',
          path: '/system/user',
          icon: 'i-material-symbols:person',
          sort: 1,
          visible: true,
          level: 0,
          moduleId: params.moduleId,
          subAppId: params.subAppId,
        },
        {
          id: 2,
          title: '角色管理',
          path: '/system/role',
          icon: 'i-material-symbols:group',
          sort: 2,
          visible: true,
          level: 0,
          moduleId: params.moduleId,
          subAppId: params.subAppId,
        },
      ],
      total: 2,
    }
  },

  // 添加获取模块列表的模拟接口
  getModuleList: async (params) => {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 500))
    return {
      data: [
        {
          id: '1-1',
          name: '用户中心',
          icon: 'i-material-symbols:person',
          sort: 1,
          type: '1',
          remark: '用户管理模块',
          subAppId: params.subAppId,
        },
      ],
      total: 1,
    }
  },

  // 添加获取子应用列表的模拟接口
  getSubAppList: async (params) => {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 500))
    return {
      data: [
        {
          id: '1',
          name: '系统管理',
          alias: 'system',
          url: '/system',
          icon: 'i-material-symbols:settings',
        },
      ],
      total: 1,
    }
  },
}
