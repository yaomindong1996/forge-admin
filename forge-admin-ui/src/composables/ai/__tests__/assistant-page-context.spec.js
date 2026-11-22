import { describe, expect, it } from 'vitest'
import { assistantPageDirectory, currentAssistantPage, findAssistantGuide, messageWithPageContext } from '../assistant-page-context'

const user = { userId: '11', permissions: ['system:user:add', 'flow:task:todo'] }
const can = code => user.permissions.includes(code)
const menu = (key, label, path, extra) => ({ key, label, path, type: 'menu', ...extra })
const system = { key: 'system', type: 'dir', label: '系统管理', children: [
  menu('1', '用户管理', '/system/user'),
  menu('2', '定时任务配置', '/system/job-config'),
  menu('3', '费用报表', '/report-center/run'),
  menu('4', '外部工单', '//external.invalid'),
  menu('5', '审计日志', '/system/audit'),
  menu('10', '角色管理', '/system/role'),
] }
const business = { key: 'biz', type: 'dir', label: '办公', children: [
  menu('6', '设备登记', '/biz/equipment'),
  menu('7', '供应商名录', '/app-center/app/100'),
  menu('8', '伙伴平台', '/partner', { ssoEnabled: 1 }),
] }
const permission = {
  menuDataLoaded: true,
  menus: [system, business],
  accessRoutes: [...system.children, ...business.children].filter(item => item.key !== '5').map(item => ({ path: item.path })),
}

describe('助手页面上下文', () => {
  it('只检索当前权限目录中的内部页面，新业务菜单无需添加别名也能找到', () => {
    const entries = assistantPageDirectory(permission, user)
    expect(entries.map(item => item.id)).toEqual(['1', '2', '10', '6', '7', '/flow/todo'])
    expect(assistantPageDirectory({ ...permission, menuDataLoaded: false }, user)).toEqual([])
    const matched = findAssistantGuide('设备登记怎么新增？', null, entries, can)
    expect(matched.options).toMatchObject([{ id: '6', trail: ['办公', '设备登记'], steps: [{ anchor: 'add' }] }])
    expect(findAssistantGuide('审计日志入口在哪？', null, entries, can).options).toHaveLength(0)
    expect(findAssistantGuide('打开 https://example.invalid', null, entries, can)).toBeNull()
  })

  it('找操作时先选真实菜单，账号没有新增权限时只提供页面入口', () => {
    const entries = assistantPageDirectory(permission, user)
    const guide = findAssistantGuide('我可以在哪里新增用户？', null, entries, can)
    expect(guide.options).toMatchObject([{ title: '用户管理', steps: [{ anchor: 'add' }] }])
    expect(findAssistantGuide('用户在哪里新增？', null, entries, () => false).options[0].steps).toEqual([])
    expect(findAssistantGuide('查我的待办', null, entries, can)).toBeNull()
    expect(findAssistantGuide('帮我创建一个费用审批应用', null, entries, can)).toBeNull()
  })

  it('角色配置问题能定位到角色管理并给出配置步骤', () => {
    const entries = assistantPageDirectory(permission, user)
    const guide = findAssistantGuide('角色怎么配置？', null, entries, can)
    expect(guide.options).toMatchObject([{
      title: '角色管理',
      steps: [{ anchor: 'role-list' }],
    }])
  })

  it('重名菜单提供候选，带目录名称时优先匹配该目录', () => {
    const entries = assistantPageDirectory(permission, user)
    const duplicates = [...entries, { id: '9', title: '设备登记', path: '/facility/equipment', trail: ['设施', '设备登记'] }]
    expect(findAssistantGuide('设备登记在哪？', null, duplicates, can).options.map(item => item.id)).toEqual(['6', '9'])
    expect(findAssistantGuide('办公设备登记在哪里？', null, duplicates, can).options.map(item => item.id)).toEqual(['6'])
  })

  it('显示当前子页，发送的上下文不含路由参数与记录标识', () => {
    const entries = assistantPageDirectory(permission, user)
    const route = { path: '/system/job-config/editor', fullPath: '/system/job-config/editor?id=private-record', meta: { title: '定时任务编辑' } }
    const page = currentAssistantPage(route, entries)
    expect(page.trail).toEqual(['系统管理', '定时任务配置', '定时任务编辑'])
    const guide = findAssistantGuide('这个页面怎么用？', page, entries, can)
    expect(guide.options[0].currentScope).toBe(route.fullPath)
    const message = messageWithPageContext('填写要求是什么', page)
    expect(message).toContain('定时任务编辑')
    expect(message).not.toMatch(/private-record|\/system\/job-config/)
    expect(currentAssistantPage({ path: '/report-center/run' }, entries)).toBeNull()
    expect(currentAssistantPage({
      path: '/app-center/application/supplier/runtime',
      fullPath: '/app-center/application/supplier/runtime?menuKey=7',
      query: { menuKey: '7' },
    }, entries)?.title).toBe('供应商名录')
  })
})
