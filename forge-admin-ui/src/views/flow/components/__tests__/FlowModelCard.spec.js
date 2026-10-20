import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FlowModelCard from '../FlowModelCard.vue'

const publicationOptions = [
  { label: '尚未发布', value: 'UNPUBLISHED' },
  { label: '有未发布修改', value: 'CHANGED' },
  { label: '与发布版本一致', value: 'SYNCED' },
  { label: '发布信息待核对', value: 'UNKNOWN' },
]
function render(item, extra = {}) {
  return mount(FlowModelCard, { props: { item: { id: 'fixture-A', modelName: '合成流程', version: 1, ...item }, publicationOptions, ...extra }, global: { stubs: {
    WorkspaceIllustration: true,
    Dropdown: { template: '<div><slot /></div>' },
    DictTag: { props: ['options', 'value'], template: '<span>{{ options.find(item => item.value === value)?.label }}</span>' },
  } } })
}
describe('流程模型卡片', () => {
  it('草稿不显示伪造 v1，设计文案与信息编辑区分', async () => {
    const wrapper = render({ status: 0, publicationState: 'UNPUBLISHED' })
    expect(wrapper.text()).toContain('尚未发布')
    expect(wrapper.text()).not.toContain('v1')
    await wrapper.findAll('button').find(button => button.text() === '设计流程').trigger('click')
    expect(wrapper.emitted('design')).toHaveLength(1)
    wrapper.unmount()
  })
  it('变更卡片显示真实发布版本和发布更新，直显不超过两项', async () => {
    const wrapper = render({ status: 1, publicationState: 'CHANGED', publishedVersion: 3 })
    expect(wrapper.text()).toContain('发布版本 v3')
    expect(wrapper.text()).toContain('有未发布修改')
    expect(wrapper.findAll('.card-action-link')).toHaveLength(2)
    await wrapper.findAll('button').find(button => button.text() === '发布更新').trigger('click')
    expect(wrapper.emitted('deploy')).toHaveLength(1)
    wrapper.unmount()
  })
  it('已同步卡片进入运行记录，未知状态不显示原始英文', async () => {
    const wrapper = render({ status: 1, publicationState: 'SYNCED', publishedVersion: 3 })
    await wrapper.findAll('button').find(button => button.text() === '运行记录').trigger('click')
    expect(wrapper.emitted('instances')).toHaveLength(1)
    await wrapper.setProps({ item: { status: 1, publicationState: 'FUTURE_STATE' } })
    expect(wrapper.text()).toContain('发布信息暂不可用')
    expect(wrapper.text()).not.toContain('FUTURE_STATE')
    wrapper.unmount()
  })
  it.each([{ busy: true }, { sortMode: true }])('忙碌或排序时禁用动作 %j', (props) => {
    const wrapper = render({ status: 0 }, props)
    expect(wrapper.findAll('button').every(button => button.attributes('disabled') !== undefined)).toBe(true)
    wrapper.unmount()
  })
})
