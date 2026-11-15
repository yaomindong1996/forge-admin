import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import VersionCompare from '../versionCompare.vue'

const getVersionList = vi.fn()
vi.mock('@/api/version', () => ({
  default: {
    getVersionList: (...args) => getVersionList(...args),
    compareVersions: vi.fn(),
  },
}))

vi.mock('naive-ui', () => ({
  NModal: { template: '<div class="n-modal"><slot /></div>' },
  NSpace: { template: '<div><slot /></div>' },
  NSelect: {
    props: ['value', 'options', 'placeholder'],
    template: '<div class="n-select" :data-value="value" :data-placeholder="placeholder">{{ value }}</div>',
  },
  NButton: { template: '<button><slot /></button>' },
  NDivider: { template: '<hr />' },
  NH3: { template: '<h3><slot /></h3>' },
  NEmpty: { template: '<div />' },
  NText: { template: '<span><slot /></span>' },
  NList: { template: '<ul><slot /></ul>' },
  NListItem: { template: '<li><slot /></li>' },
}))

describe('versionCompare preselect', () => {
  beforeEach(() => {
    getVersionList.mockReset()
    getVersionList.mockResolvedValue({
      data: {
        records: [
          { version: 3, versionName: 'v3', publishTime: '2026-11-14' },
          { version: 2, versionName: 'v2', publishTime: '2026-11-14' },
          { version: 1, versionName: 'v1', publishTime: '2026-11-14' },
        ],
      },
    })
  })

  it('打开后预选最新两个版本', async () => {
    const wrapper = mount(VersionCompare, {
      props: { modelId: 1 },
    })
    await flushPromises()
    expect(getVersionList).toHaveBeenCalled()
    expect(wrapper.vm.version1).toBe(3)
    expect(wrapper.vm.version2).toBe(2)
    wrapper.unmount()
  })
})
