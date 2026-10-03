import { mount } from '@vue/test-utils'
import { NTag } from 'naive-ui'
import { createPinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useDictStore } from '@/stores/system/dictStore'
import HomeApprovalCenter from '../components/HomeApprovalCenter.vue'
import HomeBuildPath from '../components/HomeBuildPath.vue'
import HomeQuickEntries from '../components/HomeQuickEntries.vue'
import HomeTodoList from '../components/HomeTodoList.vue'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('@/composables/useDict', () => ({ getDictData: vi.fn(async () => []) }))

let wrappers = []

function render(component, props = {}) {
  const pinia = createPinia()
  const dict = useDictStore(pinia)
  // 测试字典是隔离的协议样例，不替代生产字典接口。
  dict.dictCache.set('flow_todo_status', [
    { value: 0, label: '待办', listClass: 'warning' },
    { value: 1, label: '已签收', listClass: 'info' },
  ])
  dict.dictCache.set('flow_priority', [{ value: 2, label: '高', listClass: 'warning' }])
  const wrapper = mount(component, { props, global: { plugins: [pinia], components: { NTag } } })
  wrappers.push(wrapper)
  return wrapper
}

beforeEach(() => push.mockClear())
afterEach(() => {
  wrappers.forEach(wrapper => wrapper.unmount())
  wrappers = []
})

describe('homepage section contracts', () => {
  it('keeps the four business building steps and renders visible SVG icons', async () => {
    const wrapper = render(HomeBuildPath)
    const buttons = wrapper.findAll('nav button')
    const paths = ['/app-center', '/ai/lowcode-apps', '/flow/model', '/system/menu']
    expect(buttons).toHaveLength(paths.length)
    for (const [index, button] of buttons.entries()) {
      expect(button.find('svg').exists()).toBe(true)
      await button.trigger('click')
      expect(push).toHaveBeenLastCalledWith(paths[index])
    }
    expect(wrapper.find('img').attributes('alt')).toBe('')
  })

  it('shows supplied approval counts and preserves statistics and action destinations', async () => {
    const wrapper = render(HomeApprovalCenter, {
      todoCount: 17,
      doneCount: 93,
      startedCount: 24,
      pendingStarted: 5,
    })
    expect(wrapper.findAll('.approval-summary-value strong').map(item => item.text())).toEqual(['17', '93', '24'])
    expect(wrapper.text()).toContain('5 个审批中')
    const buttons = [...wrapper.findAll('.approval-summary'), ...wrapper.findAll('nav button')]
    const paths = ['/flow/todo', '/flow/done', '/flow/started', '/flow/template', '/flow/cc', '/flow/monitor']
    for (const [index, button] of buttons.entries()) {
      await button.trigger('click')
      expect(push).toHaveBeenLastCalledWith(paths[index])
    }
  })

  it('keeps all six management shortcuts with a separate SVG for each destination', async () => {
    const wrapper = render(HomeQuickEntries)
    const buttons = wrapper.findAll('nav button')
    const paths = ['/system/user', '/system/role', '/system/org', '/system/menu', '/system/post', '/system/file-list']
    expect(buttons).toHaveLength(paths.length)
    expect(new Set(buttons.map(button => button.find('svg').html())).size).toBe(paths.length)
    for (const [index, button] of buttons.entries()) {
      await button.trigger('click')
      expect(push).toHaveBeenLastCalledWith(paths[index])
    }
  })

  it('uses dictionary statuses and passes the untouched task record to the existing navigation', async () => {
    const tasks = [
      { taskId: 'task-a', title: '合同审批', taskName: '经理审批', status: 0, assignee: '', priority: 2 },
      { id: 'task-b', processTitle: '采购申请', status: 0, assignee: 'owner', priority: 1 },
    ]
    const wrapper = render(HomeTodoList, { tasks })
    const rows = wrapper.findAll('.home-todo-row')
    expect(rows).toHaveLength(2)
    expect(rows[0].text()).toContain('待办')
    expect(rows[0].text()).toContain('高')
    expect(rows[1].text()).toContain('已签收')
    expect(rows[0].find('button').exists()).toBe(false)
    expect(rows[0].attributes('aria-label')).toBe('处理合同审批')
    await rows[0].trigger('click')
    expect(wrapper.emitted('open')[0][0]).toEqual(tasks[0])
    expect(wrapper.emitted('open')[0][0].taskId).toBe('task-a')
    await wrapper.find('header button').trigger('click')
    expect(push).toHaveBeenLastCalledWith('/flow/todo')
  })

  it('distinguishes initial loading from an empty inbox without fabricating task rows', async () => {
    const wrapper = render(HomeTodoList, { loading: true })
    expect(wrapper.find('[aria-label="正在加载待办任务"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('暂无待办任务')
    expect(wrapper.find('.home-todo-row').exists()).toBe(false)
    await wrapper.setProps({ loading: false })
    expect(wrapper.text()).toContain('暂无待办任务')
    expect(wrapper.text()).not.toContain('全部处理完成')
    expect(wrapper.find('img').attributes('alt')).toBe('')
  })
})
