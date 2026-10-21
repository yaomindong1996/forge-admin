/**
 * 各组件真正可赋值的槽位定义。
 * WidgetDataBindingEditor 只展示当前组件的槽位，避免「显示文本/值/标题/描述」四件套对不上组件。
 */

/** @typedef {{ key: string, label: string, placeholder?: string, primary?: boolean, fallbackProp?: string }} WidgetBindSlot */
/** @typedef {{ kind: 'object' | 'list', supportsCompose?: boolean, needsContextPath?: boolean, contextPathLabel?: string, contextPathHelp?: string, primarySlot?: string, slots: WidgetBindSlot[] }} WidgetBindProfile */

/** @type {Record<string, WidgetBindProfile>} */
export const WIDGET_BINDING_SLOTS = {
  statistic: {
    kind: 'object',
    supportsCompose: true,
    needsContextPath: false,
    primarySlot: 'valueField',
    slots: [
      { key: 'titleField', label: '标题', placeholder: '如 title / name', fallbackProp: 'title' },
      { key: 'valueField', label: '数值', placeholder: '如 amount / total', primary: true, fallbackProp: 'value' },
      { key: 'descriptionField', label: '说明', placeholder: '如 description', fallbackProp: 'description' },
      { key: 'metaField', label: '趋势', placeholder: '如 trend / growthRate', fallbackProp: 'trend' },
    ],
  },
  'stats-strip': {
    kind: 'list',
    supportsCompose: false,
    needsContextPath: true,
    contextPathLabel: '指标列表字段',
    contextPathHelp: '选「列表循环」时使用：从子表/数组字段循环渲染多张指标卡。',
    supportsRenderMode: true,
    renderModeOptions: [
      { label: '手动多项', value: 'manual', help: '自己增删指标卡，每项可写死或点选字段' },
      { label: '列表循环', value: 'list', help: '从一个数组字段循环出多张卡' },
    ],
    primarySlot: 'valueField',
    slots: [
      { key: 'labelField', label: '指标名', placeholder: 'label / name' },
      { key: 'valueField', label: '数值', placeholder: 'value / count', primary: true },
      { key: 'metaField', label: '趋势', placeholder: 'trend / growth' },
    ],
  },
  'quick-entry': {
    kind: 'list',
    supportsCompose: false,
    needsContextPath: true,
    contextPathLabel: '入口列表字段',
    contextPathHelp: '选「列表循环」时使用：从数组字段循环渲染入口卡片。',
    supportsRenderMode: true,
    renderModeOptions: [
      { label: '手动多项', value: 'manual', help: '自己增删入口，配置标题与跳转页' },
      { label: '列表循环', value: 'list', help: '从一个数组字段循环出入口卡' },
    ],
    primarySlot: 'titleField',
    slots: [
      { key: 'titleField', label: '标题', placeholder: 'title / name', primary: true },
      { key: 'descriptionField', label: '描述', placeholder: 'description / desc' },
      { key: 'valueField', label: '目标页', placeholder: 'targetPageId / pageId' },
      { key: 'metaField', label: '图标', placeholder: 'icon' },
    ],
  },
  'bar-chart': {
    kind: 'list',
    supportsCompose: false,
    needsContextPath: true,
    contextPathLabel: '图表数据列表',
    contextPathHelp: '选「列表循环」时：每行映射分类、数值，可选系列名做多系列。',
    supportsRenderMode: true,
    renderModeOptions: [
      { label: '手动数据', value: 'manual', help: '在属性里填写分类轴与系列数值' },
      { label: '列表绑定', value: 'list', help: '从数组字段聚合为图表数据' },
    ],
    primarySlot: 'valueField',
    slots: [
      { key: 'labelField', label: '分类轴', placeholder: 'category / name' },
      { key: 'valueField', label: '数值', placeholder: 'value / count', primary: true },
      { key: 'metaField', label: '系列名', placeholder: 'series / group' },
    ],
  },
  'line-chart': {
    kind: 'list',
    supportsCompose: false,
    needsContextPath: true,
    contextPathLabel: '图表数据列表',
    contextPathHelp: '选「列表循环」时：每行映射分类、数值，可选系列名做多系列。',
    supportsRenderMode: true,
    renderModeOptions: [
      { label: '手动数据', value: 'manual', help: '在属性里填写分类轴与系列数值' },
      { label: '列表绑定', value: 'list', help: '从数组字段聚合为图表数据' },
    ],
    primarySlot: 'valueField',
    slots: [
      { key: 'labelField', label: '分类轴', placeholder: 'category / name' },
      { key: 'valueField', label: '数值', placeholder: 'value / count', primary: true },
      { key: 'metaField', label: '系列名', placeholder: 'series / group' },
    ],
  },
  'info-panel': {
    kind: 'object',
    supportsCompose: true,
    primarySlot: 'contentField',
    slots: [
      { key: 'titleField', label: '标题', placeholder: 'title', fallbackProp: 'title' },
      { key: 'contentField', label: '内容', placeholder: 'content / message', primary: true, fallbackProp: 'content' },
      { key: 'valueField', label: '类型', placeholder: 'type（info/success/warning/error）', fallbackProp: 'type' },
    ],
  },
  'custom-html': {
    kind: 'object',
    supportsCompose: true,
    primarySlot: 'contentField',
    slots: [
      { key: 'titleField', label: '标题', placeholder: 'title', fallbackProp: 'title' },
      { key: 'contentField', label: '内容', placeholder: 'content / html', primary: true, fallbackProp: 'content' },
    ],
  },
  'tag-list': {
    kind: 'list',
    needsContextPath: true,
    contextPathLabel: '标签列表字段',
    contextPathHelp: '选存放标签数组的字段（如 tags），再映射每条的文本和类型。',
    slots: [
      { key: 'labelField', label: '标签文本', placeholder: 'label / name' },
      { key: 'valueField', label: '标签类型', placeholder: 'type / status' },
    ],
  },
  steps: {
    kind: 'list',
    needsContextPath: true,
    contextPathLabel: '步骤列表字段',
    contextPathHelp: '选存放步骤数组的字段，再映射标题和描述。',
    slots: [
      { key: 'titleField', label: '步骤标题', placeholder: 'title' },
      { key: 'descriptionField', label: '步骤描述', placeholder: 'description' },
    ],
  },
  timeline: {
    kind: 'list',
    needsContextPath: true,
    contextPathLabel: '节点数组字段',
    contextPathHelp: '先选子表/数组（如操作记录列表），再在下方给「每条节点」点选标题、内容、时间字段（相对该数组）。',
    slots: [
      { key: 'titleField', label: '节点标题', placeholder: 'title' },
      { key: 'descriptionField', label: '节点内容', placeholder: 'content / description' },
      { key: 'metaField', label: '时间', placeholder: 'time / date' },
    ],
  },
  'empty-state': {
    kind: 'object',
    supportsCompose: true,
    primarySlot: 'descriptionField',
    slots: [
      { key: 'titleField', label: '标题', placeholder: 'title', fallbackProp: 'title' },
      { key: 'descriptionField', label: '描述', placeholder: 'description', primary: true, fallbackProp: 'description' },
      { key: 'valueField', label: '按钮文案', placeholder: 'actionText', fallbackProp: 'actionText' },
    ],
  },
  'text-title': {
    kind: 'object',
    supportsCompose: true,
    primarySlot: 'titleField',
    slots: [
      { key: 'titleField', label: '标题', placeholder: 'text / title', primary: true, fallbackProp: 'text' },
      { key: 'descriptionField', label: '副标题', placeholder: 'subtitle', fallbackProp: 'subtitle' },
    ],
  },
  paragraph: {
    kind: 'object',
    supportsCompose: true,
    primarySlot: 'contentField',
    slots: [
      { key: 'contentField', label: '段落内容', placeholder: 'content / text', primary: true, fallbackProp: 'content' },
    ],
  },
  link: {
    kind: 'object',
    supportsCompose: true,
    primarySlot: 'titleField',
    slots: [
      { key: 'titleField', label: '链接文本', placeholder: 'text / label', primary: true, fallbackProp: 'text' },
      { key: 'valueField', label: '链接地址', placeholder: 'href / url', fallbackProp: 'href' },
    ],
  },
  'text-tip': {
    kind: 'object',
    supportsCompose: true,
    primarySlot: 'contentField',
    slots: [
      { key: 'titleField', label: '标题', placeholder: 'title', fallbackProp: 'title' },
      { key: 'contentField', label: '内容', placeholder: 'content', primary: true, fallbackProp: 'content' },
      { key: 'valueField', label: '类型', placeholder: 'type', fallbackProp: 'type' },
    ],
  },
  'audio-player': {
    kind: 'object',
    primarySlot: 'valueField',
    slots: [
      { key: 'titleField', label: '标题', placeholder: 'title', fallbackProp: 'title' },
      { key: 'valueField', label: '音频地址', placeholder: 'src / url', primary: true, fallbackProp: 'src' },
    ],
  },
  'video-player': {
    kind: 'object',
    primarySlot: 'valueField',
    slots: [
      { key: 'titleField', label: '标题', placeholder: 'title', fallbackProp: 'title' },
      { key: 'valueField', label: '视频地址', placeholder: 'src / url', primary: true, fallbackProp: 'src' },
      { key: 'metaField', label: '封面', placeholder: 'poster', fallbackProp: 'poster' },
    ],
  },
  avatar: {
    kind: 'object',
    supportsCompose: true,
    primarySlot: 'titleField',
    slots: [
      { key: 'titleField', label: '名称', placeholder: 'name', primary: true, fallbackProp: 'name' },
      { key: 'descriptionField', label: '描述', placeholder: 'description / role', fallbackProp: 'description' },
      { key: 'valueField', label: '头像地址', placeholder: 'src / avatar', fallbackProp: 'src' },
    ],
  },
}

const DEFAULT_OBJECT_PROFILE = {
  kind: 'object',
  supportsCompose: true,
  primarySlot: 'valueField',
  slots: [
    { key: 'valueField', label: '值', placeholder: 'value', primary: true },
    { key: 'titleField', label: '标题', placeholder: 'title' },
    { key: 'contentField', label: '内容', placeholder: 'content' },
    { key: 'descriptionField', label: '描述', placeholder: 'description' },
  ],
}

/** 历史默认映射名：未真正点选字段时不算「已选」 */
export const CLASSIC_DEFAULT_BINDING_PATHS = new Set([
  'label', 'value', 'title', 'description', 'content', 'meta', 'trend',
  'type', 'text', 'name', 'src', 'href', 'poster', 'subtitle', 'actionText',
])

/**
 * 指标卡等列表组件的渲染模式：manual=手动多项，list=数组循环
 */
export function resolveWidgetRenderMode(binding = {}, blockType = '') {
  const profile = resolveWidgetBindingProfile(blockType)
  if (profile.supportsRenderMode) {
    // 默认手动多项，避免一开数据源就把多张卡吃成「列表循环」
    return binding.renderMode === 'list' ? 'list' : 'manual'
  }
  if (profile.kind === 'list') {
    if (binding.enabled === true && binding.sourceType && binding.sourceType !== 'static')
      return 'list'
    return 'manual'
  }
  return 'manual'
}

/**
 * @param {string} blockType
 * @returns {WidgetBindProfile}
 */
export function resolveWidgetBindingProfile(blockType = '') {
  return WIDGET_BINDING_SLOTS[blockType] || DEFAULT_OBJECT_PROFILE
}

/**
 * 槽位是否展示。默认展示；slotHidden[key]=true 时隐藏。
 */
export function isBindingSlotVisible(binding = {}, slotKey = '') {
  if (!slotKey)
    return true
  const hidden = binding.slotHidden
  if (!hidden || typeof hidden !== 'object')
    return true
  return hidden[slotKey] !== true
}

/**
 * 路径是否为用户真正点选的字段（排除历史默认映射名）
 */
export function isExplicitBindingPath(path = '', fields = []) {
  const normalized = String(path || '').trim()
  if (!normalized)
    return false
  const catalogPaths = new Set(
    (Array.isArray(fields) ? fields : [])
      .map(item => String(item?.fieldCode || item?.field || item?.path || '').trim())
      .filter(Boolean),
  )
  return catalogPaths.has(normalized)
    || normalized.includes('.')
    || !CLASSIC_DEFAULT_BINDING_PATHS.has(normalized)
}

/**
 * 已选槽位摘要：[{ label, path }]
 */
export function listAssignedBindingSlots(binding = {}, blockType = '', fields = []) {
  const profile = resolveWidgetBindingProfile(blockType)
  return (profile.slots || [])
    .map((slot) => {
      const path = String(binding[slot.key] || '').trim()
      if (!isExplicitBindingPath(path, fields))
        return null
      return { key: slot.key, label: slot.label, path, primary: Boolean(slot.primary) }
    })
    .filter(Boolean)
}

/**
 * 组件内容区：哪些静态属性在绑定启用时应降级为「占位默认值」
 */
export function resolveBoundFallbackProps(blockType = '') {
  const profile = resolveWidgetBindingProfile(blockType)
  return (profile.slots || [])
    .map(slot => slot.fallbackProp)
    .filter(Boolean)
}

/**
 * 收集绑定用到的字段路径（含组合显示 / 列表路径）
 */
export function collectBindingFieldPaths(binding = {}, blockType = '', fields = []) {
  const profile = resolveWidgetBindingProfile(blockType)
  const paths = new Set()
  const push = (path) => {
    const normalized = String(path || '').trim()
    if (normalized && isExplicitBindingPath(normalized, fields))
      paths.add(normalized)
  }
  push(binding.contextPath)
  ;(profile.slots || []).forEach(slot => push(binding[slot.key]))
  ;(Array.isArray(binding.displayFields) ? binding.displayFields : []).forEach(push)
  const template = String(binding.displayTemplate || '')
  template.replace(/\{([^{}]+)\}/g, (_, token) => {
    push(token)
    return ''
  })
  return [...paths]
}

/**
 * 当前详情记录是否已能解析出绑定值；否则应用预览样例。
 */
export function hasResolvedBindingValues(record = {}, binding = {}, blockType = '', fields = [], getNestedValue = getNestedRecordValue) {
  if (!record || typeof record !== 'object')
    return false
  const paths = collectBindingFieldPaths(binding, blockType, fields)
  if (!paths.length)
    return Object.keys(record).length > 0
  return paths.some((path) => {
    const value = getNestedValue(record, path)
    return value !== undefined && value !== null && value !== ''
  })
}

/**
 * 设计/预览无真实详情时，按已绑字段生成样例数据，避免仍显示静态占位。
 */
export function buildBindingPreviewRecord(binding = {}, fields = [], blockType = '') {
  const profile = resolveWidgetBindingProfile(blockType)
  const record = {}
  const fieldMap = new Map(
    (Array.isArray(fields) ? fields : [])
      .map((item) => {
        const code = String(item?.fieldCode || item?.field || item?.path || '').trim()
        return code ? [code, item] : null
      })
      .filter(Boolean),
  )

  const assigned = listAssignedBindingSlots(binding, blockType, fields)
  if (profile.kind === 'list') {
    const listPath = String(binding.contextPath || '').trim()
    const row = {}
    assigned.forEach((slot, index) => {
      const boundPath = String(binding[slot.key] || '').trim()
      const leaf = boundPath.includes('.') ? boundPath.split('.').at(-1) : boundPath
      const rowKey = leaf || (
        slot.key === 'labelField'
          ? 'label'
          : slot.key === 'metaField'
            ? 'trend'
            : slot.key === 'descriptionField'
              ? 'description'
              : slot.key === 'titleField'
                ? 'title'
                : 'value'
      )
      row[rowKey] = buildSampleValueForPath(boundPath || rowKey, fieldMap.get(boundPath) || fieldMap.get(leaf), index, slot.label)
    })
    if (!Object.keys(row).length) {
      row.label = '示例指标'
      row.value = '1,280'
      row.trend = '+8%'
      row.title = '示例节点'
      row.description = '示例描述'
      row.content = '示例内容'
      row.time = '2026-05-21'
      row.type = 'default'
    }
    if (listPath) {
      setNestedRecordValue(record, listPath, [
        row,
        {
          ...row,
          label: row.label ? `${row.label}2` : row.label,
          title: row.title ? `${row.title}2` : row.title,
          value: row.value === '1,280' ? '860' : row.value,
        },
      ])
      return record
    }
    return [row]
  }

  assigned.forEach((slot, index) => {
    setNestedRecordValue(
      record,
      slot.path,
      buildSampleValueForPath(slot.path, fieldMap.get(slot.path), index, slot.label),
    )
  })
  ;(Array.isArray(binding.displayFields) ? binding.displayFields : []).forEach((path, index) => {
    if (!isExplicitBindingPath(path, fields))
      return
    if (getNestedRecordValue(record, path) !== undefined)
      return
    setNestedRecordValue(record, path, buildSampleValueForPath(path, fieldMap.get(path), index, path))
  })
  return record
}

export function setNestedRecordValue(target = {}, path = '', value) {
  const parts = String(path || '').split('.').map(item => item.trim()).filter(Boolean)
  if (!parts.length)
    return target
  let current = target
  for (let index = 0; index < parts.length - 1; index += 1) {
    const key = parts[index]
    if (!current[key] || typeof current[key] !== 'object' || Array.isArray(current[key]))
      current[key] = {}
    current = current[key]
  }
  current[parts[parts.length - 1]] = value
  return target
}

export function getNestedRecordValue(source, path = '') {
  if (!path)
    return source
  return String(path).split('.').reduce((current, key) => {
    if (current === null || current === undefined)
      return undefined
    return current[key]
  }, source)
}

function buildSampleValueForPath(path = '', field = null, index = 0, slotLabel = '') {
  const label = String(field?.label || field?.fieldName || slotLabel || path || '示例').trim() || '示例'
  const dataType = String(field?.dataType || field?.fieldType || field?.type || '').toLowerCase()
  if (field?.dictType)
    return `${label}示例`
  if (['int', 'bigint', 'decimal', 'number', 'money', 'amount'].includes(dataType) || /count|amount|total|qty|num|数量|金额|总数/i.test(path))
    return dataType === 'decimal' ? `${1280 + index}.50` : String(1280 + index * 36)
  if (dataType.includes('date'))
    return dataType.includes('time') ? '2026-05-21 09:30:00' : '2026-05-21'
  if (/trend|growth|rate|涨跌|趋势/i.test(path) || slotLabel === '趋势')
    return index % 2 === 0 ? '+12.5%' : '-3.2%'
  if (/type|status/i.test(path) && slotLabel === '类型')
    return 'info'
  return `${label}示例`
}
