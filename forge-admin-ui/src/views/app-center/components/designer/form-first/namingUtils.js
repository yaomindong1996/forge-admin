const KNOWN_FIELD_CODES = {
  客户名称: 'customerName',
  联系电话: 'contactPhone',
  客户等级: 'customerLevel',
  客户状态: 'customerStatus',
  负责人: 'ownerUserId',
  所属部门: 'ownerDeptId',
  所属地区: 'regionCode',
  所属区域: 'regionCode',
  备注: 'remark',
  客户编码: 'customerCode',
  联系人: 'contactName',
  联系邮箱: 'contactEmail',
  详细地址: 'address',
  跟进状态: 'followStatus',
  创建时间: 'createTime',
  更新时间: 'updateTime',
}

const CHINESE_TERMS = [
  ['客户', 'customer'],
  ['联系人', 'contact'],
  ['联系', 'contact'],
  ['电话', 'phone'],
  ['手机号', 'mobile'],
  ['手机', 'mobile'],
  ['邮箱', 'email'],
  ['邮件', 'email'],
  ['名称', 'name'],
  ['姓名', 'name'],
  ['编码', 'code'],
  ['编号', 'no'],
  ['代码', 'code'],
  ['等级', 'level'],
  ['级别', 'level'],
  ['状态', 'status'],
  ['类型', 'type'],
  ['分类', 'category'],
  ['负责人', 'ownerUserId'],
  ['所属', 'owner'],
  ['部门', 'dept'],
  ['组织', 'org'],
  ['机构', 'org'],
  ['地区', 'region'],
  ['区域', 'region'],
  ['省份', 'province'],
  ['城市', 'city'],
  ['地址', 'address'],
  ['详细', 'detail'],
  ['备注', 'remark'],
  ['描述', 'description'],
  ['说明', 'description'],
  ['创建', 'create'],
  ['更新', 'update'],
  ['时间', 'time'],
  ['日期', 'date'],
  ['开始', 'start'],
  ['结束', 'end'],
  ['跟进', 'follow'],
  ['行业', 'industry'],
  ['来源', 'source'],
  ['性别', 'gender'],
  ['年龄', 'age'],
  ['金额', 'amount'],
  ['价格', 'price'],
  ['数量', 'quantity'],
  ['单价', 'unitPrice'],
  ['总价', 'totalPrice'],
  ['折扣', 'discount'],
  ['税额', 'taxAmount'],
  ['合同', 'contract'],
  ['订单', 'order'],
  ['产品', 'product'],
  ['商品', 'product'],
  ['供应商', 'supplier'],
  ['商机', 'opportunity'],
  ['线索', 'lead'],
  ['回款', 'payment'],
  ['支付', 'payment'],
  ['付款', 'payment'],
  ['收款', 'receipt'],
  ['发票', 'invoice'],
  ['项目', 'project'],
  ['任务', 'task'],
  ['计划', 'plan'],
  ['审批', 'approval'],
  ['流程', 'flow'],
  ['用户', 'user'],
  ['人员', 'user'],
  ['员工', 'employee'],
  ['角色', 'role'],
  ['岗位', 'post'],
  ['菜单', 'menu'],
  ['权限', 'permission'],
  ['数据', 'data'],
  ['字典', 'dict'],
  ['父级', 'parent'],
  ['上级', 'parent'],
  ['下级', 'child'],
  ['子级', 'child'],
  ['是否', 'is'],
  ['启用', 'enabled'],
  ['禁用', 'disabled'],
  ['有效', 'valid'],
  ['失效', 'invalid'],
  ['排序', 'sort'],
  ['图片', 'image'],
  ['附件', 'file'],
  ['文件', 'file'],
  ['标题', 'title'],
  ['内容', 'content'],
  ['明细', 'detail'],
  ['详情', 'detail'],
  ['主表', 'master'],
  ['子表', 'detail'],
  ['关联', 'relation'],
  ['引用', 'reference'],
  ['申请', 'apply'],
  ['审核', 'audit'],
  ['通过', 'passed'],
  ['拒绝', 'rejected'],
  ['评分', 'score'],
  ['标签', 'tag'],
  ['颜色', 'color'],
  ['规格', 'spec'],
  ['单位', 'unit'],
  ['库存', 'stock'],
  ['仓库', 'warehouse'],
  ['物流', 'logistics'],
  ['运输', 'transport'],
  ['车牌', 'plateNo'],
  ['司机', 'driver'],
  ['车辆', 'vehicle'],
  ['银行', 'bank'],
  ['账号', 'accountNo'],
  ['账户', 'account'],
  ['开户行', 'bankName'],
  ['管理', 'management'],
  ['选择器', 'selector'],
  ['选择', 'select'],
  ['单选', 'radio'],
  ['多选', 'checkbox'],
  ['输入框', 'input'],
  ['输入', 'input'],
  ['字段', 'field'],
  ['开关', 'switch'],
]

const SORTED_CHINESE_TERMS = [...CHINESE_TERMS].sort((a, b) => b[0].length - a[0].length)

export function generateFieldCode(label = '') {
  const text = String(label || '').trim()
  if (KNOWN_FIELD_CODES[text])
    return KNOWN_FIELD_CODES[text]
  const words = inferNameWords(text)
  if (words.length)
    return toLowerCamel(words)
  return `field${stableHash(text).toString(36)}`
}

export function generateObjectCode(name = '') {
  const words = inferNameWords(name)
  return normalizeSnakeCode(words.length ? words.join('_') : name, 'business_object', 48)
}

export function generateSuiteCode(name = '') {
  const words = inferNameWords(name)
  return normalizeSnakeCode(words.length ? words.join('_') : name, 'BUSINESS', 32).toUpperCase()
}

export function normalizeFieldCode(value = '', fallbackLabel = '') {
  const source = String(value || '').trim() || generateFieldCode(fallbackLabel)
  const words = source.includes('_') ? source.split('_') : splitAsciiWords(source)
  const normalized = words.length ? toLowerCamel(words) : generateFieldCode(source)
  return ensureLeadingLetter(normalized || 'field', 'field').slice(0, 64)
}

export function normalizeObjectCode(value = '', fallbackName = '') {
  const source = String(value || '').trim() || generateObjectCode(fallbackName)
  return normalizeSnakeCode(source, 'business_object', 48)
}

export function normalizeSuiteCode(value = '', fallbackName = '') {
  const source = String(value || '').trim() || generateSuiteCode(fallbackName)
  return normalizeSnakeCode(source, 'BUSINESS', 32).toUpperCase()
}

export function buildModelCode(suiteCode = '', objectCode = '') {
  const suite = normalizeSnakeCode(suiteCode, '', 24)
  const object = normalizeSnakeCode(objectCode, 'business_object', 48)
  if (!suite || object.startsWith(`${suite}_`))
    return object.slice(0, 64)
  return `${suite}_${object}`.slice(0, 64)
}

export function camelToSnake(value = '') {
  return normalizeSnakeCode(value, '', 64)
}

function inferNameWords(value = '') {
  const words = []
  const segments = String(value || '').match(/[a-z0-9]+|[\u4E00-\u9FFF]+/gi) || []
  segments.forEach((segment) => {
    if (/^[a-z0-9]+$/i.test(segment)) {
      words.push(...splitAsciiWords(segment))
      return
    }
    words.push(...translateChineseSegment(segment))
  })
  return words.map(normalizeWord).filter(Boolean)
}

function translateChineseSegment(segment = '') {
  const words = []
  let index = 0
  while (index < segment.length) {
    const match = SORTED_CHINESE_TERMS.find(([term]) => segment.startsWith(term, index))
    if (match) {
      words.push(...splitAsciiWords(match[1]))
      index += match[0].length
    }
    else {
      index += 1
    }
  }
  return words
}

function splitAsciiWords(value = '') {
  const normalized = String(value || '')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[^a-z0-9]+/gi, ' ')
    .trim()
  return normalized ? normalized.split(/\s+/) : []
}

function normalizeWord(value = '') {
  const word = String(value || '').replace(/[^a-z0-9]/gi, '')
  if (!word)
    return ''
  return word.toLowerCase()
}

function toLowerCamel(words = []) {
  return words.map(normalizeWord).filter(Boolean).map((word, index) => {
    if (index === 0)
      return word
    return word.slice(0, 1).toUpperCase() + word.slice(1)
  }).join('')
}

function normalizeSnakeCode(value = '', fallback = 'code', maxLength = 64) {
  const source = String(value || '')
  const words = inferNameWords(source)
  let normalized = words.length
    ? words.join('_')
    : source
        .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
        .replace(/\W+/g, '_')
        .replace(/_+/g, '_')
        .replace(/^_+|_+$/g, '')
        .toLowerCase()
  normalized = ensureLeadingLetter(normalized || fallback, fallback)
  return normalized.slice(0, maxLength).replace(/_+$/g, '') || fallback
}

function ensureLeadingLetter(value = '', prefix = 'field') {
  const normalized = String(value || '').replace(/^_+/, '')
  if (/^[a-z]/i.test(normalized))
    return normalized
  return `${prefix}_${normalized}`.replace(/_+/g, '_')
}

function stableHash(value = '') {
  let hash = 0
  const text = String(value || '')
  for (let index = 0; index < text.length; index += 1)
    hash = ((hash * 31) + text.charCodeAt(index)) >>> 0
  return hash
}
