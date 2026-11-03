import { request } from '@/utils'

function pickRows(data) {
  if (Array.isArray(data))
    return data
  if (Array.isArray(data?.list))
    return data.list
  if (Array.isArray(data?.records))
    return data.records
  return []
}

export async function resolveCreatedRoleRecord(submitted = {}, tenantId) {
  const roleKey = submitted.roleKey
  if (!roleKey)
    return null
  const res = await request.get('/system/role/page', {
    params: {
      pageNum: 1,
      pageSize: 20,
      roleKey,
      tenantId: tenantId ?? submitted.tenantId,
    },
  })
  const rows = pickRows(res?.data)
  return rows.find(row => row.roleKey === roleKey) || rows[0] || null
}

export async function resolveCreatedPostRecord(submitted = {}, tenantId) {
  const postCode = submitted.postCode
  if (!postCode)
    return null
  const res = await request.get('/system/post/list', {
    params: {
      postCode,
      tenantId: tenantId ?? submitted.tenantId,
    },
  })
  const rows = pickRows(res?.data)
  return rows.find(row => row.postCode === postCode) || rows[0] || null
}

export async function resolveCreatedDictDataRecord(submitted = {}) {
  const dictType = submitted.dictType
  const dictValue = submitted.dictValue
  if (!dictType || dictValue === undefined || dictValue === null || dictValue === '')
    return null
  const res = await request.get('/system/dict/data/list', {
    params: { dictType },
  })
  const rows = pickRows(res?.data)
  return rows.find(row => String(row.dictValue) === String(dictValue)) || rows[0] || null
}

export async function resolveCreatedOrgRecord(submitted = {}, tenantId) {
  const orgName = submitted.orgName
  if (!orgName)
    return null
  const res = await request.get('/system/org/tree', {
    params: {
      tenantId: tenantId ?? submitted.tenantId,
    },
  })
  const rows = flattenTree(pickRows(res?.data))
  const parentId = submitted.parentId ?? 0
  return rows.find(row =>
    row.orgName === orgName
    && Number(row.parentId ?? 0) === Number(parentId ?? 0),
  ) || rows.find(row => row.orgName === orgName) || null
}

export async function resolveCreatedClientRecord(submitted = {}) {
  const clientCode = submitted.clientCode
  if (!clientCode)
    return null
  const res = await request.get('/system/client/list')
  const rows = pickRows(res?.data)
  return rows.find(row => row.clientCode === clientCode) || rows[0] || null
}

function flattenTree(nodes = [], childrenField = 'children') {
  const result = []
  const walk = (list) => {
    ;(list || []).forEach((node) => {
      if (!node)
        return
      result.push(node)
      if (Array.isArray(node[childrenField]) && node[childrenField].length)
        walk(node[childrenField])
    })
  }
  walk(nodes)
  return result
}
