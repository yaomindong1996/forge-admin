import { getDictData } from '@/composables/useDict'
import { request } from '@/utils'

const FORGE_BUSINESS_MENU = {
  name: 'forgeBusiness',
  title: '业务组件',
  list: [],
}

const DRAG_TAG_BY_COMPONENT = {
  dictSelect: 'forgeDictSelect',
  regionTreeSelect: 'forgeRegionTreeSelect',
  orgTreeSelect: 'forgeOrgTreeSelect',
  userSelect: 'forgeUserSelect',
  fileUpload: 'forgeFileUpload',
  imageUpload: 'forgeImageUpload',
  objectReference: 'forgeObjectReference',
  subTable: 'forgeSubTable',
}

const COMPONENT_BY_DRAG_TAG = Object.entries(DRAG_TAG_BY_COMPONENT).reduce((result, [componentKey, dragTag]) => {
  result[dragTag] = componentKey
  return result
}, {})

let fieldIndex = 0
const installedDesigners = new WeakSet()
const componentRuleVersions = new WeakMap()
const previewOptionCache = new Map()
let dictTypeOptions = []
let dictTypeOptionsLoaded = false
let dictTypeOptionsLoading = null

export function installForgeBusinessComponents(designer) {
  if (!designer)
    return
  if (!installedDesigners.has(designer)) {
    designer.addMenu?.({ ...FORGE_BUSINESS_MENU, list: [] })
    designer.addComponent?.(createForgeBusinessDragRules())
    installedDesigners.add(designer)
  }
  installForgeBusinessComponentRules(designer)
  loadDictTypeOptions().then(() => installForgeBusinessComponentRules(designer))
}

export function resolveDesignerDragTag(componentKey) {
  return DRAG_TAG_BY_COMPONENT[componentKey] || ''
}

export function resolveForgeComponentKeyFromDragTag(dragTag) {
  return COMPONENT_BY_DRAG_TAG[dragTag] || ''
}

export function getDesignPlaceholderOptions() {
  return []
}

export async function hydrateForgeBusinessPreviewRules(rules = []) {
  const nextRules = cloneValue(Array.isArray(rules) ? rules : [])
  const tasks = []
  walkRules(nextRules, (rule) => {
    tasks.push(hydratePreviewRule(rule))
  })
  await Promise.all(tasks)
  return nextRules
}

function createForgeBusinessDragRules() {
  return [
    createSelectDragRule({
      name: DRAG_TAG_BY_COMPONENT.dictSelect,
      label: '字典选择',
      title: '字典选择',
      componentKey: 'dictSelect',
      props: {
        dictType: '',
        placeholder: '请选择字典项',
        clearable: true,
        filterable: true,
      },
      options: getDesignPlaceholderOptions('dictSelect'),
    }),
    createTreeSelectDragRule({
      name: DRAG_TAG_BY_COMPONENT.regionTreeSelect,
      label: '行政区划',
      title: '行政区划',
      componentKey: 'regionTreeSelect',
      props: {
        placeholder: '请选择行政区划',
        clearable: true,
        filterable: true,
        rootCode: '150000',
        dataRight: true,
        virtualDisabled: true,
      },
      data: [],
    }),
    createTreeSelectDragRule({
      name: DRAG_TAG_BY_COMPONENT.orgTreeSelect,
      label: '组织选择',
      title: '组织选择',
      componentKey: 'orgTreeSelect',
      props: {
        placeholder: '请选择组织',
        clearable: true,
        filterable: true,
      },
      data: [],
    }),
    createSelectDragRule({
      name: DRAG_TAG_BY_COMPONENT.userSelect,
      label: '人员选择',
      title: '人员选择',
      componentKey: 'userSelect',
      props: {
        placeholder: '请选择人员',
        clearable: true,
        filterable: true,
        labelValueField: '',
      },
      options: [],
    }),
    createUploadDragRule({
      name: DRAG_TAG_BY_COMPONENT.fileUpload,
      label: '文件上传',
      title: '文件上传',
      componentKey: 'fileUpload',
      props: {
        autoUpload: false,
        limit: 5,
        valueType: 'id',
      },
    }),
    createUploadDragRule({
      name: DRAG_TAG_BY_COMPONENT.imageUpload,
      label: '图片上传',
      title: '图片上传',
      componentKey: 'imageUpload',
      props: {
        autoUpload: false,
        listType: 'picture-card',
        accept: 'image/*',
        limit: 5,
        valueType: 'id',
      },
    }),
    createSelectDragRule({
      name: DRAG_TAG_BY_COMPONENT.objectReference,
      label: '引用对象',
      title: '引用对象',
      componentKey: 'objectReference',
      props: {
        placeholder: '请选择关联数据',
        clearable: true,
        filterable: true,
        referenceObjectCode: '',
        referenceDisplayField: 'name',
      },
      options: getDesignPlaceholderOptions('objectReference'),
    }),
    createSubTableDragRule(),
  ]
}

function createForgeBusinessComponentRules() {
  return {
    [DRAG_TAG_BY_COMPONENT.dictSelect]: () => [
      dictTypeRule(),
      switchRule('multiple', '多选'),
      switchRule('filterable', '可搜索', true),
      switchRule('clearable', '可清空', true),
      selectRule('cascadeConfig>mode', '级联方式', [
        { label: '关联字典', value: 'linkedDict' },
        { label: '字典父子', value: 'parentDictCode' },
        { label: '远程参数', value: 'remoteParam' },
      ]),
      inputRule('cascadeConfig>sourceField', '上级字段', '例如：customerLevel'),
      inputRule('cascadeConfig>sourceDictType', '上级字典类型', '例如：sys_customer_level'),
      inputRule('cascadeConfig>linkedDictType', '关联字典类型', '默认使用上级字典类型'),
      inputRule('cascadeConfig>paramName', '远程参数名', '例如：parentId'),
      switchRule('cascadeConfig>clearOnParentChange', '上级变化清空当前值', true),
    ],
    [DRAG_TAG_BY_COMPONENT.regionTreeSelect]: () => [
      inputRule('rootCode', '根区划编码', '默认 150000'),
      switchRule('dataRight', '启用数据权限', true),
      switchRule('virtualDisabled', '编辑态禁选 ALL 节点', true),
      switchRule('filterable', '可搜索', true),
      switchRule('clearable', '可清空', true),
    ],
    [DRAG_TAG_BY_COMPONENT.orgTreeSelect]: () => [
      switchRule('multiple', '多选'),
      switchRule('filterable', '可搜索', true),
      switchRule('clearable', '可清空', true),
      inputRule('optionSource>api', '组织接口', '留空使用系统组织树'),
      inputRule('optionSource>labelField', '显示字段', '默认 orgName'),
      inputRule('optionSource>valueField', '值字段', '默认 id'),
    ],
    [DRAG_TAG_BY_COMPONENT.userSelect]: () => [
      switchRule('multiple', '多选'),
      switchRule('clearable', '可清空', true),
      inputRule('targetField', '名称回填字段', '例如：ownerUserIdName'),
      inputRule('labelValueField', '已有名称字段', '编辑回显时使用'),
    ],
    [DRAG_TAG_BY_COMPONENT.fileUpload]: () => uploadRules(false),
    [DRAG_TAG_BY_COMPONENT.imageUpload]: () => uploadRules(true),
    [DRAG_TAG_BY_COMPONENT.objectReference]: () => [
      inputRule('referenceObjectCode', '目标对象编码', '例如：crm_customer'),
      inputRule('referenceDisplayField', '显示字段', '例如：customerName'),
      inputRule('optionSource>api', '远程接口', '例如：get@/ai/crud/crm_customer/page'),
      inputRule('optionSource>recordsField', '列表路径', '默认 records'),
      inputRule('optionSource>valueField', '值字段', '默认 id'),
      inputRule('optionSource>labelField', '显示字段', '默认 name'),
      switchRule('filterable', '可搜索', true),
      switchRule('clearable', '可清空', true),
    ],
    [DRAG_TAG_BY_COMPONENT.subTable]: () => [
      inputRule('targetObjectCode', '明细对象编码', '例如：crm_contact'),
      inputRule('relationKey', '关系编码', '例如：customer_contacts'),
      switchRule('inlineCreateEnabled', '允许行内新增', true),
      switchRule('showInDetail', '详情页展示', true),
    ],
    select: () => [
      dictTypeRule('dictType', '系统字典'),
    ],
    radio: () => [
      dictTypeRule('dictType', '系统字典'),
    ],
    checkbox: () => [
      dictTypeRule('dictType', '系统字典'),
    ],
  }
}

function installForgeBusinessComponentRules(designer) {
  const version = dictTypeOptionsLoaded ? `dict-loaded-${dictTypeOptions.length}` : 'dict-loading'
  if (componentRuleVersions.get(designer) === version)
    return
  Object.entries(createForgeBusinessComponentRules()).forEach(([name, rule]) => {
    designer.setComponentRuleConfig?.(name, rule, true)
  })
  componentRuleVersions.set(designer, version)
}

function createSelectDragRule({ name, label, title, componentKey, props = {}, options = [] }) {
  return {
    name,
    label,
    icon: 'icon-select',
    menu: FORGE_BUSINESS_MENU.name,
    input: true,
    validate: ['string', 'number', 'array'],
    rule() {
      return {
        type: 'select',
        field: createDesignerFieldName(),
        title,
        props: { ...props },
        options: cloneValue(options),
        _forge: buildForgeMeta(componentKey),
      }
    },
    props: () => [],
  }
}

function createTreeSelectDragRule({ name, label, title, componentKey, props = {}, data = [] }) {
  return {
    name,
    label,
    icon: 'icon-tree-select',
    menu: FORGE_BUSINESS_MENU.name,
    input: true,
    validate: ['string', 'number', 'array'],
    rule() {
      return {
        type: 'elTreeSelect',
        field: createDesignerFieldName(),
        title,
        props: {
          nodeKey: 'value',
          data: cloneValue(data),
          props: { label: 'label', value: 'value', children: 'children' },
          ...props,
        },
        _forge: buildForgeMeta(componentKey),
      }
    },
    props: () => [],
  }
}

function createUploadDragRule({ name, label, title, componentKey, props = {} }) {
  return {
    name,
    label,
    icon: 'icon-upload',
    menu: FORGE_BUSINESS_MENU.name,
    input: true,
    validate: ['string', 'array'],
    rule() {
      return {
        type: 'upload',
        field: createDesignerFieldName(),
        title,
        props: {
          action: '/',
          ...props,
        },
        _forge: buildForgeMeta(componentKey),
      }
    },
    props: () => [],
  }
}

function createSubTableDragRule() {
  return {
    name: DRAG_TAG_BY_COMPONENT.subTable,
    label: '明细表',
    icon: 'icon-table',
    menu: FORGE_BUSINESS_MENU.name,
    input: false,
    validate: false,
    rule() {
      return {
        type: 'input',
        title: '明细表',
        props: {
          placeholder: '明细表组件，发布时绑定子对象关系',
          disabled: true,
          targetObjectCode: '',
          relationKey: '',
        },
        _forge: buildForgeMeta('subTable', {
          fieldBinding: {
            mode: 'virtual',
            fieldCode: '',
            createIfMissing: false,
            source: 'designer',
            locked: true,
          },
        }),
      }
    },
    props: () => [],
  }
}

function uploadRules(image) {
  return [
    inputNumberRule('limit', '数量限制', 5),
    inputNumberRule('fileSize', '大小限制(MB)', image ? 5 : 20),
    inputRule('fileType', '允许类型', image ? 'jpg,png,jpeg' : 'doc,docx,xls,xlsx,pdf'),
    inputRule('storageType', '存储类型', '留空使用系统默认'),
    selectRule('valueType', '存储值', [
      { label: '文件 ID', value: 'id' },
      { label: '文件对象', value: 'object' },
    ], 'id'),
    switchRule('multiple', '多选'),
    switchRule('showTip', '显示提示', true),
  ]
}

function inputRule(field, title, placeholder = '') {
  return {
    type: 'input',
    field,
    title,
    props: {
      placeholder,
      clearable: true,
    },
  }
}

function inputNumberRule(field, title, value = null) {
  return {
    type: 'inputNumber',
    field,
    title,
    value,
    props: {
      min: 0,
      controls: false,
    },
  }
}

function switchRule(field, title, value = false) {
  return {
    type: 'switch',
    field,
    title,
    value,
  }
}

function selectRule(field, title, options = [], value = undefined, props = {}) {
  const rule = {
    type: 'select',
    field,
    title,
    props: {
      clearable: true,
      ...props,
    },
    options,
  }
  if (value !== undefined)
    rule.value = value
  return rule
}

function dictTypeRule(field = 'dictType', title = '字典类型') {
  return selectRule(field, title, dictTypeOptions, undefined, {
    filterable: true,
    allowCreate: true,
    defaultFirstOption: true,
    placeholder: dictTypeOptionsLoaded ? '选择系统字典或输入新字典类型' : '正在加载系统字典，可直接输入新类型',
  })
}

function buildForgeMeta(componentKey, overrides = {}) {
  return {
    componentKey,
    fieldBinding: {
      mode: 'field',
      fieldCode: '',
      createIfMissing: true,
      source: 'designer',
      locked: false,
      ...(overrides.fieldBinding || {}),
    },
    layout: {
      span: ['textarea', 'fileUpload', 'imageUpload', 'subTable'].includes(componentKey) ? 2 : 1,
      align: 'left',
      ...(overrides.layout || {}),
    },
    props: overrides.props || {},
  }
}

async function hydratePreviewRule(rule = {}) {
  const componentKey = resolvePreviewComponentKey(rule)
  try {
    if (['dictSelect', 'select', 'radio', 'checkbox', 'cascader'].includes(componentKey)) {
      const dictType = rule.props?.dictType
      if (dictType) {
        rule.options = await cachedPreviewOptions(`dict:${dictType}`, () => getDictData(dictType))
        return
      }
    }
    if (componentKey === 'userSelect') {
      rule.options = await cachedPreviewOptions('system:user:page', loadUserPreviewOptions)
      return
    }
    if (componentKey === 'orgTreeSelect') {
      rule.props = {
        ...(rule.props || {}),
        data: await cachedPreviewOptions('system:org:tree', loadOrgPreviewTree),
        nodeKey: 'value',
        props: { label: 'label', value: 'value', children: 'children' },
      }
      return
    }
    if (componentKey === 'regionTreeSelect') {
      const rootCode = rule.props?.rootCode || '150000'
      const dataRight = rule.props?.dataRight !== false
      const virtualDisabled = rule.props?.virtualDisabled !== false
      rule.props = {
        ...(rule.props || {}),
        data: await cachedPreviewOptions(`system:region:${rootCode}:${dataRight}:${virtualDisabled}`, () =>
          loadRegionPreviewTree(rootCode, dataRight, virtualDisabled)),
        nodeKey: 'value',
        props: { label: 'label', value: 'value', children: 'children' },
      }
      return
    }
    if (componentKey === 'objectReference') {
      rule.options = await loadObjectReferencePreviewOptions(rule)
    }
  }
  catch (error) {
    console.warn(`[form-first] 加载 ${componentKey || rule.type} 预览数据失败`, error)
  }
}

function resolvePreviewComponentKey(rule = {}) {
  const componentKey = rule._forge?.componentKey || resolveForgeComponentKeyFromDragTag(rule._fc_drag_tag)
  if (componentKey)
    return componentKey
  const type = rule.type || ''
  if (['select', 'radio', 'checkbox', 'cascader'].includes(type))
    return type
  return ''
}

async function loadDictTypeOptions() {
  if (dictTypeOptionsLoaded)
    return dictTypeOptions
  if (dictTypeOptionsLoading)
    return dictTypeOptionsLoading
  dictTypeOptionsLoading = request.get('/system/dict/type/list', {
    params: { dictStatus: 1 },
  }).then((res) => {
    dictTypeOptions = (Array.isArray(res?.data) ? res.data : [])
      .filter(item => item?.dictType)
      .map(item => ({
        label: item.dictName ? `${item.dictName}（${item.dictType}）` : item.dictType,
        value: item.dictType,
      }))
    dictTypeOptionsLoaded = true
    return dictTypeOptions
  }).catch((error) => {
    console.warn('[form-first] 加载系统字典类型失败', error)
    return dictTypeOptions
  }).finally(() => {
    dictTypeOptionsLoading = null
  })
  return dictTypeOptionsLoading
}

async function cachedPreviewOptions(key, loader) {
  if (previewOptionCache.has(key))
    return cloneValue(previewOptionCache.get(key))
  const value = await loader()
  previewOptionCache.set(key, cloneValue(value))
  return cloneValue(value)
}

async function loadUserPreviewOptions() {
  const res = await request.get('/system/user/page', {
    params: {
      pageNum: 1,
      pageSize: 20,
    },
  })
  const rows = res?.data?.records || res?.data?.list || res?.data?.rows || []
  return rows.map(user => ({
    label: user.realName || user.name || user.nickname || user.username || String(user.id || ''),
    value: user.id,
    raw: user,
  })).filter(option => option.value !== null && option.value !== undefined)
}

async function loadOrgPreviewTree() {
  const res = await request.get('/system/org/tree')
  return normalizeTreeOptions(res?.data || [], {
    valueFields: ['id', 'orgId', 'value', 'key'],
    labelFields: ['orgName', 'name', 'label', 'title'],
  })
}

async function loadRegionPreviewTree(rootCode, dataRight, virtualDisabled) {
  const res = await request.get('/system/region/treeAll', {
    params: { rootCode, dataRight },
  })
  return normalizeTreeOptions(res?.data || [], {
    valueFields: ['code', 'value', 'key'],
    labelFields: ['name', 'label', 'title'],
    disabled: node => virtualDisabled && String(node?.code || node?.value || '').endsWith('ALL'),
  })
}

async function loadObjectReferencePreviewOptions(rule = {}) {
  const props = rule.props || {}
  const optionSource = props.optionSource || {}
  const referenceObjectCode = props.referenceObjectCode || ''
  const api = optionSource.api || (referenceObjectCode ? `get@/ai/crud/${referenceObjectCode}/page` : '')
  if (!api)
    return []
  const cacheKey = `object:${api}:${optionSource.recordsField || ''}:${optionSource.labelField || props.referenceDisplayField || ''}`
  return cachedPreviewOptions(cacheKey, async () => {
    const { method, url } = parseApiConfig(api)
    const params = {
      pageNum: 1,
      pageSize: 20,
      ...(optionSource.params || {}),
    }
    const res = await request({
      method,
      url,
      params: method === 'get' ? params : undefined,
      data: method === 'get' ? undefined : params,
    })
    const rows = extractRows(res?.data, optionSource)
    const valueField = optionSource.valueField || 'id'
    const labelField = optionSource.labelField || props.referenceDisplayField || 'name'
    return rows.map(row => ({
      label: row[labelField] ?? row.name ?? row.title ?? row.label ?? String(row[valueField] ?? ''),
      value: row[valueField],
      raw: row,
    })).filter(option => option.value !== null && option.value !== undefined)
  })
}

function parseApiConfig(api) {
  const text = String(api || '')
  const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['get', text]
  return {
    method: String(method || 'get').toLowerCase(),
    url: urlParts.join('@') || text,
  }
}

function extractRows(data, optionSource = {}) {
  if (Array.isArray(data))
    return data
  if (!data || typeof data !== 'object')
    return []
  if (optionSource.recordsField) {
    const rows = getNestedValue(data, optionSource.recordsField)
    if (Array.isArray(rows))
      return rows
  }
  return data.records || data.list || data.rows || []
}

function normalizeTreeOptions(nodes = [], config = {}) {
  return (Array.isArray(nodes) ? nodes : []).map((node) => {
    const value = resolveFirstValue(node, config.valueFields || ['value', 'key', 'id'])
    const label = resolveFirstValue(node, config.labelFields || ['label', 'name', 'title'])
    const children = normalizeTreeOptions(node.children || [], config)
    const option = {
      ...node,
      value,
      key: node.key ?? value,
      label: label === null || label === undefined || label === '' ? String(value ?? '') : String(label),
    }
    if (config.disabled?.(node))
      option.disabled = true
    if (children.length)
      option.children = children
    return option
  }).filter(option => option.value !== null && option.value !== undefined)
}

function resolveFirstValue(source = {}, fields = []) {
  for (const field of fields) {
    if (source[field] !== null && source[field] !== undefined && source[field] !== '')
      return source[field]
  }
  return undefined
}

function getNestedValue(source, path) {
  return String(path || '')
    .split('.')
    .filter(Boolean)
    .reduce((value, key) => value?.[key], source)
}

function walkRules(rules = [], visitor) {
  ;(Array.isArray(rules) ? rules : []).forEach((rule) => {
    if (!rule || typeof rule !== 'object')
      return
    visitor(rule)
    if (Array.isArray(rule.children))
      walkRules(rule.children, visitor)
  })
}

function createDesignerFieldName() {
  fieldIndex += 1
  return `field_${Date.now()}_${fieldIndex}`
}

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value ?? null))
}
