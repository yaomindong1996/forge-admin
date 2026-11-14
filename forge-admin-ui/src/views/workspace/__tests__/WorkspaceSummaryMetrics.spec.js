import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { getWorkspaceSummary } from '@/api/workspace'
import WorkspaceSummaryMetrics from '../WorkspaceSummaryMetrics.vue'

vi.mock('@/api/workspace', () => ({ getWorkspaceSummary: vi.fn() }))
const badgeStore = reactive({ syncedVersion: 0, counts: {} })
vi.mock('@/stores/flow/workspaceBadgeStore', () => ({ useWorkspaceBadgeStore: () => badgeStore }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/flow/cc', query: {} }),
  useRouter: () => ({ push: vi.fn() }),
}))

const timeout = Object.assign(new Error('timeout of 3500ms exceeded'), { code: 'ECONNABORTED' })
const ok = { code: 200, data: { todoCount: 19, doneWeekCount: 8, startedRunningCount: 63, ccUnreadCount: 0 } }

function render() {
  return mount(WorkspaceSummaryMetrics, { global: { stubs: { NAlert: { template: '<div class="summary-alert"><slot /></div>' } } } })
}

describe('workspaceSummaryMetrics', () => {
  beforeEach(() => {
    vi.mocked(getWorkspaceSummary).mockReset()
  })

  it('retries once on timeout and shows real counts without the timeout alert', async () => {
    vi.mocked(getWorkspaceSummary).mockRejectedValueOnce(timeout).mockResolvedValueOnce(ok)
    const wrapper = render()
    await flushPromises()
    expect(getWorkspaceSummary).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.summary-alert').exists()).toBe(false)
    expect(wrapper.vm.summary.todoCount).toBe(19)
    wrapper.unmount()
  })

  it('shows the timeout alert only after the retry also times out', async () => {
    vi.mocked(getWorkspaceSummary).mockImplementation(() => Promise.reject(timeout))
    const wrapper = render()
    await flushPromises()
    expect(getWorkspaceSummary).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.summary-alert').text()).toContain('响应超时')
    wrapper.unmount()
  })

  it('does not retry non-timeout failures', async () => {
    vi.mocked(getWorkspaceSummary).mockImplementation(() => Promise.reject(Object.assign(new Error('未登录'), { code: 401 })))
    const wrapper = render()
    await flushPromises()
    expect(getWorkspaceSummary).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })

  it('syncs counts after the badge store refreshes following an action', async () => {
    vi.mocked(getWorkspaceSummary).mockResolvedValueOnce({ code: 200, data: { ...ok.data, ccUnreadCount: 1 } })
    const wrapper = render()
    await flushPromises()
    expect(wrapper.vm.summary.ccUnreadCount).toBe(1)
    badgeStore.counts = { ...ok.data, ccUnreadCount: 0 }
    badgeStore.syncedVersion += 1
    await flushPromises()
    expect(wrapper.vm.summary.ccUnreadCount).toBe(0)
    expect(getWorkspaceSummary).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })
})
