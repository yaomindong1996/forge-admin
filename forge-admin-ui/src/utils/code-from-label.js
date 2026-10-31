/**
 * 根据中文名称自动生成编码：角色全拼、权限标识英文语义 + 拼音兜底。
 */
import { pinyin } from 'pinyin-pro'

/** 常见业务词 → 英文片段（权限标识 / 字典 / 配置优先用） */
const SEMANTIC_PHRASES = [
  ['重置密码', 'resetPwd'],
  ['强制下线', 'forceLogout'],
  ['数据权限', 'dataScope'],
  ['分配用户', 'assignUser'],
  ['分配角色', 'assignRole'],
  ['批量删除', 'batchDelete'],
  ['批量导入', 'batchImport'],
  ['批量导出', 'batchExport'],
  ['行政区划', 'region'],
  ['组织架构', 'org'],
  ['用户管理', 'user'],
  ['角色管理', 'role'],
  ['菜单管理', 'menu'],
  ['岗位管理', 'post'],
  ['字典管理', 'dict'],
  ['参数设置', 'config'],
  ['系统管理', 'system'],
  ['权限字符', 'roleKey'],
  ['权限标识', 'perms'],
  ['客户端', 'client'],
  ['租户', 'tenant'],
  ['部门', 'dept'],
  ['组织', 'org'],
  ['用户', 'user'],
  ['角色', 'role'],
  ['菜单', 'menu'],
  ['岗位', 'post'],
  ['字典', 'dict'],
  ['参数', 'config'],
  ['配置', 'config'],
  ['文件', 'file'],
  ['消息', 'message'],
  ['流程', 'flow'],
  ['登录', 'login'],
  ['超时', 'timeout'],
  ['性别', 'sex'],
  ['状态', 'status'],
  ['类型', 'type'],
  ['名称', 'name'],
  ['编码', 'code'],
  ['新增', 'add'],
  ['添加', 'add'],
  ['创建', 'add'],
  ['编辑', 'edit'],
  ['修改', 'edit'],
  ['更新', 'edit'],
  ['删除', 'delete'],
  ['移除', 'remove'],
  ['查询', 'query'],
  ['查看', 'query'],
  ['详情', 'query'],
  ['列表', 'list'],
  ['分页', 'page'],
  ['导出', 'export'],
  ['导入', 'import'],
  ['下载', 'download'],
  ['上传', 'upload'],
  ['刷新', 'refresh'],
  ['缓存', 'cache'],
  ['授权', 'auth'],
  ['启用', 'enable'],
  ['停用', 'disable'],
  ['禁用', 'disable'],
  ['发布', 'publish'],
  ['下线', 'offline'],
  ['复制', 'copy'],
  ['排序', 'sort'],
  ['管理', 'manage'],
]

const ACTION_ALIASES = [
  ['重置密码', 'resetPwd'],
  ['强制下线', 'forceLogout'],
  ['批量删除', 'batchDelete'],
  ['批量导入', 'batchImport'],
  ['批量导出', 'batchExport'],
  ['新增', 'add'],
  ['添加', 'add'],
  ['创建', 'add'],
  ['编辑', 'edit'],
  ['修改', 'edit'],
  ['更新', 'edit'],
  ['删除', 'delete'],
  ['移除', 'remove'],
  ['查询', 'query'],
  ['查看', 'query'],
  ['详情', 'query'],
  ['列表', 'list'],
  ['导出', 'export'],
  ['导入', 'import'],
  ['下载', 'download'],
  ['上传', 'upload'],
  ['授权', 'auth'],
  ['启用', 'enable'],
  ['停用', 'disable'],
  ['禁用', 'disable'],
  ['发布', 'publish'],
  ['复制', 'copy'],
  ['刷新', 'refresh'],
]

/**
 * @param {string} text
 * @param {'roleKey'|'perms'|'postCode'|'dictType'|'configKey'|'categoryCode'|'genericCode'|'apiCode'|string} scene
 * @param {object} [options]
 * @returns {string}
 */
export function generateCodeFromLabel(text, scene = 'roleKey', options = {}) {
  const source = String(text || '').trim()
  if (!source)
    return ''

  switch (scene) {
    case 'perms':
      return buildPermsCode(source, options)
    case 'apiCode':
      return buildColonCode(source)
    case 'dictType':
      return buildSnakeCode(source, { prefix: 'sys_', semantic: true })
    case 'configKey':
      return buildDotCode(source, { prefix: options.prefix ?? 'sys.', semantic: true })
    case 'exportConfigKey':
      return buildSnakeCode(source, { semantic: true })
    case 'postCode':
    case 'categoryCode':
    case 'genericCode':
    case 'groupCode':
    case 'templateCode':
    case 'dimensionCode':
    case 'connectionCode':
    case 'clientCode':
    case 'datasourceCode':
    case 'agentCode':
      return buildSnakeCode(source, { semantic: false })
    case 'roleKey':
    default:
      return buildSnakeCode(source, { semantic: false })
  }
}

function buildColonCode(source) {
  const token = buildSemanticOrPinyinToken(source, { preferCamel: false, joiner: ':' })
  return sanitizeCodeToken(token, ':')
}

function buildPermsCode(source, options = {}) {
  const parentPrefix = resolvePermsParentPrefix(options.parentPerms)
  const { action, remain } = splitActionToken(source)
  const resourceToken = buildSemanticOrPinyinToken(remain || source, { preferCamel: true })
    || buildSemanticOrPinyinToken(source, { preferCamel: true })
    || 'resource'
  const actionToken = action || inferDefaultAction(options.resourceType)
  const moduleToken = parentPrefix.module || 'system'
  const resource = parentPrefix.resource || resourceToken
  return `${moduleToken}:${resource}:${actionToken}`
}

function resolvePermsParentPrefix(parentPerms = '') {
  const text = String(parentPerms || '').trim()
  if (!text)
    return {}
  const parts = text.split(':').map(item => item.trim()).filter(Boolean)
  if (parts.length >= 2)
    return { module: parts[0], resource: parts[1] }
  if (parts.length === 1)
    return { module: parts[0] }
  return {}
}

function splitActionToken(source = '') {
  const text = String(source || '').trim()
  for (const [cn, en] of ACTION_ALIASES) {
    if (text.endsWith(cn))
      return { action: en, remain: text.slice(0, -cn.length).replace(/(管理|配置|设置)$/g, '').trim() }
    if (text.startsWith(cn) && text.length > cn.length)
      return { action: en, remain: text.slice(cn.length).replace(/(管理|配置|设置)$/g, '').trim() }
  }
  const cleaned = text.replace(/(管理|配置|设置)$/g, '').trim()
  return { action: '', remain: cleaned }
}

function inferDefaultAction(resourceType) {
  const type = Number(resourceType)
  if (type === 2)
    return 'list'
  if (type === 4)
    return 'api'
  return 'query'
}

function buildSnakeCode(source, { prefix = '', semantic = false } = {}) {
  const token = semantic
    ? buildSemanticOrPinyinToken(source, { preferCamel: false })
    : toFullPinyinSnake(source)
  const body = sanitizeCodeToken(token, '_')
  if (!body)
    return ''
  if (prefix && body.startsWith(prefix.replace(/_$/g, '')))
    return body.startsWith(prefix) ? body : `${prefix}${body}`
  return `${prefix}${body}`
}

function buildDotCode(source, { prefix = '', semantic = false } = {}) {
  const token = semantic
    ? buildSemanticOrPinyinToken(source, { preferCamel: false, joiner: '.' })
    : toFullPinyinSnake(source).replace(/_/g, '.')
  const body = sanitizeCodeToken(token, '.')
  if (!body)
    return ''
  if (prefix && (body.startsWith(prefix) || body.startsWith(prefix.replace(/\.$/, ''))))
    return body.includes('.') ? body : `${prefix}${body}`
  return `${prefix}${body}`
}

function buildSemanticOrPinyinToken(source, { preferCamel = false, joiner = '_' } = {}) {
  const text = String(source || '').trim()
  if (!text)
    return ''
  const chunks = []
  let rest = text
  while (rest) {
    let matched = false
    for (const [cn, en] of SEMANTIC_PHRASES) {
      if (rest.startsWith(cn)) {
        chunks.push(en)
        rest = rest.slice(cn.length)
        matched = true
        break
      }
    }
    if (matched)
      continue
    const latin = rest.match(/^[A-Za-z0-9]+/)
    if (latin) {
      chunks.push(latin[0].toLowerCase())
      rest = rest.slice(latin[0].length)
      continue
    }
    const han = rest.match(/^[\u4E00-\u9FFF]+/)
    if (han) {
      chunks.push(toFullPinyinSnake(han[0]))
      rest = rest.slice(han[0].length)
      continue
    }
    rest = rest.slice(1)
  }
  if (!chunks.length)
    return toFullPinyinSnake(text)
  if (preferCamel && chunks.length === 1)
    return chunks[0]
  return chunks.join(joiner)
}

function toFullPinyinSnake(text = '') {
  const source = String(text || '').trim()
  if (!source)
    return ''
  // 已是编码风格则规范化后返回
  if (/^[a-zA-Z][a-zA-Z0-9_.:-]*$/.test(source))
    return sanitizeCodeToken(source, '_')

  const parts = pinyin(source, {
    toneType: 'none',
    type: 'array',
    nonZh: 'consecutive',
  })
  return sanitizeCodeToken(
    (Array.isArray(parts) ? parts : [String(parts || '')])
      .map(item => String(item || '').trim())
      .filter(Boolean)
      .join('_'),
    '_',
  )
}

function sanitizeCodeToken(value = '', joiner = '_') {
  const escaped = joiner === '.' ? '\\.' : joiner === ':' ? ':' : '_'
  const pattern = new RegExp(`[^a-zA-Z0-9${escaped}]+`, 'g')
  const collapse = new RegExp(`${joiner === '.' ? '\\.' : joiner}{2,}`, 'g')
  const trimEdge = new RegExp(`^${joiner === '.' ? '\\.' : joiner}+|${joiner === '.' ? '\\.' : joiner}+$`, 'g')
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[\s\-]+/g, joiner)
    .replace(pattern, joiner)
    .replace(collapse, joiner)
    .replace(trimEdge, '')
}

/**
 * 名称 → 编码自动回填：空值或仍是上次自动值时跟随；用户改过则不再覆盖。
 */
export function createAutoCodeSync({
  scene,
  targetField,
  isCreate = formData => !hasPersistedId(formData),
  buildOptions = () => ({}),
} = {}) {
  let lastGenerated = ''
  let dirty = false

  function reset() {
    lastGenerated = ''
    dirty = false
  }

  function applyTarget(formData, context, code) {
    if (!targetField)
      return
    if (typeof context?.patchFormData === 'function') {
      context.patchFormData({ [targetField]: code })
      return
    }
    if (formData && typeof formData === 'object')
      formData[targetField] = code
  }

  function onSourceChange({ value, formData, context } = {}) {
    if (!isCreate(formData || {})) {
      const current = String(formData?.[targetField] ?? '').trim()
      if (current)
        return
    }
    if (dirty)
      return
    const current = String(formData?.[targetField] ?? '').trim()
    if (current && current !== lastGenerated) {
      dirty = true
      return
    }
    const code = generateCodeFromLabel(value, scene, buildOptions(formData || {}, context))
    lastGenerated = code
    applyTarget(formData, context, code)
  }

  function onTargetChange({ value } = {}) {
    const next = String(value ?? '').trim()
    if (!next) {
      dirty = false
      lastGenerated = ''
      return
    }
    if (next !== lastGenerated)
      dirty = true
  }

  return {
    reset,
    onSourceChange,
    onTargetChange,
    getLastGenerated: () => lastGenerated,
  }
}

function hasPersistedId(formData = {}) {
  return Boolean(
    formData.id
    || formData.dictId
    || formData.configId
    || formData.postId
    || formData.roleId
    || formData.templateId
    || formData.datasourceId,
  )
}
