const text = value => String(value ?? '').trim()

export function extractBusinessBindingRows(response) {
  const data = response?.data ?? response
  if (Array.isArray(data))
    return data
  return data?.records || data?.list || []
}

/** 表单引用是设计器明确选择的应用；共享对象的反查主应用不能覆盖该上下文。 */
export function mergeModelBusinessBindings(bindings = [], formBindings = []) {
  const configured = formBindings[0]
  if (!configured)
    return bindings
  if (!text(configured.applicationId) && !text(configured.applicationName))
    return bindings.length ? bindings : formBindings
  return [configured, ...bindings.filter(item => text(item.objectCode) !== text(configured.objectCode))]
}

export function formatBusinessBindings(row = {}) {
  if (row.bindingLoadError)
    return '业务绑定暂不可用'
  const names = (row.businessBindings || []).map((item) => {
    const application = text(item.applicationName || item.applicationCode)
    const object = text(item.objectName || item.objectCode)
    return application && object && application !== object
      ? `${application} · ${object}`
      : application || object || text(item.suiteName) || text(item.bindingName)
  }).filter(Boolean)
  if (!names.length)
    return '未绑定业务应用'
  return names.length > 2 ? `${names.slice(0, 2).join('、')} 等 ${names.length} 个业务应用` : names.join('、')
}

export function getModelSecondaryAction(row) {
  if (row.status === 0)
    return { key: 'deploy', label: '发布流程' }
  if (row.status === 1 && row.publicationState === 'CHANGED')
    return { key: 'deploy', label: '发布更新' }
  if ([1, 2].includes(row.status))
    return { key: 'instances', label: '运行记录' }
  return null
}

export function publishedVersionLabel(row) {
  // 不再拿模型初始 version=1 冒充已发布版本，不推算“下一版”。
  const version = row.publishedVersion
  if (Number.isInteger(version) && version > 0)
    return `发布版本 v${version}`
  return row.publicationState === 'UNPUBLISHED' ? '流程草稿' : '发布版本 —'
}
