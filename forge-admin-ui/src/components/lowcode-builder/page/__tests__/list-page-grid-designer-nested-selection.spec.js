import fs from 'node:fs'
import path from 'node:path'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { useListDesignerStore } from '@/store'
import ListPageGridDesigner from '../ListPageGridDesigner.vue'

vi.mock('@/router', () => ({ default: { currentRoute: { value: { query: {} } } } }))

function readSource(relativePath) {
  return fs.readFileSync(path.resolve(process.cwd(), relativePath), 'utf8')
}

function createTabsLayout() {
  return {
    cols: 12,
    rowHeight: 32,
    gap: 8,
    designWidth: 1366,
    layoutType: 'simple-crud',
    items: [
      {
        id: 'tabs_root',
        blockType: 'tabs',
        label: 'Tabs 标签页',
        gridX: 0,
        gridY: 0,
        gridW: 12,
        gridH: 6,
        props: {
          tabs: [
            {
              key: 'tab1',
              title: '标签一',
              children: [
                {
                  id: 'info_child',
                  blockType: 'info-panel',
                  label: 'Tabs 内提示',
                  gridX: 0,
                  gridY: 0,
                  gridW: 12,
                  gridH: 2,
                  props: {
                    title: '提示信息',
                    content: '嵌套内容',
                    type: 'info',
                  },
                },
              ],
            },
          ],
        },
      },
    ],
  }
}

/**
 * ListPageGridDesigner 嵌套选中：组件 mount 覆盖主路径，源码契约覆盖 hydrate/样式硬约束。
 */
describe('list page grid designer nested selection', () => {
  it('invalid and removed selections leave an empty panel without reading a null block', async () => {
    const wrapper = mount(ListPageGridDesigner, {
      props: { modelValue: createTabsLayout(), panelOnly: true, activeBlockId: 'missing' },
      global: {
        plugins: [createPinia()],
        stubs: { FieldConfigDrawer: true, WidgetDataBindingEditor: true },
      },
    })
    await nextTick()
    expect(wrapper.find('.property-empty').exists()).toBe(true)
    await wrapper.setProps({ activeBlockId: 'info_child' })
    expect(wrapper.find('.prop-title').text()).toBe('提示面板')
    await wrapper.setProps({ modelValue: { ...createTabsLayout(), items: [] } })
    await nextTick()
    expect(wrapper.find('.property-empty').exists()).toBe(true)
    wrapper.unmount()
  })

  it('opens the property panel for a child nested inside Tabs', async () => {
    const wrapper = mount(ListPageGridDesigner, {
      props: {
        modelValue: createTabsLayout(),
        modelSchema: {},
        fields: [],
        panelOnly: true,
        activeBlockId: 'info_child',
      },
      global: {
        plugins: [createPinia()],
        // 只验证真实选择/属性面板；数据绑定编辑器的消息 Provider 和请求有独立用例。
        stubs: { FieldConfigDrawer: true, WidgetDataBindingEditor: true },
      },
    })

    // 首次选择之后，布局更新不能反复用旧 activeBlockId 覆盖用户当前选择。
    useListDesignerStore().selectedBlockId = 'tabs_root'
    await wrapper.setProps({ modelValue: { ...createTabsLayout(), gap: 10 } })
    await nextTick()
    expect(wrapper.find('.prop-title').text()).toBe('Tabs 标签页')

    await wrapper.setProps({ activeBlockId: 'tabs_root' })
    await nextTick()
    expect(wrapper.find('.prop-title').text()).toBe('Tabs 标签页')
    wrapper.unmount()
  })

  it('panel-only activeBlockId sync forces on id change and preserves selection on layout echo', () => {
    const source = readSource('src/components/lowcode-builder/page/composables/useListPageGridDesigner.part1.js')
    expect(source).toContain('function syncActiveBlockSelection')
    expect(source).toContain('findBlockInTree(blocks.value, blockId)')
    expect(source).toContain('syncActiveBlockSelection({ force: true })')
    expect(source).toContain('syncActiveBlockSelection({ force: false })')
    expect(source).toMatch(/watch\(\s*\(\)\s*=>\s*props\.activeBlockId/)
    expect(source).toMatch(/watch\(\s*blocks\s*,/)
  })

  it('hydrateInitialState re-syncs active block after layout apply', () => {
    const source = readSource('src/components/lowcode-builder/page/composables/useListPageGridDesigner.part1.js')
    expect(source).toMatch(/async function hydrateInitialState[\s\S]*syncActiveBlockSelection\(\{\s*force:\s*true\s*\}\)/)
  })

  it('nested container child style uses pixel height instead of top-level 100%', () => {
    const source = readSource('src/components/lowcode-builder/page/composables/useListPageGridDesigner.part3.js')
    // createContainerChildBlock 必须写死 height: childHeight，禁止 child.props.style.height || childHeight
    // （默认 style.height 为 '100%'，truthy 会把嵌套子块锁成错误固定高）
    expect(source).toMatch(/function createContainerChildBlock[\s\S]*height:\s*childHeight/)
    expect(source).not.toMatch(/height:\s*child\.props\?\.style\?\.height\s*\|\|\s*childHeight/)
    expect(source).toContain('顶层默认 height 是 100%')
  })
})
