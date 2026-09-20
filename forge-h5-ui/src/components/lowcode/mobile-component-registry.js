export const MOBILE_COMPONENT_CAPABILITY = Object.freeze({
  INTERACTIVE: 'interactive',
  ADAPTED: 'adapted',
  READONLY: 'readonly',
  BLOCKED: 'blocked',
})

const INTERACTIVE = MOBILE_COMPONENT_CAPABILITY.INTERACTIVE
const ADAPTED = MOBILE_COMPONENT_CAPABILITY.ADAPTED
const READONLY = MOBILE_COMPONENT_CAPABILITY.READONLY
const BLOCKED = MOBILE_COMPONENT_CAPABILITY.BLOCKED

const aliases = Object.freeze({
  'input-number': 'number',
  inputNumber: 'number',
  integer: 'number',
  upload: 'fileUpload',
  image: 'imageUpload',
  password: 'input',
  datePicker: 'date',
  timePicker: 'time',
  richText: 'rich-text',
  colorPicker: 'color',
  title: 'text-title',
  description: 'paragraph',
  signature: 'signature-pad',
  signaturePad: 'signature-pad',
  pillSelect: 'radioButton',
  sectionTitle: 'formSectionTitle',
  row: 'grid',
  fcRow: 'grid',
  elCard: 'card',
  elTabs: 'tabs',
  elCollapse: 'collapse',
  'field-input': 'input',
  'field-textarea': 'textarea',
  'field-number': 'number',
  'field-select': 'select',
  'field-dict-select': 'dictSelect',
  'field-tree-select': 'treeSelect',
  'field-org-tree-select': 'orgTreeSelect',
  'field-user-select': 'userSelect',
  'field-region-tree-select': 'regionTreeSelect',
  'field-cascader': 'cascader',
  'field-date': 'date',
  'field-datetime': 'datetime',
  'field-switch': 'switch',
  'field-upload': 'fileUpload',
  'field-image-upload': 'imageUpload',
  'grid-layout': 'grid',
  'box-layout': 'box',
  'section-divider': 'formSectionTitle',
  'save-button': 'action-button',
  'detail-field': 'text',
})

function entry(kind, capability, renderer, extra = {}) {
  return Object.freeze({ kind, capability, renderer, ...extra })
}

const registry = {
  input: entry('field', INTERACTIVE, 'text'),
  textarea: entry('field', INTERACTIVE, 'textarea'),
  text: entry('field', READONLY, 'text-display'),
  barcodeScanner: entry('field', ADAPTED, 'barcode-scanner'),
  number: entry('field', INTERACTIVE, 'number'),
  money: entry('field', INTERACTIVE, 'money'),
  slider: entry('field', INTERACTIVE, 'slider'),
  rate: entry('field', INTERACTIVE, 'rate'),
  color: entry('field', ADAPTED, 'color'),
  select: entry('field', INTERACTIVE, 'picker'),
  dictSelect: entry('field', INTERACTIVE, 'picker'),
  radio: entry('field', INTERACTIVE, 'radio'),
  radioButton: entry('field', INTERACTIVE, 'radio-button'),
  checkbox: entry('field', INTERACTIVE, 'checkbox'),
  transfer: entry('field', ADAPTED, 'transfer'),
  cascader: entry('field', ADAPTED, 'cascader'),
  treeSelect: entry('field', ADAPTED, 'tree-select'),
  customSelect: entry('field', ADAPTED, 'remote-select'),
  date: entry('field', INTERACTIVE, 'datetime-picker'),
  datetime: entry('field', INTERACTIVE, 'datetime-picker'),
  time: entry('field', INTERACTIVE, 'datetime-picker'),
  daterange: entry('field', INTERACTIVE, 'datetime-range'),
  datetimerange: entry('field', INTERACTIVE, 'datetime-range'),
  month: entry('field', INTERACTIVE, 'datetime-picker'),
  year: entry('field', INTERACTIVE, 'datetime-picker'),
  timerange: entry('field', INTERACTIVE, 'datetime-range'),
  numberrange: entry('field', INTERACTIVE, 'number-range'),
  range: entry('field', INTERACTIVE, 'number-range'),
  switch: entry('field', INTERACTIVE, 'switch'),
  userSelect: entry('field', ADAPTED, 'entity-select'),
  orgTreeSelect: entry('field', ADAPTED, 'tree-select'),
  regionTreeSelect: entry('field', ADAPTED, 'cascader'),
  objectReference: entry('field', ADAPTED, 'record-selector'),
  recordSelector: entry('field', ADAPTED, 'record-selector'),
  fileUpload: entry('field', INTERACTIVE, 'file-upload'),
  imageUpload: entry('field', INTERACTIVE, 'image-upload'),
  array: entry('field', ADAPTED, 'array'),

  grid: entry('layout', ADAPTED, 'grid'),
  col: entry('layout', ADAPTED, 'column'),
  table: entry('layout', ADAPTED, 'card-table'),
  tableCell: entry('layout', ADAPTED, 'table-cell'),
  card: entry('layout', INTERACTIVE, 'card'),
  tabs: entry('layout', ADAPTED, 'tabs'),
  tabPane: entry('layout', ADAPTED, 'tab-pane'),
  collapse: entry('layout', INTERACTIVE, 'collapse'),
  collapseItem: entry('layout', INTERACTIVE, 'collapse-item'),
  box: entry('layout', INTERACTIVE, 'box'),
  divider: entry('layout', INTERACTIVE, 'divider'),
  spacer: entry('layout', INTERACTIVE, 'spacer'),
  space: entry('layout', ADAPTED, 'space'),
  groupTitle: entry('layout', READONLY, 'group-title'),
  formSectionTitle: entry('layout', READONLY, 'section-title'),

  AiCrudPage: entry('business', ADAPTED, 'mobile-crud'),
  AiTable: entry('business', ADAPTED, 'card-table'),
  AiForm: entry('business', INTERACTIVE, 'form'),
  subTable: entry('business', ADAPTED, 'sub-table'),
  'search-form': entry('business', ADAPTED, 'search-form'),
  toolbar: entry('business', ADAPTED, 'toolbar'),
  'data-table': entry('business', ADAPTED, 'card-table'),
  'tree-panel': entry('business', ADAPTED, 'tree-panel'),
  'detail-info': entry('business', READONLY, 'descriptions'),
  'step-form': entry('business', ADAPTED, 'step-form'),
  'signature-pad': entry('business', INTERACTIVE, 'signature'),
  'sub-table-tabs': entry('business', ADAPTED, 'sub-table-tabs'),

  'back-button': entry('page', INTERACTIVE, 'back-button'),
  'page-title': entry('page', READONLY, 'page-title'),
  'text-title': entry('page', READONLY, 'text-title'),
  paragraph: entry('page', READONLY, 'paragraph'),
  statistic: entry('page', READONLY, 'statistic'),
  'text-tip': entry('page', READONLY, 'text-tip'),
  'tag-list': entry('page', READONLY, 'tag-list'),

  'audio-player': entry('media', ADAPTED, 'audio'),
  'video-player': entry('media', ADAPTED, 'video'),
  avatar: entry('media', READONLY, 'avatar'),
  barcode: entry('media', ADAPTED, 'barcode'),
  qrcode: entry('media', ADAPTED, 'qrcode'),
  iframe: entry('media', READONLY, 'web-view', { h5Capability: READONLY, miniProgramCapability: BLOCKED }),

  'rich-text': entry('widget', READONLY, 'safe-rich-text'),
  watermark: entry('widget', ADAPTED, 'watermark'),
  'vue-component': entry('widget', BLOCKED, 'blocked-dynamic-component'),
  'html-tag': entry('widget', READONLY, 'safe-rich-text'),
  markdown: entry('widget', READONLY, 'safe-markdown'),
  calendar: entry('widget', ADAPTED, 'calendar'),
  code: entry('widget', READONLY, 'code'),
  countdown: entry('widget', ADAPTED, 'countdown'),
  descriptions: entry('widget', READONLY, 'descriptions'),
  announcement: entry('widget', READONLY, 'announcement'),
  list: entry('widget', READONLY, 'list'),
  log: entry('widget', READONLY, 'log'),
  'number-animation': entry('widget', ADAPTED, 'number-animation'),
  breadcrumb: entry('widget', ADAPTED, 'breadcrumb'),
  menu: entry('widget', ADAPTED, 'menu'),
  pagination: entry('widget', ADAPTED, 'pagination'),
  split: entry('widget', ADAPTED, 'stacked-split'),

  'query-set': entry('action', ADAPTED, 'query-set'),
  'custom-query': entry('action', INTERACTIVE, 'action-button'),
  'import-button': entry('action', ADAPTED, 'action-button'),
  'export-button': entry('action', INTERACTIVE, 'action-button'),
  'add-button': entry('action', INTERACTIVE, 'action-button'),
  'reset-button': entry('action', INTERACTIVE, 'action-button'),
  'action-button': entry('action', INTERACTIVE, 'action-button'),
  'button-group': entry('action', ADAPTED, 'button-group'),
  link: entry('action', INTERACTIVE, 'link'),
  'info-panel': entry('display', READONLY, 'info-panel'),
  steps: entry('display', READONLY, 'steps'),
  timeline: entry('display', READONLY, 'timeline'),
  'empty-state': entry('display', READONLY, 'empty-state'),
  'custom-html': entry('display', READONLY, 'safe-rich-text'),
  'stats-strip': entry('display', READONLY, 'stats-strip'),
}

export const mobileComponentRegistry = Object.freeze(registry)
export const mobileComponentAliases = aliases

const normalizedTypeIndex = Object.freeze([
  ...Object.keys(registry).map(type => [compactType(type), type]),
  ...Object.entries(aliases).map(([alias, type]) => [compactType(alias), type]),
].reduce((result, [key, type]) => {
  result[key] = type
  return result
}, {}))

export function normalizeMobileComponentType(type = '') {
  const raw = String(type || '').trim()
  if (!raw) return ''
  return aliases[raw] || normalizedTypeIndex[compactType(raw)] || raw
}

export function resolveMobileComponent(type = '', options = {}) {
  const normalizedType = normalizeMobileComponentType(type)
  const registered = registry[normalizedType]
  if (!registered) {
    return {
      type: normalizedType || 'unknown',
      sourceType: String(type || ''),
      kind: 'unknown',
      capability: BLOCKED,
      renderer: 'unsupported',
      reason: '移动端尚未登记该组件，已阻止编辑和提交',
    }
  }
  const platform = String(options.platform || '').toLowerCase()
  const platformCapability = platform === 'h5'
    ? registered.h5Capability
    : platform ? registered.miniProgramCapability : undefined
  return {
    type: normalizedType,
    sourceType: String(type || ''),
    ...registered,
    capability: platformCapability || registered.capability,
  }
}

export function isMobileComponentEditable(type = '', options = {}) {
  const capability = resolveMobileComponent(type, options).capability
  return capability === INTERACTIVE || capability === ADAPTED
}

export function listRegisteredMobileComponentTypes() {
  return Object.keys(registry)
}

function compactType(type = '') {
  return String(type || '').replace(/[\s_-]/g, '').toLowerCase()
}
