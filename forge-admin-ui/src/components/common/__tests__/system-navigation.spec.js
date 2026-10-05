import { readFileSync } from 'node:fs'
import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it } from 'vitest'
import dynamicIcons from '@/assets/icons/dynamic-icons'
import {
  buildRoleUserOrgTreeOptions,
  flattenOrgNodes,
  normalizeNumberList,
  normalizeSingleNumber,
} from '@/views/system/role-organization-presentation'
import { getOrganizationNodeIcon } from '../organization-tree-icons'
import PremiumTree from '../PremiumTree.vue'
import SystemTableCell from '../SystemTableCell.vue'

const nodes = [{ id: 1, label: '组织', children: [{ id: 2, label: '成员组' }, { id: 3, label: '协作组' }] }]
let wrapper
afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ''
})
function source(relative) {
  return readFileSync(new URL(relative, import.meta.url), 'utf8')
}

describe('统一组织树图标', () => {
  it.each([undefined, null, 0, '0', ''])('根 parentId=%s 使用建筑线性图标', (parentId) => {
    expect(getOrganizationNodeIcon({ parentId })).toBe('i-lucide:building-2')
  })
  it('分支和叶子可辨别，缺失 children 安全处理', () => {
    expect(getOrganizationNodeIcon({ parentId: 1, children: [{}] })).toBe('i-lucide:folder-tree')
    expect(getOrganizationNodeIcon({ parentId: 1 })).toBe('i-lucide:users')
    expect(getOrganizationNodeIcon({ parentId: 1, children: [], hasChildren: true })).toBe('i-lucide:folder-tree')
  })
  it('用户、组织、岗位、角色、人员选择都接入同一解析', () => {
    for (const file of [
      '../../../stores/system/user-management/utils.js',
      '../../../views/system/org.vue',
      '../../../views/system/post.vue',
      '../../../views/system/composables/useRolePage.part1.js',
      '../../UserSelectPanel.vue',
    ]) {
      expect(source(file)).toContain('organization-tree-icons')
    }
  })
  it('动态组织与资源图标全部进入构建 safelist，不在生产丢失', () => {
    for (const icon of ['building-2', 'folder-tree', 'users', 'folder', 'file-text', 'panel-top', 'mouse-pointer-2', 'braces', 'layers']) {
      expect(dynamicIcons).toContain(`i-lucide:${icon}`)
    }
  })
  it('公共树默认图标线性化，并保留业务自定义图标', async () => {
    wrapper = mount(PremiumTree, { props: { data: nodes, keyField: 'id', expandedKeys: [1] } })
    expect(wrapper.find('.premium-tree-icon .i-lucide\\:folder').exists()).toBe(true)
    expect(wrapper.find('.premium-tree-icon .i-lucide\\:file-text').exists()).toBe(true)
    await wrapper.setProps({ getNodeIcon: () => 'custom-business-icon' })
    expect(wrapper.findAll('.custom-business-icon')).toHaveLength(3)
  })
  it('点击和键盘展开保留受控事件，选中 keys 不改变类型', async () => {
    wrapper = mount(PremiumTree, { props: { data: nodes, keyField: 'id' } })
    await wrapper.find('[role="treeitem"]').trigger('keydown', { key: 'ArrowRight' })
    expect(wrapper.emitted('update:expanded-keys')[0][0]).toEqual([1])
    await wrapper.setProps({ expandedKeys: [1] })
    await wrapper.findAll('[role="treeitem"]')[1].trigger('click')
    expect(wrapper.emitted('update:selected-keys')[0][0]).toEqual([2])
  })
  it('父节点级联勾选仍提交叶子 keys', async () => {
    wrapper = mount(PremiumTree, { props: { data: nodes, keyField: 'id', checkable: true } })
    await wrapper.find('[role="treeitem"]').trigger('click')
    expect(wrapper.emitted('update:checked-keys')[0][0]).toEqual([2, 3])
  })
})

describe('公共列表单元格', () => {
  it('主值常规字重，仍保留实体和辅助标识', () => {
    expect(source('../SystemTableCell.vue')).toMatch(/\.system-table-cell__primary\s*\{[^}]*font-weight: 400/)
    wrapper = mount(SystemTableCell, { props: { title: '示例成员', subtitle: '@fixture', interactive: true, avatar: true } })
    expect(wrapper.find('.system-table-cell__primary').text()).toBe('示例成员')
    expect(wrapper.find('.system-table-cell__secondary').text()).toBe('@fixture')
    expect(wrapper.find('.system-table-cell__avatar').text()).toBe('示')
  })
  it('实体点击继续发出 activate', async () => {
    wrapper = mount(SystemTableCell, { props: { title: '成员', interactive: true } })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('activate')).toHaveLength(1)
  })
  it('关联主值和 +N 展开保留且去重', async () => {
    wrapper = mount(SystemTableCell, { attachTo: document.body, props: { values: ['组织一', '组织二', '组织一'] } })
    expect(wrapper.find('.system-table-cell__primary').text()).toBe('组织一')
    expect(wrapper.find('.system-table-cell__count').text()).toBe('+1')
    await wrapper.find('.system-table-cell__count').trigger('click')
    expect(document.body.textContent).toContain('组织二')
  })
  it('工作台顶级交互状态透明，但仍保留独立选中横条', () => {
    const css = source('../../../styles/layout-chrome.css')
    expect(css).toMatch(/button:is\(\.active, \.is-intent, \.is-open, :hover\)\s*\{[^}]*background: transparent/)
    expect(css).toContain('background: var(--top-menu-active-bar-color, var(--primary-color))')
  })
})

describe('角色组织树提取兼容', () => {
  it('数组/空值数字规范化继续保留原行为', () => {
    expect(normalizeSingleNumber(['', '12'])).toBe(12)
    expect(normalizeSingleNumber('invalid', 0)).toBe(0)
    expect(normalizeNumberList([12, '12', null, '13'])).toEqual([12, 13])
  })
  it('限定范围保留禁选祖先，但不暴露无权限叶子', () => {
    const list = [{ id: 1, orgName: '上级组织', children: [{ id: 2, orgName: '授权组织' }, { id: 3, orgName: '其它组织' }] }]
    expect(buildRoleUserOrgTreeOptions(list, new Set([2]))).toEqual([
      { label: '上级组织', value: 1, disabled: true, children: [{ label: '授权组织', value: 2, disabled: false, children: [] }] },
    ])
    expect(flattenOrgNodes(list).map(item => item.id)).toEqual([1, 2, 3])
  })
  it('全局范围仍允许所有组织节点', () => {
    expect(buildRoleUserOrgTreeOptions([{ id: '12', orgName: '示例组织' }], new Set(), true)).toEqual([
      { label: '示例组织', value: 12, disabled: false, children: [] },
    ])
  })
})
