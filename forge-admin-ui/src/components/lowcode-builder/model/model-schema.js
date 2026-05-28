export const tableModeOptions = [
  { label: '创建新业务表', value: 'CREATE' },
  { label: '绑定已有表', value: 'EXISTING' },
]

export const appTypeOptions = [
  { label: '单表应用', value: 'SINGLE' },
  { label: '树形单表', value: 'TREE' },
]

export const dataTypeOptions = [
  { label: '短文本', value: 'varchar' },
  { label: '定长文本', value: 'char' },
  { label: '长文本', value: 'text' },
  { label: '整数', value: 'int' },
  { label: '长整数', value: 'bigint' },
  { label: '小数', value: 'decimal' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '时间', value: 'time' },
  { label: '开关值', value: 'tinyint' },
]

export const componentTypeOptions = [
  { label: '单行输入', value: 'input' },
  { label: '多行文本', value: 'textarea' },
  { label: '数字输入', value: 'number' },
  { label: '下拉选择', value: 'select' },
  { label: '单选', value: 'radio' },
  { label: '多选', value: 'checkbox' },
  { label: '字典选择器', value: 'dictSelect' },
  { label: '树形选择', value: 'treeSelect' },
  { label: '组织树选择', value: 'orgTreeSelect' },
  { label: '用户选择', value: 'userSelect' },
  { label: '区划树选择', value: 'regionTreeSelect' },
  { label: '级联选择', value: 'cascader' },
  { label: '开关', value: 'switch' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '时间', value: 'time' },
  { label: '图片上传', value: 'imageUpload' },
  { label: '文件上传', value: 'fileUpload' },
]

export const queryTypeOptions = [
  { label: '等于', value: 'eq' },
  { label: '包含', value: 'like' },
  { label: '大于等于', value: 'ge' },
  { label: '小于等于', value: 'le' },
  { label: '区间', value: 'between' },
  { label: '多值', value: 'in' },
]

export const sensitiveTypeOptions = [
  { label: '不敏感', value: 'NONE' },
  { label: '手机号', value: 'PHONE' },
  { label: '身份证', value: 'ID_CARD' },
  { label: '邮箱', value: 'EMAIL' },
  { label: '银行卡', value: 'BANK_CARD' },
  { label: '姓名', value: 'NAME' },
  { label: '地址', value: 'ADDRESS' },
]

export const auditFieldNames = [
  'id',
  'tenantId',
  'createBy',
  'createTime',
  'createDept',
  'updateBy',
  'updateTime',
  'delFlag',
]

export const auditColumnNames = [
  'id',
  'tenant_id',
  'create_by',
  'create_time',
  'create_dept',
  'update_by',
  'update_time',
  'del_flag',
]

export const systemFieldDefinitions = [
  {
    field: 'id',
    columnName: 'id',
    label: 'ID',
    dataType: 'bigint',
    componentType: 'number',
    required: true,
    searchable: true,
    listVisible: true,
    formVisible: false,
    primaryKey: true,
    autoIncrement: true,
    width: 100,
    remark: '自增主键，系统生成',
  },
  {
    field: 'tenantId',
    columnName: 'tenant_id',
    label: '租户ID',
    dataType: 'bigint',
    componentType: 'number',
    required: true,
    searchable: false,
    listVisible: false,
    formVisible: false,
    width: 120,
    remark: '租户隔离字段，系统写入',
  },
  {
    field: 'createBy',
    columnName: 'create_by',
    label: '创建人',
    dataType: 'bigint',
    componentType: 'number',
    searchable: false,
    listVisible: false,
    formVisible: false,
    width: 120,
    remark: '审计字段，系统写入',
  },
  {
    field: 'createTime',
    columnName: 'create_time',
    label: '创建时间',
    dataType: 'datetime',
    componentType: 'datetime',
    required: true,
    searchable: true,
    listVisible: true,
    formVisible: false,
    sortable: true,
    width: 180,
    remark: '审计字段，系统写入',
  },
  {
    field: 'createDept',
    columnName: 'create_dept',
    label: '创建部门',
    dataType: 'bigint',
    componentType: 'number',
    searchable: false,
    listVisible: false,
    formVisible: false,
    width: 120,
    remark: '审计字段，系统写入',
  },
  {
    field: 'updateBy',
    columnName: 'update_by',
    label: '更新人',
    dataType: 'bigint',
    componentType: 'number',
    searchable: false,
    listVisible: false,
    formVisible: false,
    width: 120,
    remark: '审计字段，系统写入',
  },
  {
    field: 'updateTime',
    columnName: 'update_time',
    label: '更新时间',
    dataType: 'datetime',
    componentType: 'datetime',
    required: true,
    searchable: false,
    listVisible: true,
    formVisible: false,
    sortable: true,
    width: 180,
    remark: '审计字段，系统写入',
  },
  {
    field: 'delFlag',
    columnName: 'del_flag',
    label: '删除标志',
    dataType: 'char',
    length: 1,
    componentType: 'input',
    required: true,
    searchable: false,
    listVisible: false,
    formVisible: false,
    width: 100,
    remark: '逻辑删除字段，系统维护',
  },
]

export const indexTypeOptions = [
  { label: '普通索引', value: 'NORMAL' },
  { label: '唯一索引', value: 'UNIQUE' },
]

export function createDefaultModelSchema(options = {}) {
  const objectCode = options.objectCode || 'lowcode_demo'
  const objectName = options.objectName || '低代码应用'
  const tableName = options.tableName || 'biz_lowcode_demo'
  const fields = ensureSystemFields([
    createDefaultField('name', '名称'),
    {
      ...createDefaultField('status', '状态'),
      dataType: 'varchar',
      length: 32,
      componentType: 'select',
      dictType: 'common_status',
      queryType: 'eq',
    },
  ], options.tenantEnabled !== false)

  return {
    schemaVersion: 2,
    domain: options.domain || {
      id: null,
      code: '',
      name: '',
    },
    object: {
      code: objectCode,
      name: objectName,
      description: '',
      ...(options.object || {}),
    },
    appType: options.appType || 'SINGLE',
    tableMode: options.tableMode || 'CREATE',
    tableName,
    businessName: objectName,
    treeConfig: {
      enabled: false,
      keyField: 'id',
      parentField: 'parentId',
      labelField: 'name',
      childrenField: 'children',
      treeTitle: '树形导航',
      loadMode: 'full',
    },
    fields,
    relations: [],
    indexes: [],
    policies: createDefaultPolicies(fields),
    children: [],
  }
}

export function createDefaultPolicies(fields = []) {
  return normalizeLowcodePolicies({ fields, policies: {} }).policies
}

export function normalizeLowcodePolicies(model = {}) {
  const fields = Array.isArray(model.fields) ? model.fields : []
  const source = model.policies || {}
  const userRef = resolvePolicyField(fields, source.userField, source.userColumn, ['createBy', 'create_by', 'userId', 'user_id'], 'createBy', 'create_by')
  const orgRef = resolvePolicyField(fields, source.orgField, source.orgColumn, ['orgId', 'org_id', 'deptId', 'dept_id', 'createDept', 'create_dept'], 'createDept', 'create_dept')
  const regionRef = resolvePolicyField(fields, source.regionField, source.regionColumn, ['regionCode', 'region_code', 'areaCode', 'area_code'], '', '')
  const tenantRef = resolvePolicyField(fields, source.tenantField, source.tenantColumn, ['tenantId', 'tenant_id'], 'tenantId', 'tenant_id')
  const logicDeleteRef = resolvePolicyField(fields, source.logicDeleteField, source.logicDeleteColumn, ['delFlag', 'del_flag'], 'delFlag', 'del_flag')
  model.policies = {
    dataScope: normalizeDataScope(source.dataScope),
    userField: userRef.field,
    userColumn: userRef.column,
    orgField: orgRef.field,
    orgColumn: orgRef.column,
    regionField: regionRef.field,
    regionColumn: regionRef.column,
    auditEnabled: true,
    primaryKeyStrategy: 'AUTO_INCREMENT',
    primaryKeyField: 'id',
    tenantField: tenantRef.field || 'tenantId',
    tenantColumn: tenantRef.column || 'tenant_id',
    logicDeleteField: logicDeleteRef.field || 'delFlag',
    logicDeleteColumn: logicDeleteRef.column || 'del_flag',
  }
  return model
}

function normalizeDataScope(dataScope) {
  const normalized = String(dataScope || 'TENANT').trim().toUpperCase()
  return normalized === 'SYSTEM_DATA_SCOPE' ? 'FOLLOW_SYSTEM' : normalized
}

function resolvePolicyField(fields, configuredField, configuredColumn, candidates, fallbackField, fallbackColumn) {
  const refs = new Map()
  fields.forEach((field) => {
    if (!field)
      return
    const fieldName = field.field || ''
    const columnName = field.columnName || camelToSnake(fieldName)
    const ref = { field: fieldName, column: columnName }
    if (fieldName)
      refs.set(String(fieldName).toLowerCase(), ref)
    if (columnName)
      refs.set(String(columnName).toLowerCase(), ref)
  })
  const configured = findPolicyRef(refs, configuredField) || findPolicyRef(refs, configuredColumn)
  if (configured)
    return configured
  for (const candidate of candidates) {
    const matched = findPolicyRef(refs, candidate)
    if (matched)
      return matched
  }
  return {
    field: configuredField || fallbackField || '',
    column: configuredColumn || fallbackColumn || '',
  }
}

function findPolicyRef(refs, key) {
  if (!key)
    return null
  return refs.get(String(key).toLowerCase()) || null
}

export function createDefaultField(field = 'fieldName', label = '字段名称') {
  return {
    field,
    columnName: camelToSnake(field),
    label,
    dataType: 'varchar',
    length: 128,
    precision: 2,
    required: false,
    defaultValue: null,
    searchable: false,
    listVisible: true,
    formVisible: true,
    componentType: 'input',
    queryType: 'like',
    dictType: '',
    sensitiveType: 'NONE',
    encryptAlgorithm: '',
    sortable: false,
    primaryKey: false,
    systemField: false,
    readonly: false,
    autoIncrement: false,
    width: 160,
    remark: '',
  }
}

export function createSystemField(definition = {}) {
  return {
    ...createDefaultField(definition.field, definition.label),
    ...definition,
    field: definition.field,
    columnName: definition.columnName,
    required: Boolean(definition.required),
    defaultValue: null,
    queryType: definition.queryType || 'eq',
    dictType: '',
    sensitiveType: 'NONE',
    encryptAlgorithm: '',
    primaryKey: Boolean(definition.primaryKey),
    systemField: true,
    readonly: true,
    autoIncrement: Boolean(definition.autoIncrement),
    sortable: Boolean(definition.sortable),
    width: definition.width || 120,
    remark: definition.remark || '系统字段',
  }
}

export function ensureSystemFields(fields = [], tenantEnabled = true) {
  const businessFields = (fields || []).filter(field => !isAuditField(field))
  const idField = createSystemField(systemFieldDefinitions[0])
  const tenantField = tenantEnabled ? createSystemField(systemFieldDefinitions[1]) : null
  const auditFields = systemFieldDefinitions
    .slice(2)
    .map(createSystemField)
  return [idField, ...(tenantField ? [tenantField] : []), ...businessFields, ...auditFields]
}

export function isSystemField(field = {}) {
  return Boolean(field.systemField) || isAuditField(field)
}

export function isReservedSystemField(field = {}) {
  return isAuditField(field)
}

export function isLockedSystemField(field = {}) {
  return isReservedSystemField(field) || Boolean(field.readonly)
}

export function createDefaultIndex(index = 1) {
  return {
    indexName: '',
    indexType: 'NORMAL',
    fields: [],
    unique: false,
    auto: false,
    remark: `业务索引${index}`,
  }
}

export function normalizeFieldName(value) {
  const cleaned = String(value || '')
    .replace(/\W/g, ' ')
    .replace(/[_\s]+([a-z0-9])/gi, (_, char) => char.toUpperCase())
    .replace(/^[^a-z]+/i, '')

  if (!cleaned)
    return 'fieldName'
  return cleaned.charAt(0).toLowerCase() + cleaned.slice(1)
}

export function normalizeObjectCode(value, fallback = '') {
  const cleaned = String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
    .replace(/^[^a-z]+/, '')
    .replace(/_+$/g, '')

  return cleaned || fallback
}

export function normalizeTableName(value) {
  const cleaned = String(value || '')
    .trim()
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
    .replace(/^[^a-z]+/, '')

  return cleaned || 'biz_lowcode_demo'
}

export function camelToSnake(value) {
  return String(value || '')
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W/g, '_')
    .replace(/_+/g, '_')
    .toLowerCase()
}

export function cloneSchema(value) {
  return JSON.parse(JSON.stringify(value))
}

export function isSameSchema(left, right) {
  return JSON.stringify(left || null) === JSON.stringify(right || null)
}

export function createFieldFromIndex(index) {
  return createDefaultField(`field${index}`, `业务字段${index}`)
}

export function createFieldFromTemplate(template = {}) {
  const fieldName = normalizeFieldName(template.field || template.columnName || 'fieldName')
  const systemDefinition = systemFieldDefinitions.find(item => item.field === fieldName || item.columnName === template.columnName)
  if (systemDefinition)
    return createSystemField(systemDefinition)
  return {
    ...createDefaultField(fieldName, template.label || '字段名称'),
    ...template,
    field: fieldName,
    columnName: template.columnName || camelToSnake(fieldName),
    dataType: template.dataType || 'varchar',
    length: template.length || 128,
    precision: template.precision ?? 2,
    required: Boolean(template.required),
    searchable: Boolean(template.searchable),
    listVisible: template.listVisible !== false,
    formVisible: template.formVisible !== false,
    componentType: template.componentType || 'input',
    queryType: template.queryType || 'like',
    sensitiveType: template.sensitiveType || 'NONE',
    encryptAlgorithm: template.encryptAlgorithm || '',
    sortable: Boolean(template.sortable),
    primaryKey: false,
    systemField: false,
    readonly: false,
    autoIncrement: false,
    width: template.width || 160,
    remark: template.remark || '',
  }
}

export function createFieldFromGenColumn(column = {}) {
  const fieldName = normalizeFieldName(column.javaField || column.columnName || 'fieldName')
  const dataType = resolveDataType(column.columnType, column.javaType)
  return {
    ...createDefaultField(fieldName, column.columnComment || fieldName),
    field: fieldName,
    columnName: column.columnName || camelToSnake(fieldName),
    label: column.columnComment || fieldName,
    dataType,
    length: resolveColumnLength(column.columnType, dataType),
    precision: resolveColumnPrecision(column.columnType),
    required: Boolean(column.isRequired),
    searchable: Boolean(column.isQuery),
    listVisible: column.isList !== 0,
    formVisible: column.isEdit !== 0,
    componentType: resolveComponentType(column.htmlType, dataType),
    queryType: resolveQueryType(column.queryType),
    dictType: column.dictType || '',
    sensitiveType: column.desensitizeType || 'NONE',
    sortable: false,
    primaryKey: Boolean(column.isPk),
    systemField: isAuditField({ field: fieldName, columnName: column.columnName }),
    readonly: isAuditField({ field: fieldName, columnName: column.columnName }),
    autoIncrement: Boolean(column.isPk) && String(column.extra || column.columnExtra || '').toLowerCase().includes('auto_increment'),
    width: dataType === 'datetime' ? 180 : 160,
    remark: column.columnComment || '',
  }
}

export function isAuditField(field = {}) {
  return auditFieldNames.includes(field.field) || auditColumnNames.includes(field.columnName)
}

function resolveDataType(columnType = '', javaType = '') {
  const type = String(columnType || '').toLowerCase()
  const java = String(javaType || '').toLowerCase()
  if (type.includes('bigint') || java.includes('long'))
    return 'bigint'
  if (type.includes('int') || java.includes('integer'))
    return type.includes('tinyint') ? 'tinyint' : 'int'
  if (type.includes('decimal') || type.includes('numeric') || java.includes('bigdecimal'))
    return 'decimal'
  if (type.includes('datetime') || type.includes('timestamp') || java.includes('localdatetime'))
    return 'datetime'
  if (type === 'time' || java.includes('localtime'))
    return 'time'
  if (type === 'date' || java.includes('localdate'))
    return 'date'
  if (type.includes('text') || type.includes('json'))
    return 'text'
  return 'varchar'
}

function resolveColumnLength(columnType = '', dataType = 'varchar') {
  const match = String(columnType || '').match(/\((\d+)/)
  if (match)
    return Number(match[1])
  if (dataType === 'text')
    return null
  return dataType === 'varchar' ? 128 : null
}

function resolveColumnPrecision(columnType = '') {
  const match = String(columnType || '').match(/\(\d+\s*,\s*(\d+)\)/)
  return match ? Number(match[1]) : 2
}

function resolveComponentType(htmlType = '', dataType = 'varchar') {
  const type = String(htmlType || '').toLowerCase()
  const componentMap = {
    textarea: 'textarea',
    select: 'select',
    radio: 'radio',
    checkbox: 'checkbox',
    dictselect: 'dictSelect',
    switch: 'switch',
    treeselect: 'treeSelect',
    orgtreeselect: 'orgTreeSelect',
    userselect: 'userSelect',
    regiontreeselect: 'regionTreeSelect',
    cascader: 'cascader',
    imageupload: 'imageUpload',
    fileupload: 'fileUpload',
  }
  if (componentMap[type])
    return componentMap[type]
  if (type.includes('date') || ['date', 'datetime', 'time'].includes(dataType))
    return ['date', 'datetime', 'time'].includes(dataType) ? dataType : 'date'
  if (['int', 'bigint', 'decimal'].includes(dataType))
    return 'number'
  if (dataType === 'tinyint')
    return 'switch'
  return dataType === 'text' ? 'textarea' : 'input'
}

function resolveQueryType(queryType = '') {
  const type = String(queryType || '').toLowerCase()
  if (['eq', 'like', 'ge', 'le', 'between', 'in'].includes(type))
    return type
  if (type === '=' || type === 'equal')
    return 'eq'
  return type || 'like'
}
