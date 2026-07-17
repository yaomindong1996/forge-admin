const TYPE_DEFAULTS = {
  DATE: {
    segmentValue: 'yyyyMMdd',
    segmentLength: 8,
  },
  FIXED: {
    segmentValue: 'CODE',
    segmentLength: 4,
  },
  SEQ: {
    segmentValue: null,
    segmentLength: 4,
    padEnabled: 1,
    padChar: '0',
    padDirection: 'LEFT',
    radixType: 'DECIMAL',
    resetEnabled: 1,
    resetPolicy: 'DAY',
    startValue: 1,
  },
  VARIABLE: {
    segmentValue: null,
    variableSource: 'CUSTOM',
    segmentLength: 8,
  },
  SYS_VAR: {
    segmentValue: 'tenantId',
    segmentLength: 8,
  },
}

let segmentSeed = 0

export function createLatestRequestGuard() {
  let version = 0
  return {
    begin() {
      version += 1
      return version
    },
    invalidate() {
      version += 1
    },
    isLatest(requestVersion) {
      return requestVersion === version
    },
  }
}

export function hasCodeRulePermission(userStore, permission) {
  if (userStore?.isAdmin)
    return true
  const grants = [
    ...(Array.isArray(userStore?.permissions) ? userStore.permissions : []),
    ...(Array.isArray(userStore?.apiPermissions) ? userStore.apiPermissions : []),
  ]
  return grants.includes(permission) || grants.includes('**') || grants.includes('*:*:*')
}

function nextSegmentKey(type) {
  segmentSeed += 1
  return `${String(type || 'segment').toLowerCase()}_${Date.now().toString(36)}_${segmentSeed.toString(36)}`
}

export function createCodeRuleSegment(type = 'FIXED', order = 1) {
  const normalizedType = TYPE_DEFAULTS[type] ? type : 'FIXED'
  return {
    segmentKey: nextSegmentKey(normalizedType),
    segmentOrder: order,
    segmentType: normalizedType,
    segmentValue: null,
    variableSource: 'CUSTOM',
    segmentLength: null,
    padEnabled: 0,
    padChar: null,
    padDirection: 'LEFT',
    groupEnabled: 0,
    includeInCode: 1,
    radixType: null,
    resetEnabled: 0,
    resetPolicy: 'NONE',
    startValue: 1,
    excludeAmbiguous: 0,
    ...TYPE_DEFAULTS[normalizedType],
  }
}

export function changeCodeRuleSegmentType(segment, type) {
  const next = createCodeRuleSegment(type, segment?.segmentOrder || 1)
  next.segmentKey = segment?.segmentKey || next.segmentKey
  next.includeInCode = Number(segment?.includeInCode) === 0 ? 0 : 1
  next.groupEnabled = type === 'SEQ' ? 0 : Number(segment?.groupEnabled) === 1 ? 1 : 0
  return next
}

export function changeCodeRuleVariableSource(segment, variableSource) {
  return {
    ...segment,
    variableSource: variableSource === 'LOWCODE' ? 'LOWCODE' : 'CUSTOM',
    segmentValue: null,
  }
}

export function applyLowCodeVariableMapping(
  segments,
  targetSegmentKey,
  mapping,
  currentSourceObjectId,
) {
  const sourceObjectId = mapping?.sourceObjectId === null || mapping?.sourceObjectId === undefined
    ? null
    : String(mapping.sourceObjectId)
  const fieldCode = String(mapping?.fieldCode || '').trim()
  if (!sourceObjectId || !fieldCode)
    throw new Error('低代码字段映射必须选择业务对象和字段')

  const targetExists = (segments || []).some(segment => segment?.segmentKey === targetSegmentKey)
  if (!targetExists)
    throw new Error('待映射的编码分段已不存在')

  const objectChanged = String(currentSourceObjectId || '') !== sourceObjectId
  const clearedSegmentKeys = []
  const nextSegments = (segments || []).map((segment) => {
    if (segment?.segmentKey === targetSegmentKey) {
      return {
        ...segment,
        variableSource: 'LOWCODE',
        segmentValue: fieldCode,
      }
    }
    if (objectChanged
      && segment?.segmentType === 'VARIABLE'
      && segment?.variableSource === 'LOWCODE'
      && segment?.segmentValue) {
      clearedSegmentKeys.push(segment.segmentKey)
      return { ...segment, segmentValue: null }
    }
    return segment
  })

  return {
    sourceObjectId,
    objectChanged,
    clearedSegmentKeys,
    segments: normalizeCodeRuleSegments(nextSegments),
  }
}

export function normalizeCodeRuleSegments(segments = []) {
  return [...segments]
    .sort((left, right) => Number(left?.segmentOrder || 0) - Number(right?.segmentOrder || 0))
    .map((segment, index) => ({
      ...segment,
      segmentKey: segment?.segmentKey || nextSegmentKey(segment?.segmentType),
      segmentOrder: index + 1,
      includeInCode: Number(segment?.includeInCode) === 0 ? 0 : 1,
      groupEnabled: Number(segment?.groupEnabled) === 1 ? 1 : 0,
      padEnabled: Number(segment?.padEnabled) === 1 ? 1 : 0,
      resetEnabled: Number(segment?.resetEnabled) === 1 ? 1 : 0,
      excludeAmbiguous: Number(segment?.excludeAmbiguous) === 1 ? 1 : 0,
      variableSource: segment?.segmentType === 'VARIABLE' && segment?.variableSource === 'LOWCODE'
        ? 'LOWCODE'
        : 'CUSTOM',
    }))
}

export function segmentDeclaredLength(segment) {
  const configured = Number(segment?.segmentLength)
  if (Number.isFinite(configured) && configured > 0)
    return configured
  if (['DATE', 'FIXED'].includes(segment?.segmentType))
    return String(segment?.segmentValue || '').length
  return 0
}

export function validateCodeRuleDraft(draft = {}) {
  const errors = []
  const warnings = []
  const segments = normalizeCodeRuleSegments(draft.segments)
  if (!String(draft.ruleCode || '').match(/^[A-Z]\w{0,63}$/i))
    errors.push('规则编码必须以字母开头，且只能包含字母、数字和下划线')
  if (!String(draft.ruleName || '').trim())
    errors.push('规则名称不能为空')
  if (!String(draft.category || '').trim())
    errors.push('编码分类不能为空')
  if (!String(draft.scene || '').trim())
    errors.push('适用场景不能为空')
  if (!segments.length)
    errors.push('至少添加一个编码分段')
  if (!segments.some(segment => Number(segment.includeInCode) === 1))
    errors.push('至少需要一个列入编码的分段')
  if (segments.filter(segment => segment.segmentType === 'SEQ').length > 1)
    errors.push('一条规则最多只能包含一个流水号段')
  if (segments.some(segment => segment.segmentType === 'VARIABLE' && segment.variableSource === 'LOWCODE') && !draft.sourceObjectId)
    errors.push('业务变量段必须选择字段来源业务对象')

  const keys = new Set()
  segments.forEach((segment, index) => {
    const label = `第 ${index + 1} 段`
    if (keys.has(segment.segmentKey))
      errors.push(`${label}的稳定分段键重复`)
    keys.add(segment.segmentKey)
    if (!segment.segmentType)
      errors.push(`${label}未选择分段类型`)
    if (['DATE', 'FIXED', 'VARIABLE', 'SYS_VAR'].includes(segment.segmentType) && !String(segment.segmentValue || '').trim())
      errors.push(`${label}缺少配置值`)
    const variableName = String(segment.segmentValue || '').trim()
    if (segment.segmentType === 'VARIABLE'
      && segment.variableSource === 'CUSTOM'
      && variableName
      && !variableName.match(/^[A-Z_]\w{0,63}$/i)) {
      errors.push(`${label}的自定义变量名必须以字母或下划线开头，且只能包含字母、数字和下划线`)
    }
    if (segment.segmentType === 'SEQ' && !(Number(segment.segmentLength) >= 1 && Number(segment.segmentLength) <= 32))
      errors.push(`${label}的流水长度必须在 1 到 32 之间`)
    if (Number(segment.groupEnabled) === 1 && Number(segment.includeInCode) !== 1)
      warnings.push(`${label}参与分组但不输出，跨分组可能产生相同编号`)
  })
  const totalLength = segments
    .filter(segment => Number(segment.includeInCode) === 1)
    .reduce((total, segment) => total + segmentDeclaredLength(segment), 0)
  if (totalLength > 96)
    errors.push('编码声明总长度不能超过 96 个字符')
  if (!segments.some(segment => segment.segmentType === 'SEQ'))
    warnings.push('当前规则没有流水号段，相同输入会生成相同编码')
  return { valid: errors.length === 0, errors, warnings, totalLength, segments }
}

export function buildCodeRulePreviewPayload(draft = {}, fields = {}) {
  const validation = validateCodeRuleDraft(draft)
  return {
    id: draft.id || null,
    ruleCode: draft.ruleCode || 'preview_rule',
    ruleName: draft.ruleName || '编码规则预览',
    category: draft.category || 'COMMON',
    sequence: Number(draft.sampleSequence || 1),
    fields: { ...fields },
    segments: validation.segments,
  }
}

export function createEmptyCodeRuleDraft() {
  return {
    id: null,
    versionNo: null,
    ruleCode: '',
    ruleName: '',
    scene: 'COMMON',
    category: 'COMMON',
    sourceObjectId: null,
    sourceObjectCode: null,
    status: 1,
    inCodeList: 1,
    remark: '',
    builtin: 0,
    sampleSequence: 1,
    segments: [
      createCodeRuleSegment('FIXED', 1),
      createCodeRuleSegment('DATE', 2),
      createCodeRuleSegment('SEQ', 3),
    ],
  }
}
