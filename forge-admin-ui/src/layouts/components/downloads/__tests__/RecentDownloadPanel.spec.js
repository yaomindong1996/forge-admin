import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive, ref } from 'vue'
import RecentDownloadPanel from '../RecentDownloadPanel.vue'
import RecentDownloadRow from '../RecentDownloadRow.vue'

const fixture = vi.hoisted(() => ({ store: null }))
vi.mock('@/stores/system/recentDownloadsStore', () => ({ useRecentDownloadsStore: () => fixture.store }))
vi.mock('@/composables/useDict', () => ({ useDict: () => ({
  dict: ref({ sys_file_download_source: [{ value: 'export', label: '导出' }, { value: 'file', label: '文件' }] }),
}) }))

const button = { props: ['disabled'], template: '<button :disabled="disabled"><slot /></button>' }
const globals = { stubs: {
  NButton: button,
  NPopconfirm: { template: '<div><slot name="trigger" /></div>' },
  NSkeleton: { template: '<span />' },
  NProgress: { props: ['percentage'], template: '<div role="progressbar" :aria-valuenow="percentage" />' },
} }
const saved = { id: '1', fileId: 'file-1', fileName: '设计测试.xlsx', source: 'export', fileSize: 1024, createTime: '2026-10-17T10:15:00' }

beforeEach(() => {
  fixture.store = reactive({ groups: [], tasks: [], loading: false, error: '', canClear: false, hasMore: false, activeCount: 0, seeAll: false, refresh: vi.fn(), download: vi.fn(), clear: vi.fn() })
})

describe('统一最近下载面板', () => {
  it('区分骨架、空态及失败重试，不伪造示例文件', async () => {
    const wrapper = mount(RecentDownloadPanel, { global: globals })
    expect(wrapper.text()).toContain('暂无下载记录')
    fixture.store.loading = true
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[aria-label="正在加载下载记录"]').exists()).toBe(true)
    fixture.store.loading = false
    fixture.store.error = '下载记录加载失败，请重试'
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[role="alert"]').text()).toContain('下载记录加载失败')
    await wrapper.find('[role="alert"] button').trigger('click')
    expect(fixture.store.refresh).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })

  it('真实记录显示来源字典、时间、大小以及受控的再次下载', async () => {
    const wrapper = mount(RecentDownloadRow, { props: { row: saved }, global: globals })
    expect(wrapper.text()).toContain('设计测试.xlsx')
    expect(wrapper.text()).toContain('导出')
    expect(wrapper.text()).toContain('1 KB')
    expect(wrapper.find('.downloads-file-meta').text()).not.toContain('T10')
    await wrapper.get('button').trigger('click')
    expect(fixture.store.download).toHaveBeenCalledWith(saved)
    wrapper.unmount()
  })

  it('历史导出没有 fileId 时不提供虚假下载按钮', () => {
    const wrapper = mount(RecentDownloadRow, { props: { row: { ...saved, fileId: null } }, global: globals })
    expect(wrapper.text()).toContain('请回原页面重新导出')
    expect(wrapper.find('button').exists()).toBe(false)
    wrapper.unmount()
  })

  it('有实际总长度才显示进度，接收中禁止重复点击', async () => {
    const row = reactive({ ...saved, phase: 'receiving', loaded: 30, runtime: true })
    fixture.store.tasks = [row]
    const wrapper = mount(RecentDownloadRow, { props: { row }, global: globals })
    expect(wrapper.find('[role="progressbar"]').exists()).toBe(false)
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
    row.total = 100
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[role="progressbar"]').attributes('aria-valuenow')).toBe('30')
    wrapper.unmount()
  })

  it('面板 Escape 发出关闭事件，管理员身份与普通用户文案有区别', async () => {
    fixture.store.seeAll = true
    const wrapper = mount(RecentDownloadPanel, { global: globals })
    expect(wrapper.text()).toContain('本租户的下载记录')
    await wrapper.get('[role="dialog"]').trigger('keydown', { key: 'Escape' })
    expect(wrapper.emitted('close')).toHaveLength(1)
    wrapper.unmount()
  })
})
