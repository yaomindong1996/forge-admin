import { shallowMount } from '@vue/test-utils'
import { NButton } from 'naive-ui'
import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import PluginBuildExecution from '../components/PluginBuildExecution.vue'

vi.mock('@/composables/useDict', () => ({
  useDict: () => ({ dict: ref({ sys_plugin_build_phase: [{ value: 'container_build', label: '隔离容器构建' }] }) }),
}))
const execution = {
  workerId: 'test-worker',
  phase: 'container_build',
  sourceCommit: 'a'.repeat(40),
  image: 'builder@sha256:hash',
  startedTime: '2026-10-08T09:00:00',
  leaseExpiresTime: '2026-10-08T09:01:30',
  deadlineTime: '2026-10-08T09:25:00',
  leaseExpired: false,
}
function mount(value, busy = false) {
  return shallowMount(PluginBuildExecution, {
    props: { execution: value, busy },
    global: { renderStubDefaultSlot: true },
  })
}
describe('构建执行记录', () => {
  it('人工封存没有worker报告，不能冒充执行结果', () => {
    const wrapper = mount({ ...execution, finishedTime: '2026-10-08T10:30:00' })
    expect(wrapper.text()).toContain('结束时间为人工关闭时间')
    expect(wrapper.text()).toContain('没有执行器结果报告')
    expect(wrapper.text()).not.toContain('个文件')
    wrapper.unmount()
  })
  it('未领取不显示执行记录，正常记录展示固定输入且刷新有真实事件', async () => {
    const empty = mount(null)
    expect(empty.find('section').exists()).toBe(false)
    empty.unmount()
    const wrapper = mount(execution)
    expect(wrapper.text()).toContain('test-worker')
    expect(wrapper.text()).toContain('2026-10-08 09:00:00')
    expect(wrapper.text()).not.toContain('租约已到期')
    wrapper.findComponent(NButton).vm.$emit('click')
    expect(wrapper.emitted('refresh')).toHaveLength(1)
    await wrapper.setProps({ busy: true })
    expect(wrapper.findComponent(NButton).props('disabled')).toBe(true)
    wrapper.unmount()
  })
  it('失租明确提示不自动抢占；失败仅显示稳定代码，没有危险下载或部署按钮', () => {
    const wrapper = mount({ ...execution, leaseExpired: true, result: { success: false, failureCode: 'SOURCE_DIRTY' } })
    expect(wrapper.text()).toContain('不会自动抢占或重试')
    expect(wrapper.text()).toContain('SOURCE_DIRTY')
    expect(wrapper.find('a').exists()).toBe(false)
    expect(wrapper.find('script').exists()).toBe(false)
    wrapper.unmount()
  })
  it('完成结果显示产物摘要和待部署边界，不把报告当作已安装/运行健康', () => {
    const wrapper = mount({
      ...execution,
      finishedTime: '2026-10-08T09:10:00',
      result: {
        success: true,
        artifactCount: 2,
        artifactBytes: 1048576,
        artifactManifestSha256: 'b'.repeat(64),
      },
    })
    expect(wrapper.text()).toContain('2 个文件')
    expect(wrapper.text()).toContain('1.0 MiB')
    expect(wrapper.text()).toContain('尚未部署')
    expect(wrapper.text()).not.toContain('硬期限')
    wrapper.unmount()
  })
})
