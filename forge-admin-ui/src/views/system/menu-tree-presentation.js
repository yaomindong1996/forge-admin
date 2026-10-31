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
    h('span', { class: 'nav-tree-name', title: option.label }, option.label),
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
    // 无下级时不要保留空 children[]，否则 n-tree 仍显示展开箭头误导可展开
    const hasChildren = children.length > 0
    return {
      ...item,
      key: item.id,
      label: item.resourceName,
      children: hasChildren ? children : undefined,
      isLeaf: !hasChildren,
    }
  }).filter(Boolean)
}
