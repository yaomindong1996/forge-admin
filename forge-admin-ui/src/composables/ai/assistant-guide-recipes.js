const pages = {
  '/system/user': {
    keywords: ['系统用户', '用户账号', '账号管理', '添加员工'],
    search: ['search', '在筛选区输入用户名或姓名，查询对应的用户。'],
    add: ['add', '点击「新增」，填写账号和人员信息后保存。', 'system:user:add'],
    edit: ['list', '先筛选到目标用户，再点击该行的「编辑」。'],
  },
  '/system/org': {
    keywords: ['组织', '部门', '组织架构'],
    search: ['org-tree', '在左侧组织树中选中目标组织，查看下属部门及人员。'],
    add: ['add-org', '创建独立组织时点击「新增顶级组织」。已有上级时，在组织树中选择上级，再添加下级。', 'system:org:add'],
  },
  '/system/role': {
    keywords: ['角色', '角色权限', '分配菜单', '给角色授权'],
    search: ['search-role', '在左侧查找目标角色，右侧会显示该角色的配置。'],
    edit: ['role-list', '在左侧找到目标角色，从该角色的更多操作打开权限配置。'],
    add: ['add-role', '点击左侧新增角色按钮，填写名称和适用组织。', 'system:role:add'],
  },
  '/system/menu': {
    keywords: ['菜单', '菜单配置', '菜单资源'],
    search: ['search', '在左侧通过资源树定位目录或菜单，可以先按客户端筛选。'],
    add: ['add', '创建一级入口时点击「新增顶级菜单」。添加子项时，先在左侧选中父级，再点「新增子项」。'],
    edit: ['edit', '在资源树选中需要修改的菜单，点击「编辑本级」后核对并保存。'],
  },
  '/system/job-config': {
    keywords: ['定时任务', '调度任务', '任务调度'],
    search: ['search', '按任务名称或分组筛选，查看已有的调度任务。'],
    add: ['job-add', '从列表新增任务，配置执行内容和触发时间，再选择保存后的状态。', 'system:jobConfig:add'],
  },
  '/system/operation-log': {
    keywords: ['操作日志', '操作审计', '谁改了', '谁操作了'],
    search: ['search', '用操作人、页面或时间过滤审计记录，打开对应条目查看详情。'],
  },
  '/flow/model': {
    keywords: ['流程模型', '审批流程', '配置流程', '设计流程', '流程设计'],
    search: ['search', '按模型名称筛选，再打开对应的流程模型。'],
    add: ['add', '点击「新增模型」，填写基本信息后进入流程设计器。', 'flow:model:add'],
    edit: ['list', '找到目标模型后打开设计器，检查节点和审批人配置，再按页面流程发布。'],
  },
  '/workspace/todo': {
    keywords: ['待办', '办理审批', '审批任务', '待我审批', '我的审批', '处理审批'],
    search: ['search', '按申请标题查询待办，然后打开对应的任务卡片。'],
    edit: ['list', '核对任务详情、附件和审批意见，使用当前节点显示的办理操作。'],
  },
  '/workspace/started': {
    keywords: ['我发起的', '发起的审批', '审批进度', '撤回申请', '催办审批'],
    search: ['search', '按申请标题定位记录，打开详情可以查看审批历史和当前状态。'],
  },
  '/app-center': {
    keywords: ['应用中心', '业务应用', '发布应用', '应用市场', '应用版本'],
    edit: ['page', '在「我的应用」找到对应应用并打开。在应用工作台核对页面和数据模型，再进入发布。'],
  },
}

const aliases = {
  '/flow/todo': '/workspace/todo',
  '/flow/started': '/workspace/started',
}

function resolvePage(path) {
  return pages[path] || pages[aliases[path]]
}

export function guideKeywords(path) {
  return resolvePage(path)?.keywords || []
}

const operation = [
  ['add', /新增|新建|创建|添加|建个|建一个|加个|录入/],
  ['edit', /编辑|修改|变更|配置|发布|授权|分配|处理|办理/],
  ['search', /查找|查询|筛选|搜索|查|找/],
]

const nativeSteps = {
  add: ['add', '打开页面后，可以定位当前可用的「新增」入口。'],
  edit: ['list', '可先定位列表，找到需要修改的记录，再查看该行可用的操作。'],
  search: ['search', '可以定位页面的筛选区，按名称或其他可用条件查询。'],
}

export function guideSteps(path, question, can) {
  const page = resolvePage(path)
  const selected = operation.find(([key, pattern]) => (!page || page[key]) && pattern.test(question))?.[0]
  if (!page) {
    const step = nativeSteps[selected]
    return step ? [{ anchor: step[0], text: step[1] }] : []
  }
  const keys = selected ? [selected] : ['search', 'edit', 'add']
  return keys.flatMap((key) => {
    const step = page[key]
    if (!step || (step[2] && !can(step[2])))
      return []
    return [{ anchor: step[0], text: step[1] }]
  })
}
