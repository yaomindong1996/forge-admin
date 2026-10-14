import { describe, expect, it } from 'vitest'
import { buildMegaSections, buildWorkbenchMenus, countMenuLeaves, filterMegaSections, findWorkbenchTrail, resolveMegaActiveSection } from '../menu-model'

const menu = (id, label, path, children = [], extra = {}) => ({ id, label, path, children, type: children.length ? 'module' : 'menu', ...extra })

describe('业务工作台接口菜单', () => {
  it('空权限不注入演示、首页或系统菜单', () => {
    expect(buildWorkbenchMenus()).toEqual([])
    expect(findWorkbenchTrail([], 'missing')).toEqual([])
    expect(buildMegaSections()).toEqual([])
  })

  it('保留接口顺序、唯一 key、查询参数和 SSO 元数据且不修改源数据', () => {
    const source = [menu('200', '报表', '/project/items?type=report', [], { ssoEnabled: 1, ssoTargetClient: 'forge_report', openTarget: '_blank' }), menu('100', '首页', '/home')]
    const result = buildWorkbenchMenus(source)
    expect(result.map(item => item.key)).toEqual(['200', '100'])
    expect(result[0]).toMatchObject({ path: '/project/items?type=report', ssoEnabled: 1, ssoTargetClient: 'forge_report', openTarget: '_blank' })
    expect(source[0]).not.toHaveProperty('key')
  })

  it('隐藏项和筛选后为空的目录不会出现在导航中', () => {
    const result = buildWorkbenchMenus([
      menu('hidden', '隐藏目录', '', [menu('child', '子项', '/hidden')], { meta: { hidden: true } }),
      menu('empty', '空目录', '', [menu('disabled', '停用', '/disabled', [], { menuStatus: 0 })]),
      menu('invisible', '不可见', '/invisible', [], { visible: 0 }),
      menu('normal', '正常', '/normal'),
    ])
    expect(result.map(item => item.key)).toEqual(['normal'])
  })

  it('深层菜单与重复路径通过菜单 key 区分所属面包屑', () => {
    const items = buildWorkbenchMenus([
      menu('a', '业务一', '', [menu('a1', '分类一', '', [menu('a2', '入口一', '/shared')])]),
      menu('b', '业务二', '', [menu('b1', '分类二', '', [menu('b2', '入口二', '/shared')])]),
    ])
    expect(findWorkbenchTrail(items, 'b2').map(item => item.label)).toEqual(['业务二', '分类二', '入口二'])
    expect(findWorkbenchTrail(items, 'unknown')).toEqual([])
  })

  it('大面板保留直属入口与多级分组的原有顺序', () => {
    const [root] = buildWorkbenchMenus([menu('root', '平台管理', '', [
      menu('one', '首页', '/home'),
      menu('group', '系统管理', '/system', [menu('nested', '组织权限', '', [menu('role', '角色', '/system/role')])]),
      menu('last', '日志', '/system/log'),
    ])])
    const sections = buildMegaSections(root)
    expect(sections.map(section => section.title)).toEqual(['首页', '系统管理', '日志'])
    expect(sections.map(section => section.hasChildren)).toEqual([false, true, false])
    expect(sections[0].entry.path).toBe('/home')
    expect(sections[2].entry.path).toBe('/system/log')
    expect(sections[1].items.map(item => item.key)).toEqual(['nested'])
    expect(sections[1].items[0].children[0].path).toBe('/system/role')
    expect(sections[1].entry.path).toBe('/system')
  })

  it('兼容原布局的 subapp 容器，抬升子菜单而不丢入口', () => {
    const result = buildWorkbenchMenus([menu('container', '应用', '', [menu(123, '入口', '/app')], { type: 'subapp' })])
    expect(result.map(item => item.key)).toEqual(['123'])
    expect(findWorkbenchTrail(result, 123)[0].path).toBe('/app')
  })

  it('展开菜单搜索保留命中项父级上下文且不修改原分组', () => {
    const [root] = buildWorkbenchMenus([menu('root', '业务中心', '', [
      menu('case-group', '订单管理', '', [
        menu('case-list', '订单列表', '/business/order'),
        menu('party', '客户管理', '/business/customer'),
      ]),
      menu('ops-group', '经营治理', '', [
        menu('rules', '业务规则', '/business/operations/rule'),
        menu('audit', '业务审计', '/business/operations/audit'),
      ]),
    ])])
    const sections = buildMegaSections(root)

    const byLeaf = filterMegaSections(sections, '审计')
    expect(byLeaf).toHaveLength(1)
    expect(byLeaf[0].title).toBe('经营治理')
    expect(byLeaf[0].items.map(item => item.label)).toEqual(['业务审计'])

    const bySection = filterMegaSections(sections, '订单')
    expect(bySection[0].items.map(item => item.label)).toEqual(['订单列表', '客户管理'])
    expect(filterMegaSections(sections, '不存在')).toEqual([])
    expect(sections[1].items).toHaveLength(2)
  })

  it('展开菜单搜索可命中无下级的二级直接入口', () => {
    const [root] = buildWorkbenchMenus([menu('root', '业务中心', '', [
      menu('workbench', '业务工作台', '/workspace/summary'),
      menu('case-group', '订单管理', '', [menu('case', '订单列表', '/business/order')]),
    ])])

    const result = filterMegaSections(buildMegaSections(root), '工作台')
    expect(result).toHaveLength(1)
    expect(result[0]).toMatchObject({ key: 'workbench', title: '业务工作台', hasChildren: false })
    expect(result[0].entry.path).toBe('/workspace/summary')
    expect(result[0].items).toEqual([])
  })

  it('支持业务中心与平台管理双域下的三级产品导航', () => {
    const items = buildWorkbenchMenus([
      menu('business', '业务中心', '', [
        menu('business-workbench', '业务工作台', '/workspace/summary'),
        menu('case-group', '订单管理', '/business/order-management', [
          menu('case', '订单列表', '/business/order'),
          menu('party', '客户管理', '/business/customer'),
        ]),
        menu('approval-group', '审批与提醒', '/business/approval-reminder', [
          menu('todo', '我的待办', '/flow/todo'),
          menu('approval', '业务申请', '/business/approval'),
        ]),
      ]),
      menu('platform', '平台管理', '/platform', [
        menu('access-group', '组织与权限', '/platform/access', [
          menu('user', '用户管理', '/system/user'),
        ]),
        menu('development-group', '开发与集成', '/platform/development', [
          menu('flow', '流程中心', '/flow'),
        ]),
      ]),
    ])

    expect(items.map(item => item.label)).toEqual(['业务中心', '平台管理'])
    expect(buildMegaSections(items[0]).map(section => section.title)).toEqual(['业务工作台', '订单管理', '审批与提醒'])
    expect(buildMegaSections(items[1]).map(section => section.title)).toEqual(['组织与权限', '开发与集成'])
    expect(findWorkbenchTrail(items, 'todo').map(item => item.label)).toEqual(['业务中心', '审批与提醒', '我的待办'])
    expect(findWorkbenchTrail(items, 'user').map(item => item.label)).toEqual(['平台管理', '组织与权限', '用户管理'])
  })

  it('统计多级分组的实际入口数量，用于菜单入口汇总', () => {
    const items = [
      menu('direct', '直接入口', '/direct'),
      menu('group', '三级分组', '', [
        menu('nested', '四级分组', '', [
          menu('leaf-one', '入口一', '/one'),
          menu('leaf-two', '入口二', '/two'),
        ]),
      ]),
    ]
    expect(countMenuLeaves(items)).toBe(3)
    expect(countMenuLeaves()).toBe(0)
  })

  it('工作台混合叶子与目录时，当前路由叶子优先于请假管理等分组回退', () => {
    const [root] = buildWorkbenchMenus([menu(43, '工作台', '/workbench', [
      menu(9283, '首页', '/home'),
      menu(80, '我的待办', '/flow/todo'),
      menu(81, '我的已办', '/flow/done'),
      menu(900, '请假管理', '/leave', [
        menu(901, '请假申请', '/leave/apply'),
        menu(902, '我的请假', '/leave/list'),
      ]),
    ])])
    const sections = buildMegaSections(root)

    expect(resolveMegaActiveSection(sections, { routeSectionKey: '80', activeKey: '80' })?.title).toBe('我的待办')
    expect(resolveMegaActiveSection(sections, { routeSectionKey: '900', activeKey: '901' })?.title).toBe('请假管理')
    expect(resolveMegaActiveSection(sections, { activeKey: '901' })?.title).toBe('请假管理')
    expect(resolveMegaActiveSection(sections, { previewKey: '900', routeSectionKey: '80', activeKey: '80' })?.title).toBe('请假管理')
    // 无预览、无路由命中时不默认高亮第一项
    expect(resolveMegaActiveSection(sections, {})).toBeUndefined()
  })
})
