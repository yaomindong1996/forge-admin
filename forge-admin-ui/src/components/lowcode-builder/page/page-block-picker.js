/**
 * 自由布局「添加组件」与容器右键「插入组件」共用的目录过滤 / 分组。
 * 保证两处可选组件与分组一致，避免右键菜单漏掉新挂件。
 */
import { listPageBlockCatalog } from './page-schema-catalog.js'

/** 与加号弹层一致的分组顺序 */
export const PAGE_BLOCK_PICKER_GROUPS = Object.freeze([
  { key: 'list', label: '数据' },
  { key: 'chart', label: '图表' },
  { key: 'view', label: '展示' },
  { key: 'other', label: '其他' },
])

/**
 * 组内常用组件靠前（工作台高频：待办、公告、指标、排行、图表、CRUD、布局容器）。
 * 未列入的按原 catalog 顺序排在后面。
 */
export const FREQUENT_PAGE_BLOCK_TYPES = Object.freeze([
  'workflow-task-list',
  'notice-board',
  'welcome-banner',
  'workspace-summary-metrics',
  'stats-strip',
  'top-ranking',
  'quick-entry',
  'bar-chart',
  'line-chart',
  'area-chart',
  'pie-chart',
  'AiCrudPage',
  'AiTable',
  'AiForm',
  'card',
  'panel-box',
  'grid-layout',
  'box-layout',
  'tabs',
  'page-title',
  'info-panel',
  'custom-html',
])

/**
 * 默认不在容器内再嵌套的布局容器。
 * 栅格可插入到卡片/面板/盒子；标签页仍排除，避免深层结构失控。
 * 栅格格子内嵌套栅格由容器侧动态 exclude（见 GridBlockRenderer）。
 */
export const DEFAULT_NEST_EXCLUDE_BLOCK_TYPES = Object.freeze([
  'tabs',
])

/** 已在栅格/标签页内时，禁止再嵌套同级布局容器 */
export const GRID_CELL_NEST_EXCLUDE_BLOCK_TYPES = Object.freeze([
  'grid-layout',
  'tabs',
])

/**
 * 与加号弹层 resolveComponentPickerGroup 同源，保证分组一致。
 * @param {{ blockType?: string, title?: string, group?: string }} item
 * @returns {'list' | 'chart' | 'view' | 'other'}
 */
export function resolvePageBlockPickerGroup(item = {}) {
  const descriptor = `${item.blockType || ''} ${item.title || ''} ${item.group || ''}`.toLowerCase()
  if (/chart|gauge|stat|progress|趋势|图表|指标|饼|面积|排行/.test(descriptor))
    return 'chart'
  if (/crud|table|list|search|列表|查询|数据|待办|通知|公告|欢迎/.test(descriptor))
    return 'list'
  if (/title|text|html|layout|tabs|divider|view|card|panel|box|标题|文本|布局|标签|分割|卡片|面板|盒子|容器/.test(descriptor))
    return 'view'
  return 'other'
}

function frequentRank(blockType = '') {
  const idx = FREQUENT_PAGE_BLOCK_TYPES.indexOf(String(blockType || ''))
  return idx >= 0 ? idx : FREQUENT_PAGE_BLOCK_TYPES.length + 1
}

/**
 * 组内排序：常用优先，其余保持 catalog 相对顺序。
 * @param {Array} items
 */
export function sortPageBlockPickerItems(items = []) {
  return [...items].sort((a, b) => {
    const rankDiff = frequentRank(a?.blockType) - frequentRank(b?.blockType)
    if (rankDiff !== 0)
      return rankDiff
    return String(a?.title || '').localeCompare(String(b?.title || ''), 'zh-CN')
  })
}

/**
 * 过滤可插入/可添加的页面区块（隐藏、onlyFor、去重）。
 * @param {{ keyword?: string, excludeBlockTypes?: string[], allowOnlyFor?: string[] }} [options]
 */
export function listInsertablePageBlocks(options = {}) {
  const keyword = String(options.keyword || '').trim().toLowerCase()
  const exclude = new Set((options.excludeBlockTypes || []).map(String))
  const allowOnlyFor = Array.isArray(options.allowOnlyFor) && options.allowOnlyFor.length
    ? options.allowOnlyFor
    : ['simple-crud']
  const seen = new Set()

  const filtered = listPageBlockCatalog.filter((item) => {
    const type = String(item?.blockType || '').trim()
    if (!type || seen.has(type) || exclude.has(type))
      return false
    if (item.hidden)
      return false
    if (item.onlyFor && !item.onlyFor.some(key => allowOnlyFor.includes(key)))
      return false
    seen.add(type)
    if (!keyword)
      return true
    return `${item.title || ''}${item.techTitle || ''}${item.desc || ''}${item.blockType || ''}`
      .toLowerCase()
      .includes(keyword)
  })
  return sortPageBlockPickerItems(filtered)
}

/**
 * 按加号弹层分组结构输出插入目录。
 * @param {{ keyword?: string, excludeBlockTypes?: string[], allowOnlyFor?: string[] }} [options]
 * @returns {Array<{ key: string, label: string, items: Array }>}
 */
export function buildPageBlockPickerGroups(options = {}) {
  const items = listInsertablePageBlocks(options)
  return PAGE_BLOCK_PICKER_GROUPS
    .map(group => ({
      ...group,
      items: sortPageBlockPickerItems(
        items.filter(item => resolvePageBlockPickerGroup(item) === group.key),
      ),
    }))
    .filter(group => group.items.length)
}
