import { h } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'

const typeIcons = {
  1: 'i-lucide:folder',
  2: 'i-lucide:panel-top',
  3: 'i-lucide:mouse-pointer-2',
  4: 'i-lucide:braces',
}

export function getResourceTypeConfig(type) {
  return { icon: typeIcons[Number(type)] || 'i-lucide:layers' }
}

export function renderNavigationLabel({ option }) {
  return h('div', { class: 'nav-tree-label' }, [
    h('span', { 'class': 'nav-tree-icon-shell', 'aria-hidden': true }, [
      h(IconRenderer, { icon: getResourceTypeConfig(option.resourceType).icon, fontSize: 15 }),
    ]),
    h('span', { class: 'nav-tree-name' }, option.label),
  ])
}

export function buildNavigationTree(list = [], keyword = '') {
  return (Array.isArray(list) ? list : []).map((item) => {
    const type = Number(item.resourceType)
    const children = buildNavigationTree(item.children || [], keyword)
    const matched = !keyword || [item.resourceName, item.path]
      .some(value => String(value || '').toLowerCase().includes(keyword))
    if (![1, 2].includes(type) && !children.length) {
      return null
    }
    if (keyword && !matched && !children.length) {
      return null
    }
    return { ...item, key: item.id, label: item.resourceName, children }
  }).filter(Boolean)
}
