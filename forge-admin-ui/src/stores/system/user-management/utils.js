import { h } from 'vue'
import { getOrganizationNodeIcon } from '@/components/common/organization-tree-icons'
import DictTag from '@/components/DictTag.vue'

export const USER_TYPE_DICT = 'sys_user_type'

export const USER_STATUS_DICT = 'sys_user_status'

export const USER_SEX_DICT = 'sys_user_sex'

export const ROLE_DATA_SCOPE_DICT = 'sys_role_data_scope'

export const ROLE_TYPE_DICT = 'sys_role_type'

export const NORMAL_DISABLE_DICT = 'sys_normal_disable'

export const FORM_TENANT_ID_EXPR = '$' + '{tenantId}'

export const FORM_MAIN_ORG_ID_EXPR = '$' + '{mainOrgId}'

export const resetPwdRules = {
  password: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码不能少于6位', trigger: 'blur' }],
}

export function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: normalizeSingleNumber(item.value, item.value),
  }))
}

export function normalizeSingleNumber(value, fallback = null) {
  if (Array.isArray(value)) {
    const first = value.find(item => item !== null && item !== undefined && item !== '')
    return normalizeSingleNumber(first, fallback)
  }
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? fallback : numberValue
}

export function normalizeNumberList(value) {
  const list = Array.isArray(value) ? value : (value === null || value === undefined || value === '' ? [] : [value])
  return Array.from(new Set(list
    .map(item => normalizeSingleNumber(item))
    .filter(item => item !== null)))
}

export function flattenOrgNodes(list = []) {
  return (list || []).flatMap((item) => {
    const current = [item]
    const children = flattenOrgNodes(item.children || [])
    return [...current, ...children]
  })
}

export function convertOrgToTreeSelect(list = []) {
  return (list || []).map(item => ({
    label: item.orgName || item.label || String(item.id),
    value: normalizeSingleNumber(item.id),
    key: normalizeSingleNumber(item.id),
    children: item.children?.length ? convertOrgToTreeSelect(item.children) : undefined,
  }))
}

export function formatUserOrgBindingLabel(item = {}) {
  const roleNames = Array.isArray(item.roleNames) ? item.roleNames.filter(Boolean) : []
  const suffix = roleNames.length > 0
    ? ` · ${roleNames.slice(0, 2).join('、')}${roleNames.length > 2 ? '等' : ''}`
    : ''
  return `${item.orgName || item.orgId}${Number(item.isMain) === 1 ? ' · 主组织' : ''}${suffix}`
}

export function resolveTenantIds(data = {}, fallbackTenantId = null) {
  const tenantIds = normalizeNumberList(data.tenantIds)
  if (tenantIds.length > 0) {
    return tenantIds
  }

  const tenantId = normalizeSingleNumber(data.tenantId)
  if (tenantId !== null) {
    return [tenantId]
  }

  const fallback = normalizeSingleNumber(fallbackTenantId)
  return fallback !== null ? [fallback] : []
}

export function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

export function countTreeNodes(list = []) {
  return list.reduce((total, item) => total + 1 + countTreeNodes(item.children || []), 0)
}

export function isSameKey(left, right) {
  return String(left) === String(right)
}

export function formatTenantNameList(row = {}) {
  const tenantNames = String(row.tenantName || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
  if (tenantNames.length > 0) {
    return Array.from(new Set(tenantNames))
  }
  if (row.tenantId) {
    return [String(row.tenantId)]
  }
  return []
}

export function splitTableCellValues(value) {
  const rawValues = Array.isArray(value) ? value : [value]
  return rawValues
    .flatMap(item => String(item || '').split(/[、,，]/))
    .map(item => item.trim())
    .filter(Boolean)
}

export function resolveUserDisplayName(row = {}) {
  return row.realName || row.nickname || row.username || `用户${row.id || ''}`
}

export function resolveUserAccountLabel(row = {}) {
  const username = String(row.username || '').trim()
  return username && username !== resolveUserDisplayName(row) ? `@${username}` : ''
}

export function resolveOptionLabel(options = [], value) {
  return options.find(option => isSameKey(option?.value, value))?.label || ''
}

export function renderDictTag(options = [], value, className = '') {
  return h('span', { class: ['user-table-tag', className] }, [
    h(DictTag, { options, value, size: 'small', forceTag: true }),
  ])
}

export function resolveRoleDictValue(row = {}, field) {
  const aliasMap = {
    roleType: ['roleType', 'role_type', 'type'],
    dataScope: ['dataScope', 'data_scope'],
    roleStatus: ['roleStatus', 'role_status', 'status'],
  }
  const keys = aliasMap[field] || [field]
  const value = keys.map(key => row[key]).find(item => item !== null && item !== undefined && item !== '')
  return normalizeSingleNumber(value, value ?? '')
}

export function getLeftOrgNodeIcon(node = {}) {
  return getOrganizationNodeIcon(node)
}

export function getLeftOrgNodeTone(node = {}) {
  if (!node.parentId || Number(node.parentId) === 0) {
    return 'folder'
  }
  return node.children?.length ? 'folder' : 'menu'
}

export function normalizeOrgTreeNodes(list = []) {
  return (list || []).map(item => ({
    ...item,
    children: Array.isArray(item.children) ? normalizeOrgTreeNodes(item.children) : [],
  }))
}

export function convertRegionToTreeSelect(list, virtualDisabled = true) {
  return list.map((item) => {
    const node = {
      label: item.name,
      value: item.code,
      key: item.code,
    }
    if (virtualDisabled && item.code && item.code.endsWith('ALL')) {
      node.disabled = true
    }
    if (item.children && item.children.length > 0) {
      node.children = convertRegionToTreeSelect(item.children, virtualDisabled)
    }
    return node
  })
}

export function findOrgNode(treeData, orgId) {
  for (const node of treeData) {
    if (node.id === orgId) {
      return node
    }
    if (node.children && node.children.length > 0) {
      const found = findOrgNode(node.children, orgId)
      if (found) {
        return found
      }
    }
  }
  return null
}
